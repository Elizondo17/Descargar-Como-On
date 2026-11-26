package com.example.util

import com.example.data.model.PsaContactInteraction
import java.util.Calendar

enum class CallingWindowSlot(
    val id: String,
    val label: String,
    val rangeLabel: String,
    val startHour: Int,
    val endHourExclusive: Int
) {
    MORNING("MORNING", "Mañana", "09:00 - 12:59", 9, 13),
    AFTERNOON("AFTERNOON", "Tarde", "13:00 - 17:59", 13, 18),
    EVENING("EVENING", "Tardecita", "18:00 - 21:59", 18, 22),
    OFF_HOURS("OFF_HOURS", "Fuera de franja", "22:00 - 08:59", 22, 9);

    companion object {
        fun forHour(hourOfDay: Int): CallingWindowSlot = when (hourOfDay) {
            in 9..12 -> MORNING
            in 13..17 -> AFTERNOON
            in 18..21 -> EVENING
            else -> OFF_HOURS
        }
    }
}

data class CallCoachingInsight(
    val callsToday: Int,
    val dailyGoal: Int,
    val badgeTitle: String,
    val headline: String,
    val motivationalTip: String,
    val organizationTip: String,
    val nextActionTip: String
)

data class HourlyCallEffectiveness(
    val hour: Int,
    val totalCalls: Int,
    val connectedCalls: Int,
    val conversionPercent: Int
) {
    val hourLabel: String
        get() = String.format(java.util.Locale.ROOT, "%02d:00", hour)
}

data class WindowCallEffectiveness(
    val slot: CallingWindowSlot,
    val totalCalls: Int,
    val connectedCalls: Int,
    val conversionPercent: Int
)

data class CallTimingAnalysis(
    val totalAnalyzedCalls: Int,
    val hourlyBreakdown: List<HourlyCallEffectiveness>,
    val windowBreakdown: List<WindowCallEffectiveness>,
    val bestWindow: WindowCallEffectiveness?,
    val bestHour: HourlyCallEffectiveness?,
    val recommendationHeadline: String,
    val recommendationDetail: String,
    val hasPersonalHistory: Boolean,
    val recommendedReminderHour: Int,
    val recommendedReminderMinute: Int
)

object PsaCallCoachingEngine {

