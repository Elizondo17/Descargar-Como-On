package com.example.ui.screens.psa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PsaPricingConfig
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.BusinessPresentationEngine
import com.example.util.BusinessPresentationSessionState
import com.example.util.CareerRankId
import com.example.util.CashflowQuadrant
import com.example.util.DynamicPresentationMetrics
import com.example.util.PsaCountryConfig
import com.example.util.PsaCountryRegistry
import java.text.NumberFormat

@Composable
internal fun SlideBlockARenderer(
    slideIndex: Int,
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit,
    dynamicMetrics: DynamicPresentationMetrics,
    pricingConfig: PsaPricingConfig,
    activeCountry: PsaCountryConfig,
    currencyFormat: NumberFormat,
    activeInviteName: String
) {
    when (slideIndex) {
        0 -> Slide01Cover(activeCountry = activeCountry, activeInviteName = activeInviteName)
        1 -> Slide02QuadrantEdai(state = state, onStateChange = onStateChange)
        2 -> Slide03FlexBenefits(state = state, onStateChange = onStateChange)
        3 -> Slide04DistributorRole(state = state, onStateChange = onStateChange)
        4 -> Slide05PersonalDistribution(
            state = state,
            onStateChange = onStateChange,
            dynamicMetrics = dynamicMetrics,
            currencyFormat = currencyFormat
        )
        5 -> Slide06NetworkDistribution(
            state = state,
            onStateChange = onStateChange,
            dynamicMetrics = dynamicMetrics,
            currencyFormat = currencyFormat
        )
        6 -> Slide07CareerLadder(
            state = state,
            onStateChange = onStateChange,
            dynamicMetrics = dynamicMetrics,
            currencyFormat = currencyFormat
        )
        7 -> Slide08CountryCareerTable(
            pricingConfig = pricingConfig,
            activeCountry = activeCountry
        )
        8 -> Slide09EffectiveSponsorships(state = state, onStateChange = onStateChange)
    }
}

@Composable
private fun Slide01Cover(
    activeCountry: PsaCountryConfig,
    activeInviteName: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = SlateBackground,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, PsaCyanPrimary.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${activeCountry.flag}  ${activeCountry.name} • Edición Oficial Vigente",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "Presentación del Negocio",
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 23.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Oportunidad de desarrollo personal, profesional y económico",
                    color = TextSecondary,
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (activeInviteName.isNotBlank()) {
            Surface(
                color = EmeraldSuccess.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "👋 Sesión personalizada para $activeInviteName — Cifras ajustadas en ${activeCountry.currencyCode} con tu catálogo actual.",
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            Text(
                text = "Recorre las 23 láminas organizadas en 5 bloques con cálculos dinámicos en ${activeCountry.currencyCode}.",
                color = TextSecondary,
                fontSize = 12.5.sp
            )
        }
    }
}

