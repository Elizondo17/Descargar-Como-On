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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.DeliveryStats
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private enum class AnalyticsScope {
    TEAM_BY_SELLER,
    MY_ACTIVITY
}

private enum class AnalyticsWindow {
    LAST_7_DAYS,
    ALL_TIME
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardAnalyticsChartsSection(
    teamStats: List<SellerPerformanceStats>,
    localBuckets: List<SellerActivityBucket>,
    deliveryStats: DeliveryStats,
    currentUserEmail: String,
    canViewSubordinates: Boolean,
    onRefreshBoard: () -> Unit,
    onInspectSeller: ((SellerPerformanceStats) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cleanCurrentEmail = remember(currentUserEmail) { currentUserEmail.trim().lowercase() }
    val hasTeamRows = remember(teamStats, canViewSubordinates) {
        canViewSubordinates || teamStats.size > 1
    }

    var scope by remember(hasTeamRows) {
        mutableStateOf(if (hasTeamRows) AnalyticsScope.TEAM_BY_SELLER else AnalyticsScope.MY_ACTIVITY)
    }
    var window by remember { mutableStateOf(AnalyticsWindow.LAST_7_DAYS) }
    var selectedSellerIndex by remember { mutableIntStateOf(-1) }
    var selectedDayIndex by remember { mutableIntStateOf(-1) }

    val selfStats = remember(teamStats, cleanCurrentEmail, deliveryStats) {
        teamStats.firstOrNull {
            cleanCurrentEmail.isNotBlank() && it.sellerEmail.trim().lowercase() == cleanCurrentEmail
        } ?: SellerPerformanceStats(
            ownerUid = "local",
            sellerName = "Mi Cuenta",
            sellerEmail = cleanCurrentEmail,
            messagesSuccess = deliveryStats.successCount,
            messagesFailed = deliveryStats.failedCount,
            messagesTotal = deliveryStats.totalCount
        )
    }

    val activeRows = remember(scope, teamStats, selfStats) {
        when (scope) {
            AnalyticsScope.TEAM_BY_SELLER -> if (teamStats.isNotEmpty()) teamStats else listOf(selfStats)
            AnalyticsScope.MY_ACTIVITY -> listOf(selfStats)
        }
    }

    val displayBuckets = remember(scope, activeRows, localBuckets) {
        if (scope == AnalyticsScope.MY_ACTIVITY && localBuckets.isNotEmpty()) {
            localBuckets
        } else {
            val allSellerBuckets = activeRows.map { it.parsedRecentBuckets }.filter { it.isNotEmpty() }
            if (allSellerBuckets.isEmpty()) {
                localBuckets
            } else {
                val base = allSellerBuckets.first()
                base.mapIndexed { idx, bucket ->
                    var sumOk = 0
                    var sumFail = 0
                    var sumCalls = 0
                    allSellerBuckets.forEach { list ->
                        val match = list.getOrNull(idx) ?: list.find { it.dayKey == bucket.dayKey }
                        if (match != null) {
                            sumOk += match.messagesSuccess
                            sumFail += match.messagesFailed
                            sumCalls += match.callsCount
                        }
                    }
                    bucket.copy(
                        messagesSuccess = sumOk,
                        messagesFailed = sumFail,
                        callsCount = sumCalls
                    )
                }
            }
        }
    }

    val totalMsgOk = remember(window, activeRows, displayBuckets) {
        if (window == AnalyticsWindow.LAST_7_DAYS && displayBuckets.isNotEmpty()) {
            displayBuckets.sumOf { it.messagesSuccess }
        } else {
            activeRows.sumOf { it.messagesSuccess }
        }
    }
    val totalMsgFail = remember(window, activeRows, displayBuckets) {
        if (window == AnalyticsWindow.LAST_7_DAYS && displayBuckets.isNotEmpty()) {
            displayBuckets.sumOf { it.messagesFailed }
        } else {
            activeRows.sumOf { it.messagesFailed }
        }
    }
    val totalMessages = totalMsgOk + totalMsgFail
    val msgSuccessRate = if (totalMessages > 0) (totalMsgOk * 100f / totalMessages) else 0f

    val totalCalls = remember(window, activeRows, displayBuckets) {
        if (window == AnalyticsWindow.LAST_7_DAYS && displayBuckets.isNotEmpty()) {
            displayBuckets.sumOf { it.callsCount }
        } else {
            activeRows.sumOf { it.callsAttempted }
        }
    }
    val totalConnectedCalls = remember(activeRows) {
        activeRows.sumOf { it.callsConnected }
    }
    val callRate = if (totalCalls > 0) {
        ((totalConnectedCalls.coerceAtMost(totalCalls)) * 100f / totalCalls)
    } else 0f

    val selectedSeller = activeRows.getOrNull(selectedSellerIndex)
    val selectedDay = displayBuckets.getOrNull(selectedDayIndex)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(SlateCard, SlateSurface)
                )
            )
            .border(1.dp, SlateCardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .testTag("dashboard_analytics_section"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(IndigoPrimary.copy(alpha = 0.18f))
                        .border(1.dp, IndigoPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analítica visual",
                        tint = PsaCyanGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Analítica de Mensajería y Equipo",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    )
                    Text(
                        text = if (scope == AnalyticsScope.TEAM_BY_SELLER) {
                            "${activeRows.size} vendedor(es) visibles en tu rama"
                        } else {
                            "Métricas personales de mensajes y llamadas"
                        },
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }

            IconButton(
                onClick = onRefreshBoard,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("analytics_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar analítica",
                    tint = PsaCyanPrimary
                )
            }
        }

        // Scope + Window Controls
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (hasTeamRows) {
                FilterChip(
                    selected = scope == AnalyticsScope.TEAM_BY_SELLER,
                    onClick = {
                        scope = AnalyticsScope.TEAM_BY_SELLER
                        selectedSellerIndex = -1
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Por Vendedor", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.Black,
                        selectedLeadingIconColor = Color.Black
                    ),
                    modifier = Modifier.testTag("analytics_scope_team_chip")
                )
            }

            FilterChip(
                selected = scope == AnalyticsScope.MY_ACTIVITY,
                onClick = {
                    scope = AnalyticsScope.MY_ACTIVITY
                    selectedSellerIndex = 0
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text("Mi Actividad", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = IndigoPrimary,
                    selectedLabelColor = Color.Black,
                    selectedLeadingIconColor = Color.Black
                ),
                modifier = Modifier.testTag("analytics_scope_self_chip")
            )

            FilterChip(
                selected = window == AnalyticsWindow.LAST_7_DAYS,
                onClick = { window = AnalyticsWindow.LAST_7_DAYS },
                label = { Text("Últimos 7 días", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PurpleNeon.copy(alpha = 0.25f),
                    selectedLabelColor = PsaCyanGlow
                ),
                modifier = Modifier.testTag("analytics_window_7d_chip")
            )

            FilterChip(
                selected = window == AnalyticsWindow.ALL_TIME,
                onClick = { window = AnalyticsWindow.ALL_TIME },
                label = { Text("Acumulado", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PurpleNeon.copy(alpha = 0.25f),
                    selectedLabelColor = PsaCyanGlow
                ),
                modifier = Modifier.testTag("analytics_window_all_chip")
            )
        }

        // KPI Summary Strip
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsMiniKpi(
                label = "Msgs Exitosos",
                value = "$totalMsgOk",
                accent = EmeraldSuccess,
                modifier = Modifier.weight(1f, fill = true)
            )
            AnalyticsMiniKpi(
                label = "Fallos",
                value = "$totalMsgFail",
                accent = RoseError,
                modifier = Modifier.weight(1f, fill = true)
            )
            AnalyticsMiniKpi(
                label = "Efectividad",
                value = "${"%.0f".format(msgSuccessRate)}%",
                accent = PsaCyanPrimary,
                modifier = Modifier.weight(1f, fill = true)
            )
            AnalyticsMiniKpi(
                label = "Llamadas",
                value = "$totalCalls",
                accent = AmberDelay,
                modifier = Modifier.weight(1f, fill = true)
            )
        }

        // Donut Gauges: Messaging Success Ratio + Call Effectiveness
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SlateBackground.copy(alpha = 0.72f))
                .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DonutEffectivenessRing(
                title = "Éxito Mensajes",
                subtitle = "$totalMsgOk ok / $totalMsgFail fallo",
                percentage = msgSuccessRate,
                primaryColor = EmeraldSuccess,
                secondaryColor = if (totalMsgFail > 0) RoseError else SlateCardBorder,
                modifier = Modifier.weight(1f)
            )
            DonutEffectivenessRing(
                title = "Llamadas / Citas",
                subtitle = "$totalConnectedCalls conect. / $totalCalls tot.",
                percentage = callRate,
                primaryColor = PsaCyanPrimary,
                secondaryColor = AmberDelay.copy(alpha = 0.35f),
                modifier = Modifier.weight(1f)
            )
        }

        // Interactive Per-Seller Stacked Bar Chart (Exitosos vs Fallos)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SlateBackground.copy(alpha = 0.72f))
                .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
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
                        text = "Éxito vs. Fallos por Vendedor",
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Toca una barra para ver llamadas y coaching",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChartLegendDot(color = EmeraldSuccess, label = "Ok")
                    ChartLegendDot(color = RoseError, label = "Fallo")
                }
            }

            if (activeRows.isEmpty() || activeRows.all { it.messagesTotal == 0 && it.callsAttempted == 0 }) {
                AnalyticsEmptyChartNotice(
                    message = "Aún no hay envíos ni llamadas registradas en este rango. Cuando se registren contactos PSA o campañas, verás las barras comparativas aquí."
                )
            } else {
                SellerStackedBarChartCanvas(
                    sellers = activeRows.take(10),
                    selectedIndex = selectedSellerIndex,
                    onSelectIndex = { idx ->
                        selectedSellerIndex = if (selectedSellerIndex == idx) -1 else idx
                    }
                )
            }

            AnimatedVisibility(visible = selectedSeller != null) {
                selectedSeller?.let { stats ->
                    SellerTooltipCoachingCard(
                        stats = stats,
                        onOpenCoaching = onInspectSeller?.let { cb -> { cb(stats) } }
                    )
                }
            }
        }

        // 7-Day Trend Area & Bar Chart
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SlateBackground.copy(alpha = 0.72f))
                .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
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
                        text = "Actividad Reciente (7 Días)",
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Toca un día para ver mensajes y llamadas",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChartLegendDot(color = EmeraldSuccess, label = "Ok")
                    ChartLegendDot(color = RoseError, label = "Fallo")
                    ChartLegendDot(color = PsaCyanPrimary, label = "Llamadas")
                }
            }

            if (displayBuckets.isEmpty() || displayBuckets.all { it.totalEvents == 0 }) {
                AnalyticsEmptyChartNotice(
                    message = "Sin actividad diaria en los últimos 7 días todavía."
                )
            } else {
                WeeklyActivityTrendCanvas(
                    buckets = displayBuckets,
                    selectedDayIndex = selectedDayIndex,
                    onSelectDay = { idx ->
                        selectedDayIndex = if (selectedDayIndex == idx) -1 else idx
                    }
                )
            }

            AnimatedVisibility(visible = selectedDay != null) {
                selectedDay?.let { bucket ->
                    Surface(
                        color = SlateCard,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "📅 ${bucket.label.ifBlank { bucket.dayKey }}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                            Text(
                                text = "✅ ${bucket.messagesSuccess} ok   ❌ ${bucket.messagesFailed} fallos   📞 ${bucket.callsCount} llamadas",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Recent Activity Per Seller Feed
        if (activeRows.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Actividad Reciente por Vendedor",
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                activeRows.take(6).forEachIndexed { idx, seller ->
                    SellerRecentActivityRow(
                        stats = seller,
                        isSelected = idx == selectedSellerIndex,
                        onClick = {
                            if (onInspectSeller != null) {
                                onInspectSeller(seller)
                            } else {
                                selectedSellerIndex = if (selectedSellerIndex == idx) -1 else idx
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsMiniKpi(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SlateBackground.copy(alpha = 0.8f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = value,
            color = accent,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            maxLines = 1
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DonutEffectivenessRing(
    title: String,
    subtitle: String,
    percentage: Float,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedPct by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 100f),
        animationSpec = tween(durationMillis = 550),
        label = "donut_pct"
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 7.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                drawArc(
                    color = secondaryColor.copy(alpha = 0.65f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                val sweep = (animatedPct / 100f) * 360f
                if (sweep > 0f) {
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                text = "${animatedPct.roundToInt()}%",
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 10.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SellerStackedBarChartCanvas(
    sellers: List<SellerPerformanceStats>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val density = LocalDensity.current
    val maxTotal = remember(sellers) {
        max(
            1,
            sellers.maxOfOrNull {
                max(it.messagesSuccess + it.messagesFailed, it.callsAttempted)
            } ?: 1
        )
    }

    val labelPaint = remember(density) {
        Paint().apply {
            isAntiAlias = true
            color = TextSecondary.toArgb()
            textSize = with(density) { 10.sp.toPx() }
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
        val minSlotWidth = 60.dp
        val computedWidth = (minSlotWidth * sellers.size.coerceAtLeast(1)).coerceAtLeast(maxWidth - 8.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Canvas(
                modifier = Modifier
                    .width(computedWidth)
                    .height(172.dp)
                    .pointerInput(sellers) {
                        detectTapGestures { offset ->
                            val leftMargin = 32.dp.toPx()
                            val usableWidth = (size.width - leftMargin - 10.dp.toPx()).coerceAtLeast(1f)
                            val slotW = usableWidth / sellers.size.coerceAtLeast(1)
                            val tappedIdx = ((offset.x - leftMargin) / slotW).toInt()
                            if (tappedIdx in sellers.indices) {
                                onSelectIndex(tappedIdx)
                            }
                        }
                    }
                    .testTag("analytics_seller_bar_chart")
            ) {
                val leftMargin = 32.dp.toPx()
                val topPad = 14.dp.toPx()
                val bottomPad = 28.dp.toPx()
                val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(10f)
                val chartWidth = (size.width - leftMargin - 10.dp.toPx()).coerceAtLeast(10f)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

                // Horizontal grid lines + Y-axis labels
                val steps = 3
                for (i in 0..steps) {
                    val fraction = i.toFloat() / steps
                    val y = topPad + chartHeight - (chartHeight * fraction)
                    drawLine(
                        color = SlateCardBorder.copy(alpha = 0.7f),
                        start = Offset(leftMargin, y),
                        end = Offset(size.width - 8.dp.toPx(), y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (i > 0) dashEffect else null
                    )
                    val tickVal = (maxTotal * fraction).roundToInt()
                    drawContext.canvas.nativeCanvas.drawText(
                        "$tickVal",
                        leftMargin - 6.dp.toPx(),
                        y + 3.dp.toPx(),
                        axisPaint
                    )
                }

                val slotW = chartWidth / sellers.size.coerceAtLeast(1)
                val barW = (slotW * 0.44f).coerceIn(14.dp.toPx(), 32.dp.toPx())
                val callBarW = (barW * 0.42f).coerceAtLeast(6.dp.toPx())

                sellers.forEachIndexed { idx, seller ->
                    val centerX = leftMargin + (slotW * idx) + (slotW / 2f)
                    val isSelected = idx == selectedIndex
                    if (isSelected) {
                        drawRoundRect(
                            color = PsaCyanPrimary.copy(alpha = 0.14f),
                            topLeft = Offset(centerX - slotW * 0.44f, topPad - 4.dp.toPx()),
                            size = Size(slotW * 0.88f, chartHeight + 8.dp.toPx()),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }

                    val msgTotal = seller.messagesSuccess + seller.messagesFailed
                    val totalBarH = (msgTotal.toFloat() / maxTotal) * chartHeight
                    val okH = (seller.messagesSuccess.toFloat() / maxTotal) * chartHeight
                    val failH = (seller.messagesFailed.toFloat() / maxTotal) * chartHeight
                    val barLeft = centerX - (barW / 2f) - (callBarW * 0.35f)
                    val baseY = topPad + chartHeight

                    // Success bar segment (bottom)
                    if (okH > 0f) {
                        drawRoundRect(
                            color = EmeraldSuccess,
                            topLeft = Offset(barLeft, baseY - okH),
                            size = Size(barW, okH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                    // Failed bar segment (stacked on top)
                    if (failH > 0f) {
                        drawRoundRect(
                            color = RoseError,
                            topLeft = Offset(barLeft, baseY - okH - failH),
                            size = Size(barW, failH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                    if (totalBarH == 0f) {
                        drawLine(
                            color = SlateCardBorder,
                            start = Offset(barLeft, baseY - 2.dp.toPx()),
                            end = Offset(barLeft + barW, baseY - 2.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Adjacent thin Call bar
                    val callH = (seller.callsAttempted.toFloat() / maxTotal) * chartHeight
                    if (callH > 0f) {
                        drawRoundRect(
                            color = PsaCyanPrimary,
                            topLeft = Offset(barLeft + barW + 3.dp.toPx(), baseY - callH),
                            size = Size(callBarW, callH),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }

                    val shortName = seller.sellerName
                        .trim()
                        .split(" ")
                        .firstOrNull()
                        .orEmpty()
                        .take(8)
                        .ifBlank { "Vend." }
                    drawContext.canvas.nativeCanvas.drawText(
                        shortName,
                        centerX,
                        size.height - 8.dp.toPx(),
                        labelPaint
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyActivityTrendCanvas(
    buckets: List<SellerActivityBucket>,
    selectedDayIndex: Int,
    onSelectDay: (Int) -> Unit
) {
    val density = LocalDensity.current
    val maxDayTotal = remember(buckets) {
        max(1, buckets.maxOfOrNull { max(it.messagesSuccess + it.messagesFailed, it.callsCount) } ?: 1)
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
            textSize = with(density) { 9.5.sp.toPx() }
            textAlign = Paint.Align.RIGHT
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .pointerInput(buckets) {
                detectTapGestures { offset ->
                    val leftMargin = 28.dp.toPx()
                    val usableW = (size.width - leftMargin - 8.dp.toPx()).coerceAtLeast(1f)
                    val slotW = usableW / buckets.size.coerceAtLeast(1)
                    val idx = ((offset.x - leftMargin) / slotW).toInt()
                    if (idx in buckets.indices) {
                        onSelectDay(idx)
                    }
                }
            }
            .testTag("analytics_7day_trend_chart")
    ) {
        val leftMargin = 28.dp.toPx()
        val topPad = 12.dp.toPx()
        val bottomPad = 24.dp.toPx()
        val chartH = (size.height - topPad - bottomPad).coerceAtLeast(10f)
        val chartW = (size.width - leftMargin - 8.dp.toPx()).coerceAtLeast(10f)
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

        for (step in 0..2) {
            val f = step / 2f
            val y = topPad + chartH - (chartH * f)
            drawLine(
                color = SlateCardBorder.copy(alpha = 0.65f),
                start = Offset(leftMargin, y),
                end = Offset(size.width - 8.dp.toPx(), y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (step > 0) dash else null
            )
            drawContext.canvas.nativeCanvas.drawText(
                "${(maxDayTotal * f).roundToInt()}",
                leftMargin - 5.dp.toPx(),
                y + 3.dp.toPx(),
                axisPaint
            )
        }

        val slotW = chartW / buckets.size.coerceAtLeast(1)
        val barW = (slotW * 0.42f).coerceIn(10.dp.toPx(), 24.dp.toPx())
        val callLinePoints = mutableListOf<Offset>()

        buckets.forEachIndexed { idx, bucket ->
            val cx = leftMargin + (slotW * idx) + (slotW / 2f)
            val baseY = topPad + chartH

            if (idx == selectedDayIndex) {
                drawRoundRect(
                    color = PsaCyanPrimary.copy(alpha = 0.14f),
                    topLeft = Offset(cx - slotW * 0.44f, topPad - 2.dp.toPx()),
                    size = Size(slotW * 0.88f, chartH + 4.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }

            val okH = (bucket.messagesSuccess.toFloat() / maxDayTotal) * chartH
            val failH = (bucket.messagesFailed.toFloat() / maxDayTotal) * chartH
            if (okH > 0f) {
                drawRoundRect(
                    color = EmeraldSuccess,
                    topLeft = Offset(cx - barW / 2f, baseY - okH),
                    size = Size(barW, okH),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
            if (failH > 0f) {
                drawRoundRect(
                    color = RoseError,
                    topLeft = Offset(cx - barW / 2f, baseY - okH - failH),
                    size = Size(barW, failH),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }

            val callY = baseY - ((bucket.callsCount.toFloat() / maxDayTotal) * chartH)
            callLinePoints.add(Offset(cx, callY))

            drawContext.canvas.nativeCanvas.drawText(
                bucket.label.ifBlank { bucket.dayKey.takeLast(5) },
                cx,
                size.height - 6.dp.toPx(),
                labelPaint
            )
        }

        // Overlay smooth polyline + area glow for calls per day
        if (callLinePoints.size >= 2) {
            val linePath = Path().apply {
                moveTo(callLinePoints.first().x, callLinePoints.first().y)
                callLinePoints.drop(1).forEach { pt ->
                    lineTo(pt.x, pt.y)
                }
            }
            drawPath(
                path = linePath,
                color = PsaCyanPrimary,
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        callLinePoints.forEach { pt ->
            drawCircle(color = SlateBackground, radius = 4.dp.toPx(), center = pt)
            drawCircle(color = PsaCyanGlow, radius = 2.6.dp.toPx(), center = pt)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SellerTooltipCoachingCard(
    stats: SellerPerformanceStats,
    onOpenCoaching: (() -> Unit)?
) {
    val highFailure = stats.messagesTotal >= 4 && stats.messagingFailureRate > 25f
    val needsCalls = stats.callsAttempted == 0 && stats.totalContacts > 0
    val coachingTip = when {
        highFailure -> "Revisar horarios de envío, calidad del guion y pausas anti-bloqueo (fallos en ${"%.0f".format(stats.messagingFailureRate)}%)."
        needsCalls -> "Tiene ${stats.totalContacts} contacto(s) sin seguimiento telefónico: reforzar llamadas de cierre."
        stats.callsAttempted > 0 && stats.callConnectionRate < 35f -> "Baja tasa de atención en llamadas (${"%.0f".format(stats.callConnectionRate)}%). Probar horarios alternativos y WhatsApp previo."
        else -> "Métricas equilibradas. Mantener cadencia diaria de demostraciones y seguimiento."
    }

    Surface(
        color = SlateCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_seller_tooltip_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stats.sellerName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (stats.sellerEmail.isNotBlank()) {
                        Text(
                            text = "${stats.sellerEmail} • Nivel ${stats.hierarchyDepth}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Surface(
                    color = if (highFailure) RoseError.copy(alpha = 0.16f) else EmeraldSuccess.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${"%.0f".format(stats.messagingSuccessRate)}% éxito",
                        color = if (highFailure) RoseError else EmeraldSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("✅ ${stats.messagesSuccess} ok", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("❌ ${stats.messagesFailed} fallos", color = RoseError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("📞 ${stats.callsConnected}/${stats.callsAttempted} llamadas", color = PsaCyanGlow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("🏠 ${stats.completedDemos} demos • ${stats.closedSales} cierres", color = TextSecondary, fontSize = 12.sp)
            }

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmberDelay.copy(alpha = 0.12f))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TipsAndUpdates,
                    contentDescription = null,
                    tint = AmberDelay,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = coachingTip,
                    color = TextPrimary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }

            if (onOpenCoaching != null) {
                TextButton(
                    onClick = onOpenCoaching,
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "Abrir Coaching Completo →",
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SellerRecentActivityRow(
    stats: SellerPerformanceStats,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val highFailure = stats.messagesTotal >= 4 && stats.messagingFailureRate > 25f
    val borderColor = when {
        isSelected -> PsaCyanPrimary
        highFailure -> RoseError.copy(alpha = 0.45f)
        else -> SlateCardBorder
    }
    Surface(
        color = SlateBackground.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (highFailure) RoseError.copy(alpha = 0.2f) else IndigoPrimary.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stats.sellerName.trim().firstOrNull()?.uppercase() ?: "V",
                    color = if (highFailure) RoseError else PsaCyanGlow,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stats.sellerName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatRelativeActivityTime(stats.lastActivityAtMs.coerceAtLeast(stats.updatedAtMs)),
                        color = TextMuted,
                        fontSize = 10.5.sp
                    )
                }

                Text(
                    text = stats.lastActivityLabel.ifBlank {
                        "Msgs: ${stats.messagesSuccess} ok / ${stats.messagesFailed} fallos • Llamadas: ${stats.callsAttempted}"
                    },
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (highFailure) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (highFailure) RoseError else EmeraldSuccess,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${stats.messagesSuccess}/${stats.messagesTotal} msgs (${"%.0f".format(stats.messagingSuccessRate)}%)",
                            color = if (highFailure) RoseError else EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = PsaCyanPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${stats.callsConnected}/${stats.callsAttempted} llamadas",
                            color = PsaCyanGlow,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(
    color: Color,
    label: String
) {
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
private fun AnalyticsEmptyChartNotice(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SlateCard.copy(alpha = 0.65f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.TrendingUp,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = message,
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

fun formatRelativeActivityTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return "Sin registro"
    val diff = System.currentTimeMillis() - timestampMs
    return when {
        diff < 60_000L -> "Ahora"
        diff < 3_600_000L -> "Hace ${diff / 60_000L}m"
        diff < 86_400_000L -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestampMs))
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestampMs))
    }
}
