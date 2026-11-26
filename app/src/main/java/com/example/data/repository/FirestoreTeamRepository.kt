package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.data.model.ApprovalStatus
import com.example.data.model.CloudAccessToken
import com.example.data.model.CloudAccessTokenStatus
import com.example.data.model.HierarchyRequest
import com.example.data.model.HierarchyRequestStatus
import com.example.data.model.HierarchyRequestType
import com.example.data.model.HierarchyTreeBuilder
import com.example.data.model.MensajeGlobal
import com.example.data.model.MensajeGlobalPrioridad
import com.example.data.model.PsaContactLead
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaLeadStatus
import com.example.data.model.PsaPostSaleInstallation
import com.example.data.model.PsaPostSaleStatus
import com.example.data.model.PsaVisitStatus
import com.example.data.model.RoleVisibilityConfig
import com.example.data.model.SellerPerformanceStats
import com.example.data.model.TeamNotification
import com.example.data.model.TeamNotificationPriority
import com.example.data.model.TeamNotificationTargetMode
import com.example.data.model.TeamNotificationType
import com.example.data.model.UserAccount
import java.security.SecureRandom
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

sealed class CloudAuthOutcome {
    data class Authorized(val user: UserAccount, val serverWallMs: Long = System.currentTimeMillis()) : CloudAuthOutcome()
    data class Restricted(val user: UserAccount, val serverWallMs: Long = System.currentTimeMillis()) : CloudAuthOutcome()
    data class NotAuthorized(val email: String, val displayName: String) : CloudAuthOutcome()
    data class DeviceLimitReached(val user: UserAccount, val currentDeviceLabel: String) : CloudAuthOutcome()
    data class Error(val message: String) : CloudAuthOutcome()
}

data class CloudUserSnapshotEvent(
    val user: UserAccount?,
    val exists: Boolean,
    val isFromServer: Boolean,
    val serverWallMs: Long
)

class FirestoreTeamRepository(context: Context) : AgendaCloudSyncGateway {