@Composable
private fun Slide02QuadrantEdai(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit
) {
    var selectionTargetIsCurrent by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Mode selector: Where are you vs Where do you want to be
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectionTargetIsCurrent,
                onClick = { selectionTargetIsCurrent = true },
                label = {
                    Text(
                        text = "Estoy en: ${state.currentQuadrant?.letter ?: "?"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AmberDelay.copy(alpha = 0.24f),
                    selectedLabelColor = AmberDelay
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("btn_edai_mode_current")
            )
            FilterChip(
                selected = !selectionTargetIsCurrent,
                onClick = { selectionTargetIsCurrent = false },
                label = {
                    Text(
                        text = "Quiero estar en: ${state.desiredQuadrant?.letter ?: "?"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldSuccess.copy(alpha = 0.24f),
                    selectedLabelColor = EmeraldSuccess
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("btn_edai_mode_desired")
            )
        }

        // Left vs Right context strips from PDF
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = AmberDelay.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.45f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("95% PERSONAS • 5% DINERO", color = AmberDelay, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    Text("TIEMPO POR DINERO", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Text("INGRESOS ACTIVOS", color = TextSecondary, fontSize = 10.5.sp)
                }
            }
            Surface(
                color = EmeraldSuccess.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.45f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("5% PERSONAS • 95% DINERO", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    Text("LIBERTAD FINANCIERA", color = RoseError, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("INGRESOS PASIVOS", color = TextSecondary, fontSize = 10.5.sp)
                }
            }
        }

        // 2x2 Quadrant Grid: E | D top, A | I bottom
        val rows = listOf(
            listOf(CashflowQuadrant.E, CashflowQuadrant.D),
            listOf(CashflowQuadrant.A, CashflowQuadrant.I)
        )
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { q ->
                    val isCurrent = state.currentQuadrant == q
                    val isDesired = state.desiredQuadrant == q
                    val borderColor = when {
                        isCurrent && isDesired -> PurpleNeon
                        isDesired -> EmeraldSuccess
                        isCurrent -> AmberDelay
                        else -> SlateCardBorder
                    }
                    Surface(
                        color = if (isCurrent || isDesired) borderColor.copy(alpha = 0.16f) else SlateBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(if (isCurrent || isDesired) 1.5.dp else 1.dp, borderColor),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (selectionTargetIsCurrent) {
                                    onStateChange(state.copy(currentQuadrant = q))
                                } else {
                                    onStateChange(state.copy(desiredQuadrant = q))
                                }
                            }
                            .testTag("quadrant_${q.letter.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = q.letter,
                                    color = PsaCyanPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (isCurrent) {
                                        MiniTagBadge("HOY", AmberDelay)
                                    }
                                    if (isDesired) {
                                        MiniTagBadge("META", EmeraldSuccess)
                                    }
                                }
                            }
                            Text(q.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(q.subtitle, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        PromptQuestionBanner("¿En qué cuadrante estás y en cuál querés estar?")
    }
}

@Composable
private fun Slide03FlexBenefits(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BusinessPresentationEngine.flexBenefits.forEach { option ->
            val selected = state.selectedPriorityId == option.id
            Surface(
                color = if (selected) PsaCyanPrimary.copy(alpha = 0.16f) else SlateBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) PsaCyanPrimary else SlateCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onStateChange(state.copy(selectedPriorityId = option.id)) }
                    .testTag("flex_benefit_${option.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Star,
                        contentDescription = null,
                        tint = if (selected) PsaCyanGlow else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = option.title,
                        color = if (selected) TextPrimary else TextSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        PromptQuestionBanner("¿Cuál de estos es tu prioridad?")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Slide04DistributorRole(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit
) {
    var selectedModeDetail by remember { mutableStateOf("personal") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Triple Impacto PSA",
            color = PsaCyanGlow,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Valor Social", "Valor Ambiental", "Valor Económico").forEach { pillar ->
                val active = pillar in state.selectedTripleImpactPillars
                FilterChip(
                    selected = active,
                    onClick = {
                        val updated = if (active) {
                            state.selectedTripleImpactPillars - pillar
                        } else {
                            state.selectedTripleImpactPillars + pillar
                        }
                        onStateChange(state.copy(selectedTripleImpactPillars = updated))
                    },
                    label = { Text(pillar, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        Icon(
                            imageVector = when (pillar) {
                                "Valor Social" -> Icons.Default.Groups
                                "Valor Ambiental" -> Icons.Default.Eco
                                else -> Icons.Default.Paid
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BusinessPresentationEngine.distributorRolesList.forEach { bullet ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AmberDelay,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(bullet, color = TextPrimary, fontSize = 13.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = if (selectedModeDetail == "personal") PsaCyanPrimary.copy(alpha = 0.16f) else SlateBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (selectedModeDetail == "personal") PsaCyanPrimary else SlateCardBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 74.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { selectedModeDetail = "personal" }
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(18.dp))
                    Text("Distribución personal:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Demostramos los productos en los hogares", color = TextSecondary, fontSize = 11.sp)
                }
            }
            Surface(
                color = if (selectedModeDetail == "network") PurpleNeon.copy(alpha = 0.16f) else SlateBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (selectedModeDetail == "network") PurpleNeon else SlateCardBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 74.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { selectedModeDetail = "network" }
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Hub, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(18.dp))
                    Text("Distribución en Red:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Presentamos la oportunidad del negocio", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun Slide05PersonalDistribution(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit,
    dynamicMetrics: DynamicPresentationMetrics,
    currencyFormat: NumberFormat
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Llamamos a conocidos para visitarlos, demostrarles los purificadores y que respondan una encuesta; luego les pedimos que nos recomienden a sus contactos",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = SlateBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Ganancia (1 producto)", color = TextSecondary, fontSize = 11.5.sp)
                    Text(
                        text = currencyFormat.format(dynamicMetrics.personalOneProductProfit),
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        modifier = Modifier.testTag("slide5_personal_one_profit")
                    )
                    Text("Base: ${dynamicMetrics.referenceUnitName}", color = TextMuted, fontSize = 10.5.sp)
                }
            }
            Surface(
                color = SlateBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.6f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Ganancia de 10 Productos", color = TextSecondary, fontSize = 11.5.sp)
                    Text(
                        text = currencyFormat.format(dynamicMetrics.personalTenProductsProfit),
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        modifier = Modifier.testTag("slide5_personal_ten_profit")
                    )
                    Text("Calculado con catálogo actual", color = TextMuted, fontSize = 10.5.sp)
                }
            }
        }

        Surface(
            color = PsaCyanPrimary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Si haces 30 Demos, seguramente 10 HOGARES TE DIRÁN QUE SÍ",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(12.dp)
            )
        }

        OutlinedTextField(
            value = state.personalTop10ProspectsNote,
            onValueChange = { onStateChange(state.copy(personalTop10ProspectsNote = it)) },
            label = { Text("Anota primeras ideas de personas (ej: vecinos, familia)", fontSize = 11.5.sp) },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PsaCyanPrimary,
                unfocusedBorderColor = SlateCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        PromptQuestionBanner("¿Quiénes pueden ser las primeras 10 personas a las que les mostremos nuestros purificadores?")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Slide06NetworkDistribution(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit,
    dynamicMetrics: DynamicPresentationMetrics,
    currencyFormat: NumberFormat
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "¿Conocés personas a las que les gustaría mejorar su calidad de vida?",
            color = PsaCyanGlow,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp
        )

        // Ladder preview dj -> D -> DC -> CE -> LE -> LJ -> EM
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CareerRankId.entries.forEach { rank ->
                Surface(
                    color = SlateBackground,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "${rank.code} • ${rank.title}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Surface(
            color = SlateBackground,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Un distribuidor de tu Red que comercializa 10 productos gana ${currencyFormat.format(dynamicMetrics.personalTenProductsProfit)}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "→ VOS COBRÁS ${currencyFormat.format(dynamicMetrics.networkOneDistTenProductsProfit)}",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    modifier = Modifier.testTag("slide6_network_1x10_profit")
                )
            }
        }

        Surface(
            color = SlateBackground,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "10 distribuidores que comercializan 10 productos cada uno",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "→ VOS COBRÁS ${currencyFormat.format(dynamicMetrics.networkTenDistTenProductsProfit)}",
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    modifier = Modifier.testTag("slide6_network_10x10_profit")
                )
            }
        }

        OutlinedTextField(
            value = state.networkTop10ProspectsNote,
            onValueChange = { onStateChange(state.copy(networkTop10ProspectsNote = it)) },
            label = { Text("Primeros contactos para invitar al negocio", fontSize = 11.5.sp) },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurpleNeon,
                unfocusedBorderColor = SlateCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        PromptQuestionBanner("¿Quiénes querés que sean los 10 primeros en escuchar la propuesta?")
    }
}

@Composable
private fun Slide07CareerLadder(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit,
    dynamicMetrics: DynamicPresentationMetrics,
    currencyFormat: NumberFormat
) {
    val selectedProjection = dynamicMetrics.careerRanks.firstOrNull { it.rank == state.selectedCareerRank }
        ?: dynamicMetrics.careerRanks.first()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "(Para este ejemplo cada persona comercializa 1 Purificador Semanal — cifras actualizadas con tu catálogo)",
            color = TextSecondary,
            fontSize = 12.sp
        )

        dynamicMetrics.careerRanks.forEach { proj ->
            val selected = proj.rank == state.selectedCareerRank
            Surface(
                color = if (selected) EmeraldSuccess.copy(alpha = 0.16f) else SlateBackground,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) EmeraldSuccess else SlateCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onStateChange(state.copy(selectedCareerRank = proj.rank)) }
                    .testTag("career_rank_${proj.rank.code}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = proj.rank.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "(${proj.rank.teamSizeLabel})",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = if (proj.estimatedMonthlyIncome <= 0.0) {
                            "Sin ganancias pasivas"
                        } else {
                            currencyFormat.format(proj.estimatedMonthlyIncome)
                        },
                        color = if (selected) EmeraldSuccess else PsaCyanGlow,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }

        Surface(
            color = EmeraldSuccess.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Meta seleccionada: ${selectedProjection.rank.title} (${selectedProjection.rank.teamSizeLabel})",
                color = EmeraldSuccess,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(10.dp)
            )
        }

        PromptQuestionBanner("¿Cuál categoría te permite el estilo de vida que deseas?")
    }
}

