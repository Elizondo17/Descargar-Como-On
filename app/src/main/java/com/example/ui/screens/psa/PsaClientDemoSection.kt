package com.example.ui.screens.psa

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shower
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaPricingConfig
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalIsLargeFontMode
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DemoBudgetPaymentMode
import com.example.util.DemoCartLineDetail
import com.example.util.DemoCartSelection
import com.example.util.DemoContainerOption
import com.example.util.DemoWaterSource
import com.example.util.PsaCountryConfig
import com.example.util.PsaCountryRegistry
import com.example.util.PsaDemoAiContext
import com.example.util.PsaDemoDiagnosticSummary
import com.example.util.PsaDemoReportExporter
import com.example.util.PsaHardnessStripCatalog
import java.util.Locale
import kotlin.math.abs

private enum class DemoModuleFilter(val label: String, val tag: String) {
    ALL("Todos", "all"),
    CLORIMETRO("1. Clorímetro", "clorimetro"),
    TRIHALOMETANOS("2. THMs y Ducha", "thm"),
    HERVOR("3. Hervor", "hervor"),
    SARRO("4. Sarro", "sarro"),
    HIDRATACION("5. Agua Diaria", "hidratacion"),
    GASTO_ANUAL("6. Gasto Real", "gasto"),
    POR_QUE_PSA("7. Por qué PSA", "motivos"),
    AGUAS_Y_EQUIPO("8. Aguas y Equipo", "aguas")
}

private data class ChlorineVisualLevel(
    val color: Color,
    val colorHex: Long,
    val title: String,
    val accent: Color,
    val summary: String,
    val bodyImpact: String
)

private data class ThmExposureMode(
    val title: String,
    val badge: String,
    val riskPct: Float,
    val accent: Color,
    val detail: String,
    val retainedContaminantsNote: String,
    val psaPrevention: String
)

private data class BoilingStageInfo(
    val label: String,
    val minutesLabel: String,
    val waterRemainingPct: Float,
    val concentrationLabel: String,
    val accent: Color,
    val explanation: String
)

private data class HardnessVisualInfo(
    val label: String,
    val ppm: String,
    val accent: Color,
    val pipeClogFraction: Float,
    val energyPenaltyPct: Int,
    val impactHome: String,
    val protectionPsa: String
)

private data class WaterTypeComparison(
    val title: String,
    val badge: String,
    val accent: Color,
    val pros: String,
    val cons: String
)

