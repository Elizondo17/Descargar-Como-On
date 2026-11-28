package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PsaPricingConfig
import com.example.data.repository.PsaFinancingConfig
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalIsLargeFontMode
import com.example.ui.theme.PsaBlueDark
import com.example.ui.theme.PsaCardBorder
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.PsaWaterBg
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DemoWaterSource
import com.example.util.PsaCountryConfig
import com.example.util.PsaCountryRegistry
import com.example.util.PsaHardnessStripCatalog
import java.util.Locale
import kotlin.math.abs

private const val TOTAL_TUTORIAL_STEPS = 4

/**
 * Tutorial interactivo de bienvenida mostrado en el primer ingreso del usuario.
 * Explica de forma práctica e interactiva el flujo completo de:
 *  1. Bienvenida y circuito de venta PSA
 *  2. Demo de Agua (Agua Corriente / Clorímetro vs Agua de Pozo / Dureza)
 *  3. Cotizador (Equipos, Contado, Anticipo, Cuotas y Envío)
 *  4. Acceso directo a la Demo o al Cotizador
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaWelcomeTutorialDialog(
    pricingConfig: PsaPricingConfig,
    financingConfig: PsaFinancingConfig,
    activeCountry: PsaCountryConfig,
    onStartDemo: () -> Unit,
    onStartBudget: () -> Unit,
    onDismiss: () -> Unit
) {
    val isLargeFont = LocalIsLargeFontMode.current
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var selectedFlowHighlight by rememberSaveable { mutableIntStateOf(0) }

    // Interactive Step 2 state (Demo de Agua sandbox)
    var sampleWaterSource by remember { mutableStateOf(DemoWaterSource.AGUA_CORRIENTE) }
    var sampleChlorinePpm by remember { mutableFloatStateOf(1.0f) }
    var sampleHardnessIdx by remember { mutableIntStateOf(4) }

    // Interactive Step 3 state (Cotizador sandbox)
    var sampleMonths by rememberSaveable { mutableIntStateOf(6) }
    var includeSampleAdvance by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        if (currentStep > 0) {
            currentStep -= 1
        } else {
            onDismiss()
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (currentStep + 1).toFloat() / TOTAL_TUTORIAL_STEPS.toFloat(),
        animationSpec = tween(durationMillis = 250),
        label = "tutorial_progress"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            color = SlateCard,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.2.dp, PsaCyanPrimary.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp)
                .widthIn(max = 540.dp)
                .heightIn(max = 680.dp)
                .testTag("welcome_tutorial_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // --- HEADER ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(PsaBlueDark.copy(alpha = 0.9f), SlateCard)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PsaCyanPrimary.copy(alpha = 0.18f))
                                    .border(1.dp, PsaCyanGlow.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = "Tutorial PSA",
                                    tint = PsaCyanGlow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Bienvenido a Como On",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.5.sp
                                )
                                Text(
                                    text = "Paso ${currentStep + 1} de $TOTAL_TUTORIAL_STEPS • Guía Interactiva",
                                    color = PsaCyanGlow,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("tutorial_step_label")
                                )
                            }
                        }

                        TextButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("tutorial_btn_skip")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Omitir tutorial",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Omitir",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PsaCyanPrimary,
                        trackColor = SlateBackground
                    )
                }

                // --- SCROLLABLE INTERACTIVE BODY ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(180))
                        },
                        label = "tutorial_step_content"
                    ) { step ->
                        when (step) {
                            0 -> WelcomeOverviewStep(
                                selectedHighlight = selectedFlowHighlight,
                                onSelectHighlight = { selectedFlowHighlight = it },
                                onJumpToStep = { currentStep = it }
                            )

                            1 -> InteractiveWaterDemoStep(
                                waterSource = sampleWaterSource,
                                onWaterSourceChange = { sampleWaterSource = it },
                                chlorinePpm = sampleChlorinePpm,
                                onChlorineChange = { sampleChlorinePpm = it },
                                hardnessIdx = sampleHardnessIdx,
                                onHardnessChange = { sampleHardnessIdx = it },
                                isLargeFont = isLargeFont
                            )

                            2 -> InteractiveQuoteStep(
                                pricingConfig = pricingConfig,
                                financingConfig = financingConfig,
                                activeCountry = activeCountry,
                                preferredWaterSource = sampleWaterSource,
                                selectedMonths = sampleMonths,
                                onSelectMonths = { sampleMonths = it },
                                includeAdvance = includeSampleAdvance,
                                onToggleAdvance = { includeSampleAdvance = !includeSampleAdvance }
                            )

                            else -> FinishTutorialStep(
                                onStartDemo = onStartDemo,
                                onStartBudget = onStartBudget,
                                isLargeFont = isLargeFont
                            )
                        }
                    }
                }

                HorizontalDivider(color = SlateCardBorder)

                // --- FOOTER NAVIGATION ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateSurface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Interactive step dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(TOTAL_TUTORIAL_STEPS) { index ->
                            val isActive = index == currentStep
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .height(8.dp)
                                    .width(if (isActive) 24.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) PsaCyanPrimary else SlateCardBorder
                                    )
                                    .clickable { currentStep = index }
                                    .testTag("tutorial_dot_$index")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 0) {
                            OutlinedButton(
                                onClick = { currentStep -= 1 },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SlateCardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("tutorial_btn_prev")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Atrás", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        if (currentStep < TOTAL_TUTORIAL_STEPS - 1) {
                            Button(
                                onClick = { currentStep += 1 },
                                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("tutorial_btn_next")
                            ) {
                                Text(
                                    text = "Siguiente",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("tutorial_btn_finish")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Finalizar",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeOverviewStep(
    selectedHighlight: Int,
    onSelectHighlight: (Int) -> Unit,
    onJumpToStep: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PsaWaterBg),
            border = BorderStroke(1.dp, PsaCardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "💧 Tu herramienta de demostración y cierre",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Text(
                    text = "En menos de 1 minuto aprendé a mostrar la calidad del agua frente al cliente y pasar directo a calcular el presupuesto ideal. Tocá cada tarjeta para ver su función:",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }
        }

        val items = listOf(
            Triple(
                "1. Demo de Agua en Vivo",
                "Compará cloro (Agua Corriente) o sarro/dureza (Agua de Pozo), tomá foto del test y calculá el ahorro anual.",
                Icons.Default.Science
            ),
            Triple(
                "2. Carrito Inteligente",
                "Elegí los purificadores recomendados y envialos directo al Cotizador con un solo toque.",
                Icons.Default.ShoppingCart
            ),
            Triple(
                "3. Cotizador y Envío",
                "Calculá descuento de contado, adelanto en efectivo, cuotas y compartí el presupuesto por WhatsApp.",
                Icons.Default.Calculate
            )
        )

        items.forEachIndexed { index, (title, detail, icon) ->
            val selected = selectedHighlight == index
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) PsaCyanPrimary.copy(alpha = 0.14f) else SlateBackground
                ),
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) PsaCyanGlow else SlateCardBorder
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectHighlight(index) }
                    .testTag("tutorial_overview_card_$index")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (selected) PsaCyanPrimary.copy(alpha = 0.22f) else SlateCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) PsaCyanGlow else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = title,
                            color = if (selected) PsaCyanGlow else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = detail,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        if (selectedHighlight == 0 || selectedHighlight == 2) {
            val targetStep = if (selectedHighlight == 0) 1 else 2
            val btnLabel = if (selectedHighlight == 0) "Ir ahora al paso Demo de Agua" else "Ir ahora al paso Cotizador"
            OutlinedButton(
                onClick = { onJumpToStep(targetStep) },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("tutorial_overview_jump_btn")
            ) {
                Text(btnLabel, color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InteractiveWaterDemoStep(
    waterSource: DemoWaterSource,
    onWaterSourceChange: (DemoWaterSource) -> Unit,
    chlorinePpm: Float,
    onChlorineChange: (Float) -> Unit,
    hardnessIdx: Int,
    onHardnessChange: (Int) -> Unit,
    isLargeFont: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "🔬 1. Cómo usar la Demo de Agua",
            color = PsaCyanGlow,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp
        )
        Text(
            text = "En la pantalla principal seleccionás si el cliente tiene Agua Corriente (Red) o Agua de Pozo. ¡Probá cambiar los controles acá abajo!",
            color = TextSecondary,
            fontSize = 12.5.sp,
            lineHeight = 17.sp
        )

        // Interactive source toggle matching PsaClientDemoSection
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
                val selected = waterSource == src
                val pillColor = if (src == DemoWaterSource.AGUA_CORRIENTE) PsaCyanPrimary else Color(0xFFEC407A)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) pillColor else Color.Transparent)
                        .clickable { onWaterSourceChange(src) }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .testTag(
                            if (src == DemoWaterSource.AGUA_CORRIENTE) "tutorial_source_tap" else "tutorial_source_well"
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = src.label,
                        color = if (selected) Color.Black else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (waterSource == DemoWaterSource.AGUA_CORRIENTE) {
            val (stageColor, stageTitle, stageAccent, stageSummary) = remember(chlorinePpm) {
                when {
                    chlorinePpm <= 0.15f -> listOf(
                        Color(0xFFE0F7FA),
                        "0.0 ppm • Sin cloro residual",
                        EmeraldSuccess,
                        "Así sale el agua filtrada por PSA: cristalina, sin olor a lavandina ni trihalometanos."
                    )
                    chlorinePpm <= 0.65f -> listOf(
                        Color(0xFFFFF59D),
                        "0.5 ppm • Cloro mínimo de red",
                        Color(0xFFFFCA28),
                        "El reactivo vira a amarillo suave: hay cloro residual en la canilla."
                    )
                    chlorinePpm <= 1.25f -> listOf(
                        Color(0xFFFFEE58),
                        "1.0 ppm • Concentración media/alta",
                        AmberDelay,
                        "Amarillo intenso: altera el sabor de comidas/infusiones y genera THMs al hervir."
                    )
                    chlorinePpm <= 2.05f -> listOf(
                        Color(0xFFFFA726),
                        "2.0 ppm • Carga química elevada",
                        RoseError,
                        "Ámbar fuerte: irrita mucosas, reseca piel/cabello en ducha e impacta la salud diaria."
                    )
                    else -> listOf(
                        Color(0xFFE65100),
                        "3.0 ppm • Concentración crítica",
                        Color(0xFFFF5252),
                        "Naranja profundo: exceso notorio de cloro libre tras purgas o refuerzos."
                    )
                }
            }
            val liquidColor = stageColor as Color
            val accentColor = stageAccent as Color
            val animatedLiquid by animateColorAsState(
                targetValue = liquidColor,
                animationSpec = tween(240),
                label = "tutorial_chlorine_color"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateBackground),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Probá el Clorímetro interactivo:",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TutorialGlassPreview(
                            label = "Canilla",
                            ppmLabel = "${String.format(Locale.US, "%.1f", chlorinePpm)} ppm",
                            liquidColor = animatedLiquid,
                            accent = accentColor,
                            modifier = Modifier.weight(1f)
                        )
                        TutorialGlassPreview(
                            label = "Con PSA",
                            ppmLabel = "0.0 ppm",
                            liquidColor = Color(0xFFE0F7FA),
                            accent = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            0.0f to "0.0 PSA",
                            1.0f to "1.0 Medio",
                            2.0f to "2.0 Alto",
                            3.0f to "3.0 Crítico"
                        ).forEach { (ppm, label) ->
                            val active = abs(chlorinePpm - ppm) < 0.25f
                            FilterChip(
                                selected = active,
                                onClick = { onChlorineChange(ppm) },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (ppm == 0f) EmeraldSuccess.copy(alpha = 0.25f) else PsaCyanPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White,
                                    containerColor = SlateCard,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("tutorial_chlorine_chip_$ppm")
                            )
                        }
                    }

                    Slider(
                        value = chlorinePpm,
                        onValueChange = onChlorineChange,
                        valueRange = 0f..3f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = SlateCardBorder
                        ),
                        modifier = Modifier.testTag("tutorial_chlorine_slider")
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.14f))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = stageTitle as String,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = stageSummary as String,
                                color = TextPrimary,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        } else {
            val stages = PsaHardnessStripCatalog.stages
            val activeStage = stages[hardnessIdx.coerceIn(0, stages.lastIndex)]
            val stripColor = Color(activeStage.swatchHex)

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateBackground),
                border = BorderStroke(1.dp, stripColor.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Probá la Tira Reactiva de Dureza/Sarro:",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        stages.forEach { st ->
                            val selected = st.index == activeStage.index
                            val swatch = Color(st.swatchHex)
                            FilterChip(
                                selected = selected,
                                onClick = { onHardnessChange(st.index) },
                                label = {
                                    Text(
                                        text = "${st.ppmCaCO3} • ${st.colorName.substringBefore(" ")}",
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = swatch.copy(alpha = 0.3f),
                                    selectedLabelColor = Color.White,
                                    containerColor = SlateCard,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = SlateCardBorder,
                                    selectedBorderColor = swatch
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("tutorial_hardness_chip_${st.index}")
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(stripColor.copy(alpha = 0.14f))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${activeStage.ppmCaCO3} • ${activeStage.hardnessLevel}",
                                color = if (activeStage.index <= 1) EmeraldSuccess else PsaCyanGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = activeStage.recommendedAction,
                                color = TextPrimary,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Concise tips for Demo features
        Card(
            colors = CardDefaults.cardColors(containerColor = PsaWaterBg),
            border = BorderStroke(1.dp, PsaCardBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "💡 En la Demo real además podés:",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "• Sacar foto al test OTO/tira para detectar el color automáticamente.\n• Calcular litros diarios y gasto anual en bidones de la familia.\n• Tocar 'Cotizar' (Tarjeta o Efectivo) para llevar el carrito directo al Cotizador.",
                    color = TextSecondary,
                    fontSize = if (isLargeFont) 12.sp else 11.5.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun TutorialGlassPreview(
    label: String,
    ppmLabel: String,
    liquidColor: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SlateCard)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .size(width = 58.dp, height = 56.dp)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                .background(Color(0xFF0F172A))
                .border(
                    1.5.dp,
                    Color.White.copy(alpha = 0.65f),
                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(liquidColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ppmLabel,
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InteractiveQuoteStep(
    pricingConfig: PsaPricingConfig,
    financingConfig: PsaFinancingConfig,
    activeCountry: PsaCountryConfig,
    preferredWaterSource: DemoWaterSource,
    selectedMonths: Int,
    onSelectMonths: (Int) -> Unit,
    includeAdvance: Boolean,
    onToggleAdvance: () -> Unit
) {
    val currencyFormat = remember(activeCountry.code) {
        PsaCountryRegistry.currencyNumberFormat(activeCountry)
    }
    val purifiers = remember(pricingConfig) { pricingConfig.getAllPurifiers() }
    val sampleModel = remember(purifiers, preferredWaterSource) {
        val targetId = if (preferredWaterSource == DemoWaterSource.AGUA_DE_POZO) "rotech" else "senior4"
        purifiers.firstOrNull { it.id == targetId } ?: purifiers.first()
    }
    val listPrice = sampleModel.price
    val cashDiscountPct = 0.0
    val cashTotal = (listPrice * (1.0 - cashDiscountPct / 100.0)).coerceAtLeast(0.0)
    val advanceAmount = if (includeAdvance && selectedMonths > 1) listPrice * 0.20 else 0.0
    val financedBalance = (listPrice - advanceAmount).coerceAtLeast(0.0)
    val interestPct = when (selectedMonths) {
        1 -> 0f
        3 -> financingConfig.defaultInterest3Months
        6 -> financingConfig.defaultInterest6Months
        12 -> financingConfig.defaultInterest12Months
        else -> 0f
    }
    val totalWithInterest = financedBalance * (1.0 + interestPct / 100.0)
    val installmentValue = if (selectedMonths > 1) totalWithInterest / selectedMonths else cashTotal

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "💰 2. Cómo usar el Cotizador",
            color = EmeraldSuccess,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp
        )
        Text(
            text = "Al pasar desde la Demo, el equipo ya llega cargado en el carrito. Probá simular un plan con precios reales de ${activeCountry.flag} ${activeCountry.name}:",
            color = TextSecondary,
            fontSize = 12.5.sp,
            lineHeight = 17.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = SlateBackground),
            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Equipo ejemplo (${preferredWaterSource.label}):",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = sampleModel.name,
                            color = PsaCyanGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                    Text(
                        text = currencyFormat.format(listPrice),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "Elegí forma de pago o cuotas:",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        1 to if (cashDiscountPct > 0) "Contado (${cashDiscountPct.toInt()}% OFF)" else "Contado",
                        3 to "3 Cuotas",
                        6 to "6 Cuotas",
                        12 to "12 Cuotas"
                    ).forEach { (months, label) ->
                        val active = selectedMonths == months
                        FilterChip(
                            selected = active,
                            onClick = { onSelectMonths(months) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (months == 1) EmeraldSuccess.copy(alpha = 0.25f) else PurpleNeon.copy(alpha = 0.25f),
                                selectedLabelColor = Color.White,
                                containerColor = SlateCard,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("tutorial_quote_plan_$months")
                        )
                    }
                }

                if (selectedMonths > 1) {
                    FilterChip(
                        selected = includeAdvance,
                        onClick = onToggleAdvance,
                        label = {
                            Text(
                                text = if (includeAdvance) "✓ Con adelanto 20% (${currencyFormat.format(advanceAmount)})" else "+ Simular adelanto / seña 20%",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSuccess.copy(alpha = 0.22f),
                            selectedLabelColor = EmeraldSuccess,
                            containerColor = SlateCard,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("tutorial_quote_advance_toggle")
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF042436))
                        .border(1.dp, PsaCyanGlow.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .testTag("tutorial_quote_result_card"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedMonths == 1) {
                                if (cashDiscountPct > 0) "TOTAL CONTADO (${cashDiscountPct.toInt()}% OFF)" else "TOTAL CONTADO (PRECIO DE LISTA)"
                            } else {
                                "CUOTA MENSUAL ($selectedMonths MESES)"
                            },
                            color = PsaCyanGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedMonths == 1) {
                                currencyFormat.format(cashTotal)
                            } else {
                                "${currencyFormat.format(installmentValue)} / mes"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        if (selectedMonths > 1) {
                            Text(
                                text = "Recargo configurado: ${interestPct.toInt()}%${if (includeAdvance) " • Saldo descontando seña" else ""}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = PsaWaterBg),
            border = BorderStroke(1.dp, PsaCardBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "💡 En el Cotizador real además podés:",
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "• Sumar varios purificadores o cargos adicionales al mismo presupuesto.\n• Ver la barra de amortización vs. bidones (en cuántos meses se paga solo).\n• Enviar el presupuesto armado por WhatsApp o copiarlo con 1 toque.",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun FinishTutorialStep(
    onStartDemo: () -> Unit,
    onStartBudget: () -> Unit,
    isLargeFont: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A241B)),
            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "¡Ya conocés lo esencial!",
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Elegí por dónde querés empezar ahora. Podés volver a abrir este tutorial en cualquier momento desde el menú lateral ☰ o en ⚙️ Ayuda.",
                    color = TextPrimary,
                    fontSize = if (isLargeFont) 12.5.sp else 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Button(
            onClick = onStartDemo,
            colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("tutorial_btn_go_demo")
        ) {
            Icon(Icons.Default.Science, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Probar Demo de Agua",
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }

        OutlinedButton(
            onClick = onStartBudget,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.2.dp, EmeraldSuccess),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("tutorial_btn_go_budget")
        ) {
            Icon(Icons.Default.Calculate, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Ir al Cotizador",
                color = EmeraldSuccess,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
    }
}