    fun buildAdaptiveCoaching(
        callsToday: Int,
        successToday: Int,
        appointmentsToday: Int = 0,
        pendingQueueSize: Int = 0,
        dailyGoal: Int = 15,
        hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): CallCoachingInsight {
        val currentSlot = CallingWindowSlot.forHour(hourOfDay)
        val slotTip = when (currentSlot) {
            CallingWindowSlot.MORNING ->
                "Franja de mañana (09:30–12:00): ideal para contactos cálidos, vecinos o amas de casa antes del almuerzo."
            CallingWindowSlot.AFTERNOON ->
                "Franja de tarde (13:30–17:00): priorizá seguimientos rápidos y reprogramados mientras vuelven de sus actividades."
            CallingWindowSlot.EVENING ->
                "Franja tardecita (18:00–20:30): el mejor horario para encontrar a la familia completa en casa y cerrar citas."
            CallingWindowSlot.OFF_HOURS ->
                "Fuera de horario comercial: dejá preparada tu lista de mañana con bloques de 10 contactos por franja."
        }

        val orgTip = when {
            pendingQueueSize >= 10 ->
                "Organizá tandas cortas de 5 llamadas seguidas + 1 descanso de 5 min; arrancá por los marcados como 'Reprogramado' o 'Interesado'."
            pendingQueueSize > 0 ->
                "Tenés $pendingQueueSize pendientes: dividilos en 2 bloques (primero calientes/referidos, luego primer contacto)."
            else ->
                "Cargá al menos 10 referidos o vecinos hoy antes de empezar la tanda para no cortar el ritmo entre llamada y llamada."
        }

        return when {
            callsToday == 0 -> CallCoachingInsight(
                callsToday = 0,
                dailyGoal = dailyGoal,
                badgeTitle = "Arranque del Día",
                headline = "¡Tu primera llamada destraba todo el día!",
                motivationalTip = "Solo necesitás romper el hielo con 3 llamados cortos. Recordá: ofrecés un control gratuito de agua que cuida a su familia.",
                organizationTip = orgTip,
                nextActionTip = slotTip
            )
            callsToday in 1..4 -> CallCoachingInsight(
                callsToday = callsToday,
                dailyGoal = dailyGoal,
                badgeTitle = "Entrando en Calor",
                headline = "Ya rompiste la inercia con $callsToday llamada(s)",
                motivationalTip = if (successToday > 0) {
                    "¡Excelente! Ya lograste $successToday respuesta(s) efectiva(s). Mantené ese tono cercano y proponé dos opciones de horario."
                } else {
                    "Los 'no atiende' son normales: anotá el horario y pasá rápido al siguiente contacto sin frenarte."
                },
                organizationTip = orgTip,
                nextActionTip = slotTip
            )
            callsToday in 5..11 -> CallCoachingInsight(
                callsToday = callsToday,
                dailyGoal = dailyGoal,
                badgeTitle = "Ritmo Constante",
                headline = "¡Buen ritmo! Llevás $callsToday llamados hoy",
                motivationalTip = if (appointmentsToday > 0) {
                    "Ya coordinaste $appointmentsToday cita(s). Cada tanda de 5 llamadas suma una nueva familia interesada en ver el test."
                } else {
                    "En este volumen suele aparecer la primera cita del día. Usá el cierre doble: '¿Te queda mejor hoy 18:30 o mañana 11:00?'."
                },
                organizationTip = orgTip,
                nextActionTip = slotTip
            )
            else -> CallCoachingInsight(
                callsToday = callsToday,
                dailyGoal = dailyGoal,
                badgeTitle = "Líder en Acción",
                headline = "¡Imparable! $callsToday llamadas realizadas hoy",
                motivationalTip = "Ritmo de alto rendimiento ($successToday contactos efectivos). Aprovechá tu energía para reconfirmar las citas de mañana.",
                organizationTip = "Cerrá el día dejando etiquetados a los que no atendieron para reintentarlos mañana en otra franja horaria.",
                nextActionTip = slotTip
            )
        }
    }