/**
 * Pestaña 'Demostración' dual e interactiva frente al cliente:
 * - Switch superior entre Agua Corriente (Red) y Agua de Pozo (Napa)
 * - Hidratación por personas en el hogar (Módulo 5) -> Gasto real con Infusiones y Cocina (Módulo 6)
 * - Recomendación limpia sin cuotas previas + paso directo a Cotizar (Tarjeta o Efectivo)
 * - Exportación del diagnóstico de calidad del agua a PDF o WhatsApp
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaClientDemoSection(
    pricingConfig: PsaPricingConfig,
    activeCountry: PsaCountryConfig,
    isFullscreenPresentation: Boolean = false,
    initialWaterSource: DemoWaterSource = DemoWaterSource.AGUA_CORRIENTE,
    onWaterSourceChange: ((DemoWaterSource) -> Unit)? = null,
    initialTapCart: Map<String, Int> = mapOf("senior4" to 1),
    onTapCartChange: ((Map<String, Int>) -> Unit)? = null,
    initialWellCart: Map<String, Int> = mapOf("rotech" to 1),
    onWellCartChange: ((Map<String, Int>) -> Unit)? = null,
    onOpenFullscreen: (() -> Unit)? = null,
    onCloseFullscreen: (() -> Unit)? = null,
    onRequestCartBudget: (List<DemoCartSelection>, DemoBudgetPaymentMode) -> Unit = { _, _ -> },
    activeVisit: PsaHomeVisit? = null,
    onClearActiveVisit: (() -> Unit)? = null,
    onSaveDiagnosticToVisit: ((visit: PsaHomeVisit, waterSource: String, chlorinePpm: Float, hardnessPpm: Int, realLitersPerDay: Double, cartSummary: String, testPhotoPath: String) -> Unit)? = null,
    onOpenAiAssistant: (() -> Unit)? = null,
    onAiContextChanged: ((PsaDemoAiContext) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isLargeFont = LocalIsLargeFontMode.current
    val allPurifiers = remember(pricingConfig) { pricingConfig.getAllPurifiers() }

    var waterSource by remember { mutableStateOf(initialWaterSource) }
    var selectedTapModule by remember { mutableStateOf(DemoModuleFilter.ALL) }
    var selectedWellModule by remember { mutableStateOf(WellDemoModuleFilter.ALL) }

    // Tap water states
    var chlorinePpm by remember { mutableFloatStateOf(1.0f) }
    var thmExposureIdx by remember { mutableIntStateOf(2) }
    var boilingStageIdx by remember { mutableIntStateOf(2) }
    var kitchenTestTab by remember { mutableIntStateOf(0) }
    var selectedHardnessIdx by remember { mutableIntStateOf(2) }
    var selectedComparisonModelId by remember { mutableStateOf("senior4") }

    // Real photo capture & HSV detection states
    var tapPhotoPath by remember { mutableStateOf("") }
    var tapDetectedLabel by remember { mutableStateOf("") }
    var tapDetectedSwatchInt by remember { mutableStateOf<Int?>(null) }
    var tapDetectedHex by remember { mutableStateOf("") }
    var tapQualityTitle by remember { mutableStateOf("") }
    var tapRequiresFiltering by remember { mutableStateOf<Boolean?>(null) }
    var tapRecEquipment by remember { mutableStateOf("") }
    var tapRecReason by remember { mutableStateOf("") }
    var tapRecIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var wellPhotoPath by remember { mutableStateOf("") }

    // Multi-equipment cart states (tap & well)
    var tapCartQuantities by remember { mutableStateOf(initialTapCart) }
    var wellCartQuantities by remember { mutableStateOf(initialWellCart) }

    LaunchedEffect(initialTapCart) {
        tapCartQuantities = initialTapCart
    }
    LaunchedEffect(initialWellCart) {
        wellCartQuantities = initialWellCart
    }

    val toggleCartItem: (Map<String, Int>, String) -> Map<String, Int> = { current, id ->
        if ((current[id] ?: 0) > 0) current - id else current + (id to 1)
    }
    val stepCartQuantity: (Map<String, Int>, String, Int) -> Map<String, Int> = { current, id, delta ->
        val next = ((current[id] ?: 0) + delta).coerceAtMost(10)
        if (next <= 0) current - id else current + (id to next)
    }

    // Well water hardness strip index (0..5: Verde brillante -> Fucsia intenso)
    var selectedWellStripIdx by remember { mutableIntStateOf(4) }

    // Shared household & real bottled spend states
    var adultsCount by remember { mutableIntStateOf(2) }
    var childrenCount by remember { mutableIntStateOf(2) }
    var hasHotWeatherOrSport by remember { mutableStateOf(false) }
    var hasPregnancyOrNursing by remember { mutableStateOf(false) }
    var includeInfusions by remember { mutableStateOf(true) }
    var includeCooking by remember { mutableStateOf(true) }
    var containerOption by remember { mutableStateOf(DemoContainerOption.BIDON_20L) }
    var containerPriceText by remember(activeCountry.code, containerOption) {
        val defaultUnitPrice = if (containerOption == DemoContainerOption.BIDON_20L) {
            pricingConfig.bidonPrice.toLong().toString()
        } else {
            pricingConfig.bottlePrice.toLong().toString()
        }
        mutableStateOf(defaultUnitPrice)
    }

    var showListPrice by remember { mutableStateOf(false) }
    var clientExportLabel by remember { mutableStateOf(activeVisit?.customerName.orEmpty()) }

    LaunchedEffect(activeVisit?.id) {
        if (activeVisit != null) {
            if (activeVisit.customerName.isNotBlank()) {
                clientExportLabel = activeVisit.customerName
            }
            if (activeVisit.chlorinePpm > 0f) {
                chlorinePpm = activeVisit.chlorinePpm
            }
            if (activeVisit.testPhotoPath.isNotBlank()) {
                if (activeVisit.waterSource == DemoWaterSource.AGUA_DE_POZO.name) {
                    wellPhotoPath = activeVisit.testPhotoPath
                } else {
                    tapPhotoPath = activeVisit.testPhotoPath
                }
            }
        }
    }

    LaunchedEffect(initialWaterSource) {
        waterSource = initialWaterSource
    }

    val cardSpacing = if (isLargeFont) 18.dp else 14.dp
    val cardPadding = if (isLargeFont) 18.dp else 16.dp

    val showTapModule: (DemoModuleFilter) -> Boolean = { filter ->
        selectedTapModule == DemoModuleFilter.ALL || selectedTapModule == filter
    }

    val tapConsumptionStats = remember(
        adultsCount,
        childrenCount,
        hasHotWeatherOrSport,
        hasPregnancyOrNursing,
        includeInfusions,
        includeCooking,
        containerOption,
        containerPriceText
    ) {
        PsaDemoReportExporter.calculateRealConsumptionStats(
            adultsCount = adultsCount,
            childrenCount = childrenCount,
            hasHotWeatherOrSport = hasHotWeatherOrSport,
            hasPregnancyOrNursing = hasPregnancyOrNursing,
            includeInfusions = includeInfusions,
            includeCooking = includeCooking,
            containerOption = containerOption,
            containerUnitPrice = containerPriceText.toDoubleOrNull()?.coerceAtLeast(10.0) ?: 1000.0
        )
    }

    val comparisonModels = remember(allPurifiers) {
        val preferredIds = listOf("senior4", "senik", "quantum", "croma", "vero")
        preferredIds.mapNotNull { id -> allPurifiers.firstOrNull { it.id == id } }
            .ifEmpty { allPurifiers.take(4) }
    }
    val selectedTapComparisonModel = comparisonModels.firstOrNull { it.id == selectedComparisonModelId } ?: comparisonModels.first()

    LaunchedEffect(
        waterSource,
        chlorinePpm,
        selectedWellStripIdx,
        tapConsumptionStats,
        tapCartQuantities,
        wellCartQuantities,
        clientExportLabel,
        allPurifiers
    ) {
        val hardnessStages = PsaHardnessStripCatalog.stages
        val activeHardness = hardnessStages[selectedWellStripIdx.coerceIn(0, hardnessStages.lastIndex)].ppmNumeric
        val activeCart = if (waterSource == DemoWaterSource.AGUA_DE_POZO) wellCartQuantities else tapCartQuantities
        val cartIds = activeCart.filterValues { it > 0 }.keys.toList()
        val cartText = cartIds.mapNotNull { id ->
            val model = allPurifiers.firstOrNull { it.id == id } ?: return@mapNotNull null
            val qty = activeCart[id] ?: 1
            if (qty > 1) "${qty}x ${model.name}" else model.name
        }.joinToString(" + ")
        onAiContextChanged?.invoke(
            PsaDemoAiContext(
                sourceOrigin = "Demostración",
                waterSource = waterSource.name,
                chlorinePpm = chlorinePpm,
                hardnessPpm = activeHardness,
                realLitersPerDay = tapConsumptionStats.totalRealLitersDay,
                annualBottledSpend = tapConsumptionStats.annualBottledSpend,
                cartModelIds = cartIds,
                cartSummary = cartText,
                clientName = clientExportLabel.trim()
            )
        )
    }

    val wellBlocks = if (waterSource == DemoWaterSource.AGUA_DE_POZO) {
        WellWaterDemoModules(
            selectedModule = selectedWellModule,
            selectedStripIdx = selectedWellStripIdx,
            onSelectStripIdx = { selectedWellStripIdx = it },
            adultsCount = adultsCount,
            onAdultsChange = { adultsCount = it },
            childrenCount = childrenCount,
            onChildrenChange = { childrenCount = it },
            hasHotWeatherOrSport = hasHotWeatherOrSport,
            onToggleHotWeather = { hasHotWeatherOrSport = !hasHotWeatherOrSport },
            hasPregnancyOrNursing = hasPregnancyOrNursing,
            onTogglePregnancy = { hasPregnancyOrNursing = !hasPregnancyOrNursing },
            includeInfusions = includeInfusions,
            onToggleInfusions = { includeInfusions = !includeInfusions },
            includeCooking = includeCooking,
            onToggleCooking = { includeCooking = !includeCooking },
            containerOption = containerOption,
            onSelectContainer = { containerOption = it },
            containerPriceText = containerPriceText,
            onContainerPriceChange = { containerPriceText = it },
            allPurifiers = allPurifiers,
            wellCartQuantities = wellCartQuantities,
            onToggleWellCartItem = { id ->
                val updated = toggleCartItem(wellCartQuantities, id)
                wellCartQuantities = updated
                onWellCartChange?.invoke(updated)
            },
            onUpdateWellCartQuantity = { id, delta ->
                val updated = stepCartQuantity(wellCartQuantities, id, delta)
                wellCartQuantities = updated
                onWellCartChange?.invoke(updated)
            },
            activeCountry = activeCountry,
            isLargeFont = isLargeFont,
            cardPadding = cardPadding,
            showListPrice = showListPrice,
            onToggleShowPrice = { showListPrice = !showListPrice },
            clientLabel = clientExportLabel,
            onClientLabelChange = { clientExportLabel = it },
            onRequestCartBudget = onRequestCartBudget,
            wellPhotoPath = wellPhotoPath,
            onWellPhotoPathChange = { wellPhotoPath = it },
            onSetWellCartRecommended = { ids ->
                val updated = ids.filter { id -> allPurifiers.any { it.id == id } }
                    .associateWith { 1 }
                    .ifEmpty { mapOf("rotech" to 1) }
                wellCartQuantities = updated
                onWellCartChange?.invoke(updated)
            },
            activeVisit = activeVisit,
            onClearActiveVisit = onClearActiveVisit,
            onSaveDiagnosticToVisit = onSaveDiagnosticToVisit?.let { saver ->
                { visit, hardnessPpm, realLitersPerDay, cartSummary, photoPath ->
                    saver(
                        visit,
                        DemoWaterSource.AGUA_DE_POZO.name,
                        0f,
                        hardnessPpm,
                        realLitersPerDay,
                        cartSummary,
                        photoPath
                    )
                }
            }
        )
    } else {
        emptyList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .let { if (isFullscreenPresentation) it.statusBarsPadding().navigationBarsPadding() else it }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(cardSpacing)
    ) {
        // --- ENCABEZADO + SWITCH AGUA CORRIENTE / AGUA DE POZO + SELECTOR DE MÓDULOS ---
        item {
            Surface(
                color = SlateSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(cardPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isLargeFont && !isFullscreenPresentation) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DemoHeaderText(isFullscreenPresentation, waterSource)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DemoHeaderActionButton(
                                    isFullscreenPresentation = isFullscreenPresentation,
                                    onOpenFullscreen = onOpenFullscreen,
                                    onCloseFullscreen = onCloseFullscreen,
                                    fullWidth = true
                                )
                            }
                        }
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.padding(top = 6.dp)) {
                                DemoHeaderText(isFullscreenPresentation, waterSource)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DemoHeaderActionButton(
                                    isFullscreenPresentation = isFullscreenPresentation,
                                    onOpenFullscreen = onOpenFullscreen,
                                    onCloseFullscreen = onCloseFullscreen,
                                    fullWidth = false
                                )
                            }
                        }
                    }

                    // Switch superior Agua Corriente / Agua de Pozo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SlateBackground)
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DemoWaterSource.entries.forEach { src ->
                            val isSelected = waterSource == src
                            val pillColor = if (src == DemoWaterSource.AGUA_CORRIENTE) PsaCyanPrimary else Color(0xFFEC407A)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSelected) pillColor else Color.Transparent)
                                    .clickable {
                                        waterSource = src
                                        onWaterSourceChange?.invoke(src)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag(
                                        if (src == DemoWaterSource.AGUA_CORRIENTE) "demo_source_tap" else "demo_source_well"
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = src.label,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Selector horizontal dinámico de módulos según tipo de agua
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (waterSource == DemoWaterSource.AGUA_CORRIENTE) {
                            DemoModuleFilter.entries.forEach { module ->
                                DemoSharedChip(
                                    label = module.label,
                                    selected = selectedTapModule == module,
                                    selectedColor = PsaCyanPrimary,
                                    onClick = { selectedTapModule = module },
                                    testTag = "demo_module_tab_${module.tag}"
                                )
                            }
                        } else {
                            WellDemoModuleFilter.entries.forEach { module ->
                                DemoSharedChip(
                                    label = module.label,
                                    selected = selectedWellModule == module,
                                    selectedColor = Color(0xFFEC407A),
                                    onClick = { selectedWellModule = module },
                                    testTag = "well_module_tab_${module.tag}"
                                )
                            }
                        }
                    }
                }
            }
        }

        if (waterSource == DemoWaterSource.AGUA_DE_POZO) {
            wellBlocks.forEach { blockComposable ->
                item {
                    blockComposable()
                }
            }
        } else {
            // =========================================================================
            // FLUJO AGUA CORRIENTE (RED): 8 MÓDULOS ACTUALIZADOS
            // =========================================================================

            // --- MÓDULO 1: COMPARATIVA VISUAL DE CLORO + EFECTO EN EL ORGANISMO ---
            if (showTapModule(DemoModuleFilter.CLORIMETRO)) {
                item {
                    val stage = remember(chlorinePpm) {
                        when {
                            chlorinePpm <= 0.15f -> ChlorineVisualLevel(
                                color = Color(0xFFE0F7FA),
                                colorHex = 0xFFE0F7FAL,
                                title = "0.0 ppm • Sin cloro residual",
                                accent = EmeraldSuccess,
                                summary = "Cristalina y pura. Libre de cloro, olor a lavandina y partículas en suspensión.",
                                bodyImpact = "Sin exposición química: agua segura que protege la mucosa digestiva y la microbiota intestinal."
                            )
                            chlorinePpm <= 0.65f -> ChlorineVisualLevel(
                                color = Color(0xFFFFF59D),
                                colorHex = 0xFFFFF59DL,
                                title = "0.5 ppm • Cloro mínimo de red",
                                accent = Color(0xFFFFCA28),
                                summary = "Tono amarillo pálido con reactivo OTO: el cloro residual llega activo hasta la canilla.",
                                bodyImpact = "En el organismo irrita suavemente mucosas y, día tras día, altera parte de la flora intestinal benéfica."
                            )
                            chlorinePpm <= 1.25f -> ChlorineVisualLevel(
                                color = Color(0xFFFFEE58),
                                colorHex = 0xFFFFEE58L,
                                title = "1.0 ppm • Concentración media / alta",
                                accent = AmberDelay,
                                summary = "Amarillo intenso. Altera el aroma del mate, té o café y se absorbe al cocinar.",
                                bodyImpact = "Al beberla todos los días, el cloro actúa como oxidante dentro del tracto gástrico y puede generar subproductos (THMs)."
                            )
                            chlorinePpm <= 2.05f -> ChlorineVisualLevel(
                                color = Color(0xFFFFA726),
                                colorHex = 0xFFFFA726L,
                                title = "2.0 ppm • Carga química elevada",
                                accent = RoseError,
                                summary = "Ámbar fuerte. Alta presencia de desinfectantes químicos en el agua diaria.",
                                bodyImpact = "Mayor irritación gástrica y mucosa oral, resequedad en piel/cabello e impacto continuo sobre la microbiota."
                            )
                            else -> ChlorineVisualLevel(
                                color = Color(0xFFE65100),
                                colorHex = 0xFFE65100L,
                                title = "3.0 ppm • Concentración muy alta",
                                accent = Color(0xFFFF5252),
                                summary = "Naranja profundo. Exceso notorio de cloro libre tras purgas o refuerzo en planta.",
                                bodyImpact = "Exposición elevada de oxidantes al beber, cocinar y ducharse; fuerte olor a lavandina y riesgo elevado de THMs."
                            )
                        }
                    }
                    val animatedTapColor by animateColorAsState(
                        targetValue = stage.color,
                        animationSpec = tween(durationMillis = 260),
                        label = "tap_chlorine_color"
                    )

                    DemoSharedCard(
                        icon = Icons.Default.Science,
                        title = "1. Comparativa Visual de Cloro (Clorímetro)",
                        accent = stage.accent,
                        padding = cardPadding
                    ) {
                        if (isLargeFont) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ChlorineGlassBox(
                                    title = "Agua de Canilla",
                                    ppmText = "${String.format(Locale.US, "%.1f", chlorinePpm)} ppm",
                                    liquidColor = animatedTapColor,
                                    badgeColor = stage.accent,
                                    subtitle = if (chlorinePpm <= 0.15f) "Sin reactivo / limpia" else "Reacciona en amarillo",
                                    modifier = Modifier.fillMaxWidth().testTag("demo_clorimetro_canilla")
                                )
                                ChlorineGlassBox(
                                    title = "Purificador PSA",
                                    ppmText = "0.0 ppm",
                                    liquidColor = Color(0xFFE0F7FA),
                                    badgeColor = EmeraldSuccess,
                                    subtitle = "100% cristalina",
                                    modifier = Modifier.fillMaxWidth().testTag("demo_clorimetro_psa")
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ChlorineGlassBox(
                                    title = "Agua de Canilla",
                                    ppmText = "${String.format(Locale.US, "%.1f", chlorinePpm)} ppm",
                                    liquidColor = animatedTapColor,
                                    badgeColor = stage.accent,
                                    subtitle = if (chlorinePpm <= 0.15f) "Sin cloro" else "Con reactivo",
                                    modifier = Modifier.weight(1f).testTag("demo_clorimetro_canilla")
                                )
                                ChlorineGlassBox(
                                    title = "Purificador PSA",
                                    ppmText = "0.0 ppm",
                                    liquidColor = Color(0xFFE0F7FA),
                                    badgeColor = EmeraldSuccess,
                                    subtitle = "Siempre pura",
                                    modifier = Modifier.weight(1f).testTag("demo_clorimetro_psa")
                                )
                            }
                        }

                        PsaRealTestPhotoCard(
                            isWellWater = false,
                            savedPhotoPath = tapPhotoPath,
                            detectedLabel = tapDetectedLabel,
                            detectedSwatchColorInt = tapDetectedSwatchInt,
                            detectedHex = tapDetectedHex,
                            detectedQualityTitle = tapQualityTitle,
                            detectedRequiresFiltering = tapRequiresFiltering,
                            detectedRecommendedEquipment = tapRecEquipment,
                            detectedRecommendationReason = tapRecReason,
                            detectedRecommendedIds = tapRecIds,
                            onApplyRecommendedCart = { ids ->
                                val updated = ids.filter { id -> allPurifiers.any { it.id == id } }
                                    .associateWith { 1 }
                                    .ifEmpty { mapOf("senior4" to 1) }
                                tapCartQuantities = updated
                                onTapCartChange?.invoke(updated)
                            },
                            onPhotoAnalyzed = { outcome ->
                                tapPhotoPath = outcome.savedPhotoPath
                                tapDetectedSwatchInt = outcome.sampledColorInt
                                tapDetectedHex = outcome.sampledHexLabel
                                outcome.chlorineResult?.let { cr ->
                                    tapDetectedLabel = cr.confidenceLabel
                                    tapQualityTitle = cr.qualityStatusTitle
                                    tapRequiresFiltering = cr.requiresFiltering
                                    tapRecEquipment = cr.recommendedEquipmentLabel
                                    tapRecReason = cr.recommendationSummary
                                    tapRecIds = cr.recommendedCartIds
                                    chlorinePpm = cr.detectedPpm
                                }
                            },
                            onClearPhoto = {
                                tapPhotoPath = ""
                                tapDetectedLabel = ""
                                tapDetectedSwatchInt = null
                                tapDetectedHex = ""
                                tapQualityTitle = ""
                                tapRequiresFiltering = null
                                tapRecEquipment = ""
                                tapRecReason = ""
                                tapRecIds = emptyList()
                            },
                            isLargeFont = isLargeFont
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SlateBackground)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Escala Clorímetro OTO (toca un nivel o desliza)",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    0.0f to Color(0xFFE0F7FA),
                                    0.5f to Color(0xFFFFF59D),
                                    1.0f to Color(0xFFFFEE58),
                                    2.0f to Color(0xFFFFA726),
                                    3.0f to Color(0xFFE65100)
                                ).forEach { (ppmVal, swatch) ->
                                    val active = abs(chlorinePpm - ppmVal) < 0.25f
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(swatch)
                                            .border(
                                                width = if (active) 2.5.dp else 1.dp,
                                                color = if (active) Color.White else SlateCardBorder,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { chlorinePpm = ppmVal },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$ppmVal",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0.0f to "0.0 PSA",
                                0.5f to "0.5 Suave",
                                1.0f to "1.0 Medio",
                                2.0f to "2.0 Alto",
                                3.0f to "3.0 Crítico"
                            ).forEach { (ppm, label) ->
                                DemoSharedChip(
                                    label = label,
                                    selected = abs(chlorinePpm - ppm) < 0.25f,
                                    selectedColor = if (ppm == 0f) EmeraldSuccess else PsaCyanPrimary,
                                    onClick = { chlorinePpm = ppm },
                                    testTag = "demo_chlorine_chip_$ppm"
                                )
                            }
                        }

                        Slider(
                            value = chlorinePpm,
                            onValueChange = { chlorinePpm = it },
                            valueRange = 0f..3f,
                            steps = 5,
                            colors = SliderDefaults.colors(
                                thumbColor = stage.accent,
                                activeTrackColor = stage.accent,
                                inactiveTrackColor = SlateCardBorder
                            ),
                            modifier = Modifier.testTag("psa_slider_chlorine")
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(stage.accent.copy(alpha = 0.12f))
                                .border(1.dp, stage.accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(stage.title, color = stage.accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(stage.summary, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            HorizontalDivider(color = stage.accent.copy(alpha = 0.25f))
                            Text(
                                text = "¿Qué hace el cloro dentro del cuerpo al beberlo?",
                                color = stage.accent,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(stage.bodyImpact, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // --- MÓDULO 2: IMPACTO DE TRIHALOMETANOS (THMs), RETENCIÓN COMPLETA Y DUCHA ---
            if (showTapModule(DemoModuleFilter.TRIHALOMETANOS)) {
                item {
                    val thmModes = remember {
                        listOf(
                            ThmExposureMode(
                                title = "Al Beber todos los días",
                                badge = "Ingesta directa",
                                riskPct = 0.72f,
                                accent = AmberDelay,
                                detail = "El cloro libre reacciona con trazas orgánicas microscópicas generando trihalometanos (THMs), que ingresan directo al organismo.",
                                retainedContaminantsNote = "Además del cloro, el purificador PSA retiene sedimentos en suspensión, turbidez, óxido y metales pesados de cañerías viejas (como plomo), plaguicidas/orgánicos, olores y frena la proliferación bacteriana con carbón activado bacteriostático.",
                                psaPrevention = "El carbón activado en bloque + zeolitas + Fipor de PSA garantizan agua pura, cristalina y segura en cada vaso."
                            ),
                            ThmExposureMode(
                                title = "Al Cocinar y Hervir Vapor",
                                badge = "Aceleración térmica",
                                riskPct = 0.92f,
                                accent = RoseError,
                                detail = "El calor dispara la reacción química entre el cloro de la red y la materia orgánica de sopas, arroz o infusiones, liberando vapor con THMs.",
                                retainedContaminantsNote = "Retiene también óxido de tanque, arenilla e impurezas antes de que los absorba la comida caliente.",
                                psaPrevention = "Cocinar con agua ya purificada evita formar trihalometanos dentro de la olla."
                            ),
                            ThmExposureMode(
                                title = "En la Ducha (Vapor Caliente)",
                                badge = "Inhalación + Poros abiertos",
                                riskPct = 0.95f,
                                accent = PsaCyanGlow,
                                detail = "En una ducha de 10 minutos con agua caliente (>38°C), el cloro y los THMs se volatilizan en el vapor del baño cerrado: se inhalan directo a los pulmones y penetran por la piel con poros dilatados.",
                                retainedContaminantsNote = "Evita resequedad de piel, picazón, caída o quiebre de cabello y vapor clorado en vías respiratorias.",
                                psaPrevention = "PSA Ducha (declorador) elimina el cloro libre en el momento exacto del baño cuidando piel, cabello y pulmones."
                            )
                        )
                    }
                    val currentThm = thmModes[thmExposureIdx]
                    val animatedThmPct by animateFloatAsState(
                        targetValue = currentThm.riskPct,
                        animationSpec = tween(durationMillis = 280),
                        label = "thm_risk_anim"
                    )

                    DemoSharedCard(
                        icon = Icons.Default.WarningAmber,
                        title = "2. Trihalometanos (THMs), Contaminantes y Ducha",
                        accent = RoseError,
                        padding = cardPadding
                    ) {
                        DemoBulletRow(
                            badge = "La reacción química",
                            badgeColor = AmberDelay,
                            text = "Cloro residual + materia orgánica natural = Trihalometanos (THMs), subproductos volátiles que irritan mucosas y se acumulan por exposición diaria."
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Al Beber", "Cocinar / Olla", "Vapor en Ducha").forEachIndexed { idx, label ->
                                DemoSharedChip(
                                    label = label,
                                    selected = thmExposureIdx == idx,
                                    selectedColor = thmModes[idx].accent,
                                    onClick = { thmExposureIdx = idx },
                                    testTag = "demo_thm_mode_$idx"
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SlateBackground)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(currentThm.title, color = currentThm.accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(currentThm.badge, color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                            LinearProgressIndicator(
                                progress = { animatedThmPct },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = currentThm.accent,
                                trackColor = SlateCardBorder
                            )
                            Text(currentThm.detail, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = currentThm.retainedContaminantsNote,
                                color = PsaCyanGlow,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Protección PSA: ${currentThm.psaPrevention}",
                                color = EmeraldSuccess,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Bloque específico Vapor en la Ducha -> SOLO PSA Ducha recomendado
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PsaCyanPrimary.copy(alpha = 0.08f))
                                .border(1.dp, PsaCyanGlow.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .testTag("demo_shower_steam_box"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Shower, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "¿Por qué el vapor de la ducha multiplica la absorción?",
                                    color = PsaCyanGlow,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text(
                                text = "• Vapor concentrado: El agua tibia/caliente transforma el cloro y los THMs en gas dentro de un ambiente cerrado.",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Doble vía de ingreso: Al inhalar ese vapor húmedo y dilatarse los poros de la piel, el cuerpo absorbe mucho más cloro que tomando un vaso frío.",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Solución recomendada: PSA Ducha (declorador), diseñado específicamente para eliminar el cloro durante el baño y cuidar piel, cabello y vías respiratorias.",
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // --- MÓDULO 3: EFECTO DEL HERVOR EN AGUA ---
            if (showTapModule(DemoModuleFilter.HERVOR)) {
                item {
                    val boilingStages = remember {
                        listOf(
                            BoilingStageInfo("Agua Fría", "0 min", 1.0f, "100% (Base)", PsaCyanGlow, "Conserva todo su volumen original, junto con el cloro y las sales minerales disueltas de la red."),
                            BoilingStageInfo("Hervor Inicial", "3 min", 0.88f, "+14% concentración", AmberDelay, "Parte del agua pura escapa como vapor; los nitratos, sales de sarro y metales pesados quedan más concentrados."),
                            BoilingStageInfo("Hervor Prolongado", "10 min", 0.74f, "+35% concentración", RoseError, "Se evapora cerca de un cuarto del líquido puro. Hervir NO purifica químicos ni sarro: concentra lo que no se evapora en la pava u olla.")
                        )
                    }
                    val activeBoil = boilingStages[boilingStageIdx]

                    DemoSharedCard(
                        icon = Icons.Default.LocalFireDepartment,
                        title = "3. Efecto del Hervor en el Agua",
                        accent = AmberDelay,
                        padding = cardPadding
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            boilingStages.forEachIndexed { idx, st ->
                                DemoSharedChip(
                                    label = "${st.label} (${st.minutesLabel})",
                                    selected = boilingStageIdx == idx,
                                    selectedColor = st.accent,
                                    onClick = { boilingStageIdx = idx },
                                    testTag = "demo_boil_stage_$idx"
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SlateBackground)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isLargeFont) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Agua pura restante: ${(activeBoil.waterRemainingPct * 100).toInt()}%", color = PsaCyanGlow, fontWeight = FontWeight.Bold)
                                    Text("Sales / Químicos: ${activeBoil.concentrationLabel}", color = activeBoil.accent, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Agua pura restante: ${(activeBoil.waterRemainingPct * 100).toInt()}%", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    Text(activeBoil.concentrationLabel, color = activeBoil.accent, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }
                            LinearProgressIndicator(
                                progress = { activeBoil.waterRemainingPct },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = activeBoil.accent,
                                trackColor = SlateCardBorder
                            )
                            Text(activeBoil.explanation, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Recomendado: Purificar primero con PSA y recién después calentar o cocinar.",
                                color = EmeraldSuccess,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        HorizontalDivider(color = SlateCardBorder.copy(alpha = 0.5f))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DemoSharedChip(
                                label = "Cocinar arroz / fideos",
                                selected = kitchenTestTab == 0,
                                selectedColor = PsaCyanPrimary,
                                onClick = { kitchenTestTab = 0 },
                                testTag = "demo_kitchen_rice"
                            )
                            DemoSharedChip(
                                label = "Lavar verduras / frutas",
                                selected = kitchenTestTab == 1,
                                selectedColor = PsaCyanPrimary,
                                onClick = { kitchenTestTab = 1 },
                                testTag = "demo_kitchen_veggies"
                            )
                        }

                        val tapDesc = if (kitchenTestTab == 0) {
                            "Al hervir arroz o fideos con agua de red, el almidón absorbe hasta el 85% del cloro en minutos."
                        } else {
                            "Al lavar hojas y frutas con agua clorada, la superficie absorbe cloro y arrastra trazas de red."
                        }
                        val psaDesc = if (kitchenTestTab == 0) {
                            "Cocción en agua purificada: sabor natural de la comida sin absorber químicos clorados."
                        } else {
                            "Verduras más limpias, frescas y libres de olor a lavandina u óxido de cañería."
                        }

                        if (isLargeFont) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                KitchenCompareBox("Con Agua de Canilla", tapDesc, RoseError, Modifier.fillMaxWidth())
                                KitchenCompareBox("Con Agua PSA", psaDesc, EmeraldSuccess, Modifier.fillMaxWidth())
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                KitchenCompareBox("Con Agua de Canilla", tapDesc, RoseError, Modifier.weight(1f))
                                KitchenCompareBox("Con Agua PSA", psaDesc, EmeraldSuccess, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // --- MÓDULO 4: PROBLEMAS DE SARRO Y CAÑOS VIEJOS ---
            if (showTapModule(DemoModuleFilter.SARRO)) {
                item {
                    val hardnessStages = remember {
                        listOf(
                            HardnessVisualInfo("Blanda", "< 60 ppm", EmeraldSuccess, 0.08f, 0, "Poco sarro visible; grifería y electrodomésticos limpios.", "Protege el sabor diario eliminando cloro y sedimentos."),
                            HardnessVisualInfo("Media", "60-120 ppm", PsaCyanGlow, 0.24f, 8, "Empieza el sarro blanco en pava eléctrica y picos.", "Evita manchas en vajilla y mejora la calidad para infusiones."),
                            HardnessVisualInfo("Dura", "120-200 ppm", AmberDelay, 0.48f, 18, "Acumulación notoria en duchas, termotanque y resistencias (+18% consumo eléctrico/gas).", "PSA R-200 o Domus reduce incrustaciones en caños, caldera y piel."),
                            HardnessVisualInfo("Muy Dura", "> 200 ppm", RoseError, 0.70f, 28, "Taponamiento progresivo de cañerías, baja presión de agua, piel seca y ropa opaca.", "Combinación recomendada: PSA Domus / Descalcificador R-200 + Purificador PSA en cocina.")
                        )
                    }
                    val currentHardness = hardnessStages[selectedHardnessIdx]
                    val animPipeClog by animateFloatAsState(
                        targetValue = currentHardness.pipeClogFraction,
                        animationSpec = tween(durationMillis = 300),
                        label = "pipe_clog"
                    )

                    DemoSharedCard(
                        icon = Icons.Default.Kitchen,
                        title = "4. Problemas de Sarro y Caños Viejos",
                        accent = AmberDelay,
                        padding = cardPadding
                    ) {
                        DemoBulletRow(
                            badge = "Caños viejos y tanques",
                            badgeColor = RoseError,
                            text = "En el trayecto desde la planta hasta el domicilio, el agua arrastra óxido de cañerías antiguas, tierra y sedimentos."
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            hardnessStages.forEachIndexed { idx, info ->
                                DemoSharedChip(
                                    label = info.label,
                                    selected = selectedHardnessIdx == idx,
                                    selectedColor = info.accent,
                                    onClick = { selectedHardnessIdx = idx },
                                    testTag = "demo_hardness_$idx"
                                )
                            }
                        }

                        val pipeGraphic: @Composable () -> Unit = {
                            Box(
                                modifier = Modifier.size(96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val radius = size.minDimension / 2f - 6.dp.toPx()
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    drawCircle(
                                        color = Color(0xFF475569),
                                        radius = radius,
                                        center = center,
                                        style = Stroke(width = 8.dp.toPx())
                                    )
                                    val scaleWidth = radius * 0.75f * animPipeClog
                                    if (scaleWidth > 1f) {
                                        drawCircle(
                                            color = Color(0xFFE2E8F0).copy(alpha = 0.85f),
                                            radius = radius - 4.dp.toPx() - scaleWidth / 2f,
                                            center = center,
                                            style = Stroke(width = scaleWidth, cap = StrokeCap.Round)
                                        )
                                    }
                                    val innerWaterRadius = (radius - 6.dp.toPx() - scaleWidth).coerceAtLeast(6.dp.toPx())
                                    drawCircle(
                                        color = PsaCyanPrimary.copy(alpha = 0.75f),
                                        radius = innerWaterRadius,
                                        center = center
                                    )
                                }
                                Text(
                                    text = "${(100f - animPipeClog * 75f).toInt()}%\npaso",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        val hardnessDetails: @Composable () -> Unit = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${currentHardness.label} (${currentHardness.ppm})", color = currentHardness.accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Sobrecosto energético en termotanque/pava: +${currentHardness.energyPenaltyPct}%",
                                    color = if (currentHardness.energyPenaltyPct > 0) AmberDelay else EmeraldSuccess,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(currentHardness.impactHome, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                Text(currentHardness.protectionPsa, color = EmeraldSuccess, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (isLargeFont) {
                            Column(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SlateBackground).padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pipeGraphic()
                                hardnessDetails()
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SlateBackground).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pipeGraphic()
                                Box(modifier = Modifier.weight(1f)) { hardnessDetails() }
                            }
                        }
                    }
                }
            }

            // --- MÓDULO 5 (SWAPPED): AGUA DIARIA POR PERSONAS EN EL HOGAR ---
            if (showTapModule(DemoModuleFilter.HIDRATACION)) {
                item {
                    SharedHydrationByPeopleCard(
                        moduleTitle = "5. Agua Diaria Recomendada en Casa",
                        adultsCount = adultsCount,
                        onAdultsChange = { adultsCount = it },
                        childrenCount = childrenCount,
                        onChildrenChange = { childrenCount = it },
                        hasHotWeatherOrSport = hasHotWeatherOrSport,
                        onToggleHotWeather = { hasHotWeatherOrSport = !hasHotWeatherOrSport },
                        hasPregnancyOrNursing = hasPregnancyOrNursing,
                        onTogglePregnancy = { hasPregnancyOrNursing = !hasPregnancyOrNursing },
                        baseHydrationLitersDay = tapConsumptionStats.baseHydrationLitersDay,
                        isLargeFont = isLargeFont,
                        cardPadding = cardPadding
                    )
                }
            }

            // --- MÓDULO 6 (SWAPPED): GASTO REAL ANUAL CON INFUSIONES Y COCINA ---
            if (showTapModule(DemoModuleFilter.GASTO_ANUAL)) {
                item {
                    SharedRealBottledExpenseCard(
                        moduleTitle = "6. Gasto Real Anual: Bidones / Botellas vs PSA",
                        consumptionStats = tapConsumptionStats,
                        includeInfusions = includeInfusions,
                        onToggleInfusions = { includeInfusions = !includeInfusions },
                        includeCooking = includeCooking,
                        onToggleCooking = { includeCooking = !includeCooking },
                        containerOption = containerOption,
                        onSelectContainer = { containerOption = it },
                        containerPriceText = containerPriceText,
                        onContainerPriceChange = { containerPriceText = it },
                        highlightedModel = selectedTapComparisonModel,
                        collapsibleOtherModels = comparisonModels,
                        onSelectComparisonModel = { selectedComparisonModelId = it },
                        activeCountry = activeCountry,
                        isLargeFont = isLargeFont,
                        cardPadding = cardPadding
                    )
                }
            }

            // --- MÓDULO 7: MOTIVOS PARA DECIDIR EN FAMILIA ---
            if (showTapModule(DemoModuleFilter.POR_QUE_PSA)) {
                item {
                    val dailyAmortized3Years = selectedTapComparisonModel.price / 1095.0

                    DemoSharedCard(
                        icon = Icons.Default.VerifiedUser,
                        title = "7. ¿Por qué dar el paso hoy con PSA?",
                        accent = EmeraldSuccess,
                        padding = cardPadding
                    ) {
                        val pillarBox: @Composable (ImageVector, Color, String, String, String) -> Unit = { ico, accent, tag, heading, body ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SlateBackground)
                                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                                    .testTag(tag),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(ico, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                    }
                                    Text(heading, color = accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }
                                Text(body, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        pillarBox(
                            Icons.Default.LocalCafe,
                            EmeraldSuccess,
                            "demo_pillar_daily_coffee",
                            "Inversión amortizada menor a un café diario",
                            "Cuidar toda el agua que toma y cocina la familia con ${selectedTapComparisonModel.name} equivale apenas a ${PsaCountryRegistry.formatCurrency(dailyAmortized3Years, activeCountry)} por día a lo largo de sus 3 años de vida útil — menos que una botella chica o un café diario."
                        )
                        pillarBox(
                            Icons.Default.ChildCare,
                            PsaCyanGlow,
                            "demo_pillar_kids_pets",
                            "Cuidado para bebés, niños y mascotas",
                            "La leche o mamadera del bebé, las comidas de los chicos y el agua de las mascotas quedan libres de cloro residual, óxido, metales pesados y sabor químico desde el primer día."
                        )
                        pillarBox(
                            Icons.Default.Kitchen,
                            AmberDelay,
                            "demo_pillar_flavor_cooking",
                            "Sabor real de mate, café y cocción sin químicos",
                            "Al eliminar cloro y olores, la yerba mate rinde más, el café conserva su aroma real y sopas, arroz, pastas o verduras se cocinan sin absorber gusto a lavandina."
                        )
                        pillarBox(
                            Icons.Default.Shield,
                            PsaCyanPrimary,
                            "demo_pillar_warranty_3y",
                            "Garantía oficial de fábrica y 36 meses de respaldo",
                            "Respaldo directo PSA de 3 años (hasta ${selectedTapComparisonModel.capacityLiters.takeIf { it > 0 } ?: 36000} litros según equipo), sin depender del reparto de bidones ni cargar plástico."
                        )
                    }
                }
            }

            // --- MÓDULO 8: 4 TIPOS DE AGUA, EQUIPO RECOMENDADO + COTIZAR & EXPORTAR PDF/WHATSAPP ---
            if (showTapModule(DemoModuleFilter.AGUAS_Y_EQUIPO)) {
                item {
                    val waterTypes = remember {
                        listOf(
                            WaterTypeComparison("Agua de Mesa", "Industrial", Color(0xFF90A4AE), "Sabor neutro inmediato.", "Agua de red filtrada con agregado de sodio y envase plástico reutilizado."),
                            WaterTypeComparison("Envasada (Bidón)", "Plástico", AmberDelay, "Práctica de emergencia.", "Expuesta a sol/depósitos (microplásticos) y alto gasto mensual fijo."),
                            WaterTypeComparison("Agua Mineral", "Manantial", Color(0xFF64B5F6), "Origen natural de napa.", "Minerales fijos, peso de envases y costo elevado para cocinar todos los días."),
                            WaterTypeComparison("Purificada PSA", "En tu hogar", EmeraldSuccess, "Sin cloro, metales ni microplásticos al instante desde tu canilla para tomar y cocinar.", "Costo mínimo por litro y cero acarreos.")
                        )
                    }

                    DemoSharedCard(
                        icon = Icons.Default.WaterDrop,
                        title = "8A. Comparativa: 4 Tipos de Agua",
                        accent = PsaCyanPrimary,
                        padding = cardPadding
                    ) {
                        if (isLargeFont) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                waterTypes.forEach { WaterTypeCard(it, Modifier.fillMaxWidth()) }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    WaterTypeCard(waterTypes[0], Modifier.weight(1f))
                                    WaterTypeCard(waterTypes[1], Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    WaterTypeCard(waterTypes[2], Modifier.weight(1f))
                                    WaterTypeCard(waterTypes[3], Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                item {
                    DemoSharedCard(
                        icon = Icons.Default.VerifiedUser,
                        title = "8B. Equipos Recomendados PSA",
                        accent = PsaCyanGlow,
                        padding = cardPadding
                    ) {
                        PsaAccordionEquipmentCartSelector(
                            allPurifiers = allPurifiers,
                            prioritizedIds = listOf("senior4", "senik", "ducha", "c3", "quantum", "domus", "rotech"),
                            cartQuantities = tapCartQuantities,
                            onToggleItem = { id ->
                                val updated = toggleCartItem(tapCartQuantities, id)
                                tapCartQuantities = updated
                                onTapCartChange?.invoke(updated)
                            },
                            onUpdateQuantity = { id, delta ->
                                val updated = stepCartQuantity(tapCartQuantities, id, delta)
                                tapCartQuantities = updated
                                onTapCartChange?.invoke(updated)
                            },
                            country = activeCountry,
                            isLargeFont = isLargeFont,
                            showListPrice = showListPrice,
                            onToggleShowPrice = { showListPrice = !showListPrice },
                            onRequestCartBudget = onRequestCartBudget
                        )
                    }
                }

                item {
                    val tapCartLines = remember(allPurifiers, tapCartQuantities) {
                        allPurifiers.mapNotNull { model ->
                            val qty = tapCartQuantities[model.id] ?: 0
                            if (qty > 0) DemoCartLineDetail(model, qty) else null
                        }
                    }
                    val primaryTapModel = tapCartLines.firstOrNull()?.model ?: selectedTapComparisonModel
                    val secondaryTapModel = tapCartLines.getOrNull(1)?.model
                    val totalTapCartPrice = tapCartLines.sumOf { it.subtotal }.takeIf { it > 0.0 } ?: primaryTapModel.price
                    val annualPsaAmortized = totalTapCartPrice / 3.0

                    val riskTitle = when {
                        chlorinePpm <= 0.15f -> "0.0 ppm • Agua pura sin cloro residual"
                        chlorinePpm <= 0.65f -> "Nivel bajo/medio de cloro residual detectado"
                        chlorinePpm <= 1.25f -> "Concentración media/alta de cloro residual (${String.format(Locale.US, "%.1f", chlorinePpm)} ppm)"
                        else -> "Concentración elevada de cloro libre (${String.format(Locale.US, "%.1f", chlorinePpm)} ppm)"
                    }
                    val colorHex = when {
                        chlorinePpm <= 0.15f -> 0xFFE0F7FAL
                        chlorinePpm <= 0.65f -> 0xFFFFF59DL
                        chlorinePpm <= 1.25f -> 0xFFFFEE58L
                        chlorinePpm <= 2.05f -> 0xFFFFA726L
                        else -> 0xFFE65100L
                    }
                    val tapCartText = tapCartLines.joinToString(", ") { "${it.quantity}x ${it.model.name}" }
                        .ifBlank { primaryTapModel.name }
                    val diagnosticSummary = PsaDemoDiagnosticSummary(
                        clientLabel = clientExportLabel,
                        country = activeCountry,
                        waterSource = DemoWaterSource.AGUA_CORRIENTE,
                        chlorinePpm = chlorinePpm,
                        chlorineRiskTitle = riskTitle,
                        chlorineColorHex = colorHex,
                        boilingSummary = "Hervir sin purificar concentra sales y dispara trihalometanos (THMs) en cocción e infusiones.",
                        consumptionStats = tapConsumptionStats,
                        primaryModel = primaryTapModel,
                        secondaryModel = secondaryTapModel,
                        cartItems = tapCartLines,
                        annualPsaCostAmortized = annualPsaAmortized,
                        annualNetSavings = tapConsumptionStats.annualBottledSpend - annualPsaAmortized,
                        threeYearNetSavings = tapConsumptionStats.threeYearBottledSpend - totalTapCartPrice,
                        plasticBottlesAvoidedYear = ((tapConsumptionStats.totalRealLitersMonth * 12.0) / 1.5).toInt(),
                        showListPrice = showListPrice,
                        testPhotoPath = tapPhotoPath
                    )
                    PsaDiagnosticExportCard(
                        summary = diagnosticSummary,
                        clientLabel = clientExportLabel,
                        onClientLabelChange = { clientExportLabel = it },
                        isLargeFont = isLargeFont,
                        cardPadding = cardPadding,
                        activeVisit = activeVisit,
                        onClearActiveVisit = onClearActiveVisit,
                        onSaveDiagnosticToVisit = onSaveDiagnosticToVisit?.let { saver ->
                            { visit ->
                                saver(
                                    visit,
                                    DemoWaterSource.AGUA_CORRIENTE.name,
                                    chlorinePpm,
                                    0,
                                    tapConsumptionStats.totalRealLitersDay,
                                    tapCartText,
                                    tapPhotoPath
                                )
                            }
                        }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun DemoHeaderText(isFullscreenPresentation: Boolean, waterSource: DemoWaterSource) {
    Text(
        text = if (isFullscreenPresentation) {
            "Presentación PSA • ${waterSource.label}"
        } else {
            "Demostración • ${waterSource.label}"
        },
        color = PsaCyanGlow,
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun DemoHeaderActionButton(
    isFullscreenPresentation: Boolean,
    onOpenFullscreen: (() -> Unit)?,
    onCloseFullscreen: (() -> Unit)?,
    fullWidth: Boolean
) {
    if (isFullscreenPresentation && onCloseFullscreen != null) {
        IconButton(
            onClick = onCloseFullscreen,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(RoseError.copy(alpha = 0.14f))
                .border(1.dp, RoseError.copy(alpha = 0.55f), CircleShape)
                .testTag("demo_exit_fullscreen_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar presentación",
                tint = RoseError,
                modifier = Modifier.size(20.dp)
            )
        }
    } else if (!isFullscreenPresentation && onOpenFullscreen != null) {
        Button(
            onClick = onOpenFullscreen,
            colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier
                .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
                .heightIn(min = 48.dp)
                .testTag("demo_open_fullscreen_btn")
        ) {
            Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Modo Presentación", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChlorineGlassBox(
    title: String,
    ppmText: String,
    liquidColor: Color,
    badgeColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SlateBackground)
            .border(1.dp, badgeColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

        Box(
            modifier = Modifier
                .size(width = 76.dp, height = 96.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(Color(0xFF0F172A))
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .background(liquidColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ppmText,
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.5.sp
                )
            }
        }

        Text(
            text = subtitle,
            color = badgeColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp
        )
    }
}

@Composable
private fun DemoBulletRow(
    badge: String,
    badgeColor: Color,
    text: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = badge.uppercase(Locale.ROOT),
            color = badgeColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.4.sp
        )
        Text(
            text = text,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun KitchenCompareBox(
    title: String,
    body: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SlateBackground)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text(body, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WaterTypeCard(
    item: WaterTypeComparison,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SlateBackground)
            .border(1.dp, item.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(item.badge, color = item.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(item.title, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text("Lo bueno: ${item.pros}", color = EmeraldSuccess, style = MaterialTheme.typography.bodySmall)
        Text("A tener en cuenta: ${item.cons}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Modo Presentación limpio en pantalla completa (sin menús ni barras de vendedor).
 */
