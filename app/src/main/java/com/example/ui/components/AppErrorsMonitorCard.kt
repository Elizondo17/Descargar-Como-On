package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppErrorReport
import com.example.util.AppErrorStage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AppErrorFilter {
    OPEN,
    RESOLVED,
    ALL
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppErrorsMonitorCard(
    appErrors: List<AppErrorReport>,
    isLoading: Boolean,
    pendingLocalCount: Int,
    onRefresh: () -> Unit,
    onFlushLocal: () -> Unit,
    onSendTestReport: () -> Unit,
    onToggleResolve: (String, Boolean) -> Unit,
    onDeleteError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val openCount = remember(appErrors) { appErrors.count { it.status != "RESOLVED" } }
    val resolvedCount = remember(appErrors) { appErrors.count { it.status == "RESOLVED" } }
    var isExpanded by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(AppErrorFilter.OPEN) }
    var expandedTraceErrorId by remember { mutableStateOf<String?>(null) }

    val filteredErrors = remember(appErrors, selectedFilter) {
        when (selectedFilter) {
            AppErrorFilter.OPEN -> appErrors.filter { it.status != "RESOLVED" }
            AppErrorFilter.RESOLVED -> appErrors.filter { it.status == "RESOLVED" }
            AppErrorFilter.ALL -> appErrors
        }
    }

    val headerTint = if (openCount > 0) RoseError else EmeraldSuccess
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SlateCard)
            .border(
                width = 1.dp,
                color = if (openCount > 0) RoseError.copy(alpha = 0.55f) else SlateCardBorder,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("admin_btn_toggle_app_errors"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(headerTint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Monitor de errores Firebase",
                        tint = headerTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🩺 Monitor Firebase (AppErrors)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                    Text(
                        text = if (openCount > 0) {
                            "$openCount fallo(s) abierto(s) en tiempo real"
                        } else {
                            "Sin errores críticos activos • ${appErrors.size} en historial"
                        },
                        color = if (openCount > 0) RoseError else TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(headerTint.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (openCount > 0) "$openCount OPEN" else "OK",
                        color = headerTint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Ocultar monitor de errores" else "Mostrar monitor de errores",
                    tint = TextSecondary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onRefresh,
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("admin_btn_refresh_app_errors")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Actualizar errores",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Actualizar", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        if (pendingLocalCount > 0) {
                            Button(
                                onClick = onFlushLocal,
                                colors = ButtonDefaults.buttonColors(containerColor = AmberDelay),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_btn_flush_local_errors")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Subir errores locales",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Subir Cola ($pendingLocalCount)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        OutlinedButton(
                            onClick = onSendTestReport,
                            border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("admin_btn_test_app_error")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Probar captura de error",
                                tint = PsaCyanGlow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Probar", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = PsaCyanGlow)
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == AppErrorFilter.OPEN,
                            onClick = { selectedFilter = AppErrorFilter.OPEN },
                            label = { Text("Pendientes ($openCount)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoseError.copy(alpha = 0.22f),
                                selectedLabelColor = RoseError
                            ),
                            modifier = Modifier.testTag("admin_filter_errors_open")
                        )
                        FilterChip(
                            selected = selectedFilter == AppErrorFilter.RESOLVED,
                            onClick = { selectedFilter = AppErrorFilter.RESOLVED },
                            label = { Text("Resueltos ($resolvedCount)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldSuccess.copy(alpha = 0.22f),
                                selectedLabelColor = EmeraldSuccess
                            ),
                            modifier = Modifier.testTag("admin_filter_errors_resolved")
                        )
                        FilterChip(
                            selected = selectedFilter == AppErrorFilter.ALL,
                            onClick = { selectedFilter = AppErrorFilter.ALL },
                            label = { Text("Todos (${appErrors.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleNeon.copy(alpha = 0.22f),
                                selectedLabelColor = PurpleNeon
                            ),
                            modifier = Modifier.testTag("admin_filter_errors_all")
                        )
                    }

                    if (filteredErrors.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateBackground)
                                .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Todo saludable en esta vista. Los fallos de inicialización Firebase (onCreate) y Logcat filtrado aparecerán aquí en tiempo real.",
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    } else {
                        filteredErrors.take(15).forEach { report ->
                            val isResolved = report.status == "RESOLVED"
                            val stageColor = if (report.stage == AppErrorStage.LOGCAT_FIREBASE.name) AmberDelay else RoseError
                            val isTraceOpen = expandedTraceErrorId == report.id

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SlateBackground)
                                    .border(
                                        1.dp,
                                        if (isResolved) EmeraldSuccess.copy(alpha = 0.35f) else stageColor.copy(alpha = 0.55f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(stageColor.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = report.stage,
                                                color = stageColor,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isResolved) EmeraldSuccess.copy(alpha = 0.18f) else RoseError.copy(alpha = 0.18f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = report.status,
                                                color = if (isResolved) EmeraldSuccess else RoseError,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    val timestampMs = if (report.occurredAtMs > 0L) report.occurredAtMs else report.createdAtMs
                                    if (timestampMs > 0L) {
                                        Text(
                                            text = dateFormat.format(Date(timestampMs)),
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "${report.reporterName} • ${report.reporterEmail.ifBlank { report.ownerUid.take(10) }}",
                                    color = PsaCyanGlow,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp
                                )

                                Text(
                                    text = "${report.deviceModel.ifBlank { "Android" }} • SDK ${report.androidSdk} • v${report.appVersionName} (${report.appVersionCode})",
                                    color = TextSecondary,
                                    fontSize = 10.5.sp
                                )

                                Text(
                                    text = report.errorMessage,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )

                                if (report.logcatExcerpt.isNotBlank() || report.stackTraceExcerpt.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            expandedTraceErrorId = if (isTraceOpen) null else report.id
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.heightIn(min = 36.dp)
                                    ) {
                                        Text(
                                            text = if (isTraceOpen) "Ocultar traza Logcat ▲" else "Ver traza / Logcat Firebase ▼",
                                            color = IndigoPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    AnimatedVisibility(visible = isTraceOpen) {
                                        val combinedTrace = buildString {
                                            if (report.stackTraceExcerpt.isNotBlank()) {
                                                append("STACK TRACE:\n")
                                                append(report.stackTraceExcerpt)
                                            }
                                            if (report.logcatExcerpt.isNotBlank()) {
                                                if (isNotEmpty()) append("\n\n")
                                                append("LOGCAT FIREBASE (PID):\n")
                                                append(report.logcatExcerpt)
                                            }
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black.copy(alpha = 0.7f))
                                                .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = combinedTrace,
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 14.sp
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    cb.setPrimaryClip(ClipData.newPlainText("AppError", "${report.errorMessage}\n$combinedTrace"))
                                                    Toast.makeText(context, "Detalle copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.heightIn(min = 36.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar error", modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Copiar detalle", fontSize = 10.5.sp)
                                            }
                                        }
                                    }
                                }

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onToggleResolve(report.id, !isResolved) },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isResolved) AmberDelay.copy(alpha = 0.6f) else EmeraldSuccess.copy(alpha = 0.6f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("admin_btn_resolve_error_${report.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = if (isResolved) "Reabrir error" else "Marcar resuelto",
                                            tint = if (isResolved) AmberDelay else EmeraldSuccess,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isResolved) "Reabrir" else "Marcar resuelto",
                                            color = if (isResolved) AmberDelay else EmeraldSuccess,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteError(report.id) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("admin_btn_delete_error_${report.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar registro de error",
                                            tint = RoseError,
                                            modifier = Modifier.size(18.dp)
                                        )
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
