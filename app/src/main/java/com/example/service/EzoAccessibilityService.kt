package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.animation.ValueAnimator
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.ScannedChat
import com.example.data.model.ScriptItem
import com.example.util.UnattendedAutomationLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class AntiSpamTimerState(
    val isWaiting: Boolean = false,
    val secondsRemaining: Int = 0,
    val targetDurationSeconds: Int = 0,
    val lastJitterDelaySeconds: Int = 0
)

data class SafeDeliveryResult(
    val isSuccess: Boolean,
    val delaySecondsUsed: Int,
    val errorMessage: String? = null
)

data class ScannedCrmFolder(
    val name: String,
    val badgeCount: Int = 0,
    val isSelected: Boolean = false
)

data class ScannedCrmChat(
    val contactId: String,
    val lastMessage: String,
    val dateText: String,
    val hasUnread24h: Boolean,
    val folderName: String = "",
    val nodeInfo: AccessibilityNodeInfo? = null
)

class EzoAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private var windowManager: WindowManager? = null
    private var floatingRootView: View? = null
    private var isExpanded = false

    // Floating UI elements
    private var bubbleContainer: FrameLayout? = null
    private var panelContainer: LinearLayout? = null
    private var tvBubbleStatus: TextView? = null
    private var tvScannedCount: TextView? = null
    private var tvSelectedChatsCount: TextView? = null
    private var tvMiniProgressBadge: TextView? = null
    private var btnScanScreen: Button? = null
    private var btnStartAutomation: Button? = null
    private var quickResponsesContainer: LinearLayout? = null
    private var cachedScriptsList: List<ScriptItem> = emptyList()

    // Battery Saving Dim Overlay
    private var dimOverlayView: View? = null
    private var dimOverlayIcon: TextView? = null
    private var dimOverlayTitle: TextView? = null
    private var dimOverlayStatusText: TextView? = null

    companion object {
        var instance: EzoAccessibilityService? = null
            private set

        private val _isBankingMode = MutableStateFlow(false)
        val isBankingMode = _isBankingMode.asStateFlow()

        private val _isAppPaused = MutableStateFlow(false)
        val isAppPaused = _isAppPaused.asStateFlow()

        private val _selectedChatsReadyCount = MutableStateFlow(0)
        val selectedChatsReadyCount = _selectedChatsReadyCount.asStateFlow()

        fun updateSelectedChatsCount(count: Int) {
            _selectedChatsReadyCount.value = count
            instance?.let { service ->
                service.mainHandler.post {
                    service.tvSelectedChatsCount?.text = if (count > 0) {
                        "🎯 $count chats listos para enviar"
                    } else {
                        "🎯 Sin chats elegidos pendientes"
                    }
                }
            }
        }

        fun enableBankingMode(fromAutoDetection: Boolean = false) {
            instance?.enableBankingModeInternal(fromAutoDetection)
        }

        fun disableBankingMode() {
            instance?.disableBankingModeInternal()
        }

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected = _isServiceConnected.asStateFlow()

        private val _isBubbleVisible = MutableStateFlow(false)
        val isBubbleVisible = _isBubbleVisible.asStateFlow()

        const val DEFAULT_MIN_DELAY_SECONDS = 30
        const val DEFAULT_MAX_DELAY_SECONDS = 45
        const val HARD_FLOOR_MIN_DELAY_SECONDS = 20

        private val _antiSpamTimerState = MutableStateFlow(AntiSpamTimerState())
        val antiSpamTimerState = _antiSpamTimerState.asStateFlow()

        private val _scannedFolders = MutableStateFlow<List<ScannedCrmFolder>>(emptyList())
        val scannedFolders = _scannedFolders.asStateFlow()

        private val _activeCrmFolder = MutableStateFlow("General")
        val activeCrmFolder = _activeCrmFolder.asStateFlow()

        private val _scannedChats = MutableStateFlow<List<ScannedCrmChat>>(emptyList())
        val scannedChats = _scannedChats.asStateFlow()

        private val _detectedCrmApp = MutableStateFlow("Ninguno")
        val detectedCrmApp = _detectedCrmApp.asStateFlow()

        private val _activeCrmPackage = MutableStateFlow("")
        val activeCrmPackage = _activeCrmPackage.asStateFlow()

        private val _isCrmWindowActive = MutableStateFlow(false)
        val isCrmWindowActive = _isCrmWindowActive.asStateFlow()

        private val _visibleChatsCount = MutableStateFlow(0)
        val visibleChatsCount = _visibleChatsCount.asStateFlow()

        private val _lastDetectedWindowTitle = MutableStateFlow("")
        val lastDetectedWindowTitle = _lastDetectedWindowTitle.asStateFlow()

        fun identifyCrmPackage(packageName: String): String? {
            val pkg = packageName.lowercase()
            if (pkg.isBlank() || pkg.contains("launcher") || pkg.contains("systemui") || pkg.contains("inputmethod") || pkg.contains("settings")) {
                return null
            }
            return when {
                pkg == "com.whatsapp.w4b" -> "WhatsApp Business"
                pkg == "com.whatsapp" -> "WhatsApp"
                pkg.contains("telegram") -> "Telegram"
                pkg.contains("apps.messaging") -> "Google Mensajes (RCS/SMS)"
                pkg.contains("ezocrm") -> "EzoCRM"
                pkg.contains("hubspot") -> "HubSpot CRM"
                pkg.contains("zoho") -> "Zoho CRM"
                pkg.contains("kommo") || pkg.contains("amocrm") -> "Kommo CRM"
                pkg.contains("salesforce") -> "Salesforce"
                pkg.contains("bitrix24") -> "Bitrix24"
                pkg.contains("clientify") -> "Clientify CRM"
                pkg.contains("sirena") -> "Sirena CRM"
                pkg.contains("trengo") -> "Trengo"
                pkg.contains("freshchat") || pkg.contains("freshdesk") -> "Freshdesk / Freshchat"
                pkg.contains("zendesk") -> "Zendesk"
                pkg.contains("lead2team") -> "Lead2Team"
                pkg.contains("manychat") -> "ManyChat"
                pkg.contains("facebook.orca") -> "Messenger"
                pkg.contains("pipefy") -> "Pipefy"
                pkg.contains("monday") -> "Monday CRM"
                pkg.contains("intercom") -> "Intercom"
                pkg.contains("crisp") -> "Crisp"
                pkg.contains("tawk") -> "Tawk.to"
                pkg.contains("chrome") || pkg.contains("firefox") || pkg.contains("browser") -> "CRM Web (Navegador)"
                else -> {
                    val segments = pkg.split('.').filter { it !in listOf("com", "org", "net", "app", "android", "mobile") }
                    val name = segments.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Externo"
                    "CRM ($name)"
                }
            }
        }

        private val _processedRecipientsInSession = java.util.Collections.synchronizedSet(mutableSetOf<String>())

        fun markRecipientProcessed(identifier: String) {
            val clean = identifier.trim().lowercase()
            if (clean.isNotBlank()) {
                _processedRecipientsInSession.add(clean)
                val digits = clean.filter { it.isDigit() }
                if (digits.length >= 6) {
                    _processedRecipientsInSession.add(digits)
                }
            }
        }

        fun isRecipientProcessed(identifier: String): Boolean {
            val clean = identifier.trim().lowercase()
            if (clean.isBlank()) return false
            if (_processedRecipientsInSession.contains(clean)) return true
            val digits = clean.filter { it.isDigit() }
            return digits.length >= 6 && _processedRecipientsInSession.contains(digits)
        }

        fun clearProcessedRecipientsSession() {
            _processedRecipientsInSession.clear()
        }

        fun unmarkRecipientProcessed(identifier: String) {
            val clean = identifier.trim().lowercase()
            if (clean.isNotBlank()) {
                _processedRecipientsInSession.remove(clean)
                val digits = clean.filter { it.isDigit() }
                if (digits.length >= 6) {
                    _processedRecipientsInSession.remove(digits)
                }
            }
        }

        var onStartAutomationRequested: (() -> Unit)? = null
        var onPauseAutomationRequested: (() -> Unit)? = null
        var onStopAutomationRequested: (() -> Unit)? = null

        fun isAccessibilityEnabledInSettings(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            val myComponentName = ComponentName(context, EzoAccessibilityService::class.java).flattenToString()
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(myComponentName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        fun toggleFloatingBubble(context: Context, show: Boolean) {
            val service = instance
            if (service != null) {
                if (show) service.showFloatingOverlay() else service.hideFloatingOverlay()
            } else {
                Toast.makeText(
                    context,
                    "Primero activa el servicio de accesibilidad de Como On en los Ajustes del sistema.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        fun setAppPaused(context: Context, paused: Boolean) {
            _isAppPaused.value = paused
            if (paused) {
                try {
                    onPauseAutomationRequested?.invoke()
                } catch (_: Exception) {}
                instance?.hideFloatingOverlay()
                instance?.hideDimOverlay()
                instance?.hideLiveCloserOverlay()
                instance?.updateFloatingStatus("Pausado")
            } else {
                _isBankingMode.value = false
                instance?.let { service ->
                    service.showFloatingOverlay()
                    service.updateFloatingStatus("Listo")
                }
            }
        }

        /**
         * Disables the accessibility service completely using Android's disableSelf() API.
         * This removes the service from Android's active accessibility list, allowing
         * banking apps like Banco Nación (BNA+) to open without detecting remote control.
         */
        fun disableServiceCompletely(context: Context): Boolean {
            return executeKillSwitch(context)
        }

        /**
         * Kill Switch: Stops all accessibility services, hides all overlays,
         * cancels all application notifications, and detaches the service completely.
         * Guarantees 100% safe operation in banking applications (Banco Nación, Santander, etc.).
         */
        fun executeKillSwitch(context: Context): Boolean {
            try {
                onStopAutomationRequested?.invoke()
            } catch (_: Exception) {}

            val service = instance
            if (service != null) {
                service.hideFloatingOverlay()
                service.hideDimOverlay()
                service.hideLiveCloserOverlay()
            }

            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancelAll()
                androidx.core.app.NotificationManagerCompat.from(context).cancelAll()
            } catch (_: Exception) {}

            _isBankingMode.value = true
            _isAppPaused.value = true
            _isServiceConnected.value = false
            _isBubbleVisible.value = false

            if (service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                service.disableSelf()
                Toast.makeText(
                    context,
                    "🛡️ Kill Switch activado: accesibilidad y notificaciones apagadas para bancos.",
                    Toast.LENGTH_LONG
                ).show()
                return true
            }

            openAccessibilitySettings(context)
            return false
        }

        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Abre Ajustes > Accesibilidad en tu teléfono", Toast.LENGTH_LONG).show()
            }
        }
    }

    private var lastQuickCheckTimeMs = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        info.notificationTimeout = 120
        serviceInfo = info

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Keep scripts updated for Quick Responses in floating bubble
        serviceScope.launch {
            try {
                AppDatabase.getDatabase(applicationContext).scriptDao().getAllScripts().collect { scripts ->
                    cachedScriptsList = scripts
                    withContext(Dispatchers.Main) {
                        populateQuickResponses(scripts)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isBankingApp(pkg: String): Boolean {
        val p = pkg.lowercase()
        return p.contains("bank") || p.contains("banco") || p.contains("mercadopago") ||
                p.contains("santander") || p.contains("galicia") || p.contains("bbva") ||
                p.contains("brubank") || p.contains("uala") || p.contains("cuentadni") ||
                p.contains("nacion") || p.contains("macro") || p.contains("patagonia") ||
                p.contains("icbc") || p.contains("supervielle") || p.contains("itau") ||
                p.contains("hipotecario")
    }

    fun enableBankingModeInternal(fromAutoDetection: Boolean = false) {
        _isBankingMode.value = true
        mainHandler.post {
            hideFloatingOverlay()
            hideDimOverlay()
            val helper = NotificationHelper(applicationContext)
            helper.showBankingModeNotification()
            val msg = if (fromAutoDetection) {
                "App bancaria detectada: Como On ocultado para proteger tu sesión"
            } else {
                "Modo Banco activo: superposiciones retiradas para operar seguro"
            }
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun disableBankingModeInternal() {
        _isBankingMode.value = false
        mainHandler.post {
            val helper = NotificationHelper(applicationContext)
            helper.dismissBankingModeNotification()
            showFloatingOverlay()
            Toast.makeText(applicationContext, "Burbuja de Como On restaurada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        // Ignore events while in Banking Mode
        if (_isBankingMode.value) return

        // Auto-protection: If user opened a banking app without clicking button, hide overlay immediately
        if (isBankingApp(pkg)) {
            enableBankingModeInternal(fromAutoDetection = true)
            return
        }

        // Ignore events from Como On so we don't treat our own app as a CRM
        if (pkg == applicationContext.packageName) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val crmName = identifyCrmPackage(pkg)
                if (crmName != null) {
                    _detectedCrmApp.value = crmName
                    _activeCrmPackage.value = pkg
                    _isCrmWindowActive.value = true

                    val eventTexts = event.text.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
                    if (eventTexts.isNotEmpty()) {
                        _lastDetectedWindowTitle.value = eventTexts.joinToString(" • ")
                    }

                    // Update floating bubble status text with the detected CRM
                    updateFloatingStatus("🟢 CRM: $crmName", null)

                    // Throttle quick chat count check (at most once every 1200ms)
                    val now = System.currentTimeMillis()
                    if (now - lastQuickCheckTimeMs > 1200) {
                        lastQuickCheckTimeMs = now
                        serviceScope.launch(Dispatchers.Default) {
                            val count = countVisibleChatsQuickly()
                            withContext(Dispatchers.Main) {
                                _visibleChatsCount.value = count
                                if (count > 0) {
                                    tvScannedCount?.text = "$count chats en pantalla"
                                }
                            }
                        }
                    }
                } else {
                    // If user moved away to system UI, home launcher, etc.
                    if (_isCrmWindowActive.value && !pkg.contains("systemui") && !pkg.contains("inputmethod")) {
                        _isCrmWindowActive.value = false
                        _detectedCrmApp.value = "Ninguno"
                        updateFloatingStatus("Listo para interactuar sobre la pantalla actual", null)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        // Interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        hideFloatingOverlay()
        hideDimOverlay()
        if (instance == this) {
            instance = null
            _isServiceConnected.value = false
        }
    }

    // ==========================================
    // FLOATING OVERLAY BUBBLE & CONTROLS
    // ==========================================

    fun showFloatingOverlay() {
        if (floatingRootView != null) return
        val wm = windowManager ?: return

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 350
        }

        val rootView = buildFloatingViewHierarchy(wm, layoutParams)
        floatingRootView = rootView

        try {
            wm.addView(rootView, layoutParams)
            _isBubbleVisible.value = true
            Toast.makeText(this, "Burbuja de control Como On activada", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al mostrar la burbuja: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun hideFloatingOverlay() {
        hideLiveCloserOverlay()
        val view = floatingRootView ?: return
        val wm = windowManager ?: return
        try {
            wm.removeView(view)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        floatingRootView = null
        _isBubbleVisible.value = false
    }

    fun isFloatingOverlayVisible(): Boolean = floatingRootView != null

    private fun buildFloatingViewHierarchy(
        wm: WindowManager,
        params: WindowManager.LayoutParams
    ): View {
        val root = FrameLayout(this)

        // Collapsed Bubble View with Live Mini Progress Badge
        val bubble = FrameLayout(this).apply {
            elevation = dpToPx(8).toFloat()

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                // Icon circle
                val iconBox = FrameLayout(context).apply {
                    val bg = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#0F172A")) // Slate 900
                        setStroke(dpToPx(2), Color.parseColor("#6366F1")) // Indigo
                    }
                    background = bg

                    val ivIcon = ImageView(context).apply {
                        setImageResource(R.drawable.ic_launcher_como_on)
                        setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
                    }
                    addView(ivIcon, FrameLayout.LayoutParams(dpToPx(54), dpToPx(54), Gravity.CENTER))
                }
                addView(iconBox, LinearLayout.LayoutParams(dpToPx(54), dpToPx(54)))

                // Mini live progress badge in EzoCRM
                val badge = TextView(context).apply {
                    val badgeBg = GradientDrawable().apply {
                        cornerRadius = dpToPx(14).toFloat()
                        setColor(Color.parseColor("#0F172A"))
                        setStroke(dpToPx(1), Color.parseColor("#10B981"))
                    }
                    background = badgeBg
                    setTextColor(Color.WHITE)
                    textSize = 11f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5))
                    visibility = View.GONE
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginStart = dpToPx(6)
                    }
                    layoutParams = lp
                }
                tvMiniProgressBadge = badge
                addView(badge)
            }
            addView(row, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        }
        bubbleContainer = bubble

        // Expanded Control Panel View
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16).toFloat()
                setColor(Color.parseColor("#0F172A"))
                setStroke(dpToPx(1), Color.parseColor("#334155"))
            }
            background = bg
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
            elevation = dpToPx(12).toFloat()
            visibility = View.GONE
        }
        panelContainer = panel

        // Panel Header
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvTitle = TextView(this).apply {
            text = "Como On CRM"
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnMinimize = Button(this).apply {
            text = "—"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            setOnClickListener {
                collapsePanel(params, wm)
            }
        }

        val btnClose = Button(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 14f
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            setOnClickListener {
                hideFloatingOverlay()
            }
        }

        headerRow.addView(tvTitle)
        headerRow.addView(btnMinimize)
        headerRow.addView(btnClose)
        panel.addView(headerRow)

        // Status text
        val tvStatus = TextView(this).apply {
            text = "Listo para interactuar sobre la pantalla actual"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 11f
            setPadding(0, dpToPx(4), 0, dpToPx(6))
        }
        tvBubbleStatus = tvStatus
        panel.addView(tvStatus)

        // Scanned count indicator
        val tvCount = TextView(this).apply {
            text = "0 chats detectados"
            setTextColor(Color.parseColor("#10B981"))
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dpToPx(4))
        }
        tvScannedCount = tvCount
        panel.addView(tvCount)

        // Selected chats ready count indicator badge
        val tvSelectedBadge = TextView(this).apply {
            val count = _selectedChatsReadyCount.value
            text = if (count > 0) "🎯 $count chats listos para enviar" else "🎯 Sin chats elegidos pendientes"
            setTextColor(Color.parseColor("#38BDF8")) // Sky blue neon
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dpToPx(8))
        }
        tvSelectedChatsCount = tvSelectedBadge
        panel.addView(tvSelectedBadge)

        // Button: Escanear Pantalla
        val scanBtn = Button(this).apply {
            text = "🔍 Escanear Pantalla CRM"
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            val btnBg = GradientDrawable().apply {
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#6366F1")) // Indigo
            }
            background = btnBg
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            setOnClickListener {
                tvBubbleStatus?.text = "Escaneando chats y números..."
                serviceScope.launch {
                    val count = scanScreenAndPersist(autoScroll = true)
                    withContext(Dispatchers.Main) {
                        tvBubbleStatus?.text = "¡Escaneo completado!"
                        tvScannedCount?.text = "$count chats guardados"
                        Toast.makeText(
                            this@EzoAccessibilityService,
                            "Se guardaron $count chats para automatizar",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        btnScanScreen = scanBtn
        panel.addView(scanBtn)

        val spacer1 = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dpToPx(8)) }
        panel.addView(spacer1)

        // Button: Iniciar Automatización
        val autoBtn = Button(this).apply {
            text = "▶ Iniciar Envío de Mensajes"
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            val btnBg = GradientDrawable().apply {
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#10B981")) // Emerald
            }
            background = btnBg
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            setOnClickListener {
                onStartAutomationRequested?.invoke()
                tvBubbleStatus?.text = "Iniciando cadencia de mensajes..."
            }
        }
        btnStartAutomation = autoBtn
        panel.addView(autoBtn)

        val spacer2 = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dpToPx(8)) }
        panel.addView(spacer2)

        // Action Row (Pause & Minimize)
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val btnPause = Button(this).apply {
            text = "⏸ Pausar"
            setTextColor(Color.parseColor("#F59E0B"))
            textSize = 11f
            val btnBg = GradientDrawable().apply {
                cornerRadius = dpToPx(6).toFloat()
                setColor(Color.parseColor("#1E293B"))
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                onPauseAutomationRequested?.invoke()
                tvBubbleStatus?.text = "Automatización pausada"
            }
        }

        val spacer3 = View(this).apply { layoutParams = LinearLayout.LayoutParams(dpToPx(6), 1) }

        val btnCollapse = Button(this).apply {
            text = "Minimizar"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 11f
            val btnBg = GradientDrawable().apply {
                cornerRadius = dpToPx(6).toFloat()
                setColor(Color.parseColor("#1E293B"))
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                collapsePanel(params, wm)
            }
        }

        actionRow.addView(btnPause)
        actionRow.addView(spacer3)
        actionRow.addView(btnCollapse)
        panel.addView(actionRow)

        val spacerBank = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dpToPx(8)) }
        panel.addView(spacerBank)

        // Button: Modo Banco (Ocultar Todo)
        val btnBankMode = Button(this).apply {
            text = "🏦 Modo Banco (Ocultar Todo)"
            setTextColor(Color.parseColor("#FCA5A5"))
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            val btnBg = GradientDrawable().apply {
                cornerRadius = dpToPx(6).toFloat()
                setColor(Color.parseColor("#450A0A")) // Deep dark red
                setStroke(dpToPx(1), Color.parseColor("#EF4444"))
            }
            background = btnBg
            setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
            setOnClickListener {
                enableBankingModeInternal(fromAutoDetection = false)
            }
        }
        panel.addView(btnBankMode)

        val spacerCloser = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dpToPx(8)) }
        panel.addView(spacerCloser)

        // Button: 🔮 Closer IA en Vivo (WhatsApp • Instagram • Facebook)
        val btnLiveCloser = Button(this).apply {
            text = "🔮 Closer IA en Vivo (Sugerir Respuestas)"
            setTextColor(Color.WHITE)
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            val btnBg = GradientDrawable().apply {
                cornerRadius = dpToPx(6).toFloat()
                setColor(Color.parseColor("#7C3AED")) // Vibrant purple closer
                setStroke(dpToPx(1), Color.parseColor("#C4B5FD"))
            }
            background = btnBg
            setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
            setOnClickListener {
                showLiveCloserOverlay()
            }
        }
        panel.addView(btnLiveCloser)

        val spacerQuick = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dpToPx(8)) }
        panel.addView(spacerQuick)

        val quickHeader = TextView(this).apply {
            text = "💬 Respuestas Rápidas (1 toque)"
            setTextColor(Color.parseColor("#C084FC")) // Light purple neon
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dpToPx(4))
        }
        panel.addView(quickHeader)

        val scrollQuick = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val quickContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        quickResponsesContainer = quickContainer
        scrollQuick.addView(quickContainer)
        panel.addView(scrollQuick)

        populateQuickResponses(cachedScriptsList)

        root.addView(bubble, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        root.addView(panel, FrameLayout.LayoutParams(dpToPx(270), FrameLayout.LayoutParams.WRAP_CONTENT))

        // Touch listener for smooth dragging, snap-to-edge, drag-down-to-close and tapping
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var hasMoved = false
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val dismissThresholdY = (screenHeight * 0.72).toInt()
        val bubbleSize = dpToPx(56)

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    hasMoved = false
                    bubble.alpha = 0.85f
                    bubble.scaleX = 0.92f
                    bubble.scaleY = 0.92f
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                        hasMoved = true
                        params.x = initialX + dx
                        params.y = (initialY + dy).coerceIn(dpToPx(50), screenHeight - dpToPx(110))

                        // Visual feedback when dragged down towards bottom zone
                        if (event.rawY > dismissThresholdY) {
                            (bubble.background as? GradientDrawable)?.setStroke(dpToPx(3), Color.parseColor("#EF4444"))
                        } else {
                            (bubble.background as? GradientDrawable)?.setStroke(dpToPx(2), Color.parseColor("#6366F1"))
                        }

                        try {
                            wm.updateViewLayout(root, params)
                        } catch (e: Exception) {
                            // Ignore update exceptions
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    bubble.alpha = 1f
                    bubble.scaleX = 1f
                    bubble.scaleY = 1f
                    (bubble.background as? GradientDrawable)?.setStroke(dpToPx(2), Color.parseColor("#6366F1"))

                    if (hasMoved && event.rawY > dismissThresholdY) {
                        // User dragged the bubble down into the dismiss zone!
                        hideFloatingOverlay()
                        Toast.makeText(this@EzoAccessibilityService, "Burbuja cerrada", Toast.LENGTH_SHORT).show()
                    } else if (hasMoved) {
                        // Smooth snap to closest screen edge (left or right)
                        val centerX = params.x + (bubbleSize / 2)
                        val targetX = if (centerX < screenWidth / 2) {
                            dpToPx(8) // Left edge
                        } else {
                            screenWidth - bubbleSize - dpToPx(8) // Right edge
                        }

                        val startX = params.x
                        val animator = ValueAnimator.ofInt(startX, targetX).apply {
                            duration = 180
                            addUpdateListener { va ->
                                params.x = va.animatedValue as Int
                                try {
                                    wm.updateViewLayout(root, params)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }
                        }
                        animator.start()
                    } else {
                        // Single tap -> expand panel
                        expandPanel(params, wm)
                    }
                    true
                }
                else -> false
            }
        }

        return root
    }

    private fun expandPanel(params: WindowManager.LayoutParams, wm: WindowManager) {
        isExpanded = true
        bubbleContainer?.visibility = View.GONE
        panelContainer?.visibility = View.VISIBLE
        val screenWidth = resources.displayMetrics.widthPixels
        val panelWidth = dpToPx(270)
        if (params.x + panelWidth > screenWidth) {
            params.x = (screenWidth - panelWidth - dpToPx(10)).coerceAtLeast(dpToPx(10))
        }
        params.width = panelWidth
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        try {
            wm.updateViewLayout(floatingRootView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun collapsePanel(params: WindowManager.LayoutParams, wm: WindowManager) {
        isExpanded = false
        panelContainer?.visibility = View.GONE
        bubbleContainer?.visibility = View.VISIBLE
        val screenWidth = resources.displayMetrics.widthPixels
        val bubbleSize = dpToPx(56)
        params.x = if (params.x > screenWidth / 2) {
            screenWidth - bubbleSize - dpToPx(8)
        } else {
            dpToPx(8)
        }
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        try {
            wm.updateViewLayout(floatingRootView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateLiveProgress(
        sentCount: Int,
        totalCount: Int,
        nextDelaySeconds: Int,
        statusLabel: String? = null
    ) {
        mainHandler.post {
            val badge = tvMiniProgressBadge ?: return@post
            if (totalCount <= 0) {
                badge.visibility = View.GONE
                return@post
            }
            badge.visibility = View.VISIBLE
            val countdownPart = when {
                nextDelaySeconds > 0 -> "⏳ Siguiente: ${nextDelaySeconds}s"
                statusLabel != null -> statusLabel
                else -> "🚀 Enviando..."
            }
            badge.text = "📤 $sentCount / $totalCount  •  $countdownPart"
        }
    }

    fun hideLiveProgressBadge() {
        mainHandler.post {
            tvMiniProgressBadge?.visibility = View.GONE
        }
    }

    fun playSafetyAlertSound() {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(applicationContext, alertUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun vibrateSafetyAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 300, 150, 300), -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun vibrateCompletion() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 240), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 240), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 180, 120, 240), -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateFloatingStatus(text: String, countText: String? = null) {
        mainHandler.post {
            tvBubbleStatus?.text = text
            if (countText != null) {
                tvScannedCount?.text = countText
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun populateQuickResponses(scripts: List<ScriptItem>) {
        val container = quickResponsesContainer ?: return
        container.removeAllViews()

        if (scripts.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Sin scripts aún"
                setTextColor(Color.parseColor("#64748B"))
                textSize = 10f
                setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            }
            container.addView(emptyTv)
            return
        }

        val displayScripts = scripts.take(12)
        for (script in displayScripts) {
            val chip = Button(this).apply {
                text = script.title.take(18)
                textSize = 10f
                isAllCaps = false
                val bg = GradientDrawable().apply {
                    cornerRadius = dpToPx(14).toFloat()
                    setColor(Color.parseColor("#2E1065")) // Deep purple low battery
                    setStroke(dpToPx(1), Color.parseColor("#9333EA"))
                }
                background = bg
                setTextColor(Color.WHITE)
                setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dpToPx(28)
                ).apply {
                    setMargins(0, 0, dpToPx(6), 0)
                }
                setOnClickListener {
                    injectQuickResponse(script.title, script.content)
                }
            }
            container.addView(chip)
        }
    }

    fun injectQuickResponse(title: String, content: String) {
        val processed = com.example.util.TextTemplateParser.process(content)

        // 1. Copy to clipboard as reliable backup
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Como On Quick Response", processed)
            clipboard?.setPrimaryClip(clip)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Direct injection into active EditText in WhatsApp / CRM
        var injected = false
        try {
            val root = rootInActiveWindow
            if (root != null) {
                val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focusedNode != null && (focusedNode.isEditable || focusedNode.className?.toString()?.contains("EditText") == true)) {
                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, processed)
                    }
                    injected = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                }

                if (!injected) {
                    val editNodes = mutableListOf<AccessibilityNodeInfo>()
                    fun searchEditTexts(node: AccessibilityNodeInfo) {
                        if (node.isEditable || node.className?.toString()?.contains("EditText") == true) {
                            editNodes.add(node)
                        }
                        for (i in 0 until node.childCount) {
                            node.getChild(i)?.let { searchEditTexts(it) }
                        }
                    }
                    searchEditTexts(root)
                    val targetNode = editNodes.lastOrNull()
                    if (targetNode != null) {
                        val args = Bundle().apply {
                            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, processed)
                        }
                        injected = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Smoothly collapse floating panel
        floatingRootView?.let { root ->
            val wm = windowManager
            val params = root.layoutParams as? WindowManager.LayoutParams
            if (wm != null && params != null) {
                collapsePanel(params, wm)
            }
        }

        // 4. User feedback
        if (injected) {
            Toast.makeText(this, "¡$title insertado en el chat!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "¡$title copiado al portapapeles! Listo para pegar", Toast.LENGTH_SHORT).show()
        }
    }

    private var liveCloserOverlayView: View? = null

    fun extractVisibleConversationText(): String {
        val root = rootInActiveWindow ?: return ""
        val messages = mutableListOf<String>()
        val screenHeight = resources.displayMetrics.heightPixels

        fun collectTexts(node: AccessibilityNodeInfo) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            // Focus on chat message content area
            if (rect.top > screenHeight * 0.10 && rect.bottom < screenHeight * 0.90) {
                val text = node.text?.toString()?.trim()
                if (!text.isNullOrBlank() && text.length > 2) {
                    val lower = text.lowercase()
                    val isTimestamp = text.matches(Regex("""^\d{1,2}:\d{2}(\s?[apAP][mM])?$"""))
                    val isIgnored = lower == "en línea" || lower == "online" || lower == "escribiendo..." ||
                            lower == "hoy" || lower == "ayer" || lower.startsWith("leído") || lower == "mensaje"
                    if (!isTimestamp && !isIgnored && !messages.contains(text)) {
                        messages.add(text)
                    }
                }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { collectTexts(it) }
            }
        }

        collectTexts(root)
        return messages.takeLast(6).joinToString("\n")
    }

    fun showLiveCloserOverlay() {
        val wm = windowManager ?: return
        hideLiveCloserOverlay()

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val screenWidth = resources.displayMetrics.widthPixels
        val dialogWidth = (screenWidth * 0.92).toInt().coerceAtMost(dpToPx(380))

        val params = WindowManager.LayoutParams(
            dialogWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#0F172A"))
                cornerRadius = dpToPx(16).toFloat()
                setStroke(dpToPx(2), Color.parseColor("#8B5CF6"))
            }
            background = bg
            setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "🔮 Closer IA (WhatsApp • IG • FB)"
            setTextColor(Color.parseColor("#C4B5FD"))
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnClose = Button(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 14f
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            setOnClickListener { hideLiveCloserOverlay() }
        }
        header.addView(title)
        header.addView(btnClose)
        container.addView(header)

        val tvStatus = TextView(this).apply {
            text = "Leyendo conversación y preparando respuestas de alto cierre..."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 11f
            setPadding(0, dpToPx(4), 0, dpToPx(8))
        }
        container.addView(tvStatus)

        val optionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(320)
            )
            addView(optionsContainer)
        }
        container.addView(scrollView)

        try {
            wm.addView(container, params)
            liveCloserOverlayView = container
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al mostrar Closer IA: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        }

        val visibleContext = extractVisibleConversationText()
        val queryContext = visibleContext.ifBlank { "Hola, me gustaría más información y saber de qué se trata." }

        serviceScope.launch {
            val db = com.example.data.local.AppDatabase.getDatabase(this@EzoAccessibilityService)
            val repo = com.example.data.repository.EzoRepository(db, this@EzoAccessibilityService)
            val profile = repo.getBusinessProfile()
            val gemini = GeminiSalesService(customApiKeyProvider = { repo.getGeminiApiKey() })
            val result = gemini.generateSmartResponses(queryContext, profile)

            withContext(Dispatchers.Main) {
                if (liveCloserOverlayView == null) return@withContext
                tvStatus.text = "Técnicas de venta adaptadas a ${profile.businessName}:"
                optionsContainer.removeAllViews()

                val options = result.getOrNull().orEmpty()
                if (options.isEmpty()) {
                    val tvEmpty = TextView(this@EzoAccessibilityService).apply {
                        text = "No se pudieron obtener sugerencias. Revisa tu clave de Gemini."
                        setTextColor(Color.parseColor("#EF4444"))
                        textSize = 11f
                    }
                    optionsContainer.addView(tvEmpty)
                } else {
                    for (opt in options) {
                        val card = LinearLayout(this@EzoAccessibilityService).apply {
                            orientation = LinearLayout.VERTICAL
                            val cardBg = GradientDrawable().apply {
                                setColor(Color.parseColor("#1E293B"))
                                cornerRadius = dpToPx(10).toFloat()
                                setStroke(dpToPx(1), Color.parseColor("#334155"))
                            }
                            background = cardBg
                            setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 0, 0, dpToPx(8))
                            }
                        }

                        val optTitle = TextView(this@EzoAccessibilityService).apply {
                            text = "★ ${opt.title}"
                            setTextColor(Color.parseColor("#38BDF8"))
                            textSize = 11f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                        }
                        card.addView(optTitle)

                        val optText = TextView(this@EzoAccessibilityService).apply {
                            text = opt.text
                            setTextColor(Color.WHITE)
                            textSize = 12f
                            setPadding(0, dpToPx(4), 0, dpToPx(6))
                        }
                        card.addView(optText)

                        val actionRow = LinearLayout(this@EzoAccessibilityService).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.END
                        }

                        val btnCopy = Button(this@EzoAccessibilityService).apply {
                            text = "Copiar"
                            setTextColor(Color.parseColor("#94A3B8"))
                            textSize = 10f
                            val bBg = GradientDrawable().apply {
                                setColor(Color.parseColor("#0F172A"))
                                cornerRadius = dpToPx(6).toFloat()
                            }
                            background = bBg
                            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                            setOnClickListener {
                                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Closer IA", opt.text)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(this@EzoAccessibilityService, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            }
                        }

                        val spacerBtn = View(this@EzoAccessibilityService).apply {
                            layoutParams = LinearLayout.LayoutParams(dpToPx(6), 1)
                        }

                        val btnInject = Button(this@EzoAccessibilityService).apply {
                            text = "➔ Inyectar en Chat"
                            setTextColor(Color.WHITE)
                            textSize = 10f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                            val bBg = GradientDrawable().apply {
                                setColor(Color.parseColor("#6366F1"))
                                cornerRadius = dpToPx(6).toFloat()
                            }
                            background = bBg
                            setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
                            setOnClickListener {
                                injectQuickResponse(opt.title, opt.text)
                                hideLiveCloserOverlay()
                            }
                        }

                        actionRow.addView(btnCopy)
                        actionRow.addView(spacerBtn)
                        actionRow.addView(btnInject)
                        card.addView(actionRow)

                        optionsContainer.addView(card)
                    }
                }
            }
        }
    }

    fun hideLiveCloserOverlay() {
        liveCloserOverlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
            liveCloserOverlayView = null
        }
    }

    fun showDimOverlay(statusText: String) {
        mainHandler.post {
            if (dimOverlayView != null) {
                dimOverlayStatusText?.text = statusText
                return@post
            }
            val wm = windowManager ?: return@post
            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            val overlay = FrameLayout(this).apply {
                setBackgroundColor(Color.parseColor("#E6050811"))
                setOnClickListener {
                    hideDimOverlay()
                    Toast.makeText(context, "Atenuación pausada al tocar pantalla", Toast.LENGTH_SHORT).show()
                }
            }

            val centerBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24))
            }

            val iconView = TextView(this).apply {
                text = "🌙"
                textSize = 36f
                gravity = Gravity.CENTER
            }
            dimOverlayIcon = iconView

            val titleView = TextView(this).apply {
                text = "Modo Ahorro de Batería Activo"
                setTextColor(Color.parseColor("#E2E8F0"))
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(8), 0, dpToPx(4))
            }
            dimOverlayTitle = titleView

            val tvStatus = TextView(this).apply {
                text = statusText
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 12f
                gravity = Gravity.CENTER
            }
            dimOverlayStatusText = tvStatus

            val hintView = TextView(this).apply {
                text = "Toca la pantalla en cualquier momento para ver el chat"
                setTextColor(Color.parseColor("#64748B"))
                textSize = 11f
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(16), 0, 0)
            }

            centerBox.addView(iconView)
            centerBox.addView(titleView)
            centerBox.addView(tvStatus)
            centerBox.addView(hintView)

            overlay.addView(
                centerBox,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )

            try {
                wm.addView(overlay, params)
                dimOverlayView = overlay
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateDimOverlayText(statusText: String) {
        mainHandler.post {
            dimOverlayStatusText?.text = statusText
        }
    }

    fun hideDimOverlay() {
        mainHandler.post {
            val view = dimOverlayView ?: return@post
            val wm = windowManager ?: return@post
            try {
                wm.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            dimOverlayView = null
            dimOverlayIcon = null
            dimOverlayTitle = null
            dimOverlayStatusText = null
        }
    }

    // ==========================================
    // SCREEN SCANNER & PERSISTENCE
    // ==========================================

    suspend fun scanScreenAndPersist(autoScroll: Boolean = true): Int = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(applicationContext)
        val foundChatsMap = mutableMapOf<String, ScannedChat>()

        // 1. Detect folders on screen and determine active folder
        val rootNode = rootInActiveWindow
        val detectedFolders = if (rootNode != null) detectCrmFolders(rootNode) else emptyList()
        val currentFolder = detectedFolders.firstOrNull { it.isSelected }?.name
            ?: _activeCrmFolder.value
            ?: "General"
        val folderNamesSet = detectedFolders.map { it.name.lowercase() }.toSet()

        withContext(Dispatchers.Main) {
            updateFloatingStatus("Escaneando carpeta '$currentFolder'...", null)
        }

        // 2. Scan initial visible chats in current folder
        if (rootNode != null) {
            extractChatsFromNodeTree(rootNode, foundChatsMap, currentFolder, folderNamesSet)
        }

        // 3. Scroll to look at all chats inside the folder that was left on screen
        if (autoScroll) {
            var consecutiveNoNewChats = 0
            val maxScrollPasses = 8

            for (pass in 0 until maxScrollPasses) {
                val countBefore = foundChatsMap.size

                val scrolled = withContext(Dispatchers.Main) {
                    performReliableScrollForward()
                }

                // Wait for the CRM / WhatsApp list to scroll and render new rows
                delay(950)

                val activeRoot = rootInActiveWindow
                if (activeRoot != null) {
                    extractChatsFromNodeTree(activeRoot, foundChatsMap, currentFolder, folderNamesSet)
                }

                val newlyFound = foundChatsMap.size - countBefore
                withContext(Dispatchers.Main) {
                    updateFloatingStatus(
                        "Carpeta '$currentFolder': ${foundChatsMap.size} chats",
                        "${foundChatsMap.size} chats"
                    )
                }

                if (newlyFound == 0) {
                    consecutiveNoNewChats++
                    if (consecutiveNoNewChats >= 2) {
                        // Reached the end of the folder chat list
                        break
                    }
                } else {
                    consecutiveNoNewChats = 0
                }
            }
        }

        val chatList = foundChatsMap.values.toList()
        if (chatList.isNotEmpty()) {
            database.scannedChatDao().insertChats(chatList)
            val legacyChats = chatList.map {
                ScannedCrmChat(
                    contactId = it.phoneNumber.ifBlank { it.id },
                    lastMessage = it.lastMessage,
                    dateText = it.dateText,
                    hasUnread24h = false,
                    folderName = it.folderName
                )
            }
            _scannedChats.value = legacyChats
        }

        withContext(Dispatchers.Main) {
            updateFloatingStatus("¡Listo en '$currentFolder'!", "${chatList.size} chats")
        }

        return@withContext chatList.size
    }

    fun countVisibleChatsQuickly(): Int {
        val root = rootInActiveWindow ?: return 0
        val detectedFolders = detectCrmFolders(root)
        val activeFolder = _activeCrmFolder.value
        val folderNames = detectedFolders.map { it.name.lowercase() }.toSet()
        val temp = mutableMapOf<String, ScannedChat>()
        extractChatsFromNodeTree(root, temp, activeFolder, folderNames)
        return temp.size
    }

    fun detectCrmFolders(rootNode: AccessibilityNodeInfo): List<ScannedCrmFolder> {
        val folderList = mutableListOf<ScannedCrmFolder>()
        val seenNames = mutableSetOf<String>()
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels

        collectFoldersRecursive(rootNode, folderList, seenNames, screenHeight)

        if (folderList.isNotEmpty()) {
            _scannedFolders.value = folderList
            val selected = folderList.firstOrNull { it.isSelected }?.name
                ?: folderList.firstOrNull()?.name
                ?: "General"
            _activeCrmFolder.value = selected
        }

        return folderList
    }

    private fun collectFoldersRecursive(
        node: AccessibilityNodeInfo,
        results: MutableList<ScannedCrmFolder>,
        seen: MutableSet<String>,
        screenHeight: Int
    ) {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        // Focus on upper region for tabs / filter chips
        if (rect.top > screenHeight * 0.40) return

        val text = node.text?.toString()?.trim() ?: ""
        val desc = node.contentDescription?.toString()?.trim() ?: ""
        val candidate = if (text.isNotBlank()) text else desc
        val className = node.className?.toString()?.lowercase() ?: ""

        val isTabClass = className.contains("tab") || className.contains("chip") || className.contains("radio")
        val isLikelyFolderTab = (isTabClass || node.isClickable) && isRecognizedFolderLabel(candidate)

        if (isLikelyFolderTab && candidate.length in 2..30) {
            val cleanName = cleanFolderName(candidate)
            val lower = cleanName.lowercase()
            if (cleanName.isNotBlank() && !seen.contains(lower) && !isSystemActionLabel(cleanName)) {
                seen.add(lower)
                val isSelected = node.isSelected || node.isChecked || desc.contains("seleccionad", ignoreCase = true) || desc.contains("selected", ignoreCase = true)
                val badge = extractBadgeCount(candidate)
                results.add(
                    ScannedCrmFolder(
                        name = cleanName,
                        badgeCount = badge,
                        isSelected = isSelected
                    )
                )
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectFoldersRecursive(child, results, seen, screenHeight)
        }
    }

    private fun isRecognizedFolderLabel(text: String): Boolean {
        val t = cleanFolderName(text).lowercase()
        val standardFolders = listOf(
            "todos", "all", "no leídos", "no leidos", "unread", "favoritos", "favorites",
            "grupos", "groups", "comunidades", "canales", "channels", "archivados", "archived",
            "clientes", "leads", "ventas", "trabajo", "personal", "contactos", "seguimiento",
            "cotizaciones", "cerrados", "nuevos", "pendientes", "bandeja", "inbox", "importantes"
        )
        if (standardFolders.any { t == it }) return true
        if (isSystemActionLabel(t) || isNumericBadge(t)) return false
        return t.length in 2..20 && t.all { it.isLetter() || it.isWhitespace() }
    }

    private fun cleanFolderName(raw: String): String {
        return raw.replace(Regex("""\(\d+\)"""), "")
            .replace(Regex("""\d+$"""), "")
            .trim()
    }

    private fun extractBadgeCount(raw: String): Int {
        val match = Regex("""\(?(\d{1,3})\)?""").find(raw)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun isSystemActionLabel(text: String): Boolean {
        val t = text.trim().lowercase()
        val system = listOf(
            "buscar", "search", "atrás", "back", "volver", "más opciones", "more options",
            "cámara", "camera", "nuevo chat", "new chat", "chats", "estados", "llamadas",
            "ajustes", "settings", "editar", "edit", "filtro", "filtros"
        )
        return system.any { t == it }
    }

    private fun isFolderOrTab(text: String, detectedFolders: Set<String>): Boolean {
        val t = cleanFolderName(text).lowercase()
        if (t.isEmpty()) return false
        if (detectedFolders.contains(t)) return true
        if (isRecognizedFolderLabel(text)) return true
        val knownFolders = listOf(
            "todos", "all", "no leídos", "no leidos", "unread", "favoritos", "favorites",
            "grupos", "groups", "comunidades", "canales", "channels", "archivados", "archived",
            "clientes", "leads", "ventas", "trabajo", "personal", "contactos", "seguimiento",
            "cotizaciones", "cerrados", "nuevos", "pendientes", "bandeja", "inbox", "importantes"
        )
        return knownFolders.any { t == it }
    }

    private fun extractChatsFromNodeTree(
        node: AccessibilityNodeInfo,
        results: MutableMap<String, ScannedChat>,
        currentFolder: String,
        detectedFolderNames: Set<String>
    ) {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val screenHeight = resources.displayMetrics.heightPixels

        // NEVER scan the top header/tab/filter region as chats!
        if (rect.bottom < screenHeight * 0.18) {
            return
        }

        // 1. WhatsApp / WhatsApp Business specific view ID matching (highest accuracy)
        val waNameNode = findNodeByPartialId(node, "conversations_row_contact_name") ?: findNodeByPartialId(node, "contact_name")
        if (waNameNode != null) {
            val waDateNode = findNodeByPartialId(node, "conversations_row_date") ?: findNodeByPartialId(node, "date") ?: findNodeByPartialId(node, "timestamp")
            val waMsgNode = findNodeByPartialId(node, "single_msg_tv") ?: findNodeByPartialId(node, "last_msg") ?: findNodeByPartialId(node, "msg_preview")

            val rawName = waNameNode.text?.toString()?.trim() ?: ""
            if (rawName.isNotBlank() && !isBadLabel(rawName) && !isFolderOrTab(rawName, detectedFolderNames)) {
                val rawDate = waDateNode?.text?.toString()?.trim() ?: "Hoy"
                val rawSnippet = cleanSnippetText(waMsgNode?.text?.toString()?.trim() ?: "")

                val cleanPhone = rawName.filter { it.isDigit() || it == '+' }
                val idKey = if (cleanPhone.length >= 7) cleanPhone else rawName

                if (!results.containsKey(idKey)) {
                    results[idKey] = ScannedChat(
                        id = idKey,
                        name = rawName,
                        phoneNumber = if (cleanPhone.length >= 7) cleanPhone else idKey,
                        dateText = if (rawDate.isNotBlank()) rawDate else "Hoy",
                        lastMessage = rawSnippet,
                        isSelected = true,
                        isSent = false,
                        folderName = currentFolder
                    )
                }
            }
            return
        }

        // 2. Generic CRM / Messaging structural detection
        val nodeText = node.text?.toString()?.trim() ?: ""
        val nodeDesc = node.contentDescription?.toString()?.trim() ?: ""
        val candidate = if (nodeText.isNotBlank()) nodeText else nodeDesc

        val phoneMatch = Regex("""(\+?\d{1,4}[-.\s]?)?(\(?\d{2,4}\)?[-.\s]?)?\d{3,5}[-.\s]?\d{3,5}""").find(candidate)
        val isLikelyPhone = phoneMatch != null && phoneMatch.value.filter { it.isDigit() }.length >= 7

        val isFolder = isFolderOrTab(candidate, detectedFolderNames)

        if (!isFolder && (isLikelyPhone || (node.isClickable && candidate.length in 3..50 && !isBadLabel(candidate)))) {
            var container = node
            var depth = 0
            while (container.parent != null && depth < 3 && (!container.isClickable || container.childCount < 2)) {
                container = container.parent
                depth++
            }

            val texts = mutableListOf<String>()
            collectTextsRecursive(container, texts)

            if (texts.isNotEmpty()) {
                val hasExplicitDate = findDateInTexts(texts) != null
                val detectedDate = findDateInTexts(texts) ?: "Hoy"

                // 1. Extract pure phone/user digits (min 7 digits) from any text in the row
                val phoneDigits = texts.map { it.filter { c -> c.isDigit() } }
                    .firstOrNull { it.length >= 7 && !isLikelyTimestampOrDate(it) } ?: ""

                // 2. Look for real human contact name (pure text, no digits, excluding badges and folders)
                val detectedHumanName = texts.firstOrNull { t ->
                    t != detectedDate &&
                            !isLikelyTimestampOrDate(t) &&
                            !isBadLabel(t) &&
                            !isNumericBadge(t) &&
                            !isChannelOrStatusBadge(t) &&
                            !isFolderOrTab(t, detectedFolderNames) &&
                            t.filter { it.isDigit() }.length < 4 &&
                            t.length in 3..45
                }

                // If no real human name exists, use the clean digits (never prefix letters like "WA" or "EC")
                val finalCleanName = when {
                    !detectedHumanName.isNullOrBlank() -> detectedHumanName
                    phoneDigits.isNotBlank() -> phoneDigits
                    else -> texts.firstOrNull { !isLikelyTimestampOrDate(it) && !isNumericBadge(it) && !isChannelOrStatusBadge(it) }?.trim() ?: ""
                }

                val finalPhone = if (phoneDigits.isNotBlank()) phoneDigits else finalCleanName.filter { it.isDigit() }

                // Find last message snippet
                val detectedSnippet = texts.firstOrNull { t ->
                    t != detectedDate &&
                            t != detectedHumanName &&
                            t.filter { it.isDigit() } != phoneDigits &&
                            !isLikelyTimestampOrDate(t) &&
                            !isBadLabel(t) &&
                            !isNumericBadge(t) &&
                            !isChannelOrStatusBadge(t) &&
                            !isFolderOrTab(t, detectedFolderNames) &&
                            t.length >= 3
                }?.let { cleanSnippetText(it) } ?: ""

                val isValidChatRow = finalPhone.length >= 7 ||
                        (finalCleanName.isNotBlank() && (hasExplicitDate || detectedSnippet.length >= 3))

                if (isValidChatRow && finalCleanName.isNotBlank() && !isBadLabel(finalCleanName) && !isFolderOrTab(finalCleanName, detectedFolderNames)) {
                    val idKey = if (finalPhone.length >= 7) finalPhone else finalCleanName
                    if (idKey.isNotBlank() && !results.containsKey(idKey)) {
                        results[idKey] = ScannedChat(
                            id = idKey,
                            name = finalCleanName,
                            phoneNumber = if (finalPhone.length >= 7) finalPhone else idKey,
                            dateText = detectedDate,
                            lastMessage = detectedSnippet,
                            isSelected = true,
                            isSent = false,
                            folderName = currentFolder
                        )
                    }
                }
            }
        }

        // Traverse children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            extractChatsFromNodeTree(child, results, currentFolder, detectedFolderNames)
        }
    }

    private fun findNodeByPartialId(node: AccessibilityNodeInfo, partialId: String): AccessibilityNodeInfo? {
        val id = node.viewIdResourceName?.lowercase() ?: ""
        if (id.contains(partialId.lowercase())) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByPartialId(child, partialId)
            if (found != null) return found
        }
        return null
    }

    private fun isLikelyTimestampOrDate(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty()) return false
        // Clock time: 14:30, 9:00 AM, 11:45 pm, 08:30
        if (t.matches(Regex("""^(?:[01]?\d|2[0-3]):[0-5]\d(?:\s*(?:am|pm|AM|PM|a\.m\.|p\.m\.))?$"""))) return true
        // Relative dates
        val relative = listOf("hoy", "ayer", "anteayer", "today", "yesterday")
        if (relative.any { t.equals(it, ignoreCase = true) }) return true
        // Weekdays
        val weekdays = listOf(
            "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo",
            "lun", "mar", "mié", "mie", "jue", "vie", "sáb", "sab", "dom",
            "mon", "tue", "wed", "thu", "fri", "sat", "sun"
        )
        if (weekdays.any { t.equals(it, ignoreCase = true) }) return true
        // Formatted dates: dd/mm, dd/mm/yyyy, dd-mm-yyyy, dd.mm
        if (t.matches(Regex("""^\d{1,2}[/.-]\d{1,2}(?:[/.-]\d{2,4})?$"""))) return true
        // Dates with month name
        val months = listOf(
            "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic",
            "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
        )
        if (months.any { t.contains(it, ignoreCase = true) } && t.any { it.isDigit() }) return true
        return false
    }

    private fun findDateInTexts(texts: List<String>): String? {
        return texts.firstOrNull { isLikelyTimestampOrDate(it) }
    }

    private fun isNumericBadge(text: String): Boolean {
        val t = text.trim()
        return t.matches(Regex("""^\d{1,3}\+?$"""))
    }

    private fun isChannelOrStatusBadge(text: String): Boolean {
        val t = text.trim().lowercase()
        val badges = listOf(
            "wa", "ec", "lead", "chat", "nuevo", "cliente", "whatsapp", "fb", "ig", "crm",
            "w4b", "bot", "auto", "msg", "inbox", "wapp", "wsp", "web"
        )
        return badges.contains(t) || (t.length <= 2 && !t.all { it.isDigit() })
    }

    private fun isBadLabel(text: String): Boolean {
        val t = text.trim().lowercase()
        val bad = listOf(
            "sin leer", "leído", "enviado", "entregado", "archivados", "archived",
            "buscar", "search", "nuevo chat", "chats", "estados", "llamadas", "calls",
            "atrás", "back", "volver", "mensaje", "escribe un mensaje", "type a message",
            "audio", "foto", "video", "sticker", "documento", "cámara", "más opciones"
        )
        return bad.any { t == it }
    }

    private fun cleanSnippetText(raw: String): String {
        return raw.trim()
            .removePrefix("✓✓")
            .removePrefix("✓")
            .removePrefix("✔✔")
            .removePrefix("✔")
            .trim()
    }

    private suspend fun performReliableScrollForward(): Boolean {
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            val verticalList = findVerticalScrollableNode(rootNode)
            if (verticalList != null) {
                val scrolledAction = verticalList.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                if (scrolledAction) {
                    return true
                }
            }
        }
        // Fallback: Gesture swipe up on the middle of the screen
        return swipeUpOnChatList()
    }

    private fun findVerticalScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val screenHeight = resources.displayMetrics.heightPixels

        // Exclude horizontal scrollbars and top tab bars
        val className = node.className?.toString()?.lowercase() ?: ""
        val isHorizontal = className.contains("horizontal")
        val isTall = rect.height() > (screenHeight * 0.35)

        if (node.isScrollable && !isHorizontal && isTall) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findVerticalScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    private suspend fun swipeUpOnChatList(): Boolean = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            val centerX = screenWidth / 2f
            val startY = screenHeight * 0.72f
            val endY = screenHeight * 0.26f

            val path = Path().apply {
                moveTo(centerX, startY)
                lineTo(centerX, endY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, 320)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            var done = false
            var ok = false
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    ok = true
                    done = true
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    ok = false
                    done = true
                }
            }, null)

            for (i in 0 until 12) {
                if (done) break
                delay(35)
            }
            return@withContext ok
        }
        return@withContext false
    }

    private suspend fun performReliableScrollBackward(): Boolean {
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            val verticalList = findVerticalScrollableNode(rootNode)
            if (verticalList != null) {
                val scrolledAction = verticalList.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                if (scrolledAction) {
                    return true
                }
            }
        }
        // Fallback: Gesture swipe down on the middle of the screen
        return swipeDownOnChatList()
    }

    private suspend fun swipeDownOnChatList(): Boolean = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            val centerX = screenWidth / 2f
            val startY = screenHeight * 0.28f
            val endY = screenHeight * 0.72f

            val path = Path().apply {
                moveTo(centerX, startY)
                lineTo(centerX, endY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, 320)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            var done = false
            var ok = false
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    ok = true
                    done = true
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    ok = false
                    done = true
                }
            }, null)

            for (i in 0 until 12) {
                if (done) break
                delay(35)
            }
            return@withContext ok
        }
        return@withContext false
    }

    suspend fun clickAtCoordinatesSuspending(x: Float, y: Float): Boolean = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, 50)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            var completed = false
            var success = false

            val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    success = true
                    completed = true
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    success = false
                    completed = true
                }
            }, null)

            if (!dispatched) return@withContext false

            for (i in 0 until 10) {
                if (completed) break
                delay(20)
            }
            return@withContext success
        }
        return@withContext false
    }

    suspend fun findOrScrollToChat(
        recipientName: String,
        recipientPhone: String,
        maxScrollAttempts: Int = 10
    ): AccessibilityNodeInfo? {
        val queryName = recipientName.trim()
        val queryPhone = recipientPhone.trim()

        // Step 1: Check initial visible screen strictly by user number of the contact
        var chatNode = findMatchingChatOnScreen(queryName, queryPhone)
        if (chatNode != null) return chatNode

        // Step 2: Scroll down (forward) up to 10 times searching for chat with 800ms network delay
        updateFloatingStatus("Buscando a $recipientName (scroll ↓)...")
        for (attempt in 1..maxScrollAttempts) {
            performReliableScrollForward()
            delay(800) // Give CRM network pagination time to fetch and render new chat rows
            chatNode = findMatchingChatOnScreen(queryName, queryPhone)
            if (chatNode != null) return chatNode
        }

        // Step 3: Scroll up (backward) up to 10 times if not found with 800ms network delay
        updateFloatingStatus("Buscando a $recipientName (scroll ↑)...")
        for (attempt in 1..maxScrollAttempts) {
            performReliableScrollBackward()
            delay(800) // Give CRM network pagination time to fetch and render new chat rows
            chatNode = findMatchingChatOnScreen(queryName, queryPhone)
            if (chatNode != null) return chatNode
        }

        return null
    }

    private fun findMatchingChatOnScreen(queryName: String, queryPhone: String): AccessibilityNodeInfo? {
        val phoneMatch = findChatRowByPhoneNumber(queryPhone, queryName)
        if (phoneMatch != null) return phoneMatch
        if (queryName.isNotBlank() && queryName != queryPhone) {
            val nameMatch = findChatRowByPhoneNumber(queryName, queryName)
            if (nameMatch != null) return nameMatch
        }
        return null
    }

    private fun collectTextsRecursive(node: AccessibilityNodeInfo, list: MutableList<String>) {
        val t = node.text?.toString()?.trim()
        if (!t.isNullOrBlank() && !list.contains(t)) {
            list.add(t)
        }
        val desc = node.contentDescription?.toString()?.trim()
        if (!desc.isNullOrBlank() && !list.contains(desc)) {
            list.add(desc)
        }
        for (i in 0 until node.childCount) {
            val c = node.getChild(i) ?: continue
            collectTextsRecursive(c, list)
        }
    }

    // ==========================================
    // ROBUST CHAT DELIVERY (ENTER, TYPE, SEND ARROW, EXIT)
    // ==========================================

    suspend fun performChatDelivery(
        recipientName: String,
        recipientPhone: String,
        messageText: String
    ): Pair<Boolean, String?> {
        // Explicit check: Ensure we are in the target CRM application context before attempting delivery
        if (!isApplicationContext()) {
            return Pair(false, "APP_MINIMIZED: No se detecta el entorno de la app CRM en pantalla.")
        }

        // 1. Locate and click chat row to open conversation if in chat list
        // User requirement: Scroll down up to 10 times, and if not found, scroll up up to 10 times
        val chatNode = findOrScrollToChat(recipientName, recipientPhone, maxScrollAttempts = 10)

        if (chatNode == null) {
            // User requirement: Quedarse en la lista sin pulsar Atras y continuar con el siguiente contacto sin sacarlo de la lista
            return Pair(false, "CONTACT_NOT_FOUND_AFTER_SCROLL: No se encontró al contacto '$recipientName' tras buscar con scroll abajo y arriba. Pasando al siguiente contacto.")
        }

        val clicked = clickNodeCenter(chatNode)
        if (!clicked) {
            chatNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        // Allow CRM transition to conversation view
        delay(500)

        // Explicit check: Ensure focus remained within CRM application context after clicking chat row
        if (!isApplicationContext()) {
            return Pair(false, "APP_MINIMIZED: Foco perdido fuera de la aplicación CRM tras pulsar el chat.")
        }

        // 2. Poll for message input box (give external CRM time to load conversation)
        var inputNode: AccessibilityNodeInfo? = null
        for (attempt in 0 until 14) {
            delay(250)
            inputNode = findMessageInputField()
            if (inputNode != null) break
        }

        if (inputNode == null) {
            inputNode = findBottomAreaInputNode()
        }

        if (inputNode == null) {
            // Guarantee exiting chat safely back to list
            exitChatSafely()
            return Pair(false, "Cuadro de mensaje no encontrado en pantalla tras abrir el chat del CRM")
        }

        // 3. Click directly into the message box and request focus
        val inputRect = Rect()
        inputNode.getBoundsInScreen(inputRect)
        clickNodeCenter(inputNode)
        if (inputRect.width() > 0 && inputRect.height() > 0) {
            clickAtCoordinates(inputRect.centerX().toFloat(), inputRect.centerY().toFloat())
        }
        inputNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        inputNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        inputNode.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        delay(350)

        // 4. Place script content into clipboard and inject
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("crm_message", messageText)
        clipboard.setPrimaryClip(clip)

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, messageText)
        }

        var textInjected = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (!textInjected) {
            val active = findMessageInputField() ?: inputNode
            active.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            textInjected = active.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        if (!textInjected) {
            val focused = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: inputNode
            textInjected = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (!textInjected) {
                textInjected = focused.performAction(AccessibilityNodeInfo.ACTION_PASTE) ||
                        inputNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            }
        }
        // Additional fallback: If still not verified, paste directly into active focus
        if (!textInjected) {
            val currentRoot = rootInActiveWindow
            val focusedInput = currentRoot?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            textInjected = focusedInput?.performAction(AccessibilityNodeInfo.ACTION_PASTE) == true
        }

        // Give the CRM 500ms to register input changes and enable/show the Send button
        delay(500)

        // 5. Execute Multi-Layer Send Action (Explicit node, Right icon in pill, IME Send Action, Coordinate tap)
        var sendClicked = false

        for (attempt in 0 until 10) {
            delay(150)
            val latestInput = findMessageInputField() ?: inputNode
            val inputBounds = Rect()
            latestInput.getBoundsInScreen(inputBounds)

            val executed = executeSendMessageAction(latestInput, inputBounds)
            if (executed) {
                sendClicked = true
                break
            }
        }

        // Give external CRM 2500ms to process, send and transmit the message (showing double check ✓✓)
        delay(2500)

        // Lock recipient in memory immediately upon send action so duplicate messaging is strictly impossible
        if (sendClicked || textInjected) {
            markRecipientProcessed(recipientName)
            markRecipientProcessed(recipientPhone)
            val digits = recipientPhone.filter { it.isDigit() }
            if (digits.length >= 6) {
                markRecipientProcessed(digits)
            }
        }

        // Log send event in unattended diagnostics
        UnattendedAutomationLogger.logEvent(
            context = applicationContext,
            tag = "ENVIO",
            recipient = "$recipientName ($recipientPhone)",
            status = if (sendClicked || textInjected) "ENVIADO_OK" else "ENVIO_FALLIDO",
            exitedChatSuccessfully = false,
            details = "Mensaje procesado. Iniciando espera de 2s para detectar flecha de salida..."
        )

        // 6. ALWAYS EXIT CHAT SAFELY BACK TO CRM LIST WITH SMART VERIFICATION
        val (exited, exitError) = exitChatSafelyWithVerification(recipientName, recipientPhone)
        if (!exited) {
            UnattendedAutomationLogger.logEvent(
                context = applicationContext,
                tag = "SALIDA_FALLIDA",
                recipient = "$recipientName ($recipientPhone)",
                status = "STILL_IN_CHAT",
                exitedChatSuccessfully = false,
                details = "Pausa de seguridad activada tras 3 reintentos por coordenadas. Alerta sonora activada."
            )
            return Pair(false, exitError ?: "No se pudo salir del chat tras el envío")
        }

        UnattendedAutomationLogger.logEvent(
            context = applicationContext,
            tag = "SALIDA_EXITOSA",
            recipient = "$recipientName ($recipientPhone)",
            status = "LISTA_CHATS_CONFIRMADA",
            exitedChatSuccessfully = true,
            details = "Regreso confirmado a lista de chats sin tecla de sistema."
        )

        // Explicit check: Ensure focus remained within CRM application context after navigating back
        if (!isApplicationContext()) {
            return Pair(false, "APP_MINIMIZED: La aplicación CRM perdió el foco al regresar a la lista de chats.")
        }

        if (!sendClicked && !textInjected) {
            return Pair(false, "No se pudo completar el envío del mensaje en el CRM")
        }

        return Pair(true, null)
    }

    suspend fun executeSendMessageAction(
        inputNode: AccessibilityNodeInfo,
        inputBounds: Rect
    ): Boolean {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        // Layer 1: Check if there is an explicit Send Node in the hierarchy
        val root = rootInActiveWindow
        if (root != null) {
            val sendNode = findSendNodeRecursive(root)
            if (sendNode != null) {
                val clicked = sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK) ||
                        (sendNode.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
                if (clicked) return true
                val rect = Rect()
                sendNode.getBoundsInScreen(rect)
                if (rect.width() > 0 && rect.height() > 0) {
                    if (clickAtCoordinatesSuspending(rect.centerX().toFloat(), rect.centerY().toFloat())) {
                        return true
                    }
                }
            }
        }

        // Layer 2: Find the right-most clickable icon / button in the bottom composer row (e.g. EzoCRM microphone/arrow circle)
        val composerRightIcon = findComposerRightIcon(inputNode, inputBounds)
        if (composerRightIcon != null) {
            val clicked = composerRightIcon.performAction(AccessibilityNodeInfo.ACTION_CLICK) ||
                    (composerRightIcon.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
            if (clicked) return true
            val iconRect = Rect()
            composerRightIcon.getBoundsInScreen(iconRect)
            if (iconRect.width() > 0 && iconRect.height() > 0) {
                if (clickAtCoordinatesSuspending(iconRect.centerX().toFloat(), iconRect.centerY().toFloat())) {
                    return true
                }
            }
        }

        // Layer 3: Execute Android IME action (ACTION_IME_ENTER) on input or focused field
        var imeActionSuccess = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val imeEnterId = AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id
            imeActionSuccess = inputNode.performAction(imeEnterId) ||
                    (rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.performAction(imeEnterId) == true)
        }
        if (imeActionSuccess) {
            delay(250)
        }

        // Layer 4: Tap at the physical position of the right-most icon in the message bar
        // In EzoCRM, the pill bar stretches across the bottom; the icon is at the right edge
        val pillRightX = if (inputBounds.right > screenWidth * 0.70) {
            (inputBounds.right - dpToPx(20)).toFloat()
        } else {
            (screenWidth - dpToPx(38)).toFloat()
        }
        val pillCenterY = inputBounds.centerY().toFloat().coerceAtLeast(screenHeight * 0.40f)

        // Try coordinate tap on pill right icon
        val tappedPill = clickAtCoordinatesSuspending(pillRightX, pillCenterY)
        if (tappedPill) {
            delay(250)
        }

        // Layer 5: Fallback tap on the bottom right corner (where floating send buttons or IME Enter buttons reside)
        val cornerX = (screenWidth - dpToPx(32)).toFloat()
        val cornerY = (screenHeight - dpToPx(36)).toFloat()
        clickAtCoordinatesSuspending(cornerX, cornerY)

        return true
    }

    private fun findComposerRightIcon(inputNode: AccessibilityNodeInfo, inputBounds: Rect): AccessibilityNodeInfo? {
        val screenWidth = resources.displayMetrics.widthPixels
        val minX = (screenWidth * 0.68).toInt()

        // 1. Search siblings of inputNode inside its parent container
        val parent = inputNode.parent
        if (parent != null) {
            for (i in (parent.childCount - 1) downTo 0) {
                val child = parent.getChild(i) ?: continue
                val r = Rect()
                child.getBoundsInScreen(r)
                if (r.right >= minX && r.centerY() in (inputBounds.top - 120)..(inputBounds.bottom + 120)) {
                    if (child.isClickable || child.parent?.isClickable == true) {
                        return if (child.isClickable) child else child.parent
                    }
                }
            }
        }

        // 2. Search root tree for any clickable node in the composer right region
        val root = rootInActiveWindow ?: return null
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectClickableComposerNodes(root, inputBounds, minX, candidates)
        return candidates.maxByOrNull {
            val r = Rect()
            it.getBoundsInScreen(r)
            r.right
        }
    }

    private fun collectClickableComposerNodes(
        node: AccessibilityNodeInfo,
        inputBounds: Rect,
        minX: Int,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        val r = Rect()
        node.getBoundsInScreen(r)
        val isAlignedY = r.centerY() in (inputBounds.top - 140)..(inputBounds.bottom + 140)
        val isRightSided = r.centerX() >= minX || r.right >= minX
        val isValidSize = r.width() in dpToPx(16)..dpToPx(140) && r.height() in dpToPx(16)..dpToPx(140)

        if ((node.isClickable || node.parent?.isClickable == true) && isAlignedY && isRightSided && isValidSize) {
            results.add(if (node.isClickable) node else (node.parent ?: node))
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectClickableComposerNodes(child, inputBounds, minX, results)
        }
    }

    private fun findBottomAreaInputNode(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val screenHeight = resources.displayMetrics.heightPixels
        val minY = (screenHeight * 0.50).toInt()
        val candidates = mutableListOf<Pair<AccessibilityNodeInfo, Int>>()
        collectInputCandidates(root, minY, screenHeight, candidates)
        candidates.sortByDescending { (node, score) ->
            val rect = Rect()
            node.getBoundsInScreen(rect)
            score * 10000 + rect.bottom
        }
        val topCandidate = candidates.firstOrNull()?.first
        if (topCandidate != null) return topCandidate

        // Geometric fallback: Detect bottom composer pill bar
        val screenWidth = resources.displayMetrics.widthPixels
        return findBottomComposerByGeometry(root, screenHeight, screenWidth)
    }

    fun findMessageInputField(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val screenHeight = resources.displayMetrics.heightPixels
        val minY = (screenHeight * 0.25).toInt()

        val candidates = mutableListOf<Pair<AccessibilityNodeInfo, Int>>()
        collectInputCandidates(rootNode, minY, screenHeight, candidates)

        candidates.sortByDescending { (node, score) ->
            val rect = Rect()
            node.getBoundsInScreen(rect)
            score * 10000 + rect.bottom
        }

        val topCandidate = candidates.firstOrNull()?.first
        if (topCandidate != null) return topCandidate

        val screenWidth = resources.displayMetrics.widthPixels
        return findBottomComposerByGeometry(rootNode, screenHeight, screenWidth)
    }

    private fun findBottomComposerByGeometry(
        root: AccessibilityNodeInfo,
        screenHeight: Int,
        screenWidth: Int
    ): AccessibilityNodeInfo? {
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        fun scan(node: AccessibilityNodeInfo) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            // Bottom composer bar in CRM apps sits strictly in the bottom 25% of the screen
            val inBottomZone = rect.centerY() > screenHeight * 0.75 && rect.centerY() < screenHeight * 0.99
            val reasonableWidth = rect.width() in (screenWidth * 0.30).toInt()..(screenWidth * 0.98).toInt()
            val reasonableHeight = rect.height() in dpToPx(24)..dpToPx(130)

            val text = (node.text?.toString() ?: "").lowercase()
            val hint = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) node.hintText?.toString() ?: "" else "").lowercase()
            val desc = (node.contentDescription?.toString() ?: "").lowercase()
            val id = (node.viewIdResourceName?.lowercase() ?: "")
            val cls = (node.className?.toString() ?: "").lowercase()

            val isDirectEditable = node.isEditable || cls.contains("edittext") || cls.contains("textinput")
            // True input fields use hintText or specific ID, not regular chat list text
            val hasTrueComposerHint = hint.contains("mensaje") || hint.contains("message") || hint.contains("escribe")
            val hasComposerId = id.contains("composer") || id.contains("msg_text") || id.contains("et_message") || id.contains("input_box")

            val hasInputAffordance = isDirectEditable || hasTrueComposerHint || hasComposerId

            if (inBottomZone && reasonableWidth && reasonableHeight && hasInputAffordance && node.childCount <= 3) {
                candidates.add(node)
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                scan(child)
            }
        }
        scan(root)
        return candidates.maxByOrNull {
            val r = Rect()
            it.getBoundsInScreen(r)
            r.bottom
        }
    }

    private fun collectInputCandidates(
        node: AccessibilityNodeInfo,
        minY: Int,
        screenHeight: Int,
        results: MutableList<Pair<AccessibilityNodeInfo, Int>>
    ) {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        if (rect.bottom >= minY && rect.width() > 10 && rect.height() > 10) {
            val text = node.text?.toString() ?: ""
            val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                node.hintText?.toString() ?: ""
            } else ""
            val desc = node.contentDescription?.toString() ?: ""
            val className = node.className?.toString() ?: ""
            val id = node.viewIdResourceName?.lowercase() ?: ""

            val isDirectEditable = node.isEditable ||
                    className.contains("EditText", ignoreCase = true) ||
                    className.contains("TextInput", ignoreCase = true)

            val combined = "$text $hint $desc $id".lowercase()
            val isSearchBar = (rect.top < screenHeight * 0.25 && (combined.contains("buscar") || combined.contains("search")))

            if (!isSearchBar) {
                var score = 0
                if (isDirectEditable) score += 60
                val hasComposerText = combined.contains("mensaje") || combined.contains("message") ||
                        combined.contains("escribe") || combined.contains("escribir") ||
                        combined.contains("write") || combined.contains("responder") ||
                        combined.contains("reply") || combined.contains("respuesta")

                if (hasComposerText) score += 120
                if (combined.contains("nota") || combined.contains("comentario") || combined.contains("chat")) score += 30
                if (id.contains("entry") || id.contains("composer") || id.contains("msg_text") || id.contains("et_message")) score += 45
                if (rect.centerY() > screenHeight * 0.50) score += 40

                val isContainer = className.contains("RecyclerView") || className.contains("ListView") ||
                        className.contains("ScrollView") || (node.childCount > 2 && !isDirectEditable)

                // Specifically for EzoCRM and custom CRM chat bars:
                // Placeholder views displaying "Mensaje..." often have isEditable = false and isClickable = false,
                // but clicking their coordinates directly or their parent activates the soft keyboard and converts to an input field.
                val isEzoCrmMessageBar = hasComposerText && rect.centerY() > screenHeight * 0.40 && rect.height() in 15..500

                if (!isContainer && (isDirectEditable || isEzoCrmMessageBar || (score >= 70 && (node.isClickable || node.isFocusable)))) {
                    results.add(Pair(node, score + if (isEzoCrmMessageBar) 100 else 0))
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectInputCandidates(child, minY, screenHeight, results)
        }
    }

    private fun clickSendArrowNearInput(inputNode: AccessibilityNodeInfo): Boolean {
        val inputRect = Rect()
        inputNode.getBoundsInScreen(inputRect)

        val root = rootInActiveWindow ?: return false
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        findSendCandidates(root, inputRect, candidates)

        for (candidate in candidates) {
            val clicked = clickNodeCenter(candidate)
            if (clicked) return true
        }

        // Tap physical coordinates just to the right of the message box
        val arrowX = (inputRect.right + dpToPx(32)).toFloat()
        val arrowY = inputRect.centerY().toFloat()
        return clickAtCoordinates(arrowX, arrowY)
    }

    private fun findSendCandidates(node: AccessibilityNodeInfo, inputRect: Rect, results: MutableList<AccessibilityNodeInfo>) {
        val nodeRect = Rect()
        node.getBoundsInScreen(nodeRect)

        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        val isSendText = desc.contains("enviar") || desc.contains("send") || desc.contains("flecha") ||
                text.contains("enviar") || text.contains("send") || text.contains("➤") || text.contains("→") ||
                viewId.contains("send") || viewId.contains("arrow")

        val isNearInputRight = (nodeRect.left >= inputRect.left && nodeRect.centerY() in (inputRect.top - 120)..(inputRect.bottom + 120))

        if (isSendText || (isNearInputRight && node.isClickable && nodeRect.width() in 20..320)) {
            if (!results.contains(node)) {
                results.add(node)
            }
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findSendCandidates(it, inputRect, results) }
        }
    }

    fun clickSendButton(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val sendNode = findSendNodeRecursive(rootNode)
        if (sendNode != null) {
            return clickNodeCenter(sendNode)
        }
        return false
    }

    private fun findSendNodeRecursive(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        val isSend = desc.contains("enviar") || desc.contains("send") ||
                desc.contains("responder") || desc.contains("reply") ||
                desc.contains("submit") || desc.contains("publicar") ||
                desc.contains("post") || desc.contains("guardar") ||
                desc.contains("flecha") || desc.contains("arrow") ||
                desc.contains("avión") || desc.contains("avion") ||
                desc.contains("plane") || desc.contains("paper") ||
                text.contains("enviar") || text.contains("send") ||
                text.contains("responder") || text.contains("reply") ||
                text.contains("guardar") || text.contains("save") ||
                viewId.contains("send") || viewId.contains("enviar") ||
                viewId.contains("composer_send") || viewId.contains("btn_send") ||
                viewId.contains("send_button") || viewId.contains("action_send") ||
                viewId.contains("submit") || viewId.contains("action_reply") ||
                viewId.contains("btn_reply") || viewId.contains("fab_send") ||
                text == "➤" || text == "→" || text == "➜" || text == "►" || text == "▶" || text == "↵"

        val rect = Rect()
        node.getBoundsInScreen(rect)
        val screenHeight = resources.displayMetrics.heightPixels
        // The send button in chat composers is situated in the middle/lower screen area
        val isInBottomComposerArea = rect.bottom >= screenHeight * 0.35

        if (isSend && isInBottomComposerArea && rect.width() in 15..500 && rect.height() in 15..500) {
            return if (node.isClickable) node else (node.parent ?: node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findSendNodeRecursive(child)
            if (found != null) return found
        }
        return null
    }

    fun isInsideConversation(): Boolean {
        if (!isCurrentWindowInCrmApp()) return false
        val root = rootInActiveWindow ?: return false

        val hasTopBackButton = findTopBackButton(root) != null
        val hasChatListElements = findChatListNode(root) != null

        // In the main chat list screen, there are chat items but NO top back button
        if (hasChatListElements && !hasTopBackButton) {
            return false
        }

        // Inside a conversation, either the top back button or a real input field is present
        return hasTopBackButton || (findMessageInputField() != null)
    }

    /**
     * Explicit check to ensure focus remains strictly within the target CRM application context
     * during automation loops, preventing navigation from escaping to the launcher or other apps.
     */
    fun isApplicationContext(targetPackage: String? = null): Boolean {
        val root = rootInActiveWindow ?: return false
        val pkg = root.packageName?.toString()?.lowercase()?.trim() ?: return false
        if (pkg.isBlank()) return false

        // Disqualify launchers, system UI, keyboards, and recent apps overview
        if (pkg.contains("launcher") || pkg.contains("systemui") ||
            pkg.contains("inputmethod") || pkg.contains("recents") ||
            pkg.contains("android.googlequicksearchbox")) {
            return false
        }

        // Disqualify Como On itself (our own application package)
        if (pkg == applicationContext.packageName.lowercase().trim()) {
            return false
        }

        val expected = targetPackage?.lowercase()?.trim() ?: _activeCrmPackage.value.lowercase().trim()
        if (expected.isNotBlank()) {
            if (pkg == expected) return true
            if (expected.contains("ezocrm") && pkg.contains("ezocrm")) return true
            if (expected.contains("whatsapp") && pkg.contains("whatsapp")) return true
            if (expected.contains("kommo") && pkg.contains("kommo")) return true
            val idExpected = identifyCrmPackage(expected)
            val idActual = identifyCrmPackage(pkg)
            if (idExpected != null && idActual == idExpected) return true
            return false
        }

        return identifyCrmPackage(pkg) != null
    }

    fun isCurrentWindowInCrmApp(): Boolean = isApplicationContext()

    fun isChatListScreen(): Boolean {
        if (!isCurrentWindowInCrmApp()) return false
        return !isInsideConversation()
    }

    fun findChatListNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val id = node.viewIdResourceName?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val className = node.className?.toString() ?: ""

        val isChatRowId = id.contains("conversations_row") ||
                id.contains("contact_name") ||
                id.contains("chat_row") ||
                id.contains("conversation_contact_name") ||
                id.contains("single_msg_tv") ||
                id.contains("chat_title") ||
                id.contains("chat_list") ||
                id.contains("contact_list") ||
                id.contains("lead_item") ||
                id.contains("deal_item") ||
                id.contains("inbox_item")

        val isChatListContainer = (className.contains("RecyclerView") || className.contains("ListView")) &&
                node.childCount > 0

        val isChatTab = (id.contains("fab") || desc.contains("nuevo") || desc.contains("new") ||
                text == "chats" || desc == "chats" || text == "inbox" || desc == "inbox" ||
                text == "contactos" || desc == "contactos" || text == "leads" || desc == "leads")

        if (isChatRowId || (isChatListContainer && !id.contains("message")) || isChatTab) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findChatListNode(child)
            if (found != null) return found
        }
        return null
    }

    fun isChatListNodePresent(): Boolean {
        if (!isCurrentWindowInCrmApp()) return false
        val root = rootInActiveWindow ?: return false

        val hasChatListElements = findChatListNode(root) != null
        val hasTopBackButton = findTopBackButton(root) != null
        val hasMessageInput = findMessageInputField() != null

        // 1. Definite proof: Chat list rows/containers are present and there is NO top back button
        if (hasChatListElements && !hasTopBackButton) {
            return true
        }

        // 2. Definite proof: Neither top back button nor message composer is present
        if (!hasTopBackButton && !hasMessageInput) {
            return true
        }

        // 3. Fallback: Chat list elements exist in current window hierarchy
        if (hasChatListElements) {
            return true
        }

        return !hasMessageInput
    }

    suspend fun waitForChatList(timeoutMs: Long = 2500L, intervalMs: Long = 200L): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (isChatListNodePresent()) {
                return true
            }
            delay(intervalMs)
        }
        return isChatListNodePresent()
    }

    suspend fun exitChatSafelyWithVerification(
        recipientName: String = "",
        recipientPhone: String = ""
    ): Pair<Boolean, String?> {
        // Step 0: Check if we are already in the chat list (no composer input present)
        if (isChatListNodePresent()) {
            UnattendedAutomationLogger.logEvent(
                context = applicationContext,
                tag = "SALIDA_VERIFICADA",
                recipient = "$recipientName ($recipientPhone)",
                status = "YA_EN_LISTA",
                exitedChatSuccessfully = true,
                details = "Ya en lista de chats sin necesidad de acción."
            )
            return Pair(true, null)
        }

        // Explicit check: Ensure focus is strictly within the CRM application context
        if (!isApplicationContext()) {
            UnattendedAutomationLogger.logEvent(
                context = applicationContext,
                tag = "ERROR_CONTEXTO",
                recipient = "$recipientName ($recipientPhone)",
                status = "APP_MINIMIZED",
                exitedChatSuccessfully = false,
                details = "La aplicación no está en el contexto del CRM."
            )
            return Pair(false, "APP_MINIMIZED: La aplicación no está en el contexto del CRM. Pausa preventiva.")
        }

        // Requisito: Tras enviar, esperar para detectar la flecha 'atrás' o confirmar retorno a la lista
        delay(1500)

        // Si ya retornó a la lista de chats automáticamente tras enviar, confirmar de inmediato
        if (isChatListNodePresent()) {
            UnattendedAutomationLogger.logEvent(
                context = applicationContext,
                tag = "SALIDA_VERIFICADA",
                recipient = "$recipientName ($recipientPhone)",
                status = "YA_EN_LISTA",
                exitedChatSuccessfully = true,
                details = "Confirmada presencia en lista de chats tras espera post-envío."
            )
            return Pair(true, null)
        }

        // Intento 1: Detectar la flecha 'atrás' superior izquierda en la jerarquía del chat
        val root = rootInActiveWindow
        val backBtn = if (root != null) findTopBackButton(root) else null
        if (backBtn != null) {
            val clicked = if (backBtn.isClickable) backBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK) else false
            if (!clicked) {
                clickNodeCenter(backBtn)
            }
            if (waitForChatList(timeoutMs = 1500L, intervalMs = 150L) || isChatListNodePresent()) {
                UnattendedAutomationLogger.logEvent(
                    context = applicationContext,
                    tag = "SALIDA_NODO_EXITO",
                    recipient = "$recipientName ($recipientPhone)",
                    status = "OK",
                    exitedChatSuccessfully = true,
                    details = "Flecha atrás detectada por nodo y pulsada con éxito."
                )
                return Pair(true, null)
            }
        }

        // Requisito: Si no la encuentra o no salió, reintentar buscarla por coordenadas un máximo de 3 veces
        // Coordenadas calibradas del botón circular blanco con flecha '<' en EzoCRM:
        val defaultArrowX = dpToPx(42).toFloat()
        val defaultArrowY = dpToPx(65).toFloat()

        for (attempt in 1..3) {
            UnattendedAutomationLogger.logEvent(
                context = applicationContext,
                tag = "REINTENTO_COORDENADAS",
                recipient = "$recipientName ($recipientPhone)",
                status = "REINTENTO_$attempt",
                exitedChatSuccessfully = false,
                details = "Reintento $attempt/3 por coordenadas ($defaultArrowX, $defaultArrowY)"
            )

            clickAtCoordinates(defaultArrowX, defaultArrowY)

            val reached = waitForChatList(timeoutMs = 1200L, intervalMs = 150L)
            if (reached || isChatListNodePresent()) {
                UnattendedAutomationLogger.logEvent(
                    context = applicationContext,
                    tag = "SALIDA_COORDENADAS_EXITO",
                    recipient = "$recipientName ($recipientPhone)",
                    status = "OK",
                    exitedChatSuccessfully = true,
                    details = "Salida exitosa al reintento $attempt/3 por coordenadas."
                )
                return Pair(true, null)
            }
            delay(400)
        }

        // Requisito: Si tras el tercer intento sigue en chat, activa una alerta sonora y pausa la automatización de forma segura
        playSafetyAlertSound()
        vibrateSafetyAlert()

        UnattendedAutomationLogger.logEvent(
            context = applicationContext,
            tag = "ALERTA_PAUSA_SEGURIDAD",
            recipient = "$recipientName ($recipientPhone)",
            status = "STILL_IN_CHAT",
            exitedChatSuccessfully = false,
            details = "Sigue en chat tras 3 reintentos. Alerta sonora activada. Pausando de forma segura."
        )

        return Pair(
            false,
            "STILL_IN_CHAT: No se pudo salir del chat tras 3 intentos por coordenadas. Alerta sonora activada y pausa de seguridad efectuada."
        )
    }

    fun exitChatSafely(recipientName: String = "", recipientPhone: String = "") {
        // If outside CRM context or already in chat list (no message input), NEVER press BACK!
        if (!isApplicationContext() || isChatListNodePresent()) {
            return
        }
        val root = rootInActiveWindow
        val backBtn = if (root != null) findTopBackButton(root) else null
        if (backBtn != null) {
            val clicked = if (backBtn.isClickable) backBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK) else false
            if (!clicked) {
                clickNodeCenter(backBtn)
            }
        } else {
            clickAtCoordinates(dpToPx(42).toFloat(), dpToPx(65).toFloat())
        }
    }

    fun findTopBackButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val screenHeight = resources.displayMetrics.heightPixels
        val screenWidth = resources.displayMetrics.widthPixels

        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        val isBackText = desc.contains("atrás") || desc.contains("atras") ||
                desc.contains("back") || desc.contains("volver") ||
                desc.contains("regresar") || desc.contains("cerrar") || desc.contains("close") ||
                desc.contains("desplazarse hacia arriba") || desc.contains("navigate up") ||
                desc.contains("up") || desc.contains("arrow_back") || desc.contains("chevron") ||
                text == "←" || text == "<" || text == "«" ||
                viewId.contains("back") || viewId.contains("up") || viewId.contains("nav_back") ||
                viewId.contains("toolbar_back") || viewId.contains("action_back") ||
                viewId.contains("btn_back") || viewId.contains("close") || viewId.contains("home") ||
                viewId.contains("navigation_icon")

        // Top-left area scaled dynamically by screen size
        val isInTopLeftArea = rect.top < (screenHeight * 0.25) && rect.left < (screenWidth * 0.40) &&
                rect.width() > 0 && rect.height() > 0

        // In EzoCRM / React Native chat headers, top-left back arrow is often an icon without text
        val isTopLeftCircularNav = rect.top < (screenHeight * 0.16) && rect.left < (screenWidth * 0.25) &&
                rect.width() in dpToPx(24)..dpToPx(85) && rect.height() in dpToPx(24)..dpToPx(85) &&
                (node.isClickable || node.parent?.isClickable == true)

        if ((isBackText && isInTopLeftArea) || isTopLeftCircularNav) {
            return if (node.isClickable) node else (node.parent ?: node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findTopBackButton(child)
            if (found != null) return found
        }
        return null
    }

    fun clickAtCoordinates(x: Float, y: Float, onComplete: ((Boolean) -> Unit)? = null): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, 50)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            return dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    onComplete?.invoke(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    onComplete?.invoke(false)
                }
            }, null)
        }
        return false
    }

    fun clickNodeCenter(node: AccessibilityNodeInfo, onComplete: ((Boolean) -> Unit)? = null): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val centerX = rect.centerX().toFloat()
        val centerY = rect.centerY().toFloat()

        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            onComplete?.invoke(true)
            return true
        }

        return clickAtCoordinates(centerX, centerY, onComplete)
    }

    fun calculateRandomDelay(
        minSeconds: Int = DEFAULT_MIN_DELAY_SECONDS,
        maxSeconds: Int = DEFAULT_MAX_DELAY_SECONDS
    ): Int {
        val safeMin = minSeconds.coerceAtLeast(HARD_FLOOR_MIN_DELAY_SECONDS)
        val safeMax = maxSeconds.coerceAtLeast(safeMin)
        return Random.nextInt(safeMin, safeMax + 1)
    }

    suspend fun awaitAntiSpamDelay(
        minSeconds: Int = DEFAULT_MIN_DELAY_SECONDS,
        maxSeconds: Int = DEFAULT_MAX_DELAY_SECONDS,
        onTick: ((Int) -> Unit)? = null
    ): Int {
        val jitterDelay = calculateRandomDelay(minSeconds, maxSeconds)

        _antiSpamTimerState.value = AntiSpamTimerState(
            isWaiting = true,
            secondsRemaining = jitterDelay,
            targetDurationSeconds = jitterDelay,
            lastJitterDelaySeconds = jitterDelay
        )

        for (sec in jitterDelay downTo 1) {
            _antiSpamTimerState.value = _antiSpamTimerState.value.copy(secondsRemaining = sec)
            updateFloatingStatus("Pausa anti-spam: ${sec}s restantes")
            onTick?.invoke(sec)
            delay(1000)
        }

        _antiSpamTimerState.value = _antiSpamTimerState.value.copy(
            isWaiting = false,
            secondsRemaining = 0
        )
        onTick?.invoke(0)
        return jitterDelay
    }

    fun findChatRowByPhoneNumber(targetPhone: String, targetName: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val targetDigits = targetPhone.filter { it.isDigit() }
        val cleanTargetName = targetName.trim().lowercase()

        // 1. Direct search by contact_name / conversations_row_contact_name IDs (highest accuracy in CRM lists)
        val nameNodes = mutableListOf<AccessibilityNodeInfo>()
        collectContactNameNodes(rootNode, nameNodes)

        for (nameNode in nameNodes) {
            val headerText = (nameNode.text ?: nameNode.contentDescription)?.toString()?.trim() ?: ""
            if (headerText.isBlank()) continue
            val headerDigits = headerText.filter { it.isDigit() }

            // User requirement: Never match or click a row that has already been processed in this session
            if (isRecipientProcessed(headerText) || (headerDigits.length >= 6 && isRecipientProcessed(headerDigits))) {
                continue
            }

            val isDigitMatch = targetDigits.length >= 6 && headerDigits.length >= 6 &&
                    (headerDigits == targetDigits || headerDigits.endsWith(targetDigits) || targetDigits.endsWith(headerDigits))
            val isNameMatch = cleanTargetName.isNotBlank() && headerText.lowercase() == cleanTargetName

            if (isDigitMatch || isNameMatch) {
                var clickable = nameNode
                while (clickable.parent != null && !clickable.isClickable) {
                    clickable = clickable.parent
                }
                return if (clickable.isClickable) clickable else nameNode
            }
        }

        // 2. Search structural chat row containers, extracting and evaluating all candidate texts in row
        val rowContainers = mutableListOf<AccessibilityNodeInfo>()
        collectChatRowContainers(rootNode, rowContainers)

        for (row in rowContainers) {
            val texts = mutableListOf<String>()
            collectTextsExcludingSnippets(row, texts)
            if (texts.isEmpty()) continue

            // User requirement: Never match or click a row that has already been processed in this session
            val isAlreadyProcessed = texts.any { t ->
                val d = t.filter { c -> c.isDigit() }
                isRecipientProcessed(t) || (d.length >= 6 && isRecipientProcessed(d))
            }
            if (isAlreadyProcessed) continue

            // Check if ANY text in the row contains the target digits (ignoring prefixes like "WA", "EC", etc.)
            val rowDigitsList = texts.map { it.filter { c -> c.isDigit() } }.filter { it.length >= 6 }
            val isDigitMatch = targetDigits.length >= 6 && rowDigitsList.any { d ->
                d == targetDigits || d.endsWith(targetDigits) || targetDigits.endsWith(d)
            }

            // Name match across non-snippet texts in row
            val isNameMatch = cleanTargetName.isNotBlank() && texts.any { t ->
                val cleanT = t.trim().lowercase()
                cleanT == cleanTargetName || (cleanTargetName.length >= 4 && cleanT.contains(cleanTargetName))
            }

            if (isDigitMatch || isNameMatch) {
                var clickable = row
                while (clickable.parent != null && !clickable.isClickable) {
                    clickable = clickable.parent
                }
                return if (clickable.isClickable) clickable else row
            }
        }

        return null
    }

    private fun collectContactNameNodes(node: AccessibilityNodeInfo, results: MutableList<AccessibilityNodeInfo>) {
        val id = node.viewIdResourceName?.lowercase() ?: ""
        if (id.contains("conversations_row_contact_name") || id.contains("contact_name") || id.contains("chat_title")) {
            results.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectContactNameNodes(child, results)
        }
    }

    private fun collectChatRowContainers(node: AccessibilityNodeInfo, results: MutableList<AccessibilityNodeInfo>) {
        val id = node.viewIdResourceName?.lowercase() ?: ""
        val className = node.className?.toString() ?: ""
        val isRow = id.contains("conversations_row") || id.contains("chat_row") || id.contains("lead_item") ||
                id.contains("inbox_item") || (node.isClickable && node.childCount >= 2 && !className.contains("RecyclerView") && !className.contains("ListView"))

        if (isRow && !results.contains(node)) {
            results.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectChatRowContainers(child, results)
        }
    }

    private fun extractRowContactHeader(rowNode: AccessibilityNodeInfo): String {
        // Collect all text nodes, strictly ignoring message snippet / preview nodes
        val texts = mutableListOf<String>()
        collectTextsExcludingSnippets(rowNode, texts)

        // First non-date, non-badge text in the row is the contact header (number or name of the chat)
        return texts.firstOrNull { t ->
            !isLikelyTimestampOrDate(t) && !isNumericBadge(t) && t.length in 2..50
        } ?: ""
    }

    private fun collectTextsExcludingSnippets(node: AccessibilityNodeInfo, list: MutableList<String>) {
        val id = node.viewIdResourceName?.lowercase() ?: ""
        // Crucial: Reject last message previews / snippets so we NEVER match inside the chat message body
        val isSnippetNode = id.contains("single_msg_tv") || id.contains("last_msg") ||
                id.contains("msg_preview") || id.contains("snippet") || id.contains("msg_text")

        if (isSnippetNode) return

        val text = node.text?.toString()?.trim()
        if (!text.isNullOrBlank() && !list.contains(text)) {
            list.add(text)
        }
        val desc = node.contentDescription?.toString()?.trim()
        if (!desc.isNullOrBlank() && !list.contains(desc)) {
            list.add(desc)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTextsExcludingSnippets(child, list)
        }
    }

    fun findChatNodeOnScreen(searchQuery: String): AccessibilityNodeInfo? {
        val query = searchQuery.trim()
        if (query.isBlank() || isRecipientProcessed(query)) return null
        return findChatRowByPhoneNumber(query, query)
    }

    fun goBackToChatList(): Boolean {
        exitChatSafely()
        return true
    }
}
