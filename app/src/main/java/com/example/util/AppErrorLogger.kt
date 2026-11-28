package com.example.util

import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

enum class AppErrorStage {
    ON_CREATE,
    LOGCAT_FIREBASE
}

data class PendingAppError(
    val id: String,
    val stage: String,
    val errorMessage: String,
    val stackTraceExcerpt: String,
    val logcatExcerpt: String,
    val deviceModel: String,
    val androidSdk: Int,
    val appVersionName: String,
    val appVersionCode: Int,
    val occurredAtMs: Long
)

data class AppErrorReport(
    val id: String = "",
    val ownerUid: String = "",
    val reporterEmail: String = "",
    val reporterName: String = "",
    val stage: String = AppErrorStage.ON_CREATE.name,
    val errorMessage: String = "",
    val stackTraceExcerpt: String = "",
    val logcatExcerpt: String = "",
    val deviceModel: String = "",
    val androidSdk: Int = Build.VERSION.SDK_INT,
    val appVersionName: String = "",
    val appVersionCode: Int = 1,
    val status: String = "OPEN",
    val occurredAtMs: Long = 0L,
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L
)

/**
 * Wrapper seguro de registro de errores de inicialización Firebase (onCreate + Logcat filtrado del propio PID).
 * - Nunca falla ni detiene el arranque si Firebase aún no está inicializado o no hay sesión.
 * - Encola errores en almacenamiento local acotado (máx. 15) y los sube a `/AppErrors` cuando hay FirebaseAuth + red.
 */
object AppErrorLogger {

    private const val TAG = "AppErrorLogger"
    private const val PREFS_NAME = "como_on_app_errors_queue"
    private const val KEY_QUEUE_JSON = "pending_errors_json"
    private const val COLLECTION_APP_ERRORS = "AppErrors"
    private const val MAX_QUEUE_SIZE = 15
    private const val DEDUP_WINDOW_MS = 10L * 60L * 1000L
    private const val MAX_LOGCAT_LINES = 25
    private const val MAX_LOGCAT_CHARS = 2500

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val FIREBASE_TAG_REGEX = Regex(
        pattern = "(FirebaseApp|FirebaseInit|FirebaseAuth|FirebaseFirestore|Firestore|FirebaseMessaging|FirebaseInstallations|AppCheck|FCM|GooglePlayServices|DynamiteModule|GoogleApiAvailability|com\\.google\\.firebase)",
        option = RegexOption.IGNORE_CASE
    )
    private val ERROR_SIGNAL_REGEX = Regex(
        pattern = "(\\s[EFW]/|\\b[EFW]/|\\b(FATAL|Exception|Error|FAILED|PERMISSION_DENIED|UNAVAILABLE|Missing|NullPointer|IllegalState|SecurityException|Timeout)\\b)",
        option = RegexOption.IGNORE_CASE
    )
    private val SEVERE_LOGCAT_REGEX = Regex(
        pattern = "(\\s[EF]/|\\b[EF]/|\\b(FATAL|Exception|PERMISSION_DENIED|UNAVAILABLE|FirebaseApp is not initialized|NullPointerException|IllegalStateException)\\b)",
        option = RegexOption.IGNORE_CASE
    )
    private val BEARER_TOKEN_REGEX = Regex("Bearer\\s+[A-Za-z0-9\\-._~+/]+=*", RegexOption.IGNORE_CASE)

    fun extractFirebaseLogcatLines(rawLogcat: String): String {
        if (rawLogcat.isBlank()) return ""
        val filtered = rawLogcat
            .lineSequence()
            .map { it.trimEnd() }
            .filter { line ->
                line.isNotBlank() &&
                    FIREBASE_TAG_REGEX.containsMatchIn(line) &&
                    ERROR_SIGNAL_REGEX.containsMatchIn(line)
            }
            .map { line ->
                BEARER_TOKEN_REGEX.replace(line, "Bearer [REDACTED]").take(260)
            }
            .toList()
            .takeLast(MAX_LOGCAT_LINES)

        if (filtered.isEmpty()) return ""
        return filtered.joinToString("\n").take(MAX_LOGCAT_CHARS)
    }

    fun hasCriticalFirebaseSignal(filteredLogcat: String): Boolean {
        if (filteredLogcat.isBlank()) return false
        return SEVERE_LOGCAT_REGEX.containsMatchIn(filteredLogcat)
    }

