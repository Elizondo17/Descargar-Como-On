package com.example.util

import android.content.Context
import com.example.data.model.AgendaTimeWindowFilter
import com.example.data.model.AgendaVisitStatusFilter
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaSavingsResult
import com.example.data.model.PsaScriptCategory
import com.example.data.model.PsaScriptTemplate
import com.example.data.model.PsaSlotStatus
import com.example.data.model.PsaTimeSlot
import com.example.data.model.PsaVisitStatus
import com.example.data.model.PsaMessageTone
import com.example.data.model.SalesKnowledgeItem
import com.example.data.model.ScheduleIntervalConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object PsaScheduleManager {

    // Default daily schedule time windows for in-home demos (1 hour per visit + transit)
    val DEFAULT_HOURLY_RANGES = listOf(
        "09:30" to "10:30",
        "11:00" to "12:00",
        "15:00" to "16:00",
        "16:30" to "17:30",
        "18:00" to "19:00",
        "19:30" to "20:30"
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))
    private val shortDayFormat = SimpleDateFormat("EEE d", Locale("es", "ES"))

    /**
     * Parses "HH:mm" into minutes since midnight, returning null if malformed.
     */
    fun parseTimeMinutes(timeText: String): Int? {
        return try {
            val parts = timeText.trim().split(":")
            if (parts.size != 2) return null
            val hour = parts[0].trim().toInt()
            val minute = parts[1].trim().toInt()
            if (hour !in 0..23 || minute !in 0..59) return null
            hour * 60 + minute
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Formats minutes since midnight into "HH:mm" (wrapped to 24h).
     */
    fun formatMinutesAsTime(totalMinutes: Int): String {
        val safe = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)
        val hour = safe / 60
        val minute = safe % 60
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    /**
     * Shifts "HH:mm" by [deltaMinutes], clamped cleanly inside [00:00..23:59].
     */
    fun shiftTimeClamped(timeText: String, deltaMinutes: Int, fallback: String = "09:00"): String {
        val base = parseTimeMinutes(timeText) ?: parseTimeMinutes(fallback) ?: (9 * 60)
        val shifted = (base + deltaMinutes).coerceIn(0, 23 * 60 + 59)
        return formatMinutesAsTime(shifted)
    }

    /**
     * Calculates an end time [durationMinutes] after the start time in HH:mm format.
     */
    fun calculateEndTime(startTime: String, durationMinutes: Int = 60): String {
        val startMin = parseTimeMinutes(startTime) ?: return startTime
        val safeDuration = durationMinutes.coerceIn(15, 240)
        return formatMinutesAsTime(startMin + safeDuration)
    }

    fun getScheduleIntervalConfig(context: Context): ScheduleIntervalConfig {
        val prefs = context.getSharedPreferences("psa_schedule_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("schedule_interval_config_json", null) ?: return ScheduleIntervalConfig()
        return try {
            val obj = org.json.JSONObject(json)
            ScheduleIntervalConfig(
                workStartTime = obj.optString("workStartTime", "09:00"),
                workEndTime = obj.optString("workEndTime", "20:30"),
                demoDurationMinutes = obj.optInt("demoDurationMinutes", 60).coerceIn(15, 240),
                travelBufferMinutes = obj.optInt("travelBufferMinutes", 60).coerceIn(0, 180)
            )
        } catch (_: Exception) {
            ScheduleIntervalConfig()
        }
    }

    fun hasCustomIntervalConfig(context: Context): Boolean {
        val prefs = context.getSharedPreferences("psa_schedule_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("has_custom_interval_config", false)
    }

    fun saveScheduleIntervalConfig(context: Context, config: ScheduleIntervalConfig) {
        val cleanStart = parseTimeMinutes(config.workStartTime)?.let { formatMinutesAsTime(it) } ?: "09:00"
        val cleanEnd = parseTimeMinutes(config.workEndTime)?.let { formatMinutesAsTime(it) } ?: "20:30"
        val sanitized = config.copy(
            workStartTime = cleanStart,
            workEndTime = cleanEnd,
            demoDurationMinutes = config.demoDurationMinutes.coerceIn(15, 240),
            travelBufferMinutes = config.travelBufferMinutes.coerceIn(0, 180)
        )
        val obj = org.json.JSONObject().apply {
            put("workStartTime", sanitized.workStartTime)
            put("workEndTime", sanitized.workEndTime)
            put("demoDurationMinutes", sanitized.demoDurationMinutes)
            put("travelBufferMinutes", sanitized.travelBufferMinutes)
        }
        val generatedSlots = generateDailySlotsFromInterval(sanitized)
        context.getSharedPreferences("psa_schedule_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("schedule_interval_config_json", obj.toString())
            .putBoolean("has_custom_interval_config", true)
            .apply()
        saveDailySlots(context, generatedSlots)
    }

    fun getVisitDurationOverrides(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences("psa_schedule_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("visit_duration_overrides_json", null) ?: return emptyMap()
        return try {
            val obj = org.json.JSONObject(json)
            val result = mutableMapOf<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val minutes = obj.optInt(key, -1)
                if (minutes in 15..240) {
                    result[key] = minutes
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveVisitDurationOverride(context: Context, visitId: String, durationMinutes: Int) {
        if (visitId.isBlank()) return
        val prefs = context.getSharedPreferences("psa_schedule_prefs", Context.MODE_PRIVATE)
        val current = getVisitDurationOverrides(context).toMutableMap()
        current[visitId] = durationMinutes.coerceIn(15, 240)
        val obj = org.json.JSONObject()
        current.forEach { (id, mins) -> obj.put(id, mins) }
        prefs.edit().putString("visit_duration_overrides_json", obj.toString()).apply()
    }

    /**
     * Generates daily [start to end] time blocks within [config.workStartTime..config.workEndTime],
     * spacing each candidate block by [config.demoDurationMinutes] + [config.travelBufferMinutes].
     */
    fun generateDailySlotsFromInterval(config: ScheduleIntervalConfig): List<Pair<String, String>> {
        val startMin = parseTimeMinutes(config.workStartTime) ?: (9 * 60)
        val rawEndMin = parseTimeMinutes(config.workEndTime) ?: (20 * 60 + 30)
        val duration = config.demoDurationMinutes.coerceIn(15, 240)
        val buffer = config.travelBufferMinutes.coerceIn(0, 180)
        val endMin = if (rawEndMin >= startMin + duration) rawEndMin else (startMin + duration).coerceAtMost(23 * 60 + 59)

        val step = (duration + buffer).coerceAtLeast(15)
        val result = mutableListOf<Pair<String, String>>()
        var cursor = startMin
        while (cursor + duration <= endMin && result.size < 24) {
            result.add(formatMinutesAsTime(cursor) to formatMinutesAsTime(cursor + duration))
            cursor += step
        }
        return if (result.isEmpty()) listOf(formatMinutesAsTime(startMin) to formatMinutesAsTime(startMin + duration)) else result
    }

    fun matchesTimeWindow(startTime: String, window: AgendaTimeWindowFilter): Boolean {
        if (window == AgendaTimeWindowFilter.ALL) return true
        val startMin = parseTimeMinutes(startTime) ?: return true
        return when (window) {
            AgendaTimeWindowFilter.ALL -> true
            AgendaTimeWindowFilter.MORNING -> startMin < 13 * 60
            AgendaTimeWindowFilter.AFTERNOON -> startMin in (13 * 60) until (18 * 60)
            AgendaTimeWindowFilter.EVENING -> startMin >= 18 * 60
        }
    }

    fun filterVisitsByStatusAndWindow(
        visits: List<PsaHomeVisit>,
        selectedDateKey: String,
        statusFilter: AgendaVisitStatusFilter,
        timeWindow: AgendaTimeWindowFilter = AgendaTimeWindowFilter.ALL
    ): List<PsaHomeVisit> {
        return visits.asSequence()
            .filter { selectedDateKey.isBlank() || it.scheduledDate == selectedDateKey }
            .filter { statusFilter.matchesVisitStatus(it.status) }
            .filter { matchesTimeWindow(it.scheduledTime, timeWindow) }
            .sortedWith(compareBy<PsaHomeVisit> { it.scheduledDate }.thenBy { it.scheduledTime })
            .toList()
    }

    fun countVisitsByStatusForDate(
        visits: List<PsaHomeVisit>,
        selectedDateKey: String,
        timeWindow: AgendaTimeWindowFilter = AgendaTimeWindowFilter.ALL
    ): Map<AgendaVisitStatusFilter, Int> {
        val scoped = visits.filter {
            (selectedDateKey.isBlank() || it.scheduledDate == selectedDateKey) &&
                matchesTimeWindow(it.scheduledTime, timeWindow)
        }
        return mapOf(
            AgendaVisitStatusFilter.ALL to scoped.size,
            AgendaVisitStatusFilter.PENDING to scoped.count { AgendaVisitStatusFilter.PENDING.matchesVisitStatus(it.status) },
            AgendaVisitStatusFilter.COMPLETED to scoped.count { AgendaVisitStatusFilter.COMPLETED.matchesVisitStatus(it.status) },
            AgendaVisitStatusFilter.CANCELLED to scoped.count { AgendaVisitStatusFilter.CANCELLED.matchesVisitStatus(it.status) }
        )
    }

    /**
     * Retrieves configured daily slots for PSA schedule or returns standard defaults.
     */
    fun getDailySlots(context: Context): List<Pair<String, String>> {
        val prefs = context.getSharedPreferences("psa_schedule_prefs", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("custom_daily_slots_json", null) ?: return DEFAULT_HOURLY_RANGES
        return try {
            val array = org.json.JSONArray(jsonString)
            val list = mutableListOf<Pair<String, String>>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val start = obj.getString("start")
                val end = obj.optString("end", calculateEndTime(start))
                list.add(start to end)
            }
            if (list.isEmpty()) DEFAULT_HOURLY_RANGES else list
        } catch (_: Exception) {
            DEFAULT_HOURLY_RANGES
        }
    }

    /**
     * Persists configured daily slots for PSA schedule.
     */
    fun saveDailySlots(context: Context, slots: List<Pair<String, String>>) {
        val array = org.json.JSONArray()
        slots.forEach { (start, end) ->
            val obj = org.json.JSONObject().apply {
                put("start", start)
                put("end", end)
            }
            array.put(obj)
        }
        context.getSharedPreferences("psa_schedule_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("custom_daily_slots_json", array.toString())
            .apply()
    }

    /**
     * Generates a 7-day schedule starting from today, merging with existing booked visits
     * (including any custom free-time visits) and user-blocked slots stored in SharedPreferences.
     */
    fun generateScheduleForDays(
        context: Context,
        daysAhead: Int = 7,
        bookedVisits: List<PsaHomeVisit> = emptyList()
    ): List<PsaTimeSlot> {
        val prefs = context.getSharedPreferences("psa_schedule_prefs", Context.MODE_PRIVATE)
        val blockedIds = prefs.getStringSet("blocked_slot_ids", emptySet()) ?: emptySet()
        val intervalConfig = getScheduleIntervalConfig(context)
        val useIntervalEngine = hasCustomIntervalConfig(context)
        val dailySlots = getDailySlots(context)
        val visitDurations = getVisitDurationOverrides(context)

        return generateScheduleForDaysWithConfig(
            daysAhead = daysAhead,
            bookedVisits = bookedVisits,
            dailySlots = dailySlots,
            blockedIds = blockedIds,
            intervalConfig = intervalConfig,
            useIntervalBufferWindows = useIntervalEngine,
            visitDurationsById = visitDurations
        )
    }

    /**
     * Pure testable schedule builder combining daily ranges, booked visits, visit durations,
     * and automatic travel/delay buffers.
     */
    fun generateScheduleForDaysWithConfig(
        daysAhead: Int = 7,
        bookedVisits: List<PsaHomeVisit> = emptyList(),
        dailySlots: List<Pair<String, String>> = DEFAULT_HOURLY_RANGES,
        blockedIds: Set<String> = emptySet(),
        intervalConfig: ScheduleIntervalConfig = ScheduleIntervalConfig(),
        useIntervalBufferWindows: Boolean = false,
        visitDurationsById: Map<String, Int> = emptyMap(),
        startCalendar: Calendar = Calendar.getInstance()
    ): List<PsaTimeSlot> {
        val calendar = (startCalendar.clone() as Calendar)
        val slots = mutableListOf<PsaTimeSlot>()

        val activeVisits = bookedVisits.filter { it.status != PsaVisitStatus.CANCELADA }
        val visitsByDate = activeVisits.groupBy { it.scheduledDate }

        for (dayOffset in 0 until daysAhead) {
            val dateKey = dateFormat.format(calendar.time)
            val rawDisplay = displayFormat.format(calendar.time)
            val displayDate = rawDisplay.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val isSunday = dayOfWeek == Calendar.SUNDAY

            val dayVisits = visitsByDate[dateKey].orEmpty()
            val visitsByTime = dayVisits.associateBy { it.scheduledTime }

            val allRangesForDay = dailySlots.toMutableList()
            dayVisits.forEach { visit ->
                val visitDuration = visitDurationsById[visit.id] ?: intervalConfig.demoDurationMinutes
                val visitEnd = calculateEndTime(visit.scheduledTime, visitDuration)
                val existingIndex = allRangesForDay.indexOfFirst { it.first == visit.scheduledTime }
                if (existingIndex < 0) {
                    allRangesForDay.add(visit.scheduledTime to visitEnd)
                } else if (useIntervalBufferWindows || visitDurationsById.containsKey(visit.id)) {
                    allRangesForDay[existingIndex] = visit.scheduledTime to visitEnd
                }
            }
            allRangesForDay.sortBy { it.first }

            val daySlots = mutableListOf<PsaTimeSlot>()
            for ((start, end) in allRangesForDay) {
                val slotId = "slot_${dateKey}_${start.replace(":", "")}"
                val existingVisit = visitsByTime[start]

                val status = when {
                    existingVisit != null -> PsaSlotStatus.BOOKED
                    blockedIds.contains(slotId) -> PsaSlotStatus.BLOCKED
                    isSunday -> PsaSlotStatus.BLOCKED
                    else -> PsaSlotStatus.FREE
                }

                daySlots.add(
                    PsaTimeSlot(
                        id = slotId,
                        dateKey = dateKey,
                        displayDate = displayDate,
                        startTime = start,
                        endTime = end,
                        status = status,
                        assignedCustomerName = existingVisit?.customerName.orEmpty(),
                        assignedCustomerPhone = existingVisit?.phone.orEmpty(),
                        assignedAddress = existingVisit?.address.orEmpty(),
                        visitId = existingVisit?.id
                    )
                )
            }

            if (useIntervalBufferWindows) {
                val bufferMins = intervalConfig.travelBufferMinutes
                val occupiedWindows = dayVisits.mapNotNull { visit ->
                    val s = parseTimeMinutes(visit.scheduledTime) ?: return@mapNotNull null
                    val dur = visitDurationsById[visit.id] ?: intervalConfig.demoDurationMinutes
                    s to (s + dur)
                }
                val resolvedSlots = mutableListOf<PsaTimeSlot>()
                for (slot in daySlots) {
                    if (slot.status == PsaSlotStatus.BOOKED) {
                        resolvedSlots.add(slot)
                        continue
                    }
                    val slotStart = parseTimeMinutes(slot.startTime)
                    val slotEnd = parseTimeMinutes(slot.endTime)
                    if (slotStart == null || slotEnd == null) {
                        resolvedSlots.add(slot)
                        continue
                    }
                    val overlapsVisit = occupiedWindows.any { (vStart, vEnd) ->
                        slotStart < vEnd && slotEnd > vStart
                    }
                    if (overlapsVisit) {
                        continue
                    }
                    val inBufferWindow = bufferMins > 0 && occupiedWindows.any { (_, vEnd) ->
                        val bufEnd = vEnd + bufferMins
                        slotStart < bufEnd && slotEnd > vEnd
                    }
                    if (inBufferWindow) {
                        resolvedSlots.add(
                            slot.copy(
                                status = PsaSlotStatus.BUFFER,
                                assignedCustomerName = "Margen de traslado / demora (~${bufferMins} min)"
                            )
                        )
                    } else {
                        resolvedSlots.add(slot)
                    }
                }
                if (bufferMins > 0) {
                    val syntheticBuffers = mutableListOf<PsaTimeSlot>()
                    for (booked in resolvedSlots.filter { it.status == PsaSlotStatus.BOOKED }) {
                        val bEnd = parseTimeMinutes(booked.endTime) ?: continue
                        val bufEnd = (bEnd + bufferMins).coerceAtMost(23 * 60 + 59)
                        if (bufEnd <= bEnd) continue
                        val hasExistingBufferOrBookingAfter = resolvedSlots.any { candidate ->
                            val cStart = parseTimeMinutes(candidate.startTime) ?: return@any false
                            val cEnd = parseTimeMinutes(candidate.endTime) ?: return@any false
                            (candidate.status == PsaSlotStatus.BUFFER || candidate.status == PsaSlotStatus.BOOKED) &&
                                cStart < bufEnd && cEnd > bEnd
                        }
                        if (!hasExistingBufferOrBookingAfter) {
                            val bStartStr = formatMinutesAsTime(bEnd)
                            val bEndStr = formatMinutesAsTime(bufEnd)
                            syntheticBuffers.add(
                                PsaTimeSlot(
                                    id = "buffer_${dateKey}_${bStartStr.replace(":", "")}",
                                    dateKey = dateKey,
                                    displayDate = displayDate,
                                    startTime = bStartStr,
                                    endTime = bEndStr,
                                    status = PsaSlotStatus.BUFFER,
                                    assignedCustomerName = "Margen de seguridad (~${bufferMins} min traslado/demora)"
                                )
                            )
                        }
                    }
                    resolvedSlots.addAll(syntheticBuffers)
                }
                resolvedSlots.sortBy { it.startTime }
                slots.addAll(resolvedSlots)
            } else {
                // Standard succeeding-slot BUFFER logic
                for (i in 0 until daySlots.size - 1) {
                    if (daySlots[i].status == PsaSlotStatus.BOOKED) {
                        val nextSlot = daySlots[i + 1]
                        if (nextSlot.status != PsaSlotStatus.BOOKED) {
                            daySlots[i + 1] = nextSlot.copy(
                                status = PsaSlotStatus.BUFFER,
                                assignedCustomerName = "Margen de seguridad (traslado/demora)"
                            )
                        }
                    }
                }
                slots.addAll(daySlots)
            }

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return slots
    }

    /**
     * Toggles slot blocked state in preferences.
     */
    fun toggleSlotBlocked(context: Context, slotId: String): Boolean {
        val prefs = context.getSharedPreferences("psa_schedule_prefs", Context.MODE_PRIVATE)
        val blockedIds = prefs.getStringSet("blocked_slot_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

        val willBlock = !blockedIds.contains(slotId)
        if (willBlock) {
            blockedIds.add(slotId)
        } else {
            blockedIds.remove(slotId)
        }
        prefs.edit().putStringSet("blocked_slot_ids", blockedIds).apply()
        return willBlock
    }

    /**
     * Finds the next N available slots from now.
     */
    fun getNextAvailableSlots(slots: List<PsaTimeSlot>, count: Int = 2): List<PsaTimeSlot> {
        return slots.filter { it.status == PsaSlotStatus.FREE }.take(count)
    }

    /**
     * Automatically drafts a warm, natural, human appointment proposal with the next 2 real available slots.
     */
    fun generateAppointmentProposalMessage(
        clientName: String = "",
        neighborhood: String = "",
        availableSlots: List<PsaTimeSlot>,
        tone: PsaMessageTone = PsaMessageTone.CALIDO_HUMANO
    ): String {
        val nameClean = clientName.trim()
        val zoneClean = neighborhood.trim()

        val slot1 = availableSlots.getOrNull(0)
        val slot2 = availableSlots.getOrNull(1)

        val slot1Text = if (slot1 != null) "${slot1.displayDate} a las ${slot1.startTime}hs" else ""
        val slot2Text = if (slot2 != null) "${slot2.displayDate} a las ${slot2.startTime}hs" else ""

        val hasTwo = (slot1 != null && slot2 != null)
        val hasOne = (slot1 != null)

        return when (tone) {
            PsaMessageTone.CALIDO_HUMANO -> {
                val greeting = if (nameClean.isNotBlank()) "¡Hola $nameClean! ¿Cómo estás? 😊 " else "¡Hola! ¿Cómo estás? 😊 "
                val zoneInfo = if (zoneClean.isNotBlank()) "por $zoneClean" else "por tu zona"

                if (hasTwo) {
                    "${greeting}Te escribo cortito porque esta semana andamos $zoneInfo mostrando una prueba súper simple de 10 minutitos con el agua de la canilla, para ver cómo está saliendo de cloro y sarro.\n\n" +
                    "Es totalmente gratis y sin ningún compromiso (¡no te voy a vender nada raro! jaja), solo para que veas en vivo con unas gotitas cómo está el agua con la que toman y cocinan en casa.\n\n" +
                    "Justo me quedaron dos huequitos libres por tu barrio:\n" +
                    "👉 Opción 1: $slot1Text\n" +
                    "👉 Opción 2: $slot2Text\n\n" +
                    "¿Cuál de los dos te quedaría más cómodo para que pasemos un ratito?"
                } else if (hasOne) {
                    "${greeting}Te escribo porque andamos $zoneInfo haciendo la prueba rápida de agua de 10 min sin cargo. Me quedó libre el $slot1Text. ¿Te sirve que pasemos a esa hora o preferís otro momento?"
                } else {
                    "${greeting}Te escribo para coordinar los 10 minutitos de la prueba de cloro en tu canilla (es sin costo ni compromiso). ¿Te queda mejor que pasemos un día de semana a la tarde o un sábado?"
                }
            }

            PsaMessageTone.CORTO_DIRECTO -> {
                val greeting = if (nameClean.isNotBlank()) "¡Hola $nameClean! " else "¡Hola! "
                val zoneInfo = if (zoneClean.isNotBlank()) "en $zoneClean" else "en tu barrio"

                if (hasTwo) {
                    "${greeting}Te consulto rápido: esta semana estamos pasando $zoneInfo haciendo el test de cloro de 10 min en el agua de la canilla (es gratis y sin compromiso).\n\n" +
                    "Tengo dos opciones libres:\n" +
                    "1) $slot1Text\n" +
                    "2) $slot2Text\n\n" +
                    "¿Te sirve alguna de estas dos para que pasemos?"
                } else if (hasOne) {
                    "${greeting}Te aviso que tengo un hueco libre el $slot1Text para pasar a hacerte la prueba de cloro en la canilla de 10 min. ¿Te queda bien?"
                } else {
                    "${greeting}¿Cómo estás? Queríamos coordinar la prueba rápida de agua en tu casa sin costo. ¿Qué día te queda mejor?"
                }
            }

            PsaMessageTone.VECINO_AMIGABLE -> {
                val greeting = if (nameClean.isNotBlank()) "Hola $nameClean, ¿todo bien? " else "Hola vecino/a, ¿todo bien? "
                val zoneInfo = if (zoneClean.isNotBlank()) "en $zoneClean" else "en el barrio"

                if (hasTwo) {
                    "${greeting}Te cuento que varios vecinos de acá $zoneInfo nos pidieron revisar el agua de red porque venía con mucho gusto a cloro. Estamos pasando a hacer la prueba sin cargo en 10 minutos.\n\n" +
                    "Si están en casa, me queda libre:\n" +
                    "👉 $slot1Text\n" +
                    "👉 $slot2Text\n\n" +
                    "¿Te gustaría que te revisemos la canilla un segundo para que te quedes tranquilo/a?"
                } else if (hasOne) {
                    "${greeting}Andamos $zoneInfo revisando el cloro de la red en las casas de los vecinos. Tengo libre el $slot1Text. ¿Querés que pasemos 10 minutos?"
                } else {
                    "${greeting}¿Querés que pasemos a revisarte el agua de la canilla sin cargo? Avisame qué día te viene bien y combinamos."
                }
            }
        }
    }

    fun saveCustomProposal(context: Context, text: String) {
        context.getSharedPreferences("psa_schedule_prefs", Context.MODE_PRIVATE)
            .edit().putString("custom_proposal_template", text).apply()
    }

    fun getCustomProposal(context: Context): String? {
        return context.getSharedPreferences("psa_schedule_prefs", Context.MODE_PRIVATE)
            .getString("custom_proposal_template", null)
    }

    fun clearCustomProposal(context: Context) {
        context.getSharedPreferences("psa_schedule_prefs", Context.MODE_PRIVATE)
            .edit().remove("custom_proposal_template").apply()
    }

    fun saveCustomScript(context: Context, scriptId: String, newText: String) {
        context.getSharedPreferences("psa_schedule_prefs", Context.MODE_PRIVATE)
            .edit().putString("custom_script_$scriptId", newText).apply()
    }

    fun getCustomScript(context: Context, scriptId: String, defaultText: String): String {
        return context.getSharedPreferences("psa_schedule_prefs", Context.MODE_PRIVATE)
            .getString("custom_script_$scriptId", defaultText) ?: defaultText
    }

    /**
     * Persists PSA Pricing configuration in SharedPreferences scoped to active country.
     */
    fun savePsaPricingConfig(context: Context, config: com.example.data.model.PsaPricingConfig) {
        val activeCountry = PsaPortalSyncManager.getSelectedCountryCode(context)
        savePsaPricingConfigForCountry(context, activeCountry, config)
    }

    fun savePsaPricingConfigForCountry(
        context: Context,
        countryCode: String,
        config: com.example.data.model.PsaPricingConfig
    ) {
        val code = PsaCountryRegistry.getByCode(countryCode).code
        fun serializeList(list: List<com.example.data.model.PsaPurifierModel>): String {
            val arr = org.json.JSONArray()
            list.forEach { p ->
                arr.put(
                    org.json.JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("price", p.price)
                        put("installmentsCount", p.installmentsCount)
                        put("lifespanMonths", p.lifespanMonths)
                        put("description", p.description)
                        put("category", p.category)
                        put("officialUrl", p.officialUrl)
                        put("anmatRegistration", p.anmatRegistration)
                        put("highlightFeature", p.highlightFeature)
                    }
                )
            }
            return arr.toString()
        }

        context.getSharedPreferences("psa_schedule_prefs_$code", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("has_saved_pricing_v2", true)
            .putFloat("price_bidon", config.bidonPrice.toFloat())
            .putFloat("price_bottle", config.bottlePrice.toFloat())
            .putFloat("price_senior4", config.senior4Price.toFloat())
            .putFloat("price_quantum", config.quantumPrice.toFloat())
            .putFloat("price_senik", config.senikPrice.toFloat())
            .putFloat("price_croma", config.cromaPrice.toFloat())
            .putFloat("price_vero", config.veroPrice.toFloat())
            .putFloat("price_s1000", config.s1000Price.toFloat())
            .putFloat("price_mini", config.miniPrice.toFloat())
            .putFloat("price_c3", config.c3Price.toFloat())
            .putFloat("price_ducha", config.duchaPrice.toFloat())
            .putFloat("price_descalcificador", config.descalcificadorPrice.toFloat())
            .putFloat("price_domus", config.domusPrice.toFloat())
            .putFloat("price_maxima", config.maximaPrice.toFloat())
            .putFloat("price_pileta", config.piletaPrice.toFloat())
            .putFloat("price_aire", config.airePrice.toFloat())
            .putFloat("price_ropot", config.ropotPrice.toFloat())
            .putFloat("price_rotech", config.rotechPrice.toFloat())
            .putFloat("price_botella_termica", config.botellaTermicaPrice.toFloat())
            .putFloat("price_repuesto_senior", config.repuestoSeniorPrice.toFloat())
            .putInt("installments_count", config.installmentsCount)
            .putInt("default_lifespan_months", config.defaultLifespanMonths)
            .putString("default_comparison_type", config.defaultComparisonType.name)
            .putString("custom_purifiers_json", serializeList(config.customPurifiers))
            .putString("live_catalog_json", serializeList(config.liveCatalogItems))
            .putInt("weekly_visits_goal", config.weeklyVisitsGoal)
            .apply()
    }

    /**
     * Retrieves stored PSA Pricing configuration for active country (migrates old stale AR defaults if needed).
     */
    fun getPsaPricingConfig(context: Context): com.example.data.model.PsaPricingConfig {
        val activeCountry = PsaPortalSyncManager.getSelectedCountryCode(context)
        return getPsaPricingConfigForCountry(context, activeCountry)
    }

    fun getPsaPricingConfigForCountry(
        context: Context,
        countryCode: String
    ): com.example.data.model.PsaPricingConfig {
        val code = PsaCountryRegistry.getByCode(countryCode).code
        val baseline = PsaCountryRegistry.defaultPricingForCountry(code)
        val prefs = context.getSharedPreferences("psa_schedule_prefs_$code", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("has_saved_pricing_v2", false)) {
            return baseline
        }

        fun deserializeList(jsonStr: String?): List<com.example.data.model.PsaPurifierModel> {
            if (jsonStr.isNullOrBlank()) return emptyList()
            val list = mutableListOf<com.example.data.model.PsaPurifierModel>()
            try {
                val jsonArray = org.json.JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        com.example.data.model.PsaPurifierModel(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            price = obj.getDouble("price"),
                            installmentsCount = obj.optInt("installmentsCount", 12),
                            lifespanMonths = obj.optInt("lifespanMonths", 36),
                            description = obj.optString("description", ""),
                            category = obj.optString("category", "Cocina"),
                            officialUrl = obj.optString("officialUrl", "https://www.psa.com.ar"),
                            anmatRegistration = obj.optString("anmatRegistration", "Oficial PSA"),
                            highlightFeature = obj.optString("highlightFeature", "Catálogo oficial PSA")
                        )
                    )
                }
            } catch (_: Exception) {}
            return list
        }

        val customList = deserializeList(prefs.getString("custom_purifiers_json", "[]"))
        val liveList = deserializeList(prefs.getString("live_catalog_json", "[]"))

        val comparisonTypeName = prefs.getString("default_comparison_type", com.example.data.model.PsaComparisonType.BIDON_20L.name)
        val comparisonType = try {
            com.example.data.model.PsaComparisonType.valueOf(comparisonTypeName ?: "BIDON_20L")
        } catch (_: Exception) {
            com.example.data.model.PsaComparisonType.BIDON_20L
        }

        fun sanitizeField(key: String, fallback: Double): Double {
            val loaded = prefs.getFloat(key, fallback.toFloat()).toDouble()
            return if (loaded > 0.0 && loaded in (fallback * 0.22)..(fallback * 6.0)) {
                loaded
            } else {
                fallback
            }
        }

        val loadedSenior4 = sanitizeField("price_senior4", baseline.senior4Price)
        val loadedSenik = sanitizeField("price_senik", baseline.senikPrice)
        val loadedQuantum = sanitizeField("price_quantum", baseline.quantumPrice)

        val sanitized = com.example.data.model.PsaPricingConfig(
            bidonPrice = sanitizeField("price_bidon", baseline.bidonPrice),
            bottlePrice = sanitizeField("price_bottle", baseline.bottlePrice),
            senior4Price = loadedSenior4,
            quantumPrice = loadedQuantum,
            senikPrice = loadedSenik,
            cromaPrice = sanitizeField("price_croma", baseline.cromaPrice),
            veroPrice = sanitizeField("price_vero", baseline.veroPrice),
            s1000Price = sanitizeField("price_s1000", baseline.s1000Price),
            miniPrice = sanitizeField("price_mini", baseline.miniPrice),
            c3Price = sanitizeField("price_c3", baseline.c3Price),
            duchaPrice = sanitizeField("price_ducha", baseline.duchaPrice),
            descalcificadorPrice = sanitizeField("price_descalcificador", baseline.descalcificadorPrice),
            domusPrice = sanitizeField("price_domus", baseline.domusPrice),
            maximaPrice = sanitizeField("price_maxima", baseline.maximaPrice),
            piletaPrice = sanitizeField("price_pileta", baseline.piletaPrice),
            airePrice = sanitizeField("price_aire", baseline.airePrice),
            ropotPrice = sanitizeField("price_ropot", baseline.ropotPrice),
            rotechPrice = sanitizeField("price_rotech", baseline.rotechPrice),
            botellaTermicaPrice = sanitizeField("price_botella_termica", baseline.botellaTermicaPrice),
            repuestoSeniorPrice = sanitizeField("price_repuesto_senior", baseline.repuestoSeniorPrice),
            installmentsCount = prefs.getInt("installments_count", baseline.installmentsCount).coerceIn(1, 36),
            defaultLifespanMonths = prefs.getInt("default_lifespan_months", 36).coerceIn(6, 120),
            defaultComparisonType = comparisonType,
            customPurifiers = customList,
            liveCatalogItems = liveList,
            weeklyVisitsGoal = prefs.getInt("weekly_visits_goal", 10).coerceIn(1, 200)
        )
        return sanitized
    }

    fun restoreBaselinePricingForCountry(
        context: Context,
        countryCode: String
    ): com.example.data.model.PsaPricingConfig {
        val code = PsaCountryRegistry.getByCode(countryCode).code
        val baseline = PsaCountryRegistry.defaultPricingForCountry(code)
        savePsaPricingConfigForCountry(context, code, baseline)
        PsaStockManager.syncWithOfficialPvp(context, baseline, code)
        return baseline
    }

    /**
     * Calculates weekly visits goal stats, progress percentage, badges and motivational message.
     */
    fun getWeeklyVisitsStats(
        bookedVisits: List<PsaHomeVisit>,
        goal: Int = 10
    ): com.example.data.model.PsaWeeklyGoalStats {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfWeekKey = dateFormat.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val endOfWeekKey = dateFormat.format(cal.time)

        val safeGoal = if (goal <= 0) 10 else goal
        val currentWeekBooked = bookedVisits.count { visit ->
            visit.status != PsaVisitStatus.CANCELADA &&
                visit.scheduledDate >= startOfWeekKey &&
                visit.scheduledDate <= endOfWeekKey
        }

        val percent = (currentWeekBooked.toFloat() / safeGoal.toFloat()).coerceAtLeast(0f)

        val (badgeTitle, badgeIcon) = when {
            currentWeekBooked == 0 -> "Comenzando" to "🌱"
            currentWeekBooked in 1 until (safeGoal / 2) -> "Primer Paso" to "🥉"
            currentWeekBooked in (safeGoal / 2) until (safeGoal * 8 / 10) -> "En Ritmo" to "🥈"
            currentWeekBooked in (safeGoal * 8 / 10) until safeGoal -> "Cerca de la Meta" to "🥇"
            currentWeekBooked == safeGoal -> "Campeón Semanal" to "🏆"
            else -> "Superestrella" to "🔥"
        }

        val motivationalMessage = when {
            currentWeekBooked == 0 -> "¡Agendá tu primera cita de la semana para encender la racha!"
            currentWeekBooked < safeGoal -> "¡Faltan ${safeGoal - currentWeekBooked} citas para alcanzar tu meta semanal de $safeGoal!"
            currentWeekBooked == safeGoal -> "🎉 ¡Objetivo de $safeGoal citas alcanzado! ¡Excelente trabajo de demostración!"
            else -> "🔥 ¡Imparable! Superaste tu meta con $currentWeekBooked citas agendadas."
        }

        return com.example.data.model.PsaWeeklyGoalStats(
            bookedVisitsCount = currentWeekBooked,
            goal = safeGoal,
            progressPercent = percent.coerceAtMost(1.0f),
            badgeTitle = badgeTitle,
            badgeIcon = badgeIcon,
            motivationalMessage = motivationalMessage
        )
    }

    /**
     * Calculates extended savings comparing either bottles or jugs against a purifier model
     * factoring in full lifetime duration (months/years) and total accumulated investment return.
     */
    fun calculateSavings(
        bottlesPerWeek: Int,
        bottlePrice: Double = 3500.0,
        purifierPrice: Double = 280000.0,
        installmentsCount: Int = 12,
        comparisonType: com.example.data.model.PsaComparisonType = com.example.data.model.PsaComparisonType.BIDON_20L,
        lifespanMonths: Int = 36,
        containerLiters: Double = 0.0
    ): PsaSavingsResult {
        val safeConsumption = if (bottlesPerWeek <= 0) 1 else bottlesPerWeek
        val safePrice = if (bottlePrice <= 0.0) 3500.0 else bottlePrice
        val safeLifespan = if (lifespanMonths <= 0) 36 else lifespanMonths
        val safeLiters = if (containerLiters <= 0.0) (if (comparisonType == com.example.data.model.PsaComparisonType.BIDON_20L) 20.0 else 2.0) else containerLiters

        val monthlyUnits = safeConsumption * 4.33
        val monthlyCost = monthlyUnits * safePrice
        val yearlyCost = monthlyCost * 12.0
        val fiveYearCost = yearlyCost * 5.0
        val lifespanTotalWaterCost = monthlyCost * safeLifespan

        val safeInstallments = if (installmentsCount <= 0) 12 else installmentsCount
        val suggestedMonthlyInstallment = purifierPrice / safeInstallments
        val monthlySavings = (monthlyCost - suggestedMonthlyInstallment).coerceAtLeast(0.0)
        val totalLifespanSavings = (lifespanTotalWaterCost - purifierPrice).coerceAtLeast(0.0)

        val totalLitersPerMonth = monthlyUnits * safeLiters
        val costPerLiterBottle = if (safeLiters > 0) safePrice / safeLiters else 0.0
        // PSA senior 4 produces 36000L for purifierPrice
        val costPerLiterPsa = if (purifierPrice > 0) purifierPrice / 36000.0 else 0.0
        val plasticBottlesAvoidedYear = ((totalLitersPerMonth * 12.0) / 0.5).toInt() // Botellas de 500ml evitadas

        val litersLabel = if (safeLiters % 1.0 == 0.0) "${safeLiters.toInt()}L" else "${safeLiters}L"
        val unitLabel = if (comparisonType == com.example.data.model.PsaComparisonType.BIDON_20L) "bidones de $litersLabel" else "botellas de $litersLabel"
        val yearsDisplay = String.format(Locale.US, "%.1f", safeLifespan / 12.0)

        val impactMessage = "Consumiendo $safeConsumption $unitLabel por semana gastás " +
            "$${String.format(Locale.US, "%,.0f", monthlyCost)} al mes y $${String.format(Locale.US, "%,.0f", yearlyCost)} al año.\n\n" +
            "En la vida útil del purificador ($safeLifespan meses / $yearsDisplay años), gastarías $${String.format(Locale.US, "%,.0f", lifespanTotalWaterCost)} en plástico.\n" +
            "Con el purificador PSA ($safeInstallments cuotas fijas de $${String.format(Locale.US, "%,.0f", suggestedMonthlyInstallment)}), " +
            "el ahorro total es de $${String.format(Locale.US, "%,.0f", totalLifespanSavings)} con agua pura ilimitada.\n" +
            "🌱 Además evitás $plasticBottlesAvoidedYear botellas plásticas al año."

        return PsaSavingsResult(
            consumptionPerWeek = safeConsumption,
            unitPrice = safePrice,
            comparisonType = comparisonType,
            monthlyCost = monthlyCost,
            yearlyCost = yearlyCost,
            fiveYearCost = fiveYearCost,
            lifespanMonths = safeLifespan,
            lifespanTotalBottleCost = lifespanTotalWaterCost,
            purifierTotalPrice = purifierPrice,
            suggestedPsaMonthlyPayment = suggestedMonthlyInstallment,
            monthlySavings = monthlySavings,
            totalLifespanSavings = totalLifespanSavings,
            impactMessage = impactMessage,
            totalLitersPerMonth = totalLitersPerMonth,
            costPerLiterBottle = costPerLiterBottle,
            costPerLiterPsa = costPerLiterPsa,
            plasticBottlesAvoidedYear = plasticBottlesAvoidedYear
        )
    }

    /**
     * Master library of proven PSA direct sales templates - 100% human, warm, conversational.
     */
    val PSA_SCRIPTS = listOf(
        PsaScriptTemplate(
            id = "psa_script_chlorine_hook",
            title = "🧪 Gancho de Prueba en Casa (10 min)",
            category = PsaScriptCategory.GANCHO_TEST_CLORO,
            description = "El mensaje más cálido y humano para abrir conversación sin generar rechazo.",
            templateBody = "¡Hola {nombre}! ¿Cómo estás? Te escribo porque esta semana andamos por tu barrio mostrando una prueba súper simple de 10 minutitos con el agua de la canilla, para que veas qué tal viene de cloro y sarro.\n\nEs totalmente gratis y sin ningún compromiso (¡cero presión de compra!), solo para que conozcas la calidad del agua que usan para tomar y cocinar.\n\n¿Te quedaría más cómodo que pasemos un ratito a la tarde o un sábado por la mañana?",
            successRate = 95
        ),
        PsaScriptTemplate(
            id = "psa_script_bottle_pain",
            title = "🚰 Dejar los Bidones y el Peso",
            category = PsaScriptCategory.DOLOR_BIDON,
            description = "Conversación sincera sobre la molestia de cargar bidones y gastar en plástico.",
            templateBody = "¡Hola {nombre}! Una consulta rápida: ¿en tu casa siguen comprando bidones pesados?\n\nLa mayoría de la gente está cansada de cargar los 20 kilos, esperar al repartidor y gastar una fortuna al mes en plástico.\n\nEn 10 minutos te mostramos cómo tener agua purificada infinita directamente en tu canilla por menos de lo que gastas hoy. ¿Te gustaría que pasemos mañana o pasado a mostrártelo?",
            successRate = 91
        ),
        PsaScriptTemplate(
            id = "psa_script_cooking_health",
            title = "🍲 Cocinar con Agua Pura (Fideos, Arroz y Té)",
            category = PsaScriptCategory.COCINA_SALUD,
            description = "Despierta la conciencia de cuidar la comida de los chicos y la familia.",
            templateBody = "¿Sabías que al hervir fideos, arroz o preparar el mate, los alimentos absorben casi todo el cloro del agua de la canilla?\n\nMuchos cuidan el agua para tomar, pero cocinan con agua con cloro. Con el purificador tienes agua limpia para tomar, cocinar y lavar verduras para toda la familia.\n\n¿Querés que te hagamos la prueba con unas gotitas en tu cocina? Es rápida y sin cargo.",
            successRate = 93
        ),
        PsaScriptTemplate(
            id = "psa_script_double_close",
            title = "⚡ Cierre de Doble Opción Amigable",
            category = PsaScriptCategory.CIERRE_DOBLE_OPCION,
            description = "Propone dos momentos concretos de forma relajada y amable.",
            templateBody = "Para pasar 10 minutitos a hacer la prueba del agua en tu cocina y que prueben el sabor, tengo dos huequitos esta semana:\n\n👉 Opción 1: {turno_1}\n👉 Opción 2: {turno_2}\n\n¿Cuál de los dos te queda más cómodo para que estemos?",
            successRate = 96
        ),
        PsaScriptTemplate(
            id = "psa_script_anti_noshow",
            title = "🛡️ Recordatorio Amable (Antes de salir)",
            category = PsaScriptCategory.RECORDATORIO_ANTI_PLANTON,
            description = "Aviso cercano y respetuoso para reconfirmar la visita.",
            templateBody = "¡Hola {nombre}! ¿Cómo andás? Te escribo para confirmar que hoy a las {horario} pasamos por tu casa ({direccion}) para la prueba de agua de 10 min.\n\nPorfa avisame si siguen bien con ese horario así ya me organizo el recorrido. ¡Nos vemos en un rato!",
            successRate = 96
        ),
        PsaScriptTemplate(
            id = "psa_script_objection_partner",
            title = "👫 'Lo tengo que hablar con mi pareja'",
            category = PsaScriptCategory.OBJECION_PAREJA,
            description = "Valida la consulta familiar con total empatía y respeto.",
            templateBody = "¡Totalmente! Es una decisión de la casa que hace a la salud de todos. Por eso mismo la idea es pasar 10 minutos cuando estén los dos, así ambos ven cómo reacciona el agua con las gotitas y prueban el sabor juntos. ¿Qué día y horario les queda mejor para que estén ambos?",
            successRate = 94
        ),
        PsaScriptTemplate(
            id = "psa_script_objection_boiled",
            title = "🔥 'Yo hiervo el agua de la canilla'",
            category = PsaScriptCategory.OBJECION_AGUA_HERVIDA,
            description = "Explicación sencilla y sin tecnicismos sobre el agua hervida.",
            templateBody = "Hervir el agua es buenísimo para matar bichitos, pero al hervir el agua se evapora y los químicos como el cloro y el sarro quedan todos concentrados en la olla. En 10 minutos te muestro con unas gotitas en un vaso cómo queda el agua antes y después. ¿Te gustaría verla?",
            successRate = 92
        ),
        PsaScriptTemplate(
            id = "psa_script_objection_price",
            title = "💰 'Es muy caro / No tengo plata ahora'",
            category = PsaScriptCategory.OBJECION_PRECIO,
            description = "Alivia la presión del dinero mostrando el ahorro real mes a mes.",
            templateBody = "Te entiendo súper bien, por eso la idea no es que pongas plata de golpe. En la charla te mostramos con números reales cómo lo que hoy gastás mes a mes en bidones o botellas te cubre la cuota del purificador. El equipo se paga solo y dejás de gastar en el súper. ¿Te parece que lo veamos sin compromiso?",
            successRate = 93
        )
    )

    /**
     * Initial high-converting knowledge base for PSA to inject into SalesKnowledgeItem.
     */
    fun getInitialPsaKnowledge(): List<SalesKnowledgeItem> {
        val now = System.currentTimeMillis()
        return listOf(
            SalesKnowledgeItem(
                id = "psa-know-chlorine-demo",
                category = "PSA_DEMO_CLORO",
                triggerContext = "Cliente duda de la calidad del agua de canilla o dice que es potable",
                winningContent = "El agua de red es potable para salir de la planta, pero en el trayecto arrastra óxido, plomo y cloro en exceso. Con unas gotas de ortotoluidina en su cocina, el agua de canilla se pone amarilla fosforescente y la de PSA cristalina: esa prueba visual cierra la venta en 15 minutos.",
                successScore = 18,
                usageCount = 24,
                isQuestion = false,
                createdAt = now
            ),
            SalesKnowledgeItem(
                id = "psa-know-bidones-vs-psa",
                category = "PSA_AHORRO_BIDONES",
                triggerContext = "Cliente dice que ya compra bidones de agua y está conforme",
                winningContent = "Calcular cuántos bidones compra por mes (ej. 4 x $3.500 = $14.000/mes). Con PSA en cuotas fijas gasta menos que en bidones, no carga 20 kilos de peso y tiene agua purificada ilimitada también para cocinar fideos, arroz y lavar verduras.",
                successScore = 16,
                usageCount = 20,
                isQuestion = false,
                createdAt = now
            ),
            SalesKnowledgeItem(
                id = "psa-know-partner-close",
                category = "PSA_OBJECION_PAREJA",
                triggerContext = "Cliente dice 'Lo consulto con mi pareja antes de decidir'",
                winningContent = "¡Excelente! La decisión es de la familia. Por eso mismo la prueba se hace con ambos presentes para que los dos vean el reactivo de cloro y tomen un vaso juntos. ¿Qué día y horario están ambos en casa?",
                successScore = 15,
                usageCount = 19,
                isQuestion = true,
                createdAt = now
            ),
            SalesKnowledgeItem(
                id = "psa-know-boiled-water",
                category = "PSA_OBJECION_HERVIDA",
                triggerContext = "Cliente dice 'Yo hiervo el agua de la canilla y listo'",
                winningContent = "Hervir mata microorganismos, pero evapora agua y concentra los químicos como el cloro, sarro y metales pesados. En 15 min le mostramos con reactivo químico lo que queda en la olla.",
                successScore = 14,
                usageCount = 17,
                isQuestion = false,
                createdAt = now
            ),
            SalesKnowledgeItem(
                id = "psa-know-double-close",
                category = "PSA_CIERRE_DOBLE",
                triggerContext = "Para cerrar la cita y evitar 'lo voy a pensar'",
                winningContent = "Tengo dos turnos en tu zona esta semana: ¿Te queda mejor el Martes a las 18:30hs o el Sábado a las 10:30hs?",
                successScore = 22,
                usageCount = 28,
                isQuestion = true,
                createdAt = now
            )
        )
    }
}
