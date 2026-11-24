package com.example.ui.screens.psa

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgendaTimeWindowFilter
import com.example.data.model.AgendaVisitStatusFilter
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaVisitStatus
import com.example.data.model.ScheduleIntervalConfig
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
import com.example.util.DemoWaterSource
import com.example.util.PsaScheduleManager
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaAgendaScheduleIntervalCard(
    intervalConfig: ScheduleIntervalConfig,
    selectedTimeWindow: AgendaTimeWindowFilter,
    onSelectTimeWindow: (AgendaTimeWindowFilter) -> Unit,
    onOpenIntervalConfigurator: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("psa_agenda_interval_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Intervalo horario del vendedor",
                        tint = PsaCyanGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Jornada: ${intervalConfig.workStartTime} – ${intervalConfig.workEndTime}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = "Demo ${intervalConfig.demoDurationMinutes}m • Margen viaje/demora ${intervalConfig.travelBufferMinutes}m",
                            color = PsaCyanGlow,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenIntervalConfigurator,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.55f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("psa_btn_adjust_schedule_interval")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = PsaCyanGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ajustar Horario",
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Franja horaria visible:",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AgendaTimeWindowFilter.entries) { window ->
                        val selected = window == selectedTimeWindow
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectTimeWindow(window) },
                            label = {
                                Text(
                                    text = window.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PsaCyanPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = SlateBackground,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = SlateCardBorder,
                                selectedBorderColor = PsaCyanGlow
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("psa_time_window_${window.name.lowercase()}")
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaAgendaStatusFilterBar(
    selectedStatusFilter: AgendaVisitStatusFilter,
    statusCounts: Map<AgendaVisitStatusFilter, Int>,
    onSelectStatusFilter: (AgendaVisitStatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Filtrar visitas por estado:",
            color = TextSecondary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AgendaVisitStatusFilter.entries) { filter ->
                val selected = filter == selectedStatusFilter
                val count = statusCounts[filter] ?: 0
                val accent = when (filter) {
                    AgendaVisitStatusFilter.ALL -> PsaCyanPrimary
                    AgendaVisitStatusFilter.PENDING -> AmberDelay
                    AgendaVisitStatusFilter.COMPLETED -> EmeraldSuccess
                    AgendaVisitStatusFilter.CANCELLED -> RoseError
                }
                val selectedTextColor = if (filter == AgendaVisitStatusFilter.CANCELLED) Color.White else Color.Black

                FilterChip(
                    selected = selected,
                    onClick = { onSelectStatusFilter(filter) },
                    label = {
                        Text(
                            text = "${filter.label} ($count)",
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent,
                        selectedLabelColor = selectedTextColor,
                        containerColor = SlateCard,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = SlateCardBorder,
                        selectedBorderColor = accent
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("psa_status_filter_${filter.name.lowercase()}")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaAgendaVisitStatusCard(
    context: Context,
    visit: PsaHomeVisit,
    durationMinutes: Int,
    travelBufferMinutes: Int,
    onStartDemo: (PsaHomeVisit) -> Unit,
    onUpdateStatus: (PsaHomeVisit, PsaVisitStatus) -> Unit,
    onRegisterPostSale: (PsaHomeVisit) -> Unit
) {
    val endTime = PsaScheduleManager.calculateEndTime(visit.scheduledTime, durationMinutes)
    val bufferEndTime = PsaScheduleManager.calculateEndTime(endTime, travelBufferMinutes)

    val (statusLabel, statusColor) = when (visit.status) {
        PsaVisitStatus.AGENDADA -> "Agendada" to AmberDelay
        PsaVisitStatus.CONFIRMADA -> "Confirmada" to PsaCyanGlow
        PsaVisitStatus.EN_DEMOSTRACION -> "En Demo" to IndigoPrimary
        PsaVisitStatus.REPROGRAMAR -> "Reprogramar" to AmberDelay
        PsaVisitStatus.CERRADA_EXITO -> "Realizada" to EmeraldSuccess
        PsaVisitStatus.CANCELADA -> "Cancelada" to RoseError
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.48f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("psa_filtered_visit_card_${visit.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column {
                    Text(
                        text = "⏰ ${visit.scheduledTime} - $endTime (${durationMinutes}m)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                    if (visit.status != PsaVisitStatus.CANCELADA && travelBufferMinutes > 0) {
                        Text(
                            text = "🚗 Margen viaje/demora: $endTime - $bufferEndTime (+${travelBufferMinutes}m)",
                            color = AmberDelay,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.16f))
                        .border(1.dp, statusColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "👤 ${visit.customerName}" + if (visit.phone.isNotBlank()) " (${visit.phone})" else "",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                if (visit.address.isNotBlank() || visit.neighborhood.isNotBlank()) {
                    Text(
                        text = "📍 ${listOf(visit.address, visit.neighborhood).filter { it.isNotBlank() }.joinToString(" - ")}",
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }
                Text(
                    text = "💧 ${visit.modelInterest} • 📅 ${visit.scheduledDate}",
                    color = PsaCyanGlow,
                    fontSize = 11.5.sp
                )
                if (visit.notes.isNotBlank()) {
                    Text(
                        text = "📝 ${visit.notes}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                if (visit.diagnosticTimestampMs > 0L) {
                    val isWell = visit.waterSource == DemoWaterSource.AGUA_DE_POZO.name
                    val metricLine = if (isWell) {
                        "Pozo • Dureza ${visit.hardnessPpm} ppm • ${String.format(Locale.US, "%.1f", visit.realLitersPerDay)} L/día"
                    } else {
                        "Corriente • Cloro ${String.format(Locale.US, "%.1f", visit.chlorinePpm)} ppm • ${String.format(Locale.US, "%.1f", visit.realLitersPerDay)} L/día"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "🔬 $metricLine", color = PsaCyanGlow, fontSize = 11.sp)
                        if (visit.testPhotoPath.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Foto de test",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(13.dp)
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
                if (visit.status != PsaVisitStatus.CANCELADA) {
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
                }

                if (visit.status != PsaVisitStatus.CERRADA_EXITO) {
                    OutlinedButton(
                        onClick = { onUpdateStatus(visit, PsaVisitStatus.CERRADA_EXITO) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("psa_btn_mark_completed_${visit.id}")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Realizada", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (visit.status == PsaVisitStatus.CANCELADA) {
                    OutlinedButton(
                        onClick = { onUpdateStatus(visit, PsaVisitStatus.AGENDADA) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("psa_btn_reactivate_${visit.id}")
                    ) {
                        Text("Reactivar", color = PsaCyanGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onUpdateStatus(visit, PsaVisitStatus.CANCELADA) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, RoseError.copy(alpha = 0.55f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("psa_btn_mark_cancelled_${visit.id}")
                    ) {
                        Text("Cancelar", color = RoseError, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (visit.status == PsaVisitStatus.CERRADA_EXITO) {
                    OutlinedButton(
                        onClick = { onRegisterPostSale(visit) },
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

                if (visit.phone.isNotBlank() && visit.status != PsaVisitStatus.CANCELADA) {
                    OutlinedButton(
                        onClick = {
                            val reminderText = "Hola ${visit.customerName}, ¡buen día! Te recuerdo la visita PSA en ${visit.address} a las ${visit.scheduledTime}hs."
                            val cleanPhone = visit.phone.replace(Regex("[^0-9]"), "")
                            val uri = if (cleanPhone.length >= 8) {
                                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=" + Uri.encode(reminderText))
                            } else {
                                Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(reminderText))
                            }
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.55f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("WhatsApp", color = AmberDelay, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PsaAgendaStatusEmptyCard(
    statusFilter: AgendaVisitStatusFilter,
    onShowAll: () -> Unit,
    onCreateVisit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, SlateCardBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("psa_agenda_status_empty")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = "No hay visitas en \"${statusFilter.label}\" para esta fecha.",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onShowAll,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Ver Todas", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = onCreateVisit,
                    colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nueva Visita", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaScheduleIntervalDialog(
    initialConfig: ScheduleIntervalConfig,
    onSave: (ScheduleIntervalConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var workStart by remember { mutableStateOf(initialConfig.workStartTime) }
    var workEnd by remember { mutableStateOf(initialConfig.workEndTime) }
    var demoDuration by remember { mutableIntStateOf(initialConfig.demoDurationMinutes) }
    var travelBuffer by remember { mutableIntStateOf(initialConfig.travelBufferMinutes) }

    val previewConfig = remember(workStart, workEnd, demoDuration, travelBuffer) {
        ScheduleIntervalConfig(
            workStartTime = workStart,
            workEndTime = workEnd,
            demoDurationMinutes = demoDuration,
            travelBufferMinutes = travelBuffer
        )
    }
    val previewSlots = remember(previewConfig) {
        PsaScheduleManager.generateDailySlotsFromInterval(previewConfig)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        shape = RoundedCornerShape(18.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Jornada y Bloques de Visita",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Define desde qué hora hasta qué hora trabajas, cuánto dura cada demo y el margen entre visitas por viaje o retraso.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                // Presets rápidos de jornada
                Text(
                    text = "Atajos de jornada:",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        Triple("Mañana (08:30–13:30)", "08:30", "13:30"),
                        Triple("Tarde (14:00–20:30)", "14:00", "20:30"),
                        Triple("Completa (09:00–20:30)", "09:00", "20:30")
                    )
                    presets.forEach { (label, pStart, pEnd) ->
                        val active = workStart == pStart && workEnd == pEnd
                        FilterChip(
                            selected = active,
                            onClick = {
                                workStart = pStart
                                workEnd = pEnd
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PsaCyanPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = SlateBackground,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                // Selector Desde / Hasta con +/- 30 min
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeStepperBox(
                        label = "Desde",
                        time = workStart,
                        onDecrease = {
                            val next = PsaScheduleManager.shiftTimeClamped(workStart, -30, "09:00")
                            workStart = next
                        },
                        onIncrease = {
                            val next = PsaScheduleManager.shiftTimeClamped(workStart, +30, "09:00")
                            val nextMin = PsaScheduleManager.parseTimeMinutes(next) ?: 0
                            val endMin = PsaScheduleManager.parseTimeMinutes(workEnd) ?: (20 * 60)
                            if (nextMin + demoDuration <= endMin) {
                                workStart = next
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TimeStepperBox(
                        label = "Hasta",
                        time = workEnd,
                        onDecrease = {
                            val next = PsaScheduleManager.shiftTimeClamped(workEnd, -30, "20:30")
                            val startMin = PsaScheduleManager.parseTimeMinutes(workStart) ?: 0
                            val nextMin = PsaScheduleManager.parseTimeMinutes(next) ?: (20 * 60)
                            if (nextMin >= startMin + demoDuration) {
                                workEnd = next
                            }
                        },
                        onIncrease = {
                            val next = PsaScheduleManager.shiftTimeClamped(workEnd, +30, "20:30")
                            workEnd = next
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Duración de cada demo
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Duración estimada por demostración:",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScheduleIntervalConfig.DURATION_OPTIONS.forEach { mins ->
                            val selected = demoDuration == mins
                            FilterChip(
                                selected = selected,
                                onClick = { demoDuration = mins },
                                label = {
                                    Text(
                                        text = if (mins >= 60 && mins % 60 == 0) "${mins / 60}h (${mins}m)" else "${mins}m",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PsaCyanPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = SlateBackground,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("psa_interval_duration_$mins")
                            )
                        }
                    }
                }

                // Margen entre visitas (traslado / demora ~1h)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = AmberDelay, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Margen de viaje / demora entre visitas:",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScheduleIntervalConfig.BUFFER_OPTIONS.forEach { mins ->
                            val selected = travelBuffer == mins
                            val label = when (mins) {
                                0 -> "Sin margen"
                                60 -> "60m (~1h)"
                                else -> "${mins}m"
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { travelBuffer = mins },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberDelay,
                                    selectedLabelColor = Color.Black,
                                    containerColor = SlateBackground,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("psa_interval_buffer_$mins")
                            )
                        }
                    }
                }

                // Vista previa en vivo de turnos generados
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateBackground)
                        .border(1.dp, PsaCyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Vista previa (${previewSlots.size} bloques de visita):",
                            color = PsaCyanGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            previewSlots.forEach { (s, e) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SlateCard)
                                        .border(1.dp, SlateCardBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$s–$e",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        if (travelBuffer > 0) {
                            Text(
                                text = "Incluye ~${travelBuffer} min libres entre cada turno para viaje o imprevistos.",
                                color = AmberDelay,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(previewConfig) },
                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("psa_btn_save_interval_config")
            ) {
                Text("Aplicar Horarios", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun TimeStepperBox(
    label: String,
    time: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SlateBackground)
            .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(text = time, color = PsaCyanGlow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = onDecrease,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("-30m", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onIncrease,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("+30m", fontSize = 11.sp, color = PsaCyanGlow, fontWeight = FontWeight.Bold)
            }
        }
    }
}
