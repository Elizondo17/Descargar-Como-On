package com.example.ui.screens.psa

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PsaContactLead
import com.example.data.model.PsaHomeVisit
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AgendaSearchScope
import com.example.util.PsaAgendaSearchHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaAgendaSearchBar(
    query: String,
    scope: AgendaSearchScope,
    totalMatchesCount: Int,
    onQueryChange: (String) -> Unit,
    onScopeChange: (AgendaSearchScope) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            label = { Text("Buscar contacto por nombre o fecha") },
            placeholder = { Text("Ej: Juan, 22/11 o Viernes") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar en agenda",
                    tint = PsaCyanGlow
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("psa_agenda_search_clear")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar búsqueda",
                            tint = TextSecondary
                        )
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlateCard,
                unfocusedContainerColor = SlateCard,
                focusedBorderColor = PsaCyanGlow,
                unfocusedBorderColor = SlateCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = PsaCyanGlow,
                unfocusedLabelColor = TextSecondary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("psa_agenda_search_input")
        )

        AnimatedVisibility(visible = query.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AgendaSearchScope.entries.forEach { option ->
                        FilterChip(
                            selected = scope == option,
                            onClick = { onScopeChange(option) },
                            label = { Text(option.label, fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PsaCyanPrimary,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("psa_agenda_search_scope_${option.name.lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$totalMatchesCount coincidencia(s) en tiempo real",
                        color = if (totalMatchesCount > 0) PsaCyanGlow else AmberDelay,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = onClear,
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Text("Ver agenda completa", color = TextSecondary, fontSize = 11.5.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaAgendaSearchVisitCard(
    context: Context,
    visit: PsaHomeVisit,
    friendlyDateLabel: String,
    onStartDemo: (PsaHomeVisit) -> Unit,
    onRegisterPostSale: (PsaHomeVisit) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1724)),
        border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("psa_search_visit_${visit.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "📅 $friendlyDateLabel • ⏰ ${visit.scheduledTime}",
                    color = PsaCyanGlow,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(IndigoPrimary.copy(alpha = 0.2f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = visit.status.name,
                        color = IndigoPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "👤 ${visit.customerName} (${visit.phone})",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )

            if (visit.address.isNotBlank() || visit.neighborhood.isNotBlank()) {
                Text(
                    text = "📍 ${listOf(visit.address, visit.neighborhood).filter { it.isNotBlank() }.joinToString(" - ")}",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
            }

            Text(
                text = "💧 Interés: ${visit.modelInterest}",
                color = TextSecondary,
                fontSize = 11.5.sp
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

                OutlinedButton(
                    onClick = {
                        val reminderText = "Hola ${visit.customerName}, ¡buen día! Te recuerdo nuestra visita en tu domicilio (${visit.address}) el ${visit.scheduledDate} a las ${visit.scheduledTime}hs para la demostración de agua PSA 💧."
                        val cleanPhone = visit.phone.replace(Regex("[^0-9]"), "")
                        runCatching {
                            val uri = if (cleanPhone.length >= 8) {
                                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=" + Uri.encode(reminderText))
                            } else {
                                Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(reminderText))
                            }
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.55f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = AmberDelay, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("WhatsApp", color = AmberDelay, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

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
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaAgendaSearchLeadCard(
    context: Context,
    lead: PsaContactLead,
    onScheduleVisitForLead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateLabel = when {
        lead.rescheduleDate.isNotBlank() -> "Reprogramado: ${PsaAgendaSearchHelper.formatFriendlyDate(lead.rescheduleDate)}"
        lead.lastContactDateMs > 0L -> "Último contacto: ${PsaAgendaSearchHelper.formatTimestampDate(lead.lastContactDateMs)}"
        else -> "Agregado: ${PsaAgendaSearchHelper.formatTimestampDate(lead.createdAtMs)}"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, SlateCardBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("psa_search_lead_${lead.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "👤 ${lead.name}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PsaCyanPrimary.copy(alpha = 0.16f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = lead.source,
                        color = PsaCyanGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (lead.phone.isNotBlank()) {
                Text(
                    text = "📞 ${lead.phone}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            if (lead.address.isNotBlank() || lead.neighborhood.isNotBlank()) {
                Text(
                    text = "📍 ${listOf(lead.address, lead.neighborhood).filter { it.isNotBlank() }.joinToString(" - ")}",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
            }

            Text(
                text = "📅 $dateLabel",
                color = PsaCyanGlow,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onScheduleVisitForLead,
                    colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Agendar Visita", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (lead.phone.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val cleanPhone = lead.phone.replace(Regex("[^0-9]"), "")
                            val msg = "Hola ${lead.name}, ¡cómo estás? Te escribo para coordinar tu demostración gratuita de agua PSA 💧."
                            runCatching {
                                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=" + Uri.encode(msg))
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("WhatsApp", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PsaAgendaSearchEmptyState(
    query: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SlateCard)
            .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
            .padding(20.dp)
            .testTag("psa_agenda_search_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = "Sin coincidencias",
            tint = TextMuted,
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = "Sin contactos ni fechas para \"$query\"",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp
        )
        Text(
            text = "Prueba con otro nombre, teléfono o formato de fecha (ej: 22/11, Nov o Viernes).",
            color = TextSecondary,
            fontSize = 11.5.sp
        )
        OutlinedButton(
            onClick = onClear,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text("Limpiar búsqueda", color = PsaCyanGlow, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}
