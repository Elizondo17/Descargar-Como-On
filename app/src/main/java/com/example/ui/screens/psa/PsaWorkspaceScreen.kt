package com.example.ui.screens.psa

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaMessageTone
import com.example.data.model.PsaPostSaleInstallation
import com.example.data.model.PsaPostSaleStatus
import com.example.data.model.PsaPricingConfig
import com.example.data.model.PsaSlotStatus
import com.example.data.model.PsaTimeSlot
import com.example.data.model.PsaVisitStatus
import com.example.data.model.ReplacementUrgency
import com.example.util.DemoToBudgetHandoff
import com.example.util.DemoWaterSource
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalIsLargeFontMode
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.PsaBlueDark
import com.example.ui.theme.PsaCardBorder
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.PsaWaterBg
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.PsaCountryConfig
import com.example.util.PsaCountryRegistry
import com.example.util.PsaScheduleManager
import java.util.Locale

enum class PsaWorkspaceTab(val label: String) {
    AGENDA("📅 Agenda"),
    CONTACTS("📞 Contactos"),
    BUDGET("💰 Cotizar"),
    STOCK("📦 Stock"),
    SCRIPTS("💬 Guiones"),
    LIVE_DEMO("🔬 Demostración"),
    AI_MEMORY("🧠 IA")
}

