package com.example.service

import com.example.data.model.AutomationConfig
import com.example.data.model.AutomationState
import com.example.data.model.LiveAutomationProgress
import com.example.data.model.ScriptItem
import com.example.data.repository.EzoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Collections
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

    companion object {
        private val processedRecipientsInSession: MutableSet<String> =
            Collections.synchronizedSet(mutableSetOf())

        private fun normalizeRecipientKeys(identifier: String): Set<String> {
            val trimmed = identifier.trim().lowercase()
            if (trimmed.isEmpty()) return emptySet()
            val digits = trimmed.filter { it.isDigit() }
            return if (digits.length >= 7) {
                setOf(trimmed, digits)
            } else {
                setOf(trimmed)
            }
        }

        fun clearProcessedRecipientsSession() {
            processedRecipientsInSession.clear()
        }

        fun markRecipientProcessed(identifier: String) {
            processedRecipientsInSession.addAll(normalizeRecipientKeys(identifier))
        }

        fun unmarkRecipientProcessed(identifier: String) {
            processedRecipientsInSession.removeAll(normalizeRecipientKeys(identifier))
        }

        fun isRecipientProcessed(identifier: String): Boolean {
            return normalizeRecipientKeys(identifier).any { processedRecipientsInSession.contains(it) }
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
        }
    }

    fun stopCampaign() {
        automationJob?.cancel()
        clearProcessedRecipientsSession()
        _progress.value = LiveAutomationProgress(
            state = AutomationState.IDLE,
            lastActionMessage = "Campaña detenida."
        )
    }

    private suspend fun runCampaignLoop(script: ScriptItem) {
        val currentCfg = _config.value

        val user = repository.currentUser.value
        if (user == null || user.isBlocked || user.isExpired) {
            _progress.value = _progress.value.copy(
                state = AutomationState.ERROR,
                lastActionMessage = "Usuario no habilitado o suscripción vencida. Inicia sesión con una cuenta activa."
            )
            return
        }

        val allSelected = repository.selectedScannedChats.first()
        val candidateRecipients = allSelected.filter {
            it.isSelected &&
                !isRecipientProcessed(it.id) &&
                !isRecipientProcessed(it.phoneNumber) &&
                !isRecipientProcessed(it.name)
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
            return
        }

        _progress.value = _progress.value.copy(
            state = AutomationState.SCANNING,
            totalChatsFound = candidateRecipients.size,
            lastActionMessage = "Iniciando preparación para ${candidateRecipients.size} contactos seleccionados...",
            currentFolder = currentCfg.targetCrmFolder
        )

        delay(600)

        var campaignSuccessCount = 0
        val campaignFailureCount = 0

        for ((index, chat) in candidateRecipients.withIndex()) {
            val contactName = chat.name.ifBlank { chat.phoneNumber }
            val recipientId = chat.phoneNumber.ifBlank { chat.id }

            val activeUser = repository.currentUser.value
            if (activeUser == null || activeUser.isBlocked || activeUser.isExpired) {
                val blockReason = if (activeUser?.isBlocked == true) {
                    "Acceso Revocado: Tu cuenta ha sido bloqueada por el administrador."
                } else {
                    "Acceso Expirado: El periodo de tu licencia ha concluido."
                }
                _progress.value = _progress.value.copy(
                    state = AutomationState.PAUSED,
                    lastActionMessage = blockReason
                )
                stopCampaign()
                return
            }

            val calNow = java.util.Calendar.getInstance()
            val nowH = calNow.get(java.util.Calendar.HOUR_OF_DAY)
            val nowM = calNow.get(java.util.Calendar.MINUTE)
            if (currentCfg.nightQuietHoursEnabled && !currentCfg.isCurrentTimeAllowed(nowH, nowM)) {
                val endStr = String.format(java.util.Locale.ROOT, "%02d:%02d", currentCfg.allowedEndHour, currentCfg.allowedEndMinute)
                _progress.value = _progress.value.copy(
                    state = AutomationState.PAUSED,
                    lastActionMessage = "Pausado automáticamente: horario nocturno alcanzado ($endStr)."
                )
                return
            }

            val minDelay = currentCfg.minDelaySeconds
            val maxDelay = currentCfg.maxDelaySeconds

            _progress.value = _progress.value.copy(
                state = AutomationState.WAITING_DELAY,
                currentChatIndex = index + 1,
                currentRecipient = "$contactName ($recipientId)",
                lastActionMessage = "Pausa anti-spam activa para proteger tu cuenta..."
            )

            val safeMin = minDelay.coerceAtLeast(1)
            val safeMax = maxDelay.coerceAtLeast(safeMin)
            val delayDuration = Random.nextInt(safeMin, safeMax + 1)
            for (sec in delayDuration downTo 1) {
                _progress.value = _progress.value.copy(secondsRemaining = sec)
                delay(1000)
            }

            _progress.value = _progress.value.copy(
                state = AutomationState.SENDING,
                secondsRemaining = 0,
                lastActionMessage = "Procesando mensaje para $contactName..."
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

            delay(800)

            markRecipientProcessed(contactName)
            markRecipientProcessed(recipientId)
            markRecipientProcessed(chat.id)
            val digits = recipientId.filter { it.isDigit() }
            if (digits.length >= 6) {
                markRecipientProcessed(digits)
            }

            repository.deleteScannedChat(chat.id)

            repository.logDelivery(
                recipientId = recipientId,
                recipientPreview = contactName,
                crmFolder = currentCfg.targetCrmFolder,
                scriptTitle = if (currentCfg.useAiSmartResponse) "Respuesta Inteligente IA (${currentCfg.aiResponseStrategy})" else script.title,
                scriptSnippet = personalizedMessage.take(60) + "...",
                status = "SUCCESS",
                errorMessage = null,
                delaySeconds = delayDuration
            )

            campaignSuccessCount++
            if (currentCfg.notifyOnEverySuccess) {
                notificationHelper.showSuccessNotification(
                    recipient = "$contactName ($recipientId)",
                    delaySeconds = delayDuration,
                    snippet = personalizedMessage.take(50) + "..."
                )
            }

            delay(400)
        }

        clearProcessedRecipientsSession()

        notificationHelper.showCampaignSummaryNotification(
            totalSent = campaignSuccessCount,
            totalFailed = campaignFailureCount
        )

        _progress.value = _progress.value.copy(
            state = AutomationState.COMPLETED,
            lastActionMessage = "¡Campaña finalizada con éxito! Se procesaron $campaignSuccessCount mensajes."
        )
    }
}
