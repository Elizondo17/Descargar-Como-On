package com.example.util

import com.example.data.model.PsaContactLead
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaPostSaleInstallation
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AgendaSearchScope(val label: String) {
    ALL("Todo"),
    NAME("Nombre"),
    DATE("Fecha")
}

object PsaAgendaSearchHelper {

    private val SPANISH_LOCALE = Locale("es", "AR")
    private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val WHITESPACE_REGEX = Regex("\\s+")

    fun normalizeSearchText(raw: String): String {
        if (raw.isBlank()) return ""
        val decomposed = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
        return DIACRITICS_REGEX.replace(decomposed, "")
            .lowercase(SPANISH_LOCALE)
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    fun buildIsoDateSearchTokens(isoDate: String, displayDate: String = ""): String {
        val cleanIso = isoDate.trim()
        val parts = mutableListOf<String>()
        if (cleanIso.isNotBlank()) parts.add(cleanIso)
        if (displayDate.isNotBlank()) parts.add(displayDate)

        if (cleanIso.length >= 10) {
            runCatching {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
                val parsed = parser.parse(cleanIso.take(10))
                if (parsed != null) {
                    parts.add(buildTimestampSearchTokens(parsed.time))
                }
            }
        }
        return normalizeSearchText(parts.joinToString(" "))
    }

    fun buildTimestampSearchTokens(timestampMs: Long): String {
        if (timestampMs <= 0L) return ""
        val date = Date(timestampMs)
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd/MM/yyyy", Locale.US),
            SimpleDateFormat("dd/MM", Locale.US),
            SimpleDateFormat("d/M", Locale.US),
            SimpleDateFormat("dd-MM-yyyy", Locale.US),
            SimpleDateFormat("dd-MM", Locale.US),
            SimpleDateFormat("EEEE d MMM yyyy", SPANISH_LOCALE),
            SimpleDateFormat("EEEE dd MMMM", SPANISH_LOCALE)
        )
        return normalizeSearchText(formats.joinToString(" ") { it.format(date) })
    }

    fun formatFriendlyDate(isoDate: String, displayDatesByKey: Map<String, String> = emptyMap()): String {
        val cleanIso = isoDate.trim()
        val display = displayDatesByKey[cleanIso]
        if (!display.isNullOrBlank()) return "$display ($cleanIso)"
        if (cleanIso.length >= 10) {
            runCatching {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
                    .parse(cleanIso.take(10))
                if (parsed != null) {
                    val label = SimpleDateFormat("EEE dd/MM/yyyy", SPANISH_LOCALE).format(parsed)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(SPANISH_LOCALE) else it.toString() }
                    return label
                }
            }
        }
        return cleanIso.ifBlank { "Sin fecha" }
    }

    fun formatTimestampDate(timestampMs: Long): String {
        if (timestampMs <= 0L) return "Sin registro"
        return SimpleDateFormat("dd/MM/yyyy", SPANISH_LOCALE).format(Date(timestampMs))
    }

    fun matchesTokens(query: String, haystackParts: List<String>): Boolean {
        val normalizedQuery = normalizeSearchText(query)
        if (normalizedQuery.isBlank()) return true
        val queryTokens = normalizedQuery.split(" ").filter { it.isNotBlank() }
        if (queryTokens.isEmpty()) return true
        val haystack = normalizeSearchText(haystackParts.filter { it.isNotBlank() }.joinToString(" "))
        return queryTokens.all { token -> haystack.contains(token) }
    }

    fun filterVisits(
        visits: List<PsaHomeVisit>,
        query: String,
        scope: AgendaSearchScope = AgendaSearchScope.ALL,
        displayDatesByKey: Map<String, String> = emptyMap()
    ): List<PsaHomeVisit> {
        if (query.isBlank()) return visits
        return visits.filter { visit ->
            val nameTokens = listOf(
                visit.customerName,
                visit.phone,
                visit.address,
                visit.neighborhood,
                visit.modelInterest
            )
            val dateTokens = listOf(
                buildIsoDateSearchTokens(visit.scheduledDate, displayDatesByKey[visit.scheduledDate].orEmpty()),
                visit.scheduledTime
            )
            when (scope) {
                AgendaSearchScope.ALL -> matchesTokens(query, nameTokens + dateTokens)
                AgendaSearchScope.NAME -> matchesTokens(query, nameTokens)
                AgendaSearchScope.DATE -> matchesTokens(query, dateTokens)
            }
        }.sortedWith(compareBy<PsaHomeVisit> { it.scheduledDate }.thenBy { it.scheduledTime })
    }

    fun filterLeads(
        leads: List<PsaContactLead>,
        query: String,
        scope: AgendaSearchScope = AgendaSearchScope.ALL
    ): List<PsaContactLead> {
        if (query.isBlank()) return leads
        return leads.filter { lead ->
            val nameTokens = listOf(
                lead.name,
                lead.phone,
                lead.address,
                lead.neighborhood,
                lead.notes,
                lead.source
            )
            val dateTokens = listOf(
                buildIsoDateSearchTokens(lead.rescheduleDate),
                buildTimestampSearchTokens(lead.lastContactDateMs),
                buildTimestampSearchTokens(lead.createdAtMs)
            )
            when (scope) {
                AgendaSearchScope.ALL -> matchesTokens(query, nameTokens + dateTokens)
                AgendaSearchScope.NAME -> matchesTokens(query, nameTokens)
                AgendaSearchScope.DATE -> matchesTokens(query, dateTokens)
            }
        }
    }

    fun filterPostSale(
        items: List<PsaPostSaleInstallation>,
        query: String,
        scope: AgendaSearchScope = AgendaSearchScope.ALL
    ): List<PsaPostSaleInstallation> {
        if (query.isBlank()) return items
        return items.filter { item ->
            val nameTokens = listOf(
                item.customerName,
                item.customerPhone,
                item.purifierName,
                item.notes
            )
            val dateTokens = listOf(
                buildTimestampSearchTokens(item.nextReplacementAtMs),
                buildTimestampSearchTokens(item.installedAtMs)
            )
            when (scope) {
                AgendaSearchScope.ALL -> matchesTokens(query, nameTokens + dateTokens)
                AgendaSearchScope.NAME -> matchesTokens(query, nameTokens)
                AgendaSearchScope.DATE -> matchesTokens(query, dateTokens)
            }
        }.sortedBy { it.nextReplacementAtMs }
    }

    fun filterUniqueDates(
        uniqueDates: List<Pair<String, String>>,
        query: String
    ): List<Pair<String, String>> {
        if (query.isBlank()) return uniqueDates
        return uniqueDates.filter { (dateKey, displayDate) ->
            matchesTokens(query, listOf(buildIsoDateSearchTokens(dateKey, displayDate)))
        }
    }
}
