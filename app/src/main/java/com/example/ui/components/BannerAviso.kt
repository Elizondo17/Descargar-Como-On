package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MensajeGlobal
import com.example.data.model.MensajeGlobalPrioridad
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.LocalIsLargeFontMode
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Global announcement banner shown at the top of the main UI.
 * Listens to Firestore's 'mensajes_globales' collection and allows sellers
 * to temporarily dismiss announcements for the current session.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BannerAviso(
    mensajes: List<MensajeGlobal>,
    dismissedIds: Set<String>,
    onDismissTemporarily: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleMensajes = remember(mensajes, dismissedIds) {
        mensajes.filter { it.activo && it.id.isNotBlank() && it.id !in dismissedIds }
    }
    val activeMensaje = visibleMensajes.firstOrNull()
    val isLargeFont = LocalIsLargeFontMode.current

    AnimatedVisibility(
        visible = activeMensaje != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        if (activeMensaje != null) {
            var expandedBody by remember(activeMensaje.id) { mutableStateOf(false) }
            val prioridad = activeMensaje.parsedPrioridad
            val accentColor = when (prioridad) {
                MensajeGlobalPrioridad.URGENTE -> RoseError
                MensajeGlobalPrioridad.IMPORTANTE -> AmberDelay
                MensajeGlobalPrioridad.INFO -> PsaCyanGlow
            }
            val leadIcon = when (prioridad) {
                MensajeGlobalPrioridad.URGENTE -> Icons.Default.WarningAmber
                MensajeGlobalPrioridad.IMPORTANTE -> Icons.Default.Campaign
                MensajeGlobalPrioridad.INFO -> Icons.Default.Info
            }
            val badgeText = when (prioridad) {
                MensajeGlobalPrioridad.URGENTE -> "ANUNCIO URGENTE"
                MensajeGlobalPrioridad.IMPORTANTE -> "AVISO IMPORTANTE"
                MensajeGlobalPrioridad.INFO -> "COMUNICADO OFICIAL"
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SlateCard,
                    border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.65f)),
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("banner_aviso_container")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.16f),
                                        accentColor.copy(alpha = 0.06f)
                                    )
                                )
                            )
                            .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = leadIcon,
                                        contentDescription = badgeText,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = accentColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.4.sp
                                        )
                                        if (activeMensaje.autorNombre.isNotBlank()) {
                                            Text(
                                                text = "• ${activeMensaje.autorNombre}",
                                                color = TextSecondary,
                                                fontSize = 10.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (visibleMensajes.size > 1) {
                                            Text(
                                                text = "(1/${visibleMensajes.size})",
                                                color = accentColor,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (activeMensaje.titulo.isNotBlank()) {
                                        Text(
                                            text = activeMensaje.titulo,
                                            color = TextPrimary,
                                            fontSize = if (isLargeFont) 14.sp else 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.testTag("banner_aviso_title")
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onDismissTemporarily(activeMensaje.id) },
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .testTag("banner_aviso_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar aviso temporalmente",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (activeMensaje.mensaje.isNotBlank()) {
                            Text(
                                text = activeMensaje.mensaje,
                                color = TextSecondary,
                                fontSize = if (isLargeFont) 13.sp else 12.sp,
                                lineHeight = if (isLargeFont) 18.sp else 16.sp,
                                maxLines = if (expandedBody) 12 else if (isLargeFont) 4 else 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .testTag("banner_aviso_message")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeMensaje.mensaje.length > 110) {
                                TextButton(
                                    onClick = { expandedBody = !expandedBody },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text(
                                        text = if (expandedBody) "Ver menos" else "Leer más",
                                        color = accentColor,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            TextButton(
                                onClick = { onDismissTemporarily(activeMensaje.id) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("banner_aviso_hide_button")
                            ) {
                                Text(
                                    text = "Ocultar por ahora",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
