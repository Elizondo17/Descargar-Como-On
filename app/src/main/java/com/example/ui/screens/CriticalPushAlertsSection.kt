package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertSeverity
import com.example.data.model.CriticalCrmAlert
import com.example.data.repository.FcmBroadcastRepository
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun LazyListScope.criticalPushAlertsTab(
    context: Context,
    adminUsername: String
) {
    item {
        CriticalPushAlertsContent(context = context, adminUsername = adminUsername)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CriticalPushAlertsContent(
    context: Context,
    adminUsername: String,
    repository: FcmBroadcastRepository = FcmBroadcastRepository.getInstance()
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf(AlertSeverity.CRITICAL) }
    var requirePause by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val isLargeFont = com.example.ui.theme.LocalIsLargeFontMode.current

    val history by repository.broadcastHistory.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // ==========================================
        // 1. BANNER PRINCIPAL DE EMISIÓN FCM
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SlateCard)
                .border(1.dp, RoseError.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(16.dp)
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(RoseError.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = RoseError,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Alertas Push (FCM)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Avisos críticos en vivo",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(EmeraldSuccess.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Topic Activo",
                        color = EmeraldSuccess,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // 2. FORMULARIO DE REDACCIÓN DE ALERTA
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SlateCard)
                .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "1. Nivel de Severidad",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = severity == AlertSeverity.CRITICAL,
                        onClick = {
                            severity = AlertSeverity.CRITICAL
                            requirePause = true
                        },
                        label = { Text("🚨 Crítica", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoseError,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("chip_severity_critical")
                    )

                    FilterChip(
                        selected = severity == AlertSeverity.MAINTENANCE,
                        onClick = { severity = AlertSeverity.MAINTENANCE },
                        label = { Text("⚠️ Mant.", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberDelay,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.testTag("chip_severity_maintenance")
                    )

                    FilterChip(
                        selected = severity == AlertSeverity.ANNOUNCEMENT,
                        onClick = {
                            severity = AlertSeverity.ANNOUNCEMENT
                            requirePause = false
                        },
                        label = { Text("ℹ️ Aviso", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("chip_severity_announcement")
                    )
                }

                Text(
                    text = "2. Contenido del Aviso",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    label = { Text("Título del aviso (ej: Mantenimiento EzoCRM)") },
                    placeholder = { Text("Ej: Pausar envíos urgentes") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = when (severity) {
                            AlertSeverity.CRITICAL -> RoseError
                            AlertSeverity.MAINTENANCE -> AmberDelay
                            AlertSeverity.ANNOUNCEMENT -> IndigoPrimary
                        },
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_alert_title")
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= 250) message = it },
                    label = { Text("Cuerpo del mensaje e instrucciones") },
                    placeholder = { Text("Describe brevemente el motivo o acción requerida por los operadores...") },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = when (severity) {
                            AlertSeverity.CRITICAL -> RoseError
                            AlertSeverity.MAINTENANCE -> AmberDelay
                            AlertSeverity.ANNOUNCEMENT -> IndigoPrimary
                        },
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_alert_message")
                )

                // Switch de Pausa
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SlateBackground)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PauseCircle,
                                contentDescription = null,
                                tint = if (requirePause) RoseError else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Instruir pausa de envíos",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "Solicita a los operadores detener cualquier automatización activa",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = requirePause,
                        onCheckedChange = { requirePause = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RoseError
                        ),
                        modifier = Modifier.testTag("switch_require_pause")
                    )
                }

                // ==========================================
                // 3. VISTA PREVIA EN VIVO ESTILO ANDROID
                // ==========================================
                Text(
                    text = "3. Vista Previa en Dispositivos Receptores",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                val previewBorderColor = when (severity) {
                    AlertSeverity.CRITICAL -> RoseError
                    AlertSeverity.MAINTENANCE -> AmberDelay
                    AlertSeverity.ANNOUNCEMENT -> IndigoPrimary
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.5.dp, previewBorderColor, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔔 Como On • Ahora",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(previewBorderColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = severity.name,
                                    color = previewBorderColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = if (title.isNotBlank()) title else "(Sin título aún)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Text(
                            text = if (message.isNotBlank()) message else "(Escribe el cuerpo del mensaje arriba)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 2
                        )

                        if (requirePause) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = RoseError, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ACCIÓN: Detención de envíos solicitada",
                                    color = RoseError,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 4. BOTONES DE ACCIÓN
                // ==========================================
                val isFormValid = title.trim().length >= 3 && message.trim().length >= 5

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!isFormValid) {
                                Toast.makeText(context, "Ingresa un título (min 3) y mensaje (min 5)", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            repository.broadcastAlert(
                                context = context,
                                title = title,
                                body = message,
                                severity = severity,
                                requirePause = requirePause,
                                adminName = adminUsername
                            )
                            Toast.makeText(context, "🔔 Alerta de prueba generada en este dispositivo", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                            .heightIn(min = 46.dp)
                            .testTag("btn_test_local_alert")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Probar Local", fontSize = 11.5.sp)
                    }

                    Button(
                        onClick = {
                            if (!isFormValid) {
                                Toast.makeText(context, "Ingresa un título (min 3) y mensaje (min 5)", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            showConfirmDialog = true
                        },
                        enabled = isFormValid && !isSending,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (severity) {
                                AlertSeverity.CRITICAL -> RoseError
                                AlertSeverity.MAINTENANCE -> AmberDelay
                                AlertSeverity.ANNOUNCEMENT -> IndigoPrimary
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1.3f) }
                            .heightIn(min = 46.dp)
                            .testTag("btn_broadcast_alert")
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Transmitir FCM", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. HISTORIAL DE ALERTAS EMITIDAS
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SlateCard)
                .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Historial de Alertas Emitidas (${history.size})",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (history.isEmpty()) {
                    Text(
                        text = "Aún no has emitido alertas en esta sesión. Las alertas transmitidas quedarán registradas aquí para control y auditoría.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        history.forEach { item ->
                            val itemColor = when (item.severity) {
                                AlertSeverity.CRITICAL -> RoseError
                                AlertSeverity.MAINTENANCE -> AmberDelay
                                AlertSeverity.ANNOUNCEMENT -> IndigoPrimary
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SlateBackground)
                                    .border(1.dp, itemColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.title,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = dateFormat.format(Date(item.timestamp)),
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                    Text(
                                        text = item.body,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(itemColor.copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = item.severity.name,
                                                color = itemColor,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (item.requireAutomationPause) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Pausa requerida", color = RoseError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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

    // Diálogo de Confirmación para Transmisión Push
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = RoseError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmar Emisión Masiva", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "¿Transmitir notificación push a todos los dispositivos?",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateBackground)
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("Título: $title", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                            Text("Mensaje: $message", color = TextSecondary, fontSize = 11.sp)
                            Text("Severidad: ${severity.name}", color = RoseError, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("Topic: ${CriticalCrmAlert.TOPIC_CRITICAL_ALERTS}", color = IndigoPrimary, fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isSending = true
                        val result = repository.broadcastAlert(
                            context = context,
                            title = title,
                            body = message,
                            severity = severity,
                            requirePause = requirePause,
                            adminName = adminUsername
                        )
                        isSending = false
                        if (result.isSuccess) {
                            Toast.makeText(context, "✅ Notificación push transmitida con éxito", Toast.LENGTH_LONG).show()
                            title = ""
                            message = ""
                        } else {
                            Toast.makeText(context, "Error al emitir: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Sí, Transmitir Ahora", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = SlateCard
        )
    }
}