data class PsaChlorineStage(
    val currentColor: Color,
    val statusTitle: String,
    val statusColor: Color,
    val diagnosisText: String,
    val foodEffectText: String,
    val thmHealthText: String,
    val salesArgumentText: String,
    val absorptionPct: Int,
    val thmRiskBadge: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PsaWorkspaceScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit = {}
) {
    BackHandler {
        onClose()
    }

    val context = LocalContext.current
    val isLargeFont = LocalIsLargeFontMode.current
    val allVisits by viewModel.allPsaVisits.collectAsState()
    val postSaleList by viewModel.allPostSaleInstallations.collectAsState()
    val scheduleSlots by viewModel.psaScheduleSlots.collectAsState()
    val selectedDateKey by viewModel.selectedPsaDateKey.collectAsState()
    val allKnowledge by viewModel.allSalesKnowledge.collectAsState()
    val pricingConfig by viewModel.psaPricingConfig.collectAsState()
    val activeCountry by viewModel.activePsaCountry.collectAsState()
    val pendingOfficialPvp by viewModel.pendingOfficialPvpConfirmation.collectAsState()

    var activeTab by remember { mutableStateOf(PsaWorkspaceTab.AGENDA) }
    var demoWaterSource by remember { mutableStateOf(DemoWaterSource.AGUA_CORRIENTE) }
    var tapCartQuantities by remember { mutableStateOf(mapOf("senior4" to 1)) }
    var wellCartQuantities by remember { mutableStateOf(mapOf("rotech" to 1)) }
    var budgetHandoff by remember { mutableStateOf<DemoToBudgetHandoff?>(null) }
    var showNewVisitDialog by remember { mutableStateOf(false) }
    var showPricingDialog by remember { mutableStateOf(false) }
    var showPresentationMode by remember { mutableStateOf(false) }
    var showAdvisorQuiz by remember { mutableStateOf(false) }
    var showPortalDialog by remember { mutableStateOf(false) }
    var showOfficialWebBrowser by remember { mutableStateOf(false) }
    var showSmartPriceImportDialog by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var selectedVisitForDemo by remember { mutableStateOf<PsaHomeVisit?>(null) }
    var selectedPurifierForWorkspace by remember(pricingConfig) {
        mutableStateOf(pricingConfig.getAllPurifiers().first())
    }
    var slotToBook by remember { mutableStateOf<PsaTimeSlot?>(null) }
    val dailySlots by viewModel.dailySlots.collectAsState()

    // Refresh schedule on load or when visits change
    LaunchedEffect(Unit, allVisits) {
        viewModel.refreshPsaSchedule(context)
    }

    // Check and trigger daily local notifications for filter replacements due within 15 days
    LaunchedEffect(Unit) {
        viewModel.checkAndNotifyDueReplacements(context)
    }

    // Auto-sync live official PSA prices when opening PSA or switching country
    LaunchedEffect(activeCountry.code) {
        viewModel.refreshOfficialPsaPrices(context, force = false)
    }

    // Filter slots for the selected date
    val uniqueDates = remember(scheduleSlots) {
        scheduleSlots.map { it.dateKey to it.displayDate }.distinctBy { it.first }
    }

    val currentDaySlots = remember(scheduleSlots, selectedDateKey) {
        if (selectedDateKey.isEmpty()) {
            scheduleSlots.take(PsaScheduleManager.DEFAULT_HOURLY_RANGES.size)
        } else {
            scheduleSlots.filter { it.dateKey == selectedDateKey }
        }
    }

    val freeSlotsCount = currentDaySlots.count { it.status == PsaSlotStatus.FREE }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        // --- HEADER LIMPIO Y ESPACIADO (Adaptable 140%–160%) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0E1826))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("psa_btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = PsaCyanGlow
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "PSA",
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 0.3.sp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedButton(
                        onClick = { showCountryPicker = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SlateCardBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("psa_btn_country_picker")
                    ) {
                        Text(
                            text = "${activeCountry.flag} ${activeCountry.currencyCode}",
                            color = PsaCyanGlow,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showOfficialWebBrowser = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("psa_btn_portal_dip"),
                        border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.45f))
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp), tint = PsaCyanGlow)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Portal DIP", color = PsaCyanGlow, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showPricingDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("psa_btn_pricing_settings"),
                        border = BorderStroke(1.dp, SlateCardBorder)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextPrimary)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Precios", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showNewVisitDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("psa_btn_new_visit")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Visita", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // --- SUB-NAVIGATION TABS ---
        ScrollableTabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = SlateBackground,
            contentColor = PsaCyanGlow,
            edgePadding = 10.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                    color = PsaCyanGlow,
                    height = 3.dp
                )
            }
        ) {
            PsaWorkspaceTab.entries.forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = if (activeTab == tab) PsaCyanGlow else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("psa_tab_${tab.name.lowercase()}")
                )
            }
        }

        // --- TAB CONTENT ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (activeTab) {
                PsaWorkspaceTab.AGENDA -> PsaAgendaTabContent(
                    context = context,
                    viewModel = viewModel,
                    uniqueDates = uniqueDates,
                    selectedDateKey = selectedDateKey,
                    currentDaySlots = currentDaySlots,
                    freeSlotsCount = freeSlotsCount,
                    pricingConfig = pricingConfig,
                    postSaleList = postSaleList,
                    onOpenNewVisitDialog = { slot ->
                        slotToBook = slot
                        showNewVisitDialog = true
                    },
                    onOpenPricingSettings = { showPricingDialog = true },
                    onStartDemo = { visit ->
                        selectedVisitForDemo = visit
                        if (visit.waterSource == DemoWaterSource.AGUA_DE_POZO.name) {
                            demoWaterSource = DemoWaterSource.AGUA_DE_POZO
                        } else if (visit.waterSource == DemoWaterSource.AGUA_CORRIENTE.name) {
                            demoWaterSource = DemoWaterSource.AGUA_CORRIENTE
                        }
                        activeTab = PsaWorkspaceTab.LIVE_DEMO
                    }
                )

                PsaWorkspaceTab.CONTACTS -> PsaContactsTabContent(
                    viewModel = viewModel
                )

                PsaWorkspaceTab.BUDGET -> PsaBudgetTabContent(
                    context = context,
                    viewModel = viewModel,
                    pricingConfig = pricingConfig,
                    activeCountry = activeCountry,
                    initialHandoff = budgetHandoff,
                    onRegisterPostSale = { custName, purifierId, purifierName, cycleMonths ->
                        viewModel.registerPostSaleInstallation(
                            customerName = custName,
                            customerPhone = selectedVisitForDemo?.phone ?: "",
                            purifierId = purifierId,
                            purifierName = purifierName,
                            replacementCycleMonths = cycleMonths,
                            visitId = selectedVisitForDemo?.id ?: ""
                        )
                    },
                    onOpenPricingSettings = { showPricingDialog = true }
                )

                PsaWorkspaceTab.STOCK -> PsaStockTabContent(
                    context = context,
                    pricingConfig = pricingConfig,
                    activeCountry = activeCountry,
                    onOpenPortalSync = { showPortalDialog = true }
                )

                PsaWorkspaceTab.SCRIPTS -> PsaScriptsTabContent(
                    context = context,
                    viewModel = viewModel,
                    scheduleSlots = scheduleSlots
                )

                PsaWorkspaceTab.LIVE_DEMO -> PsaClientDemoSection(
                    pricingConfig = pricingConfig,
                    activeCountry = activeCountry,
                    isFullscreenPresentation = false,
                    initialWaterSource = demoWaterSource,
                    onWaterSourceChange = { demoWaterSource = it },
                    initialTapCart = tapCartQuantities,
                    onTapCartChange = { tapCartQuantities = it },
                    initialWellCart = wellCartQuantities,
                    onWellCartChange = { wellCartQuantities = it },
                    onOpenFullscreen = { showPresentationMode = true },
                    onRequestCartBudget = { items, mode ->
                        budgetHandoff = DemoToBudgetHandoff(
                            purifierId = items.firstOrNull()?.purifierId ?: "senior4",
                            items = items,
                            paymentMode = mode
                        )
                        activeTab = PsaWorkspaceTab.BUDGET
                    },
                    activeVisit = selectedVisitForDemo,
                    onClearActiveVisit = { selectedVisitForDemo = null },
                    onSaveDiagnosticToVisit = { visit, source, chlorine, hardness, liters, cartText, photoPath ->
                        viewModel.saveDemoSnapshotToVisit(
                            context = context,
                            visit = visit,
                            chlorinePpm = chlorine,
                            waterSource = source,
                            hardnessPpm = hardness,
                            realLitersPerDay = liters,
                            cartSummary = cartText,
                            testPhotoPath = photoPath
                        )
                    }
                )

                PsaWorkspaceTab.AI_MEMORY -> PsaAiMemoryTabContent(
                    context = context,
                    viewModel = viewModel,
                    allKnowledge = allKnowledge
                )
            }
        }
    }

    // New Visit Dialog (Agendar con turnos libres u horario personalizado)
    if (showNewVisitDialog) {
        PsaNewVisitDialog(
            context = context,
            viewModel = viewModel,
            availableSlots = scheduleSlots.filter { it.status == PsaSlotStatus.FREE },
            pricingConfig = pricingConfig,
            initialSlot = slotToBook,
            initialDateKey = slotToBook?.dateKey ?: selectedDateKey,
            onDismiss = {
                showNewVisitDialog = false
                slotToBook = null
            }
        )
    }

    // Pricing & Schedule Settings Dialog (Precios base, cuotas, turnos de agenda y catálogo)
    if (showPricingDialog) {
        val psaFinancingConfig by viewModel.psaFinancingConfig.collectAsState()
        val cloudPricingSnapshots by viewModel.cloudPricingSnapshots.collectAsState()
        val loggedUser by viewModel.currentUser.collectAsState()
        val activeAdminSnapshot = cloudPricingSnapshots[activeCountry.code.uppercase()]
        val showAdminSyncForSeller = loggedUser?.role != "SUPERUSER" && activeAdminSnapshot != null
        PsaPricingSettingsDialog(
            context = context,
            activeCountry = activeCountry,
            initialConfig = pricingConfig,
            initialDailySlots = dailySlots,
            initialFinancingConfig = psaFinancingConfig,
            adminCloudSnapshot = if (showAdminSyncForSeller) activeAdminSnapshot else null,
            onApplyAdminPrices = { viewModel.applyLatestCloudAdminPricingManually(context) },
            onRestoreBaseline = { viewModel.restorePsaBaselinePricing(context) },
            onOpenSmartImport = {
                showPricingDialog = false
                showSmartPriceImportDialog = true
            },
            onOpenDipCredentials = {
                showPricingDialog = false
                showPortalDialog = true
            },
            onCheckOfficialWeb = {
                showPricingDialog = false
                if (activeCountry.hasPublicStore) {
                    Toast.makeText(context, "Verificando portal oficial (${activeCountry.code})...", Toast.LENGTH_SHORT).show()
                    viewModel.refreshOfficialPsaPrices(context, force = true)
                } else {
                    showOfficialWebBrowser = true
                }
            },
            onSave = { updatedConfig, updatedDailySlots, updatedFinancing ->
                viewModel.savePsaPricingConfig(context, updatedConfig)
                viewModel.saveDailySlots(context, updatedDailySlots)
                viewModel.savePsaFinancingConfig(updatedFinancing)
                showPricingDialog = false
            },
            onDismiss = { showPricingDialog = false }
        )
    }

    pendingOfficialPvp?.let { result ->
        PsaConfirmPvpDialog(
            parsedResult = result,
            activeCountry = activeCountry,
            onConfirm = { selectedItems ->
                viewModel.applyPendingOfficialPvp(context, selectedItems, fromDipPortal = false)
                Toast.makeText(
                    context,
                    "${selectedItems.size} PVP aplicados",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = {
                viewModel.dismissPendingOfficialPvp()
            }
        )
    }

    if (showCountryPicker) {
        PsaCountryPickerDialog(
            activeCountry = activeCountry,
            onSelectCountry = { code ->
                viewModel.switchPsaCountry(context, code)
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false }
        )
    }

    // Modo Presentación Limpio para Clientes en Domicilio
    if (showPresentationMode) {
        CleanClientPresentationDialog(
            pricingConfig = pricingConfig,
            activeCountry = activeCountry,
            initialWaterSource = demoWaterSource,
            onWaterSourceChange = { demoWaterSource = it },
            initialTapCart = tapCartQuantities,
            onTapCartChange = { tapCartQuantities = it },
            initialWellCart = wellCartQuantities,
            onWellCartChange = { wellCartQuantities = it },
            onRequestCartBudget = { items, mode ->
                showPresentationMode = false
                budgetHandoff = DemoToBudgetHandoff(
                    purifierId = items.firstOrNull()?.purifierId ?: "senior4",
                    items = items,
                    paymentMode = mode
                )
                activeTab = PsaWorkspaceTab.BUDGET
            },
            activeVisit = selectedVisitForDemo,
            onClearActiveVisit = { selectedVisitForDemo = null },
            onSaveDiagnosticToVisit = { visit, source, chlorine, hardness, liters, cartText, photoPath ->
                viewModel.saveDemoSnapshotToVisit(
                    context = context,
                    visit = visit,
                    chlorinePpm = chlorine,
                    waterSource = source,
                    hardnessPpm = hardness,
                    realLitersPerDay = liters,
                    cartSummary = cartText,
                    testPhotoPath = photoPath
                )
            },
            onDismiss = { showPresentationMode = false }
        )
    }

    // Asesor Guiado: ¿Cuál es tu PSA?
    if (showAdvisorQuiz) {
        PsaAdvisorQuizDialog(
            pricingConfig = pricingConfig,
            onDismiss = { showAdvisorQuiz = false },
            onSelectRecommended = { model ->
                selectedPurifierForWorkspace = model
                activeTab = PsaWorkspaceTab.LIVE_DEMO
            }
        )
    }

    // Portal Oficial de Distribuidores PSA (DIP)
    if (showPortalDialog) {
        PsaPortalCredentialsDialog(
            context = context,
            viewModel = viewModel,
            pricingConfig = pricingConfig,
            activeCountry = activeCountry,
            onDismiss = { showPortalDialog = false }
        )
    }

    // Navegador Web Oficial de PSA con Extracción en Vivo
    if (showOfficialWebBrowser) {
        PsaOfficialWebBrowserDialog(
            context = context,
            viewModel = viewModel,
            pricingConfig = pricingConfig,
            activeCountry = activeCountry,
            onDismiss = { showOfficialWebBrowser = false }
        )
    }

    // Sincronizador Inteligente PSA por Portapapeles / Texto Copiado
    if (showSmartPriceImportDialog) {
        PsaSmartPriceImportDialog(
            context = context,
            currentConfig = pricingConfig,
            activeCountry = activeCountry,
            onPricesApplied = { newConfig, _ ->
                viewModel.savePsaPricingConfig(context, newConfig)
            },
            onDismiss = { showSmartPriceImportDialog = false }
        )
    }
}

// -------------------------------------------------------------
// TAB 1: AGENDA & HORARIOS LIBRES
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PsaAgendaTabContent(
    context: Context,
    viewModel: MainViewModel,
    uniqueDates: List<Pair<String, String>>,
    selectedDateKey: String,
    currentDaySlots: List<PsaTimeSlot>,
    freeSlotsCount: Int,
    pricingConfig: PsaPricingConfig,
    postSaleList: List<PsaPostSaleInstallation>,
    onOpenNewVisitDialog: (PsaTimeSlot?) -> Unit,
    onOpenPricingSettings: () -> Unit,
    onStartDemo: (PsaHomeVisit) -> Unit
) {
    val allVisits by viewModel.allPsaVisits.collectAsState()
    val isLargeFont = LocalIsLargeFontMode.current
    var showNewPostSaleDialog by remember { mutableStateOf(false) }
    var postSaleDialogVisit by remember { mutableStateOf<PsaHomeVisit?>(null) }
    var postSaleCustomerName by remember { mutableStateOf("") }
    var postSaleCustomerPhone by remember { mutableStateOf("") }
    var postSalePurifierId by remember { mutableStateOf("senior4") }
    var postSalePurifierName by remember { mutableStateOf("PSA Senior 4 (Sobre Mesada)") }
    var postSaleCycleMonths by remember { mutableIntStateOf(12) }
    var postSaleNotes by remember { mutableStateOf("") }
    var showUrgentPostSaleOnly by remember { mutableStateOf(false) }

    val openPostSaleForm: (PsaHomeVisit?) -> Unit = { visit ->
        postSaleDialogVisit = visit
        postSaleCustomerName = visit?.customerName.orEmpty()
        postSaleCustomerPhone = visit?.phone.orEmpty()
        val matchedModel = if (visit != null) {
            pricingConfig.getAllPurifiers().firstOrNull {
                visit.modelInterest.contains(it.name.replace("PSA ", ""), ignoreCase = true) ||
                    visit.cartSummary.contains(it.name.replace("PSA ", ""), ignoreCase = true)
            }
        } else null
        postSalePurifierId = matchedModel?.id ?: "senior4"
        postSalePurifierName = visit?.cartSummary?.ifBlank { null } ?: matchedModel?.name ?: "PSA Senior 4 (Sobre Mesada)"
        postSaleCycleMonths = 12
        postSaleNotes = ""
        showNewPostSaleDialog = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Meta Semanal de Citas & Insignias de Logro Card
        item {
            val weeklyStats = PsaScheduleManager.getWeeklyVisitsStats(allVisits, pricingConfig.weeklyVisitsGoal)
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎯", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Meta Semanal de Citas",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Objetivo: ${weeklyStats.goal} citas por semana",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Badge Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (weeklyStats.bookedVisitsCount >= weeklyStats.goal)
                                        EmeraldSuccess.copy(alpha = 0.16f)
                                    else
                                        PsaCyanPrimary.copy(alpha = 0.16f)
                                )
                                .border(
                                    1.dp,
                                    if (weeklyStats.bookedVisitsCount >= weeklyStats.goal) EmeraldSuccess.copy(alpha = 0.55f) else PsaCyanGlow.copy(alpha = 0.55f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = weeklyStats.badgeIcon, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = weeklyStats.badgeTitle,
                                    color = if (weeklyStats.bookedVisitsCount >= weeklyStats.goal) EmeraldSuccess else PsaCyanGlow,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress info
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${weeklyStats.bookedVisitsCount} de ${weeklyStats.goal} citas agendadas",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "${(weeklyStats.progressPercent * 100).toInt()}%",
                            color = PsaCyanGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { weeklyStats.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (weeklyStats.bookedVisitsCount >= weeklyStats.goal) EmeraldSuccess else PsaCyanPrimary,
                        trackColor = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = weeklyStats.motivationalMessage,
                        color = if (weeklyStats.bookedVisitsCount >= weeklyStats.goal) EmeraldSuccess else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (weeklyStats.bookedVisitsCount >= weeklyStats.goal) FontWeight.Bold else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Direct Action Buttons inside Header Card (adaptive FlowRow)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenNewVisitDialog(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .let { if (isLargeFont) it.fillMaxWidth() else it }
                                .heightIn(min = 48.dp)
                                .testTag("psa_btn_add_visit_card")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Agendar Visita", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenPricingSettings,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .let { if (isLargeFont) it.fillMaxWidth() else it }
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ajustes PSA", color = PsaCyanGlow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Date selector horizontal chips
        item {
            Text(
                text = "Selecciona el día de agenda:",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uniqueDates) { (dateKey, display) ->
                    val isSelected = (dateKey == selectedDateKey)
                    val shortLabel = display.split(" ").take(2).joinToString(" ")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PsaCyanPrimary else SlateCard)
                            .border(
                                1.dp,
                                if (isSelected) PsaCyanGlow else SlateCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                viewModel.setSelectedPsaDate(dateKey)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("psa_date_chip_$dateKey")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = shortLabel,
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = dateKey,
                                color = if (isSelected) Color(0xFF083344) else TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Summary Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateCardBorder.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Disponibilidad del Día",
                            color = PsaCyanGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "$freeSlotsCount turnos libres para visitas",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (freeSlotsCount > 0) EmeraldSuccess.copy(alpha = 0.16f) else RoseError.copy(alpha = 0.16f))
                            .border(1.dp, if (freeSlotsCount > 0) EmeraldSuccess.copy(alpha = 0.5f) else RoseError.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (freeSlotsCount > 0) "🟢 $freeSlotsCount Libres" else "🔴 Completo",
                            color = if (freeSlotsCount > 0) EmeraldSuccess else RoseError,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Legend (adaptive FlowRow)
        item {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "🟢 Libre", fontSize = 11.sp, color = EmeraldSuccess)
                Text(text = "🔴 Agendado", fontSize = 11.sp, color = RoseError)
                Text(text = "🛡️ Margen", fontSize = 11.sp, color = AmberDelay)
                Text(text = "⚪ Bloqueado", fontSize = 11.sp, color = TextMuted)
            }
        }

        // List of Slots
        items(currentDaySlots) { slot ->
            PsaSlotItemCard(
                slot = slot,
                allVisits = allVisits,
                onToggleBlock = {
                    viewModel.togglePsaSlotBlocked(context, slot.id)
                },
                onBookSlot = {
                    onOpenNewVisitDialog(slot)
                },
                onStartDemo = onStartDemo,
                onRegisterPostSaleFromVisit = { visit -> openPostSaleForm(visit) }
            )
        }

        // --- SECCIÓN RECAMBIOS Y POSVENTA ---
        item {
            Spacer(modifier = Modifier.height(6.dp))
            val nowMs = System.currentTimeMillis()
            val urgentCount = postSaleList.count { it.getUrgency(nowMs) != ReplacementUrgency.AL_DIA }
            val filteredPostSale = remember(postSaleList, showUrgentPostSaleOnly, nowMs) {
                val base = if (showUrgentPostSaleOnly) {
                    postSaleList.filter { it.getUrgency(nowMs) != ReplacementUrgency.AL_DIA }
                } else {
                    postSaleList
                }
                base.sortedBy { it.nextReplacementAtMs }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(
                    1.dp,
                    if (urgentCount > 0) AmberDelay.copy(alpha = 0.55f) else PsaCyanPrimary.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("psa_post_sale_section")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (urgentCount > 0) AmberDelay else PsaCyanGlow,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Recambios y Posventa (${postSaleList.size})",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (urgentCount > 0) "$urgentCount recambio(s) por atender o vencidos" else "Seguimiento de núcleos y filtros",
                                    color = if (urgentCount > 0) AmberDelay else TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Button(
                            onClick = { openPostSaleForm(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("psa_btn_add_post_sale")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Registrar Posventa", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = !showUrgentPostSaleOnly,
                            onClick = { showUrgentPostSaleOnly = false },
                            label = { Text("Todos (${postSaleList.size})", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PsaCyanPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                        FilterChip(
                            selected = showUrgentPostSaleOnly,
                            onClick = { showUrgentPostSaleOnly = true },
                            label = { Text("Urgentes ($urgentCount)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberDelay,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }

                    if (filteredPostSale.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateBackground)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = if (showUrgentPostSaleOnly) {
                                    "No hay recambios vencidos o próximos dentro de los próximos 15 días."
                                } else {
                                    "Registra ventas e instalaciones aquí o desde Cotizar / Visitas para recibir avisos automáticos de recambio cada 6, 12, 24 o 36 meses."
                                },
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredPostSale.forEach { rec ->
                                val urgency = rec.getUrgency(nowMs)
                                val daysLeft = rec.daysUntilReplacement(nowMs)
                                val badgeColor = when (urgency) {
                                    ReplacementUrgency.VENCIDO -> RoseError
                                    ReplacementUrgency.PROXIMO -> AmberDelay
                                    ReplacementUrgency.AL_DIA -> EmeraldSuccess
                                }
                                val badgeText = when (urgency) {
                                    ReplacementUrgency.VENCIDO -> "VENCIDO (${kotlin.math.abs(daysLeft)}d)"
                                    ReplacementUrgency.PROXIMO -> if (daysLeft == 0) "HOY" else "PROXIMO (${daysLeft}d)"
                                    ReplacementUrgency.AL_DIA -> "AL DÍA (${daysLeft}d)"
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SlateBackground)
                                        .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                        .testTag("post_sale_card_${rec.id}")
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = "👤 ${rec.customerName}",
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "💧 ${rec.purifierName}",
                                                    color = PsaCyanGlow,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "📅 Próximo recambio: ${dateFmt.format(Date(rec.nextReplacementAtMs))} (cada ${rec.replacementCycleMonths}m)",
                                                    color = TextSecondary,
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(badgeColor.copy(alpha = 0.16f))
                                                    .border(1.dp, badgeColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = badgeText,
                                                    color = badgeColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf(6, 12, 24, 36).forEach { m ->
                                                FilterChip(
                                                    selected = rec.replacementCycleMonths == m,
                                                    onClick = { viewModel.updatePostSaleCycle(rec, m) },
                                                    label = { Text("${m}m", fontSize = 11.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = PsaCyanPrimary,
                                                        selectedLabelColor = Color.Black
                                                    )
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
                                                    val waMsg = "Hola ${rec.customerName}! Soy tu distribuidor PSA. Te escribo para recordarte que ya corresponde el recambio de filtro/núcleo de tu ${rec.purifierName} para seguir manteniendo el agua 100% pura. ¿Qué día te queda cómodo coordinarlo?"
                                                    val cleanPhone = rec.customerPhone.replace(Regex("[^0-9]"), "")
                                                    try {
                                                        val uri = if (cleanPhone.length >= 8) {
                                                            Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=" + Uri.encode(waMsg))
                                                        } else {
                                                            Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(waMsg))
                                                        }
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                                    } catch (e: Exception) {
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            putExtra(Intent.EXTRA_TEXT, waMsg)
                                                        }
                                                        context.startActivity(Intent.createChooser(shareIntent, "Enviar WhatsApp de Recambio"))
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) {
                                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("WhatsApp Recambio", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.markPostSaleReplacementCompleted(rec)
                                                    Toast.makeText(context, "✅ Recambio registrado; próximo ciclo reprogramado", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.6f)),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Marcar Realizado", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.deletePostSaleInstallation(rec.id)
                                                    Toast.makeText(context, "Registro de posventa eliminado", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Eliminar registro posventa", tint = RoseError, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showNewPostSaleDialog) {
        val purifiers = remember(pricingConfig) { pricingConfig.getAllPurifiers() }
        AlertDialog(
            onDismissRequest = { showNewPostSaleDialog = false },
            containerColor = SlateCard,
            title = {
                Text(
                    text = "Registrar Equipo y Recambio",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = postSaleCustomerName,
                        onValueChange = { postSaleCustomerName = it },
                        label = { Text("Cliente *") },
                        placeholder = { Text("Ej: Laura Méndez") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("post_sale_customer_input")
                    )
                    OutlinedTextField(
                        value = postSaleCustomerPhone,
                        onValueChange = { postSaleCustomerPhone = it },
                        label = { Text("Teléfono WhatsApp") },
                        placeholder = { Text("Ej: 54911...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Equipo instalado:", color = TextSecondary, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(purifiers) { model ->
                            FilterChip(
                                selected = postSalePurifierId == model.id,
                                onClick = {
                                    postSalePurifierId = model.id
                                    postSalePurifierName = model.name
                                },
                                label = { Text(model.name.replace("PSA ", ""), fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PsaCyanPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                    Text("Ciclo de recambio:", color = TextSecondary, fontSize = 12.sp)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(6, 12, 24, 36).forEach { months ->
                            FilterChip(
                                selected = postSaleCycleMonths == months,
                                onClick = { postSaleCycleMonths = months },
                                label = { Text("$months meses", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldSuccess,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                    OutlinedTextField(
                        value = postSaleNotes,
                        onValueChange = { postSaleNotes = it },
                        label = { Text("Notas (opcional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
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
                        if (postSaleCustomerName.isBlank()) {
                            Toast.makeText(context, "Ingresa el nombre del cliente", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.registerPostSaleInstallation(
                            customerName = postSaleCustomerName,
                            customerPhone = postSaleCustomerPhone,
                            purifierId = postSalePurifierId,
                            purifierName = postSalePurifierName,
                            replacementCycleMonths = postSaleCycleMonths,
                            visitId = postSaleDialogVisit?.id ?: "",
                            notes = postSaleNotes
                        )
                        Toast.makeText(context, "✅ Posventa registrada con alerta cada $postSaleCycleMonths meses", Toast.LENGTH_SHORT).show()
                        showNewPostSaleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPostSaleDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PsaSlotItemCard(
    slot: PsaTimeSlot,
    allVisits: List<PsaHomeVisit>,
    onToggleBlock: () -> Unit,
    onBookSlot: (PsaTimeSlot) -> Unit,
    onStartDemo: (PsaHomeVisit) -> Unit,
    onRegisterPostSaleFromVisit: (PsaHomeVisit) -> Unit = {}
) {
    val visit = allVisits.find { it.id == slot.visitId }
    val context = LocalContext.current

    val borderColor = when (slot.status) {
        PsaSlotStatus.FREE -> EmeraldSuccess.copy(alpha = 0.28f)
        PsaSlotStatus.BOOKED -> RoseError.copy(alpha = 0.40f)
        PsaSlotStatus.BLOCKED -> SlateCardBorder.copy(alpha = 0.5f)
        PsaSlotStatus.BUFFER -> AmberDelay.copy(alpha = 0.35f)
    }

    val cardBg = when (slot.status) {
        PsaSlotStatus.FREE -> SlateCard
        PsaSlotStatus.BOOKED -> Color(0xFF1E1621)
        PsaSlotStatus.BLOCKED -> SlateBackground
        PsaSlotStatus.BUFFER -> Color(0xFF1F1B14)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("psa_slot_${slot.startTime.replace(":", "")}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⏰ ${slot.startTime} - ${slot.endTime}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                }

                // Status chip button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (slot.status) {
                                PsaSlotStatus.FREE -> EmeraldSuccess.copy(alpha = 0.14f)
                                PsaSlotStatus.BOOKED -> RoseError.copy(alpha = 0.18f)
                                PsaSlotStatus.BLOCKED -> Color.White.copy(alpha = 0.06f)
                                PsaSlotStatus.BUFFER -> AmberDelay.copy(alpha = 0.18f)
                            }
                        )
                        .clickable {
                            if (slot.status != PsaSlotStatus.BOOKED && slot.status != PsaSlotStatus.BUFFER) {
                                onToggleBlock()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (slot.status) {
                            PsaSlotStatus.FREE -> "🟢 Disponible"
                            PsaSlotStatus.BOOKED -> "🔴 Agendado"
                            PsaSlotStatus.BLOCKED -> "⚪ Bloqueado"
                            PsaSlotStatus.BUFFER -> "🛡️ Margen (Traslado)"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (slot.status) {
                            PsaSlotStatus.FREE -> EmeraldSuccess
                            PsaSlotStatus.BOOKED -> RoseError
                            PsaSlotStatus.BLOCKED -> TextMuted
                            PsaSlotStatus.BUFFER -> AmberDelay
                        }
                    )
                }
            }

            if (slot.status == PsaSlotStatus.BOOKED && visit != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "👤 ${visit.customerName} (${visit.phone})",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "📍 ${visit.address} - ${visit.neighborhood}",
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                    Text(
                        text = "💧 Interés: ${visit.modelInterest} | ${visit.status.name}",
                        color = PsaCyanGlow,
                        fontSize = 11.5.sp
                    )

                    if (visit.diagnosticTimestampMs > 0L) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isWell = visit.waterSource == DemoWaterSource.AGUA_DE_POZO.name
                        val metricLine = if (isWell) {
                            "Pozo • Dureza: ${visit.hardnessPpm} ppm • Consumidos: ${String.format(Locale.US, "%.1f", visit.realLitersPerDay)} L/día"
                        } else {
                            "Corriente • Cloro: ${String.format(Locale.US, "%.1f", visit.chlorinePpm)} ppm • Consumidos: ${String.format(Locale.US, "%.1f", visit.realLitersPerDay)} L/día"
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF062133))
                                .border(1.dp, PsaCyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "🔬 Diagnóstico Técnico Guardado",
                                        color = PsaCyanGlow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    if (visit.testPhotoPath.isNotBlank()) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Foto del test guardada",
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Text(text = metricLine, color = TextPrimary, fontSize = 11.sp)
                                if (visit.cartSummary.isNotBlank()) {
                                    Text(text = "Equipos: ${visit.cartSummary}", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onStartDemo(visit) },
                            colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.LocalDrink, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Iniciar Demo", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val reminderText = "Hola ${visit.customerName}, ¡buen día! Te recuerdo que en 2 horas pasamos por tu domicilio (${visit.address}) a realizar la demostración y prueba de cloro en tu canilla de 10 min 🚰💧 ¡Nos vemos pronto!"
                                val cleanPhone = visit.phone.replace(Regex("[^0-9]"), "")
                                try {
                                    val uri = if (cleanPhone.length >= 8) {
                                        Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=" + Uri.encode(reminderText))
                                    } else {
                                        Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(reminderText))
                                    }
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                } catch (e: Exception) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, reminderText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Enviar Recordatorio de Visita"))
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.55f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("⏰ Recordar 2hs", color = AmberDelay, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onRegisterPostSaleFromVisit(visit) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Posventa", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (slot.status == PsaSlotStatus.BUFFER) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = AmberDelay,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Margen automático de traslado",
                        color = AmberDelay,
                        fontSize = 11.5.sp
                    )
                }
            } else if (slot.status == PsaSlotStatus.FREE) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Turno disponible",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Button(
                        onClick = { onBookSlot(slot) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agendar Aquí", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: GUIONES & WHATSAPP CON INYECCIÓN DE HORARIOS
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PsaScriptsTabContent(
    context: Context,
    viewModel: MainViewModel,
    scheduleSlots: List<PsaTimeSlot>
) {
    var prospectName by remember { mutableStateOf("") }
    var prospectZone by remember { mutableStateOf("") }
    var prospectPhone by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf(PsaMessageTone.CALIDO_HUMANO) }

    val nextAvailableSlots = remember(scheduleSlots) {
        PsaScheduleManager.getNextAvailableSlots(scheduleSlots, 2)
    }

    // Load custom saved proposal if available, else generate
    var customSavedProposal by remember {
        mutableStateOf(PsaScheduleManager.getCustomProposal(context))
    }

    var editableMessageText by remember {
        mutableStateOf(
            customSavedProposal ?: PsaScheduleManager.generateAppointmentProposalMessage(
                clientName = prospectName,
                neighborhood = prospectZone,
                availableSlots = nextAvailableSlots,
                tone = selectedTone
            )
        )
    }

    // Script editing dialog state
    var editingScriptTarget by remember { mutableStateOf<com.example.data.model.PsaScriptTemplate?>(null) }
    var scriptEditorText by remember { mutableStateOf("") }
    var refreshScriptsTrigger by remember { mutableIntStateOf(0) }

    // When name, zone, tone, or slots change, if user has not customized, update suggestion
    LaunchedEffect(prospectName, prospectZone, selectedTone, nextAvailableSlots) {
        if (customSavedProposal == null) {
            editableMessageText = PsaScheduleManager.generateAppointmentProposalMessage(
                clientName = prospectName,
                neighborhood = prospectZone,
                availableSlots = nextAvailableSlots,
                tone = selectedTone
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Smart Dynamic Generator Card (100% Human & Editable)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF032238)),
                border = BorderStroke(1.dp, PsaCyanPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mensaje Humano de Agendamiento",
                                color = PsaCyanGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (customSavedProposal != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EmeraldSuccess.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "💾 Personalizado",
                                    color = EmeraldSuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Human Tone Selector Chips (wrapped cleanly)
                    Text(text = "Tono del mensaje:", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PsaMessageTone.entries.forEach { tone ->
                            val isSelected = (selectedTone == tone)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PsaCyanPrimary else Color(0xFF071F30))
                                    .border(1.dp, if (isSelected) PsaCyanGlow else SlateCardBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedTone = tone
                                        customSavedProposal = null
                                        editableMessageText = PsaScheduleManager.generateAppointmentProposalMessage(
                                            clientName = prospectName,
                                            neighborhood = prospectZone,
                                            availableSlots = nextAvailableSlots,
                                            tone = tone
                                        )
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tone.label,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val isLargeFont = LocalIsLargeFontMode.current
                    if (isLargeFont) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = prospectName,
                                onValueChange = { prospectName = it },
                                label = { Text("Nombre Cliente", fontSize = 11.5.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("psa_input_name"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PsaCyanPrimary,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            OutlinedTextField(
                                value = prospectZone,
                                onValueChange = { prospectZone = it },
                                label = { Text("Barrio / Zona", fontSize = 11.5.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("psa_input_zone"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PsaCyanPrimary,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = prospectName,
                                onValueChange = { prospectName = it },
                                label = { Text("Nombre Cliente", fontSize = 11.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("psa_input_name"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PsaCyanPrimary,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = prospectZone,
                                onValueChange = { prospectZone = it },
                                label = { Text("Barrio / Zona", fontSize = 11.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("psa_input_zone"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PsaCyanPrimary,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = prospectPhone,
                        onValueChange = { prospectPhone = it },
                        label = { Text("Teléfono / WhatsApp (ej. 54911223344)", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("psa_input_phone"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanPrimary,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Editable Live Message Box
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "✏️ Mensaje listo para enviar:", color = TextMuted, fontSize = 11.5.sp)

                        // Quick emojis
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("😊", "💧", "🚰", "👉", "✨").forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable {
                                            editableMessageText = "$editableMessageText $emoji"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = emoji, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = editableMessageText,
                        onValueChange = {
                            editableMessageText = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("psa_editable_proposal_text"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = PsaCyanPrimary.copy(alpha = 0.4f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color.Black.copy(alpha = 0.35f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Customization actions: Save as preferred or reset
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                PsaScheduleManager.saveCustomProposal(context, editableMessageText)
                                customSavedProposal = editableMessageText
                                Toast.makeText(context, "💾 Guardado como tu mensaje preferido", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 34.dp)
                        ) {
                            Text("💾 Guardar mi versión", fontSize = 10.5.sp, color = PsaCyanGlow)
                        }

                        OutlinedButton(
                            onClick = {
                                PsaScheduleManager.clearCustomProposal(context)
                                customSavedProposal = null
                                editableMessageText = PsaScheduleManager.generateAppointmentProposalMessage(
                                    clientName = prospectName,
                                    neighborhood = prospectZone,
                                    availableSlots = nextAvailableSlots,
                                    tone = selectedTone
                                )
                                Toast.makeText(context, "🔄 Restablecido al sugerido de IA", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 34.dp)
                        ) {
                            Text("🔄 Restablecer", fontSize = 10.5.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                openWhatsApp(context, prospectPhone, editableMessageText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .testTag("psa_btn_send_whatsapp")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enviar por WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                copyToClipboard(context, editableMessageText)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, PsaCyanPrimary),
                            modifier = Modifier.heightIn(min = 42.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Section Title: Proven Library with In-App Customization
        item {
            Text(
                text = "📚 Guiones PSA",
                color = PsaCyanGlow,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Scripts items with in-app modification capability
        items(PsaScheduleManager.PSA_SCRIPTS) { script ->
            // Check if there is a customized version saved in preferences
            val currentBody = remember(refreshScriptsTrigger) {
                PsaScheduleManager.getCustomScript(context, script.id, script.templateBody)
            }
            val isCustomized = (currentBody != script.templateBody)

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, if (isCustomized) PsaCyanPrimary.copy(alpha = 0.6f) else SlateCardBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = script.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isCustomized) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PsaCyanPrimary.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "✏️ Editado",
                                        color = PsaCyanGlow,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EmeraldSuccess.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${script.successRate}% Cierre",
                                    color = EmeraldSuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = script.description, color = TextSecondary, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(text = currentBody, color = TextPrimary, fontSize = 11.sp, lineHeight = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                editingScriptTarget = script
                                scriptEditorText = currentBody
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Modificar", fontSize = 11.5.sp, color = PsaCyanGlow)
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val filled = currentBody.replace("{nombre}", prospectName.ifBlank { "vecino/a" })
                                    copyToClipboard(context, filled)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiar", fontSize = 11.5.sp, color = TextSecondary)
                            }

                            Button(
                                onClick = {
                                    val filled = currentBody.replace("{nombre}", prospectName.ifBlank { "vecino/a" })
                                    openWhatsApp(context, prospectPhone, filled)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Enviar", fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Modal Dialog to edit any script inside the app
    val targetToEdit = editingScriptTarget
    if (targetToEdit != null) {
        AlertDialog(
            onDismissRequest = { editingScriptTarget = null },
            title = {
                Text(
                    text = "✏️ Modificar: ${targetToEdit.title}",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Usa {nombre} para el nombre del cliente.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = scriptEditorText,
                        onValueChange = { scriptEditorText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PsaScheduleManager.saveCustomScript(context, targetToEdit.id, scriptEditorText)
                        refreshScriptsTrigger++
                        editingScriptTarget = null
                        Toast.makeText(context, "✅ Guión actualizado con éxito", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary)
                ) {
                    Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            PsaScheduleManager.saveCustomScript(context, targetToEdit.id, targetToEdit.templateBody)
                            refreshScriptsTrigger++
                            editingScriptTarget = null
                            Toast.makeText(context, "🔄 Guión restablecido al original", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Restablecer", color = AmberDelay, fontSize = 11.sp)
                    }

                    OutlinedButton(onClick = { editingScriptTarget = null }) {
                        Text("Cancelar", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            },
            containerColor = SlateCard
        )
    }
}

// -------------------------------------------------------------
// TAB 3: DEMOSTRADOR EN CASA (EN VIVO EN LA MESA DEL CLIENTE)
// -------------------------------------------------------------
// El contenido visual de Demostración para clientes vive ahora en PsaClientDemoSection.kt (sin mensajes ni botones de vendedor).

// -------------------------------------------------------------
// TAB 4: MEMORIA DE IA ESPECIALIZADA EN PSA
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PsaAiMemoryTabContent(
    context: Context,
    viewModel: MainViewModel,
    allKnowledge: List<com.example.data.model.SalesKnowledgeItem>
) {
    val psaKnowledge = remember(allKnowledge) {
        allKnowledge.filter { it.category.startsWith("PSA_") }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF032238)),
                border = BorderStroke(1.dp, PsaCyanPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Respuestas PSA probadas (califica ⭐ las ganadoras)",
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (psaKnowledge.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Cargando patrones de venta PSA...", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(psaKnowledge) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, SlateCardBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "⚡ ${item.triggerContext}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PsaCyanPrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "★ ${item.successScore} pts",
                                    color = PsaCyanGlow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = item.winningContent,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Usado ${item.usageCount} veces",
                                color = TextMuted,
                                fontSize = 11.sp
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        copyToClipboard(context, item.winningContent)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copiar", fontSize = 11.sp, color = PsaCyanGlow)
                                }

                                Button(
                                    onClick = {
                                        viewModel.rateSalesKnowledge(item.id, 1)
                                        Toast.makeText(context, "⭐ ¡Efectividad incrementada (+1)!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ganadora (+1)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// -------------------------------------------------------------
// NEW VISIT DIALOG
// -------------------------------------------------------------
// -------------------------------------------------------------
// NEW VISIT DIALOG (Agendar Visita a Domicilio con Horario Libre o Predeterminado)
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PsaNewVisitDialog(
    context: Context,
    viewModel: MainViewModel,
    availableSlots: List<PsaTimeSlot>,
    pricingConfig: PsaPricingConfig,
    initialSlot: PsaTimeSlot? = null,
    initialDateKey: String = "",
    onDismiss: () -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayDate = remember { dateFormat.format(Date()) }
    val tomorrowDate = remember {
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        dateFormat.format(c.time)
    }

    var selectedDate by remember {
        mutableStateOf(if (initialDateKey.isNotBlank()) initialDateKey else todayDate)
    }

    // Horario: Turnos Sugeridos vs Horario Libre
    var isCustomTimeMode by remember { mutableStateOf(initialSlot == null && availableSlots.isEmpty()) }
    var selectedSlot by remember {
        mutableStateOf(initialSlot ?: availableSlots.firstOrNull { it.dateKey == selectedDate } ?: availableSlots.firstOrNull())
    }
    var customTimeText by remember {
        mutableStateOf(initialSlot?.startTime ?: "16:00")
    }

    val allPurifiers = remember(pricingConfig) { pricingConfig.getAllPurifiers() }
    var selectedPurifier by remember {
        mutableStateOf(allPurifiers.firstOrNull()?.name ?: "PSA Senior 4 (Sobre Mesada)")
    }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📅 Agendar Visita a Domicilio",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                item {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Nombre y Apellido", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono / WhatsApp", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                item {
                    val isLargeFont = LocalIsLargeFontMode.current
                    if (isLargeFont) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Calle y Altura", fontSize = 11.5.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PsaCyanGlow,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            OutlinedTextField(
                                value = neighborhood,
                                onValueChange = { neighborhood = it },
                                label = { Text("Barrio", fontSize = 11.5.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PsaCyanGlow,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Calle y Altura", fontSize = 11.sp) },
                                modifier = Modifier.weight(1.3f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PsaCyanGlow,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            OutlinedTextField(
                                value = neighborhood,
                                onValueChange = { neighborhood = it },
                                label = { Text("Barrio", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PsaCyanGlow,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    }
                }

                // Selector de Fecha
                item {
                    Text(text = "Fecha de la Visita:", color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedDate == todayDate) PsaCyanPrimary else SlateCard)
                                .border(1.dp, if (selectedDate == todayDate) PsaCyanGlow else SlateCardBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedDate = todayDate }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Hoy ($todayDate)",
                                color = if (selectedDate == todayDate) Color.Black else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedDate == tomorrowDate) PsaCyanPrimary else SlateCard)
                                .border(1.dp, if (selectedDate == tomorrowDate) PsaCyanGlow else SlateCardBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedDate = tomorrowDate }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Mañana ($tomorrowDate)",
                                color = if (selectedDate == tomorrowDate) Color.Black else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Selector de Horario (Turnos vs Libre)
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "Horario de la Visita:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (!isCustomTimeMode) PsaCyanPrimary else Color(0xFF0F172A))
                                    .clickable { isCustomTimeMode = false }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Turnos",
                                    color = if (!isCustomTimeMode) Color.Black else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isCustomTimeMode) PsaCyanPrimary else Color(0xFF0F172A))
                                    .clickable { isCustomTimeMode = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Libre",
                                    color = if (isCustomTimeMode) Color.Black else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (!isCustomTimeMode) {
                        val slotsForDate = availableSlots.filter { it.dateKey == selectedDate }
                        val displaySlots = if (slotsForDate.isNotEmpty()) slotsForDate else availableSlots
                        if (displaySlots.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(displaySlots.take(8)) { slot ->
                                    val isSelected = (slot.id == selectedSlot?.id)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) PsaCyanPrimary else SlateCard)
                                            .border(1.dp, if (isSelected) PsaCyanGlow else SlateCardBorder, RoundedCornerShape(6.dp))
                                            .clickable {
                                                selectedSlot = slot
                                                selectedDate = slot.dateKey
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "${slot.displayDate.split(" ").take(2).joinToString(" ")} ${slot.startTime}hs",
                                            color = if (isSelected) Color.Black else TextPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(text = "No hay turnos libres registrados para este día. Usa la pestaña 'Horario Libre'.", color = RoseError, fontSize = 10.sp)
                        }
                    } else {
                        // Horario libre
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF021727), RoundedCornerShape(8.dp))
                                .border(1.dp, PsaCyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customTimeText,
                                onValueChange = { customTimeText = it },
                                label = { Text("Hora de la visita (HH:mm)", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("ej. 10:15 o 17:30", color = TextMuted, fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PsaCyanGlow,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("09:30", "11:00", "15:00", "16:30", "18:00", "19:30").forEach { preset ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (customTimeText == preset) PsaCyanPrimary else SlateCard)
                                            .clickable { customTimeText = preset }
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = preset,
                                            color = if (customTimeText == preset) Color.Black else TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Modelo de interés
                item {
                    Text(text = "Modelo de Purificador de Interés:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allPurifiers) { model ->
                            val isSelected = (model.name == selectedPurifier)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) PsaCyanPrimary else SlateCard)
                                    .border(1.dp, if (isSelected) PsaCyanGlow else SlateCardBorder, RoundedCornerShape(6.dp))
                                    .clickable { selectedPurifier = model.name }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = model.name.split(" (").first(),
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas adicionales (ej. consumen 4 bidones/sem)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTime = if (isCustomTimeMode) customTimeText.trim() else selectedSlot?.startTime.orEmpty()
                    if (customerName.isBlank() || address.isBlank() || finalTime.isBlank()) {
                        Toast.makeText(context, "Por favor completa nombre, dirección y horario de visita", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.bookPsaVisit(
                        context = context,
                        customerName = customerName,
                        phone = phone,
                        address = address,
                        neighborhood = neighborhood,
                        scheduledDate = selectedDate,
                        scheduledTime = finalTime,
                        modelInterest = selectedPurifier,
                        notes = notes
                    )
                    Toast.makeText(context, "✅ Visita agendada para el $selectedDate a las ${finalTime}hs", Toast.LENGTH_LONG).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary)
            ) {
                Text("Confirmar y Agendar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SlateCard
    )
}

// -------------------------------------------------------------
// PSA PRICING & SCHEDULE SETTINGS DIALOG (Precios Base, Turnos y Catálogo)
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PsaPricingSettingsDialog(
    context: Context,
    activeCountry: PsaCountryConfig = PsaCountryRegistry.defaultCountry(),
    initialConfig: PsaPricingConfig,
    initialDailySlots: List<Pair<String, String>>,
    initialFinancingConfig: com.example.data.repository.PsaFinancingConfig = com.example.data.repository.PsaFinancingConfig(),
    adminCloudSnapshot: com.example.util.PsaCountryPricingSnapshot? = null,
    onApplyAdminPrices: () -> Boolean = { false },
    onRestoreBaseline: () -> Unit = {},
    onOpenSmartImport: () -> Unit = {},
    onOpenDipCredentials: () -> Unit = {},
    onCheckOfficialWeb: () -> Unit = {},
    onSave: (PsaPricingConfig, List<Pair<String, String>>, com.example.data.repository.PsaFinancingConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val isLargeFont = LocalIsLargeFontMode.current
    var settingsTab by remember { mutableStateOf(0) }

    // Tab 0: Precios Base PSA, Envases y Catálogo
    var senior4PriceText by remember { mutableStateOf(initialConfig.senior4Price.toLong().toString()) }
    var senikPriceText by remember { mutableStateOf(initialConfig.senikPrice.toLong().toString()) }
    var quantumPriceText by remember { mutableStateOf(initialConfig.quantumPrice.toLong().toString()) }
    var cromaPriceText by remember { mutableStateOf(initialConfig.cromaPrice.toLong().toString()) }
    var veroPriceText by remember { mutableStateOf(initialConfig.veroPrice.toLong().toString()) }
    var s1000PriceText by remember { mutableStateOf(initialConfig.s1000Price.toLong().toString()) }
    var miniPriceText by remember { mutableStateOf(initialConfig.miniPrice.toLong().toString()) }
    var c3PriceText by remember { mutableStateOf(initialConfig.c3Price.toLong().toString()) }
    var duchaPriceText by remember { mutableStateOf(initialConfig.duchaPrice.toLong().toString()) }
    var piletaPriceText by remember { mutableStateOf(initialConfig.piletaPrice.toLong().toString()) }
    var airePriceText by remember { mutableStateOf(initialConfig.airePrice.toLong().toString()) }
    var descalcificadorPriceText by remember { mutableStateOf(initialConfig.descalcificadorPrice.toLong().toString()) }
    var domusPriceText by remember { mutableStateOf(initialConfig.domusPrice.toLong().toString()) }
    var maximaPriceText by remember { mutableStateOf(initialConfig.maximaPrice.toLong().toString()) }
    var ropotPriceText by remember { mutableStateOf(initialConfig.ropotPrice.toLong().toString()) }
    var rotechPriceText by remember { mutableStateOf(initialConfig.rotechPrice.toLong().toString()) }
    var repuestoSeniorPriceText by remember { mutableStateOf(initialConfig.repuestoSeniorPrice.toLong().toString()) }
    var bidonPriceText by remember { mutableStateOf(initialConfig.bidonPrice.toLong().toString()) }
    var bottlePriceText by remember { mutableStateOf(initialConfig.bottlePrice.toLong().toString()) }
    var installmentsText by remember { mutableStateOf(initialConfig.installmentsCount.toString()) }
    var defaultLifespanText by remember { mutableStateOf(initialConfig.defaultLifespanMonths.toString()) }
    var comparisonType by remember { mutableStateOf(initialConfig.defaultComparisonType) }
    var customPurifiersList by remember { mutableStateOf(initialConfig.customPurifiers) }
    var showAddPurifierModal by remember { mutableStateOf(false) }
    var newPurifierName by remember { mutableStateOf("") }
    var newPurifierPriceText by remember { mutableStateOf("") }
    var newPurifierInstallmentsText by remember { mutableStateOf("12") }
    var newPurifierLifespanText by remember { mutableStateOf("36") }
    var newPurifierDesc by remember { mutableStateOf("") }

    // Tab 1: Agenda y Horarios de Turnos
    var weeklyVisitsGoalText by remember { mutableStateOf(initialConfig.weeklyVisitsGoal.toString()) }
    var dailySlotsList by remember { mutableStateOf(initialDailySlots.toMutableList()) }
    var newSlotStartText by remember { mutableStateOf("") }

    // Tab 2: Financiación y Cuotas en Efectivo
    var interest3Text by remember { mutableStateOf(initialFinancingConfig.defaultInterest3Months.toString()) }
    var interest6Text by remember { mutableStateOf(initialFinancingConfig.defaultInterest6Months.toString()) }
    var interest12Text by remember { mutableStateOf(initialFinancingConfig.defaultInterest12Months.toString()) }
    var interest18Text by remember { mutableStateOf(initialFinancingConfig.defaultInterest18Months.toString()) }
    var advancePctText by remember { mutableStateOf(initialFinancingConfig.suggestedAdvancePercentage.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "⚙️ Configuración General PSA",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 3 Pestañas limpias solicitadas por el usuario: Precios, Agenda y Financiación
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF021727))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (settingsTab == 0) PsaCyanPrimary else Color.Transparent)
                            .clickable { settingsTab = 0 }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🏷️ Precios",
                            color = if (settingsTab == 0) Color.Black else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (settingsTab == 1) PsaCyanPrimary else Color.Transparent)
                            .clickable { settingsTab = 1 }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📅 Agenda",
                            color = if (settingsTab == 1) Color.Black else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (settingsTab == 2) PsaCyanPrimary else Color.Transparent)
                            .clickable { settingsTab = 2 }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💳 Financiación",
                            color = if (settingsTab == 2) Color.Black else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (settingsTab) {
                        0 -> {
                            // TAB 0: PRECIOS OFICIALES PSA Y CATÁLOGO
                            item {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val base = PsaCountryRegistry.defaultPricingForCountry(activeCountry.code)
                                            senior4PriceText = base.senior4Price.toLong().toString()
                                            senikPriceText = base.senikPrice.toLong().toString()
                                            quantumPriceText = base.quantumPrice.toLong().toString()
                                            cromaPriceText = base.cromaPrice.toLong().toString()
                                            veroPriceText = base.veroPrice.toLong().toString()
                                            s1000PriceText = base.s1000Price.toLong().toString()
                                            miniPriceText = base.miniPrice.toLong().toString()
                                            c3PriceText = base.c3Price.toLong().toString()
                                            duchaPriceText = base.duchaPrice.toLong().toString()
                                            piletaPriceText = base.piletaPrice.toLong().toString()
                                            airePriceText = base.airePrice.toLong().toString()
                                            descalcificadorPriceText = base.descalcificadorPrice.toLong().toString()
                                            domusPriceText = base.domusPrice.toLong().toString()
                                            maximaPriceText = base.maximaPrice.toLong().toString()
                                            ropotPriceText = base.ropotPrice.toLong().toString()
                                            rotechPriceText = base.rotechPrice.toLong().toString()
                                            repuestoSeniorPriceText = base.repuestoSeniorPrice.toLong().toString()
                                            bidonPriceText = base.bidonPrice.toLong().toString()
                                            bottlePriceText = base.bottlePrice.toLong().toString()
                                            onRestoreBaseline()
                                            Toast.makeText(
                                                context,
                                                "PVP base ${activeCountry.code} restaurado",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.6f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.heightIn(min = 38.dp)
                                    ) {
                                        Text("Restaurar PVP ${activeCountry.code}", color = AmberDelay, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = onOpenSmartImport,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.55f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.heightIn(min = 38.dp)
                                    ) {
                                        Text("Pegar / PDF", color = PsaCyanGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = onOpenDipCredentials,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.heightIn(min = 38.dp)
                                    ) {
                                        Text("Clave DIP", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = onCheckOfficialWeb,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.6f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .heightIn(min = 38.dp)
                                            .testTag("btn_verify_official_web_pvp")
                                    ) {
                                        Text("Verificar Web", color = PurpleNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (adminCloudSnapshot != null) {
                                        OutlinedButton(
                                            onClick = {
                                                val p = adminCloudSnapshot.pricingConfig
                                                val f = adminCloudSnapshot.financingConfig
                                                senior4PriceText = p.senior4Price.toLong().toString()
                                                senikPriceText = p.senikPrice.toLong().toString()
                                                quantumPriceText = p.quantumPrice.toLong().toString()
                                                cromaPriceText = p.cromaPrice.toLong().toString()
                                                veroPriceText = p.veroPrice.toLong().toString()
                                                s1000PriceText = p.s1000Price.toLong().toString()
                                                miniPriceText = p.miniPrice.toLong().toString()
                                                c3PriceText = p.c3Price.toLong().toString()
                                                duchaPriceText = p.duchaPrice.toLong().toString()
                                                piletaPriceText = p.piletaPrice.toLong().toString()
                                                airePriceText = p.airePrice.toLong().toString()
                                                descalcificadorPriceText = p.descalcificadorPrice.toLong().toString()
                                                domusPriceText = p.domusPrice.toLong().toString()
                                                maximaPriceText = p.maximaPrice.toLong().toString()
                                                ropotPriceText = p.ropotPrice.toLong().toString()
                                                rotechPriceText = p.rotechPrice.toLong().toString()
                                                repuestoSeniorPriceText = p.repuestoSeniorPrice.toLong().toString()
                                                bidonPriceText = p.bidonPrice.toLong().toString()
                                                bottlePriceText = p.bottlePrice.toLong().toString()
                                                installmentsText = p.installmentsCount.toString()
                                                defaultLifespanText = p.defaultLifespanMonths.toString()
                                                comparisonType = p.defaultComparisonType
                                                customPurifiersList = p.customPurifiers
                                                weeklyVisitsGoalText = p.weeklyVisitsGoal.toString()
                                                interest3Text = f.defaultInterest3Months.toString()
                                                interest6Text = f.defaultInterest6Months.toString()
                                                interest12Text = f.defaultInterest12Months.toString()
                                                interest18Text = f.defaultInterest18Months.toString()
                                                advancePctText = f.suggestedAdvancePercentage.toString()
                                                onApplyAdminPrices()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.75f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("btn_apply_admin_prices")
                                        ) {
                                            Text("Usar Precios Admin", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateBackground),
                                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "PVP EQUIPOS PRINCIPALES (${activeCountry.currencyCode})",
                                            color = EmeraldSuccess,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        )

                                        @Composable
                                        fun priceInput(label: String, value: String, onVal: (String) -> Unit) {
                                            OutlinedTextField(
                                                value = value,
                                                onValueChange = { onVal(it.filter { ch -> ch.isDigit() }) },
                                                label = { Text("$label (${activeCountry.currencySymbol})", fontSize = 11.5.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = EmeraldSuccess,
                                                    unfocusedBorderColor = SlateCardBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary
                                                )
                                            )
                                        }

                                        priceInput("PSA Senior 4 - Sobre Mesada", senior4PriceText) { senior4PriceText = it }
                                        priceInput("PSA Senik", senikPriceText) { senikPriceText = it }
                                        priceInput("PSA Quantum", quantumPriceText) { quantumPriceText = it }
                                        priceInput("PSA Senior Clásico / Croma", cromaPriceText) { cromaPriceText = it }
                                        priceInput("PSA Vero", veroPriceText) { veroPriceText = it }
                                        priceInput("PSA S-1000 II", s1000PriceText) { s1000PriceText = it }
                                        priceInput("PSA Mini", miniPriceText) { miniPriceText = it }
                                        priceInput("PSA C3 - Lavatorio / Baño", c3PriceText) { c3PriceText = it }
                                        priceInput("PSA Ducha - Declorador", duchaPriceText) { duchaPriceText = it }
                                        priceInput("PSA Iontrix3 - Pileta", piletaPriceText) { piletaPriceText = it }
                                        priceInput("PSA AirPurifier - Aire", airePriceText) { airePriceText = it }
                                        priceInput("PSA Descalcificador R-200", descalcificadorPriceText) { descalcificadorPriceText = it }
                                        priceInput("PSA Domus - Ablandador", domusPriceText) { domusPriceText = it }
                                        priceInput("PSA Máxima - Entrada", maximaPriceText) { maximaPriceText = it }
                                        priceInput("PSA Ropot - Ósmosis", ropotPriceText) { ropotPriceText = it }
                                        priceInput("PSA ROtech·UV - Ósmosis + UV", rotechPriceText) { rotechPriceText = it }
                                        priceInput("Recambio / Repuesto Núcleo", repuestoSeniorPriceText) { repuestoSeniorPriceText = it }
                                    }
                                }
                            }

                            item {
                                val isLargeFontDialog = LocalIsLargeFontMode.current
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateBackground),
                                    border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.28f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "🚰 GASTOS DE AGUA DE REFERENCIA",
                                            color = AmberDelay,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        )
                                        if (isLargeFontDialog) {
                                            OutlinedTextField(
                                                value = bidonPriceText,
                                                onValueChange = { bidonPriceText = it },
                                                label = { Text("Bidón 20L ($)", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PsaCyanGlow,
                                                    unfocusedBorderColor = SlateCardBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary
                                                )
                                            )
                                            OutlinedTextField(
                                                value = bottlePriceText,
                                                onValueChange = { bottlePriceText = it },
                                                label = { Text("Botella 2L ($)", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PsaCyanGlow,
                                                    unfocusedBorderColor = SlateCardBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary
                                                )
                                            )
                                            OutlinedTextField(
                                                value = installmentsText,
                                                onValueChange = { installmentsText = it },
                                                label = { Text("Cuotas fijas", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PsaCyanGlow,
                                                    unfocusedBorderColor = SlateCardBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary
                                                )
                                            )
                                            OutlinedTextField(
                                                value = defaultLifespanText,
                                                onValueChange = { defaultLifespanText = it },
                                                label = { Text("Vida útil (meses)", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PsaCyanGlow,
                                                    unfocusedBorderColor = SlateCardBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary
                                                )
                                            )
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = bidonPriceText,
                                                    onValueChange = { bidonPriceText = it },
                                                    label = { Text("Bidón 20L ($)", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = PsaCyanGlow,
                                                        unfocusedBorderColor = SlateCardBorder,
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary
                                                    )
                                                )
                                                OutlinedTextField(
                                                    value = bottlePriceText,
                                                    onValueChange = { bottlePriceText = it },
                                                    label = { Text("Botella 2L ($)", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = PsaCyanGlow,
                                                        unfocusedBorderColor = SlateCardBorder,
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary
                                                    )
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = installmentsText,
                                                    onValueChange = { installmentsText = it },
                                                    label = { Text("Cuotas fijas", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = PsaCyanGlow,
                                                        unfocusedBorderColor = SlateCardBorder,
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary
                                                    )
                                                )
                                                OutlinedTextField(
                                                    value = defaultLifespanText,
                                                    onValueChange = { defaultLifespanText = it },
                                                    label = { Text("Vida útil (meses)", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = PsaCyanGlow,
                                                        unfocusedBorderColor = SlateCardBorder,
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Catálogo adicional dinámico
                            item {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "📦 Catálogo Extra:",
                                        color = PsaCyanGlow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Button(
                                        onClick = { showAddPurifierModal = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.heightIn(min = 32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Crear Modelo", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (customPurifiersList.isEmpty()) {
                                item {
                                    Text(
                                        text = "No has creado modelos adicionales. Puedes crear cualquier purificador o combo extra aquí.",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            } else {
                                items(customPurifiersList) { model ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF031E2C))
                                            .border(1.dp, PsaCyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = model.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text(
                                                text = "$${String.format(Locale.US, "%,.0f", model.price)} • ${model.installmentsCount} cuotas • ${model.lifespanMonths} meses",
                                                color = EmeraldSuccess,
                                                fontSize = 10.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                customPurifiersList = customPurifiersList.filter { it.id != model.id }
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = RoseError, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // TAB 1: AGENDA Y HORARIOS
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF031E2C)),
                                    border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "🎯 META SEMANAL DE CITAS",
                                            color = PsaCyanGlow,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = weeklyVisitsGoalText,
                                            onValueChange = { weeklyVisitsGoalText = it },
                                            label = { Text("Meta semanal de citas (ej. 10)", fontSize = 10.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PsaCyanGlow,
                                                unfocusedBorderColor = SlateCardBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            )
                                        )
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "⏰ Franjas Horarias Predeterminadas de la Jornada:",
                                    color = PsaCyanGlow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = newSlotStartText,
                                        onValueChange = { newSlotStartText = it },
                                        label = { Text("Nuevo inicio (HH:mm)", fontSize = 10.sp) },
                                        placeholder = { Text("ej. 08:30 o 20:00", fontSize = 10.sp, color = TextMuted) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PsaCyanGlow,
                                            unfocusedBorderColor = SlateCardBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            val start = newSlotStartText.trim()
                                            if (start.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"))) {
                                                val end = PsaScheduleManager.calculateEndTime(start)
                                                if (dailySlotsList.none { it.first == start }) {
                                                    dailySlotsList = (dailySlotsList + (start to end)).sortedBy { it.first }.toMutableList()
                                                    newSlotStartText = ""
                                                    Toast.makeText(context, "Turno $start agregado", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "El turno $start ya existe", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Formato inválido. Usa HH:mm (ej. 10:00)", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Añadir", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }

                            items(dailySlotsList) { (start, end) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF021727))
                                        .border(1.dp, PsaCyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "⏰ $start - $end hs",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    IconButton(
                                        onClick = {
                                            if (dailySlotsList.size > 1) {
                                                dailySlotsList = dailySlotsList.filter { it.first != start }.toMutableList()
                                            } else {
                                                Toast.makeText(context, "Debes mantener al menos 1 turno en la jornada", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = RoseError, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            item {
                                OutlinedButton(
                                    onClick = {
                                        dailySlotsList = PsaScheduleManager.DEFAULT_HOURLY_RANGES.toMutableList()
                                        Toast.makeText(context, "Horarios por defecto restablecidos", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, SlateCardBorder)
                                ) {
                                    Text("Restaurar 6 Turnos Predeterminados", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }

                        2 -> {
                            // TAB 2: FINANCIACIÓN Y CARGOS POR CUOTAS EN EFECTIVO
                            item {
                                val isLargeFontDialog = LocalIsLargeFontMode.current
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateBackground),
                                    border = BorderStroke(1.dp, Color(0xFF4A90E2).copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "💳 RECARGOS POR CUOTAS (%)",
                                            color = Color(0xFF4A90E2),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        )

                                        if (isLargeFontDialog) {
                                            OutlinedTextField(
                                                value = interest3Text,
                                                onValueChange = { interest3Text = it },
                                                label = { Text("3 Cuotas (%)", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF4A90E2),
                                                    unfocusedBorderColor = SlateCardBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary
                                                )
                                            )
                                            OutlinedTextField(
                                                value = interest6Text,
                                                onValueChange = { interest6Text = it },
                                                label = { Text("6 Cuotas (%)", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF4A90E2),
                                                    unfocusedBorderColor = SlateCardBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary
                                                )
                                            )
                                            OutlinedTextField(
                                                value = interest12Text,
                                                onValueChange = { interest12Text = it },
                                                label = { Text("12 Cuotas (%)", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF4A90E2),
                                                    unfocusedBorderColor = SlateCardBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary
                                                )
                                            )
                                            OutlinedTextField(
                                                value = interest18Text,
                                                onValueChange = { interest18Text = it },
                                                label = { Text("18 Cuotas (%)", fontSize = 11.sp) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF4A90E2),
                                                    unfocusedBorderColor = SlateCardBorder,
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary
                                                )
                                            )
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = interest3Text,
                                                    onValueChange = { interest3Text = it },
                                                    label = { Text("3 Cuotas (%)", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color(0xFF4A90E2),
                                                        unfocusedBorderColor = SlateCardBorder,
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary
                                                    )
                                                )
                                                OutlinedTextField(
                                                    value = interest6Text,
                                                    onValueChange = { interest6Text = it },
                                                    label = { Text("6 Cuotas (%)", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color(0xFF4A90E2),
                                                        unfocusedBorderColor = SlateCardBorder,
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary
                                                    )
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = interest12Text,
                                                    onValueChange = { interest12Text = it },
                                                    label = { Text("12 Cuotas (%)", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color(0xFF4A90E2),
                                                        unfocusedBorderColor = SlateCardBorder,
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary
                                                    )
                                                )
                                                OutlinedTextField(
                                                    value = interest18Text,
                                                    onValueChange = { interest18Text = it },
                                                    label = { Text("18 Cuotas (%)", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color(0xFF4A90E2),
                                                        unfocusedBorderColor = SlateCardBorder,
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateBackground),
                                    border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.28f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "💵 ADELANTO SUGERIDO (%)",
                                            color = AmberDelay,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = advancePctText,
                                            onValueChange = { advancePctText = it },
                                            label = { Text("Adelanto sugerido (ej. 20%)", fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AmberDelay,
                                                unfocusedBorderColor = SlateCardBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            )
                                        )
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
                onClick = {
                    val updated = initialConfig.copy(
                        senior4Price = senior4PriceText.toDoubleOrNull() ?: initialConfig.senior4Price,
                        senikPrice = senikPriceText.toDoubleOrNull() ?: initialConfig.senikPrice,
                        quantumPrice = quantumPriceText.toDoubleOrNull() ?: initialConfig.quantumPrice,
                        cromaPrice = cromaPriceText.toDoubleOrNull() ?: initialConfig.cromaPrice,
                        veroPrice = veroPriceText.toDoubleOrNull() ?: initialConfig.veroPrice,
                        s1000Price = s1000PriceText.toDoubleOrNull() ?: initialConfig.s1000Price,
                        miniPrice = miniPriceText.toDoubleOrNull() ?: initialConfig.miniPrice,
                        c3Price = c3PriceText.toDoubleOrNull() ?: initialConfig.c3Price,
                        duchaPrice = duchaPriceText.toDoubleOrNull() ?: initialConfig.duchaPrice,
                        piletaPrice = piletaPriceText.toDoubleOrNull() ?: initialConfig.piletaPrice,
                        airePrice = airePriceText.toDoubleOrNull() ?: initialConfig.airePrice,
                        descalcificadorPrice = descalcificadorPriceText.toDoubleOrNull() ?: initialConfig.descalcificadorPrice,
                        domusPrice = domusPriceText.toDoubleOrNull() ?: initialConfig.domusPrice,
                        maximaPrice = maximaPriceText.toDoubleOrNull() ?: initialConfig.maximaPrice,
                        ropotPrice = ropotPriceText.toDoubleOrNull() ?: initialConfig.ropotPrice,
                        rotechPrice = rotechPriceText.toDoubleOrNull() ?: initialConfig.rotechPrice,
                        repuestoSeniorPrice = repuestoSeniorPriceText.toDoubleOrNull() ?: initialConfig.repuestoSeniorPrice,
                        bidonPrice = bidonPriceText.toDoubleOrNull() ?: initialConfig.bidonPrice,
                        bottlePrice = bottlePriceText.toDoubleOrNull() ?: initialConfig.bottlePrice,
                        installmentsCount = installmentsText.toIntOrNull() ?: initialConfig.installmentsCount,
                        defaultLifespanMonths = defaultLifespanText.toIntOrNull() ?: initialConfig.defaultLifespanMonths,
                        weeklyVisitsGoal = weeklyVisitsGoalText.toIntOrNull() ?: initialConfig.weeklyVisitsGoal,
                        defaultComparisonType = comparisonType,
                        customPurifiers = customPurifiersList
                    )
                    val updatedFinancing = initialFinancingConfig.copy(
                        defaultInterest3Months = interest3Text.toFloatOrNull() ?: initialFinancingConfig.defaultInterest3Months,
                        defaultInterest6Months = interest6Text.toFloatOrNull() ?: initialFinancingConfig.defaultInterest6Months,
                        defaultInterest12Months = interest12Text.toFloatOrNull() ?: initialFinancingConfig.defaultInterest12Months,
                        defaultInterest18Months = interest18Text.toFloatOrNull() ?: initialFinancingConfig.defaultInterest18Months,
                        suggestedAdvancePercentage = advancePctText.toFloatOrNull() ?: initialFinancingConfig.suggestedAdvancePercentage
                    )
                    onSave(updated, dailySlotsList, updatedFinancing)
                    Toast.makeText(context, "✅ Configuración PSA (Precios, Agenda y Financiación) guardada", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary)
            ) {
                Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SlateCard
    )

    // Modal para crear un nuevo purificador
    if (showAddPurifierModal) {
        AlertDialog(
            onDismissRequest = { showAddPurifierModal = false },
            title = {
                Text("➕ Crear Nuevo Purificador", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPurifierName,
                        onValueChange = { newPurifierName = it },
                        label = { Text("Nombre del Modelo (ej. PSA CMAX)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPurifierPriceText,
                        onValueChange = { newPurifierPriceText = it },
                        label = { Text("Precio de Lista Total ($)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    if (isLargeFont) {
                        OutlinedTextField(
                            value = newPurifierInstallmentsText,
                            onValueChange = { newPurifierInstallmentsText = it },
                            label = { Text("Cuotas fijas (ej. 12)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = newPurifierLifespanText,
                            onValueChange = { newPurifierLifespanText = it },
                            label = { Text("Meses vida útil (ej. 36)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newPurifierInstallmentsText,
                                onValueChange = { newPurifierInstallmentsText = it },
                                label = { Text("Cuotas fijas (ej. 12)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = newPurifierLifespanText,
                                onValueChange = { newPurifierLifespanText = it },
                                label = { Text("Meses vida útil (ej. 36)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newPurifierDesc,
                        onValueChange = { newPurifierDesc = it },
                        label = { Text("Breve descripción", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = newPurifierPriceText.toDoubleOrNull()
                        if (newPurifierName.isBlank() || price == null) {
                            Toast.makeText(context, "Por favor indica nombre y precio válido", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val newModel = com.example.data.model.PsaPurifierModel(
                            id = "purifier_${System.currentTimeMillis()}",
                            name = newPurifierName.trim(),
                            price = price,
                            installmentsCount = newPurifierInstallmentsText.toIntOrNull() ?: 12,
                            lifespanMonths = newPurifierLifespanText.toIntOrNull() ?: 36,
                            description = newPurifierDesc.trim()
                        )
                        customPurifiersList = customPurifiersList + newModel
                        newPurifierName = ""
                        newPurifierPriceText = ""
                        showAddPurifierModal = false
                        Toast.makeText(context, "Modelo añadido al catálogo", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary)
                ) {
                    Text("Agregar Modelo", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddPurifierModal = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SlateCard
        )
    }
}

// -------------------------------------------------------------
// UTILS
// -------------------------------------------------------------
private fun openWhatsApp(context: Context, phone: String, message: String) {
    val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
    val url = if (cleanPhone.isNotBlank()) {
        "https://wa.me/$cleanPhone?text=${Uri.encode(message)}"
    } else {
        "https://wa.me/?text=${Uri.encode(message)}"
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo abrir WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("PSA Copiado", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "📋 Copiado al portapapeles", Toast.LENGTH_SHORT).show()
}
