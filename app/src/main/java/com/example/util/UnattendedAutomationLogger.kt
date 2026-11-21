package com.example.util

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sistema de registro de eventos (log) para diagnóstico del modo 100% desatendido.
 * Guarda en un archivo local: fecha, hora, contacto, estado del envío y si se logró salir del chat correctamente.
 */
object UnattendedAutomationLogger {

    private const val LOGS_DIR = "logs"
    private const val LOG_FILE_NAME = "unattended_automation.log"
    private const val MAX_LOG_SIZE_BYTES = 2 * 1024 * 1024 // 2 MB max

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private fun getLogFile(context: Context): File {
        val dir = File(context.filesDir, LOGS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, LOG_FILE_NAME)
        if (!file.exists()) {
            file.createNewFile()
        }
        // Rotate if exceeds max size
        if (file.length() > MAX_LOG_SIZE_BYTES) {
            val backup = File(dir, "unattended_automation.old.log")
            if (backup.exists()) backup.delete()
            file.renameTo(backup)
            file.createNewFile()
        }
        return file
    }

    suspend fun logEvent(
        context: Context,
        tag: String,
        recipient: String,
        status: String,
        exitedChatSuccessfully: Boolean,
        details: String = ""
    ) = withContext(Dispatchers.IO) {
        try {
            val timestamp = dateFormat.format(Date())
            val exitStatusStr = if (exitedChatSuccessfully) "SALIDA_CHAT: OK" else "SALIDA_CHAT: PENDIENTE/FALLIDO"
            val logLine = "[$timestamp] [$tag] Destinatario: $recipient | Estado: $status | $exitStatusStr | $details\n"

            val file = getLogFile(context)
            FileWriter(file, true).use { writer ->
                writer.append(logLine)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun logEventSync(
        context: Context,
        tag: String,
        recipient: String,
        status: String,
        exitedChatSuccessfully: Boolean,
        details: String = ""
    ) {
        try {
            val timestamp = dateFormat.format(Date())
            val exitStatusStr = if (exitedChatSuccessfully) "SALIDA_CHAT: OK" else "SALIDA_CHAT: PENDIENTE/FALLIDO"
            val logLine = "[$timestamp] [$tag] Destinatario: $recipient | Estado: $status | $exitStatusStr | $details\n"

            val file = getLogFile(context)
            FileWriter(file, true).use { writer ->
                writer.append(logLine)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun readLogs(context: Context, maxLines: Int = 200): List<String> = withContext(Dispatchers.IO) {
        try {
            val file = getLogFile(context)
            if (!file.exists()) return@withContext emptyList()
            val lines = file.readLines()
            if (lines.size > maxLines) {
                lines.takeLast(maxLines)
            } else {
                lines
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun clearLogs(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getLogFile(context)
            if (file.exists()) {
                file.writeText("")
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getLogFilePath(context: Context): String {
        return getLogFile(context).absolutePath
    }
}
