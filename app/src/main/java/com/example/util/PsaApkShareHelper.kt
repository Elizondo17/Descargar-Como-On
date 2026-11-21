package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PsaApkShareHelper {

    const val EXPORT_APK_FILE_NAME = "ComoOn-PSA.apk"
    private const val SHARED_APK_DIR = "shared_apk"

    /**
     * Copies the currently installed APK (`sourceDir`) into a clean FileProvider-accessible
     * file named `ComoOn-PSA.apk` so the Superuser can send it directly via WhatsApp
     * or upload it to Google Drive without needing a web hosting server.
     */
    fun prepareShareableApkFile(context: Context): Result<File> {
        return runCatching {
            val sourcePath = context.applicationInfo?.sourceDir
                ?.takeIf { it.isNotBlank() }
                ?: error("No se encontró el archivo APK instalado en este dispositivo.")
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists() || !sourceFile.canRead()) {
                error("No se puede leer el paquete APK instalado.")
            }

            val shareDir = File(context.cacheDir, SHARED_APK_DIR).apply { mkdirs() }
            val destFile = File(shareDir, EXPORT_APK_FILE_NAME)

            // Re-copy if missing or size changed (e.g., after an update)
            if (!destFile.exists() || destFile.length() != sourceFile.length()) {
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                    }
                }
            }
            destFile
        }
    }

    fun buildApkShareChooserIntent(context: Context, apkFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, EXPORT_APK_FILE_NAME)
            putExtra(
                Intent.EXTRA_TEXT,
                "📲 Instalador oficial Como On PSA ($EXPORT_APK_FILE_NAME).\n\n👉 Ábrelo, permite la instalación en Android y entra con 'Continuar con Google'."
            )
            clipData = ClipData.newRawUri(EXPORT_APK_FILE_NAME, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(
            sendIntent,
            "Compartir APK por WhatsApp o Guardar en Drive"
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
