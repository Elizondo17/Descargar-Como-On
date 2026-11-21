package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Brush
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.data.model.ScannedChat
import com.example.util.UnattendedAutomationLogger
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Send
import com.example.data.model.ScriptFolder
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AutomationState
import com.example.data.model.ScriptItem
import com.example.service.EzoAccessibilityService
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
import com.example.ui.viewmodel.MainViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AutomationScreen(
    viewModel: MainViewModel,
    preselectedScript: ScriptItem? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLargeFont = com.example.ui.theme.LocalIsLargeFontMode.current
    val currentUser by viewModel.currentUser.collectAsState()
    val allFolders by viewModel.allFolders.collectAsState()
    val allScripts by viewModel.allScripts.collectAsState()
    val progress by viewModel.automationProgress.collectAsState()
    val config by viewModel.automationConfig.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityConnected.collectAsState()
    val isBubbleVisible by viewModel.isBubbleVisible.collectAsState()
    val isBankingMode by viewModel.isBankingMode.collectAsState()
    val isAppPaused by viewModel.isAppPaused.collectAsState()
    val selectedChatsReadyCount by viewModel.selectedChatsReadyCount.collectAsState()
    var showKillSwitchConfirmDialog by remember { mutableStateOf(false) }

    val scannedChats by viewModel.allScannedChats.collectAsState()
    val selectedChats by viewModel.selectedScannedChats.collectAsState()
    val businessProfile by viewModel.businessProfile.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val smartResponses by viewModel.smartResponses.collectAsState()

    val detectedCrmApp by viewModel.detectedCrmApp.collectAsState()
    val isCrmWindowActive by viewModel.isCrmWindowActive.collectAsState()
    val visibleChatsCount by viewModel.visibleChatsCount.collectAsState()
    val lastDetectedWindowTitle by viewModel.lastDetectedWindowTitle.collectAsState()
    val activeCrmFolder by viewModel.activeCrmFolder.collectAsState()
    val scannedCrmFolders by viewModel.scannedCrmFolders.collectAsState()
    var folderFilter by remember { mutableStateOf<String?>(null) }

    val isSuperUser = currentUser?.role == "SUPERUSER"
    val canUseAi = isSuperUser || businessProfile.isAiEnabledForOperators

    var showAiResponseDialog by remember { mutableStateOf(false) }
    var activeAiChatTarget by remember { mutableStateOf<ScannedChat?>(null) }
    var customAiPrompt by remember { mutableStateOf("") }
    var showRemoteAccessInfo by remember { mutableStateOf(false) }
    var showUnattendedLogsDialog by remember { mutableStateOf(false) }

    // Folder & Script selection states requested by user
    var selectedFolderId by remember(allFolders) {
        mutableStateOf<Long?>(allFolders.firstOrNull()?.id)
    }
    var expandedFolderMenu by remember { mutableStateOf(false) }

    val scriptsInSelectedFolder = remember(allScripts, selectedFolderId) {
        if (selectedFolderId != null) {
            allScripts.filter { it.folderId == selectedFolderId }
        } else {
            allScripts
        }
    }

    var selectedScript by remember(allScripts, preselectedScript, selectedFolderId) {
        mutableStateOf(preselectedScript ?: scriptsInSelectedFolder.firstOrNull() ?: allScripts.firstOrNull())
    }

    var expandedScriptMenu by remember { mutableStateOf(false) }

    // Auto-sync script when folder changes
    androidx.compose.runtime.LaunchedEffect(selectedFolderId, scriptsInSelectedFolder) {
        if (selectedScript == null || (selectedFolderId != null && selectedScript?.folderId != selectedFolderId)) {
            selectedScript = scriptsInSelectedFolder.firstOrNull()
        }
    }

    // Keyword, Calendar Date & Status filters for eligible chats
    var keywordFilter by remember { mutableStateOf("") }
    var calendarDateFilter by remember { mutableStateOf("") }
    var quickDateFilter by remember { mutableStateOf("TODAS") } // "TODAS", "HOY", "AYER"
    var statusFilter by remember { mutableStateOf("TODOS") } // "TODOS", "ELEGIBLES", "PENDIENTES", "ENVIADOS"
    var userDismissedAccessibilityBanner by remember { mutableStateOf(false) }

    var sliderDelayRange by remember {
        mutableStateOf(config.minDelaySeconds.toFloat()..config.maxDelaySeconds.toFloat())
    }

    var nightQuietHoursEnabled by remember { mutableStateOf(config.nightQuietHoursEnabled) }
    var allowedStartHour by remember { mutableStateOf(config.allowedStartHour) }
    var allowedStartMinute by remember { mutableStateOf(config.allowedStartMinute) }
    var allowedEndHour by remember { mutableStateOf(config.allowedEndHour) }
    var allowedEndMinute by remember { mutableStateOf(config.allowedEndMinute) }
    var dimScreenWhileAutomating by remember { mutableStateOf(config.dimScreenWhileAutomating) }
    var useAiSmartResponse by remember { mutableStateOf(config.useAiSmartResponse) }
    var aiResponseStrategy by remember { mutableStateOf(config.aiResponseStrategy) }
    var showAiTesterDialog by remember { mutableStateOf(false) }
    var showSalesMemoryDialog by remember { mutableStateOf(false) }
    var testCustomerMessage by remember { mutableStateOf("") }
    var notifyOnEverySuccess by remember { mutableStateOf(config.notifyOnEverySuccess) }

    var autoLaunchAppOnBubble by remember { mutableStateOf(false) }
    var selectedPackageToLaunch by remember { mutableStateOf("com.whatsapp") }
    var customPackageInput by remember { mutableStateOf("") }

    val isRunning = progress.state == AutomationState.WAITING_DELAY ||
            progress.state == AutomationState.SENDING ||
            progress.state == AutomationState.SCANNING

    val isAccessibilitySystemEnabled = remember(isAccessibilityActive) {
        isAccessibilityActive || EzoAccessibilityService.isAccessibilityEnabledInSettings(context)
    }

    // Dynamic filtering for eligible chats
    val filteredChats = remember(scannedChats, keywordFilter, calendarDateFilter, quickDateFilter, statusFilter) {
        scannedChats.filter { chat ->
            val matchesKeyword = keywordFilter.isBlank() ||
                    chat.name.contains(keywordFilter, ignoreCase = true) ||
                    chat.phoneNumber.contains(keywordFilter, ignoreCase = true) ||
                    chat.lastMessage.contains(keywordFilter, ignoreCase = true) ||
                    chat.folderName.contains(keywordFilter, ignoreCase = true)

            val matchesDate = when {
                calendarDateFilter.isNotBlank() -> chat.dateText.contains(calendarDateFilter, ignoreCase = true)
                quickDateFilter == "HOY" -> chat.dateText.contains("hoy", ignoreCase = true) || chat.dateText.contains(":")
                quickDateFilter == "AYER" -> chat.dateText.contains("ayer", ignoreCase = true)
                else -> true
            }

            val matchesStatus = when (statusFilter) {
                "ELEGIBLES" -> chat.isSelected && !chat.isSent
                "PENDIENTES" -> !chat.isSent
                "ENVIADOS" -> chat.isSent
                else -> true
            }

            val matchesFolder = folderFilter == null || chat.folderName.equals(folderFilter, ignoreCase = true)

            matchesKeyword && matchesDate && matchesStatus && matchesFolder
        }
    }

    val eligibleRecipients = selectedChats.filter { !it.isSent }
    val allFilteredSelected = filteredChats.isNotEmpty() && filteredChats.all { it.isSelected }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Operational Header: Modo Pausa & Kill Switch Bancos
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateCard)
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Modo Pausa Switch
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isAppPaused) AmberDelay.copy(alpha = 0.2f) else SlateBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                tint = if (isAppPaused) AmberDelay else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Modo Pausa",
                                fontWeight = FontWeight.Bold,
                                color = if (isAppPaused) AmberDelay else TextPrimary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isAppPaused) "Campaña pausada" else "Envío activo",
                                color = if (isAppPaused) AmberDelay else TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = isAppPaused,
                            onCheckedChange = { paused ->
                                viewModel.setAppPaused(context, paused)
                                android.widget.Toast.makeText(
                                    context,
                                    if (paused) "⏸️ Modo Pausa activado: Campañas y burbujas en pausa" else "▶️ Modo Pausa desactivado",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .scale(0.75f)
                                .testTag("automation_pause_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AmberDelay,
                                checkedTrackColor = AmberDelay.copy(alpha = 0.35f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SlateBackground
                            )
                        )
                    }

                    // Kill Switch Button
                    Button(
                        onClick = { showKillSwitchConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .testTag("automation_kill_switch_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Kill Switch Bancos",
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("KILL BANCOS", fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }



        // Remote Access & Modo Banco Card (1-touch protection against banking app blocks)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isBankingMode) Color(0xFF450A0A) else SlateCard)
                    .border(
                        1.dp,
                        if (isBankingMode) RoseError else IndigoPrimary.copy(alpha = 0.35f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isBankingMode) RoseError.copy(alpha = 0.2f) else IndigoPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isBankingMode) Icons.Default.Lock else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isBankingMode) RoseError else IndigoPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = if (isBankingMode) "🏦 Modo Banco ACTIVO" else "🏦 Modo Banco",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isBankingMode) "Superposiciones ocultas" else "Oculta burbuja al abrir banco",
                                    color = if (isBankingMode) EmeraldSuccess else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = IndigoPrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Accessibility, contentDescription = "Ajustes", modifier = Modifier.size(14.dp))
                            }

                            Button(
                                onClick = {
                                    if (isBankingMode) {
                                        viewModel.disableBankingMode()
                                    } else {
                                        viewModel.enableBankingMode()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBankingMode) EmeraldSuccess else Color(0xFF7F1D1D)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Text(
                                    text = if (isBankingMode) "Restaurar" else "Ocultar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Bubble Launcher Card (Only launches the bubble, never opens external apps by default)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateCard)
                    .border(
                        1.dp,
                        if (isBubbleVisible) EmeraldSuccess.copy(alpha = 0.4f) else IndigoPrimary.copy(alpha = 0.35f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp)
            ) {
                Column {
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isBubbleVisible) EmeraldSuccess.copy(alpha = 0.15f) else IndigoPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BubbleChart,
                                    contentDescription = null,
                                    tint = if (isBubbleVisible) EmeraldSuccess else IndigoPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (isBubbleVisible) "Burbuja Flotante ACTIVA" else "Burbuja Flotante",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Modo Libre sobre CRM",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val willBeVisible = !isBubbleVisible
                                viewModel.toggleFloatingBubble(context, willBeVisible)
                                if (willBeVisible && autoLaunchAppOnBubble) {
                                    val targetPkg = if (selectedPackageToLaunch == "custom") customPackageInput.trim() else selectedPackageToLaunch
                                    if (targetPkg.isNotBlank()) {
                                        try {
                                            val intent = context.packageManager.getLaunchIntentForPackage(targetPkg)
                                            if (intent != null) {
                                                context.startActivity(intent)
                                            } else {
                                                Toast.makeText(context, "No se encontró instalada: $targetPkg", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error al abrir app: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBubbleVisible) RoseError.copy(alpha = 0.85f) else IndigoPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.heightIn(min = 38.dp)
                        ) {
                            Icon(
                                imageVector = if (isBubbleVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBubbleVisible) "Ocultar" else "Lanzar",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Optional app opening toggle requested by user
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { autoLaunchAppOnBubble = !autoLaunchAppOnBubble }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Abrir una app al lanzar la burbuja (Opcional)",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (autoLaunchAppOnBubble) "Activado: abrirá la app elegida" else "Desactivado por defecto (solo flota la burbuja)",
                                color = if (autoLaunchAppOnBubble) PurpleNeon else TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = autoLaunchAppOnBubble,
                            onCheckedChange = { autoLaunchAppOnBubble = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = IndigoPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SlateBackground
                            )
                        )
                    }

                    if (autoLaunchAppOnBubble) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = selectedPackageToLaunch == "com.whatsapp",
                                onClick = { selectedPackageToLaunch = "com.whatsapp" },
                                label = { Text("WhatsApp", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = selectedPackageToLaunch == "com.whatsapp.w4b",
                                onClick = { selectedPackageToLaunch = "com.whatsapp.w4b" },
                                label = { Text("WA Business", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = selectedPackageToLaunch == "custom",
                                onClick = { selectedPackageToLaunch = "custom" },
                                label = { Text("Otro CRM", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        if (selectedPackageToLaunch == "custom") {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customPackageInput,
                                onValueChange = { customPackageInput = it },
                                label = { Text("Package Name de tu CRM (ej. com.mi.crm)", fontSize = 11.sp) },
                                placeholder = { Text("com.ejemplo.crm", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IndigoPrimary,
                                    unfocusedBorderColor = SlateCardBorder
                                )
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // DETECCIÓN DE VENTANA DEL CRM & ESTADO EN TIEMPO REAL
        // =========================================================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isCrmWindowActive) EmeraldSuccess.copy(alpha = 0.08f) else SlateCard)
                    .border(
                        1.dp,
                        if (isCrmWindowActive) EmeraldSuccess.copy(alpha = 0.4f) else SlateCardBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isCrmWindowActive) EmeraldSuccess else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCrmWindowActive) "Ventana CRM Activa: $detectedCrmApp" else "Detector de Ventanas CRM",
                                color = if (isCrmWindowActive) EmeraldSuccess else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isCrmWindowActive && visibleChatsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmeraldSuccess.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "$visibleChatsCount en pantalla",
                                    color = EmeraldSuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Active Folder Indicator & Detected Folders
                    if (isCrmWindowActive) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (activeCrmFolder != "General") EmeraldSuccess else IndigoPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Carpeta activa en CRM: ${if (activeCrmFolder != "General") activeCrmFolder else "General (Lista Principal)"}",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (scannedCrmFolders.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Carpetas detectadas:", color = TextSecondary, fontSize = 10.sp)
                                    scannedCrmFolders.take(4).forEach { folder ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (folder.name.equals(activeCrmFolder, ignoreCase = true)) EmeraldSuccess.copy(alpha = 0.25f) else SlateBackground)
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = folder.name,
                                                color = if (folder.name.equals(activeCrmFolder, ignoreCase = true)) EmeraldSuccess else TextSecondary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isCrmWindowActive) {
                            "Carpeta '$activeCrmFolder' activa en $detectedCrmApp."
                        } else {
                            "Abre tu CRM/WhatsApp en la carpeta deseada y escanea."
                        },
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.scanCrmScreenNow(autoScroll = true)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCrmWindowActive) EmeraldSuccess else IndigoPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                .heightIn(min = 40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCrmWindowActive) "Escanear '$activeCrmFolder'" else "Escanear Pantalla",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (scannedChats.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.clearScannedChats() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                                modifier = Modifier
                                    .let { if (isLargeFont) it.fillMaxWidth() else it }
                                    .heightIn(min = 40.dp)
                            ) {
                                Text("Limpiar (${scannedChats.size})", fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // PASO 1: CHATS ESCANEADOS Y FILTROS DE ELEGIBILIDAD
        // =========================================================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateCard)
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(IndigoPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("PASO 1", color = PurpleNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chats Elegibles",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Text Search Filter
                    OutlinedTextField(
                        value = keywordFilter,
                        onValueChange = { keywordFilter = it },
                        placeholder = { Text("Buscar por nombre, teléfono o texto del chat...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (keywordFilter.isNotBlank()) {
                                IconButton(onClick = { keywordFilter = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar búsqueda", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Date Filters & Calendar Picker
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Filtro por Fecha del Último Chat:",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = quickDateFilter == "TODAS" && calendarDateFilter.isBlank(),
                                onClick = {
                                    quickDateFilter = "TODAS"
                                    calendarDateFilter = ""
                                },
                                label = { Text("Todas", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = quickDateFilter == "HOY",
                                onClick = {
                                    quickDateFilter = "HOY"
                                    calendarDateFilter = ""
                                },
                                label = { Text("Hoy", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = quickDateFilter == "AYER",
                                onClick = {
                                    quickDateFilter = "AYER"
                                    calendarDateFilter = ""
                                },
                                label = { Text("Ayer", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White
                                )
                            )

                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val monthsShort = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
                                            val mStr = monthsShort.getOrElse(month) { "${month + 1}" }
                                            calendarDateFilter = "$dayOfMonth $mStr"
                                            quickDateFilter = "CUSTOM"
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (calendarDateFilter.isNotBlank()) PurpleNeon else TextSecondary
                                ),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (calendarDateFilter.isNotBlank()) calendarDateFilter else "Calendario",
                                    fontSize = 11.sp
                                )
                            }

                            if (calendarDateFilter.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        calendarDateFilter = ""
                                        quickDateFilter = "TODAS"
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(RoseError.copy(alpha = 0.15f))
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Quitar fecha", tint = RoseError, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Status Filters Row
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Filtro por Estado:",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = statusFilter == "TODOS",
                                onClick = { statusFilter = "TODOS" },
                                label = { Text("Todos (${scannedChats.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = statusFilter == "ELEGIBLES",
                                onClick = { statusFilter = "ELEGIBLES" },
                                label = { Text("Elegibles ✓ (${eligibleRecipients.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldSuccess.copy(alpha = 0.25f),
                                    selectedLabelColor = EmeraldSuccess
                                )
                            )
                            FilterChip(
                                selected = statusFilter == "PENDIENTES",
                                onClick = { statusFilter = "PENDIENTES" },
                                label = { Text("Pendientes", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberDelay.copy(alpha = 0.25f),
                                    selectedLabelColor = AmberDelay
                                )
                            )
                            FilterChip(
                                selected = statusFilter == "ENVIADOS",
                                onClick = { statusFilter = "ENVIADOS" },
                                label = { Text("Enviados", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Actions Bar (Select/Deselect filtered, Scan now, Clear)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val targetIds = filteredChats.map { it.id }
                                    viewModel.setSelectedForChatIds(targetIds, true)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = IndigoPrimary),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Text("✓ Todos (${filteredChats.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val pendingIds = filteredChats.filter { !it.isSent }.map { it.id }
                                    viewModel.selectOnlyPendingChats(pendingIds)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Text("⚡ Pendientes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val targetIds = filteredChats.map { it.id }
                                    viewModel.setSelectedForChatIds(targetIds, false)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Text("✗ Ninguno", fontSize = 11.sp)
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.scanCrmScreenNow(autoScroll = true) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Escanear Pantalla", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.clearScannedChats() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Limpiar Lista", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Scanned Chats List
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.FormatListBulleted,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Chats Detectados (${filteredChats.size})",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${eligibleRecipients.size} seleccionados y listos para recibir el mensaje",
                        color = if (eligibleRecipients.isNotEmpty()) EmeraldSuccess else AmberDelay,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Render each scanned chat in a simple, streamlined, compact card
        if (filteredChats.isNotEmpty()) {
            items(filteredChats) { chat ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SlateCard)
                        .border(
                            1.dp,
                            if (chat.isSent) EmeraldSuccess.copy(alpha = 0.4f)
                            else if (chat.isSelected) IndigoPrimary.copy(alpha = 0.6f)
                            else SlateCardBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            if (!chat.isSent) {
                                viewModel.toggleChatSelection(chat.id, !chat.isSelected)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = chat.isSelected,
                            onCheckedChange = { if (!chat.isSent) viewModel.toggleChatSelection(chat.id, it) },
                            enabled = !chat.isSent,
                            colors = CheckboxDefaults.colors(
                                checkedColor = IndigoPrimary,
                                uncheckedColor = SlateCardBorder,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = chat.phoneNumber.ifBlank { chat.name },
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                if (chat.folderName.isNotBlank() && chat.folderName != "General") {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(EmeraldSuccess.copy(alpha = 0.15f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = chat.folderName,
                                            color = EmeraldSuccess,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(IndigoPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = chat.dateText.ifBlank { "Hoy" },
                                        color = TextPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (chat.name.isNotBlank() && chat.name != chat.phoneNumber) {
                                Text(
                                    text = chat.name,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = if (chat.isSent) "✓ Mensaje enviado con éxito"
                                       else if (chat.lastMessage.isNotBlank()) chat.lastMessage
                                       else "Sin mensaje previo registrado",
                                color = if (chat.isSent) EmeraldSuccess else if (chat.lastMessage.isNotBlank()) TextSecondary else TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (canUseAi && !chat.isSent) {
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = {
                                    activeAiChatTarget = chat
                                    customAiPrompt = if (chat.lastMessage.isNotBlank()) chat.lastMessage else "Hola, información de ${businessProfile.businessName}"
                                    viewModel.generateAiSmartResponses(customAiPrompt)
                                    showAiResponseDialog = true
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = IndigoPrimary.copy(alpha = 0.12f),
                                    contentColor = IndigoPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f)),
                                modifier = Modifier.heightIn(min = 32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("IA", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateCard)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (scannedChats.isEmpty()) "No hay chats escaneados todavía" else "Ningún chat coincide con los filtros aplicados",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (scannedChats.isEmpty())
                                "Lanza la burbuja flotante sobre tu CRM/WhatsApp y pulsa 'Escanear Pantalla CRM' para capturar los chats automáticamente."
                            else "Modifica la palabra clave, el filtro de calendario o el estado para visualizar más chats.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // =========================================================================
        // PASO 2: SELECCIÓN DE CARPETA DE SCRIPTS Y MENSAJE A UTILIZAR
        // =========================================================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateCard)
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(IndigoPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("PASO 2", color = PurpleNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Carpeta y Script",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 1. Selector de Carpeta de Scripts
                    Text(
                        text = "Carpeta de Scripts:",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedFolderMenu,
                        onExpandedChange = { expandedFolderMenu = !expandedFolderMenu }
                    ) {
                        val activeFolder = allFolders.find { it.id == selectedFolderId }
                        OutlinedTextField(
                            value = activeFolder?.name ?: "Selecciona una carpeta...",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFolderMenu) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedFolderMenu,
                            onDismissRequest = { expandedFolderMenu = false },
                            modifier = Modifier.background(SlateCard)
                        ) {
                            allFolders.forEach { folder ->
                                val scriptCountInFolder = allScripts.count { it.folderId == folder.id }
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(folder.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("$scriptCountInFolder scripts disponibles", color = TextSecondary, fontSize = 10.sp)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedFolderId = folder.id
                                        expandedFolderMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Selector de Script dentro de la Carpeta
                    Text(
                        text = "Script / Mensaje a Enviar:",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedScriptMenu,
                        onExpandedChange = { expandedScriptMenu = !expandedScriptMenu }
                    ) {
                        OutlinedTextField(
                            value = selectedScript?.title ?: "Selecciona un script...",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedScriptMenu) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedScriptMenu,
                            onDismissRequest = { expandedScriptMenu = false },
                            modifier = Modifier.background(SlateCard)
                        ) {
                            scriptsInSelectedFolder.forEach { script ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(script.title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                            Text(
                                                script.content.take(50) + "...",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedScript = script
                                        expandedScriptMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Preview of the message to be sent
                    selectedScript?.let { script ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateBackground)
                                .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Vista previa del mensaje:",
                                    color = PurpleNeon,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "\"${script.content}\"",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                                Text(
                                    text = "💡 La variable {name} se reemplazará automáticamente con el nombre de cada contacto.",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Anti-Spam Range Slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = AmberDelay, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Variación Anti-Spam: ${sliderDelayRange.start.toInt()}s - ${sliderDelayRange.endInclusive.toInt()}s",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    RangeSlider(
                        value = sliderDelayRange,
                        onValueChange = { sliderDelayRange = it },
                        valueRange = 15f..60f,
                        steps = 45,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberDelay,
                            activeTrackColor = AmberDelay,
                            inactiveTrackColor = SlateCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Horario Permitido (Protección Nocturna)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SlateBackground.copy(alpha = 0.6f))
                            .border(1.dp, SlateCardBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (nightQuietHoursEnabled) PurpleNeon else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Protección Nocturna (Horario Permitido)",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Switch(
                                    checked = nightQuietHoursEnabled,
                                    onCheckedChange = {
                                        nightQuietHoursEnabled = it
                                        viewModel.updateConfig(
                                            config.copy(nightQuietHoursEnabled = it)
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = IndigoPrimary,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = SlateCard
                                    )
                                )
                            }

                            if (nightQuietHoursEnabled) {
                                val startStr = String.format(java.util.Locale.ROOT, "%02d:%02d", allowedStartHour, allowedStartMinute)
                                val endStr = String.format(java.util.Locale.ROOT, "%02d:%02d", allowedEndHour, allowedEndMinute)
                                Text(
                                    text = "Envíos entre $startStr y $endStr.",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilterChip(
                                        selected = allowedStartHour == 9 && allowedEndHour == 20,
                                        onClick = {
                                            allowedStartHour = 9
                                            allowedStartMinute = 0
                                            allowedEndHour = 20
                                            allowedEndMinute = 0
                                            viewModel.updateConfig(
                                                config.copy(
                                                    allowedStartHour = 9,
                                                    allowedStartMinute = 0,
                                                    allowedEndHour = 20,
                                                    allowedEndMinute = 0
                                                )
                                            )
                                        },
                                        label = { Text("09:00 - 20:00", fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PurpleNeon.copy(alpha = 0.25f),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                    FilterChip(
                                        selected = allowedStartHour == 8 && allowedEndHour == 21,
                                        onClick = {
                                            allowedStartHour = 8
                                            allowedStartMinute = 0
                                            allowedEndHour = 21
                                            allowedEndMinute = 0
                                            viewModel.updateConfig(
                                                config.copy(
                                                    allowedStartHour = 8,
                                                    allowedStartMinute = 0,
                                                    allowedEndHour = 21,
                                                    allowedEndMinute = 0
                                                )
                                            )
                                        },
                                        label = { Text("08:00 - 21:00", fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PurpleNeon.copy(alpha = 0.25f),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                    FilterChip(
                                        selected = allowedStartHour == 10 && allowedEndHour == 19,
                                        onClick = {
                                            allowedStartHour = 10
                                            allowedStartMinute = 0
                                            allowedEndHour = 19
                                            allowedEndMinute = 0
                                            viewModel.updateConfig(
                                                config.copy(
                                                    allowedStartHour = 10,
                                                    allowedStartMinute = 0,
                                                    allowedEndHour = 19,
                                                    allowedEndMinute = 0
                                                )
                                            )
                                        },
                                        label = { Text("10:00 - 19:00", fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PurpleNeon.copy(alpha = 0.25f),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Modo Atenuación de Pantalla (Ahorro de Batería)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SlateBackground.copy(alpha = 0.6f))
                            .border(1.dp, SlateCardBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🌙", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Atenuar pantalla durante envíos",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Reduce el brillo con pantalla oscura para evitar calentamiento y ahorrar batería. Toca para ver.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = dimScreenWhileAutomating,
                                onCheckedChange = {
                                    dimScreenWhileAutomating = it
                                    viewModel.updateConfig(
                                        config.copy(dimScreenWhileAutomating = it)
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = IndigoPrimary,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = SlateCard
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Respuestas Automáticas con Inteligencia Artificial (Nicho Comercial)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (useAiSmartResponse) IndigoPrimary.copy(alpha = 0.12f) else SlateBackground.copy(alpha = 0.6f)
                            )
                            .border(
                                1.dp,
                                if (useAiSmartResponse) IndigoPrimary.copy(alpha = 0.6f) else SlateCardBorder.copy(alpha = 0.6f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (useAiSmartResponse) IndigoPrimary else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Respuesta Automática Inteligente con IA",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (useAiSmartResponse)
                                                "Activado: La IA leerá el último chat y responderá coherentemente según el nicho comercial"
                                            else
                                                "Desactivado: Envía el guion de texto seleccionado",
                                            color = if (useAiSmartResponse) EmeraldSuccess else TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Switch(
                                    checked = useAiSmartResponse,
                                    onCheckedChange = {
                                        useAiSmartResponse = it
                                        viewModel.updateConfig(
                                            config.copy(useAiSmartResponse = it)
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = IndigoPrimary,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = SlateCard
                                    )
                                )
                            }

                            if (useAiSmartResponse) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SlateBackground.copy(alpha = 0.8f))
                                        .padding(8.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = "Nicho Activo: ${businessProfile.businessName}",
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Tipo: ${businessProfile.businessType.ifBlank { "Ventas / Asesoría General" }}",
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "Estrategia de respuesta automática:",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val strategies = listOf("Cierre Comercial", "Cálido y Empático", "Pregunta Estratégica")
                                    strategies.forEach { strategy ->
                                        FilterChip(
                                            selected = aiResponseStrategy == strategy,
                                            onClick = {
                                                aiResponseStrategy = strategy
                                                viewModel.updateConfig(
                                                    config.copy(aiResponseStrategy = strategy)
                                                )
                                            },
                                            label = { Text(strategy, fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = IndigoPrimary.copy(alpha = 0.35f),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        testCustomerMessage = scannedChats.firstOrNull { it.lastMessage.isNotBlank() }?.lastMessage
                                            ?: "Hola, me interesa lo que ofrecen en ${businessProfile.businessName}. ¿Qué opciones y facilidades tienen?"
                                        viewModel.generateAiSmartResponses(testCustomerMessage)
                                        showAiTesterDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Probar Respuesta de la IA en vivo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Control de Notificaciones de Campaña
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SlateBackground.copy(alpha = 0.6f))
                            .border(1.dp, SlateCardBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔔", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Avisos individuales de cada envío",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (!notifyOnEverySuccess)
                                        "Silenciado (Recomendado): Solo recibirás el resumen final de la campaña y avisos si ocurre un error."
                                    else
                                        "Activo: Se emitirá una notificación por cada mensaje individual entregado.",
                                    color = if (!notifyOnEverySuccess) EmeraldSuccess else TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = notifyOnEverySuccess,
                                onCheckedChange = {
                                    notifyOnEverySuccess = it
                                    viewModel.updateConfig(
                                        config.copy(notifyOnEverySuccess = it)
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = IndigoPrimary,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = SlateCard
                                )
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // PASO 3: CAMPAÑA PREPARADA Y BOTÓN DE ACTIVACIÓN
        // =========================================================================
        item {
            val activeFolderName = allFolders.find { it.id == selectedFolderId }?.name ?: "Carpeta Activa"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                SlateCard,
                                if (isRunning) AmberDelay.copy(alpha = 0.08f) else IndigoPrimary.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        if (isRunning) AmberDelay.copy(alpha = 0.5f) else IndigoPrimary.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isRunning) AmberDelay.copy(alpha = 0.2f) else EmeraldSuccess.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isRunning) "EN EJECUCIÓN" else "PASO 3",
                                color = if (isRunning) AmberDelay else EmeraldSuccess,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "Monitor de Envío en Vivo" else "Campaña Preparada para Envío",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Preparation Summary Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateBackground.copy(alpha = 0.8f))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Carpeta: $activeFolderName", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (useAiSmartResponse) Icons.Default.AutoAwesome else Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = if (useAiSmartResponse) IndigoPrimary else PurpleNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (useAiSmartResponse)
                                        "Modalidad: IA Dinámica ($aiResponseStrategy)"
                                    else
                                        "Script: ${selectedScript?.title ?: "Ninguno seleccionado"}",
                                    color = if (useAiSmartResponse) IndigoPrimary else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.FormatListBulleted, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Destinatarios elegibles: ${eligibleRecipients.size} chats listos",
                                    color = if (eligibleRecipients.isNotEmpty()) EmeraldSuccess else RoseError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = AmberDelay, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tiempo seguro: ${sliderDelayRange.start.toInt()}s - ${sliderDelayRange.endInclusive.toInt()}s entre envíos",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Live countdown or execution state
                    if (isRunning) {
                        if (progress.state == AutomationState.WAITING_DELAY) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = {
                                            val total = config.maxDelaySeconds.toFloat().coerceAtLeast(1f)
                                            (progress.secondsRemaining.toFloat() / total).coerceIn(0f, 1f)
                                        },
                                        color = AmberDelay,
                                        trackColor = SlateCardBorder,
                                        strokeWidth = 4.dp,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Text(
                                        text = "${progress.secondsRemaining}s",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Próximo: ${progress.currentRecipient}",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Pausa anti-spam activa para proteger tu cuenta de WhatsApp",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = progress.lastActionMessage,
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Main Action Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!isRunning) {
                            Button(
                                onClick = {
                                    selectedScript?.let { script ->
                                        viewModel.updateConfig(
                                            config.copy(
                                                minDelaySeconds = sliderDelayRange.start.toInt(),
                                                maxDelaySeconds = sliderDelayRange.endInclusive.toInt(),
                                                selectedScriptId = script.id,
                                                nightQuietHoursEnabled = nightQuietHoursEnabled,
                                                allowedStartHour = allowedStartHour,
                                                allowedStartMinute = allowedStartMinute,
                                                allowedEndHour = allowedEndHour,
                                                allowedEndMinute = allowedEndMinute,
                                                dimScreenWhileAutomating = dimScreenWhileAutomating
                                            )
                                        )
                                        viewModel.startAutomation(script)
                                    }
                                },
                                enabled = selectedScript != null && eligibleRecipients.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("start_campaign_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (eligibleRecipients.isNotEmpty())
                                        "Enviar a ${eligibleRecipients.size} Chats"
                                    else
                                        "Selecciona chats primero",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.pauseAutomation() },
                                    modifier = Modifier
                                        .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                        .heightIn(min = 48.dp)
                                        .testTag("pause_campaign_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberDelay)
                                ) {
                                    Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pausar", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.stopAutomation() },
                                    modifier = Modifier
                                        .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                        .heightIn(min = 48.dp)
                                        .testTag("stop_campaign_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                                ) {
                                    Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Detener", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Botón de Diagnóstico Desatendido (Logs en archivo local)
                        OutlinedButton(
                            onClick = { showUnattendedLogsDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .testTag("view_unattended_logs_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                        ) {
                            Icon(imageVector = Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📋 Log de Diagnóstico", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Asistente Comercial IA (Opcional)
        if (canUseAi) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    IndigoPrimary.copy(alpha = 0.18f),
                                    SlateCard
                                )
                            )
                        )
                        .border(1.dp, IndigoPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(IndigoPrimary.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Asistente Comercial IA",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = businessProfile.businessName,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                activeAiChatTarget = null
                                customAiPrompt = "Hola, me interesa lo que ofrecen en ${businessProfile.businessName}. ¿Qué opciones tienen y precios?"
                                viewModel.generateAiSmartResponses(customAiPrompt)
                                showAiResponseDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consultar Asistente IA", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showSalesMemoryDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🧠 Memoria de Ventas", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(showAiResponseDialog) {
        if (showAiResponseDialog && customAiPrompt.isNotBlank() && smartResponses.isEmpty()) {
            viewModel.generateAiSmartResponses(customAiPrompt)
        }
    }

    if (showAiResponseDialog) {
        AlertDialog(
            onDismissRequest = { showAiResponseDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Asistente IA: Sugerencias de Respuesta", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    activeAiChatTarget?.let { target ->
                        Text(
                            text = "Cliente: ${target.name.ifBlank { target.phoneNumber }}",
                            color = IndigoPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedTextField(
                        value = customAiPrompt,
                        onValueChange = { customAiPrompt = it },
                        label = { Text("Consulta o mensaje del cliente") },
                        placeholder = { Text("Escribe la consulta para que la IA responda...") },
                        maxLines = 3,
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
                            if (customAiPrompt.isNotBlank()) {
                                viewModel.generateAiSmartResponses(customAiPrompt)
                            }
                        },
                        enabled = !isAiLoading && customAiPrompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generando...", fontSize = 11.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generar Opciones", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isAiLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = IndigoPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Consultando a la IA...", color = TextSecondary, fontSize = 12.sp)
                        }
                    } else if (smartResponses.isEmpty()) {
                        Text(
                            text = "Presiona 'Generar Opciones'.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    } else {
                        smartResponses.forEach { option ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SlateBackground)
                                    .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(option.title, color = IndigoPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Respuesta IA", option.text)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Respuesta copiada", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar", tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    Text(option.text, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedScript = ScriptItem(
                                                    id = 0L,
                                                    folderId = selectedScript?.folderId ?: 1L,
                                                    title = option.title,
                                                    content = option.text
                                                )
                                                Toast.makeText(context, "Respuesta cargada como script a enviar", Toast.LENGTH_SHORT).show()
                                                showAiResponseDialog = false
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = IndigoPrimary),
                                            modifier = Modifier.heightIn(min = 34.dp)
                                        ) {
                                            Text("Usar como Script", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAiResponseDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cerrar")
                }
            },
            containerColor = SlateCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showAiTesterDialog) {
        AlertDialog(
            onDismissRequest = { showAiTesterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Probar IA Comercial del Nicho", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Business Niche Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(IndigoPrimary.copy(alpha = 0.12f))
                            .border(1.dp, IndigoPrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎯 Nicho Configurado:", color = IndigoPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(businessProfile.businessName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            if (businessProfile.businessType.isNotBlank()) {
                                Text("Rubro: ${businessProfile.businessType}", color = TextSecondary, fontSize = 10.sp)
                            }
                            if (businessProfile.salesObjective.isNotBlank()) {
                                Text("Objetivo: ${businessProfile.salesObjective}", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }

                    // Scanned Chats Quick Pickers
                    val chatsWithMessages = scannedChats.filter { it.lastMessage.isNotBlank() }.take(4)
                    if (chatsWithMessages.isNotEmpty()) {
                        Text("Cargar mensaje de chat escaneado en CRM:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            chatsWithMessages.forEach { chat ->
                                FilterChip(
                                    selected = testCustomerMessage == chat.lastMessage,
                                    onClick = { testCustomerMessage = chat.lastMessage },
                                    label = {
                                        Text(
                                            "${chat.name.take(8)}: ${chat.lastMessage.take(15)}...",
                                            fontSize = 9.sp,
                                            maxLines = 1
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PurpleNeon.copy(alpha = 0.3f),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = testCustomerMessage,
                        onValueChange = { testCustomerMessage = it },
                        label = { Text("Mensaje o consulta a responder") },
                        placeholder = { Text("Ej: ¿Tienen stock disponible y qué facilidades de pago ofrecen?") },
                        maxLines = 4,
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
                            if (testCustomerMessage.isNotBlank()) {
                                viewModel.generateAiSmartResponses(testCustomerMessage)
                            }
                        },
                        enabled = !isAiLoading && testCustomerMessage.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generando respuesta para el nicho...", fontSize = 11.sp)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generar Respuesta del Nicho con IA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isAiLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = IndigoPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("La IA está leyendo el mensaje y adaptando al catálogo...", color = TextSecondary, fontSize = 11.sp)
                        }
                    } else if (smartResponses.isNotEmpty()) {
                        Text(
                            text = "Respuestas generadas según tu nicho comercial:",
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        smartResponses.forEach { option ->
                            val isCurrentStrategy = option.title.contains(aiResponseStrategy, ignoreCase = true) ||
                                (aiResponseStrategy == "Cierre Comercial" && option.title.contains("Cierre", ignoreCase = true)) ||
                                (aiResponseStrategy == "Cálido y Empático" && option.title.contains("Cálido", ignoreCase = true)) ||
                                (aiResponseStrategy == "Pregunta Estratégica" && option.title.contains("Pregunta", ignoreCase = true))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrentStrategy) IndigoPrimary.copy(alpha = 0.15f) else SlateBackground)
                                    .border(
                                        1.dp,
                                        if (isCurrentStrategy) IndigoPrimary else SlateCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = option.title + if (isCurrentStrategy) " ★ (Estrategia Activa)" else "",
                                            color = if (isCurrentStrategy) IndigoPrimary else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Respuesta IA", option.text)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Respuesta copiada", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar", tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    Text(
                                        text = option.text,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAiTesterDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cerrar")
                }
            },
            containerColor = SlateCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showSalesMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showSalesMemoryDialog = false },
            confirmButton = {
                Button(
                    onClick = { showSalesMemoryDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cerrar")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    AiSalesMemorySection(viewModel = viewModel)
                }
            },
            containerColor = SlateCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showUnattendedLogsDialog) {
        var logLines by remember { mutableStateOf<List<String>>(emptyList()) }
        var isLoadingLogs by remember { mutableStateOf(true) }
        val scope = androidx.compose.runtime.rememberCoroutineScope()

        androidx.compose.runtime.LaunchedEffect(showUnattendedLogsDialog) {
            isLoadingLogs = true
            logLines = UnattendedAutomationLogger.readLogs(context, maxLines = 150)
            isLoadingLogs = false
        }

        AlertDialog(
            onDismissRequest = { showUnattendedLogsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.FormatListBulleted, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Diagnóstico Modo Desatendido", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    Text(
                        text = "Archivo: ${UnattendedAutomationLogger.getLogFilePath(context)}",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoadingLogs) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = IndigoPrimary)
                        }
                    } else if (logLines.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No hay registros aún. Los eventos de envío, reintentos y salida de chat se guardarán aquí automáticamente.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = logLines.joinToString("\n"),
                                color = Color(0xFFE2E8F0),
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = {
                            val fullText = logLines.joinToString("\n")
                            val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipMgr.setPrimaryClip(ClipData.newPlainText("unattended_log", fullText))
                            Toast.makeText(context, "Log copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        },
                        enabled = logLines.isNotEmpty()
                    ) {
                        Text("Copiar Log", color = Color(0xFF38BDF8))
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                UnattendedAutomationLogger.clearLogs(context)
                                logLines = emptyList()
                                Toast.makeText(context, "Log limpiado", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = logLines.isNotEmpty()
                    ) {
                        Text("Limpiar", color = RoseError)
                    }

                    Button(
                        onClick = { showUnattendedLogsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cerrar")
                    }
                }
            },
            containerColor = SlateCard,
            shape = RoundedCornerShape(16.dp)
        )
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
                    text = "¿Deseas desactivar inmediatamente los servicios de accesibilidad, superposiciones y notificaciones de Como On para ingresar a apps bancarias (Banco Nación, etc.) de forma segura?",
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
                            android.widget.Toast.makeText(
                                context,
                                "🛡️ Kill Switch: Accesibilidad apagada. Seguro para bancos.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("APAGAR TODO", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillSwitchConfirmDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = SlateCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

