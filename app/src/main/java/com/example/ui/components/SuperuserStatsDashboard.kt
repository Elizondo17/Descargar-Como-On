package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SellerActivityBucket
import com.example.data.model.SellerPerformanceStats
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
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.max
import kotlin.math.roundToInt

enum class SuperuserStatsSortMode(val label: String) {
    CONVERSION("Mayor Conversión"),
    MESSAGES("Más Mensajes"),
    SUCCESS_RATE("Mejor Entrega %")
}

enum class SuperuserConversionBasis(val label: String) {
    PORTFOLIO("Conv. / Cartera"),
    DEMOS("Cierre / Demos")
}

data class SuperuserSellerMetric(
    val raw: SellerPerformanceStats,
    val sellerName: String,
    val sellerEmail: String,
    val messagesSuccess: Int,
    val messagesFailed: Int,
    val messagesSent: Int,
    val messageSuccessRatePct: Float,
    val portfolioConversionRatePct: Float,
    val demoConversionRatePct: Float,
    val activeConversionRatePct: Float
)

data class SuperuserDashboardSummary(
    val sellers: List<SuperuserSellerMetric>,
    val totalMessagesSent: Int,
    val totalMessagesSuccess: Int,
    val totalMessagesFailed: Int,
    val globalMessageSuccessRatePct: Float,
    val totalContacts: Int,
    val totalVisits: Int,
    val totalDemos: Int,
    val totalSales: Int,
    val globalConversionRatePct: Float,
    val averageSellerConversionRatePct: Float,
    val recent7DayBuckets: List<SellerActivityBucket>
)

