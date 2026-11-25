package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.example.data.model.UserAccount
import com.example.data.repository.FirestoreTeamRepository

enum class AccessLockReason {
    NONE,
    PENDING_SUPERUSER_APPROVAL,
    ADMIN_BLOCKED,
    UPDATE_REQUIRED,
    DAYS_EXPIRED,
    OFFLINE_OVER_24H,
    REBOOT_VERIFICATION_REQUIRED,
    CLOCK_TAMPER_DETECTED
}

data class LicenseEvaluationResult(
    val reason: AccessLockReason,
    val remainingDays: Long,
    val isExpiringSoon: Boolean,
    val offlineElapsedMs: Long,
    val lastOnlineVerifiedAtMs: Long,
    val estimatedRealNowMs: Long,
    val pendingTamperAlertAtMs: Long
) {
    val isLocked: Boolean
        get() = reason != AccessLockReason.NONE
}

/**
 * Local integrity & offline security guard:
 * - Enforces 24h maximum offline grace window for operators
 * - Enforces mandatory online verification after device reboot
 * - Detects clock rollback using SystemClock.elapsedRealtime() + server time offset
 * - Keeps leonelizondo16@gmail.com (SUPERUSER) exempt from offline lockouts
 */
object LicenseSecurityGuard {

    private const val PREFS_NAME = "como_on_license_guard_prefs"
    private const val KEY_USER_KEY = "verified_user_key"
    private const val KEY_LAST_VERIFIED_SERVER_WALL_MS = "last_verified_server_wall_ms"
    private const val KEY_LAST_VERIFIED_DEVICE_WALL_MS = "last_verified_device_wall_ms"
    private const val KEY_LAST_VERIFIED_ELAPSED_MS = "last_verified_elapsed_ms"
    private const val KEY_LAST_SEEN_ELAPSED_MS = "last_seen_elapsed_ms"
    private const val KEY_LAST_SEEN_WALL_MS = "last_seen_wall_ms"
    private const val KEY_LAST_VERIFIED_BOOT_COUNT = "last_verified_boot_count"
    private const val KEY_SERVER_OFFSET_MS = "server_time_offset_ms"
    private const val KEY_TAMPER_FLAG = "tamper_detected_flag"
    private const val KEY_PENDING_TAMPER_REPORT_MS = "pending_tamper_report_ms"
    private const val KEY_REBOOT_PENDING = "reboot_verification_pending"

    const val OFFLINE_LOCK_THRESHOLD_MS: Long = 24L * 60L * 60L * 1000L
    const val CLOCK_ROLLBACK_TOLERANCE_MS: Long = 5L * 60L * 1000L
    const val EXPIRING_SOON_DAYS_THRESHOLD: Long = 3L
    private const val DAY_MS: Long = 24L * 60L * 60L * 1000L

    fun userKeyFor(user: UserAccount): String {
        return user.normalizedEmail.ifBlank { user.id.trim().lowercase() }
    }

    fun isExemptSuperuser(user: UserAccount?): Boolean {
        if (user == null || user.role != "SUPERUSER") return false
        return user.email.isBlank() ||
            user.email.equals(FirestoreTeamRepository.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)
    }

