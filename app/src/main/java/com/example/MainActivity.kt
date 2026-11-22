package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.scale
import com.example.ui.theme.AmberDelay
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import android.widget.Toast
import com.example.data.model.UserAccount
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ScriptItem
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.AutomationScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ScriptsScreen
import com.example.ui.screens.SuperuserScreen
import com.example.ui.screens.RestrictedAccessScreen
import com.example.ui.components.GeneralSettingsAndHelpDialog
import com.example.util.AccessLockReason
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.style.TextOverflow
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Surface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.PurpleGlow
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import androidx.compose.material.icons.filled.WaterDrop

enum class AppTab {
    DASHBOARD,
    SCRIPTS,
    AUTOMATION,
    PSA_WORKSPACE,
    SUPERUSER
}

class MainActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { com.google.firebase.FirebaseApp.initializeApp(this) }
        enableEdgeToEdge()
        runCatching {
            com.example.util.CriticalNotificationHelper.ensureNotificationChannel(this)
            com.example.service.EzoFirebaseMessagingService.subscribeToCriticalAlertsTopic()
        }
        handleBankingModeIntent(intent)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val fontScaleLevel by viewModel.fontScaleLevel.collectAsState()
            MyApplicationTheme(fontScaleLevel = fontScaleLevel) {
                MainApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleBankingModeIntent(intent)
    }

    private fun handleBankingModeIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra("ACTION_RESTORE_BUBBLE", false) == true) {
            com.example.service.EzoAccessibilityService.disableBankingMode()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isAppPaused by viewModel.isAppPaused.collectAsState()
    val isBankingMode by viewModel.isBankingMode.collectAsState()
    val killSwitchTriggered by viewModel.killSwitchTriggered.collectAsState()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsState()
    val fontScaleLevel by viewModel.fontScaleLevel.collectAsState()
    val hasSeenSetupGuide by viewModel.hasSeenSetupGuide.collectAsState()
    val activePsaCountry by viewModel.activePsaCountry.collectAsState()
    val licenseEvaluation by viewModel.licenseEvaluation.collectAsState()
    val isNetworkOnline by viewModel.isNetworkOnline.collectAsState()
    val lockVerificationFeedback by viewModel.lockVerificationFeedback.collectAsState()
    val pendingAdminPricingUpdate by viewModel.pendingAdminPricingUpdate.collectAsState()
    val pricingSyncBannerNotice by viewModel.pricingSyncBannerNotice.collectAsState()
    val availableAppUpdate by viewModel.availableAppUpdate.collectAsState()
    val appUpdateCheckFeedback by viewModel.appUpdateCheckFeedback.collectAsState()
    val isAppDistributionSignedIn by viewModel.isAppDistributionTesterSignedIn.collectAsState()
    val isLargeFont = com.example.ui.theme.LocalIsLargeFontMode.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showKillSwitchConfirmDialog by remember { mutableStateOf(false) }
    var showSettingsAndHelpDialog by remember { mutableStateOf(false) }
    var showExpiringRenewDialog by remember { mutableStateOf(false) }

    var currentTab by remember { mutableStateOf(AppTab.PSA_WORKSPACE) }
    var scriptForAutomation by remember { mutableStateOf<ScriptItem?>(null) }

    val activeUserKey = currentUser?.let { it.email.ifBlank { it.id } } ?: ""
    LaunchedEffect(activeUserKey) {
        val logged = currentUser
        if (logged != null) {
            currentTab = if (logged.role == "SUPERUSER") AppTab.DASHBOARD else AppTab.PSA_WORKSPACE
        }
    }

    // Re-evaluate local & online integrity every time the app enters foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAppForegrounded(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(pricingSyncBannerNotice) {
        val msg = pricingSyncBannerNotice
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.dismissPricingSyncBannerNotice()
        }
    }

    LaunchedEffect(appUpdateCheckFeedback) {
        val msg = appUpdateCheckFeedback
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearAppUpdateCheckFeedback()
        }
    }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.trySilentCloudSignIn(context)
    }

    val accountBlockedNotice by viewModel.accountBlockedNotice.collectAsState()
    if (accountBlockedNotice != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAccountBlockedNotice() },
            title = {
                Text("⚠️ Notificación de Cuenta", fontWeight = FontWeight.Bold, color = RoseError)
            },
            text = {
                Text(accountBlockedNotice ?: "", color = TextPrimary)
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissAccountBlockedNotice() },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Entendido", color = Color.White)
                }
            },
            containerColor = SlateCard
        )
    }

    val updateOffer = availableAppUpdate
    if (updateOffer != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAppUpdateDialog() },
            title = {
                Text(
                    text = "🚀 Nueva Versión Disponible",
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.PsaCyanGlow,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Hay una actualización lista para Como On.",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateBackground)
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Instalada: v${BuildConfig.VERSION_NAME} (Comp. ${BuildConfig.VERSION_CODE})",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Nueva en servidor: v${updateOffer.latestVersionName} (Comp. ${updateOffer.latestVersionCode})",
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                    if (updateOffer.releaseNotes.isNotBlank()) {
                        Text(
                            text = "Novedades:\n${updateOffer.releaseNotes}",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                    Text(
                        text = "Esta actualización es opcional. Al tocar Descargar APK se abrirá tu navegador para bajar el instalador.",
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = com.example.util.AppUpdateChecker.buildBrowserDownloadIntent(updateOffer.apkDownloadUrl)
                        if (intent != null) {
                            try {
                                context.startActivity(intent)
                                viewModel.dismissAppUpdateDialog()
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context,
                                    "No se encontró un navegador para abrir el enlace de descarga.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "El enlace de descarga no es válido.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("download_new_apk_button")
                ) {
                    Text("Descargar APK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = { viewModel.dismissAppUpdateDialog() },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("dismiss_update_dialog_button")
                ) {
                    Text("Más tarde", color = TextSecondary)
                }
            },
            containerColor = SlateCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Check if user is not logged in
    val user = currentUser
    if (user == null) {
        AuthScreen(
            viewModel = viewModel,
            onLoginSuccess = {
                val loggedUser = viewModel.currentUser.value
                currentTab = if (loggedUser?.role == "SUPERUSER") AppTab.DASHBOARD else AppTab.PSA_WORKSPACE
            }
        )
        return
    }

    val effectiveLockReason = when {
        licenseEvaluation.isLocked -> licenseEvaluation.reason
        user.isBlocked -> AccessLockReason.ADMIN_BLOCKED
        user.isExpired -> AccessLockReason.DAYS_EXPIRED
        else -> AccessLockReason.NONE
    }

    // Immediate blocking, offline 24h, reboot, clock-tamper & expiration gate
    if (effectiveLockReason != AccessLockReason.NONE) {
        var showContactDialog by remember { mutableStateOf(false) }
        RestrictedAccessScreen(
            user = user,
            lockReason = effectiveLockReason,
            lastOnlineVerifiedAtMs = licenseEvaluation.lastOnlineVerifiedAtMs,
            isNetworkOnline = isNetworkOnline,
            feedbackMessage = lockVerificationFeedback,
            onRefresh = {
                viewModel.refreshUserCloudStatus(user.username)
            },
            onContactSuperuser = {
                showContactDialog = true
            },
            onLogout = {
                viewModel.logout()
            }
        )
        if (showContactDialog) {
            ContactSuperuserDialog(
                user = user,
                lockReason = effectiveLockReason,
                onDismiss = { showContactDialog = false }
            )
        }
        return
    }

    val isGlobalLegacyEnabled by viewModel.isGlobalLegacyEnabled.collectAsState()
    val roleMatrix by viewModel.roleVisibilityConfig.collectAsState()
    val isSuperuser = user.role == "SUPERUSER"
    val canAccessLegacy = isSuperuser || isGlobalLegacyEnabled || user.canAccessLegacyModules

    val showDashboard = roleMatrix.isTabVisible("DASHBOARD", isSuperuser) && canAccessLegacy
    val showScripts = roleMatrix.isTabVisible("SCRIPTS", isSuperuser) && canAccessLegacy
    val showAutomation = roleMatrix.isTabVisible("AUTOMATION", isSuperuser) && canAccessLegacy
    val showPsa = roleMatrix.isTabVisible("PSA_WORKSPACE", isSuperuser)
    val showSuperuser = isSuperuser && roleMatrix.isTabVisible("SUPERUSER", true)

    LaunchedEffect(currentTab, showDashboard, showScripts, showAutomation, showPsa, showSuperuser, isSuperuser) {
        val isCurrentTabVisible = when (currentTab) {
            AppTab.DASHBOARD -> showDashboard
            AppTab.SCRIPTS -> showScripts
            AppTab.AUTOMATION -> showAutomation
            AppTab.PSA_WORKSPACE -> showPsa
            AppTab.SUPERUSER -> isSuperuser
        }
        if (!isCurrentTabVisible) {
            currentTab = when {
                showPsa -> AppTab.PSA_WORKSPACE
                showSuperuser || isSuperuser -> AppTab.SUPERUSER
                showDashboard -> AppTab.DASHBOARD
                showScripts -> AppTab.SCRIPTS
                showAutomation -> AppTab.AUTOMATION
                else -> AppTab.PSA_WORKSPACE
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = SlateBackground,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stylized compact Como On badge (ultralight, crisp vector, comfortable to the eye)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(1.dp, PurpleNeon.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_como_on_power_clean),
                                    contentDescription = "Como On",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "COMO ",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "ON",
                                    color = PurpleNeon,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Clean Crown button visible ONLY for superuser to switch to or return from Admin
                        if (isSuperuser) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                onClick = {
                                    if (currentTab == AppTab.SUPERUSER) {
                                        currentTab = AppTab.PSA_WORKSPACE
                                    } else {
                                        if (!roleMatrix.suSuperuserEnabled) {
                                            viewModel.saveRoleVisibilityConfig(roleMatrix.copy(suSuperuserEnabled = true))
                                        }
                                        currentTab = AppTab.SUPERUSER
                                    }
                                },
                                shape = CircleShape,
                                color = if (currentTab == AppTab.SUPERUSER) PurpleNeon.copy(alpha = 0.35f) else Color(0xFF1E1435),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (currentTab == AppTab.SUPERUSER) Color(0xFFFFD700) else PurpleNeon.copy(alpha = 0.65f)
                                ),
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("admin_badge_button")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "👑",
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsAndHelpDialog = true },
                        modifier = Modifier.testTag("settings_help_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración y Ayuda",
                            tint = if (fontScaleLevel != com.example.ui.theme.FontScaleLevel.NORMAL) PurpleNeon else TextSecondary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = TextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateCard
                )
            )
        },
        bottomBar = {
            val visibleCount = listOf(showDashboard, showScripts, showAutomation, showPsa, showSuperuser).count { it }
            if (visibleCount > 1) {
                NavigationBar(
                    containerColor = SlateCard,
                    contentColor = TextSecondary,
                    tonalElevation = 8.dp
                ) {
                    val navFontSize = when (fontScaleLevel) {
                        com.example.ui.theme.FontScaleLevel.ULTRA_GRANDE -> 8.5.sp
                        com.example.ui.theme.FontScaleLevel.CHICATO -> 9.5.sp
                        else -> 11.sp
                    }
                    if (showDashboard) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.DASHBOARD,
                            onClick = { currentTab = AppTab.DASHBOARD },
                            icon = { Icon(Icons.Default.Assessment, contentDescription = "Panel") },
                            label = { Text("Panel", fontSize = navFontSize, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = IndigoPrimary,
                                selectedTextColor = IndigoPrimary,
                                indicatorColor = IndigoPrimary.copy(alpha = 0.15f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_dashboard")
                        )
                    }

                    if (showScripts) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.SCRIPTS,
                            onClick = { currentTab = AppTab.SCRIPTS },
                            icon = { Icon(Icons.Default.Description, contentDescription = "Scripts") },
                            label = { Text("Scripts", fontSize = navFontSize, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = IndigoPrimary,
                                selectedTextColor = IndigoPrimary,
                                indicatorColor = IndigoPrimary.copy(alpha = 0.15f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_scripts")
                        )
                    }

                    if (showAutomation) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.AUTOMATION,
                            onClick = { currentTab = AppTab.AUTOMATION },
                            icon = { Icon(Icons.Default.Bolt, contentDescription = "Envíos") },
                            label = { Text("Envíos", fontSize = navFontSize, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = IndigoPrimary,
                                selectedTextColor = IndigoPrimary,
                                indicatorColor = IndigoPrimary.copy(alpha = 0.15f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_automation")
                        )
                    }

                    if (showPsa) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.PSA_WORKSPACE,
                            onClick = { currentTab = AppTab.PSA_WORKSPACE },
                            icon = { Icon(Icons.Default.WaterDrop, contentDescription = "PSA Agua") },
                            label = { Text("PSA", fontSize = navFontSize, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.example.ui.theme.PsaCyanGlow,
                                selectedTextColor = com.example.ui.theme.PsaCyanGlow,
                                indicatorColor = com.example.ui.theme.PsaCyanPrimary.copy(alpha = 0.2f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_psa_workspace")
                        )
                    }

                    if (showSuperuser) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.SUPERUSER,
                            onClick = { currentTab = AppTab.SUPERUSER },
                            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Superusuario") },
                            label = { Text("Admin", fontSize = navFontSize, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = IndigoPrimary,
                                selectedTextColor = IndigoPrimary,
                                indicatorColor = IndigoPrimary.copy(alpha = 0.15f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_superuser")
                        )
                    }
                }
            }
        }
    )
 { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Safety status banner for Kill Switch / Modo Pausa
            if (killSwitchTriggered || (isBankingMode && !isAccessibilityConnected)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RoseError.copy(alpha = 0.15f))
                        .border(1.dp, RoseError.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = RoseError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "🛡️ Modo Banco Seguro",
                                    color = RoseError,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Accesibilidad apagada.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        if (!isLargeFont) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.openAccessibilitySettings(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Reactivar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (isLargeFont) {
                        Button(
                            onClick = { viewModel.openAccessibilitySettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reactivar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (isAppPaused) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmberDelay.copy(alpha = 0.15f))
                        .border(1.dp, AmberDelay.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                tint = AmberDelay,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⏸️ Modo Pausa Activo",
                                color = AmberDelay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        if (!isLargeFont) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.setAppPaused(context, false) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Reanudar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (isLargeFont) {
                        Button(
                            onClick = { viewModel.setAppPaused(context, false) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reanudar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!isSuperuser && licenseEvaluation.isExpiringSoon && licenseEvaluation.remainingDays in 1L..3L) {
                val daysLeft = licenseEvaluation.remainingDays
                Surface(
                    color = AmberDelay.copy(alpha = 0.16f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberDelay.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("expiring_soon_banner")
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "⏳ Te ${if (daysLeft == 1L) "queda 1 día" else "quedan $daysLeft días"} de uso. Renueva a tiempo para no perder acceso.",
                            color = AmberDelay,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Button(
                            onClick = { showExpiringRenewDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("expiring_soon_renew_button")
                        ) {
                            Text("Renovar", fontSize = 11.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!hasSeenSetupGuide && currentTab != AppTab.DASHBOARD) {
                Surface(
                    color = IndigoPrimary.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("initial_welcome_banner")
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "👋 Guía inicial: ajusta letra, país y ayuda en ⚙️ arriba.",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = { showSettingsAndHelpDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 34.dp)
                            ) {
                                Text("Abrir ⚙️", fontSize = 11.sp, color = PurpleNeon, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.setSetupGuideDismissed(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 34.dp)
                            ) {
                                Text("Listo", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_switch"
                ) { tab ->
                    when (tab) {
                        AppTab.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToAutomation = { currentTab = AppTab.AUTOMATION }
                        )
                        AppTab.SCRIPTS -> ScriptsScreen(
                            viewModel = viewModel,
                            onSelectScriptForAutomation = { script ->
                                scriptForAutomation = script
                                currentTab = AppTab.AUTOMATION
                            }
                        )
                        AppTab.AUTOMATION -> AutomationScreen(
                            viewModel = viewModel,
                            preselectedScript = scriptForAutomation
                        )
                        AppTab.PSA_WORKSPACE -> com.example.ui.screens.psa.PsaWorkspaceScreen(
                            viewModel = viewModel,
                            onClose = { currentTab = AppTab.DASHBOARD }
                        )
                        AppTab.SUPERUSER -> SuperuserScreen(
                            viewModel = viewModel
                        )
                    }
                }

                // Floating lateral quick-access pull tab on the edge to open PSA workspace
                if (currentTab != AppTab.PSA_WORKSPACE) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                            .background(com.example.ui.theme.PsaCyanPrimary)
                            .border(1.dp, com.example.ui.theme.PsaCyanGlow, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                            .clickable { currentTab = AppTab.PSA_WORKSPACE }
                            .padding(horizontal = 5.dp, vertical = 10.dp)
                            .testTag("btn_psa_lateral_pull_tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Abrir PSA Purificadores",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "P\nS\nA",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                lineHeight = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showKillSwitchConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showKillSwitchConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = RoseError,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "🛡️ Activar Kill Switch (Bancos)",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "¿Deseas desactivar inmediatamente todos los servicios de accesibilidad, superposiciones y todas las notificaciones de Como On?\n\nAl activarlo, la aplicación se desactiva por completo en segundo plano para que puedas ingresar a Banco Nación (BNA+) y demás apps bancarias de forma 100% segura y sin advertencias de control remoto.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showKillSwitchConfirmDialog = false
                        val disabled = viewModel.executeKillSwitch(context)
                        if (disabled) {
                            Toast.makeText(
                                context,
                                "🛡️ Kill Switch: Accesibilidad y notificaciones apagadas. Seguro para Banco Nación.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                    modifier = Modifier.testTag("confirm_kill_switch_button")
                ) {
                    Text("APAGAR TODO", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showKillSwitchConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted)
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = SlateCard,
            tonalElevation = 6.dp
        )
    }

    if (showSettingsAndHelpDialog) {
        GeneralSettingsAndHelpDialog(
            context = context,
            currentFontLevel = fontScaleLevel,
            activeCountry = activePsaCountry,
            hasDismissedSetupGuide = hasSeenSetupGuide,
            isAppDistributionSignedIn = isAppDistributionSignedIn,
            onSelectFontLevel = { level ->
                viewModel.setFontScaleLevel(level)
            },
            onSelectCountry = { code ->
                viewModel.switchPsaCountry(context, code)
            },
            onShowSetupGuideAgain = {
                viewModel.setSetupGuideDismissed(false)
                currentTab = AppTab.DASHBOARD
            },
            onOpenAccessibilitySettings = {
                viewModel.openAccessibilitySettings(context)
            },
            onCheckAppDistributionUpdates = {
                viewModel.triggerFirebaseAppDistributionCheck()
            },
            onDismiss = { showSettingsAndHelpDialog = false }
        )
    }

    if (showExpiringRenewDialog) {
        ContactSuperuserDialog(
            user = user,
            lockReason = AccessLockReason.DAYS_EXPIRED,
            onDismiss = { showExpiringRenewDialog = false }
        )
    }

    val pendingPricingUpdate = pendingAdminPricingUpdate
    if (!isSuperuser && pendingPricingUpdate != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPendingAdminPricingUpdate(context) },
            title = {
                Text(
                    text = "🔄 Precios y Cuotas Actualizados (${pendingPricingUpdate.countryCode})",
                    color = com.example.ui.theme.PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                )
            },
            text = {
                Text(
                    text = "El Administrador actualizó la lista oficial de precios y porcentajes de financiación PSA para ${pendingPricingUpdate.countryCode}.\n\nComo editaste tus propios valores anteriormente, puedes aplicar los del Administrador o conservar tu configuración actual.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.acceptPendingAdminPricingUpdate(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("accept_admin_pricing_button")
                ) {
                    Text("Aplicar del Admin", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = { viewModel.dismissPendingAdminPricingUpdate(context) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("keep_custom_pricing_button")
                ) {
                    Text("Mantener Míos", color = TextSecondary)
                }
            },
            containerColor = SlateCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Mandatory password change ONLY for legacy offline accounts (not Google Sign-In)
    if (user.mustChangePassword && !isSuperuser && user.email.isBlank()) {
        val dialogContext = LocalContext.current
        FirstLoginChangePasswordDialog(
            user = user,
            onPasswordChanged = { newPassword ->
                viewModel.changeOperatorPassword(newPassword) { success, error ->
                    if (success) {
                        Toast.makeText(dialogContext, "¡Contraseña personal establecida correctamente!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(dialogContext, error ?: "Error al actualizar contraseña", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun FirstLoginChangePasswordDialog(
    user: UserAccount,
    onPasswordChanged: (newPassword: String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { /* Mandatory modal dialog: requires user action */ },
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9333EA).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFA855F7),
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = "Crea tu Contraseña Personal",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "¡Bienvenido, ${user.username}! Define tu contraseña personal.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = null
                    },
                    label = { Text("Nueva Contraseña") },
                    placeholder = { Text("Mínimo 4 caracteres") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Confirmar Nueva Contraseña") },
                    placeholder = { Text("Repite tu contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = RoseError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.trim().length < 4) {
                        errorMessage = "La contraseña debe tener al menos 4 caracteres."
                        return@Button
                    }
                    if (newPassword.trim() != confirmPassword.trim()) {
                        errorMessage = "Las contraseñas no coinciden."
                        return@Button
                    }
                    onPasswordChanged(newPassword.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar y Comenzar", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SlateCard,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
fun ContactSuperuserDialog(
    user: UserAccount,
    lockReason: AccessLockReason = AccessLockReason.ADMIN_BLOCKED,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val identityLabel = user.email.ifBlank { user.token }
    val contactMessage = when (lockReason) {
        AccessLockReason.DAYS_EXPIRED ->
            "Hola Administrador, se acabaron mis días de uso en Como On (usuario: ${user.username} • $identityLabel). ¿Podrías habilitarme más días para continuar?"
        AccessLockReason.OFFLINE_OVER_24H ->
            "Hola Administrador, mi cuenta (${user.username} • $identityLabel) solicita validación de conexión tras más de 24 horas sin red en Como On."
        AccessLockReason.REBOOT_VERIFICATION_REQUIRED ->
            "Hola Administrador, reinicié el teléfono y mi usuario (${user.username} • $identityLabel) requiere validar el acceso en Como On."
        AccessLockReason.CLOCK_TAMPER_DETECTED ->
            "Hola Administrador, mi usuario (${user.username} • $identityLabel) presenta una verificación de fecha/hora en Como On. ¿Podrías ayudarme a revisarlo?"
        AccessLockReason.ADMIN_BLOCKED, AccessLockReason.NONE ->
            "Hola Administrador, mi usuario (${user.username} • $identityLabel) se encuentra restringido en Como On. ¿Podrías revisar mi acceso?"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = PurpleNeon,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (lockReason == AccessLockReason.DAYS_EXPIRED) "Renovar Días de Uso" else "Contactar al Administrador",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (lockReason == AccessLockReason.DAYS_EXPIRED)
                        "Envía este mensaje por WhatsApp al Administrador para renovar tus días:"
                    else
                        "Envía un mensaje al Administrador para revisar tu acceso:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = contactMessage,
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val uri = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(contactMessage))
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, contactMessage)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Enviar mensaje al Administrador"))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("💬 Enviar por WhatsApp", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cerrar", color = TextSecondary)
            }
        },
        containerColor = SlateCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun BlockedAccountScreen(
    user: UserAccount,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    var showContactDialog by remember { mutableStateOf(false) }
    val reason = if (user.isBlocked) AccessLockReason.ADMIN_BLOCKED else AccessLockReason.DAYS_EXPIRED
    RestrictedAccessScreen(
        user = user,
        lockReason = reason,
        onRefresh = onRefresh,
        onContactSuperuser = { showContactDialog = true },
        onLogout = onLogout
    )
    if (showContactDialog) {
        ContactSuperuserDialog(
            user = user,
            lockReason = reason,
            onDismiss = { showContactDialog = false }
        )
    }
}