@Composable
fun CleanClientPresentationDialog(
    pricingConfig: PsaPricingConfig,
    activeCountry: PsaCountryConfig,
    initialWaterSource: DemoWaterSource = DemoWaterSource.AGUA_CORRIENTE,
    onWaterSourceChange: ((DemoWaterSource) -> Unit)? = null,
    initialTapCart: Map<String, Int> = mapOf("senior4" to 1),
    onTapCartChange: ((Map<String, Int>) -> Unit)? = null,
    initialWellCart: Map<String, Int> = mapOf("rotech" to 1),
    onWellCartChange: ((Map<String, Int>) -> Unit)? = null,
    onRequestCartBudget: (List<DemoCartSelection>, DemoBudgetPaymentMode) -> Unit = { _, _ -> },
    activeVisit: PsaHomeVisit? = null,
    onClearActiveVisit: (() -> Unit)? = null,
    onSaveDiagnosticToVisit: ((visit: PsaHomeVisit, waterSource: String, chlorinePpm: Float, hardnessPpm: Int, realLitersPerDay: Double, cartSummary: String, testPhotoPath: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PsaClientDemoSection(
            pricingConfig = pricingConfig,
            activeCountry = activeCountry,
            isFullscreenPresentation = true,
            initialWaterSource = initialWaterSource,
            onWaterSourceChange = onWaterSourceChange,
            initialTapCart = initialTapCart,
            onTapCartChange = onTapCartChange,
            initialWellCart = initialWellCart,
            onWellCartChange = onWellCartChange,
            onCloseFullscreen = onDismiss,
            onRequestCartBudget = onRequestCartBudget,
            activeVisit = activeVisit,
            onClearActiveVisit = onClearActiveVisit,
            onSaveDiagnosticToVisit = onSaveDiagnosticToVisit
        )
    }
}