@Composable
private fun Slide08CountryCareerTable(
    pricingConfig: PsaPricingConfig,
    activeCountry: PsaCountryConfig
) {
    val rows = remember(pricingConfig, activeCountry) {
        BusinessPresentationEngine.buildCareerTableForActiveCatalog(pricingConfig, activeCountry)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "El país activo (${activeCountry.flag} ${activeCountry.name}) se recalcula automáticamente con tu catálogo cargado.",
            color = PsaCyanGlow,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .clip(RoundedCornerShape(12.dp))
                .background(SlateBackground)
                .border(1.dp, SlateCardBorder, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TableCell("PAÍS", 116, TextSecondary, true)
                TableCell("CALIFICACIÓN\n150 PB", 104, AmberDelay, true)
                TableCell("COORDINACIÓN\n600 PB", 108, EmeraldSuccess, true)
                TableCell("LIDERAZGO\n2.400 PB", 112, PsaCyanGlow, true)
                TableCell("EJECUTIVO\n10.000 PB", 116, PurpleNeon, true)
                TableCell("EMPRESARIO\n40.000 PB", 124, TextPrimary, true)
            }
            rows.forEach { row ->
                val bg = if (row.isActiveCountry) PsaCyanPrimary.copy(alpha = 0.14f) else Color.Transparent
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(
                        text = "${row.flag} ${row.countryName}${if (row.isActiveCountry) " ★" else ""}",
                        widthDp = 116,
                        color = if (row.isActiveCountry) PsaCyanGlow else TextPrimary,
                        bold = row.isActiveCountry
                    )
                    TableCell(row.pb150, 104, TextPrimary, row.isActiveCountry)
                    TableCell(row.pb600, 108, TextPrimary, row.isActiveCountry)
                    TableCell(row.pb2400, 112, TextPrimary, row.isActiveCountry)
                    TableCell(row.pb10000, 116, TextPrimary, row.isActiveCountry)
                    TableCell(row.pb40000, 124, EmeraldSuccess, row.isActiveCountry)
                }
            }
        }
    }
}

