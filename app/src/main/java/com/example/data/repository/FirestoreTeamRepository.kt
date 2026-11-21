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
import com.example.data.model.PsaContactLead
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaLeadStatus
import com.example.data.model.PsaPostSaleInstallation
import com.example.data.model.PsaPostSaleStatus
import com.example.data.model.PsaVisitStatus
import com.example.data.model.RoleVisibilityConfig
import com.example.data.model.SellerPerformanceStats
import com.example.data.model.UserAccount
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

class FirestoreTeamRepository(context: Context) {

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
            if (cloudUser.isBlocked || cloudUser.isExpired) {
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
        val emailKey = normalizeEmail(user.email)
        if (emailKey.isBlank()) return Result.failure(IllegalArgumentException("Usuario sin correo configurado."))
        val safeMaxDevices = user.maxDevices.coerceIn(user.boundDeviceIds.size.coerceAtLeast(1), 5)
        val sanitized = user.copy(maxDevices = safeMaxDevices)
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(emailKey)
                .update(userToUpdateMap(sanitized))
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
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
        val emailKey = normalizeEmail(emailRaw)
        if (emailKey.isBlank() || emailKey == BOOTSTRAP_ADMIN_EMAIL) {
            return Result.failure(IllegalArgumentException("No se puede eliminar este perfil."))
        }
        return try {
            firestore.collection(COLLECTION_USERS).document(emailKey).delete().await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
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
            val updatedAtMs = doc.getTimestamp("updatedAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L
            com.example.util.AppUpdateInfo(
                latestVersionCode = latestCode,
                latestVersionName = latestName.ifBlank { latestCode.toString() },
                apkDownloadUrl = normalizedUrl,
                releaseNotes = notes,
                updatedAtMs = updatedAtMs
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "fetchAppVersionFromServer failed: ${e.message}")
            null
        }
    }

    /**
     * Permite únicamente al Superusuario publicar en el servidor la versión más reciente y su enlace APK.
     */
    suspend fun publishAppVersionToCloud(
        versionCode: Int,
        versionName: String,
        apkDownloadUrl: String,
        releaseNotes: String
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
                    updatedAtMs = System.currentTimeMillis()
                )
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "publishAppVersionToCloud failed: ${e.message}")
            Result.failure(Exception("No se pudo publicar la actualización: ${e.localizedMessage}"))
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

        return UserAccount(
            id = id,
            username = username,
            token = "GOOGLE-AUTH",
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
            lastTamperAlertAtMs = lastTamperAlertAtMs
        )
    }

    private fun userToCreateMap(user: UserAccount): Map<String, Any> {
        val ids = user.boundDeviceIds.take(5)
        val labels = alignLabelsWithIds(ids, user.boundDeviceLabels)
        val firestoreRole = if (user.role == "SUPERUSER") "SUPERUSER" else "OPERATOR"
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
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun userToUpdateMap(user: UserAccount): Map<String, Any> {
        val ids = user.boundDeviceIds.take(5)
        val labels = alignLabelsWithIds(ids, user.boundDeviceLabels)
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
            "updatedAt" to FieldValue.serverTimestamp()
        )
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
            Log.w(TAG, "upsertSellerContact failed: ${e.message}")
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
            Log.w(TAG, "deleteSellerContact failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun observeSellerContacts(ownerUid: String): Flow<List<PsaContactLead>> = callbackFlow {
        if (ownerUid.isBlank()) {
            close()
            return@callbackFlow
        }
        val reg = firestore
            .collection(COLLECTION_WORKSPACES)
            .document(ownerUid)
            .collection(SUBCOLLECTION_CONTACTS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "observeSellerContacts error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { parseSellerContact(it) }.orEmpty()
                trySend(list)
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
            Log.w(TAG, "upsertSellerVisit failed: ${e.message}")
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
            Log.w(TAG, "deleteSellerVisit failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun observeSellerVisits(ownerUid: String): Flow<List<PsaHomeVisit>> = callbackFlow {
        if (ownerUid.isBlank()) {
            close()
            return@callbackFlow
        }
        val reg = firestore
            .collection(COLLECTION_WORKSPACES)
            .document(ownerUid)
            .collection(SUBCOLLECTION_VISITS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "observeSellerVisits error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { parseSellerVisit(it) }.orEmpty()
                trySend(list)
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
            Log.w(TAG, "upsertPostSaleRecord failed: ${e.message}")
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
            Log.w(TAG, "deletePostSaleRecord failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun observeSellerPostSale(ownerUid: String): Flow<List<PsaPostSaleInstallation>> = callbackFlow {
        if (ownerUid.isBlank()) {
            close()
            return@callbackFlow
        }
        val reg = firestore
            .collection(COLLECTION_WORKSPACES)
            .document(ownerUid)
            .collection(SUBCOLLECTION_POST_SALE)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "observeSellerPostSale error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { parsePostSaleRecord(it) }.orEmpty()
                trySend(list)
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
                    .sortedByDescending { it.closedSales }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun fetchAllSellerStatsOnce(): List<SellerPerformanceStats> {
        return try {
            val snap = firestore.collection(COLLECTION_SELLER_STATS).get().await()
            snap.documents.mapNotNull { parseSellerStats(it) }
                .sortedByDescending { it.closedSales }
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
        return SellerPerformanceStats(
            ownerUid = ownerUid,
            sellerName = (data["sellerName"] as? String).orEmpty().ifBlank { "Vendedor PSA" },
            sellerEmail = (data["sellerEmail"] as? String).orEmpty(),
            totalContacts = (data["totalContacts"] as? Number)?.toInt() ?: 0,
            totalVisits = (data["totalVisits"] as? Number)?.toInt() ?: 0,
            completedDemos = (data["completedDemos"] as? Number)?.toInt() ?: 0,
            closedSales = (data["closedSales"] as? Number)?.toInt() ?: 0,
            pendingReplacements = (data["pendingReplacements"] as? Number)?.toInt() ?: 0,
            updatedAtMs = updatedAtMs
        )
    }
}
