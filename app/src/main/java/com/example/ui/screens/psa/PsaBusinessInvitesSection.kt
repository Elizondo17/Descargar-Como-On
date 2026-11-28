package com.example.ui.screens.psa

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.model.BusinessInviteEntity
import com.example.data.model.BusinessInviteStatus
import com.example.ui.components.ContactsPermissionRationaleDialog
import com.example.ui.components.PhoneContactPickerDialog
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DeviceContactsHelper
import com.example.util.PhoneContactEntry
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaBusinessInvitesSection(
    context: Context,
    invites: List<BusinessInviteEntity>,
    sellerDisplayName: String,
    isHierarchyBusy: Boolean = false,
    onSaveInvite: (BusinessInviteEntity) -> Unit,
    onDeleteInvite: (String) -> Unit,
    onStartPresentationForInvite: (BusinessInviteEntity) -> Unit,
    onSubmitTeamRecruit: (
        invite: BusinessInviteEntity,
        targetName: String,
        targetEmail: String,
        suggestedDays: Int,
        notes: String
    ) -> Unit,
    contactsLoaderOverride: (suspend () -> List<PhoneContactEntry>)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<BusinessInviteStatus?>(null) }
    var editingInvite by remember { mutableStateOf<BusinessInviteEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var recruitTargetInvite by remember { mutableStateOf<BusinessInviteEntity?>(null) }
    var deleteConfirmTarget by remember { mutableStateOf<BusinessInviteEntity?>(null) }

    val totalCount = invites.size
    val confirmedCount = invites.count {
        it.parsedStatus == BusinessInviteStatus.CONFIRMED || it.parsedStatus == BusinessInviteStatus.ATTENDED
    }
    val joinedCount = invites.count { it.parsedStatus == BusinessInviteStatus.JOINED_TEAM }

    val filteredInvites = remember(invites, searchQuery, selectedStatusFilter) {
        val q = searchQuery.trim().lowercase()
        invites.filter { item ->
            val matchesStatus = selectedStatusFilter == null || item.parsedStatus == selectedStatusFilter
            val matchesQuery = q.isBlank() ||
                item.name.lowercase().contains(q) ||
                item.phone.lowercase().contains(q) ||
                item.email.lowercase().contains(q) ||
                item.presentationDate.lowercase().contains(q)
            matchesStatus && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("psa_business_invites_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(PurpleNeon.copy(alpha = 0.16f))
                                .border(1.dp, PurpleNeon.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null,
                                tint = PurpleNeon,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Invitar a Presentación de Negocios",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            )
                            Text(
                                text = "Arma tu lista, envía invitación por WhatsApp, programa recordatorios y suma nuevos vendedores a tu equipo.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InviteKpiBox(
                            label = "Invitados",
                            value = "$totalCount",
                            accent = PsaCyanGlow,
                            modifier = Modifier.weight(1f)
                        )
                        InviteKpiBox(
                            label = "Confirm. / Asist.",
                            value = "$confirmedCount",
                            accent = AmberDelay,
                            modifier = Modifier.weight(1f)
                        )
                        InviteKpiBox(
                            label = "En Equipo",
                            value = "$joinedCount",
                            accent = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_new_business_invite")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = SlateBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nuevo Invitado", color = SlateBackground, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                onStartPresentationForInvite(
                                    BusinessInviteEntity(
                                        id = "",
                                        name = "",
                                        phone = ""
                                    )
                                )
                            },
                            border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_open_business_presentation_from_invites")
                        ) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Presentación Guiada", color = PurpleNeon, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Search & Filter
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar invitado por nombre, teléfono o fecha...", fontSize = 12.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PsaCyanGlow) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("business_invites_search"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsaCyanGlow,
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("Todos (${invites.size})", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PsaCyanPrimary.copy(alpha = 0.22f),
                            selectedLabelColor = PsaCyanGlow
                        )
                    )
                    BusinessInviteStatus.entries.forEach { st ->
                        val count = invites.count { it.parsedStatus == st }
                        FilterChip(
                            selected = selectedStatusFilter == st,
                            onClick = {
                                selectedStatusFilter = if (selectedStatusFilter == st) null else st
                            },
                            label = { Text("${st.label} ($count)", fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = statusAccentColor(st).copy(alpha = 0.22f),
                                selectedLabelColor = statusAccentColor(st)
                            )
                        )
                    }
                }
            }
        }

        if (filteredInvites.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, SlateCardBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = PsaCyanGlow,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = if (invites.isEmpty()) {
                                "Todavía no agregaste invitados a la Presentación de Negocios"
                            } else {
                                "Sin invitados que coincidan con el filtro"
                            },
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Toca \"Nuevo Invitado\" para buscar contactos guardados en tu celular, agendar la charla con recordatorio y enviar la invitación por WhatsApp.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(filteredInvites, key = { it.id }) { invite ->
                BusinessInviteCard(
                    invite = invite,
                    onEdit = { editingInvite = invite },
                    onDelete = { deleteConfirmTarget = invite },
                    onChangeStatus = { newStatus ->
                        onSaveInvite(invite.copy(status = newStatus.name))
                    },
                    onSendWhatsApp = {
                        val opened = openWhatsAppBusinessInvite(context, invite, sellerDisplayName)
                        if (opened) {
                            onSaveInvite(
                                invite.copy(
                                    whatsappSentCount = invite.whatsappSentCount + 1
                                )
                            )
                        }
                    },
                    onPresent = { onStartPresentationForInvite(invite) },
                    onAddToTeam = { recruitTargetInvite = invite }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showCreateDialog || editingInvite != null) {
        BusinessInviteEditorDialog(
            context = context,
            initialInvite = editingInvite,
            onSave = { saved ->
                onSaveInvite(saved)
                showCreateDialog = false
                editingInvite = null
            },
            onDismiss = {
                showCreateDialog = false
                editingInvite = null
            },
            contactsLoaderOverride = contactsLoaderOverride
        )
    }

    recruitTargetInvite?.let { target ->
        QuickTeamRecruitDialog(
            initialName = target.name,
            initialEmail = target.email,
            initialNotes = target.notes.ifBlank { "Invitado desde Presentación de Negocios (${target.phone})" },
            isBusy = isHierarchyBusy,
            onConfirm = { name, email, days, notes ->
                onSubmitTeamRecruit(target, name, email, days, notes)
                recruitTargetInvite = null
            },
            onDismiss = { recruitTargetInvite = null }
        )
    }

    deleteConfirmTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteConfirmTarget = null },
            containerColor = SlateCard,
            title = { Text("Eliminar Invitado", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "¿Seguro que deseas eliminar a ${target.name} de tu lista de Presentación de Negocios?",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteInvite(target.id)
                        deleteConfirmTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTarget = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun InviteKpiBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SlateBackground)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(text = value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BusinessInviteCard(
    invite: BusinessInviteEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onChangeStatus: (BusinessInviteStatus) -> Unit,
    onSendWhatsApp: () -> Unit,
    onPresent: () -> Unit,
    onAddToTeam: () -> Unit
) {
    val st = invite.parsedStatus
    val accent = statusAccentColor(st)

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag("business_invite_card_${invite.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = invite.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (invite.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(13.dp))
                            Text(text = invite.phone, color = TextSecondary, fontSize = 12.5.sp)
                        }
                    }
                    if (invite.email.isNotBlank()) {
                        Text(text = invite.email, color = TextMuted, fontSize = 11.5.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = st.label,
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar invitado", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar invitado", tint = RoseError, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (invite.presentationDate.isNotBlank() || invite.presentationTime.isNotBlank() || invite.reminderEnabled) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (invite.presentationDate.isNotBlank() || invite.presentationTime.isNotBlank()) {
                        Surface(
                            color = SlateBackground,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SlateCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "${invite.presentationDate.ifBlank { "Fecha por definir" }} ${invite.presentationTime}".trim(),
                                    color = TextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (invite.reminderEnabled) {
                        Surface(
                            color = AmberDelay.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AmberDelay, modifier = Modifier.size(13.dp))
                                Text("Recordatorio activo", color = AmberDelay, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (invite.whatsappSentCount > 0) {
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "WA enviados: ${invite.whatsappSentCount}",
                                color = EmeraldSuccess,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            if (invite.notes.isNotBlank()) {
                Text(text = invite.notes, color = TextSecondary, fontSize = 12.sp)
            }

            // Quick status stepper
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    BusinessInviteStatus.INVITED,
                    BusinessInviteStatus.CONFIRMED,
                    BusinessInviteStatus.ATTENDED,
                    BusinessInviteStatus.NOT_INTERESTED
                ).forEach { target ->
                    val selected = st == target
                    FilterChip(
                        selected = selected,
                        onClick = { onChangeStatus(target) },
                        label = { Text(target.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = statusAccentColor(target).copy(alpha = 0.22f),
                            selectedLabelColor = statusAccentColor(target)
                        )
                    )
                }
            }

            // Primary Actions
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSendWhatsApp,
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.7f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_invite_whatsapp_${invite.id}")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Invitar WhatsApp", color = EmeraldSuccess, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onPresent,
                    border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.7f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_invite_present_${invite.id}")
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dar Presentación", color = PurpleNeon, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                if (st != BusinessInviteStatus.JOINED_TEAM) {
                    Button(
                        onClick = onAddToTeam,
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_invite_join_team_${invite.id}")
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pasar a mi equipo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Text("Solicitud enviada al equipo", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BusinessInviteEditorDialog(
    context: Context,
    initialInvite: BusinessInviteEntity?,
    onSave: (BusinessInviteEntity) -> Unit,
    onDismiss: () -> Unit,
    contactsLoaderOverride: (suspend () -> List<PhoneContactEntry>)? = null
) {
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayDate = remember { dateFmt.format(Date()) }
    val tomorrowDate = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.let { dateFmt.format(it.time) }
    }
    val twoDaysDate = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }.let { dateFmt.format(it.time) }
    }

    var name by remember { mutableStateOf(initialInvite?.name.orEmpty()) }
    var phone by remember { mutableStateOf(initialInvite?.phone.orEmpty()) }
    var email by remember { mutableStateOf(initialInvite?.email.orEmpty()) }
    var dateKey by remember { mutableStateOf(initialInvite?.presentationDate?.ifBlank { tomorrowDate } ?: tomorrowDate) }
    var timeText by remember { mutableStateOf(initialInvite?.presentationTime?.ifBlank { "19:00" } ?: "19:00") }
    var reminderEnabled by remember { mutableStateOf(initialInvite?.reminderEnabled ?: true) }
    var status by remember { mutableStateOf(initialInvite?.parsedStatus ?: BusinessInviteStatus.INVITED) }
    var notes by remember { mutableStateOf(initialInvite?.notes.orEmpty()) }

    var showPhonePicker by remember { mutableStateOf(false) }
    var showContactsRationale by remember { mutableStateOf(false) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showPhonePicker = true
        } else {
            showContactsRationale = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .imePadding()
            .testTag("business_invite_editor_dialog"),
        containerColor = SlateCard,
        title = {
            Text(
                text = if (initialInvite == null) "Nuevo Invitado a Negocios" else "Editar Invitado",
                color = PsaCyanGlow,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedButton(
                        onClick = {
                            if (DeviceContactsHelper.hasReadContactsPermission(context) || contactsLoaderOverride != null) {
                                showPhonePicker = true
                            } else {
                                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_import_phone_contact_invite")
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buscar e importar del celular", color = PsaCyanGlow, fontWeight = FontWeight.SemiBold)
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre completo *", fontSize = 11.5.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_input_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono / WhatsApp *", fontSize = 11.5.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_input_phone"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Google (opcional para alta rápida)", fontSize = 11.5.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_input_email"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                item {
                    Text(
                        text = "Agenda de la Presentación",
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            todayDate to "Hoy",
                            tomorrowDate to "Mañana",
                            twoDaysDate to "Pasado mañana"
                        ).forEach { (d, label) ->
                            FilterChip(
                                selected = dateKey == d,
                                onClick = { dateKey = d },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = dateKey,
                        onValueChange = { dateKey = it },
                        label = { Text("Fecha (AAAA-MM-DD)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("10:00", "15:00", "18:30", "19:30", "20:30").forEach { hr ->
                            FilterChip(
                                selected = timeText == hr,
                                onClick = { timeText = hr },
                                label = { Text(hr, fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text("Hora (HH:mm)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateBackground)
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recordatorio de Presentación", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                            Text("Notifica 15 min antes de la hora agendada", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PsaCyanGlow)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas / Perfil del invitado", fontSize = 11.5.sp) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isBlank()) {
                        Toast.makeText(context, "Ingresa el nombre del invitado.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val reminderMs = computePresentationReminderAtMs(dateKey.trim(), timeText.trim())
                    val entity = (initialInvite ?: BusinessInviteEntity(
                        id = "bi_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                        name = cleanName,
                        phone = phone.trim()
                    )).copy(
                        name = cleanName,
                        phone = phone.trim(),
                        email = email.trim().lowercase(),
                        notes = notes.trim(),
                        status = status.name,
                        presentationDate = dateKey.trim(),
                        presentationTime = timeText.trim(),
                        reminderEnabled = reminderEnabled && reminderMs > 0L,
                        reminderAtMs = if (reminderEnabled) reminderMs else 0L
                    )
                    onSave(entity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_save_business_invite")
            ) {
                Text("Guardar Invitado", color = SlateBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )

    if (showPhonePicker) {
        PhoneContactPickerDialog(
            onSelectContact = { contact ->
                name = contact.name
                phone = contact.phone
                showPhonePicker = false
            },
            onDismiss = { showPhonePicker = false },
            contactsLoaderOverride = contactsLoaderOverride
        )
    }

    if (showContactsRationale) {
        ContactsPermissionRationaleDialog(
            onRetryPermission = {
                showContactsRationale = false
                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            onDismiss = { showContactsRationale = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickTeamRecruitDialog(
    initialName: String,
    initialEmail: String,
    initialNotes: String,
    isBusy: Boolean,
    onConfirm: (name: String, email: String, days: Int, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var recruitName by remember { mutableStateOf(initialName) }
    var recruitEmail by remember { mutableStateOf(initialEmail) }
    var suggestedDays by remember { mutableIntStateOf(30) }
    var recruitNotes by remember { mutableStateOf(initialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .imePadding()
            .testTag("quick_team_recruit_dialog"),
        containerColor = SlateCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = EmeraldSuccess)
                Text("Sumar Nuevo Vendedor al Equipo", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Se enviará la solicitud de alta debajo de tu equipo para que el Superusuario la active y quede vinculado contigo.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = recruitName,
                    onValueChange = { recruitName = it },
                    label = { Text("Nombre del nuevo vendedor *", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recruit_dialog_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldSuccess,
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                OutlinedTextField(
                    value = recruitEmail,
                    onValueChange = { recruitEmail = it },
                    label = { Text("Correo Google del vendedor *", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recruit_dialog_email"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldSuccess,
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15, 30, 60, 90).forEach { d ->
                        FilterChip(
                            selected = suggestedDays == d,
                            onClick = { suggestedDays = d },
                            label = { Text("$d días", fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = recruitNotes,
                    onValueChange = { recruitNotes = it },
                    label = { Text("Notas", fontSize = 11.sp) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldSuccess,
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy && recruitName.trim().isNotBlank() && recruitEmail.trim().contains("@"),
                onClick = {
                    onConfirm(recruitName.trim(), recruitEmail.trim().lowercase(), suggestedDays, recruitNotes.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_confirm_quick_recruit")
            ) {
                Text(if (isBusy) "Enviando..." else "Enviar Solicitud", color = SlateBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

private fun statusAccentColor(status: BusinessInviteStatus): Color = when (status) {
    BusinessInviteStatus.INVITED -> PsaCyanGlow
    BusinessInviteStatus.CONFIRMED -> AmberDelay
    BusinessInviteStatus.ATTENDED -> PurpleNeon
    BusinessInviteStatus.JOINED_TEAM -> EmeraldSuccess
    BusinessInviteStatus.NOT_INTERESTED -> RoseError
}

fun computePresentationReminderAtMs(dateKey: String, timeText: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val parsed = sdf.parse("${dateKey.trim()} ${timeText.trim().ifBlank { "19:00" }}") ?: return 0L
        (parsed.time - 15L * 60L * 1000L).coerceAtLeast(System.currentTimeMillis() + 10_000L)
    } catch (_: Exception) {
        0L
    }
}

private fun openWhatsAppBusinessInvite(
    context: Context,
    invite: BusinessInviteEntity,
    sellerName: String
): Boolean {
    val cleanDigits = invite.phone.filter { it.isDigit() }
    if (cleanDigits.isBlank()) {
        Toast.makeText(context, "Agrega un número de teléfono antes de abrir WhatsApp.", Toast.LENGTH_SHORT).show()
        return false
    }
    val dateLabel = invite.presentationDate.ifBlank { "próxima fecha" }
    val timeLabel = invite.presentationTime.ifBlank { "a confirmar" }
    val senderLabel = sellerName.trim().ifBlank { "tu anfitrión" }
    val firstName = invite.name.trim().split(" ").firstOrNull().orEmpty().ifBlank { "Hola" }
    val message = buildString {
        append("¡Hola $firstName! Soy $senderLabel 👋\n\n")
        append("Te quiero invitar especialmente a nuestra *Presentación de Negocios PSA* donde mostramos cómo generar ingresos propios y armar equipo en el rubro de agua pura y salud.\n\n")
        append("📅 *Día:* $dateLabel\n")
        append("⏰ *Hora:* $timeLabel hs\n\n")
        append("¿Te reservo tu lugar para que la veamos juntos?")
    }
    val encoded = URLEncoder.encode(message, "UTF-8")
    val uri = Uri.parse("https://wa.me/$cleanDigits?text=$encoded")
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (_: Exception) {
        Toast.makeText(context, "No se pudo abrir WhatsApp en este dispositivo.", Toast.LENGTH_SHORT).show()
        false
    }
}
