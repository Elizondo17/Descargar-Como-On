package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DashboardAnalyticsChartsSection
import com.example.ui.components.DeliveryLogItem
import com.example.ui.components.FirstTimeSetupGuide
import com.example.ui.components.StatCard
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToAutomation: () -> Unit,
    onNavigateToHierarchy: (inspectEmail: String?) -> Unit = {},
    canAccessAutomation: Boolean = true,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val deliveryStats by viewModel.deliveryStats.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val visibleSubordinateStats by viewModel.visibleSubordinateStats.collectAsState()
    val visibleHierarchyUsers by viewModel.visibleHierarchyUsers.collectAsState()
    val local7DayBuckets by viewModel.local7DayBuckets.collectAsState()
    val localHourlyEffectivenessBuckets by viewModel.localHourlyEffectivenessBuckets.collectAsState()
    val pendingRequestsCount by viewModel.pendingHierarchyRequestsCount.collectAsState()
    val hasSeenSetupGuide by viewModel.hasSeenSetupGuide.collectAsState()
    val isLargeFont = com.example.ui.theme.LocalIsLargeFontMode.current
    val isSuperuser = currentUser?.role == "SUPERUSER"
    val canViewHierarchy = isSuperuser || (currentUser?.canManageSubUsers == true) || visibleHierarchyUsers.isNotEmpty()

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(allLogs, selectedFilter) {
        when (selectedFilter) {
            "SUCCESS" -> allLogs.filter { it.status == "SUCCESS" }
            "FAILED" -> allLogs.filter { it.status == "FAILED" }
            else -> allLogs
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User profile & license banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateCard)
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                androidx.compose.foundation.layout.FlowRow(
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentUser?.role == "SUPERUSER") IndigoPrimary.copy(alpha = 0.2f)
                                    else EmeraldSuccess.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentUser?.role == "SUPERUSER") Icons.Default.Shield else Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (currentUser?.role == "SUPERUSER") IndigoPrimary else EmeraldSuccess,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = currentUser?.username ?: "Invitado",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (currentUser?.role == "SUPERUSER") "Administrador" else "Operador",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                if (currentUser?.role != "SUPERUSER") {
                                    Text(
                                        text = "• ${currentUser?.remainingDays}d",
                                        color = if ((currentUser?.remainingDays ?: 0) <= 2) RoseError else EmeraldSuccess,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(IndigoPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (currentUser?.role == "SUPERUSER") "ADMIN" else "ACTIVO",
                            color = IndigoPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Guía de configuración inicial (visible solo la primera vez hasta ocultarse)
        if (!hasSeenSetupGuide) {
            item {
                FirstTimeSetupGuide(
                    viewModel = viewModel,
                    onDismissGuide = { viewModel.setSetupGuideDismissed(true) }
                )
            }
        }

        // Gestión de Equipo (Superusuario o Líderes con sub-usuarios)
        if (canViewHierarchy) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateCard)
                        .border(
                            1.dp,
                            if (pendingRequestsCount > 0) AmberDelay.copy(alpha = 0.65f) else PsaCyanPrimary.copy(alpha = 0.45f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PsaCyanPrimary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountTree,
                                    contentDescription = "Estructura jerárquica",
                                    tint = PsaCyanGlow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isSuperuser) "Gestión de Equipo y Aprobaciones" else "Mi Equipo y Analíticas",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                )
                                Text(
                                    text = if (isSuperuser) {
                                        "Administra tu equipo de vendedores, métricas diarias y altas."
                                    } else {
                                        "${visibleHierarchyUsers.size} vendedor(es) a cargo para seguir día a día."
                                    },
                                    color = TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        if (pendingRequestsCount > 0) {
                            Surface(
                                color = AmberDelay.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "$pendingRequestsCount pend.",
                                    color = AmberDelay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onNavigateToHierarchy(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("open_hierarchy_management_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSuperuser) "Abrir Gestión de Equipo" else "Ver Equipo y Analíticas Diarias",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }

        // Statistical Performance Cards
        item {
            Text(
                text = "Rendimiento",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isLargeFont) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Tasa de Éxito",
                        value = "${"%.1f".format(deliveryStats.successRate)}%",
                        subtitle = "${deliveryStats.successCount} entregados",
                        icon = Icons.Default.CheckCircle,
                        accentColor = EmeraldSuccess,
                        modifier = Modifier.fillMaxWidth()
                    )
                    StatCard(
                        title = "Tasa de Fallos",
                        value = "${"%.1f".format(deliveryStats.failureRate)}%",
                        subtitle = "${deliveryStats.failedCount} con error",
                        icon = Icons.Default.Error,
                        accentColor = RoseError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    StatCard(
                        title = "Total Procesados",
                        value = "${deliveryStats.totalCount}",
                        subtitle = "Chats gestionados",
                        icon = Icons.Default.Send,
                        accentColor = IndigoPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    StatCard(
                        title = "Vigencia",
                        value = if (currentUser?.role == "SUPERUSER") "Permanente" else "${currentUser?.remainingDays}d",
                        subtitle = if (currentUser?.role == "SUPERUSER") "Acceso ilimitado" else "Días restantes",
                        icon = Icons.Default.LockClock,
                        accentColor = if (currentUser?.role == "SUPERUSER") IndigoPrimary else EmeraldSuccess,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Tasa de Éxito",
                        value = "${"%.1f".format(deliveryStats.successRate)}%",
                        subtitle = "${deliveryStats.successCount} entregados",
                        icon = Icons.Default.CheckCircle,
                        accentColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Tasa de Fallos",
                        value = "${"%.1f".format(deliveryStats.failureRate)}%",
                        subtitle = "${deliveryStats.failedCount} con error",
                        icon = Icons.Default.Error,
                        accentColor = RoseError,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Procesados",
                        value = "${deliveryStats.totalCount}",
                        subtitle = "Chats gestionados",
                        icon = Icons.Default.Send,
                        accentColor = IndigoPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Vigencia",
                        value = if (currentUser?.role == "SUPERUSER") "Permanente" else "${currentUser?.remainingDays}d",
                        subtitle = if (currentUser?.role == "SUPERUSER") "Acceso ilimitado" else "Días restantes",
                        icon = Icons.Default.LockClock,
                        accentColor = if (currentUser?.role == "SUPERUSER") IndigoPrimary else EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Visual Proportion Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateCard)
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Balance de Entregas vs. Fallos",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${deliveryStats.totalCount} totales",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(SlateCardBorder)
                ) {
                    if (deliveryStats.totalCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(deliveryStats.successRate.coerceAtLeast(0.01f))
                                .height(10.dp)
                                .background(EmeraldSuccess)
                        )
                        Box(
                            modifier = Modifier
                                .weight(deliveryStats.failureRate.coerceAtLeast(0.01f))
                                .height(10.dp)
                                .background(RoseError)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(SlateCardBorder)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Exitosos (${deliveryStats.successCount})", color = TextSecondary, fontSize = 11.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RoseError)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Fallos (${deliveryStats.failedCount})", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // Interactive Native Compose Analytics Charts (Messaging success/failure, calls & seller activity)
        item {
            DashboardAnalyticsChartsSection(
                teamStats = visibleSubordinateStats,
                localBuckets = local7DayBuckets,
                deliveryStats = deliveryStats,
                currentUserEmail = currentUser?.normalizedEmail.orEmpty(),
                canViewSubordinates = canViewHierarchy,
                onRefreshBoard = { viewModel.refreshSellerPerformanceBoard() },
                onInspectSeller = if (canViewHierarchy) { sellerStats ->
                    onNavigateToHierarchy(sellerStats.sellerEmail)
                } else null,
                hourlyBuckets = localHourlyEffectivenessBuckets
            )
        }

        // Autonomous AI Sales Memory Section
        item {
            AiSalesMemorySection(viewModel = viewModel)
        }

        // Action CTA: Go to Automation (only when user has automation module enabled)
        if (canAccessAutomation) {
            item {
                Button(
                    onClick = onNavigateToAutomation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("go_to_automation_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Iniciar Automatización",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Recent Logs Section
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Historial de Mensajes",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (allLogs.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Limpiar historial",
                            tint = TextMuted
                        )
                    }
                }
            }
        }

        // Filters chips (responsive FlowRow for 140%/160%)
        item {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("Todos (${allLogs.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = selectedFilter == "SUCCESS",
                    onClick = { selectedFilter = "SUCCESS" },
                    label = { Text("Exitosos (${deliveryStats.successCount})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldSuccess,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = selectedFilter == "FAILED",
                    onClick = { selectedFilter = "FAILED" },
                    label = { Text("Fallidos (${deliveryStats.failedCount})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RoseError,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay registros con el filtro seleccionado.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(filteredLogs) { log ->
                DeliveryLogItem(log = log)
            }
        }
    }
}