@Composable
private fun TableCell(text: String, widthDp: Int, color: Color, bold: Boolean) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(widthDp.dp)
    )
}

@Composable
private fun Slide09EffectiveSponsorships(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit
) {
    val stages = listOf(
        Triple(1, "1era Etapa de Patrocinio (D -> DC)", "Tus primeros patrocinios directos y el desarrollo de tu 1er DC con sus 5 líneas activas."),
        Triple(2, "2da Etapa de Patrocinio (DC -> Crecimiento)", "Consolidás el volumen de equipo y acompañás la formación de tu 2do DC."),
        Triple(3, "3era Etapa de Patrocinio (Hacia CE)", "Con tu 3er DC desarrollado y 15 patrocinios efectivos alcanzás Coordinador de Equipo (CE).")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Ruta visual de D → DC → CE con tus 3 pilares (1er DC, 2do DC y 3er DC)",
            color = TextSecondary,
            fontSize = 12.5.sp
        )

        stages.forEach { (stageNum, title, desc) ->
            val selected = state.selectedSponsorshipStage == stageNum
            val accent = when (stageNum) {
                1 -> PsaCyanPrimary
                2 -> RoseError
                else -> PurpleNeon
            }
            Surface(
                color = if (selected) accent.copy(alpha = 0.16f) else SlateBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else SlateCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onStateChange(state.copy(selectedSponsorshipStage = stageNum)) }
                    .testTag("sponsorship_stage_$stageNum")
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        MiniTagBadge("${stageNum * 5} / 15", accent)
                    }
                    Text(desc, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
internal fun PromptQuestionBanner(question: String) {
    Surface(
        color = SlateBackground,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "💬 Pregunta guía: $question",
            color = PurpleNeon,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSize = 12.5.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
internal fun MiniTagBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