    fun analyzeCallTiming(
        interactions: List<PsaContactInteraction>
    ): CallTimingAnalysis {
        val calls = interactions.filter { it.channel == "CALL" && it.timestampMs > 0L }
        if (calls.isEmpty()) {
            return CallTimingAnalysis(
                totalAnalyzedCalls = 0,
                hourlyBreakdown = emptyList(),
                windowBreakdown = listOf(
                    WindowCallEffectiveness(CallingWindowSlot.EVENING, 0, 0, 0),
                    WindowCallEffectiveness(CallingWindowSlot.MORNING, 0, 0, 0),
                    WindowCallEffectiveness(CallingWindowSlot.AFTERNOON, 0, 0, 0)
                ),
                bestWindow = null,
                bestHour = null,
                recommendationHeadline = "Sugerencia base PSA: 10:00–12:00 y 18:00–20:30",
                recommendationDetail = "Todavía no registraste suficientes llamadas para medir tu patrón personal. Estas dos franjas tienen la mayor tasa de respuesta familiar.",
                hasPersonalHistory = false,
                recommendedReminderHour = 18,
                recommendedReminderMinute = 0
            )
        }

        val cal = Calendar.getInstance()
        val callsByHour = hashMapOf<Int, MutableList<PsaContactInteraction>>()
        calls.forEach { interaction ->
            cal.timeInMillis = interaction.timestampMs
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            callsByHour.getOrPut(hour) { mutableListOf() }.add(interaction)
        }

        fun isEffective(item: PsaContactInteraction): Boolean =
            item.result == "SUCCESS" || item.result == "RESCHEDULED"

        val hourlyBreakdown = callsByHour.entries
            .map { (hour, list) ->
                val connected = list.count { isEffective(it) }
                val pct = ((connected * 100f) / list.size.toFloat()).toInt().coerceIn(0, 100)
                HourlyCallEffectiveness(
                    hour = hour,
                    totalCalls = list.size,
                    connectedCalls = connected,
                    conversionPercent = pct
                )
            }
            .sortedBy { it.hour }

        val mainWindows = listOf(
            CallingWindowSlot.MORNING,
            CallingWindowSlot.AFTERNOON,
            CallingWindowSlot.EVENING
        )
        val windowBreakdown = mainWindows.map { slot ->
            val inSlot = calls.filter { item ->
                cal.timeInMillis = item.timestampMs
                CallingWindowSlot.forHour(cal.get(Calendar.HOUR_OF_DAY)) == slot
            }
            val connected = inSlot.count { isEffective(it) }
            val pct = if (inSlot.isNotEmpty()) {
                ((connected * 100f) / inSlot.size.toFloat()).toInt().coerceIn(0, 100)
            } else {
                0
            }
            WindowCallEffectiveness(
                slot = slot,
                totalCalls = inSlot.size,
                connectedCalls = connected,
                conversionPercent = pct
            )
        }

        val eligibleWindows = windowBreakdown.filter { it.totalCalls >= 2 }
        val bestWindow = (if (eligibleWindows.isNotEmpty()) eligibleWindows else windowBreakdown.filter { it.totalCalls > 0 })
            .maxWithOrNull(compareBy<WindowCallEffectiveness>({ it.conversionPercent }, { it.connectedCalls }, { it.totalCalls }))

        val eligibleHours = hourlyBreakdown.filter { it.totalCalls >= 2 }
        val bestHour = (if (eligibleHours.isNotEmpty()) eligibleHours else hourlyBreakdown)
            .maxWithOrNull(compareBy<HourlyCallEffectiveness>({ it.conversionPercent }, { it.connectedCalls }, { it.totalCalls }))

        val hasEnoughPersonalData = calls.size >= 3 && (bestHour != null || bestWindow != null)
        val recHour = when {
            bestHour != null && bestHour.conversionPercent > 0 -> bestHour.hour
            bestWindow != null -> bestWindow.slot.startHour
            else -> 18
        }.coerceIn(8, 21)

        val headline = when {
            bestHour != null && hasEnoughPersonalData ->
                "Tu mejor horario: ${bestHour.hourLabel} (${bestHour.conversionPercent}% efectivos)"
            bestWindow != null ->
                "Tu mejor franja: ${bestWindow.slot.label} (${bestWindow.slot.rangeLabel})"
            else ->
                "Sugerencia base PSA: 10:00–12:00 y 18:00–20:30"
        }

        val detail = if (hasEnoughPersonalData && bestWindow != null) {
            "Analizando ${calls.size} llamadas tuyas, en ${bestWindow.slot.label} (${bestWindow.slot.rangeLabel}) lograste ${bestWindow.conversionPercent}% de contacto efectivo (${bestWindow.connectedCalls}/${bestWindow.totalCalls})."
        } else {
            "Seguí registrando el resultado de tus llamadas (${calls.size}/3 registradas) para afinar automáticamente tu horario pico personal."
        }

        return CallTimingAnalysis(
            totalAnalyzedCalls = calls.size,
            hourlyBreakdown = hourlyBreakdown,
            windowBreakdown = windowBreakdown,
            bestWindow = bestWindow,
            bestHour = bestHour,
            recommendationHeadline = headline,
            recommendationDetail = detail,
            hasPersonalHistory = hasEnoughPersonalData,
            recommendedReminderHour = recHour,
            recommendedReminderMinute = 0
        )
    }

    /**
     * Computes the next timestamp 10 minutes before [targetHour]:[targetMinute] (today if still ahead,
     * otherwise tomorrow).
     */
    fun computeNextPrePeakTriggerMs(
        targetHour: Int,
        targetMinute: Int = 0,
        leadMinutes: Int = 10,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, targetHour.coerceIn(7, 21))
            set(Calendar.MINUTE, targetMinute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -leadMinutes.coerceIn(0, 60))
        }
        if (cal.timeInMillis <= nowMs + 15_000L) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
