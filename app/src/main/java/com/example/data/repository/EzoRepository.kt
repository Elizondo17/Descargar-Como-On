package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.BusinessProfile
import com.example.data.model.DeliveryLog
import com.example.data.model.DeliveryStats
import com.example.data.model.ScannedChat
import com.example.data.model.ScriptFolder
import com.example.data.model.ScriptItem
import com.example.data.model.SalesKnowledgeItem
import com.example.data.model.MemoryEnhancedSalesAdvice
import com.example.data.model.PsaContactLead
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaPostSaleInstallation
import com.example.data.model.PsaPostSaleStatus
import com.example.data.model.PsaVisitStatus
import com.example.data.model.RoleVisibilityConfig
import com.example.data.model.SellerPerformanceStats
import com.example.data.model.UserAccount
import com.example.data.model.UserToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.UUID

class EzoRepository(
    private val database: AppDatabase,
    private val context: Context? = null
) {

    private val userDao = database.userDao()
    private val userTokenDao = database.userTokenDao()
    private val scriptDao = database.scriptDao()
    private val deliveryLogDao = database.deliveryLogDao()
    private val scannedChatDao = database.scannedChatDao()
    private val businessProfileDao = database.businessProfileDao()
    private val businessPresetDao = database.businessPresetDao()
    private val salesKnowledgeDao = database.salesKnowledgeDao()
    private val psaVisitDao = database.psaVisitDao()
    private val psaContactDao = database.psaContactDao()
    private val psaPostSaleDao = database.psaPostSaleDao()

    private val prefs = context?.getSharedPreferences("como_on_auth_prefs", Context.MODE_PRIVATE)
    val cloudTeamRepository: FirestoreTeamRepository? = context?.let { FirestoreTeamRepository(it) }

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser = _currentUser.asStateFlow()

    fun setCurrentUser(user: UserAccount?) {
        _currentUser.value = user
    }

    suspend fun upsertUserLocally(user: UserAccount) {
        userDao.insertUser(user)
        if (_currentUser.value?.id == user.id || (_currentUser.value?.email?.isNotBlank() == true && _currentUser.value?.email == user.email)) {
            _currentUser.value = user
        }
    }

    suspend fun replaceCloudUsersInCache(remoteUsers: List<UserAccount>) {
        if (remoteUsers.isEmpty()) return
        userDao.insertAll(remoteUsers)
    }

    val allUsers: Flow<List<UserAccount>> = userDao.getAllUsers()
    val allUserTokens: Flow<List<UserToken>> = userTokenDao.getAllTokens()
    val allFolders: Flow<List<ScriptFolder>> = scriptDao.getAllFolders()
    val allScripts: Flow<List<ScriptItem>> = scriptDao.getAllScripts()
    val allLogs: Flow<List<DeliveryLog>> = deliveryLogDao.getAllLogs()
    val allSalesKnowledge: Flow<List<SalesKnowledgeItem>> = salesKnowledgeDao.getAllKnowledge()
    val allPsaVisits: Flow<List<PsaHomeVisit>> = psaVisitDao.getAllVisits()
    val allPsaLeads: Flow<List<com.example.data.model.PsaContactLead>> = psaContactDao.getAllLeads()
    val allPsaInteractions: Flow<List<com.example.data.model.PsaContactInteraction>> = psaContactDao.getAllInteractions()
    val allPostSaleInstallations: Flow<List<PsaPostSaleInstallation>> = psaPostSaleDao.getAllInstallations()

    val allScannedChats: Flow<List<ScannedChat>> = scannedChatDao.getAllScannedChats()
    val selectedScannedChats: Flow<List<ScannedChat>> = scannedChatDao.getSelectedChats()
    val businessProfileFlow: Flow<BusinessProfile?> = businessProfileDao.getProfileFlow()
    val allPresets: Flow<List<com.example.data.model.BusinessPreset>> = businessPresetDao.getAllPresets()

    val deliveryStats: Flow<DeliveryStats> = combine(
        deliveryLogDao.getTotalCount(),
        deliveryLogDao.getSuccessCount(),
        deliveryLogDao.getFailedCount()
    ) { total, success, failed ->
        val sRate = if (total > 0) (success.toFloat() / total) * 100f else 0f
        val fRate = if (total > 0) (failed.toFloat() / total) * 100f else 0f
        DeliveryStats(
            totalCount = total,
            successCount = success,
            failedCount = failed,
            successRate = sRate,
            failureRate = fRate
        )
    }

    suspend fun initializeDefaultsIfNeeded() {
        // Ensure Superuser exists with user "Admin-payito" and key "amordechat"
        val existingSuperuser = userDao.getUserByUsername("Admin-payito")
        if (existingSuperuser == null) {
            val superuser = UserAccount(
                id = "admin-payito",
                username = "Admin-payito",
                role = "SUPERUSER",
                token = "amordechat",
                validFromMs = System.currentTimeMillis(),
                expiresAtMs = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10), // 10 years
                daysGranted = 3650,
                isBlocked = false,
                notes = "Superusuario Maestro - Acceso total",
                email = FirestoreTeamRepository.BOOTSTRAP_ADMIN_EMAIL
            )
            userDao.insertUser(superuser)
            userTokenDao.insertToken(
                UserToken(
                    token = "amordechat",
                    userId = "admin-payito",
                    username = "Admin-payito",
                    role = "SUPERUSER",
                    validFromMs = superuser.validFromMs,
                    expiresAtMs = superuser.expiresAtMs,
                    daysGranted = 3650,
                    isBlocked = false,
                    notes = "Token Maestro Superusuario"
                )
            )
        } else {
            if (existingSuperuser.email.isBlank()) {
                userDao.insertUser(existingSuperuser.copy(email = FirestoreTeamRepository.BOOTSTRAP_ADMIN_EMAIL))
            }
            val existingToken = userTokenDao.findByToken("amordechat")
            if (existingToken == null) {
                userTokenDao.insertToken(
                    UserToken(
                        token = "amordechat",
                        userId = existingSuperuser.id,
                        username = existingSuperuser.username,
                        role = "SUPERUSER",
                        validFromMs = existingSuperuser.validFromMs,
                        expiresAtMs = existingSuperuser.expiresAtMs,
                        daysGranted = 3650,
                        isBlocked = false,
                        notes = "Token Maestro Superusuario"
                    )
                )
            }
        }

        // Legacy saved_username/saved_key auto-login must NOT bypass Google Firebase Auth.
        val hasFirebaseSession = cloudTeamRepository?.getCurrentFirebaseUser() != null
        val savedUsername = prefs?.getString("saved_username", null)
        val savedKey = prefs?.getString("saved_key", null)

        // Hydrate users from cloud when Firebase session is active
        syncAllUsersFromCloud()

        if (hasFirebaseSession && !savedUsername.isNullOrBlank() && !savedKey.isNullOrBlank()) {
            val loginRes = loginWithCredentials(savedUsername, savedKey, remember = true)
            if (loginRes.isFailure) {
                prefs?.edit()?.remove("saved_username")?.remove("saved_key")?.apply()
                _currentUser.value = null
            }
        } else {
            if (!hasFirebaseSession && !savedUsername.isNullOrBlank()) {
                prefs?.edit()?.remove("saved_username")?.remove("saved_key")?.apply()
            }
            _currentUser.value = null
        }

        // Initialize Gemini API key from persistent storage
        com.example.service.GeminiSalesService.customApiKey = getGeminiApiKey()

        // Seed default script folders if empty
        val existingFolders = scriptDao.getAllFolders().first()
        if (existingFolders.isEmpty()) {
            val folder1Id = scriptDao.insertFolder(
                ScriptFolder(
                    name = "Lecturas y Tiradas",
                    description = "Lecturas de tarot, oráculos y mensajes de guía",
                    colorHex = "#A855F7",
                    isDefault = true
                )
            )
            val folder2Id = scriptDao.insertFolder(
                ScriptFolder(
                    name = "Limpiezas y Armonización",
                    description = "Limpiezas de energía, destrabes y armonizaciones",
                    colorHex = "#10B981",
                    isDefault = true
                )
            )
            val folder3Id = scriptDao.insertFolder(
                ScriptFolder(
                    name = "Recordatorios y Turnos",
                    description = "Avisos cordiales de citas y confirmación de espacios",
                    colorHex = "#F59E0B",
                    isDefault = true
                )
            )
            val folder4Id = scriptDao.insertFolder(
                ScriptFolder(
                    name = "Reactivación de Consultantes",
                    description = "Seguimiento cálido para retomar contacto y saber cómo están",
                    colorHex = "#EC4899",
                    isDefault = false
                )
            )
            val folder5Id = scriptDao.insertFolder(
                ScriptFolder(
                    name = "Respuestas Rápidas",
                    description = "Mensajes rápidos para responder consultas frecuentes en un toque",
                    colorHex = "#6366F1",
                    isDefault = false
                )
            )

            // Seed warm, affectionate scripts with Spintax and {afecto}
            scriptDao.insertScript(
                ScriptItem(
                    folderId = folder1Id,
                    title = "Lectura Completa Tarot",
                    content = "{Hola|Buen día|Paso a saludarte} {afecto}, {te cuento|quería comentarte|paso a avisarte} que habilité nuevos cupos para lecturas completas y tiradas personalizadas esta semana ✨🔮 ¿Te gustaría reservar tu espacio?",
                    isAutoResponse = false
                )
            )
            scriptDao.insertScript(
                ScriptItem(
                    folderId = folder2Id,
                    title = "Limpieza Energética",
                    content = "{Hola|Buenas tardes} {afecto}, {espero que estés muy bien|deseo que tengas un día lleno de luz} ✨ Quería contarte que estoy realizando sesiones de limpieza de energía y armonización. ¿Sientes que necesitas renovar tu vibra?",
                    isAutoResponse = false
                )
            )
            scriptDao.insertScript(
                ScriptItem(
                    folderId = folder3Id,
                    title = "Recordatorio de Turno",
                    content = "{Hola|Buen día} {afecto}, {recordatorio cariñoso|paso a recordarte con mucho amor} sobre nuestro encuentro programado 💖 ¿Pudiste coordinar bien tus tiempos?",
                    isAutoResponse = false
                )
            )
            scriptDao.insertScript(
                ScriptItem(
                    folderId = folder4Id,
                    title = "Reactivación con Amor",
                    content = "{Hola|Buenas tardes} {afecto}, {hacía tiempo que no sabía de ti|hoy sentí pasar a saludarte} 🌸 ¿Cómo has sentido tu energía en estos últimos días?",
                    isAutoResponse = false
                )
            )
            scriptDao.insertScript(
                ScriptItem(
                    folderId = folder5Id,
                    title = "Información y Medios de Pago",
                    content = "{Hola|Muchas gracias por escribir} {afecto} 💕 Con todo gusto te comparto los detalles y las opciones disponibles para coordinar tu sesión cuando gustes.",
                    isAutoResponse = false
                )
            )
        }

        // Seed default Business Profile if not existing
        val currentProfile = businessProfileDao.getProfile()
        if (currentProfile == null) {
            businessProfileDao.saveProfile(
                BusinessProfile(
                    id = "primary_profile",
                    businessName = "Como On Ventas & Servicios",
                    businessType = "Venta General y Servicios",
                    offeringDescription = "Productos y servicios de alta calidad, atención inmediata personalizada, planes de pago accesibles y asesoramiento integral.",
                    toneAndStyle = "Cálido, profesional, persuasivo y enfocado en la satisfacción del cliente.",
                    salesObjective = "Resolver dudas, transmitir confianza, coordinar una cita o llamada y concretar el cierre de la venta.",
                    frequentQuestionsAndObjections = "¿Cuáles son las formas de pago? ¿Hacen envíos o turnos? ¿Tienen garantía?",
                    isAiEnabledForOperators = false
                )
            )
        }

        // Seed default business presets if table is empty
        val existingPresets = businessPresetDao.getAllPresets().first()
        if (existingPresets.isEmpty()) {
            businessPresetDao.insertPreset(
                com.example.data.model.BusinessPreset(
                    id = "preset_autos",
                    title = "🚗 Autos",
                    businessName = "Autos Selectos",
                    businessType = "Venta y Financiación de Vehículos Seminuevos",
                    offeringDescription = "Venta de autos usados seleccionados y 0km multimarca. Tomamos tu usado como parte de pago al mejor precio. Financiación bancaria en cuotas fijas en pesos y entrega inmediata con peritaje y garantía de 6 meses.",
                    toneAndStyle = "Profesional, seguro, cordial y con vocabulario comercial automotriz. Transmite solidez.",
                    salesObjective = "Agendar una visita al salón de ventas para ver y probar el auto, o coordinar tasación del usado.",
                    frequentQuestionsAndObjections = "¿Aceptan permutas? Sí. ¿Financian? Sí, cuotas fijas con DNI. ¿Tienen garantía? Sí, 6 meses.",
                    isCustom = false
                )
            )
            businessPresetDao.insertPreset(
                com.example.data.model.BusinessPreset(
                    id = "preset_cortinas",
                    title = "🪟 Cortinas",
                    businessName = "Cortinas & Deco Home",
                    businessType = "Cortinas Roller y Textiles a Medida",
                    offeringDescription = "Cortinas roller Blackout térmico, Sunscreen 5% para living y telas a medida exacta. Asesoramiento sin cargo a domicilio con muestrarios, toma de medidas e instalación profesional.",
                    toneAndStyle = "Cálido, estético, empático y asesor de diseño de interiores.",
                    salesObjective = "Obtener medidas aproximadas para cotizar y agendar visita técnica con muestrarios.",
                    frequentQuestionsAndObjections = "¿Hacen colocación? Sí, incluida. ¿Demora? 7 a 10 días hábiles. ¿Presupuestos? Al instante con medidas.",
                    isCustom = false
                )
            )
            businessPresetDao.insertPreset(
                com.example.data.model.BusinessPreset(
                    id = "preset_tarot",
                    title = "🔮 Tarot",
                    businessName = "Luz Astral & Sanación",
                    businessType = "Tarot Terapéutico y Limpiezas Energéticas",
                    offeringDescription = "Lecturas profundas de Tarot para amor, trabajo y economía. Limpiezas espirituales presenciales y a distancia con corte de energías densas, apertura de caminos y armonización.",
                    toneAndStyle = "Místico, protector, respetuoso, empático y de contención energética.",
                    salesObjective = "Generar tranquilidad y confianza, explicar la modalidad y concretar reserva de turno con pago previo.",
                    frequentQuestionsAndObjections = "¿Cómo se realiza? Videollamada o WhatsApp con audios y fotos. ¿Duración? 45 min. ¿Pago? Transferencia o Mercado Pago.",
                    isCustom = false
                )
            )
        }

        // Seed default Sales Knowledge Memory if empty
        val existingKnowledge = salesKnowledgeDao.getTopWinningKnowledge(1)
        if (existingKnowledge.isEmpty()) {
            salesKnowledgeDao.insertKnowledge(
                SalesKnowledgeItem(
                    category = "QUESTION_DISCOVERY",
                    triggerContext = "Indagación inicial de necesidad",
                    winningContent = "¿Qué es lo más importante para ti al momento de elegir el servicio? ¿Priorizas rapidez, calidad o facilidades de pago?",
                    successScore = 5,
                    isQuestion = true
                )
            )
            salesKnowledgeDao.insertKnowledge(
                SalesKnowledgeItem(
                    category = "CLOSING_TECHNIQUE",
                    triggerContext = "Cierre con alternativa suave",
                    winningContent = "Si encontramos una opción que se adapte exacto a tus tiempos y con facilidades de pago, ¿te gustaría que lo confirmemos hoy mismo?",
                    successScore = 7,
                    isQuestion = true
                )
            )
            salesKnowledgeDao.insertKnowledge(
                SalesKnowledgeItem(
                    category = "OBJECTION_PRICE",
                    triggerContext = "Cliente dice que el precio está alto o lo va a pensar",
                    winningContent = "Entiendo perfectamente tu punto. Precisamente para cuidarte el bolsillo ofrecemos cuotas fijas y garantía de satisfacción total, así no arriesgas nada. ¿Te reservo el beneficio mientras esté vigente?",
                    successScore = 8,
                    isQuestion = false
                )
            )
            salesKnowledgeDao.insertKnowledge(
                SalesKnowledgeItem(
                    category = "OBJECTION_TRUST",
                    triggerContext = "Cliente pregunta por garantía o seguridad",
                    winningContent = "¡Con total tranquilidad! Todos nuestros trabajos cuentan con garantía de respaldo y acompañamiento personalizado de inicio a fin. ¿Prefieres que te pase fotos de resultados y testimonios de clientes?",
                    successScore = 6,
                    isQuestion = false
                )
            )
        }

        // Seed specialized PSA knowledge if not yet inserted
        val psaKnowledgeItems = com.example.util.PsaScheduleManager.getInitialPsaKnowledge()
        for (item in psaKnowledgeItems) {
            val exists = salesKnowledgeDao.getKnowledgeById(item.id)
            if (exists == null) {
                salesKnowledgeDao.insertKnowledge(item)
            }
        }
    }

    suspend fun loginWithCredentials(
        usernameInput: String,
        keyInput: String,
        remember: Boolean = true
    ): Result<UserAccount> {
        val cleanUser = usernameInput.trim()
        val cleanKey = keyInput.trim()

        if (cleanUser.isBlank() || cleanKey.isBlank()) {
            return Result.failure(Exception("Por favor ingresa usuario y key."))
        }

        // 1. Superuser verification (Admin-payito / amordechat)
        if (cleanUser.equals("Admin-payito", ignoreCase = true) && cleanKey.equals("amordechat", ignoreCase = true)) {
            var superuser = userDao.getUserByUsername("Admin-payito")
            if (superuser == null || superuser.token != "amordechat") {
                superuser = UserAccount(
                    id = "admin-payito",
                    username = "Admin-payito",
                    role = "SUPERUSER",
                    token = "amordechat",
                    validFromMs = System.currentTimeMillis(),
                    expiresAtMs = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10),
                    daysGranted = 3650,
                    isBlocked = false,
                    notes = "Superusuario Maestro - Acceso total"
                )
                userDao.insertUser(superuser)
            }
            _currentUser.value = superuser
            if (remember) {
                prefs?.edit()
                    ?.putString("saved_username", cleanUser)
                    ?.putString("saved_key", cleanKey)
                    ?.apply()
            }
            return Result.success(superuser)
        }

        // 2. Standard user verification against local database
        var user = userDao.getUserByCredentials(cleanUser, cleanKey)

        // 3. If not found locally, verify if it's a Portable Universal Key (for other devices)
        if (user == null) {
            val portableUser = com.example.util.LicenseManager.verifyAndCreatePortableUser(cleanUser, cleanKey)
            if (portableUser != null) {
                userDao.insertUser(portableUser)
                user = portableUser
            }
        }

        if (user == null) {
            return Result.failure(Exception("Usuario o Key incorrectos. Verifica que coincidan exactamente con la clave provista."))
        }

        if (user.isBlocked) {
            return Result.failure(Exception("Esta cuenta ha sido bloqueada. Contacta al administrador."))
        }

        if (user.isExpired) {
            return Result.failure(Exception("Tu periodo de acceso ha expirado (${user.daysGranted} días cumplidos). Contacta al administrador para renovar."))
        }

        _currentUser.value = user
        if (remember) {
            prefs?.edit()
                ?.putString("saved_username", cleanUser)
                ?.putString("saved_key", cleanKey)
                ?.apply()
        }
        return Result.success(user)
    }

    fun logout() {
        _currentUser.value = null
        prefs?.edit()?.remove("saved_username")?.remove("saved_key")?.apply()
        cloudTeamRepository?.setLastSyncedUid("")
        CoroutineScope(Dispatchers.IO).launch {
            wipeSellerLocalWorkspaceAndCaches()
        }
    }

    suspend fun createSecondaryUser(
        name: String,
        customKey: String?,
        days: Int,
        notes: String
    ): UserAccount {
        val cleanName = name.trim()
        val cleanKey = if (!customKey.isNullOrBlank()) {
            customKey.trim()
        } else {
            com.example.util.LicenseManager.generatePortableKey(cleanName, days)
        }
        val userId = "user-" + UUID.randomUUID().toString().substring(0, 8)
        val now = System.currentTimeMillis()
        val durationMs = days.toLong() * 24L * 60L * 60L * 1000L

        val newUser = UserAccount(
            id = userId,
            username = cleanName,
            role = "OPERATOR",
            token = cleanKey,
            validFromMs = now,
            expiresAtMs = now + durationMs,
            daysGranted = days,
            isBlocked = false,
            createdAt = now,
            notes = notes,
            mustChangePassword = true,
            hasCustomPassword = false,
            passwordChangedAt = null
        )
        userDao.insertUser(newUser)
        userTokenDao.insertToken(
            UserToken(
                token = cleanKey,
                userId = userId,
                username = cleanName,
                role = "OPERATOR",
                validFromMs = now,
                expiresAtMs = now + durationMs,
                daysGranted = days,
                isBlocked = false,
                notes = notes
            )
        )

        val ctx = context
        if (ctx != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    CloudSyncRepository.getInstance(ctx).pushUserUpdateToCloud(newUser)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return newUser
    }

    suspend fun updateCustomPassword(userId: String, newPassword: String): Result<Unit> {
        val cleanPassword = newPassword.trim()
        if (cleanPassword.length < 4) {
            return Result.failure(Exception("La contraseña debe tener al menos 4 caracteres."))
        }
        val now = System.currentTimeMillis()
        val user = userDao.getUserById(userId)
        if (user != null) {
            userTokenDao.deleteByUserId(userId)
            userTokenDao.insertToken(
                UserToken(
                    token = cleanPassword,
                    userId = user.id,
                    username = user.username,
                    role = user.role,
                    validFromMs = user.validFromMs,
                    expiresAtMs = user.expiresAtMs,
                    daysGranted = user.daysGranted,
                    isBlocked = user.isBlocked,
                    notes = user.notes
                )
            )
        }
        userDao.updatePersonalPassword(userId, cleanPassword, now)
        val cur = _currentUser.value
        if (cur != null && cur.id == userId) {
            val updated = cur.copy(
                token = cleanPassword,
                mustChangePassword = false,
                hasCustomPassword = true,
                passwordChangedAt = now
            )
            _currentUser.value = updated
            prefs?.edit()
                ?.putString("saved_username", updated.username)
                ?.putString("saved_key", cleanPassword)
                ?.apply()
        }
        return Result.success(Unit)
    }

    suspend fun generateBusinessProfileWithAi(rawIdea: String): Result<BusinessProfile> {
        val geminiService = com.example.service.GeminiSalesService()
        return geminiService.generateBusinessProfileFromIdea(rawIdea)
    }

    suspend fun saveCustomPreset(preset: com.example.data.model.BusinessPreset) {
        businessPresetDao.insertPreset(preset)
    }

    suspend fun deletePreset(preset: com.example.data.model.BusinessPreset) {
        businessPresetDao.deletePreset(preset)
    }

    suspend fun addDaysToUser(userId: String, extraDays: Int) {
        adjustUserDays(userId, extraDays)
    }

    suspend fun adjustUserDays(userId: String, dayDelta: Int) {
        val user = userDao.getUserById(userId) ?: return
        val deltaMillis = dayDelta.toLong() * 24L * 60L * 60L * 1000L
        val now = System.currentTimeMillis()

        val baseMs = if (user.expiresAtMs < now && dayDelta > 0) now else user.expiresAtMs
        val newExpiry = (baseMs + deltaMillis).coerceAtLeast(now - 1000L)
        val currentRemainingDays = if (user.expiresAtMs > now) {
            (user.expiresAtMs - now) / (1000L * 60 * 60 * 24)
        } else 0L
        val newDaysGranted = (currentRemainingDays + dayDelta).coerceAtLeast(0).toInt()

        val updatedUser = user.copy(
            expiresAtMs = newExpiry,
            daysGranted = newDaysGranted,
            isBlocked = if (newExpiry <= now) true else if (dayDelta > 0) false else user.isBlocked
        )
        userDao.updateUser(updatedUser)
        userTokenDao.extendTokenDays(user.token, deltaMillis, dayDelta)

        val ctx = context
        val cloudRepo = cloudTeamRepository
        if (updatedUser.email.isNotBlank() && cloudRepo != null) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudRepo.updateUserInCloud(updatedUser)
            }
        } else if (ctx != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    CloudSyncRepository.getInstance(ctx).pushUserUpdateToCloud(updatedUser)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (_currentUser.value?.id == userId) {
            _currentUser.value = updatedUser
        }
    }

    suspend fun updateUserLocally(updatedUser: UserAccount) {
        userDao.updateUser(updatedUser)
        if (updatedUser.email.isNotBlank()) {
            cloudTeamRepository?.let { repo ->
                CoroutineScope(Dispatchers.IO).launch {
                    repo.updateUserInCloud(updatedUser)
                }
            }
        }
        if (_currentUser.value?.id == updatedUser.id) {
            _currentUser.value = updatedUser
        }
    }

    suspend fun syncRemoteUserStatusLocally(username: String, isBlocked: Boolean, newExpiresAtMs: Long, newDays: Int) {
        val user = userDao.getUserByUsername(username) ?: return
        val updated = user.copy(
            isBlocked = isBlocked,
            expiresAtMs = if (newExpiresAtMs > 0) newExpiresAtMs else user.expiresAtMs,
            daysGranted = if (newDays > 0) newDays else user.daysGranted
        )
        userDao.updateUser(updated)
        if (_currentUser.value?.id == user.id) {
            _currentUser.value = updated
        }
    }

    suspend fun updateUserDetails(
        userId: String,
        newName: String,
        newNotes: String,
        remainingDays: Int,
        maxDevices: Int? = null
    ) {
        val user = userDao.getUserById(userId) ?: return
        val now = System.currentTimeMillis()
        val durationMillis = remainingDays.toLong() * 24L * 60L * 60L * 1000L
        val newExpiry = now + durationMillis
        val resolvedMax = (maxDevices ?: user.maxDevices).coerceIn(user.boundDeviceIds.size.coerceAtLeast(1), 5)

        val updatedUser = user.copy(
            username = newName.trim(),
            notes = newNotes.trim(),
            expiresAtMs = newExpiry,
            daysGranted = remainingDays,
            isBlocked = if (remainingDays <= 0) true else false,
            maxDevices = resolvedMax
        )
        userDao.updateUser(updatedUser)
        userTokenDao.deleteByUserId(userId)
        userTokenDao.insertToken(
            UserToken(
                token = updatedUser.token,
                userId = updatedUser.id,
                username = updatedUser.username,
                role = updatedUser.role,
                validFromMs = updatedUser.validFromMs,
                expiresAtMs = newExpiry,
                daysGranted = remainingDays,
                isBlocked = updatedUser.isBlocked,
                notes = updatedUser.notes
            )
        )

        val ctx = context
        val cloudRepo = cloudTeamRepository
        if (updatedUser.email.isNotBlank() && cloudRepo != null) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudRepo.updateUserInCloud(updatedUser)
            }
        } else if (ctx != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    CloudSyncRepository.getInstance(ctx).pushUserUpdateToCloud(updatedUser)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (_currentUser.value?.id == userId) {
            _currentUser.value = updatedUser
        }
    }

    suspend fun toggleBlockUser(userId: String, shouldBlock: Boolean) {
        userDao.setUserBlocked(userId, shouldBlock)
        userTokenDao.setTokenBlockedByUserId(userId, shouldBlock)
        val updatedUser = userDao.getUserById(userId)
        val ctx = context
        val cloudRepo = cloudTeamRepository
        if (updatedUser != null && updatedUser.email.isNotBlank() && cloudRepo != null) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudRepo.updateUserInCloud(updatedUser)
            }
        } else if (updatedUser != null && ctx != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    CloudSyncRepository.getInstance(ctx).pushUserUpdateToCloud(updatedUser)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updatedUser
        }
    }

    suspend fun setUserUpdateLock(
        userId: String,
        updateRequired: Boolean,
        requiredVersionCode: Int,
        reason: String,
        syncToCloud: Boolean = true
    ) {
        val cleanReason = reason.trim().take(300)
        val safeRequiredCode = requiredVersionCode.coerceIn(0, 100000)
        userDao.setUserUpdateLock(userId, updateRequired, safeRequiredCode, cleanReason)
        val updatedUser = userDao.getUserById(userId)
        val cloudRepo = cloudTeamRepository
        if (syncToCloud && updatedUser != null && updatedUser.email.isNotBlank() && cloudRepo != null) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudRepo.updateUserInCloud(updatedUser)
            }
        }
        if (_currentUser.value?.id == userId && updatedUser != null) {
            _currentUser.value = updatedUser
        }
    }

    suspend fun deleteUser(user: UserAccount) {
        userDao.deleteUser(user)
        userTokenDao.deleteByUserId(user.id)
        val ctx = context
        val cloudRepo = cloudTeamRepository
        if (user.email.isNotBlank() && cloudRepo != null) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudRepo.deleteUserFromCloud(user.email)
            }
        } else if (ctx != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    CloudSyncRepository.getInstance(ctx).deleteUserFromCloud(user.username)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (_currentUser.value?.id == user.id) {
            _currentUser.value = null
        }
    }

    suspend fun syncAllUsersFromCloud() {
        val ctx = context ?: return
        try {
            val cloudRepo = cloudTeamRepository
            if (cloudRepo != null && cloudRepo.getCurrentFirebaseUser() != null) {
                val remoteFirestoreUsers = cloudRepo.fetchAllAuthorizedUsersOnce()
                if (remoteFirestoreUsers.isNotEmpty()) {
                    userDao.insertAll(remoteFirestoreUsers)
                }
                return
            }
            val remoteUsers = CloudSyncRepository.getInstance(ctx).fetchAllUsersFromCloud()
            for (remote in remoteUsers) {
                if (remote.username.equals("Admin-payito", ignoreCase = true)) continue
                val existing = userDao.getUserByUsername(remote.username)
                if (existing == null) {
                    userDao.insertUser(remote)
                    userTokenDao.insertToken(
                        UserToken(
                            token = remote.token,
                            userId = remote.id,
                            username = remote.username,
                            role = remote.role,
                            validFromMs = remote.validFromMs,
                            expiresAtMs = remote.expiresAtMs,
                            daysGranted = remote.daysGranted,
                            isBlocked = remote.isBlocked,
                            notes = remote.notes
                        )
                    )
                } else {
                    val merged = existing.copy(
                        token = if (remote.token.isNotBlank()) remote.token else existing.token,
                        expiresAtMs = remote.expiresAtMs,
                        daysGranted = remote.daysGranted,
                        isBlocked = remote.isBlocked,
                        canAccessLegacyModules = remote.canAccessLegacyModules,
                        notes = remote.notes
                    )
                    userDao.updateUser(merged)
                    userTokenDao.setTokenBlockedByUserId(merged.id, merged.isBlocked)
                    if (_currentUser.value?.id == merged.id) {
                        _currentUser.value = merged
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRoleVisibilityConfig(): RoleVisibilityConfig {
        val p = context?.getSharedPreferences("como_on_role_visibility_prefs", Context.MODE_PRIVATE)
            ?: return RoleVisibilityConfig()
        return RoleVisibilityConfig(
            opPsaEnabled = p.getBoolean("opPsaEnabled", true),
            opScriptsEnabled = p.getBoolean("opScriptsEnabled", false),
            opAutomationEnabled = p.getBoolean("opAutomationEnabled", false),
            opDashboardEnabled = p.getBoolean("opDashboardEnabled", false),
            opAiSalesEnabled = p.getBoolean("opAiSalesEnabled", false),
            opPricingEditEnabled = p.getBoolean("opPricingEditEnabled", false),
            suPsaEnabled = p.getBoolean("suPsaEnabled", true),
            suScriptsEnabled = p.getBoolean("suScriptsEnabled", true),
            suAutomationEnabled = p.getBoolean("suAutomationEnabled", true),
            suDashboardEnabled = p.getBoolean("suDashboardEnabled", true),
            suSuperuserEnabled = p.getBoolean("suSuperuserEnabled", true)
        )
    }

    suspend fun saveRoleVisibilityConfig(config: RoleVisibilityConfig, globalLegacyEnabled: Boolean? = null) {
        val p = context?.getSharedPreferences("como_on_role_visibility_prefs", Context.MODE_PRIVATE)
        p?.edit()
            ?.putBoolean("opPsaEnabled", config.opPsaEnabled)
            ?.putBoolean("opScriptsEnabled", config.opScriptsEnabled)
            ?.putBoolean("opAutomationEnabled", config.opAutomationEnabled)
            ?.putBoolean("opDashboardEnabled", config.opDashboardEnabled)
            ?.putBoolean("opAiSalesEnabled", config.opAiSalesEnabled)
            ?.putBoolean("opPricingEditEnabled", config.opPricingEditEnabled)
            ?.putBoolean("suPsaEnabled", config.suPsaEnabled)
            ?.putBoolean("suScriptsEnabled", config.suScriptsEnabled)
            ?.putBoolean("suAutomationEnabled", config.suAutomationEnabled)
            ?.putBoolean("suDashboardEnabled", config.suDashboardEnabled)
            ?.putBoolean("suSuperuserEnabled", config.suSuperuserEnabled)
            ?.apply()

        val cloudRepo = cloudTeamRepository
        if (cloudRepo != null && cloudRepo.getCurrentFirebaseUser() != null) {
            cloudRepo.saveRoleMatrixToCloud(config, globalLegacyEnabled)
        } else {
            val ctx = context
            if (ctx != null) {
                try {
                    CloudSyncRepository.getInstance(ctx).pushRoleMatrixToCloud(config)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Sales Knowledge with Evolutionary Memory
    suspend fun addWinningSalesKnowledge(
        content: String,
        category: String = "GENERAL_WINNING",
        isQuestion: Boolean = false,
        triggerContext: String = ""
    ) {
        val item = SalesKnowledgeItem(
            category = category,
            triggerContext = triggerContext,
            winningContent = content.trim(),
            successScore = 2,
            isQuestion = isQuestion
        )
        salesKnowledgeDao.insertKnowledge(item)
        val ctx = context
        if (ctx != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    CloudSyncRepository.getInstance(ctx).pushSalesKnowledgeToCloud(item)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun rateSalesKnowledge(id: String, increment: Int = 1) {
        salesKnowledgeDao.incrementSuccessScore(id, increment)
        val updated = salesKnowledgeDao.getKnowledgeById(id)
        val ctx = context
        if (updated != null && ctx != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    CloudSyncRepository.getInstance(ctx).pushSalesKnowledgeToCloud(updated)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun deleteSalesKnowledge(id: String) {
        salesKnowledgeDao.deleteKnowledge(id)
    }

    suspend fun syncRemoteKnowledgeLocally(remoteItems: List<SalesKnowledgeItem>) {
        if (remoteItems.isNotEmpty()) {
            salesKnowledgeDao.insertKnowledgeList(remoteItems)
        }
    }

    suspend fun generateMemoryEnhancedAdvice(
        chatContext: String,
        geminiSalesService: com.example.service.GeminiSalesService
    ): Result<MemoryEnhancedSalesAdvice> {
        val profile = businessProfileDao.getProfile() ?: BusinessProfile(id = "primary_profile")
        val topKnowledge = salesKnowledgeDao.getTopWinningKnowledge(10)
        return geminiSalesService.generateMemoryEnhancedAdvice(
            chatContext = chatContext,
            profile = profile,
            memoryItems = topKnowledge
        )
    }

    // Scanned Chats Persistence
    suspend fun saveScannedChats(chats: List<ScannedChat>) {
        if (chats.isNotEmpty()) {
            scannedChatDao.insertChats(chats)
        }
    }

    suspend fun toggleChatSelection(id: String, selected: Boolean) {
        if (selected) {
            com.example.service.EzoAccessibilityService.unmarkRecipientProcessed(id)
        }
        scannedChatDao.setSelected(id, selected)
    }

    suspend fun setSelectedForChatIds(ids: List<String>, selected: Boolean) {
        if (ids.isNotEmpty()) {
            if (selected) {
                ids.forEach { com.example.service.EzoAccessibilityService.unmarkRecipientProcessed(it) }
            }
            scannedChatDao.setSelectedForIds(ids, selected)
        }
    }

    suspend fun setAllChatsSelected(selected: Boolean) {
        if (selected) {
            com.example.service.EzoAccessibilityService.clearProcessedRecipientsSession()
        }
        scannedChatDao.setAllSelected(selected)
    }

    suspend fun clearScannedChats() {
        scannedChatDao.clearAll()
    }

    suspend fun markChatSent(id: String) {
        scannedChatDao.markAsSent(id)
    }

    suspend fun deleteScannedChat(id: String) {
        scannedChatDao.deleteById(id)
    }

    // Scripts and Folders
    suspend fun addFolder(name: String, description: String, colorHex: String): Long {
        return scriptDao.insertFolder(
            ScriptFolder(
                name = name.trim(),
                description = description.trim(),
                colorHex = colorHex
            )
        )
    }

    suspend fun deleteFolder(folder: ScriptFolder) {
        scriptDao.deleteFolder(folder)
    }

    suspend fun addScript(folderId: Long, title: String, content: String, isAutoResponse: Boolean = false, triggerKeyword: String = ""): Long {
        return scriptDao.insertScript(
            ScriptItem(
                folderId = folderId,
                title = title.trim(),
                content = content.trim(),
                isAutoResponse = isAutoResponse,
                triggerKeyword = triggerKeyword.trim()
            )
        )
    }

    suspend fun deleteScript(script: ScriptItem) {
        scriptDao.deleteScript(script)
    }

    // Delivery Logs
    suspend fun logDelivery(
        recipientId: String,
        recipientPreview: String,
        crmFolder: String,
        scriptTitle: String,
        scriptSnippet: String,
        status: String,
        errorMessage: String? = null,
        delaySeconds: Int = 35
    ) {
        deliveryLogDao.insertLog(
            DeliveryLog(
                recipientId = recipientId,
                recipientPreview = recipientPreview,
                crmFolder = crmFolder,
                scriptTitle = scriptTitle,
                scriptSnippet = scriptSnippet,
                status = status,
                errorMessage = errorMessage,
                delaySeconds = delaySeconds,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearAllDeliveryLogs() {
        deliveryLogDao.clearLogs()
    }

    // Business Profile & AI Configuration
    suspend fun getBusinessProfile(): BusinessProfile {
        return businessProfileDao.getProfile() ?: BusinessProfile()
    }

    suspend fun saveBusinessProfile(profile: BusinessProfile) {
        businessProfileDao.saveProfile(profile.copy(lastUpdated = System.currentTimeMillis()))
    }

    suspend fun setAiEnabledForOperators(enabled: Boolean) {
        val current = getBusinessProfile()
        businessProfileDao.saveProfile(current.copy(isAiEnabledForOperators = enabled, lastUpdated = System.currentTimeMillis()))
    }

    fun getGeminiApiKey(): String {
        return prefs?.getString("custom_gemini_api_key", "")?.trim() ?: ""
    }

    fun saveGeminiApiKey(apiKey: String) {
        val cleanKey = apiKey.trim()
        prefs?.edit()?.putString("custom_gemini_api_key", cleanKey)?.apply()
        com.example.service.GeminiSalesService.customApiKey = cleanKey
    }

    // PSA Home Visits Operations
    suspend fun insertPsaVisit(visit: PsaHomeVisit) {
        val uid = cloudTeamRepository?.getCurrentFirebaseUser()?.uid.orEmpty()
        val withOwner = if (uid.isNotBlank()) visit.copy(ownerUid = uid, updatedAtMs = System.currentTimeMillis()) else visit
        psaVisitDao.insertVisit(withOwner)
        if (uid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudTeamRepository?.upsertSellerVisit(uid, withOwner)
                publishCurrentSellerStats()
            }
        }
    }

    suspend fun updatePsaVisit(visit: PsaHomeVisit) {
        val uid = cloudTeamRepository?.getCurrentFirebaseUser()?.uid.orEmpty()
        val withOwner = if (uid.isNotBlank()) visit.copy(ownerUid = uid, updatedAtMs = System.currentTimeMillis()) else visit
        psaVisitDao.updateVisit(withOwner)
        if (uid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudTeamRepository?.upsertSellerVisit(uid, withOwner)
                publishCurrentSellerStats()
            }
        }
    }

    suspend fun deletePsaVisit(visitId: String) {
        psaVisitDao.deleteVisitById(visitId)
        val uid = cloudTeamRepository?.getCurrentFirebaseUser()?.uid.orEmpty()
        if (uid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudTeamRepository?.deleteSellerVisit(uid, visitId)
                publishCurrentSellerStats()
            }
        }
    }

    // PSA Post-Sale Filter Replacements Operations
    suspend fun insertPostSaleInstallation(record: PsaPostSaleInstallation) {
        val uid = cloudTeamRepository?.getCurrentFirebaseUser()?.uid.orEmpty()
        val withOwner = if (uid.isNotBlank()) record.copy(ownerUid = uid, updatedAtMs = System.currentTimeMillis()) else record
        psaPostSaleDao.insertInstallation(withOwner)
        if (uid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudTeamRepository?.upsertPostSaleRecord(uid, withOwner)
                publishCurrentSellerStats()
            }
        }
    }

    suspend fun updatePostSaleInstallation(record: PsaPostSaleInstallation) {
        val uid = cloudTeamRepository?.getCurrentFirebaseUser()?.uid.orEmpty()
        val withOwner = if (uid.isNotBlank()) record.copy(ownerUid = uid, updatedAtMs = System.currentTimeMillis()) else record
        psaPostSaleDao.updateInstallation(withOwner)
        if (uid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudTeamRepository?.upsertPostSaleRecord(uid, withOwner)
                publishCurrentSellerStats()
            }
        }
    }

    suspend fun deletePostSaleInstallation(recordId: String) {
        psaPostSaleDao.deleteById(recordId)
        val uid = cloudTeamRepository?.getCurrentFirebaseUser()?.uid.orEmpty()
        if (uid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudTeamRepository?.deletePostSaleRecord(uid, recordId)
                publishCurrentSellerStats()
            }
        }
    }

    // PSA Contact Leads & Interactions Operations
    suspend fun insertPsaLead(lead: com.example.data.model.PsaContactLead) {
        psaContactDao.insertLead(lead)
        val uid = cloudTeamRepository?.getCurrentFirebaseUser()?.uid.orEmpty()
        if (uid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudTeamRepository?.upsertSellerContact(uid, lead)
                publishCurrentSellerStats()
            }
        }
    }

    suspend fun updatePsaLead(lead: com.example.data.model.PsaContactLead) {
        psaContactDao.updateLead(lead)
        val uid = cloudTeamRepository?.getCurrentFirebaseUser()?.uid.orEmpty()
        if (uid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudTeamRepository?.upsertSellerContact(uid, lead)
                publishCurrentSellerStats()
            }
        }
    }

    suspend fun deletePsaLead(lead: com.example.data.model.PsaContactLead) {
        psaContactDao.deleteLead(lead)
        val uid = cloudTeamRepository?.getCurrentFirebaseUser()?.uid.orEmpty()
        if (uid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudTeamRepository?.deleteSellerContact(uid, lead.id)
                publishCurrentSellerStats()
            }
        }
    }

    suspend fun deletePsaLeadById(id: String) {
        psaContactDao.deleteLeadById(id)
        val uid = cloudTeamRepository?.getCurrentFirebaseUser()?.uid.orEmpty()
        if (uid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudTeamRepository?.deleteSellerContact(uid, id)
                publishCurrentSellerStats()
            }
        }
    }

    suspend fun logContactInteraction(
        lead: com.example.data.model.PsaContactLead,
        channel: String, // "CALL", "MESSAGE"
        result: String,  // "SUCCESS", "NO_ANSWER", "RESCHEDULED", "REJECTED"
        note: String = "",
        rescheduleDate: String = ""
    ) {
        val dateKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val interaction = com.example.data.model.PsaContactInteraction(
            leadId = lead.id,
            leadName = lead.name,
            channel = channel,
            result = result,
            dateKey = dateKey,
            timestampMs = System.currentTimeMillis(),
            note = note
        )
        psaContactDao.insertInteraction(interaction)

        val newCalls = if (channel == "CALL") lead.callAttemptsCount + 1 else lead.callAttemptsCount
        val newMessages = if (channel == "MESSAGE") lead.messageAttemptsCount + 1 else lead.messageAttemptsCount
        val newStatus = when (result) {
            "SUCCESS" -> com.example.data.model.PsaLeadStatus.APPOINTMENT_SET
            "NO_ANSWER" -> com.example.data.model.PsaLeadStatus.NO_ANSWER
            "RESCHEDULED" -> com.example.data.model.PsaLeadStatus.RESCHEDULED
            "REJECTED" -> com.example.data.model.PsaLeadStatus.NOT_INTERESTED
            else -> lead.status
        }

        val updatedLead = lead.copy(
            callAttemptsCount = newCalls,
            messageAttemptsCount = newMessages,
            lastContactDateMs = System.currentTimeMillis(),
            lastContactChannel = channel,
            status = newStatus,
            rescheduleDate = if (rescheduleDate.isNotBlank()) rescheduleDate else lead.rescheduleDate,
            rescheduleNote = if (note.isNotBlank()) note else lead.rescheduleNote
        )
        psaContactDao.updateLead(updatedLead)
        val uid = cloudTeamRepository?.getCurrentFirebaseUser()?.uid.orEmpty()
        if (uid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                cloudTeamRepository?.upsertSellerContact(uid, updatedLead)
                publishCurrentSellerStats()
            }
        }
    }

    suspend fun batchUpdatePsaLeadsStatus(
        leadIds: List<String>,
        newStatus: com.example.data.model.PsaLeadStatus,
        rescheduleDate: String = "",
        rescheduleNote: String = ""
    ) {
        if (leadIds.isEmpty()) return
        val existingLeads = psaContactDao.getLeadsByIds(leadIds)
        if (existingLeads.isEmpty()) return
        val updated = existingLeads.map { lead ->
            lead.copy(
                status = newStatus,
                rescheduleDate = if (newStatus == com.example.data.model.PsaLeadStatus.RESCHEDULED && rescheduleDate.isNotBlank()) {
                    rescheduleDate.trim()
                } else {
                    lead.rescheduleDate
                },
                rescheduleNote = if (rescheduleNote.isNotBlank()) {
                    rescheduleNote.trim()
                } else {
                    lead.rescheduleNote
                }
            )
        }
        psaContactDao.updateLeads(updated)
        val uid = cloudTeamRepository?.getCurrentFirebaseUser()?.uid.orEmpty()
        if (uid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                updated.forEach { cloudTeamRepository?.upsertSellerContact(uid, it) }
                publishCurrentSellerStats()
            }
        }
    }

    suspend fun batchReschedulePsaLeads(
        leadIds: List<String>,
        rescheduleDate: String,
        rescheduleNote: String = ""
    ) {
        if (leadIds.isEmpty() || rescheduleDate.isBlank()) return
        val existingLeads = psaContactDao.getLeadsByIds(leadIds)
        if (existingLeads.isEmpty()) return
        val updated = existingLeads.map { lead ->
            lead.copy(
                status = com.example.data.model.PsaLeadStatus.RESCHEDULED,
                rescheduleDate = rescheduleDate.trim(),
                rescheduleNote = if (rescheduleNote.isNotBlank()) rescheduleNote.trim() else lead.rescheduleNote
            )
        }
        psaContactDao.updateLeads(updated)
        val uid = cloudTeamRepository?.getCurrentFirebaseUser()?.uid.orEmpty()
        if (uid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                updated.forEach { cloudTeamRepository?.upsertSellerContact(uid, it) }
                publishCurrentSellerStats()
            }
        }
    }

    suspend fun batchDeletePsaLeads(leadIds: List<String>) {
        if (leadIds.isEmpty()) return
        psaContactDao.deleteLeadsByIds(leadIds)
        val uid = cloudTeamRepository?.getCurrentFirebaseUser()?.uid.orEmpty()
        if (uid.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                leadIds.forEach { cloudTeamRepository?.deleteSellerContact(uid, it) }
                publishCurrentSellerStats()
            }
        }
    }

    // Per-Seller Cloud Cache Synchronization Helpers
    suspend fun wipeSellerLocalWorkspace() {
        psaVisitDao.clearAllVisits()
        psaContactDao.clearAllLeads()
        psaContactDao.clearAllInteractions()
        psaPostSaleDao.clearAll()
    }

    suspend fun wipeSellerLocalWorkspaceAndCaches() {
        wipeSellerLocalWorkspace()
        scannedChatDao.clearAll()
        prefs?.edit()?.remove("saved_username")?.remove("saved_key")?.apply()
        cloudTeamRepository?.setLastSyncedUid("")
        val ctx = context
        if (ctx != null) {
            com.example.util.LicenseSecurityGuard.clearState(ctx)
            com.example.util.PsaPricingCloudSyncHelper.clearSellerOverrideState(ctx)
        }
    }

    suspend fun replaceRemoteContacts(leads: List<PsaContactLead>) {
        psaContactDao.clearAllLeads()
        if (leads.isNotEmpty()) {
            psaContactDao.insertLeads(leads)
        }
    }

    suspend fun replaceRemoteVisits(visits: List<PsaHomeVisit>) {
        psaVisitDao.clearAllVisits()
        if (visits.isNotEmpty()) {
            psaVisitDao.insertVisits(visits)
        }
    }

    suspend fun replaceRemotePostSale(records: List<PsaPostSaleInstallation>) {
        psaPostSaleDao.clearAll()
        if (records.isNotEmpty()) {
            psaPostSaleDao.insertAll(records)
        }
    }

    suspend fun syncLocalWorkspaceUpToCloud(uid: String) {
        val cloudRepo = cloudTeamRepository ?: return
        if (uid.isBlank() || cloudRepo.getCurrentFirebaseUser()?.uid != uid) return
        val localLeads = psaContactDao.getAllLeads().first()
        localLeads.forEach { lead ->
            cloudRepo.upsertSellerContact(uid, lead)
        }
        val localVisits = psaVisitDao.getAllVisits().first()
        localVisits.forEach { visit ->
            val owned = if (visit.ownerUid != uid) visit.copy(ownerUid = uid) else visit
            if (owned !== visit) psaVisitDao.insertVisit(owned)
            cloudRepo.upsertSellerVisit(uid, owned)
        }
        val localPostSale = psaPostSaleDao.getAllInstallations().first()
        localPostSale.forEach { rec ->
            val owned = if (rec.ownerUid != uid) rec.copy(ownerUid = uid) else rec
            if (owned !== rec) psaPostSaleDao.insertInstallation(owned)
            cloudRepo.upsertPostSaleRecord(uid, owned)
        }
        publishCurrentSellerStats()
    }

    suspend fun publishCurrentSellerStats() {
        val cloudRepo = cloudTeamRepository ?: return
        val fbUser = cloudRepo.getCurrentFirebaseUser() ?: return
        val uid = fbUser.uid
        val curUser = _currentUser.value
        val leads = psaContactDao.getAllLeads().first()
        val visits = psaVisitDao.getAllVisits().first()
        val postSale = psaPostSaleDao.getAllInstallations().first()
        val nowMs = System.currentTimeMillis()
        val dueThresholdMs = nowMs + (15L * 86_400_000L)
        val pendingReplacementsCount = postSale.count {
            it.status == PsaPostSaleStatus.ACTIVO && it.nextReplacementAtMs <= dueThresholdMs
        }
        val completedDemosCount = visits.count { it.diagnosticTimestampMs > 0L || it.chlorinePpm > 0f || it.hardnessPpm > 0 }
        val closedSalesCount = visits.count { it.status == PsaVisitStatus.CERRADA_EXITO } + postSale.size

        val stats = SellerPerformanceStats(
            ownerUid = uid,
            sellerName = curUser?.username?.takeIf { it.isNotBlank() } ?: fbUser.displayName ?: "Vendedor PSA",
            sellerEmail = curUser?.email?.takeIf { it.isNotBlank() } ?: fbUser.email ?: "vendedor@psa.local",
            totalContacts = leads.size,
            totalVisits = visits.size,
            completedDemos = completedDemosCount,
            closedSales = closedSalesCount,
            pendingReplacements = pendingReplacementsCount
        )
        cloudRepo.upsertSellerStats(stats)
    }

    // Font Scale & Modo Chicato Persistence
    fun getFontScaleLevel(): com.example.ui.theme.FontScaleLevel {
        val name = prefs?.getString("font_scale_level", com.example.ui.theme.FontScaleLevel.NORMAL.name)
        return try {
            com.example.ui.theme.FontScaleLevel.valueOf(name ?: com.example.ui.theme.FontScaleLevel.NORMAL.name)
        } catch (e: Exception) {
            com.example.ui.theme.FontScaleLevel.NORMAL
        }
    }

    fun saveFontScaleLevel(level: com.example.ui.theme.FontScaleLevel) {
        prefs?.edit()?.putString("font_scale_level", level.name)?.apply()
    }

    fun hasSeenSetupGuide(): Boolean {
        return prefs?.getBoolean("has_seen_setup_guide", false) ?: false
    }

    fun saveHasSeenSetupGuide(seen: Boolean) {
        prefs?.edit()?.putBoolean("has_seen_setup_guide", seen)?.apply()
    }

    // PSA Financing & Cuotas Persistence
    fun getPsaFinancingConfig(): PsaFinancingConfig {
        val p = context?.getSharedPreferences("psa_financing_prefs", Context.MODE_PRIVATE)
        return PsaFinancingConfig(
            defaultInterest3Months = p?.getFloat("interest_3", 10f) ?: 10f,
            defaultInterest6Months = p?.getFloat("interest_6", 20f) ?: 20f,
            defaultInterest12Months = p?.getFloat("interest_12", 35f) ?: 35f,
            defaultInterest18Months = p?.getFloat("interest_18", 50f) ?: 50f,
            suggestedAdvancePercentage = p?.getFloat("advance_pct", 20f) ?: 20f
        )
    }

    fun savePsaFinancingConfig(config: PsaFinancingConfig) {
        val p = context?.getSharedPreferences("psa_financing_prefs", Context.MODE_PRIVATE)
        p?.edit()
            ?.putFloat("interest_3", config.defaultInterest3Months)
            ?.putFloat("interest_6", config.defaultInterest6Months)
            ?.putFloat("interest_12", config.defaultInterest12Months)
            ?.putFloat("interest_18", config.defaultInterest18Months)
            ?.putFloat("advance_pct", config.suggestedAdvancePercentage)
            ?.apply()
    }

    fun getApkDownloadUrl(): String {
        val stored = prefs?.getString(PREF_APK_DOWNLOAD_URL, DEFAULT_APK_DOWNLOAD_URL).orEmpty()
        val normalized = normalizeApkDownloadUrl(stored)
        if (normalized.isNotBlank()) return normalized
        return buildGithubDirectApkUrl(getGithubUsername(), getGithubRepo())
    }

    fun saveApkDownloadUrl(rawUrl: String): String {
        val normalized = normalizeApkDownloadUrl(rawUrl)
        prefs?.edit()?.putString(PREF_APK_DOWNLOAD_URL, normalized)?.apply()
        return normalized
    }

    fun getGithubUsername(): String {
        return cleanGithubOwner(prefs?.getString(PREF_GITHUB_USERNAME, DEFAULT_GITHUB_USERNAME).orEmpty())
    }

    fun getGithubRepo(): String {
        return cleanGithubRepo(prefs?.getString(PREF_GITHUB_REPO, DEFAULT_GITHUB_REPO).orEmpty())
    }

    fun saveGithubConfig(rawUsername: String, rawRepo: String): Triple<String, String, String> {
        val user = cleanGithubOwner(rawUsername)
        val repo = cleanGithubRepo(rawRepo)
        val autoApkUrl = buildGithubDirectApkUrl(user, repo)
        prefs?.edit()
            ?.putString(PREF_GITHUB_USERNAME, user)
            ?.putString(PREF_GITHUB_REPO, repo)
            ?.apply {
                if (autoApkUrl.isNotBlank()) {
                    putString(PREF_APK_DOWNLOAD_URL, autoApkUrl)
                }
            }
            ?.apply()
        return Triple(user, repo, autoApkUrl)
    }

    companion object {
        const val PREF_APK_DOWNLOAD_URL = "apk_download_url"
        const val PREF_GITHUB_USERNAME = "apk_github_username"
        const val PREF_GITHUB_REPO = "apk_github_repo"
        const val DEFAULT_APK_DOWNLOAD_URL = ""
        const val DEFAULT_GITHUB_USERNAME = "Elizondo17"
        const val DEFAULT_GITHUB_REPO = "Descargar-Como-On"
        private const val LEGACY_UNHOSTED_WEB_URL = "https://gen-lang-client-0791441970.web.app"

        private val DRIVE_FILE_D_REGEX = Regex("""drive\.google\.com/file/d/([a-zA-Z0-9_-]+)""")
        private val DRIVE_ID_QUERY_REGEX = Regex("""drive\.google\.com/(?:open|uc)\?(?:[^#]*&)?id=([a-zA-Z0-9_-]+)""")

        fun cleanGithubOwner(raw: String): String {
            var s = raw.trim()
                .removePrefix("https://github.com/")
                .removePrefix("http://github.com/")
                .removePrefix("github.com/")
                .removePrefix("@")
                .trim('/')
            if (s.contains("/")) {
                s = s.substringBefore("/")
            }
            return s.replace(" ", "")
        }

        fun cleanGithubRepo(raw: String): String {
            var s = raw.trim()
                .removePrefix("https://github.com/")
                .removePrefix("http://github.com/")
                .removePrefix("github.com/")
                .trim('/')
            if (s.contains("/")) {
                s = s.substringAfterLast("/")
            }
            return s.removeSuffix(".git").replace(" ", "")
        }

        fun buildGithubDirectApkUrl(rawUsername: String, rawRepo: String): String {
            val user = cleanGithubOwner(rawUsername)
            val repo = cleanGithubRepo(rawRepo)
            if (user.isBlank() || repo.isBlank()) return ""
            return "https://github.com/$user/$repo/releases/latest/download/ComoOn-PSA.apk"
        }

        fun buildGithubPagesUrl(rawUsername: String, rawRepo: String): String {
            val user = cleanGithubOwner(rawUsername)
            val repo = cleanGithubRepo(rawRepo)
            if (user.isBlank() || repo.isBlank()) return ""
            return "https://${user.lowercase()}.github.io/$repo/"
        }

        fun normalizeApkDownloadUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            if (trimmed.isBlank() || trimmed.trimEnd('/').equals(LEGACY_UNHOSTED_WEB_URL, ignoreCase = true)) {
                return DEFAULT_APK_DOWNLOAD_URL
            }
            val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "https://$trimmed"
            }

            // Convert Google Drive share/view links into direct-download URLs
            val fileMatch = DRIVE_FILE_D_REGEX.find(withScheme)
            if (fileMatch != null) {
                return "https://drive.google.com/uc?export=download&id=${fileMatch.groupValues[1]}"
            }
            val idQueryMatch = DRIVE_ID_QUERY_REGEX.find(withScheme)
            if (idQueryMatch != null) {
                return "https://drive.google.com/uc?export=download&id=${idQueryMatch.groupValues[1]}"
            }

            return withScheme.trimEnd('/')
        }

        fun buildOperatorInviteMessage(
            username: String,
            email: String,
            days: Int,
            maxDevices: Int,
            apkDownloadUrl: String
        ): String {
            val cleanUrl = normalizeApkDownloadUrl(apkDownloadUrl)
            val downloadSection = if (cleanUrl.isNotBlank()) {
                "📥 Descarga e instala la app aquí:\n$cleanUrl\n\n" +
                    "👉 Instala el APK, abre Como On y toca 'Continuar con Google' usando tu correo autorizado."
            } else {
                "📥 Instala el archivo ComoOn-PSA.apk que te comparto y abre Como On tocando 'Continuar con Google' con tu correo autorizado."
            }
            return "📲 Acceso Autorizado a Como On PSA\n" +
                "👤 Vendedor: ${username.trim()}\n" +
                "📧 Cuenta Google: ${email.trim().lowercase()}\n" +
                "📱 Dispositivos permitidos: ${maxDevices.coerceAtLeast(1)}\n" +
                "⏳ Vigencia en nube: $days días\n\n" +
                downloadSection
        }

        fun buildApkOnlyShareMessage(apkDownloadUrl: String): String {
            val cleanUrl = normalizeApkDownloadUrl(apkDownloadUrl)
            if (cleanUrl.isBlank()) {
                return "📲 Como On PSA (APK Oficial)\n\n👉 Te comparto el archivo instalador ComoOn-PSA.apk. Ábrelo en tu Android y entra con 'Continuar con Google'."
            }
            return "📲 Descarga Como On PSA (APK Oficial)\n\n" +
                "🔗 Descarga directa en 1 toque:\n$cleanUrl\n\n" +
                "👉 Descarga e instala en tu Android y entra con 'Continuar con Google'."
        }
    }
}

data class PsaFinancingConfig(
    val defaultInterest3Months: Float = 10f,
    val defaultInterest6Months: Float = 20f,
    val defaultInterest12Months: Float = 35f,
    val defaultInterest18Months: Float = 50f,
    val suggestedAdvancePercentage: Float = 20f
)