    fun getBootCount(context: Context): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
            } else {
                -1
            }
        } catch (_: Exception) {
            -1
        }
    }

    fun hasInternetConnectivity(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
        } catch (_: Exception) {
            false
        }
    }

    fun registerNetworkCallback(
        context: Context,
        onNetworkAvailable: () -> Unit,
        onNetworkLost: () -> Unit
    ): ConnectivityManager.NetworkCallback? {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return null
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    onNetworkAvailable()
                }

                override fun onLost(network: Network) {
                    onNetworkLost()
                }
            }
            cm.registerDefaultNetworkCallback(callback)
            callback
        } catch (_: Exception) {
            null
        }
    }

    fun unregisterNetworkCallback(
        context: Context,
        callback: ConnectivityManager.NetworkCallback?
    ) {
        if (callback == null) return
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            cm?.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
    }

    fun recordOnlineVerification(
        context: Context,
        user: UserAccount,
        serverWallMs: Long,
        deviceWallMs: Long = System.currentTimeMillis(),
        elapsedMs: Long = SystemClock.elapsedRealtime(),
        bootCount: Int = getBootCount(context),
        clearPendingTamperReport: Boolean = false
    ) {
        val safeServerWall = if (serverWallMs > 0L) serverWallMs else deviceWallMs
        val offsetMs = safeServerWall - deviceWallMs
        val userKey = userKeyFor(user)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_USER_KEY, userKey)
            .putLong(KEY_LAST_VERIFIED_SERVER_WALL_MS, safeServerWall)
            .putLong(KEY_LAST_VERIFIED_DEVICE_WALL_MS, deviceWallMs)
            .putLong(KEY_LAST_VERIFIED_ELAPSED_MS, elapsedMs)
            .putLong(KEY_LAST_SEEN_ELAPSED_MS, elapsedMs)
            .putLong(KEY_LAST_SEEN_WALL_MS, safeServerWall)
            .putInt(KEY_LAST_VERIFIED_BOOT_COUNT, bootCount)
            .putLong(KEY_SERVER_OFFSET_MS, offsetMs)
            .putBoolean(KEY_TAMPER_FLAG, false)
            .putBoolean(KEY_REBOOT_PENDING, false)
            .apply {
                if (clearPendingTamperReport) {
                    putLong(KEY_PENDING_TAMPER_REPORT_MS, 0L)
                }
            }
            .apply()
    }

    fun getPendingTamperReportMs(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_PENDING_TAMPER_REPORT_MS, 0L)
    }

    fun clearPendingTamperReport(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_PENDING_TAMPER_REPORT_MS, 0L).apply()
    }

    fun getLastVerifiedServerWallMs(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_VERIFIED_SERVER_WALL_MS, 0L)
    }

    fun clearState(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun evaluateAccess(
        context: Context,
        user: UserAccount?,
        nowDeviceWallMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = SystemClock.elapsedRealtime(),
        currentBootCount: Int = getBootCount(context)
    ): LicenseEvaluationResult {
        if (user == null) {
            return LicenseEvaluationResult(
                reason = AccessLockReason.NONE,
                remainingDays = 0L,
                isExpiringSoon = false,
                offlineElapsedMs = 0L,
                lastOnlineVerifiedAtMs = 0L,
                estimatedRealNowMs = nowDeviceWallMs,
                pendingTamperAlertAtMs = 0L
            )
        }

        if (isExemptSuperuser(user)) {
            return LicenseEvaluationResult(
                reason = if (user.isBlocked) AccessLockReason.ADMIN_BLOCKED else AccessLockReason.NONE,
                remainingDays = 9999L,
                isExpiringSoon = false,
                offlineElapsedMs = 0L,
                lastOnlineVerifiedAtMs = maxOf(user.lastOnlineVerifiedAtMs, nowDeviceWallMs),
                estimatedRealNowMs = nowDeviceWallMs,
                pendingTamperAlertAtMs = 0L
            )
        }

        if (user.isPendingApproval) {
            return LicenseEvaluationResult(
                reason = AccessLockReason.PENDING_SUPERUSER_APPROVAL,
                remainingDays = 0L,
                isExpiringSoon = false,
                offlineElapsedMs = 0L,
                lastOnlineVerifiedAtMs = maxOf(user.lastOnlineVerifiedAtMs, getLastVerifiedServerWallMs(context)),
                estimatedRealNowMs = nowDeviceWallMs,
                pendingTamperAlertAtMs = getPendingTamperReportMs(context)
            )
        }

        if (user.isBlocked) {
            return LicenseEvaluationResult(
                reason = AccessLockReason.ADMIN_BLOCKED,
                remainingDays = 0L,
                isExpiringSoon = false,
                offlineElapsedMs = 0L,
                lastOnlineVerifiedAtMs = maxOf(user.lastOnlineVerifiedAtMs, getLastVerifiedServerWallMs(context)),
                estimatedRealNowMs = nowDeviceWallMs,
                pendingTamperAlertAtMs = getPendingTamperReportMs(context)
            )
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userKey = userKeyFor(user)
        val storedKey = prefs.getString(KEY_USER_KEY, "").orEmpty()
        val lastVerifiedServerWallMs = if (storedKey == userKey) {
            prefs.getLong(KEY_LAST_VERIFIED_SERVER_WALL_MS, 0L)
        } else {
            0L
        }
        val lastVerifiedDeviceWallMs = prefs.getLong(KEY_LAST_VERIFIED_DEVICE_WALL_MS, 0L)
        val lastVerifiedElapsedMs = prefs.getLong(KEY_LAST_VERIFIED_ELAPSED_MS, 0L)
        val lastSeenElapsedMs = prefs.getLong(KEY_LAST_SEEN_ELAPSED_MS, 0L)
        val lastSeenWallMs = prefs.getLong(KEY_LAST_SEEN_WALL_MS, 0L)
        val lastVerifiedBootCount = prefs.getInt(KEY_LAST_VERIFIED_BOOT_COUNT, -1)
        val serverOffsetMs = prefs.getLong(KEY_SERVER_OFFSET_MS, 0L)
        var tamperFlag = storedKey == userKey && prefs.getBoolean(KEY_TAMPER_FLAG, false)
        var pendingTamperReportMs = prefs.getLong(KEY_PENDING_TAMPER_REPORT_MS, 0L)

        // If user has never verified on this process/device prefs (e.g. cold open after wipe/no cache),
        // require online verification unless their license is already expired.
        if (storedKey != userKey || lastVerifiedServerWallMs <= 0L) {
            val expiredByDeviceClock = nowDeviceWallMs > user.expiresAtMs
            val effectiveLastOnline = user.lastOnlineVerifiedAtMs
            return LicenseEvaluationResult(
                reason = if (expiredByDeviceClock) AccessLockReason.DAYS_EXPIRED else AccessLockReason.REBOOT_VERIFICATION_REQUIRED,
                remainingDays = if (expiredByDeviceClock) 0L else user.remainingDays,
                isExpiringSoon = false,
                offlineElapsedMs = 0L,
                lastOnlineVerifiedAtMs = effectiveLastOnline,
                estimatedRealNowMs = nowDeviceWallMs,
                pendingTamperAlertAtMs = pendingTamperReportMs
            )
        }

        val bootCountChanged = lastVerifiedBootCount > 0 &&
            currentBootCount > 0 &&
            currentBootCount != lastVerifiedBootCount
        val elapsedResetDetected = lastVerifiedElapsedMs > 0L &&
            (nowElapsedMs + 30_000L < lastVerifiedElapsedMs)
        val sameBootSession = !bootCountChanged && !elapsedResetDetected && nowElapsedMs >= lastSeenElapsedMs

        val adjustedNowMs = nowDeviceWallMs + serverOffsetMs
        // Detect device clock rollback (both within the same boot and across device reboots)
        if (!tamperFlag) {
            val rollbackSameBoot = sameBootSession && lastSeenElapsedMs > 0L && run {
                val monotonicExpectedWall = lastSeenWallMs + (nowElapsedMs - lastSeenElapsedMs).coerceAtLeast(0L)
                (adjustedNowMs + CLOCK_ROLLBACK_TOLERANCE_MS) < monotonicExpectedWall &&
                    (adjustedNowMs + CLOCK_ROLLBACK_TOLERANCE_MS) < lastSeenWallMs
            }
            val rollbackCrossReboot = lastSeenWallMs > 0L &&
                (adjustedNowMs + CLOCK_ROLLBACK_TOLERANCE_MS) < lastSeenWallMs
            val rollbackBeforeLastVerified = (adjustedNowMs + CLOCK_ROLLBACK_TOLERANCE_MS) < lastVerifiedServerWallMs ||
                (lastVerifiedDeviceWallMs > 0L && (nowDeviceWallMs + CLOCK_ROLLBACK_TOLERANCE_MS) < lastVerifiedDeviceWallMs)

            if (rollbackSameBoot || rollbackCrossReboot || rollbackBeforeLastVerified) {
                tamperFlag = true
                val estimatedAnomalyTime = maxOf(
                    lastSeenWallMs + (nowElapsedMs - lastSeenElapsedMs).coerceAtLeast(0L),
                    lastVerifiedServerWallMs
                )
                pendingTamperReportMs = estimatedAnomalyTime
                prefs.edit()
                    .putBoolean(KEY_TAMPER_FLAG, true)
                    .putLong(KEY_PENDING_TAMPER_REPORT_MS, estimatedAnomalyTime)
                    .apply()
            } else if (sameBootSession) {
                prefs.edit()
                    .putLong(KEY_LAST_SEEN_ELAPSED_MS, nowElapsedMs)
                    .putLong(KEY_LAST_SEEN_WALL_MS, maxOf(lastSeenWallMs, adjustedNowMs))
                    .apply()
            } else if (adjustedNowMs > lastSeenWallMs) {
                prefs.edit()
                    .putLong(KEY_LAST_SEEN_WALL_MS, maxOf(lastSeenWallMs, adjustedNowMs))
                    .apply()
            }
        }

        // Estimate real current wall-clock time anchored by server + monotonic uptime
        val elapsedSinceVerifiedMs = if (!bootCountChanged && nowElapsedMs >= lastVerifiedElapsedMs) {
            nowElapsedMs - lastVerifiedElapsedMs
        } else {
            0L
        }
        val estimatedRealNowMs = maxOf(
            nowDeviceWallMs + serverOffsetMs,
            lastVerifiedServerWallMs + elapsedSinceVerifiedMs,
            lastSeenWallMs
        )
        val offlineElapsedMs = maxOf(
            elapsedSinceVerifiedMs,
            (estimatedRealNowMs - lastVerifiedServerWallMs).coerceAtLeast(0L)
        )

        val remainingDays = if (estimatedRealNowMs > user.expiresAtMs) {
            0L
        } else {
            ((user.expiresAtMs - estimatedRealNowMs + DAY_MS - 1L) / DAY_MS).coerceAtLeast(1L)
        }

        val lastOnlineMs = maxOf(lastVerifiedServerWallMs, user.lastOnlineVerifiedAtMs)

        if (tamperFlag) {
            return LicenseEvaluationResult(
                reason = AccessLockReason.CLOCK_TAMPER_DETECTED,
                remainingDays = remainingDays,
                isExpiringSoon = false,
                offlineElapsedMs = offlineElapsedMs,
                lastOnlineVerifiedAtMs = lastOnlineMs,
                estimatedRealNowMs = estimatedRealNowMs,
                pendingTamperAlertAtMs = pendingTamperReportMs
            )
        }

        // Check expiration before reboot/offline so expired licenses display clear "days ran out" copy
        if (estimatedRealNowMs > user.expiresAtMs) {
            return LicenseEvaluationResult(
                reason = AccessLockReason.DAYS_EXPIRED,
                remainingDays = 0L,
                isExpiringSoon = false,
                offlineElapsedMs = offlineElapsedMs,
                lastOnlineVerifiedAtMs = lastOnlineMs,
                estimatedRealNowMs = estimatedRealNowMs,
                pendingTamperAlertAtMs = pendingTamperReportMs
            )
        }

        // Check reboot verification pending
        val rebootPending = prefs.getBoolean(KEY_REBOOT_PENDING, false) || bootCountChanged || elapsedResetDetected
        if (rebootPending) {
            if (!prefs.getBoolean(KEY_REBOOT_PENDING, false)) {
                prefs.edit().putBoolean(KEY_REBOOT_PENDING, true).apply()
            }
            return LicenseEvaluationResult(
                reason = AccessLockReason.REBOOT_VERIFICATION_REQUIRED,
                remainingDays = remainingDays,
                isExpiringSoon = false,
                offlineElapsedMs = offlineElapsedMs,
                lastOnlineVerifiedAtMs = lastOnlineMs,
                estimatedRealNowMs = estimatedRealNowMs,
                pendingTamperAlertAtMs = pendingTamperReportMs
            )
        }

        // Check 24h continuous offline threshold
        if (offlineElapsedMs >= OFFLINE_LOCK_THRESHOLD_MS) {
            return LicenseEvaluationResult(
                reason = AccessLockReason.OFFLINE_OVER_24H,
                remainingDays = remainingDays,
                isExpiringSoon = false,
                offlineElapsedMs = offlineElapsedMs,
                lastOnlineVerifiedAtMs = lastOnlineMs,
                estimatedRealNowMs = estimatedRealNowMs,
                pendingTamperAlertAtMs = pendingTamperReportMs
            )
        }

        return LicenseEvaluationResult(
            reason = AccessLockReason.NONE,
            remainingDays = remainingDays,
            isExpiringSoon = remainingDays in 1L..EXPIRING_SOON_DAYS_THRESHOLD,
            offlineElapsedMs = offlineElapsedMs,
            lastOnlineVerifiedAtMs = lastOnlineMs,
            estimatedRealNowMs = estimatedRealNowMs,
            pendingTamperAlertAtMs = pendingTamperReportMs
        )
    }
}
