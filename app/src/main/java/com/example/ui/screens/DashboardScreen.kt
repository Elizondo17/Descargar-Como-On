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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
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
import com.example.service.EzoAccessibilityService
import com.example.ui.components.DeliveryLogItem
import com.example.ui.components.FirstTimeSetupGuide
import com.example.ui.components.StatCard
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
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
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val deliveryStats by viewModel.deliveryStats.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val context = LocalContext.current
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsState()
    val hasSeenSetupGuide by viewModel.hasSeenSetupGuide.collectAsState()
    val isLargeFont = com.example.ui.theme.LocalIsLargeFontMode.current
    val isSystemEnabled = remember(isAccessibilityConnected) {
        isAccessibilityConnected || EzoAccessibilityService.isAccessibilityEnabledInSettings(context)
    }

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



        // Modo Banco Seguro / Pausa (Limpio, sin texto repetitivo)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSystemEnabled) SlateCard else EmeraldSuccess.copy(alpha = 0.1f))
                    .border(
                        1.dp,
                        if (isSystemEnabled) SlateCardBorder else EmeraldSuccess.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isSystemEnabled) AmberDelay.copy(alpha = 0.2f) else EmeraldSuccess.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSystemEnabled) Icons.Default.Shield else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isSystemEnabled) AmberDelay else EmeraldSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modo Banco Seguro",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isSystemEnabled) "Servicio activo" else "Apagado (apto para bancos)",
                            color = if (isSystemEnabled) AmberDelay else EmeraldSuccess,
                            fontSize = 11.5.sp
                        )
                    }
                }

                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSystemEnabled) {
                        Button(
                            onClick = { viewModel.disableServiceForBanking(context) },
                            modifier = Modifier
                                .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                .heightIn(min = 44.dp)
                                .testTag("disable_banking_service_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                        ) {
                            Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pausar Servicio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.openAccessibilitySettings(context) },
                            modifier = Modifier
                                .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                                .heightIn(min = 44.dp)
                                .testTag("reactivate_service_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reactivar Servicio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.openAccessibilitySettings(context) },
                        modifier = Modifier
                            .let { if (isLargeFont) it.fillMaxWidth() else it }
                            .heightIn(min = 44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, SlateCardBorder)
                    ) {
                        Text("Ajustes", fontSize = 12.sp)
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

        // Autonomous AI Sales Memory Section
        item {
            AiSalesMemorySection(viewModel = viewModel)
        }

        // Action CTA: Go to Automation
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
