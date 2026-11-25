package com.example.ui.screens.psa

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalIsLargeFontMode
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.PsaAiAudience
import com.example.util.PsaAiChatEntry
import com.example.util.PsaInternalKnowledgeEngine
import com.example.util.PsaKeyQuestionCategory
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaInternalAiPanel(
    viewModel: MainViewModel,
    compactSheetMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLargeFont = LocalIsLargeFontMode.current
    val audience by viewModel.psaAiAudience.collectAsState()
    val demoContext by viewModel.psaAiDemoContext.collectAsState()
    val conversation by viewModel.psaAiConversation.collectAsState()
    val activeCountry by viewModel.activePsaCountry.collectAsState()
    val pricingConfig by viewModel.psaPricingConfig.collectAsState()

    var selectedCategory by remember { mutableStateOf(PsaKeyQuestionCategory.ALL) }
    var customQuestionInput by remember { mutableStateOf("") }

    val allKeyQuestions = remember(audience) {
        PsaInternalKnowledgeEngine.getKeyQuestions(audience)
    }
    val visibleKeyQuestions = remember(allKeyQuestions, selectedCategory) {
        if (selectedCategory == PsaKeyQuestionCategory.ALL) {
            allKeyQuestions
        } else {
            allKeyQuestions.filter { it.category == selectedCategory }
        }
    }

    val submitFreeQuestion: () -> Unit = {
        val clean = customQuestionInput.trim()
        if (clean.isNotBlank()) {
            viewModel.askInternalPsaQuestion(clean)
            customQuestionInput = ""
        }
    }

    val contentColumnModifier = if (compactSheetMode) {
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    } else {
        modifier.fillMaxWidth()
    }

    Column(
        modifier = contentColumnModifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Context Summary Pill
        Surface(
            color = Color(0xFF062033),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Asistente Interno PSA",
                            tint = PsaCyanGlow,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Asistente Interno PSA",
                                color = PsaCyanGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Catálogo oficial, pruebas de agua y guiones (100% offline)",
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    if (conversation.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearPsaAiConversation() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("psa_ai_clear_chat_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Limpiar respuestas",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Live context status pill
                val isWell = demoContext.waterSource.equals("AGUA_DE_POZO", ignoreCase = true)
                val waterSummary = if (isWell) {
                    "Pozo (${if (demoContext.hardnessPpm > 0) "${demoContext.hardnessPpm} ppm dureza" else "napa"})"
                } else {
                    "Red (${String.format(Locale.US, "%.1f ppm cloro", demoContext.chlorinePpm)})"
                }
                val cartPart = demoContext.cartSummary.takeIf { it.isNotBlank() }?.let { " • Carrito: $it" } ?: ""
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateBackground.copy(alpha = 0.78f))
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${activeCountry.flag} ${activeCountry.code} • ${pricingConfig.installmentsCount} cuotas • $waterSummary$cartPart",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Audience Switcher: Vendedor vs Cliente / Usuario
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SlateCard)
                .border(1.dp, SlateCardBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PsaAiAudience.entries.forEach { mode ->
                val selected = audience == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) PsaCyanPrimary else Color.Transparent)
                        .clickable { viewModel.setPsaAiAudience(mode) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("psa_ai_audience_${mode.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (mode == PsaAiAudience.VENDEDOR) Icons.Default.AutoAwesome else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (selected) Color.Black else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mode.label,
                            color = if (selected) Color.Black else TextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Category FilterChips for Preguntas Clave
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Preguntas Clave (${audience.badge}):",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PsaKeyQuestionCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label, fontSize = 11.5.sp) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("psa_ai_cat_${cat.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurpleNeon,
                            selectedLabelColor = Color.White,
                            containerColor = SlateCard,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleKeyQuestions.forEach { kq ->
                    OutlinedButton(
                        onClick = { viewModel.askInternalPsaKeyQuestion(kq) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("psa_ai_kq_${kq.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AmberDelay,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = kq.shortLabel,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Free-text Question Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customQuestionInput,
                onValueChange = { customQuestionInput = it },
                placeholder = {
                    Text(
                        text = if (isLargeFont) "Pregunta sobre PSA o modelos..." else "Pregunta por un modelo, Fipor, pozo, precio o duda...",
                        fontSize = 12.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submitFreeQuestion() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PsaCyanGlow,
                    unfocusedBorderColor = SlateCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("psa_ai_question_input")
            )

            Button(
                onClick = submitFreeQuestion,
                enabled = customQuestionInput.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .testTag("psa_ai_send_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Consultar asistente PSA",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Answer Cards Feed
        if (conversation.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateCardBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Listo para responder al instante",
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                    Text(
                        text = "Toca una Pregunta Clave o escribe cualquier duda (por ej. 'Senior 4', 'ROtech', 'ducha', 'Fipor', 'sarro', 'alquiler'). El asistente adapta la respuesta a tus precios de ${activeCountry.name} y al test de agua activo.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        } else {
            conversation.forEach { entry ->
                PsaInternalAiAnswerCard(
                    context = context,
                    entry = entry,
                    onSaveToMemory = {
                        viewModel.savePsaAiEntryToMemory(entry)
                        Toast.makeText(context, "⭐ Guardado en Patrones de Memoria IA", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        if (compactSheetMode) {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PsaInternalAiAnswerCard(
    context: Context,
    entry: PsaAiChatEntry,
    onSaveToMemory: () -> Unit
) {
    val ans = entry.answer
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("psa_ai_answer_card_${entry.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Question Tag + Audience Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "❓ ${entry.question}",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PurpleNeon.copy(alpha = 0.18f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = entry.audience.label,
                        color = PurpleNeon,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Headline + Recommended Model
            Text(
                text = ans.headline,
                color = PsaCyanGlow,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Equipo sugerido: ${ans.recommendedModelName}",
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }

            // Pricing & Installments Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(EmeraldSuccess.copy(alpha = 0.10f))
                    .border(1.dp, EmeraldSuccess.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = ans.priceAndInstallmentText,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp
                )
            }

            // Technical / Comparative Detail
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "RESPALDO TÉCNICO",
                    color = PsaCyanGlow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = ans.technicalSummary,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            // Tip / Pitch or Client Explanation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmberDelay.copy(alpha = 0.10f))
                    .border(1.dp, AmberDelay.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = if (entry.audience == PsaAiAudience.VENDEDOR) {
                            "💡 TIP DE DEMOSTRACIÓN Y VENTA"
                        } else {
                            "✨ QUÉ SIGNIFICA PARA TU HOGAR"
                        },
                        color = AmberDelay,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = ans.demoOrClientTip,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            // Next Step / Care Note
            Text(
                text = "📌 ${ans.objectionOrNextStep}",
                color = TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )

            HorizontalDivider(color = SlateCardBorder)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Asistente PSA", ans.shareableText))
                        Toast.makeText(context, "Respuesta copiada al portapapeles", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copiar", color = PsaCyanGlow, fontSize = 11.5.sp)
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val uri = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(ans.shareableText))
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (_: Exception) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, ans.shareableText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir información PSA"))
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", color = EmeraldSuccess, fontSize = 11.5.sp)
                }

                Button(
                    onClick = onSaveToMemory,
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleNeon),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("psa_ai_save_memory_btn")
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Guardar ⭐", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
