package com.example.util

import android.content.Intent
import android.net.Uri
import com.example.data.repository.EzoRepository

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkDownloadUrl: String,
    val releaseNotes: String = "",
    val updatedAtMs: Long = 0L,
    val forceUpdate: Boolean = false
)

data class UpdateLockEvaluation(
    val isLocked: Boolean,
    val requiredVersionCode: Int,
    val reasonText: String,
    val shouldAutoClearUserFlag: Boolean
)

object AppUpdateChecker {

    /**
     * Normalizes GitHub / Google Drive URLs into a direct download link and requires strict HTTPS.
     */
    fun normalizeDownloadUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""
        val normalized = EzoRepository.normalizeApkDownloadUrl(trimmed)
        if (!normalized.startsWith("https://", ignoreCase = true)) {
            return ""
        }
        val uri = runCatching { Uri.parse(normalized) }.getOrNull() ?: return ""
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) {
            return ""
        }
        return normalized
    }

    /**
     * Compares server versionCode against installed versionCode and optional session dismissal.
     */
    fun isUpdateAvailable(
        currentVersionCode: Int,
        serverInfo: AppUpdateInfo?,
        dismissedVersionCode: Int = 0
    ): Boolean {
        if (serverInfo == null) return false
        if (serverInfo.latestVersionCode <= currentVersionCode) return false
        if (!serverInfo.forceUpdate && serverInfo.latestVersionCode == dismissedVersionCode) return false
        return normalizeDownloadUrl(serverInfo.apkDownloadUrl).isNotBlank()
    }

    /**
     * Evaluates whether a non-superuser seller must remain temporarily locked until updating the APK,
     * and whether their individual updateRequired flag should automatically clear because they opened
     * a build with versionCode >= requiredVersionCode.
     */
    fun evaluateUpdateLockState(
        installedVersionCode: Int,
        user: com.example.data.model.UserAccount?,
        serverInfo: AppUpdateInfo?
    ): UpdateLockEvaluation {
        if (user == null || user.role == "SUPERUSER") {
            return UpdateLockEvaluation(
                isLocked = false,
                requiredVersionCode = 0,
                reasonText = "",
                shouldAutoClearUserFlag = false
            )
        }

        val serverLatestCode = serverInfo?.latestVersionCode ?: 0
        val userRequiredCode = if (user.updateRequired) {
            maxOf(user.requiredVersionCode, serverLatestCode)
        } else {
            0
        }
        val globalRequiredCode = if (serverInfo?.forceUpdate == true && serverLatestCode > 0) {
            serverLatestCode
        } else {
            0
        }

        val effectiveRequiredCode = maxOf(userRequiredCode, globalRequiredCode)
        val userNeedsUpgrade = user.updateRequired && (
            userRequiredCode <= 0 || installedVersionCode < userRequiredCode
            )
        val globalNeedsUpgrade = globalRequiredCode > 0 && installedVersionCode < globalRequiredCode
        val isLocked = userNeedsUpgrade || globalNeedsUpgrade

        val shouldAutoClear = user.updateRequired && !userNeedsUpgrade && userRequiredCode > 0
        val customReason = user.updateLockReason.trim()
        val reason = when {
            customReason.isNotBlank() -> customReason
            globalNeedsUpgrade && !serverInfo?.releaseNotes.isNullOrBlank() -> serverInfo!!.releaseNotes
            effectiveRequiredCode > 0 ->
                "El Administrador solicitó actualizar a la compilación $effectiveRequiredCode para volver a habilitar tu acceso."
            else ->
                "El Administrador bloqueó temporalmente este acceso hasta instalar la nueva versión de la aplicación."
        }

        return UpdateLockEvaluation(
            isLocked = isLocked,
            requiredVersionCode = effectiveRequiredCode,
            reasonText = reason,
            shouldAutoClearUserFlag = shouldAutoClear
        )
    }

    /**
     * Play Policy compliant external browser download intent (never uses REQUEST_INSTALL_PACKAGES).
     */
    fun buildBrowserDownloadIntent(rawUrl: String): Intent? {
        val safeUrl = normalizeDownloadUrl(rawUrl)
        if (safeUrl.isBlank()) return null
        return Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
