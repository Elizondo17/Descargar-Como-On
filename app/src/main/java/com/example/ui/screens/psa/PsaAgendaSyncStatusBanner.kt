package com.example.ui.screens.psa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgendaSyncStatus
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PsaAgendaSyncStatusBanner(
    isNetworkOnline: Boolean,
    syncStatus: AgendaSyncStatus,
    onManualSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when {
        syncStatus.isSyncing -> PsaCyanGlow
        !isNetworkOnline && syncStatus.pendingCount > 0 -> AmberDelay
        !isNetworkOnline -> TextSecondary
        syncStatus.pendingCount > 0 -> AmberDelay
        else -> EmeraldSuccess
    }

    val borderColor = when {
        syncStatus.isSyncing -> PsaCyanPrimary.copy(alpha = 0.6f)
        !isNetworkOnline && syncStatus.pendingCount > 0 -> AmberDelay.copy(alpha = 0.6f)
        !isNetworkOnline -> SlateCardBorder
        syncStatus.pendingCount > 0 -> AmberDelay.copy(alpha = 0.65f)
        else -> EmeraldSuccess.copy(alpha = 0.45f)
    }

    val titleText = when {
        syncStatus.isSyncing -> "Sincronizando agenda con la nube (${syncStatus.pendingCount})..."
        !isNetworkOnline && syncStatus.pendingCount > 0 ->
            "Sin conexión • ${syncStatus.pendingCount} cambio(s) guardados en Room"
        !isNetworkOnline -> "Sin conexión • Agenda disponible localmente en Room"
        syncStatus.pendingCount > 0 ->
            "${syncStatus.pendingCount} cambio(s) locales pendientes de subir"
        else -> "Agenda sincronizada (Room + Firestore)"
    }

    val subtitleText = when {
        syncStatus.isSyncing -> "Subiendo visitas, posventa y contactos pendientes..."
        !isNetworkOnline && syncStatus.pendingCount > 0 ->
            "Se sincronizarán automáticamente al recuperar internet."
        !isNetworkOnline -> "Puedes seguir agendando y editando visitas sin conexión."
        syncStatus.pendingCount > 0 ->
            syncStatus.lastErrorMessage?.takeIf { it.isNotBlank() }
                ?: "Toca para reintentar sincronización ahora."
        else -> "Respaldo local inmediato y sincronización automática en la nube."
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .let { base ->
                if (isNetworkOnline && !syncStatus.isSyncing) {
                    base.clickable(onClick = onManualSyncClick)
                } else {
                    base
                }
            }
            .testTag("psa_agenda_sync_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (syncStatus.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = PsaCyanGlow,
                        strokeWidth = 2.dp
                    )
                } else {
                    val icon = when {
                        !isNetworkOnline -> Icons.Default.CloudOff
                        syncStatus.pendingCount > 0 -> Icons.Default.CloudUpload
                        else -> Icons.Default.CloudDone
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = titleText,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = titleText,
                        color = if (syncStatus.pendingCount > 0 || syncStatus.isSyncing) accentColor else TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitleText,
                        color = TextSecondary,
                        fontSize = 10.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isNetworkOnline && !syncStatus.isSyncing) {
                IconButton(
                    onClick = onManualSyncClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("psa_agenda_sync_retry_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sincronizar agenda ahora",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
