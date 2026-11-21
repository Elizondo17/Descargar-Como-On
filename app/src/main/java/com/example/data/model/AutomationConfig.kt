package com.example.data.model

enum class AutomationState {
    IDLE,
    SCANNING,
    WAITING_DELAY,
    SENDING,
    PAUSED,
    COMPLETED,
    ERROR
}

data class AutomationConfig(
    val targetCrmFolder: String = "Calientes MOP1",
    val targetDateFilter: String = "16 oct",
    val onlyUnread24h: Boolean = true,
    val minDelaySeconds: Int = 30,
    val maxDelaySeconds: Int = 45,
    val selectedScriptId: Long = 0L,
    val isSimulationMode: Boolean = false,
    val nightQuietHoursEnabled: Boolean = true,
    val allowedStartHour: Int = 9,
    val allowedStartMinute: Int = 0,
    val allowedEndHour: Int = 20,
    val allowedEndMinute: Int = 0,
    val dimScreenWhileAutomating: Boolean = false,
    val useAiSmartResponse: Boolean = false,
    val aiResponseStrategy: String = "Cierre Comercial",
    val notifyOnEverySuccess: Boolean = false
) {
    fun isCurrentTimeAllowed(currentHour: Int, currentMinute: Int): Boolean {
        if (!nightQuietHoursEnabled) return true
        val currentTotal = currentHour * 60 + currentMinute
        val startTotal = allowedStartHour * 60 + allowedStartMinute
        val endTotal = allowedEndHour * 60 + allowedEndMinute
        return if (startTotal <= endTotal) {
            currentTotal in startTotal..endTotal
        } else {
            // Over midnight support (e.g. 21:00 to 06:00)
            currentTotal >= startTotal || currentTotal <= endTotal
        }
    }
}

data class LiveAutomationProgress(
    val state: AutomationState = AutomationState.IDLE,
    val currentChatIndex: Int = 0,
    val totalChatsFound: Int = 0,
    val currentRecipient: String = "",
    val secondsRemaining: Int = 0,
    val lastActionMessage: String = "Listo para iniciar",
    val currentFolder: String = ""
)
