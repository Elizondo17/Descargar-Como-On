package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessPreset
import com.example.data.model.BusinessProfile
import com.example.data.model.RoleVisibilityConfig
import com.example.data.model.TeamNotificationPriority
import com.example.data.model.TeamNotificationTargetMode
import com.example.data.model.UserAccount
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.components.SuperuserStatsDashboard
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

enum class AdminPrimaryTab {
    OPERATORS,
    TOKENS,
    PERFORMANCE,
    OFFERING,
    ASSISTANT
}

enum class AssistantSubTool(val label: String) {
    LIVE_RESPONSES("⚡ Respuestas en Vivo"),
    COACHING("🎯 Auditor de Cierres"),
    CRM_LEARNING("📊 Datos CRM"),
    PUSH_ALERTS("🚨 Alertas Push")
}

data class CreatedCredentials(
    val username: String,
    val key: String,
    val days: Int,
    val maxDevices: Int = 1
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuperuserScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLargeFont = com.example.ui.theme.LocalIsLargeFontMode.current
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val isCloudGoogleConnected by viewModel.isCloudGoogleConnected.collectAsState()
    val isNetworkOnline by viewModel.isNetworkOnline.collectAsState()
    val sellerPerformanceBoard by viewModel.sellerPerformanceBoard.collectAsState()
    val apkDownloadUrl by viewModel.apkDownloadUrl.collectAsState()
    val githubUsername by viewModel.githubUsername.collectAsState()
    val githubRepo by viewModel.githubRepo.collectAsState()
    val serverAppUpdateInfo by viewModel.serverAppUpdateInfo.collectAsState()
    val isCheckingAppUpdate by viewModel.isCheckingAppUpdate.collectAsState()
    val isAppDistributionSignedIn by viewModel.isAppDistributionTesterSignedIn.collectAsState()
    val appErrors by viewModel.appErrors.collectAsState()
    val isLoadingAppErrors by viewModel.isLoadingAppErrors.collectAsState()
    val pendingLocalErrorCount by viewModel.pendingLocalErrorCount.collectAsState()
    val appErrorsFeedback by viewModel.appErrorsFeedback.collectAsState()
    val cloudAccessTokens by viewModel.cloudAccessTokens.collectAsState()
    val isRefreshingAccessTokens by viewModel.isRefreshingAccessTokens.collectAsState()
    val accessTokenActionFeedback by viewModel.accessTokenActionFeedback.collectAsState()
    val adminUserActionFeedback by viewModel.adminUserActionFeedback.collectAsState()
    val teamNotificationFeedback by viewModel.teamNotificationFeedback.collectAsState()
    val mensajesGlobales by viewModel.mensajesGlobales.collectAsState()
    val businessProfile by viewModel.businessProfile.collectAsState()
    val allScannedChats by viewModel.allScannedChats.collectAsState()
    val isGlobalLegacyEnabled by viewModel.isGlobalLegacyEnabled.collectAsState()
    val roleMatrix by viewModel.roleVisibilityConfig.collectAsState()

    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val smartResponses by viewModel.smartResponses.collectAsState()
    val salesCoachingResult by viewModel.salesCoachingResult.collectAsState()
    val chatLearningInsight by viewModel.chatLearningInsight.collectAsState()
    val aiErrorMessage by viewModel.aiErrorMessage.collectAsState()

    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val isTestingApiKey by viewModel.isTestingApiKey.collectAsState()
    val apiKeyTestStatus by viewModel.apiKeyTestStatus.collectAsState()
    var geminiKeyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
    var isApiKeyObscured by remember { mutableStateOf(true) }
    var apkUrlInput by remember(apkDownloadUrl) { mutableStateOf(apkDownloadUrl) }
    var ghUserInput by remember(githubUsername) { mutableStateOf(githubUsername) }
    var ghRepoInput by remember(githubRepo) { mutableStateOf(githubRepo) }
    var isGithubGuideExpanded by remember { mutableStateOf(false) }
    var isPublishVersionExpanded by remember { mutableStateOf(false) }
    var publishVersionCodeInput by remember { mutableStateOf(com.example.BuildConfig.VERSION_CODE.toString()) }
    var publishVersionNameInput by remember { mutableStateOf(com.example.BuildConfig.VERSION_NAME) }
    var publishApkUrlInput by remember(apkDownloadUrl, serverAppUpdateInfo) {
        mutableStateOf(serverAppUpdateInfo?.apkDownloadUrl?.takeIf { it.isNotBlank() } ?: apkDownloadUrl)
    }
    var publishReleaseNotesInput by remember(serverAppUpdateInfo) {
        mutableStateOf(serverAppUpdateInfo?.releaseNotes.orEmpty())
    }
    var publishForceUpdate by remember(serverAppUpdateInfo) {
        mutableStateOf(serverAppUpdateInfo?.forceUpdate ?: false)
    }
    var isPreparingApkShare by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(appErrorsFeedback) {
        val msg = appErrorsFeedback
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearAppErrorsFeedback()
        }
    }

    androidx.compose.runtime.LaunchedEffect(accessTokenActionFeedback) {
        val msg = accessTokenActionFeedback
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearAccessTokenFeedback()
        }
    }

    androidx.compose.runtime.LaunchedEffect(adminUserActionFeedback) {
        val msg = adminUserActionFeedback
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearAdminUserActionFeedback()
        }
    }

    androidx.compose.runtime.LaunchedEffect(teamNotificationFeedback) {
        val msg = teamNotificationFeedback
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearTeamNotificationFeedback()
        }
    }

    fun launchDirectApkFileShare() {
        if (isPreparingApkShare) return
        isPreparingApkShare = true
        coroutineScope.launch {
            val prepResult = withContext(Dispatchers.IO) {
                com.example.util.PsaApkShareHelper.prepareShareableApkFile(context)
            }
            isPreparingApkShare = false
            prepResult.onSuccess { apkFile ->
                try {
                    val shareIntent = com.example.util.PsaApkShareHelper.buildApkShareChooserIntent(context, apkFile)
                    context.startActivity(shareIntent)
                } catch (_: Exception) {
                    Toast.makeText(context, "No se pudo abrir la hoja para compartir el APK", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { err ->
                Toast.makeText(context, err.message ?: "Error al preparar el archivo APK", Toast.LENGTH_LONG).show()
            }
        }
    }

    var activePrimaryTab by remember { mutableStateOf(AdminPrimaryTab.OPERATORS) }
    var activeAssistantSubTool by remember { mutableStateOf(AssistantSubTool.LIVE_RESPONSES) }
    var isCreateOperatorExpanded by remember { mutableStateOf(false) }
    var isRoleMatrixExpanded by remember { mutableStateOf(false) }

    // User management state
    var newOperatorName by remember { mutableStateOf("") }
    var newOperatorEmail by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(30) }
    var selectedMaxDevices by remember { mutableStateOf(1) }
    var newOperatorNotes by remember { mutableStateOf("") }
    var newlyCreatedCredentials by remember { mutableStateOf<CreatedCredentials?>(null) }
    var editingUser by remember { mutableStateOf<UserAccount?>(null) }

    // Business profile form state (initialized from businessProfile)
    var editBusinessName by remember(businessProfile) { mutableStateOf(businessProfile.businessName) }
    var editBusinessType by remember(businessProfile) { mutableStateOf(businessProfile.businessType) }
    var editOffering by remember(businessProfile) { mutableStateOf(businessProfile.offeringDescription) }
    var editTone by remember(businessProfile) { mutableStateOf(businessProfile.toneAndStyle) }
    var editGoal by remember(businessProfile) { mutableStateOf(businessProfile.salesObjective) }
    var editFaq by remember(businessProfile) { mutableStateOf(businessProfile.frequentQuestionsAndObjections) }

    // AI testing input states
    var clientTestMessage by remember { mutableStateOf("") }
    var coachingConversationText by remember { mutableStateOf("") }

    // Presets & AI Creator Wizard states
    val allPresets by viewModel.allPresets.collectAsState()
    var userRawIdea by remember { mutableStateOf("") }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetTitle by remember { mutableStateOf("") }

    val secondaryUsers = allUsers.filter { it.role != "SUPERUSER" }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // Security gate: only authorized Superuser (leonelizondo16@gmail.com) can access this view
    val isAuthorizedMasterSuperuser = currentUser?.role == "SUPERUSER" && (
        currentUser?.email.isNullOrBlank() ||
            currentUser?.email.equals(com.example.data.repository.FirestoreTeamRepository.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)
        )
    if (!isAuthorizedMasterSuperuser) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SlateBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = RoseError,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Acceso Restringido",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Solo la cuenta Superusuario (${com.example.data.repository.FirestoreTeamRepository.BOOTSTRAP_ADMIN_EMAIL}) puede acceder al centro de administración.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Clean Admin Master Banner (Sleek, uncluttered, professional)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SlateCard)
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PurpleNeon.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👑",
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Panel Superusuario",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EmeraldSuccess.copy(alpha = 0.18f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ÚNICO ADMIN",
                                    color = EmeraldSuccess,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "${com.example.data.repository.FirestoreTeamRepository.BOOTSTRAP_ADMIN_EMAIL} • Google Sign-In",
                            color = com.example.ui.theme.PsaCyanGlow,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Control de operadores, licencias por días y configuración comercial",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Informative Software Version & Device Validation Banner
        item {
            val deviceLabel = remember {
                val brand = android.os.Build.MANUFACTURER.orEmpty()
                    .trim()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                val model = android.os.Build.MODEL.orEmpty().trim()
                val baseName = if (model.startsWith(brand, ignoreCase = true)) model else "$brand $model".trim()
                "${baseName.ifBlank { "Dispositivo Android" }} • Android ${android.os.Build.VERSION.RELEASE}"
            }
            val versionSummary = "Como On v${com.example.BuildConfig.VERSION_NAME} (Compilación ${com.example.BuildConfig.VERSION_CODE})"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(com.example.ui.theme.PsaCyanPrimary.copy(alpha = 0.10f))
                    .border(1.dp, com.example.ui.theme.PsaCyanGlow.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("admin_software_version_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(com.example.ui.theme.PsaCyanGlow.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Versión de software",
                            tint = com.example.ui.theme.PsaCyanGlow,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Versión v${com.example.BuildConfig.VERSION_NAME} • Comp. ${com.example.BuildConfig.VERSION_CODE}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isNetworkOnline) EmeraldSuccess.copy(alpha = 0.18f)
                                        else AmberDelay.copy(alpha = 0.18f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isNetworkOnline) "EN LÍNEA" else "SIN RED",
                                    color = if (isNetworkOnline) EmeraldSuccess else AmberDelay,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = deviceLabel,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        val cloudVer = serverAppUpdateInfo
                        if (cloudVer != null) {
                            val hasNewerOnServer = cloudVer.latestVersionCode > com.example.BuildConfig.VERSION_CODE
                            Text(
                                text = if (hasNewerOnServer) {
                                    "🚀 Nueva en servidor: v${cloudVer.latestVersionName} (Comp. ${cloudVer.latestVersionCode})"
                                } else {
                                    "☁️ Servidor al día: v${cloudVer.latestVersionName} (Comp. ${cloudVer.latestVersionCode})"
                                },
                                color = if (hasNewerOnServer) AmberDelay else EmeraldSuccess,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.checkForAppUpdate(isAutomaticStartup = false) },
                        enabled = !isCheckingAppUpdate,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("admin_btn_check_server_version")
                    ) {
                        if (isCheckingAppUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = com.example.ui.theme.PsaCyanGlow,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Verificar actualización en servidor",
                                tint = com.example.ui.theme.PsaCyanGlow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val payload = "$versionSummary | $deviceLabel | Estado: ${if (isNetworkOnline) "En línea" else "Sin red"}"
                            clipboard.setPrimaryClip(ClipData.newPlainText("Versión Como On", payload))
                            Toast.makeText(context, "Estado copiado: $versionSummary", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("admin_copy_version_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar versión del software",
                            tint = com.example.ui.theme.PsaCyanGlow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Clean Tab Segmented Selector (Uncluttered, adaptive up to 160%)
        item {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateCard)
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(12.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    AdminPrimaryTab.OPERATORS to "👥 Operadores",
                    AdminPrimaryTab.TOKENS to "🔑 Tokens",
                    AdminPrimaryTab.PERFORMANCE to "📊 Rendimiento",
                    AdminPrimaryTab.OFFERING to "💼 Mi Oferta",
                    AdminPrimaryTab.ASSISTANT to "⚡ Asistente"
                ).forEach { (tab, label) ->
                    val isSelected = activePrimaryTab == tab
                    Box(
                        modifier = Modifier
                            .let { if (isLargeFont) it.fillMaxWidth() else it }
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PurpleNeon else SlateBackground)
                            .border(
                                1.dp,
                                if (isSelected) PurpleNeon else SlateCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                activePrimaryTab = tab
                                if (tab == AdminPrimaryTab.PERFORMANCE && isCloudGoogleConnected) {
                                    viewModel.refreshSellerPerformanceBoard()
                                } else if (tab == AdminPrimaryTab.TOKENS && isCloudGoogleConnected) {
                                    viewModel.refreshCloudAccessTokens()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .testTag("admin_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (!isCloudGoogleConnected) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AmberDelay.copy(alpha = 0.14f))
                        .border(1.dp, AmberDelay.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                        .testTag("admin_google_session_banner")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AmberDelay, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sesión de Google requerida para la nube",
                                color = AmberDelay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                        Text(
                            text = "Para crear vendedores en Firestore y ver métricas en tiempo real sin error de permisos, vincula tu cuenta Google administradora (${com.example.data.repository.FirestoreTeamRepository.BOOTSTRAP_ADMIN_EMAIL}).",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = {
                                viewModel.signInWithGoogle(context) {
                                    Toast.makeText(context, "✅ Sesión Google conectada a la nube", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberDelay),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("admin_btn_connect_google")
                        ) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Conectar con Google", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Global Error Alert for AI actions if any
        aiErrorMessage?.let { errorMsg ->
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RoseError.copy(alpha = 0.15f))
                        .border(1.dp, RoseError.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = RoseError, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMsg,
                                color = RoseError,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearAiState() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Cerrar", tint = RoseError, modifier = Modifier.size(16.dp))
                            }
                        }
                        if (errorMsg.contains("Clave API de Gemini")) {
                            Button(
                                onClick = { activePrimaryTab = AdminPrimaryTab.OFFERING },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Configurar Clave API de Gemini", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // TAB 1: OPERADORES & PERMISOS
        // ==========================================
        if (activePrimaryTab == AdminPrimaryTab.OPERATORS) {
            // Summary KPI Stats Row (stacks nicely on large font)
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateCard)
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${secondaryUsers.size}", fontSize = 17.sp, fontWeight = FontWeight.Black, color = IndigoPrimary)
                            Text(text = "Total Operadores", fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateCard)
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${secondaryUsers.count { !it.isBlocked && !it.isExpired }}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldSuccess
                            )
                            Text(text = "Activos", fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateCard)
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${secondaryUsers.count { it.isBlocked || it.isExpired }}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = if (secondaryUsers.any { it.isBlocked || it.isExpired }) RoseError else TextMuted
                            )
                            Text(text = "Inactivos", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Quick Interactive Action Buttons (Nuevo Operador / Permisos)
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isCreateOperatorExpanded = !isCreateOperatorExpanded },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCreateOperatorExpanded) PurpleNeon else IndigoPrimary
                        ),
                        modifier = Modifier
                            .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                            .heightIn(min = 42.dp)
                    ) {
                        Icon(imageVector = if (isCreateOperatorExpanded) Icons.Default.Remove else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isCreateOperatorExpanded) "Ocultar Formulario" else "+ Nuevo Operador", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { isRoleMatrixExpanded = !isRoleMatrixExpanded },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                            .heightIn(min = 42.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRoleMatrixExpanded) "Ocultar Permisos" else "🎛️ Permisos", fontSize = 12.sp, color = TextPrimary)
                    }
                }
            }

            // Expandable Role Visibility Matrix
            if (isRoleMatrixExpanded) {
                roleMatrixTab(
                    roleMatrix = roleMatrix,
                    onSaveMatrix = { updated ->
                        viewModel.saveRoleVisibilityConfig(updated)
                        Toast.makeText(context, "✅ Configuración de roles guardada y sincronizada", Toast.LENGTH_SHORT).show()
                    },
                    onSyncNow = {
                        viewModel.syncAllUsersFromCloud()
                        Toast.makeText(context, "🔄 Sincronizando con la nube...", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Expandable Form to create cloud Google-authorized operator
            if (isCreateOperatorExpanded || secondaryUsers.isEmpty()) {
                item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateCard)
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Habilitar Usuario por Email de Google", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = newOperatorName,
                            onValueChange = { newOperatorName = it },
                            label = { Text("Nombre Completo / Alias") },
                            placeholder = { Text("Ej: Carlos Ventas") },
                            singleLine = true,
                            leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = IndigoPrimary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("new_user_name_input")
                        )

                        OutlinedTextField(
                            value = newOperatorEmail,
                            onValueChange = { newOperatorEmail = it },
                            label = { Text("Email de Google Autorizado") },
                            placeholder = { Text("ejemplo@gmail.com") },
                            singleLine = true,
                            leadingIcon = { Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = EmeraldSuccess) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("new_user_email_input")
                        )

                        Text(
                            text = "Vigencia de Acceso (validada en servidor de la nube):",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(7, 15, 30, 60, 90).forEach { days ->
                                FilterChip(
                                    selected = selectedDays == days,
                                    onClick = { selectedDays = days },
                                    label = { Text("$days días", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IndigoPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Dispositivos Permitidos (por defecto 1 teléfono; amplía en casos especiales):",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                (1..5).forEach { count ->
                                    FilterChip(
                                        selected = selectedMaxDevices == count,
                                        onClick = { selectedMaxDevices = count },
                                        label = {
                                            Text(
                                                text = if (count == 1) "1 equipo (Estándar)" else "$count equipos",
                                                fontSize = 12.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldSuccess,
                                            selectedLabelColor = Color.Black
                                        ),
                                        modifier = Modifier.testTag("new_user_max_devices_$count")
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = newOperatorNotes,
                            onValueChange = { newOperatorNotes = it },
                            label = { Text("Notas / Zona (opcional)") },
                            placeholder = { Text("Ej: Zona Norte • Celular + Tablet") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (!isCloudGoogleConnected) {
                                    Toast.makeText(context, "Conecta tu cuenta Google arriba para crear vendedores en la nube", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                val cleanEmail = newOperatorEmail.trim().lowercase()
                                if (newOperatorName.isBlank() || cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                                    Toast.makeText(context, "Ingresa nombre y un email de Google válido", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.createAuthorizedGoogleUser(
                                    email = cleanEmail,
                                    name = newOperatorName,
                                    days = selectedDays,
                                    notes = newOperatorNotes,
                                    maxDevices = selectedMaxDevices,
                                    onSuccess = { createdUser ->
                                        val creds = CreatedCredentials(
                                            username = createdUser.username,
                                            key = createdUser.email,
                                            days = createdUser.daysGranted,
                                            maxDevices = createdUser.maxDevices
                                        )
                                        newlyCreatedCredentials = creds

                                        val effectiveInviteUrl = com.example.data.repository.EzoRepository.buildGithubDirectApkUrl(
                                            ghUserInput,
                                            ghRepoInput
                                        ).ifBlank {
                                            com.example.data.repository.EzoRepository.normalizeApkDownloadUrl(apkUrlInput)
                                        }
                                        val shareText = com.example.data.repository.EzoRepository.buildOperatorInviteMessage(
                                            username = creds.username,
                                            email = creds.key,
                                            days = creds.days,
                                            maxDevices = creds.maxDevices,
                                            apkDownloadUrl = effectiveInviteUrl
                                        )
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Acceso Como On", shareText))

                                        Toast.makeText(context, "Usuario guardado en la nube Firestore", Toast.LENGTH_LONG).show()
                                        newOperatorName = ""
                                        newOperatorEmail = ""
                                        newOperatorNotes = ""
                                        selectedMaxDevices = 1
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = isCloudGoogleConnected,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("create_user_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCloudGoogleConnected) "Autorizar en Nube (Firestore)" else "Requiere sesión Google arriba",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        newlyCreatedCredentials?.let { creds ->
                            val effectiveInviteUrl = com.example.data.repository.EzoRepository.buildGithubDirectApkUrl(
                                ghUserInput,
                                ghRepoInput
                            ).ifBlank {
                                com.example.data.repository.EzoRepository.normalizeApkDownloadUrl(apkUrlInput)
                            }
                            val inviteText = com.example.data.repository.EzoRepository.buildOperatorInviteMessage(
                                username = creds.username,
                                email = creds.key,
                                days = creds.days,
                                maxDevices = creds.maxDevices,
                                apkDownloadUrl = effectiveInviteUrl
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldSuccess.copy(alpha = 0.12f))
                                    .border(1.dp, EmeraldSuccess.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Acceso Habilitado en Firebase Firestore:",
                                            color = EmeraldSuccess,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = buildString {
                                            append("Nombre: ${creds.username}\n")
                                            append("Email Google: ${creds.key}\n")
                                            append("Vigencia: ${creds.days} días • ${creds.maxDevices} equipo(s)\n")
                                            if (effectiveInviteUrl.isNotBlank()) {
                                                append("Link APK: $effectiveInviteUrl")
                                            } else {
                                                append("Instalador: Envío directo del archivo APK")
                                            }
                                        },
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp
                                    )
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Acceso Como On", inviteText))
                                                Toast.makeText(context, "Invitación copiada", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                                .heightIn(min = 48.dp)
                                                .testTag("admin_btn_copy_invite"),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp), tint = EmeraldSuccess)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copiar Invitación", color = EmeraldSuccess, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                shareMessageViaWhatsApp(context, inviteText)
                                            },
                                            modifier = Modifier
                                                .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                                .heightIn(min = 48.dp)
                                                .testTag("admin_btn_whatsapp_invite"),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                                        ) {
                                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Black)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Enviar Invitación", color = Color.Black, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { launchDirectApkFileShare() },
                                            enabled = !isPreparingApkShare,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag("admin_btn_attach_apk_invite"),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(15.dp), tint = com.example.ui.theme.PsaCyanGlow)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Adjuntar Archivo APK (WhatsApp / Drive)", color = com.example.ui.theme.PsaCyanGlow, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // Free & Automatic APK Distribution (GitHub Releases/Pages + Direct WhatsApp APK)
            item {
                val autoGithubApkUrl = com.example.data.repository.EzoRepository.buildGithubDirectApkUrl(ghUserInput, ghRepoInput)
                val autoGithubWebUrl = com.example.data.repository.EzoRepository.buildGithubPagesUrl(ghUserInput, ghRepoInput)
                val effectiveDownloadUrl = autoGithubApkUrl.ifBlank {
                    com.example.data.repository.EzoRepository.normalizeApkDownloadUrl(apkUrlInput)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SlateCard)
                        .border(1.dp, com.example.ui.theme.PsaCyanGlow.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = com.example.ui.theme.PsaCyanGlow,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Distribución Gratis y Automática (GitHub / WhatsApp)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Envía el archivo APK directo por WhatsApp o configura tu GitHub para generar un link permanente gratis",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { launchDirectApkFileShare() },
                            enabled = !isPreparingApkShare,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("admin_btn_share_apk_file")
                        ) {
                            if (isPreparingApkShare) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Preparando ComoOn-PSA.apk...", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Compartir Archivo APK por WhatsApp", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = ghUserInput,
                                onValueChange = { ghUserInput = it },
                                label = { Text("Usuario GitHub") },
                                placeholder = { Text("leonelizondo16") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.ui.theme.PsaCyanGlow,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                    .testTag("admin_gh_user_input")
                            )

                            OutlinedTextField(
                                value = ghRepoInput,
                                onValueChange = { ghRepoInput = it },
                                label = { Text("Repositorio") },
                                placeholder = { Text("como-on-psa") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.ui.theme.PsaCyanGlow,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                    .testTag("admin_gh_repo_input")
                            )
                        }

                        if (autoGithubApkUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldSuccess.copy(alpha = 0.10f))
                                    .border(1.dp, EmeraldSuccess.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "✔ Descarga directa automática lista:",
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                    Text(
                                        text = autoGithubApkUrl,
                                        color = TextPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = apkUrlInput,
                                onValueChange = { apkUrlInput = it },
                                label = { Text("O enlace manual alternativo (opcional)") },
                                placeholder = { Text("https://...") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = com.example.ui.theme.PsaCyanGlow
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.ui.theme.PsaCyanGlow,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_apk_url_input")
                            )
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.saveGithubDistributionConfig(ghUserInput, ghRepoInput)
                                    val updatedAuto = com.example.data.repository.EzoRepository.buildGithubDirectApkUrl(ghUserInput, ghRepoInput)
                                    if (updatedAuto.isNotBlank()) {
                                        apkUrlInput = updatedAuto
                                        Toast.makeText(context, "✅ GitHub guardado y link automático listo", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.saveApkDownloadUrl(apkUrlInput)
                                        apkUrlInput = com.example.data.repository.EzoRepository.normalizeApkDownloadUrl(apkUrlInput)
                                        Toast.makeText(context, "✅ Configuración APK guardada", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_btn_save_apk_url")
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp), tint = com.example.ui.theme.PsaCyanGlow)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Guardar Config", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    viewModel.saveGithubDistributionConfig(ghUserInput, ghRepoInput)
                                    if (autoGithubApkUrl.isNotBlank()) {
                                        apkUrlInput = autoGithubApkUrl
                                    } else {
                                        viewModel.saveApkDownloadUrl(apkUrlInput)
                                    }
                                    val shareMsg = com.example.data.repository.EzoRepository.buildApkOnlyShareMessage(effectiveDownloadUrl)
                                    shareMessageViaWhatsApp(context, shareMsg)
                                },
                                enabled = effectiveDownloadUrl.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                modifier = Modifier
                                    .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1.3f) }
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_btn_share_apk_whatsapp")
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enviar Link por WhatsApp", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (autoGithubWebUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Web Como On PSA", autoGithubWebUrl))
                                    Toast.makeText(context, "Web pública copiada: $autoGithubWebUrl", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_btn_copy_gh_web")
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = IndigoPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar Web Pública ($autoGithubWebUrl)", color = TextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Firebase App Distribution automatic tester notifications
                        OutlinedButton(
                            onClick = { viewModel.triggerFirebaseAppDistributionCheck() },
                            enabled = !isCheckingAppUpdate,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("admin_btn_firebase_app_distribution")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = EmeraldSuccess
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAppDistributionSignedIn) {
                                    "🔥 Firebase App Distribution Activo • Verificar"
                                } else {
                                    "🔥 Activar Firebase App Distribution (Grupo vendedores-psa)"
                                },
                                color = EmeraldSuccess,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Cloud version announcement panel (controls startup dialog on all devices)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateBackground.copy(alpha = 0.65f))
                                .border(1.dp, IndigoPrimary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clickable {
                                            if (!isPublishVersionExpanded && publishApkUrlInput.isBlank() && effectiveDownloadUrl.isNotBlank()) {
                                                publishApkUrlInput = effectiveDownloadUrl
                                            }
                                            isPublishVersionExpanded = !isPublishVersionExpanded
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("admin_btn_toggle_publish_version"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = com.example.ui.theme.PsaCyanGlow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "📢 Publicar Aviso de Nueva APK al Iniciar App",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (isPublishVersionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isPublishVersionExpanded) "Ocultar publicación" else "Mostrar publicación",
                                        tint = TextSecondary
                                    )
                                }

                                AnimatedVisibility(visible = isPublishVersionExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Publica aquí la última versión disponible (versionCode) para que a todo el equipo le aparezca el aviso opcional de descarga al abrir la app.",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = publishVersionCodeInput,
                                                onValueChange = { input ->
                                                    publishVersionCodeInput = input.filter { it.isDigit() }.take(6)
                                                },
                                                label = { Text("versionCode (ej: 3)") },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = com.example.ui.theme.PsaCyanGlow,
                                                    unfocusedBorderColor = SlateCardBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary
                                                ),
                                                modifier = Modifier
                                                    .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                                    .testTag("admin_publish_version_code_input")
                                            )
                                            OutlinedTextField(
                                                value = publishVersionNameInput,
                                                onValueChange = { publishVersionNameInput = it.take(40) },
                                                label = { Text("versionName (ej: 1.2)") },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = com.example.ui.theme.PsaCyanGlow,
                                                    unfocusedBorderColor = SlateCardBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary
                                                ),
                                                modifier = Modifier
                                                    .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                                    .testTag("admin_publish_version_name_input")
                                            )
                                        }
                                        OutlinedTextField(
                                            value = publishApkUrlInput,
                                            onValueChange = { publishApkUrlInput = it.take(500) },
                                            label = { Text("URL HTTPS de descarga del APK") },
                                            placeholder = { Text("https://github.com/.../ComoOn-PSA.apk") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = com.example.ui.theme.PsaCyanGlow,
                                                unfocusedBorderColor = SlateCardBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admin_publish_apk_url_input")
                                        )
                                        OutlinedTextField(
                                            value = publishReleaseNotesInput,
                                            onValueChange = { publishReleaseNotesInput = it.take(1000) },
                                            label = { Text("Novedades (opcional)") },
                                            placeholder = { Text("Precios actualizados y mejoras de estabilidad.") },
                                            maxLines = 3,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = com.example.ui.theme.PsaCyanGlow,
                                                unfocusedBorderColor = SlateCardBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admin_publish_release_notes_input")
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (publishForceUpdate) RoseError.copy(alpha = 0.14f) else EmeraldSuccess.copy(alpha = 0.12f))
                                                .border(
                                                    1.dp,
                                                    if (publishForceUpdate) RoseError.copy(alpha = 0.45f) else EmeraldSuccess.copy(alpha = 0.4f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (publishForceUpdate) {
                                                        "🔒 Bloqueo Global Obligatorio"
                                                    } else {
                                                        "📢 1° Aviso Opcional (Recomendado)"
                                                    },
                                                    color = if (publishForceUpdate) RoseError else EmeraldSuccess,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp
                                                )
                                                Text(
                                                    text = if (publishForceUpdate) {
                                                        "Exige actualizar a la nueva APK a todos los vendedores."
                                                    } else {
                                                        "Muestra el aviso con el enlace APK y permite 'Más tarde'."
                                                    },
                                                    color = TextSecondary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Switch(
                                                checked = publishForceUpdate,
                                                onCheckedChange = { publishForceUpdate = it },
                                                modifier = Modifier.testTag("admin_switch_publish_force_update")
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                val parsedCode = publishVersionCodeInput.trim().toIntOrNull() ?: 0
                                                val targetUrl = publishApkUrlInput.ifBlank { effectiveDownloadUrl }
                                                viewModel.publishAppVersionToServer(
                                                    versionCode = parsedCode,
                                                    versionName = publishVersionNameInput,
                                                    apkDownloadUrl = targetUrl,
                                                    releaseNotes = publishReleaseNotesInput,
                                                    forceUpdate = publishForceUpdate
                                                )
                                            },
                                            enabled = !isCheckingAppUpdate,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (publishForceUpdate) RoseError else IndigoPrimary
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag("admin_btn_publish_app_version")
                                        ) {
                                            Text(
                                                text = when {
                                                    isCheckingAppUpdate -> "Publicando..."
                                                    publishForceUpdate -> "Publicar Versión + Exigir Actualización"
                                                    else -> "Publicar Aviso de Versión en la Nube"
                                                },
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Collapsible 3-step quick guide
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateBackground.copy(alpha = 0.6f))
                                .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clickable { isGithubGuideExpanded = !isGithubGuideExpanded }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("admin_btn_toggle_gh_guide"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = AmberDelay,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "📘 Cómo activar tu link gratis en 3 pasos",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (isGithubGuideExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isGithubGuideExpanded) "Ocultar guía" else "Ver guía",
                                        tint = TextSecondary
                                    )
                                }

                                AnimatedVisibility(visible = isGithubGuideExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "1. En AI Studio tocá 'Push to GitHub' y creá un repositorio público gratuito (ej: como-on-psa).",
                                            color = TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                        Text(
                                            text = "2. GitHub compila el APK solo en ~3 min (pestaña Actions) y activa el link directo sin configuraciones extra.",
                                            color = TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                        Text(
                                            text = "3. Escribí arriba tu Usuario y Repositorio, tocá Guardar Config y enviá el link o el archivo por WhatsApp.",
                                            color = TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Superuser Global & Individual Notification Sender
            item {
                var isAnnounceExpanded by remember { mutableStateOf(false) }
                var announceTargetMode by remember { mutableStateOf(TeamNotificationTargetMode.ALL) }
                var announceTargetEmail by remember { mutableStateOf("") }
                var announcePriority by remember { mutableStateOf(TeamNotificationPriority.IMPORTANT) }
                var announceTitle by remember { mutableStateOf("") }
                var announceBody by remember { mutableStateOf("") }
                var sellerPickerExpanded by remember { mutableStateOf(false) }
                val candidateSellers = remember(secondaryUsers) {
                    secondaryUsers.filter { it.email.isNotBlank() }.sortedBy { it.username.lowercase() }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateCard)
                        .border(1.dp, com.example.ui.theme.PsaCyanPrimary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                        .testTag("admin_send_announcement_card"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAnnounceExpanded = !isAnnounceExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = com.example.ui.theme.PsaCyanGlow,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "📢 Enviar Aviso / Banner al Equipo",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Muestra banner en pantalla, campanita y notificación del teléfono",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isAnnounceExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isAnnounceExpanded) "Contraer avisos" else "Expandir avisos",
                            tint = TextSecondary
                        )
                    }

                    AnimatedVisibility(visible = isAnnounceExpanded) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = announceTargetMode == TeamNotificationTargetMode.ALL,
                                    onClick = { announceTargetMode = TeamNotificationTargetMode.ALL },
                                    label = { Text("🌐 Global (Todos)", fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = com.example.ui.theme.PsaCyanPrimary,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                                FilterChip(
                                    selected = announceTargetMode == TeamNotificationTargetMode.USER,
                                    onClick = { announceTargetMode = TeamNotificationTargetMode.USER },
                                    label = { Text("👤 Individual (1 vendedor)", fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PurpleNeon,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }

                            if (announceTargetMode == TeamNotificationTargetMode.USER) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { sellerPickerExpanded = true },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val selectedSeller = candidateSellers.firstOrNull {
                                            it.email.equals(announceTargetEmail, ignoreCase = true)
                                        }
                                        Text(
                                            text = if (selectedSeller != null) {
                                                "Para: ${selectedSeller.username} (${selectedSeller.email})"
                                            } else {
                                                "Seleccionar vendedor..."
                                            },
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = sellerPickerExpanded,
                                        onDismissRequest = { sellerPickerExpanded = false },
                                        modifier = Modifier.background(SlateCard)
                                    ) {
                                        candidateSellers.forEach { seller ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "${seller.username} (${seller.email})",
                                                        color = TextPrimary,
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                onClick = {
                                                    announceTargetEmail = seller.email
                                                    sellerPickerExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TeamNotificationPriority.entries.forEach { prio ->
                                    val prioColor = when (prio) {
                                        TeamNotificationPriority.URGENT -> RoseError
                                        TeamNotificationPriority.IMPORTANT -> AmberDelay
                                        TeamNotificationPriority.INFO -> IndigoPrimary
                                    }
                                    FilterChip(
                                        selected = announcePriority == prio,
                                        onClick = { announcePriority = prio },
                                        label = { Text(prio.label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = prioColor,
                                            selectedLabelColor = if (prio == TeamNotificationPriority.IMPORTANT) Color.Black else Color.White
                                        )
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = announceTitle,
                                onValueChange = { announceTitle = it },
                                label = { Text("Título del aviso", fontSize = 12.sp) },
                                placeholder = { Text("Ej: Meta semanal o Aviso importante", fontSize = 11.5.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.ui.theme.PsaCyanGlow,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_announce_title_input")
                            )

                            OutlinedTextField(
                                value = announceBody,
                                onValueChange = { announceBody = it },
                                label = { Text("Mensaje", fontSize = 12.sp) },
                                placeholder = { Text("Escribe el aviso que verán como banner y notificación...", fontSize = 11.5.sp) },
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.ui.theme.PsaCyanGlow,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_announce_body_input")
                            )

                            Button(
                                onClick = {
                                    if (announceTargetMode == TeamNotificationTargetMode.USER && announceTargetEmail.isBlank()) {
                                        Toast.makeText(context, "Selecciona un vendedor destinatario.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.sendSuperuserNotification(
                                        targetMode = announceTargetMode,
                                        targetEmail = announceTargetEmail,
                                        priority = announcePriority,
                                        title = announceTitle,
                                        body = announceBody
                                    )
                                    announceTitle = ""
                                    announceBody = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.PsaCyanPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_btn_send_announcement")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (announceTargetMode == TeamNotificationTargetMode.ALL) {
                                        "Enviar Aviso Global"
                                    } else {
                                        "Enviar Aviso Individual"
                                    },
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }

                            if (mensajesGlobales.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Banners Globales en Pantalla (${mensajesGlobales.count { it.activo }} activos)",
                                    color = com.example.ui.theme.PsaCyanGlow,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                mensajesGlobales.take(5).forEach { msg ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.28f))
                                            .border(
                                                1.dp,
                                                if (msg.activo) com.example.ui.theme.PsaCyanGlow.copy(alpha = 0.4f) else SlateCardBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "${if (msg.activo) "🟢" else "⏸️"} ${msg.titulo}",
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = msg.mensaje,
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 2
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(
                                                onClick = {
                                                    viewModel.toggleMensajeGlobalActivo(msg.id, !msg.activo)
                                                },
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) {
                                                Text(
                                                    text = if (msg.activo) "Desactivar" else "Activar",
                                                    color = if (msg.activo) AmberDelay else EmeraldSuccess,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteMensajeGlobal(msg.id) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Eliminar banner global",
                                                    tint = RoseError,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Master Control for Legacy vs Exclusive PSA
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isGlobalLegacyEnabled) IndigoPrimary.copy(alpha = 0.15f) else com.example.ui.theme.PsaCyanPrimary.copy(alpha = 0.12f))
                        .border(
                            1.dp,
                            if (isGlobalLegacyEnabled) IndigoPrimary.copy(alpha = 0.5f) else com.example.ui.theme.PsaCyanGlow.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isGlobalLegacyEnabled) Icons.Default.CheckCircle else Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isGlobalLegacyEnabled) IndigoPrimary else com.example.ui.theme.PsaCyanGlow,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Control Maestro de Versión",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isGlobalLegacyEnabled)
                                    "🌐 Acceso Global Activado: Todos los operadores tienen habilitados los módulos anteriores (EzoCRM, Automatizaciones)."
                                else
                                    "💧 Modo Restricción Activo: Todos los operadores están en Modo Exclusivo PSA, salvo excepciones individuales habilitadas abajo.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = isGlobalLegacyEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setGlobalLegacyEnabled(enabled)
                                Toast.makeText(
                                    context,
                                    if (enabled) "Módulos anteriores habilitados GLOBALMENTE para todos" else "Modo Restricción activado: PSA Exclusivo para todos",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }

            // Real-time Firebase Error Monitoring Panel (AppErrors)
            item {
                com.example.ui.components.AppErrorsMonitorCard(
                    appErrors = appErrors,
                    isLoading = isLoadingAppErrors,
                    pendingLocalCount = pendingLocalErrorCount,
                    onRefresh = { viewModel.refreshAppErrorsFromCloud() },
                    onFlushLocal = { viewModel.flushPendingAppErrorsNow(manualFeedback = true) },
                    onSendTestReport = { viewModel.sendDiagnosticTestAppError() },
                    onToggleResolve = { errorId, resolved -> viewModel.resolveAppError(errorId, resolved) },
                    onDeleteError = { errorId -> viewModel.deleteAppError(errorId) }
                )
            }

            // List of secondary users
            item {
                Text(
                    text = "Operadores Registrados (${secondaryUsers.size})",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (secondaryUsers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No has creado usuarios aún. Utiliza el formulario arriba para crear operadores con su key.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(secondaryUsers) { user ->
                    val isExpired = user.isExpired
                    val isBlocked = user.isBlocked
                    val isAccessActive = !isExpired && !isBlocked

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SlateCard)
                            .border(1.dp, if (isAccessActive) SlateCardBorder else RoseError.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isAccessActive) EmeraldSuccess.copy(alpha = 0.15f) else RoseError.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isAccessActive) EmeraldSuccess else RoseError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = user.username, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(text = if (user.notes.isNotBlank()) user.notes else "Operador CRM", color = TextSecondary, fontSize = 11.sp)
                                }

                                // Status badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                isBlocked -> RoseError.copy(alpha = 0.2f)
                                                user.updateRequired -> com.example.ui.theme.PsaCyanGlow.copy(alpha = 0.2f)
                                                isExpired -> RoseError.copy(alpha = 0.2f)
                                                user.remainingDays <= 2 -> AmberDelay.copy(alpha = 0.2f)
                                                else -> EmeraldSuccess.copy(alpha = 0.2f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isBlocked -> "BLOQUEADO"
                                            user.updateRequired -> "ACTUALIZAR"
                                            isExpired -> "EXPIRADO"
                                            else -> "${user.remainingDays}D RESTANTES"
                                        },
                                        color = when {
                                            isBlocked || isExpired -> RoseError
                                            user.updateRequired -> com.example.ui.theme.PsaCyanGlow
                                            user.remainingDays <= 2 -> AmberDelay
                                            else -> EmeraldSuccess
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Google Email & Device Binding Cloud Box
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SlateBackground)
                                    .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (user.email.isNotBlank()) "📧 ${user.email}" else "🔑 ${user.token}",
                                        color = com.example.ui.theme.PsaCyanGlow,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(IndigoPrimary.copy(alpha = 0.2f))
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "📱 ${user.boundDevicesCount} / ${user.maxDevices}",
                                            color = IndigoPrimary,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                val ids = user.boundDeviceIds
                                val labels = user.boundDeviceLabels
                                if (ids.isEmpty()) {
                                    Text(
                                        text = "Sin equipo vinculado todavía (reclama cupo al primer ingreso con Google).",
                                        color = TextMuted,
                                        fontSize = 10.5.sp
                                    )
                                } else {
                                    ids.forEachIndexed { idx, devId ->
                                        val label = labels.getOrElse(idx) { "Dispositivo ${idx + 1}" }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "• $label (${devId.take(6)})",
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(
                                                onClick = {
                                                    viewModel.unlinkUserDevice(user, devId) { _, msg ->
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.heightIn(min = 32.dp)
                                            ) {
                                                Text("Desvincular", color = RoseError, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // PSA Exclusive vs Legacy Modules Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (user.canAccessLegacyModules) IndigoPrimary.copy(alpha = 0.12f) else com.example.ui.theme.PsaCyanPrimary.copy(alpha = 0.12f))
                                    .border(
                                        1.dp,
                                        if (user.canAccessLegacyModules) IndigoPrimary.copy(alpha = 0.35f) else com.example.ui.theme.PsaCyanGlow.copy(alpha = 0.35f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (user.canAccessLegacyModules) "🔓 Módulos Anteriores Habilitados" else "💧 Modo Exclusivo PSA Purificadores",
                                        color = if (user.canAccessLegacyModules) IndigoPrimary else com.example.ui.theme.PsaCyanGlow,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (user.canAccessLegacyModules) "El operador tiene acceso al Panel y Automatizador antiguo" else "El operador solo ve el espacio de PSA Purificadores (recomendado)",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                Switch(
                                    checked = user.canAccessLegacyModules,
                                    onCheckedChange = { allow ->
                                        viewModel.toggleUserLegacyAccess(user.id, allow)
                                        Toast.makeText(
                                            context,
                                            if (allow) "Módulos anteriores habilitados para ${user.username}" else "Modo PSA Exclusivo activado para ${user.username}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = IndigoPrimary,
                                        checkedTrackColor = IndigoPrimary.copy(alpha = 0.3f),
                                        uncheckedThumbColor = com.example.ui.theme.PsaCyanGlow,
                                        uncheckedTrackColor = Color(0xFF031E2C)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Temporary Update-Required Lock Control (Auto-unlocks on new APK OR Manual Admin Re-enable)
                            val serverVer = serverAppUpdateInfo?.latestVersionCode ?: 0
                            val targetReqCode = maxOf(serverVer, com.example.BuildConfig.VERSION_CODE + 1)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (user.updateRequired) AmberDelay.copy(alpha = 0.14f)
                                        else SlateBackground.copy(alpha = 0.7f)
                                    )
                                    .border(
                                        1.dp,
                                        if (user.updateRequired) AmberDelay.copy(alpha = 0.55f) else SlateCardBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = "Estado de actualización requerida",
                                            tint = if (user.updateRequired) AmberDelay else com.example.ui.theme.PsaCyanGlow,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (user.updateRequired) {
                                                "📲 Bloqueado hasta actualizar (Comp. >= ${user.requiredVersionCode})"
                                            } else {
                                                "📲 Exigir Actualización de App"
                                            },
                                            color = if (user.updateRequired) AmberDelay else TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = if (user.updateRequired) {
                                            "Se habilitará solo al abrir la versión nueva o si lo habilitas aquí."
                                        } else {
                                            "Bloquea al vendedor hasta que instale Comp. >= $targetReqCode."
                                        },
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        viewModel.toggleUserUpdateLock(
                                            user = user,
                                            requireUpdate = !user.updateRequired,
                                            requiredVersionCode = targetReqCode
                                        )
                                    },
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (user.updateRequired) EmeraldSuccess.copy(alpha = 0.7f) else AmberDelay.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_btn_toggle_update_lock_${user.id}")
                                ) {
                                    Text(
                                        text = if (user.updateRequired) "Habilitar" else "Exigir Update",
                                        color = if (user.updateRequired) EmeraldSuccess else AmberDelay,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Vence: ${dateFormat.format(Date(user.expiresAtMs))}", color = TextMuted, fontSize = 11.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(text = "${user.remainingDays} días vigentes", color = if (isAccessActive) EmeraldSuccess else RoseError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val hasOnlineRecord = user.lastOnlineVerifiedAtMs > 0L
                            val offlineOver24h = hasOnlineRecord &&
                                (System.currentTimeMillis() - user.lastOnlineVerifiedAtMs) >= com.example.util.LicenseSecurityGuard.OFFLINE_LOCK_THRESHOLD_MS
                            val onlineTint = when {
                                !hasOnlineRecord -> TextMuted
                                offlineOver24h -> AmberDelay
                                else -> EmeraldSuccess
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(onlineTint)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasOnlineRecord) {
                                        val suffix = if (offlineOver24h) " (+24h sin red)" else ""
                                        "Última conexión: ${dateFormat.format(Date(user.lastOnlineVerifiedAtMs))}$suffix"
                                    } else {
                                        "Última conexión: sin registros aún"
                                    },
                                    color = onlineTint,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (user.lastTamperAlertAtMs > 0L) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(RoseError.copy(alpha = 0.14f))
                                        .border(1.dp, RoseError.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = RoseError,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Alerta: posible atraso de fecha detectado (${dateFormat.format(Date(user.lastTamperAlertAtMs))})",
                                        color = RoseError,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.adjustUserDays(user.id, -1) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_btn_minus1d_${user.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("1d", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.adjustUserDays(user.id, 1) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_btn_plus1d_${user.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("1d", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.adjustUserDays(user.id, 7) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_btn_plus7d_${user.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("7d", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { editingUser = user },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_btn_edit_user_${user.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Editar", fontSize = 11.5.sp)
                                }

                                IconButton(
                                    onClick = { viewModel.toggleBlockUser(user.id, !user.isBlocked) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("admin_btn_block_user_${user.id}")
                                ) {
                                    Icon(
                                        imageVector = if (user.isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                                        contentDescription = if (user.isBlocked) "Desbloquear" else "Bloquear",
                                        tint = if (user.isBlocked) EmeraldSuccess else RoseError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteUser(user) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("admin_btn_delete_user_${user.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = RoseError.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // TAB 1B: TOKENS & EXPIRACIÓN DE LICENCIAS
        // ==========================================
        if (activePrimaryTab == AdminPrimaryTab.TOKENS) {
            val resolvedTokenInviteUrl = com.example.data.repository.EzoRepository.buildGithubDirectApkUrl(
                ghUserInput,
                ghRepoInput
            ).ifBlank {
                com.example.data.repository.EzoRepository.normalizeApkDownloadUrl(apkUrlInput)
            }
            superuserTokensSection(
                context = context,
                viewModel = viewModel,
                secondaryUsers = secondaryUsers,
                cloudAccessTokens = cloudAccessTokens,
                isCloudGoogleConnected = isCloudGoogleConnected,
                isRefreshingTokens = isRefreshingAccessTokens,
                effectiveInviteUrl = resolvedTokenInviteUrl,
                isLargeFont = isLargeFont,
                onEditUser = { userToEdit -> editingUser = userToEdit }
            )
        }

        // ==========================================
        // TAB 2: MI OFERTA, PRECIOS & IA
        // ==========================================
        if (activePrimaryTab == AdminPrimaryTab.OFFERING) {
            // Identity header card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateCard)
                        .border(1.dp, IndigoPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(IndigoPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Configuración Comercial & Asistente",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Define qué vendes, tus precios de referencia, tono y objeciones. El asistente usará todo esto para auditar y sugerir respuestas.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Dedicated Gemini AI API Key Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateCard)
                        .border(
                            1.dp,
                            if (geminiApiKey.isNotBlank()) EmeraldSuccess.copy(alpha = 0.4f) else SlateCardBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (geminiApiKey.isNotBlank()) EmeraldSuccess else AmberDelay,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Clave API de Gemini (Inteligencia Artificial)",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (geminiApiKey.isNotBlank()) EmeraldSuccess.copy(alpha = 0.2f) else AmberDelay.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (geminiApiKey.isNotBlank()) "✓ Activa" else "Sin configurar",
                                    color = if (geminiApiKey.isNotBlank()) EmeraldSuccess else AmberDelay,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Configura tu clave de Google Gemini para habilitar el aprendizaje inteligente de chats, la auditoría de ventas y sugerencias automáticas.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        OutlinedTextField(
                            value = geminiKeyInput,
                            onValueChange = {
                                geminiKeyInput = it
                                viewModel.clearApiKeyTestStatus()
                            },
                            label = { Text("Clave API de Gemini (AIzaSy...)") },
                            placeholder = { Text("Pega tu clave AIzaSy aquí...") },
                            singleLine = true,
                            visualTransformation = if (isApiKeyObscured) PasswordVisualTransformation() else VisualTransformation.None,
                            trailingIcon = {
                                IconButton(onClick = { isApiKeyObscured = !isApiKeyObscured }) {
                                    Icon(
                                        imageVector = if (isApiKeyObscured) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isApiKeyObscured) "Mostrar" else "Ocultar",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("gemini_api_key_input")
                        )

                        apiKeyTestStatus?.let { status ->
                            val isSuccess = status.startsWith("✓") || status.contains("exitosa") || status.contains("éxito")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSuccess) EmeraldSuccess.copy(alpha = 0.15f) else RoseError.copy(alpha = 0.15f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = status,
                                    color = if (isSuccess) EmeraldSuccess else RoseError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.saveGeminiApiKey(geminiKeyInput)
                                    Toast.makeText(context, "Clave API de Gemini guardada", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                    .heightIn(min = 42.dp)
                                    .testTag("save_gemini_api_key_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Guardar Clave", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.testGeminiApiKey(geminiKeyInput)
                                },
                                enabled = !isTestingApiKey && (geminiKeyInput.isNotBlank() || geminiApiKey.isNotBlank()),
                                modifier = Modifier
                                    .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                    .heightIn(min = 42.dp)
                                    .testTag("test_gemini_api_key_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isTestingApiKey) {
                                    CircularProgressIndicator(color = IndigoPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Probar IA", fontSize = 12.sp, color = IndigoPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Operator Suggestions Permission toggle card (Clean, respectful wording)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateCard)
                        .border(1.dp, if (businessProfile.isAiEnabledForOperators) EmeraldSuccess.copy(alpha = 0.4f) else SlateCardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (businessProfile.isAiEnabledForOperators) EmeraldSuccess.copy(alpha = 0.2f) else SlateBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = if (businessProfile.isAiEnabledForOperators) EmeraldSuccess else IndigoPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permiso de Sugerencias para Operadores",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (businessProfile.isAiEnabledForOperators)
                                    "Habilitado: Los operadores pueden usar sugerencias de respuestas en sus chats."
                                else
                                    "Deshabilitado: Las sugerencias de respuestas están restringidas al Administrador.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = businessProfile.isAiEnabledForOperators,
                            onCheckedChange = { isEnabled ->
                                viewModel.toggleAiForOperators(isEnabled)
                                Toast.makeText(
                                    context,
                                    if (isEnabled) "Sugerencias habilitadas para operadores" else "Sugerencias restringidas al Administrador",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldSuccess,
                                checkedTrackColor = EmeraldSuccess.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SlateBackground
                            )
                        )
                    }
                }
            }

            // ==========================================
            // WIZARD CREADOR CON ORIENTACIÓN IA
            // ==========================================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateCard)
                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Asistente Creador con Orientación IA",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Escribe tu idea de negocio y la IA estructurará tu oferta comercial",
                                    color = EmeraldSuccess,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = "¿Qué ofreces? Escribe una idea breve o detalles clave (ej: 'vendo zapatillas por WhatsApp con envíos', 'delivery de pizzas caseras', 'estética y manicura', etc.):",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = userRawIdea,
                            onValueChange = { userRawIdea = it },
                            label = { Text("Cuéntame tu negocio o idea") },
                            placeholder = { Text("Ej: Vendo zapatillas y ropa deportiva importada, pagos en cuotas y envíos a todo el país...") },
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldSuccess,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "💡 O toca una sugerencia rápida:",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val ideas = listOf(
                                "👟 Zapatillas & Ropa Urbana",
                                "🍕 Pizzería & Empanadas a domicilio",
                                "💅 Estética & Manicuría profesional",
                                "🦷 Odontología & Implantes dentales",
                                "🛠️ Taller Mecánico & Repuestos",
                                "🏡 Inmobiliaria & Alquileres"
                            )
                            ideas.forEach { idea ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SlateBackground)
                                        .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                                        .clickable { userRawIdea = idea }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = idea,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (userRawIdea.isNotBlank()) {
                                    viewModel.generateBusinessProfileWithAi(userRawIdea) { generated ->
                                        if (generated != null) {
                                            editBusinessName = generated.businessName
                                            editBusinessType = generated.businessType
                                            editOffering = generated.offeringDescription
                                            editTone = generated.toneAndStyle
                                            editGoal = generated.salesObjective
                                            editFaq = generated.frequentQuestionsAndObjections
                                            Toast.makeText(context, "¡Oferta generada con IA! Revisa abajo y presiona Guardar.", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "No se pudo generar el perfil con IA", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = userRawIdea.isNotBlank() && !isAiLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldSuccess,
                                disabledContainerColor = EmeraldSuccess.copy(alpha = 0.4f)
                            )
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Orientando y creando con IA...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Orientar y Generar Configuración con IA", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // PLANTILLAS Y PRESETS DINÁMICOS
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Plantillas de Negocio Disponibles:",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = { showSavePresetDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Guardar actual", fontSize = 11.sp, color = IndigoPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (allPresets.isEmpty()) {
                        Text(
                            text = "Cargando plantillas...",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allPresets.forEach { preset ->
                                val isSelected = editBusinessType.equals(preset.businessType, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) IndigoPrimary.copy(alpha = 0.2f) else SlateCard)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) IndigoPrimary else SlateCardBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            viewModel.applyPreset(preset)
                                            editBusinessName = preset.businessName
                                            editBusinessType = preset.businessType
                                            editOffering = preset.offeringDescription
                                            editTone = preset.toneAndStyle
                                            editGoal = preset.salesObjective
                                            editFaq = preset.frequentQuestionsAndObjections
                                            Toast.makeText(context, "Plantilla '${preset.title}' cargada", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = preset.title,
                                            color = if (isSelected) IndigoPrimary else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )

                                        if (preset.isCustom) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.deletePreset(preset)
                                                    Toast.makeText(context, "Plantilla eliminada", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Eliminar plantilla",
                                                    tint = RoseError.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // FORMULARIO DE DETALLES Y EDICIÓN
            // ==========================================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateCard)
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = IndigoPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "¿Qué es lo que ofrezco? & Identidad",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = editBusinessName,
                            onValueChange = { editBusinessName = it },
                            label = { Text("Nombre del Negocio / Marca") },
                            placeholder = { Text("Ej: Autos Selectos / Tarot Místico / Cortinas Deco") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editBusinessType,
                            onValueChange = { editBusinessType = it },
                            label = { Text("Rubro o Especialidad") },
                            placeholder = { Text("Ej: Venta de vehículos seminuevos / Videncia y limpiezas") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editOffering,
                            onValueChange = { editOffering = it },
                            label = { Text("¿Qué ofrezco? (Catálogo, servicios, precios, promociones)") },
                            placeholder = { Text("Describe detalladamente lo que vendes o los servicios que brindas, facilidades de pago, garantías y ventajas.") },
                            minLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editTone,
                            onValueChange = { editTone = it },
                            label = { Text("Tono, Vocabulario y Estilo de Habla") },
                            placeholder = { Text("Ej: Místico y empático, hablando de energías y luz; o Vendedor automotriz profesional; o Cálido asesor de diseño.") },
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editGoal,
                            onValueChange = { editGoal = it },
                            label = { Text("Objetivo Principal de Conversación") },
                            placeholder = { Text("Ej: Agendar visita para ver el auto; Concretar lectura reservando turno con seña; Solicitar medidas.") },
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editFaq,
                            onValueChange = { editFaq = it },
                            label = { Text("Preguntas Frecuentes & Manejo de Objeciones") },
                            placeholder = { Text("Ej: ¿Cómo es el pago? ¿Hacen envíos? ¿Aceptan permutas? ¿Cuánto dura la sesión?") },
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showSavePresetDialog = true },
                                modifier = Modifier
                                    .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary)
                            ) {
                                Text("Guardar Plantilla", color = IndigoPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val updated = businessProfile.copy(
                                        businessName = editBusinessName.trim(),
                                        businessType = editBusinessType.trim(),
                                        offeringDescription = editOffering.trim(),
                                        toneAndStyle = editTone.trim(),
                                        salesObjective = editGoal.trim(),
                                        frequentQuestionsAndObjections = editFaq.trim()
                                    )
                                    viewModel.saveBusinessProfile(updated) {
                                        Toast.makeText(context, "¡Configuración activa guardada con éxito!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1.3f) }
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Guardar Activa", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // TAB 3: ASISTENTE COMERCIAL & HERRAMIENTAS
        // ==========================================
        if (activePrimaryTab == AdminPrimaryTab.ASSISTANT) {
            // Interactive Subtool Selector Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistantSubTool.values().forEach { tool ->
                        FilterChip(
                            selected = activeAssistantSubTool == tool,
                            onClick = { activeAssistantSubTool = tool },
                            label = { Text(tool.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (activeAssistantSubTool == AssistantSubTool.PUSH_ALERTS) {
                criticalPushAlertsTab(
                    context = context,
                    adminUsername = currentUser?.username ?: "Superusuario"
                )
            }

            if (activeAssistantSubTool == AssistantSubTool.LIVE_RESPONSES) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateCard)
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.QuestionAnswer, contentDescription = null, tint = IndigoPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Probador de Respuestas Inteligentes",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Prueba en vivo cómo responde la IA adaptándose a lo que ofreces (${businessProfile.businessType}) y con el tono configurado.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Text(
                            text = "Prueba rápida con 1 toque:",
                            color = IndigoPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        val sampleChips = remember(businessProfile.businessType) {
                            when {
                                businessProfile.businessType.contains("auto", ignoreCase = true) ||
                                        businessProfile.businessType.contains("vehículo", ignoreCase = true) -> listOf(
                                    "¿Tienen autos automáticos con garantía?",
                                    "¿Aceptan permuta o cuotas fijas?",
                                    "Me parece un poco caro, ¿se puede charlar?"
                                )
                                businessProfile.businessType.contains("cortina", ignoreCase = true) -> listOf(
                                    "¿Vienen a domicilio a tomar las medidas?",
                                    "¿Cuánto demoran en confeccionar e instalar?",
                                    "¿Qué telas recomiendan para oscurecer el living?"
                                )
                                businessProfile.businessType.contains("tarot", ignoreCase = true) ||
                                        businessProfile.businessType.contains("limpieza", ignoreCase = true) -> listOf(
                                    "Quiero una lectura de cartas sobre el amor y futuro",
                                    "¿Cómo es la sesión de limpieza energética y cuánto dura?",
                                    "Necesito saber el costo y cómo reservar turno"
                                )
                                else -> listOf(
                                    "¿Cuánto sale y qué facilidades de pago tienen?",
                                    "¿Tienen disponibilidad hoy mismo?",
                                    "Me parece caro, ¿hay alguna promoción o descuento?"
                                )
                            }
                        }

                        val liveChipsScroll = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(liveChipsScroll),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            sampleChips.forEach { sample ->
                                OutlinedButton(
                                    onClick = {
                                        clientTestMessage = sample
                                        viewModel.generateAiSmartResponses(sample)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = IndigoPrimary.copy(alpha = 0.08f),
                                        contentColor = IndigoPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(sample, fontSize = 11.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = clientTestMessage,
                            onValueChange = { clientTestMessage = it },
                            label = { Text("Mensaje o consulta del cliente") },
                            placeholder = { Text("Ej: Hola, vi tu anuncio. ¿Cuánto cuesta el servicio y qué incluye?") },
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (clientTestMessage.isBlank()) {
                                    Toast.makeText(context, "Ingresa un mensaje para generar respuestas", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.generateAiSmartResponses(clientTestMessage)
                            },
                            enabled = !isAiLoading,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analizando consulta con IA...")
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generar 3 Opciones de Respuesta", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (smartResponses.isNotEmpty()) {
                item {
                    Text(
                        text = "Respuestas Generadas por la IA:",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(smartResponses) { option ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SlateCard)
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(IndigoPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = option.title,
                                        color = IndigoPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = option.strategy,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Text(
                                text = option.text,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Respuesta IA", option.text)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Respuesta copiada para enviar en CRM", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copiar Respuesta", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                AiSalesMemorySection(viewModel = viewModel)
            }
        }

        // ==========================================
        // TAB 4: AUDITORÍA & COACHING DE VENTAS
        // ==========================================
        if (activeAssistantSubTool == AssistantSubTool.COACHING) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateCard)
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.School, contentDescription = null, tint = IndigoPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Entrenador y Auditor de Ventas",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Pega una conversación completa entre un cliente y el vendedor para que la IA evalúe la efectividad, te diga qué errores se cometieron y qué mejorar para cerrar más ventas.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        // Sample chat loader button
                        OutlinedButton(
                            onClick = {
                                coachingConversationText = """
                                Cliente: Hola, vi la publicación de ${businessProfile.businessName}. ¿Qué precio tiene?
                                Vendedor: Hola, sale ${'$'}15.000.
                                Cliente: Uh, pensé que era más económico. Gracias igual, lo voy a pensar.
                                Vendedor: Bueno, cualquier cosa me avisas.
                                """.trimIndent()
                                viewModel.evaluateSalesConversation(coachingConversationText)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = IndigoPrimary.copy(alpha = 0.08f),
                                contentColor = IndigoPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cargar Charla de Ejemplo y Auditar (1 Toque)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = coachingConversationText,
                            onValueChange = { coachingConversationText = it },
                            label = { Text("Conversación a Auditar") },
                            placeholder = { Text("Cliente: Hola, quería saber el precio...\nVendedor: Hola, sale \$5000.\nCliente: Bueno gracias, lo voy a pensar.") },
                            minLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (coachingConversationText.isBlank()) {
                                    Toast.makeText(context, "Pega una charla para evaluarla", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.evaluateSalesConversation(coachingConversationText)
                            },
                            enabled = !isAiLoading,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auditando conversación...")
                            } else {
                                Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auditar Charla y Obtener Consejos", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            salesCoachingResult?.let { coaching ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SlateCard)
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (coaching.score >= 75) EmeraldSuccess.copy(alpha = 0.2f)
                                            else if (coaching.score >= 50) AmberDelay.copy(alpha = 0.2f)
                                            else RoseError.copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${coaching.score}",
                                        color = if (coaching.score >= 75) EmeraldSuccess else if (coaching.score >= 50) AmberDelay else RoseError,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Calificación de Venta: ${coaching.score}/100",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = coaching.summary,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Strengths
                            Text(text = "✅ Puntos Fuertes:", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            coaching.strengths.forEach { str ->
                                Row {
                                    Text("• ", color = EmeraldSuccess)
                                    Text(str, color = TextPrimary, fontSize = 12.sp)
                                }
                            }

                            // Opportunities / Mistakes
                            Text(text = "⚠️ Errores u Oportunidades Perdidas:", color = RoseError, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            coaching.opportunities.forEach { opp ->
                                Row {
                                    Text("• ", color = RoseError)
                                    Text(opp, color = TextPrimary, fontSize = 12.sp)
                                }
                            }

                            // Actionable Coaching Advice
                            Text(text = "💡 Consejos para Vender Más a Futuro:", color = AmberDelay, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            coaching.coachingAdvice.forEach { adv ->
                                Row {
                                    Text("• ", color = AmberDelay)
                                    Text(adv, color = TextPrimary, fontSize = 12.sp)
                                }
                            }

                            // Ideal Response Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(IndigoPrimary.copy(alpha = 0.1f))
                                    .border(1.dp, IndigoPrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "🎯 Respuesta Ideal que debió enviarse:",
                                        color = IndigoPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = coaching.idealResponse,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Respuesta Ideal", coaching.idealResponse)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Respuesta ideal copiada", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copiar", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // TAB 5: APRENDIZAJE DE CHATS CRM
        // ==========================================
        if (activeAssistantSubTool == AssistantSubTool.CRM_LEARNING) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateCard)
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Insights, contentDescription = null, tint = IndigoPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Aprender de los Chats del CRM",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "La IA procesa los ${allScannedChats.size} chats escaneados en tu base de datos para sintetizar qué buscan los clientes, cuáles son las objeciones reales y cómo ajustar tu oferta para vender más.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Button(
                            onClick = {
                                if (allScannedChats.isEmpty()) {
                                    Toast.makeText(context, "Aún no tienes chats escaneados. Escanéalos desde la pestaña Automatizar.", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                viewModel.learnFromScannedChats()
                            },
                            enabled = !isAiLoading,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Procesando historial del CRM con IA...")
                            } else {
                                Icon(imageVector = Icons.Default.Psychology, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analizar y Aprender de los Chats (${allScannedChats.size})", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            chatLearningInsight?.let { insight ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SlateCard)
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "📊 Resumen Ejecutivo de Clientes:",
                                color = IndigoPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = insight.overallSummary,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )

                            // Questions
                            Text(text = "❓ Preguntas e Inquietudes Más Comunes:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            insight.commonQuestions.forEach { q ->
                                Row {
                                    Text("• ", color = IndigoPrimary)
                                    Text(q, color = TextSecondary, fontSize = 12.sp)
                                }
                            }

                            // Objections
                            Text(text = "🛡️ Objeciones y Frenos Frecuentes:", color = AmberDelay, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            insight.detectedObjections.forEach { obj ->
                                Row {
                                    Text("• ", color = AmberDelay)
                                    Text(obj, color = TextSecondary, fontSize = 12.sp)
                                }
                            }

                            // Actions
                            Text(text = "🚀 Acciones Recomendadas para Vender Más:", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            insight.recommendedActions.forEach { act ->
                                Row {
                                    Text("• ", color = EmeraldSuccess)
                                    Text(act, color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // ==========================================
        // TAB 4: RENDIMIENTO DEL EQUIPO (NUBE)
        // ==========================================
        if (activePrimaryTab == AdminPrimaryTab.PERFORMANCE) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SlateCard)
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "📊 Métricas Privadas por Vendedor",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Cada vendedor conserva su agenda privada; aquí ves totales agregados en tiempo real.",
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.refreshSellerPerformanceBoard() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("admin_btn_refresh_stats")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar métricas", tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Actualizar", color = IndigoPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                SuperuserStatsDashboard(
                    stats = sellerPerformanceBoard,
                    isCloudConnected = isCloudGoogleConnected,
                    onRefreshStats = { viewModel.refreshSellerPerformanceBoard() }
                )
            }

            if (sellerPerformanceBoard.isNotEmpty()) {
                item {
                    val totalContacts = sellerPerformanceBoard.sumOf { it.totalContacts }
                    val totalVisits = sellerPerformanceBoard.sumOf { it.totalVisits }
                    val totalDemos = sellerPerformanceBoard.sumOf { it.completedDemos }
                    val totalSales = sellerPerformanceBoard.sumOf { it.closedSales }
                    val totalPending = sellerPerformanceBoard.sumOf { it.pendingReplacements }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("Contactos", totalContacts, IndigoPrimary),
                            Triple("Visitas", totalVisits, com.example.ui.theme.PsaCyanGlow),
                            Triple("Demos", totalDemos, PurpleNeon),
                            Triple("Ventas", totalSales, EmeraldSuccess),
                            Triple("Recambios", totalPending, AmberDelay)
                        ).forEach { (label, value, tintColor) ->
                            Box(
                                modifier = Modifier
                                    .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SlateCard)
                                    .border(1.dp, tintColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$value", color = tintColor, fontWeight = FontWeight.Black, fontSize = 17.sp)
                                    Text(label, color = TextSecondary, fontSize = 10.5.sp)
                                }
                            }
                        }
                    }
                }

                items(sellerPerformanceBoard) { stat ->
                    val sellerMessagesSent = maxOf(stat.messagesTotal, stat.messagesSuccess + stat.messagesFailed)
                    val sellerPortfolioDenom = maxOf(stat.totalContacts, stat.totalVisits, stat.closedSales)
                    val sellerConvPct = if (sellerPortfolioDenom > 0) {
                        (stat.closedSales * 100f / sellerPortfolioDenom).coerceIn(0f, 100f)
                    } else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SlateCard)
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                            .testTag("seller_stat_card_${stat.ownerUid}")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stat.sellerName.ifBlank { "Vendedor PSA" },
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = stat.sellerEmail.ifBlank { stat.ownerUid },
                                        color = TextMuted,
                                        fontSize = 11.5.sp
                                    )
                                }
                                if (stat.updatedAtMs > 0L) {
                                    Text(
                                        text = dateFormat.format(Date(stat.updatedAtMs)),
                                        color = TextMuted,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("📤 ${stat.messagesSuccess}/$sellerMessagesSent msgs (${stat.messagingSuccessRate.roundToInt()}%)", color = com.example.ui.theme.PsaCyanGlow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("📈 ${"%.1f".format(sellerConvPct)}% conv.", color = PurpleNeon, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("📞 ${stat.totalContacts} contactos", color = IndigoPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("🏠 ${stat.totalVisits} visitas", color = com.example.ui.theme.PsaCyanGlow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("🔬 ${stat.completedDemos} demos", color = PurpleNeon, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("✅ ${stat.closedSales} ventas", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("🔧 ${stat.pendingReplacements} recambios pend.", color = AmberDelay, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog to edit user details, remaining days and maxDevices
    editingUser?.let { user ->
        EditUserDialog(
            user = user,
            onDismiss = { editingUser = null },
            onUnlinkDevice = { deviceId ->
                viewModel.unlinkUserDevice(user, deviceId) { ok, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (ok) editingUser = null
                }
            },
            onSave = { updatedName, updatedNotes, updatedDays, updatedMaxDevices ->
                viewModel.updateUserDetails(
                    userId = user.id,
                    newName = updatedName,
                    newNotes = updatedNotes,
                    remainingDays = updatedDays,
                    maxDevices = updatedMaxDevices
                ) { ok, _ ->
                    if (ok) {
                        editingUser = null
                    }
                }
            }
        )
    }

    // Modal Dialog to save custom preset
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guardar como Nueva Plantilla",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Guarda la configuración comercial actual para poder reutilizarla con 1 toque en cualquier momento.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = newPresetTitle,
                        onValueChange = { newPresetTitle = it },
                        label = { Text("Nombre de la Plantilla") },
                        placeholder = { Text("Ej: Tienda Zapatillas, Delivery Gourmet, etc.") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = newPresetTitle.trim().ifEmpty {
                            editBusinessType.trim().ifEmpty { editBusinessName.trim().ifEmpty { "Plantilla Personalizada" } }
                        }
                        viewModel.saveCustomPreset(
                            title = title,
                            businessName = editBusinessName.trim(),
                            businessType = editBusinessType.trim(),
                            offeringDescription = editOffering.trim(),
                            toneAndStyle = editTone.trim(),
                            salesObjective = editGoal.trim(),
                            frequentQuestionsAndObjections = editFaq.trim()
                        )
                        Toast.makeText(context, "Plantilla '$title' guardada con éxito", Toast.LENGTH_SHORT).show()
                        showSavePresetDialog = false
                        newPresetTitle = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Guardar Plantilla", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SlateCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditUserDialog(
    user: UserAccount,
    onDismiss: () -> Unit,
    onUnlinkDevice: (deviceId: String) -> Unit = {},
    onSave: (newName: String, newNotes: String, remainingDays: Int, maxDevices: Int) -> Unit
) {
    var editName by remember { mutableStateOf(user.username) }
    var editNotes by remember { mutableStateOf(user.notes) }
    var daysCount by remember { mutableStateOf(user.remainingDays.coerceAtLeast(0L).toInt()) }
    var maxDevicesCount by remember { mutableStateOf(user.maxDevices.coerceIn(1, 5)) }
    val minAllowedMax = user.boundDevicesCount.coerceAtLeast(1)
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    val previewExpiry = remember(daysCount) {
        val now = System.currentTimeMillis()
        now + (daysCount.toLong() * 24L * 60L * 60L * 1000L)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Editar Usuario, Días y Equipos",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (user.email.isNotBlank()) {
                    Text(
                        text = "Email Google: ${user.email}",
                        color = com.example.ui.theme.PsaCyanGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Nombre del Usuario") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndigoPrimary,
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editNotes,
                    onValueChange = { editNotes = it },
                    label = { Text("Notas / Turno") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndigoPrimary,
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Max Devices Stepper (1..5)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SlateBackground)
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Dispositivos Permitidos (${user.boundDevicesCount} vinculados):",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { if (maxDevicesCount > minAllowedMax) maxDevicesCount-- },
                                enabled = maxDevicesCount > minAllowedMax,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RoseError.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Restar dispositivo", tint = RoseError, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "$maxDevicesCount equipo(s)",
                                color = EmeraldSuccess,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            IconButton(
                                onClick = { if (maxDevicesCount < 5) maxDevicesCount++ },
                                enabled = maxDevicesCount < 5,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Sumar dispositivo", tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                            }
                        }
                        user.boundDeviceIds.forEachIndexed { idx, devId ->
                            val lbl = user.boundDeviceLabels.getOrElse(idx) { "Dispositivo ${idx + 1}" }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• $lbl", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                TextButton(
                                    onClick = { onUnlinkDevice(devId) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Desvincular", color = RoseError, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SlateBackground)
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Ajustar Días de Acceso:",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { if (daysCount > 0) daysCount-- },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(RoseError.copy(alpha = 0.15f))
                                    .testTag("edit_user_minus_1d")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Restar 1 día",
                                    tint = RoseError,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = "$daysCount días",
                                color = if (daysCount > 0) TextPrimary else RoseError,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            IconButton(
                                onClick = { daysCount++ },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess.copy(alpha = 0.15f))
                                    .testTag("edit_user_plus_1d")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Sumar 1 día",
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { daysCount = 0 },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("edit_user_expire_0d")
                            ) {
                                Text("0d (Vencer hoy)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { daysCount = (daysCount - 5).coerceAtLeast(0) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("-5d", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { daysCount += 30 },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("+30d", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = if (daysCount <= 0) "Estado al guardar: EXPIRADO (0 días — acceso bloqueado)" else "Nueva expiración: ${dateFormat.format(Date(previewExpiry))}",
                            color = if (daysCount > 0) EmeraldSuccess else RoseError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(editName, editNotes, daysCount, maxDevicesCount) },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Guardar Cambios", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SlateCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.roleMatrixTab(
    roleMatrix: RoleVisibilityConfig,
    onSaveMatrix: (RoleVisibilityConfig) -> Unit,
    onSyncNow: () -> Unit
) {
    item {
        var selectedRoleTab by remember { mutableStateOf(0) } // 0: Operador, 1: Superusuario

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateCard)
                    .border(1.dp, PurpleNeon.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PurpleNeon.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Visibilidad por Rol", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Control por perfil", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = onSyncNow,
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 38.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sincronizar", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Role Selector Buttons
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedRoleTab == 0) PurpleNeon else Color.Transparent)
                                .clickable { selectedRoleTab = 0 }
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👤 Operador",
                                color = if (selectedRoleTab == 0) Color.White else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedRoleTab == 1) IndigoPrimary else Color.Transparent)
                                .clickable { selectedRoleTab = 1 }
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👑 Admin",
                                color = if (selectedRoleTab == 1) Color.White else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (selectedRoleTab == 0) {
                // OPERATORS MODULES CONTROLS
                Text(
                    text = "Módulos y Herramientas para Operadores (Usuarios):",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                RoleToggleCard(
                    title = "💧 Módulo PSA (Cotizador, Agenda y Asesor)",
                    description = "Espacio completo de purificadores de agua PSA, comparativa de ahorro y agenda de visitas.",
                    checked = roleMatrix.opPsaEnabled,
                    onCheckedChange = { onSaveMatrix(roleMatrix.copy(opPsaEnabled = it)) }
                )

                RoleToggleCard(
                    title = "💬 Mensajes & Scripts Rápidos",
                    description = "Carpetas de guiones predefinidos y respuestas rápidas para WhatsApp.",
                    checked = roleMatrix.opScriptsEnabled,
                    onCheckedChange = { onSaveMatrix(roleMatrix.copy(opScriptsEnabled = it)) }
                )

                RoleToggleCard(
                    title = "⚡ Automatización Masiva CRM",
                    description = "Envío de campañas CRM (solo visible para Superusuario).",
                    checked = roleMatrix.opAutomationEnabled,
                    onCheckedChange = { onSaveMatrix(roleMatrix.copy(opAutomationEnabled = it)) }
                )

                RoleToggleCard(
                    title = "📊 Métricas & Dashboard",
                    description = "Resumen de mensajes enviados, tasa de éxito y actividad general.",
                    checked = roleMatrix.opDashboardEnabled,
                    onCheckedChange = { onSaveMatrix(roleMatrix.copy(opDashboardEnabled = it)) }
                )

                RoleToggleCard(
                    title = "🤖 Asesor Comercial IA para Operadores",
                    description = "Permite a los operadores consultar consejos de cierre y respuestas persuasivas.",
                    checked = roleMatrix.opAiSalesEnabled,
                    onCheckedChange = { onSaveMatrix(roleMatrix.copy(opAiSalesEnabled = it)) }
                )

                RoleToggleCard(
                    title = "⚙️ Editar Precios Base PSA",
                    description = "Permite a los operadores entrar a la configuración de precios y modificar valores base.",
                    checked = roleMatrix.opPricingEditEnabled,
                    onCheckedChange = { onSaveMatrix(roleMatrix.copy(opPricingEditEnabled = it)) }
                )
            } else {
                // SUPERUSER MODULES CONTROLS
                Text(
                    text = "Pestañas Visibles en Barra para Superusuario:",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                RoleToggleCard(
                    title = "💧 Espacio PSA en Barra",
                    description = "Muestra la pestaña de PSA en tu barra de navegación inferior.",
                    checked = roleMatrix.suPsaEnabled,
                    onCheckedChange = { onSaveMatrix(roleMatrix.copy(suPsaEnabled = it)) }
                )

                RoleToggleCard(
                    title = "💬 Scripts & Mensajes Rápidos",
                    description = "Muestra la pestaña de Scripts en tu barra de navegación.",
                    checked = roleMatrix.suScriptsEnabled,
                    onCheckedChange = { onSaveMatrix(roleMatrix.copy(suScriptsEnabled = it)) }
                )

                RoleToggleCard(
                    title = "⚡ Automatización CRM",
                    description = "Muestra la pestaña de Automatización en tu barra de navegación.",
                    checked = roleMatrix.suAutomationEnabled,
                    onCheckedChange = { onSaveMatrix(roleMatrix.copy(suAutomationEnabled = it)) }
                )

                RoleToggleCard(
                    title = "📊 Dashboard & Métricas",
                    description = "Muestra la pestaña de Panel de Métricas en tu barra de navegación.",
                    checked = roleMatrix.suDashboardEnabled,
                    onCheckedChange = { onSaveMatrix(roleMatrix.copy(suDashboardEnabled = it)) }
                )

                RoleToggleCard(
                    title = "👑 Pestaña Superusuario (Admin)",
                    description = "Muestra la pestaña en la barra inferior. Si la ocultas, siempre podrás abrirla o reactivarla tocando la insignia '👑 ADMIN' en la barra superior.",
                    checked = roleMatrix.suSuperuserEnabled,
                    onCheckedChange = { onSaveMatrix(roleMatrix.copy(suSuperuserEnabled = it)) }
                )
            }
        }
    }
}

@Composable
fun RoleToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateCard)
            .border(
                1.dp,
                if (checked) IndigoPrimary.copy(alpha = 0.5f) else SlateCardBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (checked) TextPrimary else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = IndigoPrimary,
                    checkedTrackColor = IndigoPrimary.copy(alpha = 0.35f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = SlateBackground
                )
            )
        }
    }
}

private fun shareMessageViaWhatsApp(context: Context, message: String) {
    try {
        val uri = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(message))
        val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(waIntent)
    } catch (_: Exception) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            val chooser = Intent.createChooser(sendIntent, "Compartir enlace oficial").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "No se pudo abrir WhatsApp ni el selector para compartir", Toast.LENGTH_SHORT).show()
        }
    }
}