    fun buildDeviceLabel(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val model = Build.MODEL.orEmpty().trim()
        return listOf(manufacturer, model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Dispositivo Android" }
            .take(100)
    }

    fun recordOnCreateError(
        context: Context,
        stageLabel: String,
        throwable: Throwable
    ) {
        runCatching {
            val rawMessage = "${stageLabel.trim()}: ${throwable.message ?: throwable::class.java.simpleName}"
            val stackExcerpt = throwable.stackTraceToString().take(2400)
            val logcatTail = captureOwnProcessLogcatRaw().let { extractFirebaseLogcatLines(it) }
            enqueuePendingError(
                context = context,
                stage = AppErrorStage.ON_CREATE,
                errorMessage = rawMessage,
                stackTraceExcerpt = stackExcerpt,
                logcatExcerpt = logcatTail
            )
        }.onFailure { err ->
            Log.w(TAG, "recordOnCreateError ignored: ${err.message}")
        }
    }

    fun captureOwnProcessFirebaseLogcatAsync(context: Context) {
        val appContext = context.applicationContext
        ioScope.launch {
            runCatching {
                val raw = captureOwnProcessLogcatRaw()
                val filtered = extractFirebaseLogcatLines(raw)
                if (hasCriticalFirebaseSignal(filtered)) {
                    val headline = filtered.lineSequence().lastOrNull().orEmpty()
                        .ifBlank { "Error Firebase detectado en Logcat" }
                    enqueuePendingError(
                        context = appContext,
                        stage = AppErrorStage.LOGCAT_FIREBASE,
                        errorMessage = headline.take(580),
                        stackTraceExcerpt = "",
                        logcatExcerpt = filtered
                    )
                }
            }.onFailure { err ->
                Log.w(TAG, "captureOwnProcessFirebaseLogcatAsync skipped: ${err.message}")
            }
        }
    }

    private fun captureOwnProcessLogcatRaw(): String {
        return runCatching {
            val pid = Process.myPid()
            val process = ProcessBuilder(
                "logcat",
                "-d",
                "-v",
                "threadtime",
                "-t",
                "250",
                "--pid=$pid"
            )
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readLines().takeLast(250).joinToString("\n")
            }
            process.destroy()
            output
        }.getOrDefault("")
    }

    @Synchronized
    fun enqueuePendingError(
        context: Context,
        stage: AppErrorStage,
        errorMessage: String,
        stackTraceExcerpt: String = "",
        logcatExcerpt: String = "",
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val cleanMsg = errorMessage.trim().ifBlank { "Error Firebase en ${stage.name}" }.take(600)
        val currentQueue = readQueue(context).toMutableList()

        // Deduplicate similar errors within DEDUP_WINDOW_MS
        val msgSignature = "${stage.name}|${cleanMsg.take(120).lowercase()}"
        val isDuplicate = currentQueue.any { existing ->
            val existingSig = "${existing.stage}|${existing.errorMessage.take(120).lowercase()}"
            existingSig == msgSignature && (nowMs - existing.occurredAtMs) in 0L..DEDUP_WINDOW_MS
        }
        if (isDuplicate) return false

        val safeId = "err_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val safeVersionCode = BuildConfig.VERSION_CODE.coerceIn(1, 100000)
        val safeVersionName = BuildConfig.VERSION_NAME.ifBlank { safeVersionCode.toString() }.take(40)
        val safeSdk = Build.VERSION.SDK_INT.coerceIn(21, 60)

        val entry = PendingAppError(
            id = safeId,
            stage = stage.name,
            errorMessage = cleanMsg,
            stackTraceExcerpt = stackTraceExcerpt.take(2500),
            logcatExcerpt = logcatExcerpt.take(3000),
            deviceModel = buildDeviceLabel(),
            androidSdk = safeSdk,
            appVersionName = safeVersionName,
            appVersionCode = safeVersionCode,
            occurredAtMs = nowMs.coerceAtLeast(0L)
        )

        currentQueue.add(entry)
        while (currentQueue.size > MAX_QUEUE_SIZE) {
            currentQueue.removeAt(0)
        }
        writeQueue(context, currentQueue)
        return true
    }

    @Synchronized
    fun getPendingErrors(context: Context): List<PendingAppError> {
        return readQueue(context)
    }

