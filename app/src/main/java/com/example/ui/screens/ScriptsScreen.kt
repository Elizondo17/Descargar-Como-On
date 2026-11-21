package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScriptFolder
import com.example.data.model.ScriptItem
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
fun ScriptsScreen(
    viewModel: MainViewModel,
    onSelectScriptForAutomation: (ScriptItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val folders by viewModel.allFolders.collectAsState()
    val allScripts by viewModel.allScripts.collectAsState()

    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateScriptDialog by remember { mutableStateOf(false) }

    // Folder creation form state
    var newFolderName by remember { mutableStateOf("") }
    var newFolderDesc by remember { mutableStateOf("") }

    // Script creation form state
    var newScriptTitle by remember { mutableStateOf("") }
    var newScriptContent by remember { mutableStateOf("") }
    var isAutoResponse by remember { mutableStateOf(false) }
    var triggerKeyword by remember { mutableStateOf("") }

    val activeFolderId = selectedFolderId ?: folders.firstOrNull()?.id ?: 0L
    val scriptsInActiveFolder = allScripts.filter { it.folderId == activeFolderId }

    Box(modifier = modifier.fillMaxSize().background(SlateBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scripts y Carpetas",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Guiones por intención",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = { showCreateFolderDialog = true },
                    modifier = Modifier.testTag("add_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "Nueva Carpeta",
                        tint = IndigoPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Folder Tabs Horizontal Scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                folders.forEach { folder ->
                    val isSelected = folder.id == activeFolderId
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFolderId = folder.id },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) Color.White else TextSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(folder.name)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = SlateCard,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scripts list
            if (scriptsInActiveFolder.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay scripts en esta carpeta.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Pulsa el botón '+' para agregar el primer script.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(scriptsInActiveFolder) { script ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SlateCard)
                                .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = script.title,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (script.isAutoResponse) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(EmeraldSuccess.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.QuestionAnswer,
                                                    contentDescription = null,
                                                    tint = EmeraldSuccess,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Auto-Respuesta: '${script.triggerKeyword}'",
                                                    color = EmeraldSuccess,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteScript(script) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar script",
                                            tint = RoseError.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = script.content,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Usos: ${script.usageCount}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                    Button(
                                        onClick = { onSelectScriptForAutomation(script) },
                                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        modifier = Modifier.heightIn(min = 38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cargar en Campaña", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB to add new script
        FloatingActionButton(
            onClick = { showCreateScriptDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_script_fab"),
            containerColor = IndigoPrimary,
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir Script")
        }
    }

    // Dialog: Create Folder
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            containerColor = SlateCard,
            title = { Text("Nueva Carpeta de Scripts", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ejemplos: Levantamiento, Descuento, Levantamiento de Descuentos",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "Sugerencias de categorías:",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Lecturas y Tiradas", "Limpiezas", "Lunas y Fechas", "Turnos y Pagos", "Reactivación").forEach { cat ->
                            OutlinedButton(
                                onClick = { newFolderName = cat },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 34.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleNeon)
                            ) {
                                Text(cat, fontSize = 10.5.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Nombre de Carpeta / Categoría") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newFolderDesc,
                        onValueChange = { newFolderDesc = it },
                        label = { Text("Descripción (opcional)") },
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
                        if (newFolderName.isNotBlank()) {
                            viewModel.addFolder(newFolderName, newFolderDesc, "#6366F1")
                            newFolderName = ""
                            newFolderDesc = ""
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Crear Carpeta")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Create Script
    if (showCreateScriptDialog) {
        AlertDialog(
            onDismissRequest = { showCreateScriptDialog = false },
            containerColor = SlateCard,
            title = { Text("Nuevo Script de Mensaje", color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newScriptTitle,
                        onValueChange = { newScriptTitle = it },
                        label = { Text("Título del Script") },
                        placeholder = { Text("Ej: Saludo de Levantamiento") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick variable insertion bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Variables rápidas (toca para insertar):",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { newScriptContent += "{afecto} " },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 34.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleNeon)
                            ) {
                                Text("✨ {afecto}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { newScriptContent += "{nombre} " },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 34.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = IndigoPrimary)
                            ) {
                                Text("+ {nombre}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { newScriptContent += "{saludo} " },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 34.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess)
                            ) {
                                Text("+ {saludo}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { newScriptContent += "{Hola|Buen día|Hola cariño} " },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 34.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberDelay)
                            ) {
                                Text("🔀 Spintax", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newScriptContent,
                        onValueChange = { newScriptContent = it },
                        label = { Text("Contenido del Mensaje") },
                        placeholder = { Text("Ej: {Hola|Buen día} {afecto} {nombre}, ¿cómo estás?") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dynamic Live Preview Box
                    if (newScriptContent.isNotBlank()) {
                        var previewRandomSeed by remember { mutableStateOf(0) }
                        val previewText = remember(newScriptContent, previewRandomSeed) {
                            com.example.util.TextTemplateParser.process(
                                template = newScriptContent,
                                contactName = "María",
                                contactPhone = "+54 9 11 2345-6789"
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateBackground.copy(alpha = 0.7f))
                                .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Vista Previa Dinámica:",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "Probar variación ⟳",
                                        color = PurpleNeon,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { previewRandomSeed++ }
                                    )
                                }
                                Text(
                                    text = previewText,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "¿Es respuesta automática?", color = TextPrimary, fontSize = 13.sp)
                            Text(text = "Responde si el chat entrante contiene una palabra clave", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isAutoResponse,
                            onCheckedChange = { isAutoResponse = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = IndigoPrimary)
                        )
                    }

                    if (isAutoResponse) {
                        OutlinedTextField(
                            value = triggerKeyword,
                            onValueChange = { triggerKeyword = it },
                            label = { Text("Palabra clave detonante") },
                            placeholder = { Text("Ej: precio, cotización, información") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newScriptTitle.isNotBlank() && newScriptContent.isNotBlank()) {
                            viewModel.addScript(
                                folderId = activeFolderId,
                                title = newScriptTitle,
                                content = newScriptContent,
                                isAutoResponse = isAutoResponse,
                                triggerKeyword = triggerKeyword
                            )
                            newScriptTitle = ""
                            newScriptContent = ""
                            isAutoResponse = false
                            triggerKeyword = ""
                            showCreateScriptDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Guardar Script")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateScriptDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}
