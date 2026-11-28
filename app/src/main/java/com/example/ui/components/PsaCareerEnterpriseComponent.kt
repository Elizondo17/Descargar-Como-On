package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PsaPricingConfig
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.BusinessPresentationEngine
import com.example.util.CareerRankId
import com.example.util.PsaCountryConfig
import com.example.util.PsaCountryRegistry
import com.example.util.WeeklyPlanMode
import com.example.util.WeeklyProspectingPlanState
import kotlin.math.roundToInt

/**
 * Componente interactivo de Carrera Empresarial y Plan Semanal de Prospectación.
 *
 * - Calcula automáticamente los 7 niveles de Carrera Empresarial y los 5 premios PB
 *   a partir de los precios vigentes del catálogo cargado (`pricingConfig`) y país activo.
 * - Permite alternar entre 'Medio Tiempo' (4hs L-V) y 'Full Tiempo' y ajustar las metas
 *   semanales de demostraciones, cierres, PNs y seguimientos con proyección en vivo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaCareerEnterpriseComponent(
    pricingConfig: PsaPricingConfig,
    activeCountry: PsaCountryConfig,
    planState: WeeklyProspectingPlanState,
    onPlanStateChange: (WeeklyProspectingPlanState) -> Unit,
    onSavePlan: (WeeklyProspectingPlanState) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember(activeCountry.code) {
        PsaCountryRegistry.currencyNumberFormat(activeCountry)
    }
    val catalogProducts = remember(pricingConfig) {
        pricingConfig.getAllPurifiers().take(6)
    }
    val summary = remember(pricingConfig, activeCountry, planState) {
        BusinessPresentationEngine.calculateCareerEnterpriseSummary(
            pricingConfig = pricingConfig,
            activeCountry = activeCountry,
            planState = planState
        )
    }

    var savedBannerVisible by remember { mutableStateOf(false) }

    val scheduleSlots = remember {
        listOf(
            "Mañana (09:00 - 13:00)",
            "Tarde (14:00 - 18:00)",
            "Noche (18:00 - 21:30)",
            "Fines de semana"
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("career_enterprise_component"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. HEADER + CATÁLOGO DE REFERENCIA EN VIVO
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.5.dp, PsaCyanPrimary.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PsaCyanPrimary.copy(alpha = 0.14f), SlateCard)
                        )
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PsaCyanPrimary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Carrera Empresarial PSA",
                                tint = PsaCyanGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Carrera Empresarial y Premios",
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${activeCountry.flag} ${activeCountry.name} • Recalculado según tu catálogo (${activeCountry.currencyCode})",
                                color = PsaCyanGlow,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "x${"%.2f".format(summary.catalogScaleFactor)}",
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("career_catalog_scale_badge")
                        )
                    }
                }

                Text(
                    text = "Producto de referencia del catálogo cargado:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    catalogProducts.forEach { product ->
                        val selected = summary.referencePurifierId == product.id
                        FilterChip(
                            selected = selected,
                            onClick = {
                                savedBannerVisible = false
                                onPlanStateChange(
                                    planState.copy(selectedReferencePurifierId = product.id)
                                )
                            },
                            label = {
                                Text(
                                    text = "${product.name.replace("PSA ", "").take(16)} (${currencyFormat.format(product.price)})",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PsaCyanPrimary.copy(alpha = 0.24f),
                                selectedLabelColor = PsaCyanGlow
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("career_catalog_item_${product.id}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CareerMetricTile(
                        label = "Precio catálogo base",
                        value = currencyFormat.format(summary.referenceUnitPrice),
                        accent = PsaCyanGlow,
                        tag = "career_reference_price_value",
                        modifier = Modifier.weight(1f)
                    )
                    CareerMetricTile(
                        label = "Margen 1 unidad (~35,5%)",
                        value = currencyFormat.format(summary.unitDirectProfit),
                        accent = EmeraldSuccess,
                        tag = "career_unit_profit_value",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. PREMIOS POR PUNTOS PB Y 7 NIVELES DE CARRERA (AUTO-CALCULADOS)
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, SlateCardBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("career_levels_section")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Premios por Calificación y Puntos (PB)",
                    color = AmberDelay,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Toca un premio o nivel para fijarlo como tu meta actual. Todos los montos se actualizan automáticamente con el catálogo.",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary.milestoneAwards.forEach { award ->
                        val isTarget = planState.targetRank == award.linkedRank ||
                            (planState.targetRank == CareerRankId.LJ && award.id == "pb_10000")
                        Surface(
                            color = if (isTarget) AmberDelay.copy(alpha = 0.14f) else SlateBackground,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isTarget) 1.5.dp else 1.dp,
                                color = if (isTarget) AmberDelay else SlateCardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    savedBannerVisible = false
                                    onPlanStateChange(planState.copy(targetRank = award.linkedRank))
                                }
                                .testTag("career_award_card_${award.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = AmberDelay.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${award.linkedRank.code} • ${award.pbPoints} PB",
                                                color = AmberDelay,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = award.awardTitle,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${award.incentiveDescription} (${award.teamRequirementLabel})",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currencyFormat.format(award.calculatedAmount),
                                    color = if (isTarget) AmberDelay else EmeraldSuccess,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    modifier = Modifier.testTag("career_award_amount_${award.id}")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "7 Escalones de Ingresos Mensuales en Red (Flex Plan)",
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    summary.ranks.forEach { proj ->
                        val selected = planState.targetRank == proj.rank
                        Surface(
                            color = if (selected) EmeraldSuccess.copy(alpha = 0.15f) else SlateBackground,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) EmeraldSuccess else SlateCardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    savedBannerVisible = false
                                    onPlanStateChange(planState.copy(targetRank = proj.rank))
                                }
                                .testTag("career_enterprise_rank_${proj.rank.code}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = if (selected) EmeraldSuccess else PsaCyanPrimary.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = proj.rank.code,
                                            color = if (selected) SlateBackground else PsaCyanGlow,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = proj.rank.title,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp
                                        )
                                        Text(
                                            text = proj.rank.teamSizeLabel,
                                            color = TextMuted,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                                Text(
                                    text = if (proj.estimatedMonthlyIncome <= 0.0) {
                                        "Inicio"
                                    } else {
                                        currencyFormat.format(proj.estimatedMonthlyIncome) + "/mes"
                                    },
                                    color = if (selected) EmeraldSuccess else PsaCyanGlow,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    modifier = Modifier.testTag("career_rank_income_${proj.rank.code}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. PLAN SEMANAL INTERACTIVO: MEDIO TIEMPO vs FULL TIEMPO + METAS DE PROSPECTACIÓN
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.5.dp, PurpleNeon.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("weekly_plan_interactive_section")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkHistory,
                        contentDescription = "Plan Semanal de Prospectación",
                        tint = PurpleNeon
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Plan Semanal de Prospectación",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp
                        )
                        Text(
                            text = "Alterna entre Medio Tiempo y Full Tiempo y ajusta tus metas semanales",
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }

                // Selector destacado Medio Tiempo <-> Full Tiempo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeeklyPlanModeButton(
                        title = "Medio Tiempo",
                        subtitle = "4hs L-V • Base 4D/4C/2PN",
                        selected = planState.mode == WeeklyPlanMode.HALF_TIME,
                        accent = PsaCyanGlow,
                        tag = "weekly_plan_toggle_half_time",
                        onClick = {
                            savedBannerVisible = false
                            onPlanStateChange(
                                WeeklyProspectingPlanState.forMode(
                                    mode = WeeklyPlanMode.HALF_TIME,
                                    preserving = planState
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    WeeklyPlanModeButton(
                        title = "Full Tiempo",
                        subtitle = "Completa • Base 6D/6C/5PN",
                        selected = planState.mode == WeeklyPlanMode.FULL_TIME,
                        accent = EmeraldSuccess,
                        tag = "weekly_plan_toggle_full_time",
                        onClick = {
                            savedBannerVisible = false
                            onPlanStateChange(
                                WeeklyProspectingPlanState.forMode(
                                    mode = WeeklyPlanMode.FULL_TIME,
                                    preserving = planState
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Metas semanales ajustables con steppers accesibles >= 48dp
                WeeklyGoalStepperRow(
                    label = "Demostraciones / semana",
                    sublabel = "Visitas y encuestas en hogares",
                    value = planState.demosPerWeek,
                    officialRef = planState.mode.demosCount,
                    accent = PsaCyanGlow,
                    tagPrefix = "demos",
                    onDecrement = {
                        savedBannerVisible = false
                        onPlanStateChange(planState.copy(demosPerWeek = (planState.demosPerWeek - 1).coerceAtLeast(0)))
                    },
                    onIncrement = {
                        savedBannerVisible = false
                        onPlanStateChange(planState.copy(demosPerWeek = (planState.demosPerWeek + 1).coerceAtMost(30)))
                    }
                )

                WeeklyGoalStepperRow(
                    label = "Cierres de purificadores / semana",
                    sublabel = "Equipos colocados en hogares",
                    value = planState.closesPerWeek,
                    officialRef = planState.mode.closesCount,
                    accent = EmeraldSuccess,
                    tagPrefix = "closes",
                    onDecrement = {
                        savedBannerVisible = false
                        onPlanStateChange(planState.copy(closesPerWeek = (planState.closesPerWeek - 1).coerceAtLeast(0)))
                    },
                    onIncrement = {
                        savedBannerVisible = false
                        onPlanStateChange(planState.copy(closesPerWeek = (planState.closesPerWeek + 1).coerceAtMost(30)))
                    }
                )

                WeeklyGoalStepperRow(
                    label = "Presentaciones del Negocio (PN)",
                    sublabel = "Invitados escuchando la oportunidad",
                    value = planState.pnsPerWeek,
                    officialRef = planState.mode.pnsCount,
                    accent = PurpleNeon,
                    tagPrefix = "pns",
                    onDecrement = {
                        savedBannerVisible = false
                        onPlanStateChange(planState.copy(pnsPerWeek = (planState.pnsPerWeek - 1).coerceAtLeast(0)))
                    },
                    onIncrement = {
                        savedBannerVisible = false
                        onPlanStateChange(planState.copy(pnsPerWeek = (planState.pnsPerWeek + 1).coerceAtMost(25)))
                    }
                )

                WeeklyGoalStepperRow(
                    label = "Seguimientos de invitados / semana",
                    sublabel = "2do encuentro y cierre con termómetro",
                    value = planState.followUpsPerWeek,
                    officialRef = planState.mode.followUpsCount,
                    accent = AmberDelay,
                    tagPrefix = "followups",
                    onDecrement = {
                        savedBannerVisible = false
                        onPlanStateChange(planState.copy(followUpsPerWeek = (planState.followUpsPerWeek - 1).coerceAtLeast(0)))
                    },
                    onIncrement = {
                        savedBannerVisible = false
                        onPlanStateChange(planState.copy(followUpsPerWeek = (planState.followUpsPerWeek + 1).coerceAtMost(25)))
                    }
                )

                // Días activos y franja horaria
                Text(
                    text = "Días activos y horario preferido:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(4, 5, 6, 7).forEach { days ->
                        FilterChip(
                            selected = planState.activeDaysPerWeek == days,
                            onClick = {
                                savedBannerVisible = false
                                onPlanStateChange(planState.copy(activeDaysPerWeek = days))
                            },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            label = { Text("$days días/sem", fontSize = 11.sp) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("weekly_days_$days")
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    scheduleSlots.forEach { slot ->
                        FilterChip(
                            selected = planState.selectedScheduleSlot == slot,
                            onClick = {
                                savedBannerVisible = false
                                onPlanStateChange(planState.copy(selectedScheduleSlot = slot))
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            label = { Text(slot, fontSize = 11.sp) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                // Proyección en vivo del plan semanal conectado a Carrera Empresarial
                Surface(
                    color = SlateBackground,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, EmeraldSuccess.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weekly_plan_projection_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Proyección con este ritmo (${planState.mode.label})",
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CareerMetricTile(
                                label = "Acciones / sem",
                                value = "${summary.weeklyTotalActions} (${(summary.weeklyTotalActions.toFloat() / planState.activeDaysPerWeek.coerceAtLeast(1)).roundToInt()}/día)",
                                accent = PsaCyanGlow,
                                tag = "weekly_plan_total_actions",
                                modifier = Modifier.weight(1f)
                            )
                            CareerMetricTile(
                                label = "Cierres al mes",
                                value = "${summary.monthlyClosesProjected} u. / ${summary.monthlyPnsProjected} PNs",
                                accent = PurpleNeon,
                                tag = "weekly_plan_monthly_volume",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CareerMetricTile(
                                label = "Ingreso mensual estimado",
                                value = currencyFormat.format(summary.projectedMonthlyPersonalIncome),
                                accent = EmeraldSuccess,
                                tag = "weekly_plan_monthly_income",
                                modifier = Modifier.weight(1f)
                            )
                            CareerMetricTile(
                                label = "Meta ${planState.targetRank.code} (${summary.targetAward.pbPoints} PB)",
                                value = "~${summary.estimatedMonthsToTargetRank} ${if (summary.estimatedMonthsToTargetRank == 1) "mes" else "meses"}",
                                accent = AmberDelay,
                                tag = "weekly_plan_eta_rank",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "Premio objetivo (${summary.targetAward.awardTitle}): ${currencyFormat.format(summary.targetAward.calculatedAmount)} • Incluye reuniones de equipo, VIVO PSA y autoformación.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (savedBannerVisible) {
                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weekly_plan_saved_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Plan ${planState.mode.label} guardado (${planState.demosPerWeek}D / ${planState.closesPerWeek}C / ${planState.pnsPerWeek}PN / ${planState.followUpsPerWeek}S)",
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            savedBannerVisible = false
                            onPlanStateChange(
                                WeeklyProspectingPlanState.forMode(
                                    mode = planState.mode,
                                    preserving = planState
                                )
                            )
                        },
                        border = BorderStroke(1.dp, SlateCardBorder),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(0.95f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_reset_weekly_goals")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Metas oficiales", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            savedBannerVisible = true
                            onSavePlan(planState)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.15f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_save_weekly_plan")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = SlateBackground, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar Plan", color = SlateBackground, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatefulPsaCareerEnterpriseComponent(
    pricingConfig: PsaPricingConfig,
    activeCountry: PsaCountryConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var planState by remember {
        mutableStateOf(WeeklyProspectingPlanState.loadFromPrefs(context))
    }
    PsaCareerEnterpriseComponent(
        pricingConfig = pricingConfig,
        activeCountry = activeCountry,
        planState = planState,
        onPlanStateChange = { updated -> planState = updated },
        onSavePlan = { saved -> WeeklyProspectingPlanState.saveToPrefs(context, saved) },
        modifier = modifier
    )
}

@Composable
private fun WeeklyPlanModeButton(
    title: String,
    subtitle: String,
    selected: Boolean,
    accent: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) accent.copy(alpha = 0.18f) else SlateBackground,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else SlateCardBorder),
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = title,
                    color = if (selected) accent else TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeeklyGoalStepperRow(
    label: String,
    sublabel: String,
    value: Int,
    officialRef: Int,
    accent: Color,
    tagPrefix: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Surface(
        color = SlateBackground,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "$sublabel • Base oficial: $officialRef/sem",
                        color = TextMuted,
                        fontSize = 10.5.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onDecrement,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = SlateCard,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("goal_dec_$tagPrefix")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Disminuir $label",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "$value",
                        color = accent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .testTag("goal_val_$tagPrefix")
                    )

                    IconButton(
                        onClick = onIncrement,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = accent.copy(alpha = 0.2f),
                            contentColor = accent
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("goal_inc_$tagPrefix")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aumentar $label",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { (value.toFloat() / (officialRef.coerceAtLeast(1) * 1.5f)).coerceIn(0f, 1f) },
                color = accent,
                trackColor = SlateCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun CareerMetricTile(
    label: String,
    value: String,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SlateCard)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = accent,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(tag)
        )
    }
}
