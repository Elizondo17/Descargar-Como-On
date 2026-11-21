package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.EzoAccessibilityService
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FirstTimeSetupGuide(
    viewModel: MainViewModel,
    onDismissGuide: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLargeFont = com.example.ui.theme.LocalIsLargeFontMode.current

    var resumeCounter by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeCounter++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsState()

    val isAccessibilityActive = remember(isAccessibilityConnected, resumeCounter) {
        isAccessibilityConnected || EzoAccessibilityService.isAccessibilityEnabledInSettings(context)
    }

    val areNotificationsEnabled = remember(resumeCounter) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    val canDrawOverlays = remember(resumeCounter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        resumeCounter++
    }

    val totalRequiredSteps = 2
    val completedSteps = (if (isAccessibilityActive) 1 else 0) + (if (areNotificationsEnabled) 1 else 0)
    val allCompleted = completedSteps == totalRequiredSteps
    val progressFraction = completedSteps.toFloat() / totalRequiredSteps.toFloat()

    var isExpanded by rememberSaveable(allCompleted) {
        mutableStateOf(!allCompleted)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateCard)
            .border(
                1.dp,
                if (allCompleted) EmeraldSuccess.copy(alpha = 0.35f) else IndigoPrimary.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
            .testTag("first_time_setup_guide")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("setup_toggle_expand_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (allCompleted) EmeraldSuccess.copy(alpha = 0.15f)
                            else IndigoPrimary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (allCompleted) Icons.Default.CheckCircle else Icons.Default.Settings,
                        contentDescription = null,
                        tint = if (allCompleted) EmeraldSuccess else IndigoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Configuración Inicial",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (allCompleted) EmeraldSuccess.copy(alpha = 0.2f)
                                    else AmberDelay.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (allCompleted) "LISTO" else "$completedSteps/$totalRequiredSteps",
                                color = if (allCompleted) EmeraldSuccess else AmberDelay,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = if (allCompleted) "Permisos listos" else "Activa permisos básicos para comenzar",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Colapsar guía" else "Expandir guía",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = if (allCompleted) EmeraldSuccess else IndigoPrimary,
            trackColor = SlateBackground,
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PermissionStepCard(
                    stepNumber = "1",
                    title = "Accesibilidad",
                    description = "Lectura y envío asistido en CRM.",
                    icon = Icons.Default.TouchApp,
                    isGranted = isAccessibilityActive,
                    grantedLabel = "Activo",
                    pendingLabel = "Pendiente",
                    actionButtonText = if (isAccessibilityActive) "Revisar" else "Activar",
                    onActionClick = { viewModel.openAccessibilitySettings(context) },
                    actionButtonTag = "setup_open_accessibility_button",
                    isLargeFont = isLargeFont
                )

                PermissionStepCard(
                    stepNumber = "2",
                    title = "Notificaciones",
                    description = "Alertas y control rápido de estado.",
                    icon = Icons.Default.Notifications,
                    isGranted = areNotificationsEnabled,
                    grantedLabel = "Activo",
                    pendingLabel = "Pendiente",
                    actionButtonText = if (areNotificationsEnabled) "Ajustes" else "Permitir",
                    onActionClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !areNotificationsEnabled) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            openAppNotificationSettings(context)
                        }
                    },
                    actionButtonTag = "setup_request_notifications_button",
                    isLargeFont = isLargeFont
                )

                PermissionStepCard(
                    stepNumber = "3",
                    title = "Burbuja flotante",
                    description = "Acceso rápido sobre WhatsApp.",
                    icon = Icons.Default.Layers,
                    isGranted = canDrawOverlays,
                    grantedLabel = "Activo",
                    pendingLabel = "Opcional",
                    actionButtonText = if (canDrawOverlays) "Listo" else "Habilitar",
                    onActionClick = { openOverlaySettings(context) },
                    actionButtonTag = "setup_open_overlay_settings_button",
                    isLargeFont = isLargeFont
                )

                if (onDismissGuide != null) {
                    OutlinedButton(
                        onClick = onDismissGuide,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 42.dp)
                            .testTag("setup_dismiss_guide_button"),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder)
                    ) {
                        Text(
                            text = "Ocultar guía (disponible en ⚙️ Ayuda)",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PermissionStepCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    grantedLabel: String,
    pendingLabel: String,
    actionButtonText: String,
    onActionClick: () -> Unit,
    actionButtonTag: String,
    isLargeFont: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateBackground.copy(alpha = 0.6f))
            .border(
                1.dp,
                if (isGranted) EmeraldSuccess.copy(alpha = 0.3f) else SlateCardBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) EmeraldSuccess.copy(alpha = 0.2f) else IndigoPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) EmeraldSuccess else IndigoPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "$stepNumber. $title",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = description,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isGranted) EmeraldSuccess.copy(alpha = 0.15f)
                        else AmberDelay.copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (isGranted) EmeraldSuccess.copy(alpha = 0.4f)
                        else AmberDelay.copy(alpha = 0.4f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = if (isGranted) EmeraldSuccess else AmberDelay,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isGranted) grantedLabel else pendingLabel,
                        color = if (isGranted) EmeraldSuccess else AmberDelay,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (isGranted) {
            OutlinedButton(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (isLargeFont) 46.dp else 40.dp)
                    .testTag(actionButtonTag),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$actionButtonText ✓",
                    color = EmeraldSuccess,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (isLargeFont) 46.dp else 40.dp)
                    .testTag(actionButtonTag),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = actionButtonText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun openAppNotificationSettings(context: Context) {
    try {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                else -> {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", context.packageName)
                    putExtra("app_uid", context.applicationInfo.uid)
                }
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(fallback)
    }
}

private fun openOverlaySettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }
}
