package com.example.ui.screens.psa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PsaAchievementBadge
import com.example.data.model.PsaContactChannel
import com.example.data.model.PsaContactLead
import com.example.data.model.PsaDailyGoalStats
import com.example.data.model.PsaLeadStatus
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalIsLargeFontMode
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.PsaBlueDark
import com.example.ui.theme.PsaCardBorder
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.PsaWaterBg
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class PsaLeadFilter(val label: String) {
    ALL("Todos"),
    PENDING("⏳ Pendientes"),
    SUGGEST_WHATSAPP("⚠️ 2+ Llamadas (Sugerir WhatsApp)"),
    RESCHEDULED("📅 Reprogramados"),
    APPOINTMENT_SET("🎯 Citas Concretadas"),
    NO_ANSWER("📵 Sin Respuesta")
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PsaContactsTabContent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val leads by viewModel.allPsaLeads.collectAsState()
    val dailyStats by viewModel.dailyContactStats.collectAsState()

    var activeFilter by remember { mutableStateOf(PsaLeadFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    var leadToEdit by remember { mutableStateOf<PsaContactLead?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var leadToLog by remember { mutableStateOf<PsaContactLead?>(null) }
    var showGoalDialog by remember { mutableStateOf(false) }

    // Multi-select state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedLeadIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBatchStatusDialog by remember { mutableStateOf(false) }
    var showBatchRescheduleDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedLeadIds = emptySet()
    }

    val filteredLeads = remember(leads, activeFilter, searchQuery) {
        leads.filter { lead ->
            val matchesFilter = when (activeFilter) {
                PsaLeadFilter.ALL -> true
                PsaLeadFilter.PENDING -> lead.status == PsaLeadStatus.PENDING
                PsaLeadFilter.SUGGEST_WHATSAPP -> lead.callAttemptsCount >= 2 && lead.status != PsaLeadStatus.APPOINTMENT_SET && lead.status != PsaLeadStatus.NOT_INTERESTED
                PsaLeadFilter.RESCHEDULED -> lead.status == PsaLeadStatus.RESCHEDULED || lead.rescheduleDate.isNotBlank()
                PsaLeadFilter.APPOINTMENT_SET -> lead.status == PsaLeadStatus.APPOINTMENT_SET
                PsaLeadFilter.NO_ANSWER -> lead.status == PsaLeadStatus.NO_ANSWER
            }
            val matchesSearch = searchQuery.isBlank() ||
                lead.name.contains(searchQuery, ignoreCase = true) ||
                lead.phone.contains(searchQuery, ignoreCase = true) ||
                lead.neighborhood.contains(searchQuery, ignoreCase = true) ||
                lead.source.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    val toggleLeadSelection: (String) -> Unit = { id ->
        selectedLeadIds = if (selectedLeadIds.contains(id)) {
            selectedLeadIds - id
        } else {
            selectedLeadIds + id
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PsaWaterBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = if (isSelectionMode) 110.dp else 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. HERO CARD: DAILY GOAL & ACHIEVEMENTS ---
            item {
                PsaDailyGoalAchievementCard(
                    stats = dailyStats,
                    onEditGoalClick = { showGoalDialog = true }
                )
            }

            // --- 2. SEARCH & CONTROLS ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Prospectos (${filteredLeads.size}/${leads.size})",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (leads.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        if (isSelectionMode) {
                                            isSelectionMode = false
                                            selectedLeadIds = emptySet()
                                        } else {
                                            isSelectionMode = true
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelectionMode) PsaCyanPrimary.copy(alpha = 0.18f) else Color.Transparent,
                                        contentColor = if (isSelectionMode) PsaCyanPrimary else TextSecondary
                                    ),
                                    border = BorderStroke(1.dp, if (isSelectionMode) PsaCyanPrimary else SlateCardBorder),
                                    modifier = Modifier
                                        .heightIn(min = 44.dp)
                                        .testTag("toggle_multi_select_button")
                                ) {
                                    Icon(
                                        imageVector = if (isSelectionMode) Icons.Default.Close else Icons.Default.Checklist,
                                        contentDescription = if (isSelectionMode) "Salir de selección" else "Selección múltiple",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSelectionMode) "Salir" else "Varios",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    leadToEdit = null
                                    showAddDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("add_lead_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Agregar Prospecto",
                                    tint = PsaBlueDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Nuevo",
                                    color = PsaBlueDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Active multi-select helper bar
                    AnimatedVisibility(visible = isSelectionMode) {
                        val allFilteredSelected = filteredLeads.isNotEmpty() && filteredLeads.all { selectedLeadIds.contains(it.id) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("multi_select_toolbar"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.45f))
                        ) {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${selectedLeadIds.size} seleccionados",
                                    color = PsaCyanPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            selectedLeadIds = if (allFilteredSelected) {
                                                selectedLeadIds - filteredLeads.map { it.id }.toSet()
                                            } else {
                                                selectedLeadIds + filteredLeads.map { it.id }.toSet()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("select_all_leads_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SelectAll,
                                            contentDescription = null,
                                            tint = PsaCyanPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (allFilteredSelected) "Desmarcar (${filteredLeads.size})" else "Todos (${filteredLeads.size})",
                                            color = PsaCyanPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (selectedLeadIds.isNotEmpty()) {
                                        TextButton(
                                            onClick = { selectedLeadIds = emptySet() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("Limpiar", color = TextMuted, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por nombre, teléfono, barrio o referido...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = PsaCyanPrimary, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_leads_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsaCyanPrimary,
                        unfocusedBorderColor = PsaCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SlateCard,
                        unfocusedContainerColor = SlateCard
                    ),
                    singleLine = true
                )

                // Filter chips row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(PsaLeadFilter.values()) { filter ->
                        val count = remember(leads, filter) {
                            when (filter) {
                                PsaLeadFilter.ALL -> leads.size
                                PsaLeadFilter.PENDING -> leads.count { it.status == PsaLeadStatus.PENDING }
                                PsaLeadFilter.SUGGEST_WHATSAPP -> leads.count { it.callAttemptsCount >= 2 && it.status != PsaLeadStatus.APPOINTMENT_SET && it.status != PsaLeadStatus.NOT_INTERESTED }
                                PsaLeadFilter.RESCHEDULED -> leads.count { it.status == PsaLeadStatus.RESCHEDULED || it.rescheduleDate.isNotBlank() }
                                PsaLeadFilter.APPOINTMENT_SET -> leads.count { it.status == PsaLeadStatus.APPOINTMENT_SET }
                                PsaLeadFilter.NO_ANSWER -> leads.count { it.status == PsaLeadStatus.NO_ANSWER }
                            }
                        }
                        val isSelected = activeFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { activeFilter = filter },
                            label = {
                                Text(
                                    text = "${filter.label} ($count)",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PsaCyanPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = PsaCyanPrimary,
                                containerColor = SlateCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) PsaCyanPrimary else SlateCardBorder,
                                selectedBorderColor = PsaCyanPrimary,
                                borderWidth = 1.dp
                            )
                        )
                    }
                }
            }
        }

        // --- 3. LIST OF LEADS ---
        if (filteredLeads.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, PsaCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (leads.isEmpty()) "📭 No tienes prospectos cargados" else "🔍 No hay resultados para este filtro",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (leads.isEmpty())
                                "Comienza agregando los contactos que te pasaron tus clientes satisfechos para gestionar llamadas y mensajes diarios."
                            else
                                "Prueba cambiando de filtro o borrando la búsqueda para ver tus demás prospectos.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        if (leads.isEmpty()) {
                            Button(
                                onClick = {
                                    leadToEdit = null
                                    showAddDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = PsaBlueDark)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cargar Primer Prospecto", color = PsaBlueDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredLeads, key = { it.id }) { lead ->
                val isSelected = selectedLeadIds.contains(lead.id)
                PsaLeadCard(
                    lead = lead,
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected,
                    onToggleSelect = { toggleLeadSelection(lead.id) },
                    onLongPress = {
                        if (!isSelectionMode) {
                            isSelectionMode = true
                            selectedLeadIds = selectedLeadIds + lead.id
                        }
                    },
                    onCallClick = {
                        val cleanPhone = lead.phone.replace("[^0-9+]".toRegex(), "")
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                        context.startActivity(intent)
                        leadToLog = lead
                    },
                    onWhatsAppClick = {
                        val cleanPhone = lead.phone.replace("[^0-9]".toRegex(), "")
                        val greeting = "Hola ${lead.name}, ¿cómo estás? Te escribo por recomendación de ${lead.source.ifBlank { "un conocido" }} sobre el agua pura PSA en tu hogar."
                        val encoded = Uri.encode(greeting)
                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encoded")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
                        }
                        leadToLog = lead
                    },
                    onLogInteractionClick = { leadToLog = lead },
                    onEditClick = {
                        leadToEdit = lead
                        showAddDialog = true
                    },
                    onDeleteClick = {
                        viewModel.deletePsaLead(lead)
                        Toast.makeText(context, "Prospecto eliminado", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        }

        // --- FLOATING MULTI-SELECT BATCH ACTION BAR ---
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("floating_batch_action_bar"),
                shape = RoundedCornerShape(18.dp),
                color = SlateCard,
                shadowElevation = 12.dp,
                border = BorderStroke(1.5.dp, PsaCyanGlow)
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PsaCyanPrimary.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${selectedLeadIds.size}",
                            color = PsaCyanPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = { showBatchStatusDialog = true },
                        enabled = selectedLeadIds.isNotEmpty(),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("batch_status_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleNeon,
                            disabledContainerColor = SlateBackground
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = "Cambiar estado",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Estado",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = { showBatchRescheduleDialog = true },
                        enabled = selectedLeadIds.isNotEmpty(),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("batch_reschedule_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PsaCyanPrimary,
                            disabledContainerColor = SlateBackground
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "Reprogramar",
                            tint = PsaBlueDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reprogramar",
                            color = PsaBlueDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    IconButton(
                        onClick = { showBatchDeleteConfirm = true },
                        enabled = selectedLeadIds.isNotEmpty(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedLeadIds.isNotEmpty()) RoseError.copy(alpha = 0.16f) else SlateBackground)
                            .testTag("batch_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar seleccionados",
                            tint = if (selectedLeadIds.isNotEmpty()) RoseError else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    if (showAddDialog) {
        PsaAddOrEditLeadDialog(
            lead = leadToEdit,
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, address, neighborhood, source, notes ->
                if (leadToEdit != null) {
                    viewModel.updatePsaLead(
                        leadToEdit!!.copy(
                            name = name,
                            phone = phone,
                            address = address,
                            neighborhood = neighborhood,
                            source = source,
                            notes = notes
                        )
                    )
                    Toast.makeText(context, "Prospecto actualizado", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addPsaLead(
                        name = name,
                        phone = phone,
                        address = address,
                        neighborhood = neighborhood,
                        source = source,
                        notes = notes
                    )
                    Toast.makeText(context, "Prospecto guardado", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
            }
        )
    }

    if (leadToLog != null) {
        PsaLogInteractionDialog(
            lead = leadToLog!!,
            onDismiss = { leadToLog = null },
            onSaveInteraction = { channel, result, note, rescheduleDate ->
                viewModel.logPsaLeadInteraction(
                    lead = leadToLog!!,
                    channel = channel,
                    result = result,
                    note = note,
                    rescheduleDate = rescheduleDate
                )
                Toast.makeText(context, "Gestión registrada exitosamente", Toast.LENGTH_SHORT).show()
                leadToLog = null
            },
            onConvertToAppointment = { date, time, model, notes ->
                viewModel.convertLeadToAppointment(
                    context = context,
                    lead = leadToLog!!,
                    scheduledDate = date,
                    scheduledTime = time,
                    modelInterest = model,
                    notes = notes
                )
                Toast.makeText(context, "¡Cita coordinada y agregada a tu Agenda PSA! 🎯", Toast.LENGTH_LONG).show()
                leadToLog = null
            }
        )
    }

    if (showGoalDialog) {
        PsaEditGoalDialog(
            currentGoal = dailyStats.dailyGoal,
            onDismiss = { showGoalDialog = false },
            onSaveGoal = { newGoal ->
                viewModel.setPsaDailyGoal(newGoal)
                showGoalDialog = false
            }
        )
    }

    if (showBatchStatusDialog && selectedLeadIds.isNotEmpty()) {
        PsaBatchStatusDialog(
            selectedCount = selectedLeadIds.size,
            onDismiss = { showBatchStatusDialog = false },
            onApplyStatus = { newStatus, rescheduleDate, note ->
                val count = selectedLeadIds.size
                viewModel.batchUpdatePsaLeadsStatus(
                    leadIds = selectedLeadIds,
                    newStatus = newStatus,
                    rescheduleDate = rescheduleDate,
                    rescheduleNote = note
                )
                Toast.makeText(
                    context,
                    "Estado '${newStatus.label}' aplicado a $count prospectos",
                    Toast.LENGTH_SHORT
                ).show()
                showBatchStatusDialog = false
                selectedLeadIds = emptySet()
                isSelectionMode = false
            }
        )
    }

    if (showBatchRescheduleDialog && selectedLeadIds.isNotEmpty()) {
        PsaBatchRescheduleDialog(
            selectedCount = selectedLeadIds.size,
            onDismiss = { showBatchRescheduleDialog = false },
            onConfirmReschedule = { date, note ->
                val count = selectedLeadIds.size
                viewModel.batchReschedulePsaLeads(
                    leadIds = selectedLeadIds,
                    rescheduleDate = date,
                    rescheduleNote = note
                )
                Toast.makeText(
                    context,
                    "$count prospectos reprogramados para el $date 📅",
                    Toast.LENGTH_SHORT
                ).show()
                showBatchRescheduleDialog = false
                selectedLeadIds = emptySet()
                isSelectionMode = false
            }
        )
    }

    if (showBatchDeleteConfirm && selectedLeadIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = {
                Text(
                    text = "Eliminar ${selectedLeadIds.size} prospectos",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Confirmas eliminar los ${selectedLeadIds.size} prospectos seleccionados? Esta acción no se puede deshacer.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = selectedLeadIds.size
                        viewModel.batchDeletePsaLeads(selectedLeadIds)
                        Toast.makeText(context, "$count prospectos eliminados", Toast.LENGTH_SHORT).show()
                        showBatchDeleteConfirm = false
                        selectedLeadIds = emptySet()
                        isSelectionMode = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SlateCard
        )
    }
}

// -------------------------------------------------------------
// COMPONENT: DAILY GOAL & ACHIEVEMENTS CARD
// -------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PsaDailyGoalAchievementCard(
    stats: PsaDailyGoalStats,
    onEditGoalClick: () -> Unit
) {
    val isLargeFont = LocalIsLargeFontMode.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_goal_achievement_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title & Streak Badge
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🎯 Meta Diaria de Gestiones",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onEditGoalClick, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar Meta", tint = PsaCyanPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = "Objetivo: ${stats.dailyGoal} contactos hoy",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                // Streak Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AmberDelay.copy(alpha = 0.14f))
                        .border(1.dp, AmberDelay.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Racha: ${stats.currentStreakDays} ${if (stats.currentStreakDays == 1) "día" else "días"}",
                            color = AmberDelay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Progress Bar & Percentage
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${stats.totalContactsToday} de ${stats.dailyGoal} gestiones",
                        color = PsaCyanPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${(stats.progressPercent * 100).toInt()}% cumplido",
                        color = if (stats.progressPercent >= 1f) EmeraldSuccess else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                LinearProgressIndicator(
                    progress = { stats.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (stats.progressPercent >= 1f) EmeraldSuccess else PsaCyanPrimary,
                    trackColor = SlateBackground
                )
            }

            // Breakdown Metrics Chips: 2x2 grid on large font, 4 columns on standard scale
            if (isLargeFont) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricMiniCard(
                            modifier = Modifier.weight(1f),
                            label = "Llamadas",
                            value = "${stats.callsCount}",
                            icon = "📞",
                            color = PsaCyanPrimary
                        )
                        MetricMiniCard(
                            modifier = Modifier.weight(1f),
                            label = "WhatsApp",
                            value = "${stats.messagesCount}",
                            icon = "💬",
                            color = EmeraldSuccess
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricMiniCard(
                            modifier = Modifier.weight(1f),
                            label = "Aciertos",
                            value = "${stats.successCount}",
                            icon = "🎯",
                            color = PurpleNeon
                        )
                        MetricMiniCard(
                            modifier = Modifier.weight(1f),
                            label = "Reprog.",
                            value = "${stats.rescheduledCount}",
                            icon = "⏰",
                            color = AmberDelay
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricMiniCard(
                        modifier = Modifier.weight(1f),
                        label = "Llamadas",
                        value = "${stats.callsCount}",
                        icon = "📞",
                        color = PsaCyanPrimary
                    )
                    MetricMiniCard(
                        modifier = Modifier.weight(1f),
                        label = "WhatsApp",
                        value = "${stats.messagesCount}",
                        icon = "💬",
                        color = EmeraldSuccess
                    )
                    MetricMiniCard(
                        modifier = Modifier.weight(1f),
                        label = "Aciertos",
                        value = "${stats.successCount}",
                        icon = "🎯",
                        color = PurpleNeon
                    )
                    MetricMiniCard(
                        modifier = Modifier.weight(1f),
                        label = "Reprog.",
                        value = "${stats.rescheduledCount}",
                        icon = "⏰",
                        color = AmberDelay
                    )
                }
            }

            // Achievement Badges Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Logros de Hoy",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(stats.unlockedBadges) { badge ->
                        AchievementBadgeChip(badge = badge)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniCard(
    label: String,
    value: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SlateBackground)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 14.sp)
            Text(text = value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = label, color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AchievementBadgeChip(badge: PsaAchievementBadge) {
    val isUnlocked = badge.isUnlocked
    val bgColor = if (isUnlocked) PurpleNeon.copy(alpha = 0.2f) else SlateBackground.copy(alpha = 0.5f)
    val borderColor = if (isUnlocked) PurpleNeon else SlateCardBorder.copy(alpha = 0.4f)
    val textColor = if (isUnlocked) TextPrimary else TextMuted

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = badge.icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = badge.title,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = if (isUnlocked) FontWeight.Bold else FontWeight.Normal
            )
            if (!isUnlocked) {
                Text(
                    text = "Bloqueado",
                    color = TextMuted,
                    fontSize = 9.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: PSA LEAD CARD
// -------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PsaLeadCard(
    lead: PsaContactLead,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onLogInteractionClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isTwoCallsCadence = lead.callAttemptsCount >= 2 && lead.status != PsaLeadStatus.APPOINTMENT_SET && lead.status != PsaLeadStatus.NOT_INTERESTED

    val cardContainerColor = if (isSelectionMode && isSelected) {
        PsaCyanPrimary.copy(alpha = 0.12f)
    } else {
        SlateCard
    }
    val cardBorderColor = when {
        isSelectionMode && isSelected -> PsaCyanPrimary
        isTwoCallsCadence -> AmberDelay.copy(alpha = 0.5f)
        else -> SlateCardBorder.copy(alpha = 0.65f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    }
                },
                onLongClick = onLongPress
            )
            .testTag("lead_card_${lead.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        border = BorderStroke(
            width = if (isSelectionMode && isSelected) 2.dp else 1.dp,
            color = cardBorderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Checkbox (if multi-select), Name, Source Chip, Actions
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PsaCyanPrimary,
                                uncheckedColor = TextMuted,
                                checkmarkColor = PsaBlueDark
                            ),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .testTag("lead_checkbox_${lead.id}")
                        )
                    }
                    Column {
                        Text(
                            text = lead.name,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = lead.phone,
                                color = PsaCyanPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (lead.neighborhood.isNotBlank()) {
                                Text(text = "•", color = TextMuted, fontSize = 11.sp)
                                Text(text = lead.neighborhood, color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Source and Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PsaBlueDark)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = lead.source,
                            color = PsaCyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(onClick = onEditClick, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = RoseError.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Address & Notes if available
            if (lead.address.isNotBlank() || lead.notes.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SlateBackground)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (lead.address.isNotBlank()) {
                        Text(
                            text = "📍 ${lead.address}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    if (lead.notes.isNotBlank()) {
                        Text(
                            text = "📝 ${lead.notes}",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Counters and Current Status Pill (adapts cleanly at 140%–160%)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateBackground)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "📞 ${lead.callAttemptsCount} llamadas • 💬 ${lead.messageAttemptsCount} mensajes",
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Status chip
                val statusColor = when (lead.status) {
                    PsaLeadStatus.PENDING -> TextMuted
                    PsaLeadStatus.INTERESTED -> PurpleNeon
                    PsaLeadStatus.NO_ANSWER -> AmberDelay
                    PsaLeadStatus.RESCHEDULED -> AmberDelay
                    PsaLeadStatus.APPOINTMENT_SET -> EmeraldSuccess
                    PsaLeadStatus.NOT_INTERESTED -> RoseError
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor.copy(alpha = 0.38f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${lead.status.icon} ${lead.status.label}",
                        color = statusColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Reschedule note if set
            if (lead.rescheduleDate.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberDelay.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⏰", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Llamar el ${lead.rescheduleDate}${if (lead.rescheduleNote.isNotBlank()) ": ${lead.rescheduleNote}" else ""}",
                        color = AmberDelay,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // --- CADENCE RULE BANNER (2+ LLAMADAS SUGIERE WHATSAPP) ---
            if (isTwoCallsCadence) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmberDelay.copy(alpha = 0.15f))
                        .border(1.dp, AmberDelay, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Regla de Cadencia: 2 llamadas sin éxito",
                                color = AmberDelay,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "El próximo contacto sugerido es a través de WhatsApp 💬",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // --- ACTIONS FLOWROW (adapts cleanly up to 160%) ---
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCallClick,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("call_lead_${lead.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PsaCyanPrimary),
                    border = BorderStroke(1.dp, PsaCyanPrimary)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Llamar", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Llamar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("whatsapp_lead_${lead.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTwoCallsCadence) EmeraldSuccess else EmeraldSuccess.copy(alpha = 0.85f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isTwoCallsCadence) "💬 WhatsApp ⭐" else "WhatsApp",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onLogInteractionClick,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("log_interaction_${lead.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SlateBackground),
                    border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.7f))
                ) {
                    Text("🎯 Anotar", color = PurpleNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOG: ADD OR EDIT LEAD
// -------------------------------------------------------------

@Composable
private fun PsaAddOrEditLeadDialog(
    lead: PsaContactLead?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String, neighborhood: String, source: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(lead?.name ?: "") }
    var phone by remember { mutableStateOf(lead?.phone ?: "") }
    var address by remember { mutableStateOf(lead?.address ?: "") }
    var neighborhood by remember { mutableStateOf(lead?.neighborhood ?: "") }
    var source by remember { mutableStateOf(lead?.source ?: "Referido") }
    var notes by remember { mutableStateOf(lead?.notes ?: "") }

    val sourceOptions = listOf("Referido", "Familiar", "Vecino", "Redes Sociales", "Puerta a Puerta", "Folleto")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (lead != null) "Editar Prospecto" else "Nuevo Prospecto a Contactar",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre y Apellido *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsaCyanPrimary,
                        unfocusedBorderColor = PsaCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono / WhatsApp *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsaCyanPrimary,
                        unfocusedBorderColor = PsaCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Dirección") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanPrimary,
                            unfocusedBorderColor = PsaCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = neighborhood,
                        onValueChange = { neighborhood = it },
                        label = { Text("Barrio") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanPrimary,
                            unfocusedBorderColor = PsaCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                // Source selection chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Origen del Prospecto:", color = TextSecondary, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(sourceOptions) { opt ->
                            FilterChip(
                                selected = source == opt,
                                onClick = { source = opt },
                                label = { Text(opt, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PsaCyanPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = PsaCyanPrimary
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Quién lo recomendó") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsaCyanPrimary,
                        unfocusedBorderColor = PsaCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onSave(name.trim(), phone.trim(), address.trim(), neighborhood.trim(), source.trim(), notes.trim())
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary)
            ) {
                Text("Guardar", color = PsaBlueDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SlateCard
    )
}

// -------------------------------------------------------------
// DIALOG: LOG INTERACTION (LLAMADA O MENSAJE + RESULTADO)
// -------------------------------------------------------------

@Composable
private fun PsaLogInteractionDialog(
    lead: PsaContactLead,
    onDismiss: () -> Unit,
    onSaveInteraction: (channel: String, result: String, note: String, rescheduleDate: String) -> Unit,
    onConvertToAppointment: (date: String, time: String, model: String, notes: String) -> Unit
) {
    var channel by remember { mutableStateOf(if (lead.callAttemptsCount >= 2) "MESSAGE" else "CALL") }
    var result by remember { mutableStateOf("NO_ANSWER") }
    var note by remember { mutableStateOf("") }
    var rescheduleDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var appointmentTime by remember { mutableStateOf("16:30") }
    var selectedModel by remember { mutableStateOf("PSA Senior 4") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Registrar Gestión",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Con: ${lead.name} (${lead.phone})",
                    color = PsaCyanPrimary,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Canal selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Canal utilizado:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = channel == "CALL",
                            onClick = { channel = "CALL" },
                            label = { Text("📞 Llamada Telefónica", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PsaCyanPrimary.copy(alpha = 0.25f),
                                selectedLabelColor = PsaCyanPrimary
                            )
                        )
                        FilterChip(
                            selected = channel == "MESSAGE",
                            onClick = { channel = "MESSAGE" },
                            label = { Text("💬 WhatsApp / SMS", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldSuccess.copy(alpha = 0.25f),
                                selectedLabelColor = EmeraldSuccess
                            )
                        )
                    }
                }

                // Resultado de la gestión
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("¿Qué ocurrió?", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    val results = listOf(
                        Triple("SUCCESS", "🎯 Cita Concretada (¡Acierto!)", EmeraldSuccess),
                        Triple("NO_ANSWER", "📵 No respondió", AmberDelay),
                        Triple("RESCHEDULED", "📅 Pidió llamar en otra fecha", PsaCyanPrimary),
                        Triple("REJECTED", "🚫 No interesado", RoseError)
                    )

                    results.forEach { (code, title, color) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (result == code) color.copy(alpha = 0.2f) else SlateBackground)
                                .border(1.dp, if (result == code) color else SlateCardBorder, RoundedCornerShape(8.dp))
                                .clickable { result = code }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = title, color = if (result == code) color else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // If appointment concreted, show direct booking inputs
                if (result == "SUCCESS") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldSuccess.copy(alpha = 0.1f))
                            .border(1.dp, EmeraldSuccess.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🎉 ¡Excelente! Coordina la demostración:",
                            color = EmeraldSuccess,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = rescheduleDate,
                                onValueChange = { rescheduleDate = it },
                                label = { Text("Fecha (AAAA-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldSuccess,
                                    unfocusedBorderColor = PsaCardBorder
                                )
                            )
                            OutlinedTextField(
                                value = appointmentTime,
                                onValueChange = { appointmentTime = it },
                                label = { Text("Hora (ej. 17:00)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldSuccess,
                                    unfocusedBorderColor = PsaCardBorder
                                )
                            )
                        }
                    }
                }

                // If reschedule, show target date input
                if (result == "RESCHEDULED") {
                    OutlinedTextField(
                        value = rescheduleDate,
                        onValueChange = { rescheduleDate = it },
                        label = { Text("¿Cuándo volver a llamar? (AAAA-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanPrimary,
                            unfocusedBorderColor = PsaCardBorder
                        )
                    )
                }

                // Observaciones
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota de la conversación (opcional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsaCyanPrimary,
                        unfocusedBorderColor = PsaCardBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (result == "SUCCESS") {
                        onConvertToAppointment(rescheduleDate, appointmentTime, selectedModel, note)
                    } else {
                        onSaveInteraction(channel, result, note, if (result == "RESCHEDULED") rescheduleDate else "")
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (result == "SUCCESS") EmeraldSuccess else PsaCyanPrimary
                )
            ) {
                Text(
                    text = if (result == "SUCCESS") "Guardar y Agendar Cita" else "Registrar Gestión",
                    color = if (result == "SUCCESS") Color.White else PsaBlueDark,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SlateCard
    )
}

// -------------------------------------------------------------
// DIALOG: EDIT DAILY GOAL
// -------------------------------------------------------------

@Composable
private fun PsaEditGoalDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSaveGoal: (Int) -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    val quickGoals = listOf(10, 15, 20, 25, 30, 40)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Ajustar Meta Diaria de Contactos", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Define cuántas llamadas y mensajes te propones realizar por día para mantener activo tu negocio PSA:",
                    color = TextMuted,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Cantidad de contactos por día") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsaCyanPrimary,
                        unfocusedBorderColor = PsaCardBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickGoals.forEach { q ->
                        FilterChip(
                            selected = goalText == q.toString(),
                            onClick = { goalText = q.toString() },
                            label = { Text("$q", fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = goalText.toIntOrNull() ?: 20
                    onSaveGoal(parsed)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary)
            ) {
                Text("Aplicar Meta", color = PsaBlueDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SlateCard
    )
}

// -------------------------------------------------------------
// DIALOG: BATCH UPDATE STATUS FOR MULTIPLE LEADS
// -------------------------------------------------------------

@Composable
private fun PsaBatchStatusDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onApplyStatus: (status: PsaLeadStatus, rescheduleDate: String, note: String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(PsaLeadStatus.INTERESTED) }
    var rescheduleDate by remember { mutableStateOf(nextDateFormatted(1)) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Cambiar Estado Masivo",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Se aplicará a $selectedCount ${if (selectedCount == 1) "prospecto" else "prospectos"}",
                    color = PsaCyanPrimary,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PsaLeadStatus.values().forEach { status ->
                    val statusColor = when (status) {
                        PsaLeadStatus.PENDING -> TextMuted
                        PsaLeadStatus.INTERESTED -> PurpleNeon
                        PsaLeadStatus.NO_ANSWER -> AmberDelay
                        PsaLeadStatus.RESCHEDULED -> PsaCyanPrimary
                        PsaLeadStatus.APPOINTMENT_SET -> EmeraldSuccess
                        PsaLeadStatus.NOT_INTERESTED -> RoseError
                    }
                    val isSelected = selectedStatus == status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) statusColor.copy(alpha = 0.2f) else SlateBackground)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) statusColor else SlateCardBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedStatus = status }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${status.icon} ${status.label}",
                            color = if (isSelected) statusColor else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (selectedStatus == PsaLeadStatus.RESCHEDULED) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = rescheduleDate,
                        onValueChange = { rescheduleDate = it },
                        label = { Text("Fecha a reprogramar (AAAA-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanPrimary,
                            unfocusedBorderColor = PsaCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota compartida (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsaCyanPrimary,
                        unfocusedBorderColor = PsaCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyStatus(
                        selectedStatus,
                        if (selectedStatus == PsaLeadStatus.RESCHEDULED) rescheduleDate else "",
                        note
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleNeon),
                modifier = Modifier.testTag("confirm_batch_status_button")
            ) {
                Text("Aplicar ($selectedCount)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SlateCard
    )
}

// -------------------------------------------------------------
// DIALOG: BATCH RESCHEDULE MULTIPLE LEADS
// -------------------------------------------------------------

@Composable
private fun PsaBatchRescheduleDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirmReschedule: (date: String, note: String) -> Unit
) {
    val quickDates = remember {
        listOf(
            "Hoy" to nextDateFormatted(0),
            "Mañana" to nextDateFormatted(1),
            "En 2 días" to nextDateFormatted(2),
            "En 3 días" to nextDateFormatted(3),
            "+1 Semana" to nextDateFormatted(7)
        )
    }

    var targetDate by remember { mutableStateOf(quickDates[1].second) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Reprogramar en Lote",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "$selectedCount ${if (selectedCount == 1) "prospecto" else "prospectos"} pasarán a 'Reprogramado'",
                    color = PsaCyanPrimary,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Atajos rápidos de fecha:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickDates) { (label, dateVal) ->
                            val isSelected = targetDate == dateVal
                            FilterChip(
                                selected = isSelected,
                                onClick = { targetDate = dateVal },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PsaCyanPrimary.copy(alpha = 0.25f),
                                    selectedLabelColor = PsaCyanPrimary
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("Fecha de próximo contacto (AAAA-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_reschedule_date_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsaCyanPrimary,
                        unfocusedBorderColor = PsaCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Motivo o recordatorio (opcional)") },
                    placeholder = { Text("Ej: Llamar después de las 18hs", color = TextMuted) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsaCyanPrimary,
                        unfocusedBorderColor = PsaCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (targetDate.isNotBlank()) {
                        onConfirmReschedule(targetDate.trim(), note.trim())
                    }
                },
                enabled = targetDate.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                modifier = Modifier.testTag("confirm_batch_reschedule_button")
            ) {
                Text("Reprogramar ($selectedCount)", color = PsaBlueDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SlateCard
    )
}

private fun nextDateFormatted(daysAhead: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, daysAhead)
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
}

