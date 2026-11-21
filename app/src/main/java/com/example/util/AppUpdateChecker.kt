package com.example.util

import android.content.Intent
import android.net.Uri
import com.example.data.repository.EzoRepository

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkDownloadUrl: String,
    val releaseNotes: String = "",
    val updatedAtMs: Long = 0L
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
        if (serverInfo.latestVersionCode == dismissedVersionCode) return false
        return normalizeDownloadUrl(serverInfo.apkDownloadUrl).isNotBlank()
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