    @Synchronized
    fun getPendingCount(context: Context): Int {
        return readQueue(context).size
    }

    @Synchronized
    fun removePendingByIds(context: Context, uploadedIds: Set<String>) {
        if (uploadedIds.isEmpty()) return
        val remaining = readQueue(context).filterNot { it.id in uploadedIds }
        writeQueue(context, remaining)
    }

    @Synchronized
    fun clearQueue(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_QUEUE_JSON)
            .apply()
    }

    suspend fun flushPendingErrors(
        context: Context,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        reporterNameOverride: String = ""
    ): Int {
        val fbUser = runCatching { auth.currentUser }.getOrNull() ?: return 0
        val uid = fbUser.uid.trim()
        if (uid.isBlank()) return 0

        val pending = getPendingErrors(context)
        if (pending.isEmpty()) return 0

        val reporterEmail = fbUser.email.orEmpty().trim().lowercase().take(120)
        val reporterName = reporterNameOverride.trim()
            .ifBlank { fbUser.displayName.orEmpty().trim() }
            .ifBlank { reporterEmail.substringBefore("@") }
            .ifBlank { "Vendedor PSA" }
            .take(80)

        val uploadedIds = mutableSetOf<String>()
        for (item in pending) {
            try {
                val payload = hashMapOf<String, Any>(
                    "id" to item.id.take(80),
                    "ownerUid" to uid.take(128),
                    "reporterEmail" to reporterEmail,
                    "reporterName" to reporterName,
                    "stage" to if (item.stage == AppErrorStage.LOGCAT_FIREBASE.name) "LOGCAT_FIREBASE" else "ON_CREATE",
                    "errorMessage" to item.errorMessage.ifBlank { "Error Firebase" }.take(600),
                    "stackTraceExcerpt" to item.stackTraceExcerpt.take(2500),
                    "logcatExcerpt" to item.logcatExcerpt.take(3000),
                    "deviceModel" to item.deviceModel.take(100),
                    "androidSdk" to item.androidSdk.coerceIn(21, 60),
                    "appVersionName" to item.appVersionName.ifBlank { "1.0" }.take(40),
                    "appVersionCode" to item.appVersionCode.coerceIn(1, 100000),
                    "status" to "OPEN",
                    "occurredAtMs" to item.occurredAtMs.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection(COLLECTION_APP_ERRORS)
                    .document(item.id)
                    .set(payload)
                    .await()
                uploadedIds.add(item.id)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: FirebaseFirestoreException) {
                Log.w(TAG, "Firestore AppErrors write failed [${e.code}]: ${e.message}")
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    e.code == FirebaseFirestoreException.Code.UNAVAILABLE
                ) {
                    break
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unexpected AppErrors upload error: ${e.message}")
            }
        }

        if (uploadedIds.isNotEmpty()) {
            removePendingByIds(context, uploadedIds)
        }
        return uploadedIds.size
    }

    private fun readQueue(context: Context): List<PendingAppError> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_QUEUE_JSON, null)
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<PendingAppError>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id").trim()
                if (id.isBlank()) continue
                list.add(
                    PendingAppError(
                        id = id,
                        stage = obj.optString("stage", AppErrorStage.ON_CREATE.name),
                        errorMessage = obj.optString("errorMessage"),
                        stackTraceExcerpt = obj.optString("stackTraceExcerpt"),
                        logcatExcerpt = obj.optString("logcatExcerpt"),
                        deviceModel = obj.optString("deviceModel"),
                        androidSdk = obj.optInt("androidSdk", Build.VERSION.SDK_INT),
                        appVersionName = obj.optString("appVersionName"),
                        appVersionCode = obj.optInt("appVersionCode", 1),
                        occurredAtMs = obj.optLong("occurredAtMs", 0L)
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun writeQueue(context: Context, items: List<PendingAppError>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("stage", item.stage)
                    put("errorMessage", item.errorMessage)
                    put("stackTraceExcerpt", item.stackTraceExcerpt)
                    put("logcatExcerpt", item.logcatExcerpt)
                    put("deviceModel", item.deviceModel)
                    put("androidSdk", item.androidSdk)
                    put("appVersionName", item.appVersionName)
                    put("appVersionCode", item.appVersionCode)
                    put("occurredAtMs", item.occurredAtMs)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_QUEUE_JSON, arr.toString())
            .apply()
    }
}
