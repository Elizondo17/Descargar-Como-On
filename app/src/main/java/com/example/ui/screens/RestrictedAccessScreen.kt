package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserAccount
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
import com.example.util.AccessLockReason
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RestrictedAccessScreen(
    user: UserAccount,
    lockReason: AccessLockReason = if (user.isBlocked) AccessLockReason.ADMIN_BLOCKED else AccessLockReason.DAYS_EXPIRED,
    lastOnlineVerifiedAtMs: Long = user.lastOnlineVerifiedAtMs,
    isNetworkOnline: Boolean = true,
    feedbackMessage: String? = null,
    updateLockReasonText: String = "",
    requiredVersionCode: Int = user.requiredVersionCode,
    apkDownloadUrl: String = "",
    onDownloadUpdateApk: (() -> Unit)? = null,
    onRefresh: () -> Unit,
    onContactSuperuser: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isChecking by remember { mutableStateOf(false) }
    LaunchedEffect(isChecking) {
        if (isChecking) {
            delay(2500)
            isChecking = false
        }
    }

    val effectiveReason = when {
        lockReason != AccessLockReason.NONE -> lockReason
        user.isPendingApproval -> AccessLockReason.PENDING_SUPERUSER_APPROVAL
        user.isBlocked -> AccessLockReason.ADMIN_BLOCKED
        else -> AccessLockReason.DAYS_EXPIRED
    }

    val accentColor = when (effectiveReason) {
        AccessLockReason.UPDATE_REQUIRED -> PsaCyanGlow
        AccessLockReason.PENDING_SUPERUSER_APPROVAL,
        AccessLockReason.OFFLINE_OVER_24H,
        AccessLockReason.REBOOT_VERIFICATION_REQUIRED -> AmberDelay
        else -> RoseError
    }

    val headerIcon = when (effectiveReason) {
        AccessLockReason.UPDATE_REQUIRED -> Icons.Default.SystemUpdate
        AccessLockReason.PENDING_SUPERUSER_APPROVAL -> Icons.Default.Schedule
        AccessLockReason.DAYS_EXPIRED -> Icons.Default.EventBusy
        AccessLockReason.OFFLINE_OVER_24H -> Icons.Default.WifiOff
        AccessLockReason.REBOOT_VERIFICATION_REQUIRED -> Icons.Default.Refresh
        AccessLockReason.CLOCK_TAMPER_DETECTED -> Icons.Default.Schedule
        else -> Icons.Default.Shield
    }

    val titleText = when (effectiveReason) {
        AccessLockReason.UPDATE_REQUIRED -> "ACTUALIZACIÓN REQUERIDA"
        AccessLockReason.PENDING_SUPERUSER_APPROVAL -> "CUENTA PENDIENTE DE HABILITACIÓN"
        AccessLockReason.DAYS_EXPIRED -> "SE ACABARON TUS DÍAS DE USO"
        AccessLockReason.OFFLINE_OVER_24H -> "CONEXIÓN REQUERIDA"
        AccessLockReason.REBOOT_VERIFICATION_REQUIRED -> "VERIFICACIÓN INICIAL REQUERIDA"
        AccessLockReason.CLOCK_TAMPER_DETECTED -> "VERIFICACIÓN DE FECHA REQUERIDA"
        AccessLockReason.ADMIN_BLOCKED, AccessLockReason.NONE -> "ACCESO RESTRINGIDO"
    }

    val bodyText = when (effectiveReason) {
        AccessLockReason.UPDATE_REQUIRED ->
            updateLockReasonText.takeIf { it.isNotBlank() }
                ?: "Tu acceso se encuentra pausado temporalmente hasta que descargues e instales la nueva versión de Como On. Al abrir la app actualizada (o si el Administrador la habilita), tu cuenta se reactivará automáticamente."
        AccessLockReason.PENDING_SUPERUSER_APPROVAL ->
            "Tu sub-usuario fue solicitado en la estructura del equipo, pero aún requiere la habilitación final del Superusuario. Apenas el Superusuario lo active, podrás presionar 'Validar Ahora' para ingresar."
        AccessLockReason.DAYS_EXPIRED ->
            "Se acabaron los días de uso habilitados para tu cuenta. Contacta al Administrador para habilitar más días y continuar utilizando la aplicación."
        AccessLockReason.OFFLINE_OVER_24H ->
            "Llevas más de 24 horas sin conexión a internet. Por seguridad, conéctate a Wi-Fi o datos móviles para validar tu licencia y reactivar todas las funciones."
        AccessLockReason.REBOOT_VERIFICATION_REQUIRED ->
            "El teléfono se reinició o no cuenta con una validación reciente de red. Conéctate a internet y presiona Validar Ahora para continuar."
        AccessLockReason.CLOCK_TAMPER_DETECTED ->
            "Se detectó un cambio en la fecha u hora del dispositivo. Activa la fecha y hora automática del teléfono y valida tu cuenta con conexión a internet."
        AccessLockReason.ADMIN_BLOCKED, AccessLockReason.NONE ->
            "El Administrador ha restringido el acceso para esta cuenta. Comunícate con él para revisar el estado de tu licencia."
    }

    val statusLabel = when (effectiveReason) {
        AccessLockReason.UPDATE_REQUIRED -> if (requiredVersionCode > 0) "Requiere Comp. >= $requiredVersionCode" else "Actualización pendiente"
        AccessLockReason.PENDING_SUPERUSER_APPROVAL -> "Esperando habilitación de Superusuario"
        AccessLockReason.DAYS_EXPIRED -> "Días de uso agotados"
        AccessLockReason.OFFLINE_OVER_24H -> if (isNetworkOnline) "Listo para validar con red" else "Sin conexión (+24 hs)"
        AccessLockReason.REBOOT_VERIFICATION_REQUIRED -> if (isNetworkOnline) "Validación pendiente" else "Sin conexión a internet"
        AccessLockReason.CLOCK_TAMPER_DETECTED -> "Ajuste de fecha detectado"
        AccessLockReason.ADMIN_BLOCKED, AccessLockReason.NONE -> "Cuenta bloqueada"
    }

    val lastOnlineFormatted = remember(lastOnlineVerifiedAtMs) {
        if (lastOnlineVerifiedAtMs > 0L) {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(lastOnlineVerifiedAtMs))
        } else {
            ""
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.14f),
                        SlateBackground,
                        SlateBackground
                    )
                )
            )
            .imePadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(24.dp))
                .background(SlateCard)
                .border(1.5.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = haloAlpha))
                )
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.20f))
                        .border(2.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = titleText,
                        tint = accentColor,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = titleText,
                color = accentColor,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 1.0.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = bodyText,
                color = TextSecondary,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Operador:", color = TextMuted, fontSize = 12.sp)
                        Text(user.username, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (user.email.isNotBlank()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("Email Google:", color = TextMuted, fontSize = 12.sp)
                            Text(user.email, color = PsaCyanGlow, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Dispositivos:", color = TextMuted, fontSize = 12.sp)
                        Text("${user.boundDevicesCount} / ${user.maxDevices}", color = PurpleNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (lastOnlineFormatted.isNotBlank()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("Última validación:", color = TextMuted, fontSize = 12.sp)
                            Text(lastOnlineFormatted, color = TextSecondary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Estado:", color = TextMuted, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusLabel,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (!feedbackMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmberDelay.copy(alpha = 0.14f))
                        .border(1.dp, AmberDelay.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = feedbackMessage,
                        color = AmberDelay,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (effectiveReason == AccessLockReason.UPDATE_REQUIRED) {
                if (onDownloadUpdateApk != null && apkDownloadUrl.isNotBlank()) {
                    Button(
                        onClick = onDownloadUpdateApk,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("restricted_download_update_apk_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Descargar nueva versión APK",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Descargar Nueva Versión (APK)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = {
                        isChecking = true
                        onRefresh()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("verify_cloud_status_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verificando actualización...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Ya actualicé / Verificar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ya actualicé / Verificar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onContactSuperuser,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("contact_superuser_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleNeon),
                    border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Contactar",
                        tint = PurpleNeon,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contactar al Administrador", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            } else if (effectiveReason == AccessLockReason.DAYS_EXPIRED) {
                Button(
                    onClick = onContactSuperuser,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("renew_whatsapp_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Renovar por WhatsApp",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Renovar Días por WhatsApp", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        isChecking = true
                        onRefresh()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("verify_cloud_status_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IndigoPrimary),
                    border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.7f)),
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = IndigoPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verificando tu acceso...", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Verificar estado",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verificar Estado", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                Button(
                    onClick = {
                        isChecking = true
                        onRefresh()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("verify_cloud_status_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Validando acceso...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Validar acceso",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (effectiveReason == AccessLockReason.OFFLINE_OVER_24H ||
                                effectiveReason == AccessLockReason.REBOOT_VERIFICATION_REQUIRED ||
                                effectiveReason == AccessLockReason.CLOCK_TAMPER_DETECTED
                            ) {
                                "Validar con Internet Ahora"
                            } else {
                                "Verificar Estado"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onContactSuperuser,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("contact_superuser_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleNeon),
                    border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Contactar",
                        tint = PurpleNeon,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contactar al Administrador", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("restricted_logout_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = BorderStroke(1.dp, SlateCardBorder)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cerrar Sesión", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}
