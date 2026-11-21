package com.example.util

import com.example.data.model.UserAccount
import java.security.MessageDigest
import java.util.UUID

object LicenseManager {

    private const val SECRET_SALT = "COMO_ON_CRM_SECURE_SALT_2026_V1"

    /**
     * Generates a short, portable, cryptographically verifiable activation key
     * for the operator's initial login.
     *
     * Short Format: ON<DAYS>-<4HEX>
     * Example: ON15-8F2B or ON30-A4D9
     */
    fun generatePortableKey(username: String, days: Int): String {
        val safeDays = if (days <= 0) 15 else days
        val checksum = computeShortChecksum(username, safeDays)
        return "ON$safeDays-$checksum"
    }

    /**
     * Verifies if a given token is a valid portable activation key for the given username.
     * If valid, constructs a UserAccount with the embedded expiration days and mustChangePassword = true.
     */
    fun verifyAndCreatePortableUser(username: String, keyInput: String): UserAccount? {
        val cleanUser = username.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        val cleanKey = keyInput.trim().uppercase()

        // 1. Check Short Key format: ON<DAYS>-<4HEX> (e.g. ON15-8F2B)
        val shortRegex = Regex("""^ON(\d+)-([A-F0-9]{4})$""")
        val shortMatch = shortRegex.find(cleanKey)
        if (shortMatch != null) {
            val days = shortMatch.groupValues[1].toIntOrNull() ?: return null
            val tokenChecksum = shortMatch.groupValues[2]
            val expectedChecksum = computeShortChecksum(cleanUser, days)

            if (tokenChecksum.equals(expectedChecksum, ignoreCase = true)) {
                val now = System.currentTimeMillis()
                val durationMs = days.toLong() * 24L * 60L * 60L * 1000L
                return UserAccount(
                    id = "user-" + UUID.randomUUID().toString().substring(0, 8),
                    username = username.trim(),
                    role = "OPERATOR",
                    token = keyInput.trim(),
                    validFromMs = now,
                    expiresAtMs = now + durationMs,
                    daysGranted = days,
                    isBlocked = false,
                    createdAt = now,
                    notes = "Clave de Activación Inicial ($days días)",
                    mustChangePassword = true,
                    hasCustomPassword = false,
                    passwordChangedAt = null
                )
            }
        }

        // 2. Check legacy format: COMO-<USERNAME>-<DAYS>D-<CHECKSUM>
        if (cleanKey.startsWith("COMO-") || cleanKey.startsWith("KEY-")) {
            val parts = cleanKey.split("-")
            if (parts.size >= 4) {
                val tokenUsername = parts[1].lowercase()
                val daysPart = parts[2].removeSuffix("D")
                val tokenChecksum = parts[3]
                val days = daysPart.toIntOrNull() ?: return null

                if (cleanUser.isNotBlank() && cleanUser != tokenUsername) {
                    return null
                }

                val expectedChecksum = computeLegacyChecksum(tokenUsername, days)
                if (tokenChecksum.equals(expectedChecksum, ignoreCase = true)) {
                    val now = System.currentTimeMillis()
                    val durationMs = days.toLong() * 24L * 60L * 60L * 1000L
                    return UserAccount(
                        id = "user-" + UUID.randomUUID().toString().substring(0, 8),
                        username = username.trim(),
                        role = "OPERATOR",
                        token = keyInput.trim(),
                        validFromMs = now,
                        expiresAtMs = now + durationMs,
                        daysGranted = days,
                        isBlocked = false,
                        createdAt = now,
                        notes = "Licencia Activada ($days días)",
                        mustChangePassword = true,
                        hasCustomPassword = false,
                        passwordChangedAt = null
                    )
                }
            }
        }

        return null
    }

    private fun computeShortChecksum(username: String, days: Int): String {
        val clean = username.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        val input = "$SECRET_SALT:$clean:$days"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.take(2).joinToString("") { "%02X".format(it) } // 2 bytes = 4 hex chars e.g. "8F2B"
    }

    private fun computeLegacyChecksum(username: String, days: Int): String {
        val input = "$SECRET_SALT:${username.uppercase()}:$days"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.take(3).joinToString("") { "%02X".format(it) }
    }
}