fun computeSuperuserDashboardSummary(
    stats: List<SellerPerformanceStats>,
    sortMode: SuperuserStatsSortMode = SuperuserStatsSortMode.CONVERSION,
    conversionBasis: SuperuserConversionBasis = SuperuserConversionBasis.PORTFOLIO
): SuperuserDashboardSummary {
    val metrics = stats.map { stat ->
        val msgOk = stat.messagesSuccess.coerceAtLeast(0)
        val msgFail = stat.messagesFailed.coerceAtLeast(0)
        val msgSent = maxOf(stat.messagesTotal.coerceAtLeast(0), msgOk + msgFail)
        val msgRate = if (msgSent > 0) (msgOk * 100f / msgSent).coerceIn(0f, 100f) else 0f

        val sales = stat.closedSales.coerceAtLeast(0)
        val contacts = stat.totalContacts.coerceAtLeast(0)
        val visits = stat.totalVisits.coerceAtLeast(0)
        val demos = stat.completedDemos.coerceAtLeast(0)

        val portfolioDenom = maxOf(contacts, visits, sales)
        val portfolioConv = if (portfolioDenom > 0) (sales * 100f / portfolioDenom).coerceIn(0f, 100f) else 0f

        val demoDenom = maxOf(demos, sales)
        val demoConv = if (demoDenom > 0) (sales * 100f / demoDenom).coerceIn(0f, 100f) else 0f

        val activeConv = when (conversionBasis) {
            SuperuserConversionBasis.PORTFOLIO -> portfolioConv
            SuperuserConversionBasis.DEMOS -> demoConv
        }

        SuperuserSellerMetric(
            raw = stat,
            sellerName = stat.sellerName.trim().ifBlank {
                stat.sellerEmail.substringBefore("@").ifBlank { "Vendedor PSA" }
            },
            sellerEmail = stat.sellerEmail.trim(),
            messagesSuccess = msgOk,
            messagesFailed = msgFail,
            messagesSent = msgSent,
            messageSuccessRatePct = msgRate,
            portfolioConversionRatePct = portfolioConv,
            demoConversionRatePct = demoConv,
            activeConversionRatePct = activeConv
        )
    }

    val sortedMetrics = when (sortMode) {
        SuperuserStatsSortMode.CONVERSION -> metrics.sortedWith(
            compareByDescending<SuperuserSellerMetric> { it.activeConversionRatePct }
                .thenByDescending { it.raw.closedSales }
                .thenByDescending { it.messagesSent }
                .thenBy { it.sellerName.lowercase() }
        )
        SuperuserStatsSortMode.MESSAGES -> metrics.sortedWith(
            compareByDescending<SuperuserSellerMetric> { it.messagesSent }
                .thenByDescending { it.messageSuccessRatePct }
                .thenByDescending { it.activeConversionRatePct }
                .thenBy { it.sellerName.lowercase() }
        )
        SuperuserStatsSortMode.SUCCESS_RATE -> metrics.sortedWith(
            compareByDescending<SuperuserSellerMetric> { it.messageSuccessRatePct }
                .thenByDescending { it.messagesSent }
                .thenByDescending { it.activeConversionRatePct }
                .thenBy { it.sellerName.lowercase() }
        )
    }

    val totalMsgOk = sortedMetrics.sumOf { it.messagesSuccess }
    val totalMsgFail = sortedMetrics.sumOf { it.messagesFailed }
    val totalMsgSent = sortedMetrics.sumOf { it.messagesSent }
    val globalMsgRate = if (totalMsgSent > 0) (totalMsgOk * 100f / totalMsgSent).coerceIn(0f, 100f) else 0f

    val totalContacts = sortedMetrics.sumOf { it.raw.totalContacts.coerceAtLeast(0) }
    val totalVisits = sortedMetrics.sumOf { it.raw.totalVisits.coerceAtLeast(0) }
    val totalDemos = sortedMetrics.sumOf { it.raw.completedDemos.coerceAtLeast(0) }
    val totalSales = sortedMetrics.sumOf { it.raw.closedSales.coerceAtLeast(0) }

    val globalConvDenom = when (conversionBasis) {
        SuperuserConversionBasis.PORTFOLIO -> maxOf(totalContacts, totalVisits, totalSales)
        SuperuserConversionBasis.DEMOS -> maxOf(totalDemos, totalSales)
    }
    val globalConvRate = if (globalConvDenom > 0) (totalSales * 100f / globalConvDenom).coerceIn(0f, 100f) else 0f

    val activeConversionSellers = sortedMetrics.filter { metric ->
        when (conversionBasis) {
            SuperuserConversionBasis.PORTFOLIO -> maxOf(metric.raw.totalContacts, metric.raw.totalVisits, metric.raw.closedSales) > 0
            SuperuserConversionBasis.DEMOS -> maxOf(metric.raw.completedDemos, metric.raw.closedSales) > 0
        }
    }
    val avgSellerConv = if (activeConversionSellers.isNotEmpty()) {
        activeConversionSellers.map { it.activeConversionRatePct }.average().toFloat().coerceIn(0f, 100f)
    } else 0f

    val aggregatedByDay = linkedMapOf<String, SellerActivityBucket>()
    stats.forEach { stat ->
        stat.parsedRecentBuckets.takeLast(7).forEach { bucket ->
            val key = bucket.dayKey.ifBlank { bucket.label }
            if (key.isNotBlank()) {
                val prev = aggregatedByDay[key]
                aggregatedByDay[key] = if (prev == null) {
                    bucket.copy(dayKey = key)
                } else {
                    prev.copy(
                        label = prev.label.ifBlank { bucket.label },
                        messagesSuccess = prev.messagesSuccess + bucket.messagesSuccess.coerceAtLeast(0),
                        messagesFailed = prev.messagesFailed + bucket.messagesFailed.coerceAtLeast(0),
                        callsCount = prev.callsCount + bucket.callsCount.coerceAtLeast(0),
                        appointmentsCount = prev.appointmentsCount + bucket.appointmentsCount.coerceAtLeast(0),
                        demosCount = prev.demosCount + bucket.demosCount.coerceAtLeast(0)
                    )
                }
            }
        }
    }
    val recent7Days = aggregatedByDay.values
        .sortedBy { it.dayKey }
        .takeLast(7)

    return SuperuserDashboardSummary(
        sellers = sortedMetrics,
        totalMessagesSent = totalMsgSent,
        totalMessagesSuccess = totalMsgOk,
        totalMessagesFailed = totalMsgFail,
        globalMessageSuccessRatePct = globalMsgRate,
        totalContacts = totalContacts,
        totalVisits = totalVisits,
        totalDemos = totalDemos,
        totalSales = totalSales,
        globalConversionRatePct = globalConvRate,
        averageSellerConversionRatePct = avgSellerConv,
        recent7DayBuckets = recent7Days
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuperuserStatsDashboard(
    stats: List<SellerPerformanceStats>,
    isCloudConnected: Boolean,
    onRefreshStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMode by remember { mutableStateOf(SuperuserStatsSortMode.CONVERSION) }
    var conversionBasis by remember { mutableStateOf(SuperuserConversionBasis.PORTFOLIO) }
    var selectedSellerUid by remember { mutableStateOf<String?>(null) }

    val summary = remember(stats, sortMode, conversionBasis) {
        computeSuperuserDashboardSummary(
            stats = stats,
            sortMode = sortMode,
            conversionBasis = conversionBasis
        )
    }

    val selectedMetric = remember(summary.sellers, selectedSellerUid) {
        summary.sellers.firstOrNull { it.raw.ownerUid == selectedSellerUid }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(SlateCard, SlateSurface)
                )
            )
            .border(1.dp, PsaCyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(16.dp)
            .testTag("superuser_stats_dashboard"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row
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
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PsaCyanPrimary.copy(alpha = 0.16f))
                        .border(1.dp, PsaCyanGlow.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Dashboard estadístico de vendedores",
                        tint = PsaCyanGlow,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Dashboard Estadístico",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Surface(
                            color = if (isCloudConnected) EmeraldSuccess.copy(alpha = 0.18f) else AmberDelay.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isCloudConnected) "FIRESTORE" else "LOCAL",
                                color = if (isCloudConnected) EmeraldSuccess else AmberDelay,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Rendimiento de mensajes y conversión por vendedor",
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }

            IconButton(
                onClick = onRefreshStats,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("superuser_dashboard_refresh_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar estadísticas desde Firestore",
                    tint = PsaCyanGlow
                )
            }
        }

        if (summary.sellers.isEmpty()) {
            SuperuserDashboardEmptyState(
                isCloudConnected = isCloudConnected,
                onRefreshStats = onRefreshStats
            )
            return@Column
        }

        // Controls: Sort + Conversion Basis
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SuperuserStatsSortMode.entries.forEach { mode ->
                FilterChip(
                    selected = sortMode == mode,
                    onClick = { sortMode = mode },
                    label = { Text(mode.label, fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("superuser_sort_${mode.name.lowercase()}")
                )
            }
            SuperuserConversionBasis.entries.forEach { basis ->
                FilterChip(
                    selected = conversionBasis == basis,
                    onClick = { conversionBasis = basis },
                    label = { Text(basis.label, fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurpleNeon.copy(alpha = 0.25f),
                        selectedLabelColor = PsaCyanGlow
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("superuser_basis_${basis.name.lowercase()}")
                )
            }
        }

        // KPI Summary Strip
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuperuserKpiCard(
                label = "Mensajes Totales",
                value = "${summary.totalMessagesSent}",
                sublabel = "${summary.totalMessagesSuccess} ok • ${summary.totalMessagesFailed} fallo",
                accent = PsaCyanPrimary,
                modifier = Modifier
                    .weight(1f, fill = true)
                    .testTag("superuser_kpi_messages_total")
            )
            SuperuserKpiCard(
                label = "Éxito Envío",
                value = "${summary.globalMessageSuccessRatePct.roundToInt()}%",
                sublabel = "Entrega promedio",
                accent = EmeraldSuccess,
                modifier = Modifier
                    .weight(1f, fill = true)
                    .testTag("superuser_kpi_message_rate")
            )
            SuperuserKpiCard(
                label = "Conversión Global",
                value = "${"%.1f".format(summary.globalConversionRatePct)}%",
                sublabel = "${summary.totalSales} cierres / ${summary.totalDemos} demos",
                accent = PurpleNeon,
                modifier = Modifier
                    .weight(1f, fill = true)
                    .testTag("superuser_kpi_conversion_rate")
            )
        }

        // Chart 1: Rendimiento de Mensajes Enviados por Vendedor (Exitosos vs Fallidos)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SlateBackground.copy(alpha = 0.78f))
                .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                .padding(12.dp)
                .testTag("superuser_messages_chart_card"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = PsaCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Rendimiento de Mensajes por Vendedor",
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Toca una barra para inspeccionar envíos y conversión",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuperuserLegendDot(EmeraldSuccess, "Ok")
                    SuperuserLegendDot(RoseError, "Fallo")
                }
            }

            if (summary.totalMessagesSent == 0) {
                SuperuserInlineNotice("Aún no hay mensajes enviados registrados en Firestore.")
            } else {
                SuperuserMessagesBarChartCanvas(
                    sellers = summary.sellers.take(14),
                    selectedUid = selectedSellerUid,
                    onSelectSeller = { uid ->
                        selectedSellerUid = if (selectedSellerUid == uid) null else uid
                    }
                )
            }
        }

        // Chart 2: Tasa de Conversión por Vendedor (%) + Team Average Line
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SlateBackground.copy(alpha = 0.78f))
                .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                .padding(12.dp)
                .testTag("superuser_conversion_chart_card"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = PurpleNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Tasa de Conversión por Vendedor",
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${conversionBasis.label} • Promedio equipo: ${"%.1f".format(summary.averageSellerConversionRatePct)}%",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                SuperuserLegendDot(AmberDelay, "Prom.")
            }

            SuperuserConversionChartCanvas(
                sellers = summary.sellers.take(14),
                averageConversionPct = summary.averageSellerConversionRatePct,
                selectedUid = selectedSellerUid,
                onSelectSeller = { uid ->
                    selectedSellerUid = if (selectedSellerUid == uid) null else uid
                }
            )
        }

        // Selected Seller Interactive Tooltip Detail
        AnimatedVisibility(visible = selectedMetric != null) {
            selectedMetric?.let { metric ->
                SuperuserSellerTooltipCard(metric = metric)
            }
        }

        // Chart 3: Tendencia Agregada 7 Días (si existe actividad diaria en Firestore)
        if (summary.recent7DayBuckets.any { it.totalEvents > 0 }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SlateBackground.copy(alpha = 0.78f))
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tendencia de Equipo (Últimos 7 días)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuperuserLegendDot(EmeraldSuccess, "Msgs Ok")
                        SuperuserLegendDot(PsaCyanGlow, "Demos")
                    }
                }
                Superuser7DayTrendCanvas(buckets = summary.recent7DayBuckets)
            }
        }

        // Compact Comparative Ranking Table per Seller
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SlateBackground.copy(alpha = 0.78f))
                .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = null,
                    tint = AmberDelay,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Comparativa por Vendedor (${summary.sellers.size})",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            summary.sellers.forEachIndexed { idx, metric ->
                val isSelected = metric.raw.ownerUid == selectedSellerUid
                Surface(
                    color = if (isSelected) PsaCyanPrimary.copy(alpha = 0.14f) else SlateCard,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) PsaCyanGlow else SlateCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clickable {
                            selectedSellerUid = if (isSelected) null else metric.raw.ownerUid
                        }
                        .testTag("superuser_ranking_row_${metric.raw.ownerUid}")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${idx + 1} ${metric.sellerName}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Conv. ${"%.1f".format(metric.activeConversionRatePct)}%",
                                color = if (metric.activeConversionRatePct >= summary.averageSellerConversionRatePct && metric.activeConversionRatePct > 0f) {
                                    EmeraldSuccess
                                } else {
                                    PsaCyanGlow
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Msgs: ${metric.messagesSuccess}/${metric.messagesSent} (${metric.messageSuccessRatePct.roundToInt()}% ok)",
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                            Text(
                                text = "${metric.raw.closedSales} ventas • ${metric.raw.completedDemos} demos",
                                color = TextMuted,
                                fontSize = 11.5.sp
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (metric.activeConversionRatePct / 100f).coerceIn(0f, 1f) },
                            color = PurpleNeon,
                            trackColor = SlateSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuperuserKpiCard(
    label: String,
    value: String,
    sublabel: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SlateBackground.copy(alpha = 0.85f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            color = accent,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            maxLines = 1
        )
        Text(
            text = label,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = sublabel,
            color = TextMuted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SuperuserMessagesBarChartCanvas(
    sellers: List<SuperuserSellerMetric>,
    selectedUid: String?,
    onSelectSeller: (String) -> Unit
) {
    val density = LocalDensity.current
    val maxTotal = remember(sellers) {
        max(1, sellers.maxOfOrNull { it.messagesSent } ?: 1)
    }
    val growth by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 350),
        label = "msg_bar_growth"
    )

    val labelPaint = remember(density) {
        Paint().apply {
            isAntiAlias = true
            color = TextSecondary.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val valuePaint = remember(density) {
        Paint().apply {
            isAntiAlias = true
            color = PsaCyanGlow.toArgb()
            textSize = with(density) { 9.5.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val axisPaint = remember(density) {
        Paint().apply {
            isAntiAlias = true
            color = TextMuted.toArgb()
            textSize = with(density) { 9.5.sp.toPx() }
            textAlign = Paint.Align.RIGHT
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val computedWidth = (64.dp * sellers.size.coerceAtLeast(1)).coerceAtLeast(maxWidth - 4.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Canvas(
                modifier = Modifier
                    .width(computedWidth)
                    .height(176.dp)
                    .pointerInput(sellers) {
                        detectTapGestures { offset ->
                            val leftMargin = 32.dp.toPx()
                            val usableWidth = (size.width - leftMargin - 10.dp.toPx()).coerceAtLeast(1f)
                            val slotW = usableWidth / sellers.size.coerceAtLeast(1)
                            val idx = ((offset.x - leftMargin) / slotW).toInt()
                            sellers.getOrNull(idx)?.let { onSelectSeller(it.raw.ownerUid) }
                        }
                    }
                    .testTag("superuser_messages_chart_canvas")
            ) {
                val leftMargin = 32.dp.toPx()
                val topPad = 18.dp.toPx()
                val bottomPad = 28.dp.toPx()
                val chartH = (size.height - topPad - bottomPad).coerceAtLeast(10f)
                val chartW = (size.width - leftMargin - 10.dp.toPx()).coerceAtLeast(10f)
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

                for (step in 0..3) {
                    val f = step / 3f
                    val y = topPad + chartH - (chartH * f)
                    drawLine(
                        color = SlateCardBorder.copy(alpha = 0.7f),
                        start = Offset(leftMargin, y),
                        end = Offset(size.width - 8.dp.toPx(), y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (step > 0) dash else null
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "${(maxTotal * f).roundToInt()}",
                        leftMargin - 6.dp.toPx(),
                        y + 3.dp.toPx(),
                        axisPaint
                    )
                }

                val slotW = chartW / sellers.size.coerceAtLeast(1)
                val barW = (slotW * 0.48f).coerceIn(16.dp.toPx(), 34.dp.toPx())

                sellers.forEachIndexed { idx, seller ->
                    val cx = leftMargin + (slotW * idx) + (slotW / 2f)
                    val baseY = topPad + chartH
                    val isSelected = seller.raw.ownerUid == selectedUid

                    if (isSelected) {
                        drawRoundRect(
                            color = PsaCyanPrimary.copy(alpha = 0.14f),
                            topLeft = Offset(cx - slotW * 0.44f, topPad - 6.dp.toPx()),
                            size = Size(slotW * 0.88f, chartH + 10.dp.toPx()),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }

                    val okH = (seller.messagesSuccess.toFloat() / maxTotal) * chartH * growth
                    val failH = (seller.messagesFailed.toFloat() / maxTotal) * chartH * growth
                    val barLeft = cx - (barW / 2f)

                    if (okH > 0f) {
                        drawRoundRect(
                            color = EmeraldSuccess,
                            topLeft = Offset(barLeft, baseY - okH),
                            size = Size(barW, okH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                    if (failH > 0f) {
                        drawRoundRect(
                            color = RoseError,
                            topLeft = Offset(barLeft, baseY - okH - failH),
                            size = Size(barW, failH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    val totalH = okH + failH
                    if (seller.messagesSent > 0) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "${seller.messagesSent}",
                            cx,
                            (baseY - totalH - 5.dp.toPx()).coerceAtLeast(11.dp.toPx()),
                            valuePaint
                        )
                    }

                    val shortName = seller.sellerName.split(" ").firstOrNull().orEmpty().take(8).ifBlank { "Vend." }
                    drawContext.canvas.nativeCanvas.drawText(
                        shortName,
                        cx,
                        size.height - 8.dp.toPx(),
                        labelPaint
                    )
                }
            }
        }
    }
}

@Composable
private fun SuperuserConversionChartCanvas(
    sellers: List<SuperuserSellerMetric>,
    averageConversionPct: Float,
    selectedUid: String?,
    onSelectSeller: (String) -> Unit
) {
    val density = LocalDensity.current
    val maxScalePct = remember(sellers) {
        val peak = sellers.maxOfOrNull { it.activeConversionRatePct } ?: 0f
        when {
            peak <= 25f -> 25f
            peak <= 50f -> 50f
            else -> 100f
        }
    }
    val growth by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 380),
        label = "conv_bar_growth"
    )

    val labelPaint = remember(density) {
        Paint().apply {
            isAntiAlias = true
            color = TextSecondary.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val pctPaint = remember(density) {
        Paint().apply {
            isAntiAlias = true
            color = TextPrimary.toArgb()
            textSize = with(density) { 9.5.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val axisPaint = remember(density) {
        Paint().apply {
            isAntiAlias = true
            color = TextMuted.toArgb()
            textSize = with(density) { 9.sp.toPx() }
            textAlign = Paint.Align.RIGHT
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val computedWidth = (64.dp * sellers.size.coerceAtLeast(1)).coerceAtLeast(maxWidth - 4.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Canvas(
                modifier = Modifier
                    .width(computedWidth)
                    .height(176.dp)
                    .pointerInput(sellers) {
                        detectTapGestures { offset ->
                            val leftMargin = 36.dp.toPx()
                            val usableWidth = (size.width - leftMargin - 10.dp.toPx()).coerceAtLeast(1f)
                            val slotW = usableWidth / sellers.size.coerceAtLeast(1)
                            val idx = ((offset.x - leftMargin) / slotW).toInt()
                            sellers.getOrNull(idx)?.let { onSelectSeller(it.raw.ownerUid) }
                        }
                    }
                    .testTag("superuser_conversion_chart_canvas")
            ) {
                val leftMargin = 36.dp.toPx()
                val topPad = 18.dp.toPx()
                val bottomPad = 28.dp.toPx()
                val chartH = (size.height - topPad - bottomPad).coerceAtLeast(10f)
                val chartW = (size.width - leftMargin - 10.dp.toPx()).coerceAtLeast(10f)
                val gridDash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                for (step in 0..2) {
                    val f = step / 2f
                    val y = topPad + chartH - (chartH * f)
                    drawLine(
                        color = SlateCardBorder.copy(alpha = 0.65f),
                        start = Offset(leftMargin, y),
                        end = Offset(size.width - 8.dp.toPx(), y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (step > 0) gridDash else null
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "${(maxScalePct * f).roundToInt()}%",
                        leftMargin - 6.dp.toPx(),
                        y + 3.dp.toPx(),
                        axisPaint
                    )
                }

                val slotW = chartW / sellers.size.coerceAtLeast(1)
                val barW = (slotW * 0.46f).coerceIn(16.dp.toPx(), 32.dp.toPx())

                sellers.forEachIndexed { idx, seller ->
                    val cx = leftMargin + (slotW * idx) + (slotW / 2f)
                    val baseY = topPad + chartH
                    val isSelected = seller.raw.ownerUid == selectedUid

                    if (isSelected) {
                        drawRoundRect(
                            color = PurpleNeon.copy(alpha = 0.16f),
                            topLeft = Offset(cx - slotW * 0.44f, topPad - 6.dp.toPx()),
                            size = Size(slotW * 0.88f, chartH + 10.dp.toPx()),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }

                    val barH = ((seller.activeConversionRatePct / maxScalePct).coerceIn(0f, 1f)) * chartH * growth
                    val barLeft = cx - (barW / 2f)
                    if (barH > 0f) {
                        val barColor = if (seller.activeConversionRatePct >= averageConversionPct) {
                            EmeraldSuccess
                        } else {
                            PurpleNeon
                        }
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(barColor, PsaCyanPrimary.copy(alpha = 0.8f)),
                                startY = baseY - barH,
                                endY = baseY
                            ),
                            topLeft = Offset(barLeft, baseY - barH),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                        )
                    } else {
                        drawLine(
                            color = SlateCardBorder,
                            start = Offset(barLeft, baseY - 2.dp.toPx()),
                            end = Offset(barLeft + barW, baseY - 2.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        "${seller.activeConversionRatePct.roundToInt()}%",
                        cx,
                        (baseY - barH - 5.dp.toPx()).coerceAtLeast(11.dp.toPx()),
                        pctPaint
                    )

                    val shortName = seller.sellerName.split(" ").firstOrNull().orEmpty().take(8).ifBlank { "Vend." }
                    drawContext.canvas.nativeCanvas.drawText(
                        shortName,
                        cx,
                        size.height - 8.dp.toPx(),
                        labelPaint
                    )
                }

                // Team Average Reference Line
                if (averageConversionPct > 0f) {
                    val avgY = topPad + chartH - ((averageConversionPct / maxScalePct).coerceIn(0f, 1f) * chartH)
                    drawLine(
                        color = AmberDelay,
                        start = Offset(leftMargin, avgY),
                        end = Offset(size.width - 8.dp.toPx(), avgY),
                        strokeWidth = 1.8.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                }
            }
        }
    }
}

@Composable
private fun Superuser7DayTrendCanvas(
    buckets: List<SellerActivityBucket>
) {
    val density = LocalDensity.current
    val maxVal = remember(buckets) {
        max(1, buckets.maxOfOrNull { max(it.messagesSuccess, it.demosCount) } ?: 1)
    }
    val labelPaint = remember(density) {
        Paint().apply {
            isAntiAlias = true
            color = TextSecondary.toArgb()
            textSize = with(density) { 9.5.sp.toPx() }
            textAlign = Paint.Align.CENTER
        }
    }
    val axisPaint = remember(density) {
        Paint().apply {
            isAntiAlias = true
            color = TextMuted.toArgb()
            textSize = with(density) { 9.sp.toPx() }
            textAlign = Paint.Align.RIGHT
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .testTag("superuser_trend_chart_canvas")
    ) {
        val leftMargin = 28.dp.toPx()
        val topPad = 12.dp.toPx()
        val bottomPad = 24.dp.toPx()
        val chartH = (size.height - topPad - bottomPad).coerceAtLeast(10f)
        val chartW = (size.width - leftMargin - 8.dp.toPx()).coerceAtLeast(10f)

        for (step in 0..2) {
            val f = step / 2f
            val y = topPad + chartH - (chartH * f)
            drawLine(
                color = SlateCardBorder.copy(alpha = 0.55f),
                start = Offset(leftMargin, y),
                end = Offset(size.width - 8.dp.toPx(), y),
                strokeWidth = 1.dp.toPx()
            )
            drawContext.canvas.nativeCanvas.drawText(
                "${(maxVal * f).roundToInt()}",
                leftMargin - 5.dp.toPx(),
                y + 3.dp.toPx(),
                axisPaint
            )
        }

        val slotW = chartW / buckets.size.coerceAtLeast(1)
        val barW = (slotW * 0.4f).coerceIn(10.dp.toPx(), 22.dp.toPx())
        val demoPts = mutableListOf<Offset>()

        buckets.forEachIndexed { idx, bucket ->
            val cx = leftMargin + (slotW * idx) + (slotW / 2f)
            val baseY = topPad + chartH
            val msgH = (bucket.messagesSuccess.toFloat() / maxVal) * chartH
            if (msgH > 0f) {
                drawRoundRect(
                    color = EmeraldSuccess.copy(alpha = 0.85f),
                    topLeft = Offset(cx - barW / 2f, baseY - msgH),
                    size = Size(barW, msgH),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
            val demoY = baseY - ((bucket.demosCount.toFloat() / maxVal) * chartH)
            demoPts.add(Offset(cx, demoY))

            drawContext.canvas.nativeCanvas.drawText(
                bucket.label.ifBlank { bucket.dayKey.takeLast(5) },
                cx,
                size.height - 6.dp.toPx(),
                labelPaint
            )
        }

        if (demoPts.size >= 2) {
            val linePath = Path().apply {
                moveTo(demoPts.first().x, demoPts.first().y)
                demoPts.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = linePath,
                color = PsaCyanGlow,
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        demoPts.forEach { pt ->
            drawCircle(color = SlateBackground, radius = 3.8.dp.toPx(), center = pt)
            drawCircle(color = PsaCyanGlow, radius = 2.5.dp.toPx(), center = pt)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuperuserSellerTooltipCard(
    metric: SuperuserSellerMetric
) {
    Surface(
        color = SlateCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("superuser_seller_detail_tooltip")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metric.sellerName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (metric.sellerEmail.isNotBlank()) {
                        Text(
                            text = metric.sellerEmail,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Surface(
                    color = EmeraldSuccess.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${"%.1f".format(metric.activeConversionRatePct)}% conv.",
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "📤 ${metric.messagesSent} enviados (${metric.messagesSuccess} ok / ${metric.messagesFailed} fallos • ${metric.messageSuccessRatePct.roundToInt()}%)",
                    color = PsaCyanGlow,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "👥 ${metric.raw.totalContacts} contactos • 🏠 ${metric.raw.totalVisits} visitas",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
                Text(
                    text = "🔬 ${metric.raw.completedDemos} demos • ✅ ${metric.raw.closedSales} ventas (${"%.1f".format(metric.demoConversionRatePct)}% cierre/demo)",
                    color = EmeraldSuccess,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SuperuserDashboardEmptyState(
    isCloudConnected: Boolean,
    onRefreshStats: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SlateBackground.copy(alpha = 0.72f))
            .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
            .padding(20.dp)
            .testTag("superuser_dashboard_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = "Sin estadísticas de Firestore",
            tint = TextMuted,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = if (isCloudConnected) {
                "Todavía no hay métricas de mensajes o ventas sincronizadas desde Firestore."
            } else {
                "Conecta tu cuenta Google para consultar las estadísticas de cada vendedor desde Firestore."
            },
            color = TextSecondary,
            fontSize = 12.5.sp
        )
        OutlinedButton(
            onClick = onRefreshStats,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PsaCyanGlow),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("superuser_dashboard_empty_refresh_btn")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sincronizar Firestore", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SuperuserLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = TextSecondary, fontSize = 10.5.sp)
    }
}

@Composable
private fun SuperuserInlineNotice(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SlateCard)
            .padding(12.dp)
    )
}
