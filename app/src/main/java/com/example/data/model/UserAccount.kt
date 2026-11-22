package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserAccount(
    @PrimaryKey
    val id: String,
    val username: String,
    val token: String,
    val role: String, // "SUPERUSER" or "OPERATOR"
    val daysGranted: Int,
    val validFromMs: Long,
    val expiresAtMs: Long,
    val isBlocked: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val mustChangePassword: Boolean = false,
    val hasCustomPassword: Boolean = false,
    val passwordChangedAt: Long? = null,
    val canAccessLegacyModules: Boolean = false,
    val email: String = "",
    val maxDevices: Int = 1,
    val boundDeviceIdsSerialized: String = "",
    val boundDeviceLabelsSerialized: String = "",
    val ownerUid: String = "",
    val lastOnlineVerifiedAtMs: Long = 0L,
    val lastTamperAlertAtMs: Long = 0L,
    val updateRequired: Boolean = false,
    val requiredVersionCode: Int = 0,
    val updateLockReason: String = ""
) {
    val createdAtMs: Long
        get() = createdAt

    val normalizedEmail: String
        get() = email.trim().lowercase()

    val boundDeviceIds: List<String>
        get() = boundDeviceIdsSerialized
            .split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    val boundDeviceLabels: List<String>
        get() = boundDeviceLabelsSerialized
            .split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    val boundDevicesCount: Int
        get() = boundDeviceIds.size

    fun isDeviceAllowed(deviceId: String): Boolean {
        if (deviceId.isBlank()) return false
        return boundDeviceIds.contains(deviceId)
    }

    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAtMs

    val isActive: Boolean
        get() = !isBlocked && !isExpired

    val remainingDays: Long
        get() {
            if (role == "SUPERUSER") return 9999L
            val diff = expiresAtMs - System.currentTimeMillis()
            if (diff <= 0L) return 0L
            val dayMs = 1000L * 60 * 60 * 24
            return (diff + dayMs - 1L) / dayMs
        }
}

data class RoleVisibilityConfig(
    val opPsaEnabled: Boolean = true,
    val opScriptsEnabled: Boolean = false,
    val opAutomationEnabled: Boolean = false,
    val opDashboardEnabled: Boolean = false,
    val opAiSalesEnabled: Boolean = true,
    val opPricingEditEnabled: Boolean = false,
    val suPsaEnabled: Boolean = true,
    val suScriptsEnabled: Boolean = true,
    val suAutomationEnabled: Boolean = true,
    val suDashboardEnabled: Boolean = true,
    val suSuperuserEnabled: Boolean = true
) {
    fun isTabVisible(tabName: String, isSuperuser: Boolean): Boolean {
        return when (tabName) {
            "PSA_WORKSPACE" -> if (isSuperuser) suPsaEnabled else opPsaEnabled
            "SCRIPTS" -> if (isSuperuser) suScriptsEnabled else opScriptsEnabled
            "AUTOMATION" -> if (isSuperuser) suAutomationEnabled else opAutomationEnabled
            "DASHBOARD" -> if (isSuperuser) suDashboardEnabled else opDashboardEnabled
            "SUPERUSER" -> if (isSuperuser) suSuperuserEnabled else false
            else -> false
        }
    }
}

