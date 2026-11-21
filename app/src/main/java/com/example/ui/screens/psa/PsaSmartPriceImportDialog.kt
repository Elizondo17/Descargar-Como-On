package com.example.ui.screens.psa

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PsaPricingConfig
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.PsaCountryConfig
import com.example.util.PsaCountryRegistry
import com.example.util.PsaPdfTextExtractor
import com.example.util.PsaPriceTextParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaSmartPriceImportDialog(
    context: Context,
    currentConfig: PsaPricingConfig,
    activeCountry: PsaCountryConfig = PsaCountryRegistry.defaultCountry(),
    onPricesApplied: (PsaPricingConfig, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var rawInputText by remember { mutableStateOf("") }
    val currencyFormat = remember(activeCountry.code) {
        PsaCountryRegistry.currencyNumberFormat(activeCountry)
    }

    val parsedResult = remember(rawInputText, currentConfig, activeCountry.code) {
        PsaPriceTextParser.parseText(rawInputText, currentConfig, activeCountry.code)
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val extracted = withContext(Dispatchers.IO) {
                    PsaPdfTextExtractor.extractTextFromUri(context, uri)
                }
                if (extracted.isNotBlank()) {
                    rawInputText = extracted
                } else {
                    Toast.makeText(context, "El PDF no tiene texto seleccionable", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF010F1A),
            border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Importar PVP (${activeCountry.flag} ${activeCountry.code})",
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    rawInputText = text
                                } else {
                                    Toast.makeText(context, "Portapapeles vacío", Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("btn_paste_psa_clipboard")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pegar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("btn_open_official_psa_app")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AmberDelay, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Abrir PDF", color = AmberDelay, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = rawInputText,
                    onValueChange = { rawInputText = it },
                    placeholder = {
                        Text(
                            text = "Pega aquí la lista oficial...",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 84.dp, max = 120.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF031625),
                        unfocusedContainerColor = Color(0xFF02121F),
                        focusedBorderColor = PsaCyanGlow,
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(parsedResult.items, key = { it.modelKey }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateCard)
                                .border(1.dp, PsaCyanGlow.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.modelName,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = currencyFormat.format(item.detectedPrice),
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SlateCardBorder),
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text("Cancelar", color = TextSecondary, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val updatedConfig = PsaPriceTextParser.applyToPricingConfig(currentConfig, parsedResult)
                            onPricesApplied(updatedConfig, parsedResult.items.size)
                            Toast.makeText(context, "${parsedResult.items.size} precios PVP aplicados", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        enabled = parsedResult.items.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            disabledContainerColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("btn_confirm_apply_parsed_prices")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Confirmar PVP (${parsedResult.items.size})",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }
    }
}
