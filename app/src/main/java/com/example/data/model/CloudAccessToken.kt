package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class CloudAccessTokenStatus {
    AVAILABLE,
    REDEEMED,
    REVOKED;

    companion object {
        fun fromRaw(raw: String?): CloudAccessTokenStatus {
            return entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }
                ?: AVAILABLE
        }
    }
}

enum class LicenseExpirationState {
    ACTIVE,
    EXPIRING_SOON,
    CRITICAL_24H,
    EXPIRED,
    BLOCKED,
    PENDING_ACTIVATION,
    REVOKED
}

@Keep
@IgnoreExtraProperties
data class CloudAccessToken(
    val tokenCode: String = "",
    val assignedUsername: String = "",
    val targetEmail: String = "",
    val daysGranted: Int = 30,
    val maxDevices: Int = 1,
    val notes: String = "",
    val status: String = CloudAccessTokenStatus.AVAILABLE.name,
    val redeemedByEmail: String = "",
    val redeemedByUid: String = "",
    val redeemedAtMs: Long = 0L,
    val expiresAtMs: Long = 0L,
    val createdByUid: String = "",
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L
) {
    val parsedStatus: CloudAccessTokenStatus
        get() = CloudAccessTokenStatus.fromRaw(status)
}

data class LicenseExpirationSnapshot(
    val state: LicenseExpirationState,
    val remainingMs: Long,
    val remainingDays: Long,
    val remainingHours: Long,
    val progressFraction: Float,
    val statusLabel: String,
    val countdownLabel: String,
    val isRestricted: Boolean
)

object LicenseStatusFormatter {
    private const val HOUR_MS = 60L * 60L * 1000L
    private const val DAY_MS = 24L * HOUR_MS
    const val MIN_VALIDITY_DAYS = 1
    const val MAX_VALIDITY_DAYS = 3650

