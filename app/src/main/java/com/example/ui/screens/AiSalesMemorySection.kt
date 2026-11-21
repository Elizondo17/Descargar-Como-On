package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.data.model.SalesKnowledgeItem
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSalesMemorySection(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allKnowledge by viewModel.allSalesKnowledge.collectAsState()
    val memoryAdvice by viewModel.memorySalesAdvice.collectAsState()
    val scannedChats by viewModel.allScannedChats.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiErrorMessage by viewModel.aiErrorMessage.collectAsState()

    var customerInput by remember { mutableStateOf("") }
    var selectedMemoryFilter by remember { mutableStateOf("ALL") } // ALL, QUESTIONS, RESPONSES
    var showAddDialog by remember { mutableStateOf(false) }
    var showFullKnowledgeList by remember { mutableStateOf(false) }

    val totalConversionPoints = remember(allKnowledge) {
        allKnowledge.sumOf { it.successScore }
    }

    val filteredKnowledge = remember(allKnowledge, selectedMemoryFilter) {
        when (selectedMemoryFilter) {
            "QUESTIONS" -> allKnowledge.filter { it.isQuestion }
            "RESPONSES" -> allKnowledge.filter { !it.isQuestion }
            else -> allKnowledge
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateCard)
            .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header with badge
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PurpleNeon.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "IA Memoria",
                        tint = PurpleNeon,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Memoria de Ventas IA",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(IndigoPrimary.copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Evolutiva",
                                color = IndigoPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Patrones aprendidos con ⭐",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Stats badge
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AmberDelay,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$totalConversionPoints pts",
                        color = AmberDelay,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${allKnowledge.size} patrones",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        // Input text area
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Consulta, objeción o chat del cliente:",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = customerInput,
                onValueChange = { customerInput = it },
                placeholder = {
                    Text(
                        "Ej: 'Me interesa pero el precio me parece alto', o '¿Tienen garantía y envíos?'",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_memory_customer_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoPrimary,
                    unfocusedBorderColor = SlateCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SlateBackground,
                    unfocusedContainerColor = SlateBackground
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Scanned CRM chats picker if available
            val recentCrmChats = remember(scannedChats) {
                scannedChats.filter { it.lastMessage.isNotBlank() }.take(3)
            }
            if (recentCrmChats.isNotEmpty()) {
                Text(
                    text = "📥 Cargar chat detectado en CRM:",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    recentCrmChats.forEach { chat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(IndigoPrimary.copy(alpha = 0.15f))
                                .border(1.dp, IndigoPrimary.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .clickable { customerInput = chat.lastMessage }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${chat.name.take(10)}: \"${chat.lastMessage.take(16)}...\"",
                                color = TextPrimary,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Quick suggestion chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "¿Tienen garantía?" to "Garantía",
                    "¿Por qué el precio?" to "Precio / Objeción",
                    "Lo tengo que pensar" to "Indecisión",
                    "Quiero coordinar entrega" to "Cierre"
                ).forEach { (prompt, label) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SlateBackground)
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(6.dp))
                            .clickable { customerInput = prompt }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(label, color = TextSecondary, fontSize = 10.5.sp)
                    }
                }
            }
        }

        // Generate button
        Button(
            onClick = {
                if (customerInput.isNotBlank()) {
                    viewModel.generateMemoryEnhancedAdvice(customerInput)
                } else {
                    Toast.makeText(context, "Escribe una consulta u objeción primero", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isAiLoading && customerInput.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .testTag("btn_generate_with_memory"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PurpleNeon,
                disabledContainerColor = PurpleNeon.copy(alpha = 0.3f)
            )
        ) {
            if (isAiLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Consultando memoria y optimizando...", fontSize = 12.sp, color = Color.White)
            } else {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generar Preguntas y Respuestas Ganadoras", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (aiErrorMessage != null) {
            Text(
                text = "⚠️ $aiErrorMessage",
                color = RoseError,
                fontSize = 11.sp
            )
        }

        // Generated Results from Memory Engine
        AnimatedVisibility(
            visible = memoryAdvice != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val advice = memoryAdvice
            if (advice != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateBackground)
                        .border(1.dp, IndigoPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Learning insight banner
                    if (advice.learningInsight.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(IndigoPrimary.copy(alpha = 0.15f))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = AmberDelay,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = advice.learningInsight,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 1. Persuasive Questions (To Lead & Convince)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = PurpleNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🎯 Preguntas Persuasivas (Para Descubrir y Guiar al Cierre)",
                                color = PurpleNeon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        advice.persuasiveQuestions.forEachIndexed { idx, question ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SlateCard)
                                    .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "\"$question\"",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Copy button
                                        OutlinedButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Pregunta Persuasiva", question))
                                                Toast.makeText(context, "Pregunta copiada al portapapeles", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.heightIn(min = 34.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copiar", fontSize = 10.5.sp)
                                        }

                                        // Rate button (save into memory)
                                        Button(
                                            onClick = {
                                                viewModel.addCustomWinningKnowledge(
                                                    category = "PREGUNTA",
                                                    triggerContext = customerInput,
                                                    content = question,
                                                    isQuestion = true
                                                )
                                                Toast.makeText(context, "⭐ ¡Pregunta guardada en la memoria de la IA!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.heightIn(min = 34.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = AmberDelay),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("⭐ Ganadora (+1)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. High-Conversion Responses
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "💬 Respuestas Optimizadas de Alta Conversión",
                                color = EmeraldSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        advice.highConversionResponses.forEachIndexed { idx, responseOption ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SlateCard)
                                    .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(EmeraldSuccess.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = responseOption.title,
                                                color = EmeraldSuccess,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                        if (responseOption.strategy.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = responseOption.strategy,
                                                color = TextMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = responseOption.text,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Copy button
                                        OutlinedButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Respuesta Ganadora", responseOption.text))
                                                Toast.makeText(context, "Respuesta copiada", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.heightIn(min = 34.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copiar", fontSize = 10.5.sp)
                                        }

                                        // Rate button
                                        Button(
                                            onClick = {
                                                viewModel.addCustomWinningKnowledge(
                                                    category = "RESPUESTA",
                                                    triggerContext = customerInput,
                                                    content = responseOption.text,
                                                    isQuestion = false
                                                )
                                                Toast.makeText(context, "⭐ ¡Respuesta registrada en la memoria!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.heightIn(min = 34.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = AmberDelay),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("⭐ Ganadora (+1)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Recommended Closing
                    if (advice.recommendedClosing.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(IndigoPrimary.copy(alpha = 0.12f))
                                .border(1.dp, IndigoPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "🤝 Recomendación de Cierre Comercial:",
                                    color = IndigoPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = advice.recommendedClosing,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Learned Sales Memory
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Base de Memoria Aprendida",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${filteredKnowledge.size})",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { showFullKnowledgeList = !showFullKnowledgeList },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (showFullKnowledgeList) "Ocultar" else "Ver Todos",
                        color = IndigoPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 34.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual", fontSize = 10.5.sp)
                }
            }
        }

        // Filter chips for memory
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "ALL" to "Todas",
                "QUESTIONS" to "Preguntas",
                "RESPONSES" to "Respuestas"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = selectedMemoryFilter == key,
                    onClick = { selectedMemoryFilter = key },
                    label = { Text(label, fontSize = 10.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary.copy(alpha = 0.3f),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // List preview of memory items
        val displayedItems = if (showFullKnowledgeList) filteredKnowledge else filteredKnowledge.take(4)
        if (displayedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sin frases registradas aún.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                displayedItems.forEach { item ->
                    MemoryKnowledgeCard(
                        item = item,
                        onRate = {
                            viewModel.rateSalesKnowledge(item.id, 1)
                            Toast.makeText(context, "+1 punto de conversión asignado", Toast.LENGTH_SHORT).show()
                        },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Guion Aprendido", item.winningContent))
                            Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                            viewModel.deleteSalesKnowledge(item.id)
                            Toast.makeText(context, "Patrón eliminado de la memoria", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Dialog: Add Custom Winning Pitch
    if (showAddDialog) {
        var newCategory by remember { mutableStateOf("CIERRE") }
        var newTrigger by remember { mutableStateOf("") }
        var newContent by remember { mutableStateOf("") }
        var isQuestion by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text("Agregar Frase o Pregunta a la Memoria", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = !isQuestion,
                            onClick = { isQuestion = false },
                            label = { Text("Respuesta", fontSize = 10.5.sp) }
                        )
                        FilterChip(
                            selected = isQuestion,
                            onClick = { isQuestion = true },
                            label = { Text("Pregunta Persuasiva", fontSize = 10.5.sp) }
                        )
                    }

                    OutlinedTextField(
                        value = newTrigger,
                        onValueChange = { newTrigger = it },
                        label = { Text("Contexto o duda (Opcional)") },
                        placeholder = { Text("Ej: Cuando dudan del precio") },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("Texto Ganador") },
                        placeholder = { Text("Escribe la frase o pregunta que convence al cliente...") },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newContent.isNotBlank()) {
                            viewModel.addCustomWinningKnowledge(
                                category = newCategory,
                                triggerContext = newTrigger,
                                content = newContent,
                                isQuestion = isQuestion
                            )
                            showAddDialog = false
                            Toast.makeText(context, "Patrón guardado exitosamente", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Guardar en Memoria", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar", color = TextSecondary, fontSize = 11.sp)
                }
            },
            containerColor = SlateCard
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryKnowledgeCard(
    item: SalesKnowledgeItem,
    onRate: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SlateBackground)
            .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (item.isQuestion) PurpleNeon.copy(alpha = 0.2f)
                                else EmeraldSuccess.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.isQuestion) "PREGUNTA" else item.category,
                            color = if (item.isQuestion) PurpleNeon else EmeraldSuccess,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AmberDelay,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${item.successScore} pts",
                            color = AmberDelay,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = onRate,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "+1",
                            tint = AmberDelay,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = RoseError.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Text(
                text = item.winningContent,
                color = TextPrimary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (item.triggerContext.isNotBlank()) {
                Text(
                    text = "Para casos como: \"${item.triggerContext}\"",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}
