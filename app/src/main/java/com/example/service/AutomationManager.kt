package com.example.service

import com.example.data.model.AutomationConfig
import com.example.data.model.AutomationState
import com.example.data.model.LiveAutomationProgress
import com.example.data.model.ScannedChat
import com.example.data.model.ScriptItem
import com.example.data.repository.EzoRepository
import com.example.util.UnattendedAutomationLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class AutomationManager(
    private val repository: EzoRepository,
    private val notificationHelper: NotificationHelper
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var automationJob: Job? = null
    private val geminiService = GeminiSalesService(customApiKeyProvider = { repository.getGeminiApiKey() })

    private val _progress = MutableStateFlow(LiveAutomationProgress())
    val progress = _progress.asStateFlow()

    private val _config = MutableStateFlow(AutomationConfig())
    val config = _config.asStateFlow()

    init {
        // Wire up callbacks from floating overlay
        EzoAccessibilityService.onStartAutomationRequested = {
            scope.launch {
                val scripts = repository.allScripts.first()
                val targetScript = scripts.firstOrNull { it.id == _config.value.selectedScriptId }
                    ?: scripts.firstOrNull()
                if (targetScript != null) {
                    startCampaign(targetScript)
                } else {
                    _progress.value = _progress.value.copy(
                        state = AutomationState.ERROR,
                        lastActionMessage = "Por favor selecciona o crea un script primero."
                    )
                }
            }
        }

        EzoAccessibilityService.onPauseAutomationRequested = {
            pauseCampaign()
        }

        EzoAccessibilityService.onStopAutomationRequested = {
            stopCampaign()
        }
    }

    fun updateConfig(newConfig: AutomationConfig) {
        _config.value = newConfig
    }

    fun startCampaign(selectedScript: ScriptItem) {
        if (_progress.value.state == AutomationState.WAITING_DELAY ||
            _progress.value.state == AutomationState.SENDING ||
            _progress.value.state == AutomationState.SCANNING
        ) {
            return
        }

        val currentUser = repository.currentUser.value
        if (currentUser == null || currentUser.isBlocked || currentUser.isExpired) {
            _progress.value = _progress.value.copy(
                state = AutomationState.ERROR,
                lastActionMessage = "Acceso bloqueado o expirado. Contacta al administrador."
            )
            return
        }

        val currentCfg = _config.value
        val cal = java.util.Calendar.getInstance()
        val curHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val curMin = cal.get(java.util.Calendar.MINUTE)

        if (currentCfg.nightQuietHoursEnabled && !currentCfg.isCurrentTimeAllowed(curHour, curMin)) {
            val startStr = String.format(java.util.Locale.ROOT, "%02d:%02d", currentCfg.allowedStartHour, currentCfg.allowedStartMinute)
            val endStr = String.format(java.util.Locale.ROOT, "%02d:%02d", currentCfg.allowedEndHour, currentCfg.allowedEndMinute)
            _progress.value = _progress.value.copy(
                state = AutomationState.ERROR,
                lastActionMessage = "Fuera del horario permitido ($startStr - $endStr). Desactiva la protección nocturna o espera al horario hábil."
            )
            EzoAccessibilityService.instance?.updateFloatingStatus("Fuera de horario ($startStr-$endStr)")
            return
        }

        automationJob?.cancel()
        automationJob = scope.launch {
            runCampaignLoop(selectedScript)
        }
    }

    fun pauseCampaign() {
        if (_progress.value.state != AutomationState.IDLE && _progress.value.state != AutomationState.COMPLETED) {
            automationJob?.cancel()
            _progress.value = _progress.value.copy(
                state = AutomationState.PAUSED,
                lastActionMessage = "Campaña pausada."
            )
            EzoAccessibilityService.instance?.updateFloatingStatus("Pausado")
            EzoAccessibilityService.instance?.hideDimOverlay()
        }
    }

    fun stopCampaign() {
        automationJob?.cancel()
        EzoAccessibilityService.clearProcessedRecipientsSession()
        _progress.value = LiveAutomationProgress(
            state = AutomationState.IDLE,
            lastActionMessage = "Campaña detenida."
        )
        EzoAccessibilityService.instance?.updateFloatingStatus("Detenido")
        EzoAccessibilityService.instance?.hideDimOverlay()
    }

    private suspend fun runCampaignLoop(script: ScriptItem) {
        val currentCfg = _config.value
        val accessibilityService = EzoAccessibilityService.instance
        val useRealAccessibility = accessibilityService != null

        // 0. Verify authorized active user account
        val user = repository.currentUser.value
        if (user == null || user.isBlocked || user.isExpired) {
            _progress.value = _progress.value.copy(
                state = AutomationState.ERROR,
                lastActionMessage = "Usuario no habilitado o suscripción vencida. Inicia sesión con una cuenta activa."
            )
            accessibilityService?.updateFloatingStatus("Acceso no autorizado")
            return
        }

        // Fetch selected chats from database for this round that are not in active session memory
        val allSelected = repository.selectedScannedChats.first()
        val candidateRecipients = allSelected.filter {
            it.isSelected &&
            !EzoAccessibilityService.isRecipientProcessed(it.id) &&
            !EzoAccessibilityService.isRecipientProcessed(it.phoneNumber) &&
            !EzoAccessibilityService.isRecipientProcessed(it.name)
        }

        if (candidateRecipients.isEmpty()) {
            _progress.value = _progress.value.copy(
                state = AutomationState.ERROR,
                lastActionMessage = if (allSelected.any { it.isSent }) {
                    "Todos los chats de esta ronda ya recibieron su mensaje. Marca los contactos que desees para una nueva ronda."
                } else {
                    "No hay chats seleccionados. Marca las casillas de los contactos para iniciar la ronda."
                }
            )
            accessibilityService?.updateFloatingStatus("Sin chats pendientes")
            return
        }

        _progress.value = _progress.value.copy(
            state = AutomationState.SCANNING,
            totalChatsFound = candidateRecipients.size,
            lastActionMessage = "Iniciando envío a ${candidateRecipients.size} chats seleccionados...",
            currentFolder = currentCfg.targetCrmFolder
        )

        delay(800)

        var campaignSuccessCount = 0
        var campaignFailureCount = 0

        for ((index, chat) in candidateRecipients.withIndex()) {
            val contactName = chat.name.ifBlank { chat.phoneNumber }
            val recipientId = chat.phoneNumber.ifBlank { chat.id }

            // CRITICAL: Live Security Kill Switch - Block & Expiration enforcement
            val currentUser = repository.currentUser.value
            if (currentUser == null || currentUser.isBlocked || currentUser.isExpired) {
                val blockReason = if (currentUser?.isBlocked == true) {
                    "Acceso Revocado: Tu cuenta ha sido bloqueada por el administrador."
                } else {
                    "Acceso Expirado: El periodo de tu licencia ha concluido."
                }
                _progress.value = _progress.value.copy(
                    state = AutomationState.PAUSED,
                    lastActionMessage = blockReason
                )
                accessibilityService?.updateFloatingStatus("SUSPENDIDO (Bloqueo)")
                accessibilityService?.hideDimOverlay()
                stopCampaign()
                return
            }

            // Check night quiet hours before processing each message
            val calNow = java.util.Calendar.getInstance()
            val nowH = calNow.get(java.util.Calendar.HOUR_OF_DAY)
            val nowM = calNow.get(java.util.Calendar.MINUTE)
            if (currentCfg.nightQuietHoursEnabled && !currentCfg.isCurrentTimeAllowed(nowH, nowM)) {
                val endStr = String.format(java.util.Locale.ROOT, "%02d:%02d", currentCfg.allowedEndHour, currentCfg.allowedEndMinute)
                _progress.value = _progress.value.copy(
                    state = AutomationState.PAUSED,
                    lastActionMessage = "Pausado automáticamente: horario nocturno alcanzado ($endStr)."
                )
                accessibilityService?.updateFloatingStatus("Pausado (horario nocturno)")
                accessibilityService?.hideDimOverlay()
                return
            }

            if (currentCfg.dimScreenWhileAutomating) {
                accessibilityService?.showDimOverlay("Enviando ${index + 1}/${candidateRecipients.size} a $contactName")
            }

            val minDelay = currentCfg.minDelaySeconds
            val maxDelay = currentCfg.maxDelaySeconds

            // Explicit check: Ensure focus remains strictly within the CRM application context during automation loops
            if (useRealAccessibility && accessibilityService != null && !accessibilityService.isApplicationContext()) {
                val pauseMessage = "Pausa preventiva: el CRM no está enfocado en pantalla. Abre el CRM y reanuda."
                _progress.value = _progress.value.copy(
                    state = AutomationState.PAUSED,
                    lastActionMessage = pauseMessage
                )
                accessibilityService.updateFloatingStatus("Pausado (CRM fuera de foco)")
                accessibilityService.hideDimOverlay()
                notificationHelper.showFailureNotification(
                    recipient = "$contactName ($recipientId)",
                    reason = pauseMessage
                )
                return
            }

            // 1. Anti-spam delay pacing countdown
            _progress.value = _progress.value.copy(
                state = AutomationState.WAITING_DELAY,
                currentChatIndex = index + 1,
                currentRecipient = "$contactName ($recipientId)",
                lastActionMessage = "Pausa anti-spam activa para proteger tu cuenta..."
            )

            accessibilityService?.applicationContext?.let { ctx ->
                UnattendedAutomationLogger.logEvent(
                    context = ctx,
                    tag = "PAUSA_ANTI_SPAM",
                    recipient = "$contactName ($recipientId)",
                    status = "EN_ESPERA",
                    exitedChatSuccessfully = true,
                    details = "Iniciando cuenta regresiva de pausa anti-spam (${minDelay}-${maxDelay}s)"
                )
            }

            val delayDuration = if (accessibilityService != null) {
                accessibilityService.awaitAntiSpamDelay(minDelay, maxDelay) { sec ->
                    _progress.value = _progress.value.copy(secondsRemaining = sec)
                    accessibilityService.updateLiveProgress(
                        sentCount = campaignSuccessCount,
                        totalCount = candidateRecipients.size,
                        nextDelaySeconds = sec
                    )
                }
            } else {
                val jitter = Random.nextInt(minDelay.coerceAtLeast(20), maxDelay.coerceAtLeast(minDelay) + 1)
                for (sec in jitter downTo 1) {
                    _progress.value = _progress.value.copy(secondsRemaining = sec)
                    delay(1000)
                }
                jitter
            }

            // 2. Message sending execution
            _progress.value = _progress.value.copy(
                state = AutomationState.SENDING,
                secondsRemaining = 0,
                lastActionMessage = "Escribiendo y enviando mensaje a $contactName..."
            )
            accessibilityService?.updateFloatingStatus("Enviando a $contactName (${index + 1}/${candidateRecipients.size})")
            accessibilityService?.updateLiveProgress(
                sentCount = campaignSuccessCount,
                totalCount = candidateRecipients.size,
                nextDelaySeconds = 0,
                statusLabel = "🚀 Enviando a $contactName"
            )

            val personalizedMessage = if (currentCfg.useAiSmartResponse) {
                val profile = repository.getBusinessProfile()
                val chatContext = chat.lastMessage.ifBlank { "Hola, me gustaría más información" }
                val aiResult = geminiService.generateSmartResponses(chatContext, profile)
                val responseOptions = aiResult.getOrNull()
                val selectedOption = when (currentCfg.aiResponseStrategy) {
                    "Cálido y Empático" -> responseOptions?.find { it.title.contains("Cálido", ignoreCase = true) }
                    "Pregunta Estratégica" -> responseOptions?.find { it.title.contains("Pregunta", ignoreCase = true) }
                    else -> responseOptions?.find { it.title.contains("Cierre", ignoreCase = true) }
                } ?: responseOptions?.firstOrNull()

                val aiText = selectedOption?.text?.trim()
                if (!aiText.isNullOrBlank()) {
                    com.example.util.TextTemplateParser.process(
                        template = aiText,
                        contactName = contactName,
                        contactPhone = recipientId
                    )
                } else {
                    com.example.util.TextTemplateParser.process(
                        template = script.content,
                        contactName = contactName,
                        contactPhone = recipientId
                    )
                }
            } else {
                com.example.util.TextTemplateParser.process(
                    template = script.content,
                    contactName = contactName,
                    contactPhone = recipientId
                )
            }

            val (isSuccess, failureReason) = if (useRealAccessibility && accessibilityService != null) {
                accessibilityService.performChatDelivery(
                    recipientName = contactName,
                    recipientPhone = recipientId,
                    messageText = personalizedMessage
                )
            } else {
                delay(1200)
                Pair(true, null)
            }

            // Explicit check: Ensure focus remains strictly within the CRM environment after navigating between chats
            if (useRealAccessibility && accessibilityService != null && !accessibilityService.isApplicationContext()) {
                delay(1000)
                if (!accessibilityService.isApplicationContext()) {
                    val pauseMessage = "Pausa preventiva: se perdió el contexto del CRM tras navegar entre chats. Abre el CRM y reanuda."
                    _progress.value = _progress.value.copy(
                        state = AutomationState.PAUSED,
                        lastActionMessage = pauseMessage
                    )
                    accessibilityService.updateFloatingStatus("Pausado (CRM fuera de foco)")
                    accessibilityService.hideDimOverlay()
                    notificationHelper.showFailureNotification(
                        recipient = "$contactName ($recipientId)",
                        reason = pauseMessage
                    )
                    return
                }
            }

            // DUPLICATE PREVENTION AFTER SENDING:
            // User requirement: "Desmarcarlo en la ronda y desactivarlo luego de enviar el mensaje, quitarlo de la lista"
            if (isSuccess) {
                // 1. Lock in memory immediately so it is never targeted again in this session
                EzoAccessibilityService.markRecipientProcessed(contactName)
                EzoAccessibilityService.markRecipientProcessed(recipientId)
                EzoAccessibilityService.markRecipientProcessed(chat.id)
                val digits = recipientId.filter { it.isDigit() }
                if (digits.length >= 6) {
                    EzoAccessibilityService.markRecipientProcessed(digits)
                }

                // 2. Remove sent chat from list in Como On so it disappears and avoids any duplicate delivery
                repository.deleteScannedChat(chat.id)
                EzoAccessibilityService.updateSelectedChatsCount(candidateRecipients.size - (index + 1))
            } else if (failureReason?.contains("CONTACT_NOT_") == true) {
                // User requirement: Do NOT remove or uncheck the contact from the list if not found due to network delays.
                // Keep the contact intact for future attempts and notify status.
                _progress.value = _progress.value.copy(
                    lastActionMessage = "Contacto $contactName no cargó a tiempo en CRM (red lenta). Conservado en lista. Pasando al siguiente..."
                )
            } else {
                // Keep the contact intact for future attempts and notify status.
                _progress.value = _progress.value.copy(
                    lastActionMessage = failureReason ?: "No se pudo entregar el mensaje a $contactName. Continuando..."
                )
            }

            // 3. Handle pause safely if required (only if stuck inside a conversation or minimized)
            if (!isSuccess && failureReason?.contains("CONTACT_NOT_") != true &&
                (failureReason?.contains("salir del chat") == true ||
                 failureReason?.contains("APP_MINIMIZED") == true ||
                 failureReason?.contains("STILL_IN_CHAT") == true ||
                 failureReason?.contains("CHAT_LIST_NOT_DETECTED") == true ||
                 failureReason?.contains("minimizó") == true)) {
                val pauseMessage = when {
                    failureReason.contains("APP_MINIMIZED") || failureReason.contains("minimizó") ->
                        "Pausa de seguridad: la app CRM se minimizó o fue al inicio. Abre la lista de contactos del CRM y reanuda."
                    failureReason.contains("CHAT_LIST_NOT_DETECTED") ->
                        "Pausa de seguridad: no se confirmó la lista de contactos del CRM. Vuelve a la lista y reanuda."
                    else ->
                        "Pausa de seguridad: no se pudo salir del chat para evitar duplicados. Regresa a la lista del CRM y reanuda."
                }
                _progress.value = _progress.value.copy(
                    state = AutomationState.PAUSED,
                    lastActionMessage = pauseMessage
                )
                accessibilityService?.updateFloatingStatus("Pausado (seguridad)")
                accessibilityService?.updateLiveProgress(
                    sentCount = campaignSuccessCount,
                    totalCount = candidateRecipients.size,
                    nextDelaySeconds = 0,
                    statusLabel = "⏸ Pausado por seguridad"
                )
                accessibilityService?.hideDimOverlay()
                accessibilityService?.playSafetyAlertSound()
                accessibilityService?.vibrateSafetyAlert()
                notificationHelper.showFailureNotification(
                    recipient = "$contactName ($recipientId)",
                    reason = pauseMessage
                )
                return
            }

            // 4. Log delivery result in database
            val statusStr = if (isSuccess) "SUCCESS" else "FAILED"
            repository.logDelivery(
                recipientId = recipientId,
                recipientPreview = contactName,
                crmFolder = currentCfg.targetCrmFolder,
                scriptTitle = if (currentCfg.useAiSmartResponse) "Respuesta Inteligente IA (${currentCfg.aiResponseStrategy})" else script.title,
                scriptSnippet = personalizedMessage.take(60) + "...",
                status = statusStr,
                errorMessage = failureReason,
                delaySeconds = delayDuration
            )

            // 5. Notifications
            if (isSuccess) {
                campaignSuccessCount++
                if (currentCfg.notifyOnEverySuccess) {
                    notificationHelper.showSuccessNotification(
                        recipient = "$contactName ($recipientId)",
                        delaySeconds = delayDuration,
                        snippet = personalizedMessage.take(50) + "..."
                    )
                }
            } else if (failureReason?.contains("CONTACT_NOT_") == true) {
                // Do not alarm user with failure notifications for network delays; contact is kept for later
            } else {
                campaignFailureCount++
                notificationHelper.showFailureNotification(
                    recipient = "$contactName ($recipientId)",
                    reason = failureReason ?: "Error al enviar mensaje"
                )
            }

            delay(600)
        }

        // Round completed: clear session memory so contacts can be selected again for future rounds
        EzoAccessibilityService.clearProcessedRecipientsSession()

        // Show clean consolidated summary notification upon completion
        notificationHelper.showCampaignSummaryNotification(
            totalSent = campaignSuccessCount,
            totalFailed = campaignFailureCount
        )

        _progress.value = _progress.value.copy(
            state = AutomationState.COMPLETED,
            lastActionMessage = "¡Campaña finalizada con éxito! Se enviaron $campaignSuccessCount mensajes."
        )
        accessibilityService?.updateFloatingStatus("¡Campaña finalizada!")
        accessibilityService?.updateLiveProgress(
            sentCount = campaignSuccessCount,
            totalCount = candidateRecipients.size,
            nextDelaySeconds = 0,
            statusLabel = "✅ ¡Finalizado ($campaignSuccessCount)!"
        )
        accessibilityService?.hideDimOverlay()
        accessibilityService?.vibrateCompletion()

        accessibilityService?.applicationContext?.let { ctx ->
            UnattendedAutomationLogger.logEvent(
                context = ctx,
                tag = "CAMPANA_FINALIZADA",
                recipient = "TODOS",
                status = "COMPLETADO",
                exitedChatSuccessfully = true,
                details = "Campaña completada. Total enviados: $campaignSuccessCount, fallidos: $campaignFailureCount"
            )
        }
    }
}