    private val appContext = context.applicationContext
    private val firestore: FirebaseFirestore by lazy {
        val dbId = appContext.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val credentialManager: CredentialManager by lazy { CredentialManager.create(appContext) }
    private val devicePrefs = appContext.getSharedPreferences("ezo_cloud_identity", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "FirestoreTeamRepo"
        private const val COLLECTION_USERS = "authorized_users"
        private const val COLLECTION_CONFIG = "team_config"
        private const val DOC_ROLE_MATRIX = "role_matrix"
        private const val DOC_PSA_PRICING_SYNC = "psa_pricing_sync"
        private const val DOC_APP_VERSION = "app_version"
        private const val COLLECTION_WORKSPACES = "seller_workspaces"
        private const val SUBCOLLECTION_CONTACTS = "contacts"
        private const val SUBCOLLECTION_VISITS = "visits"
        private const val SUBCOLLECTION_POST_SALE = "post_sale"
        private const val COLLECTION_SELLER_STATS = "seller_stats"
        private const val COLLECTION_APP_ERRORS = "AppErrors"
        private const val COLLECTION_ACCESS_TOKENS = "access_tokens"
        private const val COLLECTION_HIERARCHY_REQUESTS = "hierarchy_requests"
        private const val COLLECTION_TEAM_NOTIFICATIONS = "team_notifications"
        private const val COLLECTION_GLOBAL_MESSAGES = "mensajes_globales"
        private const val KEY_DEVICE_ID = "persistent_hardware_device_id"
        private const val KEY_LAST_SYNCED_UID = "last_synced_seller_uid"
        const val BOOTSTRAP_ADMIN_EMAIL = "leonelizondo16@gmail.com"
    }

    fun getLastSyncedUid(): String = devicePrefs.getString(KEY_LAST_SYNCED_UID, "").orEmpty()

    fun setLastSyncedUid(uid: String) {
        devicePrefs.edit().putString(KEY_LAST_SYNCED_UID, uid).apply()
    }

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser

    override fun currentOwnerUid(): String = auth.currentUser?.uid.orEmpty()

    override suspend fun upsertVisit(ownerUid: String, visit: PsaHomeVisit): Result<Unit> =
        upsertSellerVisit(ownerUid, visit)

    override suspend fun deleteVisit(ownerUid: String, visitId: String): Result<Unit> =
        deleteSellerVisit(ownerUid, visitId)

    override suspend fun upsertPostSale(ownerUid: String, record: PsaPostSaleInstallation): Result<Unit> =
        upsertPostSaleRecord(ownerUid, record)

    override suspend fun deletePostSale(ownerUid: String, recordId: String): Result<Unit> =
        deletePostSaleRecord(ownerUid, recordId)

    override suspend fun upsertContact(ownerUid: String, lead: PsaContactLead): Result<Unit> =
        upsertSellerContact(ownerUid, lead)

    override suspend fun deleteContact(ownerUid: String, contactId: String): Result<Unit> =
        deleteSellerContact(ownerUid, contactId)

    fun getOrCreateDeviceId(): String {
        val existing = devicePrefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        devicePrefs.edit().putString(KEY_DEVICE_ID, created).apply()
        return created
    }

    fun getDeviceHardwareLabel(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val model = Build.MODEL.orEmpty().trim()
        return listOf(manufacturer, model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Dispositivo Android" }
    }

    private fun resolveWebClientId(): String {
        val resId = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
        return if (resId != 0) appContext.getString(resId) else ""
    }

    /**
     * Flujo interactivo con Credential Manager (`GetSignInWithGoogleOption` en su propia solicitud).
     */
    suspend fun signInInteractiveWithGoogle(activityContext: Context): Result<FirebaseUser> {
        return try {
            val webClientId = resolveWebClientId()
            if (webClientId.isBlank()) {
                return Result.failure(IllegalStateException("Configuración de Google Sign-In no encontrada (default_web_client_id)."))
            }
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCred).await()
                val firebaseUser = authResult.user
                    ?: return Result.failure(IllegalStateException("No se pudo obtener la sesión de Firebase."))
                Result.success(firebaseUser)
            } else {
                Result.failure(IllegalStateException("Tipo de credencial inesperado."))
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Inicio de sesión cancelado por el usuario."))
        } catch (e: Exception) {
            Log.e(TAG, "Interactive Google sign-in failed", e)
            Result.failure(Exception(e.localizedMessage ?: "No se pudo iniciar sesión con Google."))
        }
    }

    /**
     * Auto-login silencioso al iniciar la app (`GetGoogleIdOption` en su propia GetCredentialRequest separada).
     */
    suspend fun trySilentGoogleSignIn(activityContext: Context): FirebaseUser? {
        auth.currentUser?.let { return it }
        return try {
            val webClientId = resolveWebClientId()
            if (webClientId.isBlank()) return null

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                auth.signInWithCredential(firebaseCred).await().user
            } else {
                null
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: NoCredentialException) {
            null
        } catch (e: Exception) {
            Log.d(TAG, "Silent Google sign-in skipped: ${e.message}")
            null
        }
    }

    suspend fun signOutGoogle() {
        withContext(NonCancellable) {
            try {
                auth.signOut()
            } catch (e: Exception) {
                Log.w(TAG, "Firebase signOut error: ${e.message}")
            }
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "CredentialManager clearCredentialState error: ${e.message}")
            }
        }
    }

    /**
     * Valida directamente contra servidor (`Source.SERVER`) si el email está habilitado, vigente y dentro del cupo de dispositivos.
     * Actualiza `updatedAt` (y cualquier alerta de manipulación horaria pendiente) para reflejar la última conexión real.
     */
    suspend fun verifyAndClaimAccess(
        emailRaw: String,
        firebaseUid: String,
        displayName: String = "",
        pendingTamperAlertMs: Long = 0L
    ): CloudAuthOutcome {
        val emailKey = normalizeEmail(emailRaw)
        if (emailKey.isBlank()) {
            return CloudAuthOutcome.Error("La cuenta de Google no devolvió un correo válido.")
        }

        val deviceId = getOrCreateDeviceId()
        val deviceLabel = getDeviceHardwareLabel()

        return try {
            val docRef = firestore.collection(COLLECTION_USERS).document(emailKey)
            val snapshot = docRef.get(Source.SERVER).await()

            if (!snapshot.exists()) {
                if (emailKey == BOOTSTRAP_ADMIN_EMAIL) {
                    val nowMs = System.currentTimeMillis()
                    val tenYearsMs = nowMs + (3650L * 24L * 60L * 60L * 1000L)
                    val safeAdminName = displayName.trim().ifBlank { "Administrador PSA" }.take(80)
                    val adminUser = UserAccount(
                        id = "admin_boot",
                        username = safeAdminName,
                        token = "GOOGLE-ADMIN",
                        role = "SUPERUSER",
                        daysGranted = 3650,
                        validFromMs = nowMs,
                        expiresAtMs = tenYearsMs,
                        isBlocked = false,
                        notes = "Superusuario Principal",
                        createdAt = nowMs,
                        canAccessLegacyModules = true,
                        email = emailKey,
                        maxDevices = 5,
                        boundDeviceIdsSerialized = deviceId,
                        boundDeviceLabelsSerialized = deviceLabel,
                        ownerUid = firebaseUid,
                        lastOnlineVerifiedAtMs = nowMs
                    )
                    docRef.set(userToCreateMap(adminUser)).await()
                    return CloudAuthOutcome.Authorized(adminUser, serverWallMs = nowMs)
                }
                return CloudAuthOutcome.NotAuthorized(emailKey, displayName)
            }

            val parsedUser = parseUserDocument(snapshot)
                ?: return CloudAuthOutcome.Error("El perfil en la nube tiene un formato incompatible.")
            val enforcedRole = if (emailKey == BOOTSTRAP_ADMIN_EMAIL) "SUPERUSER" else "OPERATOR"
            val cloudUser = if (parsedUser.role != enforcedRole) {
                parsedUser.copy(role = enforcedRole)
            } else {
                parsedUser
            }

            val baseServerWallMs = cloudUser.lastOnlineVerifiedAtMs.takeIf { it > 0L } ?: System.currentTimeMillis()
            if (cloudUser.isBlocked || cloudUser.isExpired || cloudUser.isPendingApproval) {
                return CloudAuthOutcome.Restricted(cloudUser, serverWallMs = baseServerWallMs)
            }

            val alreadyBound = cloudUser.isDeviceAllowed(deviceId)
            if (alreadyBound) {
                val heartbeatPayload = hashMapOf<String, Any>(
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cloudUser.ownerUid.isBlank()) {
                    heartbeatPayload["ownerUid"] = firebaseUid
                }
                if (pendingTamperAlertMs > 0L) {
                    heartbeatPayload["lastTamperAlertAtMs"] = pendingTamperAlertMs
                }
                try {
                    docRef.update(heartbeatPayload).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Heartbeat update skipped for $emailKey: ${e.message}")
                }
                val refreshedSnap = try {
                    docRef.get(Source.SERVER).await()
                } catch (_: Exception) {
                    null
                }
                val refreshedUser = refreshedSnap?.let { parseUserDocument(it) } ?: cloudUser.copy(
                    ownerUid = cloudUser.ownerUid.ifBlank { firebaseUid },
                    lastOnlineVerifiedAtMs = System.currentTimeMillis()
                )
                val verifiedServerMs = refreshedUser.lastOnlineVerifiedAtMs.takeIf { it > 0L } ?: System.currentTimeMillis()
                return CloudAuthOutcome.Authorized(refreshedUser, serverWallMs = verifiedServerMs)
            }

            val currentIds = cloudUser.boundDeviceIds
            val currentLabels = alignLabelsWithIds(currentIds, cloudUser.boundDeviceLabels)
            if (currentIds.size >= cloudUser.maxDevices) {
                return CloudAuthOutcome.DeviceLimitReached(cloudUser, deviceLabel)
            }

            val nextIds = currentIds + deviceId
            val nextLabels = (currentLabels + deviceLabel).take(nextIds.size)
            val updatePayload = hashMapOf<String, Any>(
                "ownerUid" to (cloudUser.ownerUid.ifBlank { firebaseUid }),
                "boundDeviceIds" to nextIds,
                "boundDeviceLabels" to nextLabels,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (pendingTamperAlertMs > 0L) {
                updatePayload["lastTamperAlertAtMs"] = pendingTamperAlertMs
            }
            docRef.update(updatePayload).await()

            val updatedSnap = try {
                docRef.get(Source.SERVER).await()
            } catch (_: Exception) {
                null
            }
            val parsedUpdated = updatedSnap?.let { parseUserDocument(it) }
            val updatedUser = parsedUpdated ?: cloudUser.copy(
                ownerUid = cloudUser.ownerUid.ifBlank { firebaseUid },
                boundDeviceIdsSerialized = nextIds.joinToString("|"),
                boundDeviceLabelsSerialized = nextLabels.joinToString("|"),
                lastOnlineVerifiedAtMs = System.currentTimeMillis()
            )
            val verifiedServerMs = updatedUser.lastOnlineVerifiedAtMs.takeIf { it > 0L } ?: System.currentTimeMillis()
            CloudAuthOutcome.Authorized(updatedUser, serverWallMs = verifiedServerMs)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying cloud access for $emailKey", e)
            CloudAuthOutcome.Error("Sin conexión al servidor de licencias. Activa Wi-Fi o datos móviles e intenta nuevamente.")
        }
    }

    /**
     * Escucha en tiempo real cambios en el documento del usuario conectado distinguiendo caché local vs servidor.
     */
    fun observeUserDocument(emailRaw: String): Flow<CloudUserSnapshotEvent> = callbackFlow {
        val emailKey = normalizeEmail(emailRaw)
        if (emailKey.isBlank()) {
            trySend(CloudUserSnapshotEvent(user = null, exists = false, isFromServer = false, serverWallMs = 0L))
            close()
            return@callbackFlow
        }
        val registration: ListenerRegistration = firestore
            .collection(COLLECTION_USERS)
            .document(emailKey)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "User listener error for $emailKey: ${error.message}")
                    return@addSnapshotListener
                }
                val fromServer = snapshot != null && !snapshot.metadata.isFromCache
                if (snapshot == null || !snapshot.exists()) {
                    trySend(
                        CloudUserSnapshotEvent(
                            user = null,
                            exists = false,
                            isFromServer = fromServer,
                            serverWallMs = if (fromServer) System.currentTimeMillis() else 0L
                        )
                    )
                } else {
                    val parsed = parseUserDocument(snapshot)
                    val serverMs = if (fromServer) {
                        parsed?.lastOnlineVerifiedAtMs?.takeIf { it > 0L } ?: System.currentTimeMillis()
                    } else {
                        0L
                    }
                    trySend(
                        CloudUserSnapshotEvent(
                            user = parsed,
                            exists = true,
                            isFromServer = fromServer,
                            serverWallMs = serverMs
                        )
                    )
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Lista en tiempo real de todos los usuarios autorizados (uso exclusivo de Superusuario).
     */
    fun observeAllAuthorizedUsers(): Flow<List<UserAccount>> = callbackFlow {
        val registration: ListenerRegistration = firestore
            .collection(COLLECTION_USERS)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "All users listener error: ${error.message}")
                    return@addSnapshotListener
                }
                val users = snapshots?.documents?.mapNotNull { parseUserDocument(it) }.orEmpty()
                    .sortedByDescending { it.createdAtMs }
                trySend(users)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Sincronización en tiempo real de la matriz de visibilidad de pestañas.
     */
    fun observeRoleMatrix(): Flow<RoleVisibilityConfig?> = callbackFlow {
        val registration: ListenerRegistration = firestore
            .collection(COLLECTION_CONFIG)
            .document(DOC_ROLE_MATRIX)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "RoleMatrix listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        val parsed = RoleVisibilityConfig(
                            opPsaEnabled = (data["opPsaEnabled"] as? Boolean) ?: true,
                            opScriptsEnabled = (data["opScriptsEnabled"] as? Boolean) ?: false,
                            opAutomationEnabled = (data["opAutomationEnabled"] as? Boolean) ?: false,
                            opDashboardEnabled = (data["opDashboardEnabled"] as? Boolean) ?: false,
                            opAiSalesEnabled = (data["opAiSalesEnabled"] as? Boolean) ?: false,
                            opPricingEditEnabled = (data["opPricingEditEnabled"] as? Boolean) ?: false,
                            suPsaEnabled = (data["suPsaEnabled"] as? Boolean) ?: true,
                            suScriptsEnabled = (data["suScriptsEnabled"] as? Boolean) ?: true,
                            suAutomationEnabled = (data["suAutomationEnabled"] as? Boolean) ?: true,
                            suDashboardEnabled = (data["suDashboardEnabled"] as? Boolean) ?: true,
                            suSuperuserEnabled = (data["suSuperuserEnabled"] as? Boolean) ?: true
                        )
                        trySend(parsed)
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun fetchAllAuthorizedUsersOnce(): List<UserAccount> {
        return try {
            val snap = firestore.collection(COLLECTION_USERS).get().await()
            snap.documents.mapNotNull { parseUserDocument(it) }
                .sortedByDescending { it.createdAtMs }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "fetchAllAuthorizedUsersOnce failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun createOrAuthorizeOperator(
        emailRaw: String,
        username: String,
        daysGranted: Int,
        notes: String,
        maxDevices: Int,
        canAccessLegacyModules: Boolean = false
    ): Result<UserAccount> {
        val currentFirebaseUser = auth.currentUser
            ?: return Result.failure(
                IllegalStateException("Conecta tu cuenta de Google de Administrador antes de crear operadores en la nube.")
            )
        val emailKey = normalizeEmail(emailRaw)
        if (emailKey.isBlank() || !emailKey.contains("@")) {
            return Result.failure(IllegalArgumentException("Ingresa un correo de Google válido."))
        }
        val safeUsername = username.trim().take(80)
        if (safeUsername.length < 2) {
            return Result.failure(IllegalArgumentException("El nombre debe tener al menos 2 caracteres."))
        }
        val existingSnap = try {
            firestore.collection(COLLECTION_USERS).document(emailKey).get().await()
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return Result.failure(
                    IllegalStateException(
                        "Permiso denegado en Firestore para ${currentFirebaseUser.email ?: "tu cuenta"}. Reconecta tu sesión Google de Administrador."
                    )
                )
            }
            return Result.failure(Exception("Error consultando Firestore: ${e.localizedMessage ?: e.code.name}"))
        } catch (e: Exception) {
            return Result.failure(Exception("No se pudo verificar el usuario en Firestore: ${e.localizedMessage}"))
        }
        if (existingSnap.exists()) {
            return Result.failure(IllegalStateException("Ya existe un usuario habilitado con el correo $emailKey."))
        }

        val safeMaxDevices = maxDevices.coerceIn(1, 5)
        val safeDays = daysGranted.coerceIn(1, 3650)
        val nowMs = System.currentTimeMillis()
        val expiresAtMs = nowMs + (safeDays.toLong() * 24L * 60L * 60L * 1000L)

        val newUser = UserAccount(
            id = "op_${UUID.randomUUID().toString().take(8)}",
            username = safeUsername,
            token = "GOOGLE-AUTH",
            role = "OPERATOR",
            daysGranted = safeDays,
            validFromMs = nowMs,
            expiresAtMs = expiresAtMs,
            isBlocked = false,
            notes = notes.trim().take(300),
            createdAt = nowMs,
            canAccessLegacyModules = canAccessLegacyModules,
            email = emailKey,
            maxDevices = safeMaxDevices,
            boundDeviceIdsSerialized = "",
            boundDeviceLabelsSerialized = "",
            ownerUid = ""
        )

        return try {
            firestore.collection(COLLECTION_USERS)
                .document(emailKey)
                .set(userToCreateMap(newUser))
                .await()
            Result.success(newUser)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: FirebaseFirestoreException) {
            val detail = if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "Permiso denegado: verifica que iniciaste sesión con el correo Administrador en la nube."
            } else {
                e.localizedMessage ?: e.code.name
            }
            Result.failure(Exception("No se pudo guardar en la nube: $detail"))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo guardar en la nube: ${e.localizedMessage}"))
        }
    }

    suspend fun updateUserInCloud(user: UserAccount): Result<Unit> {
        val currentFbUser = auth.currentUser
            ?: return Result.failure(
                IllegalStateException("Conecta tu cuenta Google de Superusuario arriba para guardar cambios en Firestore.")
            )
        val emailKey = normalizeEmail(user.email)
        if (emailKey.isBlank()) return Result.failure(IllegalArgumentException("Usuario sin correo configurado."))
        val safeMaxDevices = user.maxDevices.coerceIn(user.boundDeviceIds.size.coerceAtLeast(1), 5)
        val sanitized = user.copy(maxDevices = safeMaxDevices)
        val docRef = firestore.collection(COLLECTION_USERS).document(emailKey)
        return try {
            docRef.update(userToUpdateMap(sanitized)).await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                return try {
                    docRef.set(userToCreateMap(sanitized)).await()
                    Result.success(Unit)
                } catch (ce2: CancellationException) {
                    throw ce2
                } catch (e2: Exception) {
                    handleFirestoreError("updateUserInCloud(set)", e2)
                    Result.failure(Exception("No se pudo crear el usuario en Firestore: ${e2.localizedMessage}"))
                }
            }
            handleFirestoreError("updateUserInCloud", e)
            val msg = if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "Permiso denegado en Firestore para ${currentFbUser.email ?: "tu cuenta"}. Reconecta el Google de Superusuario."
            } else {
                "Error actualizando en Firestore: ${e.localizedMessage ?: e.code.name}"
            }
            Result.failure(Exception(msg))
        } catch (e: Exception) {
            handleFirestoreError("updateUserInCloud", e)
            Result.failure(Exception("Error actualizando en Firestore: ${e.localizedMessage}"))
        }
    }

    suspend fun unlinkDeviceFromUser(user: UserAccount, deviceIdToRemove: String): Result<UserAccount> {
        val emailKey = normalizeEmail(user.email)
        if (emailKey.isBlank()) return Result.failure(IllegalArgumentException("Usuario sin correo."))

        val currentIds = user.boundDeviceIds
        val currentLabels = alignLabelsWithIds(currentIds, user.boundDeviceLabels)
        val nextIds = mutableListOf<String>()
        val nextLabels = mutableListOf<String>()

        currentIds.forEachIndexed { idx, id ->
            if (id != deviceIdToRemove) {
                nextIds.add(id)
                nextLabels.add(currentLabels[idx])
            }
        }

        val updatedUser = user.copy(
            boundDeviceIdsSerialized = nextIds.joinToString("|"),
            boundDeviceLabelsSerialized = nextLabels.joinToString("|"),
            ownerUid = if (nextIds.isEmpty()) "" else user.ownerUid
        )

        return updateUserInCloud(updatedUser).map { updatedUser }
    }

    suspend fun deleteUserFromCloud(emailRaw: String): Result<Unit> {
        val currentFbUser = auth.currentUser
            ?: return Result.failure(
                IllegalStateException("Conecta tu cuenta Google de Superusuario arriba para eliminar en Firestore.")
            )
        val emailKey = normalizeEmail(emailRaw)
        if (emailKey.isBlank() || emailKey == BOOTSTRAP_ADMIN_EMAIL) {
            return Result.failure(IllegalArgumentException("No se puede eliminar este perfil."))
        }
        return try {
            firestore.collection(COLLECTION_USERS).document(emailKey).delete().await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError("deleteUserFromCloud", e)
            val msg = if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "Permiso denegado para ${currentFbUser.email ?: "tu cuenta"} al eliminar en Firestore."
            } else {
                "No se pudo eliminar en Firestore: ${e.localizedMessage ?: e.code.name}"
            }
            Result.failure(Exception(msg))
        } catch (e: Exception) {
            handleFirestoreError("deleteUserFromCloud", e)
            Result.failure(Exception("No se pudo eliminar en Firestore: ${e.localizedMessage}"))
        }
    }

    suspend fun saveRoleMatrixToCloud(
        config: RoleVisibilityConfig,
        globalLegacyEnabled: Boolean? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sesión no iniciada."))
        val payload = hashMapOf<String, Any>(
            "configType" to DOC_ROLE_MATRIX,
            "updatedByUid" to uid,
            "updatedAt" to FieldValue.serverTimestamp(),
            "opPsaEnabled" to config.opPsaEnabled,
            "opScriptsEnabled" to config.opScriptsEnabled,
            "opAutomationEnabled" to config.opAutomationEnabled,
            "opDashboardEnabled" to config.opDashboardEnabled,
            "opAiSalesEnabled" to config.opAiSalesEnabled,
            "opPricingEditEnabled" to config.opPricingEditEnabled,
            "suPsaEnabled" to config.suPsaEnabled,
            "suScriptsEnabled" to config.suScriptsEnabled,
            "suAutomationEnabled" to config.suAutomationEnabled,
            "suDashboardEnabled" to config.suDashboardEnabled,
            "suSuperuserEnabled" to config.suSuperuserEnabled
        )
        if (globalLegacyEnabled != null) {
            payload["globalLegacyEnabled"] = globalLegacyEnabled
        }
        return try {
            firestore.collection(COLLECTION_CONFIG)
                .document(DOC_ROLE_MATRIX)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo guardar matriz en la nube: ${e.localizedMessage}"))
        }
    }

    /**
     * Publica los precios y cuotas PSA del país actual desde la cuenta Superusuario para todo el equipo.
     */
    suspend fun publishGlobalPsaPricingToCloud(
        snapshot: com.example.util.PsaCountryPricingSnapshot
    ): Result<String> {
        val fbUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Sesión Google requerida."))
        if (!fbUser.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)) {
            return Result.failure(IllegalStateException("Solo el Superusuario puede publicar precios globales."))
        }
        return try {
            val docRef = firestore.collection(COLLECTION_CONFIG).document(DOC_PSA_PRICING_SYNC)
            val existingPayloadJson = try {
                docRef.get().await().getString("payloadJson").orEmpty()
            } catch (_: Exception) {
                ""
            }
            val mergedJson = com.example.util.PsaPricingCloudSyncHelper.mergeAndSerializeCountrySnapshot(
                existingPayloadJson = existingPayloadJson,
                snapshot = snapshot
            )
            val payload = hashMapOf<String, Any>(
                "configType" to DOC_PSA_PRICING_SYNC,
                "updatedByUid" to fbUser.uid,
                "updatedAt" to FieldValue.serverTimestamp(),
                "payloadJson" to mergedJson
            )
            docRef.set(payload).await()
            Result.success(mergedJson)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "publishGlobalPsaPricingToCloud failed: ${e.message}")
            Result.failure(Exception("No se pudieron publicar los precios globales: ${e.localizedMessage}"))
        }
    }

    /**
     * Escucha actualizaciones de precios y cuotas publicadas por el Administrador.
     */
    fun observeGlobalPsaPricingSync(): Flow<Pair<String, Map<String, com.example.util.PsaCountryPricingSnapshot>>> =
        callbackFlow {
            val reg = firestore
                .collection(COLLECTION_CONFIG)
                .document(DOC_PSA_PRICING_SYNC)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "observeGlobalPsaPricingSync error: ${error.message}")
                        return@addSnapshotListener
                    }
                    val payloadJson = snapshot?.getString("payloadJson").orEmpty()
                    if (payloadJson.isNotBlank()) {
                        val parsed = com.example.util.PsaPricingCloudSyncHelper.parseAllCountriesPayload(payloadJson)
                        trySend(payloadJson to parsed)
                    }
                }
            awaitClose { reg.remove() }
        }

    /**
     * Consulta al servidor de Firestore (Source.SERVER) si existe una versión más reciente publicada.
     */
    suspend fun fetchAppVersionFromServer(): com.example.util.AppUpdateInfo? {
        if (auth.currentUser == null) return null
        return try {
            val doc = firestore
                .collection(COLLECTION_CONFIG)
                .document(DOC_APP_VERSION)
                .get(Source.SERVER)
                .await()
            if (!doc.exists()) return null
            val latestCode = (doc.getLong("latestVersionCode") ?: 0L).toInt()
            if (latestCode <= 0) return null
            val latestName = doc.getString("latestVersionName").orEmpty().trim()
            val normalizedUrl = com.example.util.AppUpdateChecker.normalizeDownloadUrl(
                doc.getString("apkDownloadUrl").orEmpty()
            )
            if (normalizedUrl.isBlank()) return null
            val notes = doc.getString("releaseNotes").orEmpty().trim()
            val forceUpdate = doc.getBoolean("forceUpdate") ?: false
            val updatedAtMs = doc.getTimestamp("updatedAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L
            com.example.util.AppUpdateInfo(
                latestVersionCode = latestCode,
                latestVersionName = latestName.ifBlank { latestCode.toString() },
                apkDownloadUrl = normalizedUrl,
                releaseNotes = notes,
                updatedAtMs = updatedAtMs,
                forceUpdate = forceUpdate
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "fetchAppVersionFromServer failed: ${e.message}")
            null
        }
    }

    /**
     * Permite únicamente al Superusuario publicar en el servidor la versión más reciente, enlace APK
     * y si es aviso opcional (`forceUpdate = false`) o actualización obligatoria global (`forceUpdate = true`).
     */
    suspend fun publishAppVersionToCloud(
        versionCode: Int,
        versionName: String,
        apkDownloadUrl: String,
        releaseNotes: String,
        forceUpdate: Boolean = false
    ): Result<com.example.util.AppUpdateInfo> {
        val fbUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Sesión Google requerida."))
        if (!fbUser.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)) {
            return Result.failure(IllegalStateException("Solo el Superusuario puede publicar actualizaciones de versión."))
        }
        if (versionCode !in 1..100000) {
            return Result.failure(IllegalArgumentException("El código de versión debe estar entre 1 y 100000."))
        }
        val cleanName = versionName.trim().ifBlank { versionCode.toString() }.take(40)
        val cleanUrl = com.example.util.AppUpdateChecker.normalizeDownloadUrl(apkDownloadUrl)
        if (cleanUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingresa un enlace HTTPS válido para descargar el APK (por ejemplo GitHub Releases o Google Drive)."))
        }
        val cleanNotes = releaseNotes.trim().take(1000)

        return try {
            val payload = hashMapOf<String, Any>(
                "configType" to DOC_APP_VERSION,
                "latestVersionCode" to versionCode,
                "latestVersionName" to cleanName,
                "apkDownloadUrl" to cleanUrl.take(500),
                "releaseNotes" to cleanNotes,
                "forceUpdate" to forceUpdate,
                "updatedByUid" to fbUser.uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore
                .collection(COLLECTION_CONFIG)
                .document(DOC_APP_VERSION)
                .set(payload)
                .await()
            Result.success(
                com.example.util.AppUpdateInfo(
                    latestVersionCode = versionCode,
                    latestVersionName = cleanName,
                    apkDownloadUrl = cleanUrl,
                    releaseNotes = cleanNotes,
                    updatedAtMs = System.currentTimeMillis(),
                    forceUpdate = forceUpdate
                )
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "publishAppVersionToCloud failed: ${e.message}")
            Result.failure(Exception("No se pudo publicar la actualización: ${e.localizedMessage}"))
        }
    }

    /**
     * Cuando un vendedor abre la versión requerida (o superior), limpia automáticamente el bloqueo
     * individual `updateRequired` en Firestore si la regla del vendedor activo lo permite.
     */
    suspend fun clearOwnUpdateLockIfUpgraded(user: UserAccount): Result<Unit> {
        val fbUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Sesión Google requerida."))
        val emailKey = normalizeEmail(user.email)
        if (emailKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Usuario sin correo configurado."))
        }
        return try {
            val payload = hashMapOf<String, Any>(
                "ownerUid" to fbUser.uid.take(128),
                "updateRequired" to false,
                "updateLockReason" to "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection(COLLECTION_USERS)
                .document(emailKey)
                .update(payload)
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("clearOwnUpdateLockIfUpgraded", e)
            Result.failure(e)
        }
    }

    /**
     * Actualización parcial y directa de bloqueo por actualización requerida en Firestore.
     * Evita enviar los 21 campos completos del usuario para no tropezar con campos legados.
     */
    suspend fun updateUserUpdateLockInCloud(
        emailRaw: String,
        updateRequired: Boolean,
        requiredVersionCode: Int,
        reason: String
    ): Result<Unit> {
        val currentFbUser = auth.currentUser
            ?: return Result.failure(
                IllegalStateException("Conecta tu cuenta Google de Superusuario arriba antes de exigir actualizaciones en Firestore.")
            )
        val emailKey = normalizeEmail(emailRaw)
        if (emailKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Este usuario no tiene un correo Google configurado para sincronizar el bloqueo."))
        }
        val payload = hashMapOf<String, Any>(
            "updateRequired" to updateRequired,
            "requiredVersionCode" to requiredVersionCode.coerceIn(0, 100000),
            "updateLockReason" to reason.trim().take(300),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(emailKey)
                .update(payload)
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError("updateUserUpdateLockInCloud", e)
            val msg = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permiso denegado en Firestore para ${currentFbUser.email ?: "tu cuenta"}. Reconecta tu Google de Superusuario."
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "El usuario $emailKey aún no existe en Firestore (crealo o autorízalo primero en la nube)."
                else ->
                    "No se pudo guardar el bloqueo de actualización: ${e.localizedMessage ?: e.code.name}"
            }
            Result.failure(Exception(msg))
        } catch (e: Exception) {
            handleFirestoreError("updateUserUpdateLockInCloud", e)
            Result.failure(Exception("No se pudo guardar el bloqueo de actualización: ${e.localizedMessage}"))
        }
    }

    private fun alignLabelsWithIds(ids: List<String>, labels: List<String>): List<String> {
        return ids.mapIndexed { index, _ ->
            labels.getOrNull(index)?.takeIf { it.isNotBlank() } ?: "Dispositivo ${index + 1}"
        }
    }

    private fun parseUserDocument(doc: DocumentSnapshot): UserAccount? {
        val data = doc.data ?: return null
        val email = (data["email"] as? String)?.trim()?.lowercase().orEmpty().ifBlank { doc.id.trim().lowercase() }
        val id = (data["id"] as? String).orEmpty().ifBlank { email }
        val username = (data["username"] as? String).orEmpty().ifBlank { email.substringBefore("@") }
        val rawRole = (data["role"] as? String).orEmpty().ifBlank { "OPERATOR" }
        val role = if (rawRole == "USER") "OPERATOR" else rawRole
        val daysGranted = (data["daysGranted"] as? Number)?.toInt() ?: 15
        val validFromMs = (data["validFrom"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
        val expiresAtMs = (data["expiresAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
        val isBlocked = (data["isBlocked"] as? Boolean) ?: false
        val canAccessLegacyModules = (data["canAccessLegacyModules"] as? Boolean) ?: false
        val notes = (data["notes"] as? String).orEmpty()
        val maxDevices = ((data["maxDevices"] as? Number)?.toInt() ?: 1).coerceIn(1, 5)
        val boundDeviceIds = (data["boundDeviceIds"] as? List<*>)
            ?.mapNotNull { it as? String }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val rawLabels = (data["boundDeviceLabels"] as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()
        val boundDeviceLabels = alignLabelsWithIds(boundDeviceIds, rawLabels)
        val ownerUid = (data["ownerUid"] as? String).orEmpty()
        val createdAtMs = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: validFromMs
        val lastOnlineVerifiedAtMs = doc.getTimestamp("updatedAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
            ?: createdAtMs
        val lastTamperAlertAtMs = (data["lastTamperAlertAtMs"] as? Number)?.toLong() ?: 0L
        val updateRequired = (data["updateRequired"] as? Boolean) ?: false
        val requiredVersionCode = ((data["requiredVersionCode"] as? Number)?.toInt() ?: 0).coerceIn(0, 100000)
        val updateLockReason = (data["updateLockReason"] as? String).orEmpty().take(300)
        val accessToken = (data["accessToken"] as? String).orEmpty().trim().take(64)
            .ifBlank { "GOOGLE-AUTH" }
        val parentEmail = normalizeEmail((data["parentEmail"] as? String).orEmpty()).take(120)
        val supervisorChain = (data["supervisorChainEmails"] as? List<*>)
            ?.mapNotNull { it as? String }
            ?.let { HierarchyTreeBuilder.normalizeChain(it) }
            .orEmpty()
        val hierarchyDepth = ((data["hierarchyDepth"] as? Number)?.toInt() ?: supervisorChain.size).coerceIn(0, 15)
        val approvalStatus = (data["approvalStatus"] as? String).orEmpty().ifBlank { ApprovalStatus.ACTIVE.name }
        val canManageSubUsers = (data["canManageSubUsers"] as? Boolean) ?: false
        val visibilityDepth = ((data["visibilityDepth"] as? Number)?.toInt() ?: 0).coerceIn(0, 10)
        val requestedByEmail = normalizeEmail((data["requestedByEmail"] as? String).orEmpty()).take(120)
        val tutorialCompleted = (data["tutorialCompleted"] as? Boolean) ?: false

        return UserAccount(
            id = id,
            username = username,
            token = accessToken,
            role = role,
            daysGranted = daysGranted,
            validFromMs = validFromMs,
            expiresAtMs = expiresAtMs,
            isBlocked = isBlocked,
            notes = notes,
            createdAt = createdAtMs,
            canAccessLegacyModules = canAccessLegacyModules,
            email = email,
            maxDevices = maxDevices,
            boundDeviceIdsSerialized = boundDeviceIds.joinToString("|"),
            boundDeviceLabelsSerialized = boundDeviceLabels.joinToString("|"),
            ownerUid = ownerUid,
            lastOnlineVerifiedAtMs = lastOnlineVerifiedAtMs,
            lastTamperAlertAtMs = lastTamperAlertAtMs,
            updateRequired = updateRequired,
            requiredVersionCode = requiredVersionCode,
            updateLockReason = updateLockReason,
            parentEmail = parentEmail,
            supervisorChainSerialized = HierarchyTreeBuilder.serializeChain(supervisorChain),
            hierarchyDepth = hierarchyDepth,
            approvalStatus = approvalStatus,
            canManageSubUsers = canManageSubUsers,
            visibilityDepth = visibilityDepth,
            requestedByEmail = requestedByEmail,
            tutorialCompleted = tutorialCompleted
        )
    }

    private fun userToCreateMap(user: UserAccount): Map<String, Any> {
        val ids = user.boundDeviceIds.take(5)
        val labels = alignLabelsWithIds(ids, user.boundDeviceLabels)
        val firestoreRole = if (user.role == "SUPERUSER") "SUPERUSER" else "OPERATOR"
        val chain = HierarchyTreeBuilder.normalizeChain(user.supervisorChainEmails)
        return hashMapOf(
            "id" to user.id,
            "email" to normalizeEmail(user.email),
            "username" to user.username.trim().ifBlank { "Operador" }.take(80),
            "role" to firestoreRole,
            "daysGranted" to user.daysGranted.coerceIn(0, 3650),
            "validFrom" to FieldValue.serverTimestamp(),
            "expiresAt" to Timestamp(Date(user.expiresAtMs)),
            "isBlocked" to user.isBlocked,
            "canAccessLegacyModules" to user.canAccessLegacyModules,
            "notes" to user.notes.take(300),
            "maxDevices" to user.maxDevices.coerceIn(1, 5),
            "boundDeviceIds" to ids,
            "boundDeviceLabels" to labels,
            "ownerUid" to user.ownerUid.take(128),
            "updateRequired" to user.updateRequired,
            "requiredVersionCode" to user.requiredVersionCode.coerceIn(0, 100000),
            "updateLockReason" to user.updateLockReason.take(300),
            "accessToken" to user.token.trim().ifBlank { "GOOGLE-AUTH" }.take(64),
            "parentEmail" to normalizeEmail(user.parentEmail).take(120),
            "supervisorChainEmails" to chain,
            "hierarchyDepth" to user.hierarchyDepth.coerceIn(0, 15),
            "approvalStatus" to user.approvalStatus.ifBlank { ApprovalStatus.ACTIVE.name },
            "canManageSubUsers" to user.canManageSubUsers,
            "visibilityDepth" to user.visibilityDepth.coerceIn(0, 10),
            "requestedByEmail" to normalizeEmail(user.requestedByEmail).take(120),
            "tutorialCompleted" to user.tutorialCompleted,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun userToUpdateMap(user: UserAccount): Map<String, Any> {
        val ids = user.boundDeviceIds.take(5)
        val labels = alignLabelsWithIds(ids, user.boundDeviceLabels)
        val chain = HierarchyTreeBuilder.normalizeChain(user.supervisorChainEmails)
        return hashMapOf(
            "username" to user.username.trim().ifBlank { "Operador" }.take(80),
            "daysGranted" to user.daysGranted.coerceIn(0, 3650),
            "expiresAt" to Timestamp(Date(user.expiresAtMs)),
            "isBlocked" to user.isBlocked,
            "canAccessLegacyModules" to user.canAccessLegacyModules,
            "notes" to user.notes.take(300),
            "maxDevices" to user.maxDevices.coerceIn(ids.size.coerceAtLeast(1), 5),
            "boundDeviceIds" to ids,
            "boundDeviceLabels" to labels,
            "ownerUid" to user.ownerUid.take(128),
            "updateRequired" to user.updateRequired,
            "requiredVersionCode" to user.requiredVersionCode.coerceIn(0, 100000),
            "updateLockReason" to user.updateLockReason.take(300),
            "parentEmail" to normalizeEmail(user.parentEmail).take(120),
            "supervisorChainEmails" to chain,
            "hierarchyDepth" to user.hierarchyDepth.coerceIn(0, 15),
            "approvalStatus" to user.approvalStatus.ifBlank { ApprovalStatus.ACTIVE.name },
            "canManageSubUsers" to user.canManageSubUsers,
            "visibilityDepth" to user.visibilityDepth.coerceIn(0, 10),
            "requestedByEmail" to normalizeEmail(user.requestedByEmail).take(120),
            "tutorialCompleted" to user.tutorialCompleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    suspend fun markTutorialCompleted(emailRaw: String, completed: Boolean = true): Result<Unit> {
        auth.currentUser
            ?: return Result.failure(IllegalStateException("Sesión Google requerida."))
        val emailKey = normalizeEmail(emailRaw)
        if (emailKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Usuario sin correo configurado."))
        }
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(emailKey)
                .update(
                    mapOf(
                        "tutorialCompleted" to completed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("markTutorialCompleted", e)
            Result.failure(e)
        }
    }

    // =====================================================================
    // PER-SELLER PRIVATE WORKSPACE SYNC (/seller_workspaces/{ownerUid}/...)
    // =====================================================================

    private fun contactDocRef(ownerUid: String, contactId: String) = firestore
        .collection(COLLECTION_WORKSPACES)
        .document(ownerUid)
        .collection(SUBCOLLECTION_CONTACTS)
        .document(contactId)

    private fun visitDocRef(ownerUid: String, visitId: String) = firestore
        .collection(COLLECTION_WORKSPACES)
        .document(ownerUid)
        .collection(SUBCOLLECTION_VISITS)
        .document(visitId)

    private fun postSaleDocRef(ownerUid: String, recordId: String) = firestore
        .collection(COLLECTION_WORKSPACES)
        .document(ownerUid)
        .collection(SUBCOLLECTION_POST_SALE)
        .document(recordId)

    suspend fun upsertSellerContact(ownerUid: String, lead: PsaContactLead): Result<Unit> {
        if (auth.currentUser?.uid != ownerUid || ownerUid.isBlank()) {
            return Result.failure(IllegalStateException("Sesión Google requerida para sincronizar contactos."))
        }
        val docId = lead.id.trim().ifBlank { UUID.randomUUID().toString() }.take(80)
        val payload = hashMapOf<String, Any>(
            "id" to docId,
            "ownerUid" to ownerUid.take(128),
            "name" to lead.name.trim().ifBlank { "Contacto" }.take(120),
            "phone" to lead.phone.trim().take(40),
            "address" to lead.address.trim().take(200),
            "neighborhood" to lead.neighborhood.trim().take(120),
            "notes" to lead.notes.trim().take(500),
            "source" to lead.source.trim().take(60),
            "callAttemptsCount" to lead.callAttemptsCount.coerceIn(0, 10000),
            "messageAttemptsCount" to lead.messageAttemptsCount.coerceIn(0, 10000),
            "lastContactDateMs" to lead.lastContactDateMs.coerceAtLeast(0L),
            "lastContactChannel" to lead.lastContactChannel.trim().take(20),
            "status" to lead.status.name,
            "rescheduleDate" to lead.rescheduleDate.trim().take(20),
            "rescheduleNote" to lead.rescheduleNote.trim().take(300),
            "createdAtMs" to lead.createdAtMs.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            contactDocRef(ownerUid, docId).set(payload).await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("upsertSellerContact", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSellerContact(ownerUid: String, contactId: String): Result<Unit> {
        if (auth.currentUser?.uid != ownerUid || ownerUid.isBlank() || contactId.isBlank()) {
            return Result.failure(IllegalStateException("Sesión Google requerida."))
        }
        return try {
            contactDocRef(ownerUid, contactId).delete().await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("deleteSellerContact", e)
            Result.failure(e)
        }
    }

    fun observeSellerContacts(ownerUid: String): Flow<SellerWorkspaceSnapshot<PsaContactLead>> = callbackFlow {
        if (ownerUid.isBlank()) {
            close()
            return@callbackFlow
        }
        val reg = firestore
            .collection(COLLECTION_WORKSPACES)
            .document(ownerUid)
            .collection(SUBCOLLECTION_CONTACTS)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
                if (error != null) {
                    handleFirestoreError("observeSellerContacts", error)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { parseSellerContact(it) }.orEmpty()
                trySend(
                    SellerWorkspaceSnapshot(
                        items = list,
                        isFromCache = snap?.metadata?.isFromCache ?: false
                    )
                )
            }
        awaitClose { reg.remove() }
    }

    suspend fun upsertSellerVisit(ownerUid: String, visit: PsaHomeVisit): Result<Unit> {
        if (auth.currentUser?.uid != ownerUid || ownerUid.isBlank()) {
            return Result.failure(IllegalStateException("Sesión Google requerida para sincronizar visitas."))
        }
        val docId = visit.id.trim().ifBlank { UUID.randomUUID().toString() }.take(80)
        val payload = hashMapOf<String, Any>(
            "id" to docId,
            "ownerUid" to ownerUid.take(128),
            "customerName" to visit.customerName.trim().ifBlank { "Cliente PSA" }.take(120),
            "phone" to visit.phone.trim().take(40),
            "address" to visit.address.trim().take(200),
            "neighborhood" to visit.neighborhood.trim().take(120),
            "scheduledDate" to visit.scheduledDate.trim().take(20),
            "scheduledTime" to visit.scheduledTime.trim().take(16),
            "modelInterest" to visit.modelInterest.trim().take(120),
            "status" to visit.status.name,
            "chlorinePpm" to visit.chlorinePpm.toDouble().coerceIn(0.0, 10.0),
            "waterSource" to visit.waterSource.trim().take(40),
            "hardnessPpm" to visit.hardnessPpm.coerceIn(0, 2000),
            "realLitersPerDay" to visit.realLitersPerDay.coerceIn(0.0, 500.0),
            "cartSummary" to visit.cartSummary.trim().take(500),
            "soldCartSummary" to visit.soldCartSummary.trim().take(500),
            "testPhotoPath" to visit.testPhotoPath.trim().take(400),
            "diagnosticTimestampMs" to visit.diagnosticTimestampMs.coerceAtLeast(0L),
            "bottlesPerWeek" to visit.bottlesPerWeek.coerceIn(0, 500),
            "quotedPlan" to visit.quotedPlan.trim().take(400),
            "notes" to visit.notes.trim().take(600),
            "createdAtMs" to visit.createdAtMs.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            visitDocRef(ownerUid, docId).set(payload).await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("upsertSellerVisit", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSellerVisit(ownerUid: String, visitId: String): Result<Unit> {
        if (auth.currentUser?.uid != ownerUid || ownerUid.isBlank() || visitId.isBlank()) {
            return Result.failure(IllegalStateException("Sesión Google requerida."))
        }
        return try {
            visitDocRef(ownerUid, visitId).delete().await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("deleteSellerVisit", e)
            Result.failure(e)
        }
    }

    fun observeSellerVisits(ownerUid: String): Flow<SellerWorkspaceSnapshot<PsaHomeVisit>> = callbackFlow {
        if (ownerUid.isBlank()) {
            close()
            return@callbackFlow
        }
        val reg = firestore
            .collection(COLLECTION_WORKSPACES)
            .document(ownerUid)
            .collection(SUBCOLLECTION_VISITS)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
                if (error != null) {
                    handleFirestoreError("observeSellerVisits", error)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { parseSellerVisit(it) }.orEmpty()
                trySend(
                    SellerWorkspaceSnapshot(
                        items = list,
                        isFromCache = snap?.metadata?.isFromCache ?: false
                    )
                )
            }
        awaitClose { reg.remove() }
    }

    suspend fun upsertPostSaleRecord(ownerUid: String, record: PsaPostSaleInstallation): Result<Unit> {
        if (auth.currentUser?.uid != ownerUid || ownerUid.isBlank()) {
            return Result.failure(IllegalStateException("Sesión Google requerida para sincronizar posventa."))
        }
        val docId = record.id.trim().ifBlank { UUID.randomUUID().toString() }.take(80)
        val safeCycle = when (record.replacementCycleMonths) {
            6, 12, 24, 36 -> record.replacementCycleMonths
            else -> 12
        }
        val payload = hashMapOf<String, Any>(
            "id" to docId,
            "ownerUid" to ownerUid.take(128),
            "contactId" to record.contactId.trim().take(80),
            "visitId" to record.visitId.trim().take(80),
            "customerName" to record.customerName.trim().ifBlank { "Cliente PSA" }.take(120),
            "customerPhone" to record.customerPhone.trim().take(40),
            "purifierId" to record.purifierId.trim().ifBlank { "senior4" }.take(80),
            "purifierName" to record.purifierName.trim().ifBlank { "PSA Senior 4" }.take(160),
            "installedAtMs" to record.installedAtMs.coerceAtLeast(0L),
            "replacementCycleMonths" to safeCycle,
            "nextReplacementAtMs" to record.nextReplacementAtMs.coerceAtLeast(0L),
            "lastNotifiedDayKey" to record.lastNotifiedDayKey.trim().take(20),
            "status" to record.status.name,
            "notes" to record.notes.trim().take(400),
            "createdAtMs" to record.createdAtMs.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            postSaleDocRef(ownerUid, docId).set(payload).await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("upsertPostSaleRecord", e)
            Result.failure(e)
        }
    }

    suspend fun deletePostSaleRecord(ownerUid: String, recordId: String): Result<Unit> {
        if (auth.currentUser?.uid != ownerUid || ownerUid.isBlank() || recordId.isBlank()) {
            return Result.failure(IllegalStateException("Sesión Google requerida."))
        }
        return try {
            postSaleDocRef(ownerUid, recordId).delete().await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("deletePostSaleRecord", e)
            Result.failure(e)
        }
    }

    fun observeSellerPostSale(ownerUid: String): Flow<SellerWorkspaceSnapshot<PsaPostSaleInstallation>> = callbackFlow {
        if (ownerUid.isBlank()) {
            close()
            return@callbackFlow
        }
        val reg = firestore
            .collection(COLLECTION_WORKSPACES)
            .document(ownerUid)
            .collection(SUBCOLLECTION_POST_SALE)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
                if (error != null) {
                    handleFirestoreError("observeSellerPostSale", error)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { parsePostSaleRecord(it) }.orEmpty()
                trySend(
                    SellerWorkspaceSnapshot(
                        items = list,
                        isFromCache = snap?.metadata?.isFromCache ?: false
                    )
                )
            }
        awaitClose { reg.remove() }
    }

    // =====================================================================
    // SELLER AGGREGATED PERFORMANCE STATS (/seller_stats/{ownerUid})
    // =====================================================================

    suspend fun upsertSellerStats(stats: SellerPerformanceStats): Result<Unit> {
        val currentUid = auth.currentUser?.uid
        if (currentUid.isNullOrBlank() || stats.ownerUid != currentUid) {
            return Result.failure(IllegalStateException("Sesión Google requerida para publicar estadísticas."))
        }
        val cleanEmail = normalizeEmail(stats.sellerEmail).let {
            if (it.length >= 5) it.take(120) else "vendedor@psa.local"
        }
        val payload = hashMapOf<String, Any>(
            "ownerUid" to stats.ownerUid.take(128),
            "sellerName" to stats.sellerName.trim().ifBlank { "Vendedor PSA" }.take(80),
            "sellerEmail" to cleanEmail,
            "totalContacts" to stats.totalContacts.coerceIn(0, 100000),
            "totalVisits" to stats.totalVisits.coerceIn(0, 100000),
            "completedDemos" to stats.completedDemos.coerceIn(0, 100000),
            "closedSales" to stats.closedSales.coerceIn(0, 100000),
            "pendingReplacements" to stats.pendingReplacements.coerceIn(0, 100000),
            "messagesSuccess" to stats.messagesSuccess.coerceIn(0, 1000000),
            "messagesFailed" to stats.messagesFailed.coerceIn(0, 1000000),
            "messagesTotal" to stats.messagesTotal.coerceIn(0, 1000000),
            "callsAttempted" to stats.callsAttempted.coerceIn(0, 1000000),
            "callsConnected" to stats.callsConnected.coerceIn(0, 1000000),
            "lastMessageAtMs" to stats.lastMessageAtMs.coerceAtLeast(0L),
            "lastActivityAtMs" to stats.lastActivityAtMs.coerceAtLeast(0L),
            "lastActivityLabel" to stats.lastActivityLabel.trim().take(160),
            "recentActivityJson" to stats.recentActivityJson.ifBlank { "[]" }.take(9500),
            "parentEmail" to normalizeEmail(stats.parentEmail).take(120),
            "supervisorChainEmails" to HierarchyTreeBuilder.normalizeChain(stats.supervisorChainEmails),
            "hierarchyDepth" to stats.hierarchyDepth.coerceIn(0, 15),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            firestore.collection(COLLECTION_SELLER_STATS)
                .document(stats.ownerUid)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "upsertSellerStats failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun observeAllSellerStats(): Flow<List<SellerPerformanceStats>> = callbackFlow {
        val reg = firestore
            .collection(COLLECTION_SELLER_STATS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "observeAllSellerStats error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { parseSellerStats(it) }.orEmpty()
                    .sortedByDescending { it.lastActivityAtMs.coerceAtLeast(it.updatedAtMs) }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeSubordinateSellerStats(managerEmailRaw: String): Flow<List<SellerPerformanceStats>> = callbackFlow {
        val managerEmail = normalizeEmail(managerEmailRaw)
        if (managerEmail.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = firestore
            .collection(COLLECTION_SELLER_STATS)
            .whereArrayContains("supervisorChainEmails", managerEmail)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "observeSubordinateSellerStats error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { parseSellerStats(it) }.orEmpty()
                    .sortedByDescending { it.lastActivityAtMs.coerceAtLeast(it.updatedAtMs) }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeSubordinateUsers(managerEmailRaw: String): Flow<List<UserAccount>> = callbackFlow {
        val managerEmail = normalizeEmail(managerEmailRaw)
        if (managerEmail.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = firestore
            .collection(COLLECTION_USERS)
            .whereArrayContains("supervisorChainEmails", managerEmail)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "observeSubordinateUsers error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { parseUserDocument(it) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun fetchAllSellerStatsOnce(): List<SellerPerformanceStats> {
        return try {
            val snap = firestore.collection(COLLECTION_SELLER_STATS).get().await()
            snap.documents.mapNotNull { parseSellerStats(it) }
                .sortedByDescending { it.lastActivityAtMs.coerceAtLeast(it.updatedAtMs) }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "fetchAllSellerStatsOnce failed: ${e.message}")
            emptyList()
        }
    }

    private fun parseSellerContact(doc: DocumentSnapshot): PsaContactLead? {
        val data = doc.data ?: return null
        val id = (data["id"] as? String).orEmpty().ifBlank { doc.id }
        val statusRaw = (data["status"] as? String).orEmpty()
        val parsedStatus = try {
            PsaLeadStatus.valueOf(statusRaw)
        } catch (_: Exception) {
            PsaLeadStatus.PENDING
        }
        return PsaContactLead(
            id = id,
            name = (data["name"] as? String).orEmpty().ifBlank { "Contacto" },
            phone = (data["phone"] as? String).orEmpty(),
            address = (data["address"] as? String).orEmpty(),
            neighborhood = (data["neighborhood"] as? String).orEmpty(),
            notes = (data["notes"] as? String).orEmpty(),
            source = (data["source"] as? String).orEmpty().ifBlank { "Referido" },
            callAttemptsCount = (data["callAttemptsCount"] as? Number)?.toInt() ?: 0,
            messageAttemptsCount = (data["messageAttemptsCount"] as? Number)?.toInt() ?: 0,
            lastContactDateMs = (data["lastContactDateMs"] as? Number)?.toLong() ?: 0L,
            lastContactChannel = (data["lastContactChannel"] as? String).orEmpty(),
            status = parsedStatus,
            rescheduleDate = (data["rescheduleDate"] as? String).orEmpty(),
            rescheduleNote = (data["rescheduleNote"] as? String).orEmpty(),
            createdAtMs = (data["createdAtMs"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun parseSellerVisit(doc: DocumentSnapshot): PsaHomeVisit? {
        val data = doc.data ?: return null
        val id = (data["id"] as? String).orEmpty().ifBlank { doc.id }
        val statusRaw = (data["status"] as? String).orEmpty()
        val parsedStatus = try {
            PsaVisitStatus.valueOf(statusRaw)
        } catch (_: Exception) {
            PsaVisitStatus.AGENDADA
        }
        val updatedAtMs = doc.getTimestamp("updatedAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
            ?: System.currentTimeMillis()
        return PsaHomeVisit(
            id = id,
            customerName = (data["customerName"] as? String).orEmpty().ifBlank { "Cliente PSA" },
            phone = (data["phone"] as? String).orEmpty(),
            address = (data["address"] as? String).orEmpty(),
            neighborhood = (data["neighborhood"] as? String).orEmpty(),
            scheduledDate = (data["scheduledDate"] as? String).orEmpty(),
            scheduledTime = (data["scheduledTime"] as? String).orEmpty(),
            modelInterest = (data["modelInterest"] as? String).orEmpty().ifBlank { "PSA Senior 4" },
            status = parsedStatus,
            chlorinePpm = (data["chlorinePpm"] as? Number)?.toFloat() ?: 0f,
            bottlesPerWeek = (data["bottlesPerWeek"] as? Number)?.toInt() ?: 3,
            quotedPlan = (data["quotedPlan"] as? String).orEmpty(),
            notes = (data["notes"] as? String).orEmpty(),
            createdAtMs = (data["createdAtMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            ownerUid = (data["ownerUid"] as? String).orEmpty(),
            waterSource = (data["waterSource"] as? String).orEmpty().ifBlank { "AGUA_CORRIENTE" },
            hardnessPpm = (data["hardnessPpm"] as? Number)?.toInt() ?: 0,
            realLitersPerDay = (data["realLitersPerDay"] as? Number)?.toDouble() ?: 0.0,
            cartSummary = (data["cartSummary"] as? String).orEmpty(),
            soldCartSummary = (data["soldCartSummary"] as? String).orEmpty(),
            testPhotoPath = (data["testPhotoPath"] as? String).orEmpty(),
            diagnosticTimestampMs = (data["diagnosticTimestampMs"] as? Number)?.toLong() ?: 0L,
            updatedAtMs = updatedAtMs
        )
    }

    private fun parsePostSaleRecord(doc: DocumentSnapshot): PsaPostSaleInstallation? {
        val data = doc.data ?: return null
        val id = (data["id"] as? String).orEmpty().ifBlank { doc.id }
        val statusRaw = (data["status"] as? String).orEmpty()
        val parsedStatus = try {
            PsaPostSaleStatus.valueOf(statusRaw)
        } catch (_: Exception) {
            PsaPostSaleStatus.ACTIVO
        }
        val updatedAtMs = doc.getTimestamp("updatedAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
            ?: System.currentTimeMillis()
        return PsaPostSaleInstallation(
            id = id,
            ownerUid = (data["ownerUid"] as? String).orEmpty(),
            contactId = (data["contactId"] as? String).orEmpty(),
            visitId = (data["visitId"] as? String).orEmpty(),
            customerName = (data["customerName"] as? String).orEmpty().ifBlank { "Cliente PSA" },
            customerPhone = (data["customerPhone"] as? String).orEmpty(),
            purifierId = (data["purifierId"] as? String).orEmpty().ifBlank { "senior4" },
            purifierName = (data["purifierName"] as? String).orEmpty().ifBlank { "PSA Senior 4" },
            installedAtMs = (data["installedAtMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            replacementCycleMonths = (data["replacementCycleMonths"] as? Number)?.toInt() ?: 12,
            nextReplacementAtMs = (data["nextReplacementAtMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            lastNotifiedDayKey = (data["lastNotifiedDayKey"] as? String).orEmpty(),
            status = parsedStatus,
            notes = (data["notes"] as? String).orEmpty(),
            createdAtMs = (data["createdAtMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAtMs = updatedAtMs
        )
    }

    private fun parseSellerStats(doc: DocumentSnapshot): SellerPerformanceStats? {
        val data = doc.data ?: return null
        val ownerUid = (data["ownerUid"] as? String).orEmpty().ifBlank { doc.id }
        val updatedAtMs = doc.getTimestamp("updatedAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L
        val messagesSuccess = (data["messagesSuccess"] as? Number)?.toInt() ?: 0
        val messagesFailed = (data["messagesFailed"] as? Number)?.toInt() ?: 0
        val messagesTotal = ((data["messagesTotal"] as? Number)?.toInt() ?: (messagesSuccess + messagesFailed))
            .coerceAtLeast(messagesSuccess + messagesFailed)
        val chain = (data["supervisorChainEmails"] as? List<*>)
            ?.mapNotNull { it as? String }
            ?.let { HierarchyTreeBuilder.normalizeChain(it) }
            .orEmpty()
        return SellerPerformanceStats(
            ownerUid = ownerUid,
            sellerName = (data["sellerName"] as? String).orEmpty().ifBlank { "Vendedor PSA" },
            sellerEmail = (data["sellerEmail"] as? String).orEmpty(),
            totalContacts = (data["totalContacts"] as? Number)?.toInt() ?: 0,
            totalVisits = (data["totalVisits"] as? Number)?.toInt() ?: 0,
            completedDemos = (data["completedDemos"] as? Number)?.toInt() ?: 0,
            closedSales = (data["closedSales"] as? Number)?.toInt() ?: 0,
            pendingReplacements = (data["pendingReplacements"] as? Number)?.toInt() ?: 0,
            messagesSuccess = messagesSuccess,
            messagesFailed = messagesFailed,
            messagesTotal = messagesTotal,
            callsAttempted = (data["callsAttempted"] as? Number)?.toInt() ?: 0,
            callsConnected = (data["callsConnected"] as? Number)?.toInt() ?: 0,
            lastMessageAtMs = (data["lastMessageAtMs"] as? Number)?.toLong() ?: 0L,
            lastActivityAtMs = (data["lastActivityAtMs"] as? Number)?.toLong() ?: updatedAtMs,
            lastActivityLabel = (data["lastActivityLabel"] as? String).orEmpty(),
            recentActivityJson = (data["recentActivityJson"] as? String).orEmpty().ifBlank { "[]" },
            parentEmail = normalizeEmail((data["parentEmail"] as? String).orEmpty()),
            supervisorChainEmails = chain,
            hierarchyDepth = ((data["hierarchyDepth"] as? Number)?.toInt() ?: chain.size).coerceIn(0, 15),
            updatedAtMs = updatedAtMs
        )
    }

    // =====================================================================
    // REAL-TIME FIREBASE ERROR DIAGNOSTICS (/AppErrors/{errorId})
    // =====================================================================

    private fun handleFirestoreError(operation: String, error: Throwable?) {
        if (error == null) return
        val code = (error as? FirebaseFirestoreException)?.code?.name ?: "UNKNOWN"
        Log.e(TAG, "Firestore operation '$operation' failed [$code]: ${error.message}")
    }

    suspend fun flushPendingAppErrors(reporterNameOverride: String = ""): Int {
        return try {
            com.example.util.AppErrorLogger.flushPendingErrors(
                context = appContext,
                firestore = firestore,
                auth = auth,
                reporterNameOverride = reporterNameOverride
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("flushPendingAppErrors", e)
            0
        }
    }

    fun observeAppErrors(): Flow<List<com.example.util.AppErrorReport>> = callbackFlow {
        val reg: ListenerRegistration = firestore
            .collection(COLLECTION_APP_ERRORS)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    handleFirestoreError("observeAppErrors", error)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { parseAppErrorReport(it) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun fetchAppErrorsOnce(): Result<List<com.example.util.AppErrorReport>> {
        return try {
            val snap = firestore
                .collection(COLLECTION_APP_ERRORS)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get(Source.SERVER)
                .await()
            val list = snap.documents.mapNotNull { parseAppErrorReport(it) }
            Result.success(list)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("fetchAppErrorsOnce", e)
            Result.failure(Exception("No se pudieron cargar los errores en la nube: ${e.localizedMessage}"))
        }
    }

    suspend fun markAppErrorStatus(errorId: String, status: String): Result<Unit> {
        val currentFbUser = auth.currentUser
            ?: return Result.failure(
                IllegalStateException("Conecta tu cuenta Google de Superusuario arriba para administrar AppErrors.")
            )
        val safeStatus = if (status == "RESOLVED") "RESOLVED" else "OPEN"
        if (errorId.isBlank()) return Result.failure(IllegalArgumentException("ID de error inválido."))
        return try {
            firestore.collection(COLLECTION_APP_ERRORS)
                .document(errorId)
                .update(
                    mapOf(
                        "status" to safeStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError("markAppErrorStatus", e)
            val msg = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permiso denegado en AppErrors para ${currentFbUser.email ?: "tu cuenta"}. Reconecta Google Superusuario o despliega reglas."
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "Este registro ya no existe en Firestore (actualizando lista)."
                else ->
                    "No se pudo actualizar el estado del error: ${e.localizedMessage ?: e.code.name}"
            }
            Result.failure(Exception(msg))
        } catch (e: Exception) {
            handleFirestoreError("markAppErrorStatus", e)
            Result.failure(Exception("No se pudo actualizar el estado del error: ${e.localizedMessage}"))
        }
    }

    suspend fun deleteAppError(errorId: String): Result<Unit> {
        val currentFbUser = auth.currentUser
            ?: return Result.failure(
                IllegalStateException("Conecta tu cuenta Google de Superusuario arriba para eliminar de AppErrors.")
            )
        if (errorId.isBlank()) return Result.failure(IllegalArgumentException("ID de error inválido."))
        return try {
            firestore.collection(COLLECTION_APP_ERRORS)
                .document(errorId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError("deleteAppError", e)
            val msg = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permiso denegado al eliminar en AppErrors para ${currentFbUser.email ?: "tu cuenta"}. Reconecta Google Superusuario."
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "El registro ya había sido eliminado en Firestore."
                else ->
                    "No se pudo eliminar el registro de error: ${e.localizedMessage ?: e.code.name}"
            }
            Result.failure(Exception(msg))
        } catch (e: Exception) {
            handleFirestoreError("deleteAppError", e)
            Result.failure(Exception("No se pudo eliminar el registro de error: ${e.localizedMessage}"))
        }
    }

    private fun parseAppErrorReport(doc: DocumentSnapshot): com.example.util.AppErrorReport? {
        val data = doc.data ?: return null
        val id = doc.id
        val occurredAtMs = (data["occurredAtMs"] as? Number)?.toLong() ?: 0L
        val createdAtMs = doc.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
            ?: occurredAtMs
        val updatedAtMs = doc.getTimestamp("updatedAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
            ?: createdAtMs
        return com.example.util.AppErrorReport(
            id = id,
            ownerUid = (data["ownerUid"] as? String).orEmpty(),
            reporterEmail = (data["reporterEmail"] as? String).orEmpty(),
            reporterName = (data["reporterName"] as? String).orEmpty().ifBlank { "Vendedor" },
            stage = (data["stage"] as? String).orEmpty().ifBlank { "ON_CREATE" },
            errorMessage = (data["errorMessage"] as? String).orEmpty(),
            stackTraceExcerpt = (data["stackTraceExcerpt"] as? String).orEmpty(),
            logcatExcerpt = (data["logcatExcerpt"] as? String).orEmpty(),
            deviceModel = (data["deviceModel"] as? String).orEmpty(),
            androidSdk = (data["androidSdk"] as? Number)?.toInt() ?: Build.VERSION.SDK_INT,
            appVersionName = (data["appVersionName"] as? String).orEmpty(),
            appVersionCode = (data["appVersionCode"] as? Number)?.toInt() ?: 1,
            status = (data["status"] as? String).orEmpty().ifBlank { "OPEN" },
            occurredAtMs = occurredAtMs,
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs
        )
    }

    // =====================================================================
    // SUPERUSER ACCESS TOKENS & EXPIRATION LICENSING (/access_tokens/{code})
    // =====================================================================

    fun generateAccessTokenCode(daysGranted: Int): String {
        val safeDays = daysGranted.coerceIn(1, 3650)
        val bytes = ByteArray(4)
        SecureRandom().nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02X".format(it) }
        return "PSA-${safeDays}D-$hex"
    }

    fun observeAccessTokens(): Flow<List<CloudAccessToken>> = callbackFlow {
        val reg: ListenerRegistration = firestore
            .collection(COLLECTION_ACCESS_TOKENS)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    handleFirestoreError("observeAccessTokens", error)
                    return@addSnapshotListener
                }
                val list = snapshots?.documents
                    ?.mapNotNull { parseAccessToken(it) }
                    .orEmpty()
                    .sortedByDescending { it.createdAtMs }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun fetchAccessTokensOnce(): Result<List<CloudAccessToken>> {
        return try {
            val snap = firestore.collection(COLLECTION_ACCESS_TOKENS)
                .get(Source.SERVER)
                .await()
            val list = snap.documents
                .mapNotNull { parseAccessToken(it) }
                .sortedByDescending { it.createdAtMs }
            Result.success(list)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("fetchAccessTokensOnce", e)
            Result.failure(Exception("No se pudieron cargar los tokens: ${e.localizedMessage}"))
        }
    }

    suspend fun createAccessTokenInCloud(
        assignedUsername: String,
        targetEmailRaw: String,
        daysGranted: Int,
        maxDevices: Int,
        notes: String
    ): Result<CloudAccessToken> {
        val adminUser = auth.currentUser
            ?: return Result.failure(
                IllegalStateException("Conecta tu cuenta Google de Superusuario antes de generar tokens.")
            )
        val safeName = assignedUsername.trim().take(80)
        if (safeName.length < 2) {
            return Result.failure(IllegalArgumentException("Ingresa un nombre o alias de al menos 2 caracteres."))
        }
        val cleanTargetEmail = normalizeEmail(targetEmailRaw)
        if (cleanTargetEmail.isNotBlank() && !cleanTargetEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Si asignas un correo específico, debe ser un email válido."))
        }
        val safeDays = daysGranted.coerceIn(1, 3650)
        val safeDevices = maxDevices.coerceIn(1, 5)
        val safeNotes = notes.trim().take(300)
        val tokenCode = generateAccessTokenCode(safeDays)
        val nowMs = System.currentTimeMillis()

        val payload = hashMapOf<String, Any>(
            "tokenCode" to tokenCode,
            "assignedUsername" to safeName,
            "targetEmail" to cleanTargetEmail,
            "daysGranted" to safeDays,
            "maxDevices" to safeDevices,
            "notes" to safeNotes,
            "status" to CloudAccessTokenStatus.AVAILABLE.name,
            "createdByUid" to adminUser.uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            firestore.collection(COLLECTION_ACCESS_TOKENS)
                .document(tokenCode)
                .set(payload)
                .await()
            Result.success(
                CloudAccessToken(
                    tokenCode = tokenCode,
                    assignedUsername = safeName,
                    targetEmail = cleanTargetEmail,
                    daysGranted = safeDays,
                    maxDevices = safeDevices,
                    notes = safeNotes,
                    status = CloudAccessTokenStatus.AVAILABLE.name,
                    createdByUid = adminUser.uid,
                    createdAtMs = nowMs,
                    updatedAtMs = nowMs
                )
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError("createAccessTokenInCloud", e)
            val detail = if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "Permiso denegado: solo el Superusuario autorizado puede emitir tokens."
            } else {
                e.localizedMessage ?: e.code.name
            }
            Result.failure(Exception(detail))
        } catch (e: Exception) {
            handleFirestoreError("createAccessTokenInCloud", e)
            Result.failure(Exception("No se pudo generar el token en Firestore: ${e.localizedMessage}"))
        }
    }

    suspend fun revokeAccessTokenInCloud(tokenCodeRaw: String): Result<Unit> {
        val tokenCode = tokenCodeRaw.trim().uppercase()
        if (tokenCode.length < 6) {
            return Result.failure(IllegalArgumentException("Código de token inválido."))
        }
        return try {
            firestore.collection(COLLECTION_ACCESS_TOKENS)
                .document(tokenCode)
                .update(
                    mapOf(
                        "status" to CloudAccessTokenStatus.REVOKED.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("revokeAccessTokenInCloud", e)
            Result.failure(Exception("No se pudo revocar el token: ${e.localizedMessage}"))
        }
    }

    suspend fun deleteAccessTokenInCloud(tokenCodeRaw: String): Result<Unit> {
        val tokenCode = tokenCodeRaw.trim().uppercase()
        if (tokenCode.length < 6) {
            return Result.failure(IllegalArgumentException("Código de token inválido."))
        }
        return try {
            firestore.collection(COLLECTION_ACCESS_TOKENS)
                .document(tokenCode)
                .delete()
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("deleteAccessTokenInCloud", e)
            Result.failure(Exception("No se pudo eliminar el token: ${e.localizedMessage}"))
        }
    }

    suspend fun redeemAccessTokenForSignedInUser(tokenCodeRaw: String): CloudAuthOutcome {
        val firebaseUser = auth.currentUser
            ?: return CloudAuthOutcome.Error("Inicia sesión con Google primero para poder canjear tu token.")
        val emailKey = normalizeEmail(firebaseUser.email.orEmpty())
        if (emailKey.isBlank()) {
            return CloudAuthOutcome.Error("La cuenta de Google no devolvió un correo electrónico válido.")
        }

        val tokenCode = tokenCodeRaw.trim().uppercase()
        if (!tokenCode.matches(Regex("^[A-Z0-9_\\-]{6,40}$"))) {
            return CloudAuthOutcome.Error("Formato de token inválido. Verifica los caracteres del código recibido.")
        }

        return try {
            val tokenRef = firestore.collection(COLLECTION_ACCESS_TOKENS).document(tokenCode)
            val tokenSnap = tokenRef.get(Source.SERVER).await()
            if (!tokenSnap.exists()) {
                return CloudAuthOutcome.Error("El token '$tokenCode' no existe o fue eliminado.")
            }

            val parsedToken = parseAccessToken(tokenSnap)
                ?: return CloudAuthOutcome.Error("El token tiene un formato incompatible en el servidor.")

            when (parsedToken.parsedStatus) {
                CloudAccessTokenStatus.REVOKED -> {
                    return CloudAuthOutcome.Error("Este token fue revocado por el Administrador.")
                }
                CloudAccessTokenStatus.REDEEMED -> {
                    val usedBy = parsedToken.redeemedByEmail.ifBlank { "otro usuario" }
                    return CloudAuthOutcome.Error("Este token ya fue canjeado anteriormente por $usedBy.")
                }
                CloudAccessTokenStatus.AVAILABLE -> Unit
            }

            if (parsedToken.targetEmail.isNotBlank() && parsedToken.targetEmail != emailKey) {
                return CloudAuthOutcome.Error(
                    "Este token fue asignado exclusivamente a ${parsedToken.targetEmail} (tu sesión es $emailKey)."
                )
            }

            val userRef = firestore.collection(COLLECTION_USERS).document(emailKey)
            val existingUserSnap = userRef.get(Source.SERVER).await()
            if (existingUserSnap.exists()) {
                return CloudAuthOutcome.Error(
                    "El correo $emailKey ya tiene un registro en la nube. Toca 'Verificar ahora' o pide renovación al Administrador."
                )
            }

            val safeDays = parsedToken.daysGranted.coerceIn(1, 3650)
            val safeMaxDevices = parsedToken.maxDevices.coerceIn(1, 5)
            val nowMs = System.currentTimeMillis()
            val expiresAtMs = nowMs + (safeDays.toLong() * 24L * 60L * 60L * 1000L)
            val deviceId = getOrCreateDeviceId()
            val deviceLabel = getDeviceHardwareLabel()
            val resolvedName = parsedToken.assignedUsername.trim()
                .ifBlank { firebaseUser.displayName?.trim().orEmpty() }
                .ifBlank { emailKey.substringBefore("@") }
                .take(80)

            val newUser = UserAccount(
                id = "op_${UUID.randomUUID().toString().take(8)}",
                username = resolvedName,
                token = tokenCode,
                role = "OPERATOR",
                daysGranted = safeDays,
                validFromMs = nowMs,
                expiresAtMs = expiresAtMs,
                isBlocked = false,
                notes = parsedToken.notes.ifBlank { "Activado con token $tokenCode" }.take(300),
                createdAt = nowMs,
                canAccessLegacyModules = false,
                email = emailKey,
                maxDevices = safeMaxDevices,
                boundDeviceIdsSerialized = deviceId,
                boundDeviceLabelsSerialized = deviceLabel,
                ownerUid = firebaseUser.uid,
                lastOnlineVerifiedAtMs = nowMs
            )

            val batch = firestore.batch()
            batch.set(userRef, userToCreateMap(newUser))
            batch.update(
                tokenRef,
                mapOf(
                    "status" to CloudAccessTokenStatus.REDEEMED.name,
                    "redeemedByEmail" to emailKey,
                    "redeemedByUid" to firebaseUser.uid,
                    "redeemedAt" to FieldValue.serverTimestamp(),
                    "expiresAt" to Timestamp(Date(expiresAtMs)),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            val verifiedSnap = try {
                userRef.get(Source.SERVER).await()
            } catch (_: Exception) {
                null
            }
            val verifiedUser = verifiedSnap?.let { parseUserDocument(it) } ?: newUser
            val verifiedServerMs = verifiedUser.lastOnlineVerifiedAtMs.takeIf { it > 0L } ?: nowMs
            CloudAuthOutcome.Authorized(verifiedUser, serverWallMs = verifiedServerMs)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError("redeemAccessTokenForSignedInUser", e)
            val msg = if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "No se pudo activar el token: verifica que siga disponible o corresponda a tu cuenta."
            } else {
                "Error de servidor al canjear token: ${e.localizedMessage ?: e.code.name}"
            }
            CloudAuthOutcome.Error(msg)
        } catch (e: Exception) {
            handleFirestoreError("redeemAccessTokenForSignedInUser", e)
            CloudAuthOutcome.Error("Sin conexión con Firebase para validar el token. Intenta nuevamente.")
        }
    }

    private fun parseAccessToken(doc: DocumentSnapshot): CloudAccessToken? {
        val data = doc.data ?: return null
        val code = (data["tokenCode"] as? String).orEmpty().ifBlank { doc.id }.trim().uppercase()
        val createdAtMs = doc.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L
        val updatedAtMs = doc.getTimestamp("updatedAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: createdAtMs
        val redeemedAtMs = doc.getTimestamp("redeemedAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L
        val expiresAtMs = (data["expiresAt"] as? Timestamp)?.toDate()?.time ?: 0L
        return CloudAccessToken(
            tokenCode = code,
            assignedUsername = (data["assignedUsername"] as? String).orEmpty(),
            targetEmail = (data["targetEmail"] as? String).orEmpty().trim().lowercase(),
            daysGranted = ((data["daysGranted"] as? Number)?.toInt() ?: 30).coerceIn(1, 3650),
            maxDevices = ((data["maxDevices"] as? Number)?.toInt() ?: 1).coerceIn(1, 5),
            notes = (data["notes"] as? String).orEmpty(),
            status = (data["status"] as? String).orEmpty().ifBlank { CloudAccessTokenStatus.AVAILABLE.name },
            redeemedByEmail = (data["redeemedByEmail"] as? String).orEmpty().trim().lowercase(),
            redeemedByUid = (data["redeemedByUid"] as? String).orEmpty(),
            redeemedAtMs = redeemedAtMs,
            expiresAtMs = expiresAtMs,
            createdByUid = (data["createdByUid"] as? String).orEmpty(),
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs
        )
    }

    // =====================================================================
    // PYRAMID HIERARCHY & SUB-USER APPROVAL QUEUE (/hierarchy_requests)
    // =====================================================================

    fun observeAllHierarchyRequests(): Flow<List<HierarchyRequest>> = callbackFlow {
        val reg: ListenerRegistration = firestore
            .collection(COLLECTION_HIERARCHY_REQUESTS)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    handleFirestoreError("observeAllHierarchyRequests", error)
                    return@addSnapshotListener
                }
                val list = snapshots?.documents
                    ?.mapNotNull { parseHierarchyRequest(it) }
                    .orEmpty()
                    .sortedByDescending { it.createdAtMs }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeMyHierarchyRequests(managerEmailRaw: String): Flow<List<HierarchyRequest>> = callbackFlow {
        val managerEmail = normalizeEmail(managerEmailRaw)
        if (managerEmail.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg: ListenerRegistration = firestore
            .collection(COLLECTION_HIERARCHY_REQUESTS)
            .whereEqualTo("requestedByEmail", managerEmail)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    handleFirestoreError("observeMyHierarchyRequests", error)
                    return@addSnapshotListener
                }
                val list = snapshots?.documents
                    ?.mapNotNull { parseHierarchyRequest(it) }
                    .orEmpty()
                    .sortedByDescending { it.createdAtMs }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun submitHierarchyRequest(
        requestType: HierarchyRequestType,
        parentEmailRaw: String,
        parentName: String,
        targetEmailRaw: String,
        targetUsername: String,
        suggestedDays: Int,
        maxDevices: Int,
        notes: String
    ): Result<HierarchyRequest> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Inicia sesión con Google primero."))
        val requesterEmail = normalizeEmail(currentUser.email.orEmpty())
        val requestedParentEmail = normalizeEmail(parentEmailRaw.ifBlank { requesterEmail })
        val cleanTargetEmail = normalizeEmail(targetEmailRaw)
        if (!cleanTargetEmail.contains("@") || cleanTargetEmail.length < 5) {
            return Result.failure(IllegalArgumentException("Ingresa un correo de Google válido."))
        }
        if (cleanTargetEmail == requestedParentEmail) {
            return Result.failure(IllegalArgumentException("Un usuario no puede asignarse debajo de sí mismo."))
        }
        val safeName = targetUsername.trim().ifBlank { cleanTargetEmail.substringBefore("@") }.take(80)
        val safeDays = suggestedDays.coerceIn(1, 3650)
        val safeDevices = maxDevices.coerceIn(1, 5)
        val safeNotes = notes.trim().take(300)
        val reqId = "req_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val nowMs = System.currentTimeMillis()

        val payload = hashMapOf<String, Any>(
            "id" to reqId,
            "requestType" to requestType.name,
            "requestedParentEmail" to requestedParentEmail.take(120),
            "requestedParentName" to parentName.trim().take(80),
            "targetEmail" to cleanTargetEmail.take(120),
            "targetUsername" to safeName,
            "suggestedDays" to safeDays,
            "maxDevices" to safeDevices,
            "notes" to safeNotes,
            "requestedByUid" to currentUser.uid.take(128),
            "requestedByEmail" to requesterEmail.take(120),
            "status" to HierarchyRequestStatus.PENDING.name,
            "resolvedByEmail" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            firestore.collection(COLLECTION_HIERARCHY_REQUESTS)
                .document(reqId)
                .set(payload)
                .await()
            Result.success(
                HierarchyRequest(
                    id = reqId,
                    requestType = requestType.name,
                    requestedParentEmail = requestedParentEmail,
                    requestedParentName = parentName.trim(),
                    targetEmail = cleanTargetEmail,
                    targetUsername = safeName,
                    suggestedDays = safeDays,
                    maxDevices = safeDevices,
                    notes = safeNotes,
                    requestedByUid = currentUser.uid,
                    requestedByEmail = requesterEmail,
                    status = HierarchyRequestStatus.PENDING.name,
                    resolvedByEmail = "",
                    createdAtMs = nowMs,
                    updatedAtMs = nowMs
                )
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError("submitHierarchyRequest", e)
            val msg = if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "No tienes permiso habilitado para proponer sub-usuarios. Solicítalo al Superusuario."
            } else {
                e.localizedMessage ?: e.code.name
            }
            Result.failure(Exception(msg))
        } catch (e: Exception) {
            handleFirestoreError("submitHierarchyRequest", e)
            Result.failure(Exception("Error enviando solicitud: ${e.localizedMessage}"))
        }
    }

    suspend fun approveHierarchyRequestAndEnable(
        request: HierarchyRequest,
        allUsers: List<UserAccount>,
        overrideDays: Int? = null,
        allowSubManagement: Boolean = false,
        visibilityDepth: Int = 0
    ): Result<String> {
        val adminUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Inicia sesión con Google de Superusuario."))
        val resolverEmail = normalizeEmail(adminUser.email.orEmpty())
        val targetEmail = normalizeEmail(request.targetEmail)
        val requestedParent = normalizeEmail(request.requestedParentEmail)

        if (HierarchyTreeBuilder.wouldCreateCycle(targetEmail, requestedParent, allUsers)) {
            return Result.failure(IllegalStateException("No se puede aprobar porque crearía un ciclo jerárquico."))
        }

        return try {
            val reqRef = firestore.collection(COLLECTION_HIERARCHY_REQUESTS).document(request.id)
            val existingSnap = firestore.collection(COLLECTION_USERS).document(targetEmail).get(Source.SERVER).await()
            val batch = firestore.batch()
            val nowMs = System.currentTimeMillis()
            val parentAccount = allUsers.find { it.normalizedEmail == requestedParent }
            val parentChain = if (requestedParent.isBlank()) {
                emptyList()
            } else if (parentAccount != null) {
                HierarchyTreeBuilder.normalizeChain(listOf(requestedParent) + parentAccount.supervisorChainEmails)
            } else {
                listOf(requestedParent)
            }
            val depth = if (requestedParent.isBlank()) 0 else (parentAccount?.hierarchyDepth ?: 0) + 1

            if (existingSnap.exists()) {
                val parsedExisting = parseUserDocument(existingSnap)
                val mergedAllUsers = (allUsers.filterNot { it.normalizedEmail == targetEmail } + listOfNotNull(parsedExisting))
                val cascade = HierarchyTreeBuilder.computeReParentCascadeUpdates(
                    targetEmail = targetEmail,
                    newParentEmail = requestedParent,
                    allUsers = mergedAllUsers
                )
                cascade.forEach { step ->
                    val ref = firestore.collection(COLLECTION_USERS).document(step.user.normalizedEmail)
                    val isTarget = step.user.normalizedEmail == targetEmail
                    val updateFields = hashMapOf<String, Any>(
                        "parentEmail" to step.newParentEmail,
                        "supervisorChainEmails" to step.newChain,
                        "hierarchyDepth" to step.newDepth,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (isTarget) {
                        updateFields["approvalStatus"] = ApprovalStatus.ACTIVE.name
                        updateFields["isBlocked"] = false
                        updateFields["canManageSubUsers"] = allowSubManagement || step.user.canManageSubUsers
                        updateFields["visibilityDepth"] = visibilityDepth.coerceIn(0, 10)
                        if (overrideDays != null && overrideDays > 0) {
                            val newExpiry = nowMs + (overrideDays.toLong() * 86_400_000L)
                            updateFields["daysGranted"] = overrideDays.coerceIn(1, 3650)
                            updateFields["expiresAt"] = Timestamp(Date(newExpiry))
                        }
                    }
                    batch.update(ref, updateFields)
                    if (step.user.ownerUid.isNotBlank()) {
                        val statsRef = firestore.collection(COLLECTION_SELLER_STATS).document(step.user.ownerUid)
                        val statsSnap = statsRef.get().await()
                        if (statsSnap.exists()) {
                            batch.update(
                                statsRef,
                                mapOf(
                                    "parentEmail" to step.newParentEmail,
                                    "supervisorChainEmails" to step.newChain,
                                    "hierarchyDepth" to step.newDepth,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                    }
                }
            } else {
                val grantedDays = (overrideDays ?: request.suggestedDays).coerceIn(1, 3650)
                val expiresAtMs = nowMs + (grantedDays.toLong() * 86_400_000L)
                val newUser = UserAccount(
                    id = "op_${UUID.randomUUID().toString().take(8)}",
                    username = request.targetUsername.trim().ifBlank { targetEmail.substringBefore("@") }.take(80),
                    token = "GOOGLE-AUTH",
                    role = "OPERATOR",
                    daysGranted = grantedDays,
                    validFromMs = nowMs,
                    expiresAtMs = expiresAtMs,
                    isBlocked = false,
                    notes = request.notes.ifBlank { "Alta bajo $requestedParent" }.take(300),
                    createdAt = nowMs,
                    canAccessLegacyModules = false,
                    email = targetEmail,
                    maxDevices = request.maxDevices.coerceIn(1, 5),
                    parentEmail = requestedParent,
                    supervisorChainSerialized = HierarchyTreeBuilder.serializeChain(parentChain),
                    hierarchyDepth = depth.coerceIn(0, 15),
                    approvalStatus = ApprovalStatus.ACTIVE.name,
                    canManageSubUsers = allowSubManagement,
                    visibilityDepth = visibilityDepth.coerceIn(0, 10),
                    requestedByEmail = request.requestedByEmail
                )
                batch.set(
                    firestore.collection(COLLECTION_USERS).document(targetEmail),
                    userToCreateMap(newUser)
                )
            }

            batch.update(
                reqRef,
                mapOf(
                    "status" to HierarchyRequestStatus.APPROVED.name,
                    "resolvedByEmail" to resolverEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success("${request.targetUsername} habilitado debajo de $requestedParent.")
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("approveHierarchyRequestAndEnable", e)
            Result.failure(Exception("No se pudo aprobar la solicitud: ${e.localizedMessage}"))
        }
    }

    suspend fun rejectHierarchyRequest(requestId: String): Result<Unit> {
        val adminUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Inicia sesión con Google de Superusuario."))
        val resolverEmail = normalizeEmail(adminUser.email.orEmpty())
        return try {
            firestore.collection(COLLECTION_HIERARCHY_REQUESTS)
                .document(requestId)
                .update(
                    mapOf(
                        "status" to HierarchyRequestStatus.REJECTED.name,
                        "resolvedByEmail" to resolverEmail,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("rejectHierarchyRequest", e)
            Result.failure(Exception("No se pudo rechazar la solicitud: ${e.localizedMessage}"))
        }
    }

    suspend fun reassignUserParentInCloud(
        targetEmailRaw: String,
        newParentEmailRaw: String,
        allUsers: List<UserAccount>
    ): Result<Int> {
        val cleanTarget = normalizeEmail(targetEmailRaw)
        val cleanParent = normalizeEmail(newParentEmailRaw)
        if (cleanTarget.isBlank()) {
            return Result.failure(IllegalArgumentException("Usuario destino inválido."))
        }
        if (cleanParent.isNotBlank() && HierarchyTreeBuilder.wouldCreateCycle(cleanTarget, cleanParent, allUsers)) {
            return Result.failure(IllegalStateException("Movimiento inválido: generaría un ciclo en la pirámide."))
        }
        val updates = HierarchyTreeBuilder.computeReParentCascadeUpdates(
            targetEmail = cleanTarget,
            newParentEmail = cleanParent,
            allUsers = allUsers
        )
        if (updates.isEmpty()) {
            return Result.failure(IllegalStateException("No se encontró el usuario en la estructura actual."))
        }

        return try {
            val batch = firestore.batch()
            for (step in updates) {
                val userRef = firestore.collection(COLLECTION_USERS).document(step.user.normalizedEmail)
                batch.update(
                    userRef,
                    mapOf(
                        "parentEmail" to step.newParentEmail,
                        "supervisorChainEmails" to step.newChain,
                        "hierarchyDepth" to step.newDepth,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (step.user.ownerUid.isNotBlank()) {
                    val statsRef = firestore.collection(COLLECTION_SELLER_STATS).document(step.user.ownerUid)
                    val snap = statsRef.get().await()
                    if (snap.exists()) {
                        batch.update(
                            statsRef,
                            mapOf(
                                "parentEmail" to step.newParentEmail,
                                "supervisorChainEmails" to step.newChain,
                                "hierarchyDepth" to step.newDepth,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }
            batch.commit().await()
            Result.success(updates.size)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("reassignUserParentInCloud", e)
            Result.failure(Exception("No se pudo mover la rama: ${e.localizedMessage}"))
        }
    }

    suspend fun updateHierarchyPermissionsInCloud(
        emailRaw: String,
        canManageSubUsers: Boolean,
        visibilityDepth: Int,
        approvalStatus: String? = null,
        isBlocked: Boolean? = null,
        grantDaysIfNeeded: Int? = null
    ): Result<Unit> {
        val emailKey = normalizeEmail(emailRaw)
        if (emailKey.isBlank()) return Result.failure(IllegalArgumentException("Correo inválido."))
        val payload = hashMapOf<String, Any>(
            "canManageSubUsers" to canManageSubUsers,
            "visibilityDepth" to visibilityDepth.coerceIn(0, 10),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!approvalStatus.isNullOrBlank()) {
            payload["approvalStatus"] = approvalStatus
        }
        if (isBlocked != null) {
            payload["isBlocked"] = isBlocked
        }
        if (grantDaysIfNeeded != null && grantDaysIfNeeded > 0) {
            val safeDays = grantDaysIfNeeded.coerceIn(1, 3650)
            payload["daysGranted"] = safeDays
            payload["expiresAt"] = Timestamp(Date(System.currentTimeMillis() + safeDays.toLong() * 86_400_000L))
        }
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(emailKey)
                .update(payload)
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("updateHierarchyPermissionsInCloud", e)
            Result.failure(Exception("No se pudieron guardar los permisos: ${e.localizedMessage}"))
        }
    }

    private fun parseHierarchyRequest(doc: DocumentSnapshot): HierarchyRequest? {
        val data = doc.data ?: return null
        val id = (data["id"] as? String).orEmpty().ifBlank { doc.id }
        val createdAtMs = doc.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L
        val updatedAtMs = doc.getTimestamp("updatedAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: createdAtMs
        return HierarchyRequest(
            id = id,
            requestType = (data["requestType"] as? String).orEmpty().ifBlank { HierarchyRequestType.NEW_SUB_USER.name },
            requestedParentEmail = normalizeEmail((data["requestedParentEmail"] as? String).orEmpty()),
            requestedParentName = (data["requestedParentName"] as? String).orEmpty(),
            targetEmail = normalizeEmail((data["targetEmail"] as? String).orEmpty()),
            targetUsername = (data["targetUsername"] as? String).orEmpty(),
            suggestedDays = ((data["suggestedDays"] as? Number)?.toInt() ?: 30).coerceIn(1, 3650),
            maxDevices = ((data["maxDevices"] as? Number)?.toInt() ?: 1).coerceIn(1, 5),
            notes = (data["notes"] as? String).orEmpty(),
            requestedByUid = (data["requestedByUid"] as? String).orEmpty(),
            requestedByEmail = normalizeEmail((data["requestedByEmail"] as? String).orEmpty()),
            status = (data["status"] as? String).orEmpty().ifBlank { HierarchyRequestStatus.PENDING.name },
            resolvedByEmail = normalizeEmail((data["resolvedByEmail"] as? String).orEmpty()),
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs
        )
    }

    // =====================================================================
    // TEAM NOTIFICATIONS & LEADER COACHING (/team_notifications/{notifId})
    // =====================================================================

    suspend fun publishTeamNotification(
        type: TeamNotificationType,
        priority: TeamNotificationPriority,
        targetMode: TeamNotificationTargetMode,
        targetEmailRaw: String = "",
        title: String,
        body: String,
        actionUrl: String = "",
        requiredVersionCode: Int = 0,
        senderNameOverride: String = ""
    ): Result<TeamNotification> {
        val currentFbUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Inicia sesión con Google antes de enviar avisos."))
        val senderEmail = normalizeEmail(currentFbUser.email.orEmpty())
        if (senderEmail.isBlank()) {
            return Result.failure(IllegalStateException("Tu cuenta Google no devolvió un correo válido."))
        }
        val cleanTargetEmail = if (targetMode == TeamNotificationTargetMode.USER) {
            normalizeEmail(targetEmailRaw)
        } else {
            ""
        }
        if (targetMode == TeamNotificationTargetMode.USER && (!cleanTargetEmail.contains("@") || cleanTargetEmail.length < 5)) {
            return Result.failure(IllegalArgumentException("Selecciona un destinatario con correo Google válido."))
        }
        val cleanTitle = title.trim().take(120)
        val cleanBody = body.trim().take(1000)
        if (cleanTitle.isBlank() || cleanBody.isBlank()) {
            return Result.failure(IllegalArgumentException("Completa título y mensaje antes de enviar."))
        }
        val cleanSenderName = senderNameOverride.trim()
            .ifBlank { currentFbUser.displayName?.trim().orEmpty() }
            .ifBlank { senderEmail.substringBefore("@") }
            .take(80)
        val notifId = "ntf_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val nowMs = System.currentTimeMillis()
        val safeUrl = actionUrl.trim().take(500)
        val safeReqCode = requiredVersionCode.coerceIn(0, 100000)

        val payload = hashMapOf<String, Any>(
            "id" to notifId,
            "type" to type.name,
            "priority" to priority.name,
            "targetMode" to targetMode.name,
            "targetEmail" to cleanTargetEmail,
            "senderEmail" to senderEmail,
            "senderName" to cleanSenderName,
            "senderUid" to currentFbUser.uid.take(128),
            "title" to cleanTitle,
            "body" to cleanBody,
            "actionUrl" to safeUrl,
            "requiredVersionCode" to safeReqCode,
            "createdAtMs" to nowMs,
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            firestore.collection(COLLECTION_TEAM_NOTIFICATIONS)
                .document(notifId)
                .set(payload)
                .await()
            Result.success(
                TeamNotification(
                    id = notifId,
                    type = type.name,
                    priority = priority.name,
                    targetMode = targetMode.name,
                    targetEmail = cleanTargetEmail,
                    senderEmail = senderEmail,
                    senderName = cleanSenderName,
                    senderUid = currentFbUser.uid,
                    title = cleanTitle,
                    body = cleanBody,
                    actionUrl = safeUrl,
                    requiredVersionCode = safeReqCode,
                    createdAtMs = nowMs
                )
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError("publishTeamNotification", e)
            val msg = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    if (type == TeamNotificationType.LEADER_COACHING) {
                        "Solo líderes autorizados pueden enviar mensajes a vendedores de su propia rama."
                    } else {
                        "Solo el Superusuario puede enviar avisos globales o individuales de administración."
                    }
                else -> "Error al enviar notificación: ${e.localizedMessage ?: e.code.name}"
            }
            Result.failure(Exception(msg))
        } catch (e: Exception) {
            handleFirestoreError("publishTeamNotification", e)
            Result.failure(Exception("Error al enviar notificación: ${e.localizedMessage}"))
        }
    }

    fun observeTeamNotificationsForUser(
        viewerEmailRaw: String,
        isSuperuser: Boolean
    ): Flow<List<TeamNotification>> = callbackFlow {
        val cleanEmail = normalizeEmail(viewerEmailRaw)
        if (cleanEmail.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        if (isSuperuser && cleanEmail == BOOTSTRAP_ADMIN_EMAIL) {
            val reg = firestore.collection(COLLECTION_TEAM_NOTIFICATIONS)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        handleFirestoreError("observeTeamNotifications(admin)", error)
                        return@addSnapshotListener
                    }
                    val list = snap?.documents?.mapNotNull { parseTeamNotification(it) }.orEmpty()
                        .sortedByDescending { it.createdAtMs }
                    trySend(list)
                }
            awaitClose { reg.remove() }
            return@callbackFlow
        }

        val allMap = linkedMapOf<String, TeamNotification>()
        val directMap = linkedMapOf<String, TeamNotification>()
        val sentMap = linkedMapOf<String, TeamNotification>()

        fun emitMerged() {
            val merged = (allMap.values + directMap.values + sentMap.values)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAtMs }
            trySend(merged)
        }

        val regAll = firestore.collection(COLLECTION_TEAM_NOTIFICATIONS)
            .whereEqualTo("targetMode", TeamNotificationTargetMode.ALL.name)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    handleFirestoreError("observeTeamNotifications(all)", error)
                    return@addSnapshotListener
                }
                allMap.clear()
                snap?.documents?.mapNotNull { parseTeamNotification(it) }?.forEach { allMap[it.id] = it }
                emitMerged()
            }

        val regDirect = firestore.collection(COLLECTION_TEAM_NOTIFICATIONS)
            .whereEqualTo("targetEmail", cleanEmail)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    handleFirestoreError("observeTeamNotifications(direct)", error)
                    return@addSnapshotListener
                }
                directMap.clear()
                snap?.documents?.mapNotNull { parseTeamNotification(it) }?.forEach { directMap[it.id] = it }
                emitMerged()
            }

        val regSent = firestore.collection(COLLECTION_TEAM_NOTIFICATIONS)
            .whereEqualTo("senderEmail", cleanEmail)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    handleFirestoreError("observeTeamNotifications(sent)", error)
                    return@addSnapshotListener
                }
                sentMap.clear()
                snap?.documents?.mapNotNull { parseTeamNotification(it) }?.forEach { sentMap[it.id] = it }
                emitMerged()
            }

        awaitClose {
            regAll.remove()
            regDirect.remove()
            regSent.remove()
        }
    }

    private fun parseTeamNotification(doc: DocumentSnapshot): TeamNotification? {
        val data = doc.data ?: return null
        val id = (data["id"] as? String).orEmpty().ifBlank { doc.id }
        val serverMs = doc.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L
        val rawMs = (data["createdAtMs"] as? Number)?.toLong() ?: 0L
        return TeamNotification(
            id = id,
            type = (data["type"] as? String).orEmpty().ifBlank { TeamNotificationType.SUPERUSER_GLOBAL.name },
            priority = (data["priority"] as? String).orEmpty().ifBlank { TeamNotificationPriority.INFO.name },
            targetMode = (data["targetMode"] as? String).orEmpty().ifBlank { TeamNotificationTargetMode.ALL.name },
            targetEmail = normalizeEmail((data["targetEmail"] as? String).orEmpty()),
            senderEmail = normalizeEmail((data["senderEmail"] as? String).orEmpty()),
            senderName = (data["senderName"] as? String).orEmpty(),
            senderUid = (data["senderUid"] as? String).orEmpty(),
            title = (data["title"] as? String).orEmpty(),
            body = (data["body"] as? String).orEmpty(),
            actionUrl = (data["actionUrl"] as? String).orEmpty(),
            requiredVersionCode = ((data["requiredVersionCode"] as? Number)?.toInt() ?: 0).coerceIn(0, 100000),
            createdAtMs = maxOf(rawMs, serverMs)
        )
    }

    // =====================================================================
    // GLOBAL BANNER ANNOUNCEMENTS (/mensajes_globales/{mensajeId})
    // =====================================================================

    fun observeMensajesGlobales(includeInactiveForSuperuser: Boolean = false): Flow<List<MensajeGlobal>> = callbackFlow {
        val fbUser = auth.currentUser
        if (fbUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val viewerEmail = normalizeEmail(fbUser.email.orEmpty())
        val canListAll = includeInactiveForSuperuser && viewerEmail == BOOTSTRAP_ADMIN_EMAIL
        val query = if (canListAll) {
            firestore.collection(COLLECTION_GLOBAL_MESSAGES)
        } else {
            firestore.collection(COLLECTION_GLOBAL_MESSAGES).whereEqualTo("activo", true)
        }
        val reg: ListenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                handleFirestoreError("observeMensajesGlobales", error)
                return@addSnapshotListener
            }
            val list = snapshots?.documents
                ?.mapNotNull { parseMensajeGlobal(it) }
                .orEmpty()
                .sortedByDescending { it.creadoEnMs }
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun publishMensajeGlobal(
        titulo: String,
        mensaje: String,
        prioridad: MensajeGlobalPrioridad = MensajeGlobalPrioridad.IMPORTANTE,
        autorNombreOverride: String = ""
    ): Result<MensajeGlobal> {
        val currentFbUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Inicia sesión con Google antes de publicar anuncios globales."))
        val autorEmail = normalizeEmail(currentFbUser.email.orEmpty())
        if (autorEmail.isBlank()) {
            return Result.failure(IllegalStateException("Tu cuenta Google no devolvió un correo válido."))
        }
        val cleanTitulo = titulo.trim().take(120)
        val cleanMensaje = mensaje.trim().take(1000)
        if (cleanTitulo.isBlank() || cleanMensaje.isBlank()) {
            return Result.failure(IllegalArgumentException("Completa el título y el mensaje del anuncio global."))
        }
        val cleanAutorNombre = autorNombreOverride.trim()
            .ifBlank { currentFbUser.displayName?.trim().orEmpty() }
            .ifBlank { autorEmail.substringBefore("@") }
            .take(80)
        val mensajeId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val nowMs = System.currentTimeMillis()
        val payload = hashMapOf<String, Any>(
            "titulo" to cleanTitulo,
            "mensaje" to cleanMensaje,
            "prioridad" to prioridad.name,
            "activo" to true,
            "autorEmail" to autorEmail,
            "autorNombre" to cleanAutorNombre,
            "autorUid" to currentFbUser.uid.take(128),
            "creadoEnMs" to nowMs,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            firestore.collection(COLLECTION_GLOBAL_MESSAGES)
                .document(mensajeId)
                .set(payload)
                .await()
            Result.success(
                MensajeGlobal(
                    id = mensajeId,
                    titulo = cleanTitulo,
                    mensaje = cleanMensaje,
                    prioridad = prioridad.name,
                    activo = true,
                    autorEmail = autorEmail,
                    autorNombre = cleanAutorNombre,
                    autorUid = currentFbUser.uid,
                    creadoEnMs = nowMs
                )
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError("publishMensajeGlobal", e)
            val msg = if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "Solo el Superusuario puede publicar anuncios en mensajes_globales."
            } else {
                "Error publicando anuncio global: ${e.localizedMessage ?: e.code.name}"
            }
            Result.failure(Exception(msg))
        } catch (e: Exception) {
            handleFirestoreError("publishMensajeGlobal", e)
            Result.failure(Exception("Error publicando anuncio global: ${e.localizedMessage}"))
        }
    }

    suspend fun setMensajeGlobalActivo(mensajeId: String, activo: Boolean): Result<Unit> {
        auth.currentUser
            ?: return Result.failure(IllegalStateException("Sesión Google requerida."))
        val cleanId = mensajeId.trim()
        if (cleanId.length < 3) {
            return Result.failure(IllegalArgumentException("ID de anuncio inválido."))
        }
        return try {
            firestore.collection(COLLECTION_GLOBAL_MESSAGES)
                .document(cleanId)
                .update(
                    mapOf(
                        "activo" to activo,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("setMensajeGlobalActivo", e)
            Result.failure(Exception("No se pudo actualizar el anuncio: ${e.localizedMessage}"))
        }
    }

    suspend fun deleteMensajeGlobal(mensajeId: String): Result<Unit> {
        auth.currentUser
            ?: return Result.failure(IllegalStateException("Sesión Google requerida."))
        val cleanId = mensajeId.trim()
        if (cleanId.length < 3) {
            return Result.failure(IllegalArgumentException("ID de anuncio inválido."))
        }
        return try {
            firestore.collection(COLLECTION_GLOBAL_MESSAGES)
                .document(cleanId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError("deleteMensajeGlobal", e)
            Result.failure(Exception("No se pudo eliminar el anuncio: ${e.localizedMessage}"))
        }
    }

    private fun parseMensajeGlobal(doc: DocumentSnapshot): MensajeGlobal? {
        val data = doc.data ?: return null
        val serverMs = doc.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L
        val rawMs = (data["creadoEnMs"] as? Number)?.toLong() ?: 0L
        return MensajeGlobal(
            id = doc.id,
            titulo = (data["titulo"] as? String).orEmpty(),
            mensaje = (data["mensaje"] as? String).orEmpty(),
            prioridad = (data["prioridad"] as? String).orEmpty().ifBlank { MensajeGlobalPrioridad.IMPORTANTE.name },
            activo = (data["activo"] as? Boolean) ?: true,
            autorEmail = normalizeEmail((data["autorEmail"] as? String).orEmpty()),
            autorNombre = (data["autorNombre"] as? String).orEmpty(),
            autorUid = (data["autorUid"] as? String).orEmpty(),
            creadoEnMs = maxOf(rawMs, serverMs)
        )
    }
}