    fun sanitizeCustomDays(rawInput: String): Int? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null
        val parsed = trimmed.toIntOrNull() ?: return null
        return if (parsed in MIN_VALIDITY_DAYS..MAX_VALIDITY_DAYS) parsed else null
    }

    fun computePreviewExpirationMs(daysGranted: Int, nowMs: Long = System.currentTimeMillis()): Long {
        val safeDays = daysGranted.coerceIn(MIN_VALIDITY_DAYS, MAX_VALIDITY_DAYS)
        return nowMs + (safeDays.toLong() * DAY_MS)
    }

    fun evaluateUserLicense(
        user: UserAccount,
        nowMs: Long = System.currentTimeMillis()
    ): LicenseExpirationSnapshot {
        if (user.role == "SUPERUSER") {
            return LicenseExpirationSnapshot(
                state = LicenseExpirationState.ACTIVE,
                remainingMs = Long.MAX_VALUE,
                remainingDays = 3650L,
                remainingHours = 0L,
                progressFraction = 1f,
                statusLabel = "MASTER",
                countdownLabel = "Licencia permanente",
                isRestricted = false
            )
        }

        if (user.isBlocked) {
            val diff = user.expiresAtMs - nowMs
            return LicenseExpirationSnapshot(
                state = LicenseExpirationState.BLOCKED,
                remainingMs = diff,
                remainingDays = if (diff > 0L) diff / DAY_MS else 0L,
                remainingHours = if (diff > 0L) (diff % DAY_MS) / HOUR_MS else 0L,
                progressFraction = 0f,
                statusLabel = "BLOQUEADA",
                countdownLabel = if (diff > 0L) {
                    "Bloqueada (${formatCountdown(diff)} restantes)"
                } else {
                    "Bloqueada y vencida"
                },
                isRestricted = true
            )
        }

        return evaluateWindow(
            validFromMs = user.validFromMs,
            expiresAtMs = user.expiresAtMs,
            daysGranted = user.daysGranted,
            nowMs = nowMs
        )
    }

    fun evaluateAccessToken(
        token: CloudAccessToken,
        nowMs: Long = System.currentTimeMillis()
    ): LicenseExpirationSnapshot {
        return when (token.parsedStatus) {
            CloudAccessTokenStatus.REVOKED -> LicenseExpirationSnapshot(
                state = LicenseExpirationState.REVOKED,
                remainingMs = 0L,
                remainingDays = 0L,
                remainingHours = 0L,
                progressFraction = 0f,
                statusLabel = "REVOCADO",
                countdownLabel = "Token anulado por administrador",
                isRestricted = true
            )
            CloudAccessTokenStatus.AVAILABLE -> LicenseExpirationSnapshot(
                state = LicenseExpirationState.PENDING_ACTIVATION,
                remainingMs = token.daysGranted.toLong() * DAY_MS,
                remainingDays = token.daysGranted.toLong(),
                remainingHours = 0L,
                progressFraction = 1f,
                statusLabel = "DISPONIBLE",
                countdownLabel = "Sin canjear • otorga ${token.daysGranted} días",
                isRestricted = false
            )
            CloudAccessTokenStatus.REDEEMED -> {
                val baseStart = if (token.redeemedAtMs > 0L) token.redeemedAtMs else token.createdAtMs
                val effectiveExpires = if (token.expiresAtMs > 0L) {
                    token.expiresAtMs
                } else {
                    baseStart + token.daysGranted.toLong() * DAY_MS
                }
                evaluateWindow(
                    validFromMs = baseStart,
                    expiresAtMs = effectiveExpires,
                    daysGranted = token.daysGranted,
                    nowMs = nowMs
                )
            }
        }
    }

    private fun evaluateWindow(
        validFromMs: Long,
        expiresAtMs: Long,
        daysGranted: Int,
        nowMs: Long
    ): LicenseExpirationSnapshot {
        val diff = expiresAtMs - nowMs
        if (diff <= 0L) {
            val agoDays = abs(diff) / DAY_MS
            val agoHours = (abs(diff) % DAY_MS) / HOUR_MS
            val label = when {
                agoDays >= 1L -> "Venció hace ${agoDays}d ${agoHours}h"
                agoHours >= 1L -> "Venció hace ${agoHours}h"
                else -> "Vencida recién"
            }
            return LicenseExpirationSnapshot(
                state = LicenseExpirationState.EXPIRED,
                remainingMs = diff,
                remainingDays = 0L,
                remainingHours = 0L,
                progressFraction = 0f,
                statusLabel = "EXPIRADA",
                countdownLabel = label,
                isRestricted = true
            )
        }

        val totalWindowMs = (expiresAtMs - validFromMs)
            .takeIf { it > 0L }
            ?: (daysGranted.coerceAtLeast(1).toLong() * DAY_MS)
        val fraction = (diff.toFloat() / totalWindowMs.toFloat()).coerceIn(0f, 1f)
        val days = diff / DAY_MS
        val hours = (diff % DAY_MS) / HOUR_MS

        val (state, statusLabel) = when {
            diff <= DAY_MS -> LicenseExpirationState.CRITICAL_24H to "VENCE HOY"
            diff <= 3L * DAY_MS -> LicenseExpirationState.EXPIRING_SOON to "POR VENCER"
            else -> LicenseExpirationState.ACTIVE to "ACTIVA"
        }

        return LicenseExpirationSnapshot(
            state = state,
            remainingMs = diff,
            remainingDays = days,
            remainingHours = hours,
            progressFraction = fraction,
            statusLabel = statusLabel,
            countdownLabel = formatCountdown(diff),
            isRestricted = false
        )
    }

    fun formatCountdown(diffMs: Long): String {
        if (diffMs <= 0L) return "Expirada"
        val days = diffMs / DAY_MS
        val hours = (diffMs % DAY_MS) / HOUR_MS
        val minutes = (diffMs % HOUR_MS) / (60L * 1000L)
        return when {
            days >= 1L -> "${days}d ${hours}h restantes"
            hours >= 1L -> "${hours}h ${minutes}m restantes"
            else -> "${minutes.coerceAtLeast(1L)}m restantes"
        }
    }

    fun formatExpirationDate(expiresAtMs: Long): String {
        if (expiresAtMs <= 0L) return "Sin activar"
        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return fmt.format(Date(expiresAtMs))
    }
}
