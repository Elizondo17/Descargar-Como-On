package com.example.ui.screens.psa

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PsaComparisonType
import com.example.data.model.PsaHardnessStage
import com.example.data.model.PsaPostSaleCustomer
import com.example.data.model.PsaPricingConfig
import com.example.data.model.PsaPurifierModel
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.PsaWaterBg
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.PsaCountryRegistry
import com.example.util.PsaPortalSyncManager
import java.util.Locale

// =========================================================================
// 1. CALCULADORA HIPERCONFIGURABLE: BOTELLA vs BIDON vs PSA
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaConfigurableSavingsCard(
    context: Context,
    pricingConfig: PsaPricingConfig,
    selectedPurifier: PsaPurifierModel,
    modifier: Modifier = Modifier
) {
    val activeCountry = remember(context) {
        PsaCountryRegistry.getByCode(PsaPortalSyncManager.getSelectedCountryCode(context))
    }
    var comparisonType by remember { mutableStateOf(PsaComparisonType.BIDON_20L) }
    var capacityLiters by remember { mutableDoubleStateOf(20.0) }
    var customLitersInput by remember { mutableStateOf("") }
    var showCustomLitersField by remember { mutableStateOf(false) }

    var unitPrice by remember { mutableDoubleStateOf(pricingConfig.bidonPrice) }
    var priceInput by remember { mutableStateOf(pricingConfig.bidonPrice.toLong().toString()) }

    var unitsPerWeek by remember { mutableIntStateOf(3) }

    // Quick presets
    val bottlePresets = listOf(0.5, 1.5, 2.25)
    val bidonPresets = listOf(5.0, 10.0, 12.0, 20.0)

    // Mathematical calculations
    val monthlyUnits = unitsPerWeek * 4.33
    val monthlyWaterLiters = monthlyUnits * capacityLiters
    val monthlyCost = monthlyUnits * unitPrice
    val yearlyCost = monthlyCost * 12.0
    val threeYearCost = monthlyCost * selectedPurifier.lifespanMonths.toDouble()

    val costPerLiterContainer = if (capacityLiters > 0) unitPrice / capacityLiters else 0.0
    val costPerLiterPsa = selectedPurifier.getCostPerLiter()

    val totalLifespanSavings = (threeYearCost - selectedPurifier.price).coerceAtLeast(0.0)
    val bottlesAvoidedYear = ((monthlyWaterLiters * 12.0) / 0.5).toInt() // Botellas de 500ml

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Paid,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Calculadora de Ahorro Familiar",
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Type selector
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        PsaComparisonType.BIDON_20L to "🛢️ Bidón",
                        PsaComparisonType.BOTELLA_2L to "🍾 Botella"
                    ).forEach { (type, label) ->
                        val isSelected = (comparisonType == type)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) EmeraldSuccess else Color(0xFF0F172A))
                                .border(1.dp, if (isSelected) EmeraldSuccess else SlateCardBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    comparisonType = type
                                    if (type == PsaComparisonType.BIDON_20L) {
                                        capacityLiters = 20.0
                                        unitPrice = pricingConfig.bidonPrice
                                        priceInput = pricingConfig.bidonPrice.toLong().toString()
                                    } else {
                                        capacityLiters = 2.0
                                        unitPrice = pricingConfig.bottlePrice
                                        priceInput = pricingConfig.bottlePrice.toLong().toString()
                                    }
                                    showCustomLitersField = false
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Capacity Quick Presets
            Column {
                Text(
                    text = "Capacidad del envase actual:",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = if (comparisonType == PsaComparisonType.BIDON_20L) bidonPresets else bottlePresets
                    presets.forEach { lit ->
                        val isSelected = (!showCustomLitersField && capacityLiters == lit)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) PsaCyanPrimary else Color(0xFF0F172A))
                                .border(1.dp, if (isSelected) PsaCyanGlow else SlateCardBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    capacityLiters = lit
                                    showCustomLitersField = false
                                }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${if (lit % 1.0 == 0.0) lit.toInt() else lit}L",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextPrimary
                            )
                        }
                    }

                    // Custom Liter Button
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (showCustomLitersField) PurpleNeon.copy(alpha = 0.3f) else Color(0xFF0F172A))
                            .border(1.dp, if (showCustomLitersField) PurpleNeon else SlateCardBorder, RoundedCornerShape(6.dp))
                            .clickable { showCustomLitersField = !showCustomLitersField }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✏️ Otro",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showCustomLitersField) PurpleNeon else TextSecondary
                        )
                    }
                }

                AnimatedVisibility(visible = showCustomLitersField) {
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        OutlinedTextField(
                            value = customLitersInput,
                            onValueChange = {
                                customLitersInput = it
                                val parsed = it.toDoubleOrNull()
                                if (parsed != null && parsed > 0) {
                                    capacityLiters = parsed
                                }
                            },
                            label = { Text("Escribe los litros exactos del envase", fontSize = 10.sp) },
                            placeholder = { Text("Ej: 6.5 o 25", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleNeon,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Editable Price and Consumption
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = {
                        priceInput = it
                        unitPrice = it.toDoubleOrNull() ?: 0.0
                    },
                    label = { Text("Precio por envase ($)", fontSize = 10.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldSuccess,
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Column(modifier = Modifier.weight(1.2f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Consumo:", color = TextSecondary, fontSize = 10.sp)
                        Text("$unitsPerWeek envases/sem", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Slider(
                        value = unitsPerWeek.toFloat(),
                        onValueChange = { unitsPerWeek = it.toInt() },
                        valueRange = 1f..15f,
                        steps = 13,
                        colors = SliderDefaults.colors(thumbColor = EmeraldSuccess, activeTrackColor = EmeraldSuccess)
                    )
                }
            }

            // Results Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF031E2C))
                    .border(1.dp, EmeraldSuccess.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column {
                            Text("Costo por litro envasado:", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "${activeCountry.currencySymbol}${String.format(Locale.US, "%.1f", costPerLiterContainer)} / L",
                                color = RoseError,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Column {
                            Text("Costo con Purificador PSA:", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "${activeCountry.currencySymbol}${String.format(Locale.US, "%.2f", costPerLiterPsa)} / L",
                                color = PsaCyanGlow,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(1.dp)
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Gasto anual en plástico:", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            PsaCountryRegistry.formatCurrency(yearlyCost, activeCountry),
                            color = RoseError,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Gasto en 3 años (vida útil PSA):", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            PsaCountryRegistry.formatCurrency(threeYearCost, activeCountry),
                            color = AmberDelay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Inversión ${selectedPurifier.name.split(" (").first()}:", color = PsaCyanGlow, fontSize = 11.sp)
                        Text(
                            PsaCountryRegistry.formatCurrency(selectedPurifier.price, activeCountry),
                            color = PsaCyanGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldSuccess.copy(alpha = 0.18f))
                            .border(1.dp, EmeraldSuccess.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Column {
                                Text(
                                    text = "🎉 ¡Ahorro Neto con PSA!",
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "En 3 años ahorran ${PsaCountryRegistry.formatCurrency(totalLifespanSavings, activeCountry)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Eco, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Ecología", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "$bottlesAvoidedYear botellas/año",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // WhatsApp Share Button
            val shareMessage = remember(comparisonType, capacityLiters, unitPrice, unitsPerWeek, selectedPurifier, yearlyCost, threeYearCost, totalLifespanSavings, costPerLiterContainer, costPerLiterPsa, activeCountry.code) {
                val envaseNombre = if (comparisonType == PsaComparisonType.BIDON_20L) "bidones de ${capacityLiters.toInt()}L" else "botellas de ${capacityLiters}L"
                buildString {
                    append("💧 *COMPARATIVA DE GASTO: AGUA ENVASADA vs PURIFICADOR PSA* 💧\n")
                    append("━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                    append("📊 *Consumo familiar:* $unitsPerWeek $envaseNombre por semana\n")
                    append("💵 *Precio actual del envase:* ${PsaCountryRegistry.formatCurrency(unitPrice, activeCountry)}\n\n")
                    append("❌ *GASTO EN AGUA DE PLÁSTICO:*\n")
                    append("• Por mes: ${PsaCountryRegistry.formatCurrency(monthlyCost, activeCountry)}\n")
                    append("• Por año: ${PsaCountryRegistry.formatCurrency(yearlyCost, activeCountry)}\n")
                    append("• En 3 años: ${PsaCountryRegistry.formatCurrency(threeYearCost, activeCountry)}\n")
                    append("• Costo por litro: ${activeCountry.currencySymbol}${String.format(Locale.US, "%.1f", costPerLiterContainer)} / L\n\n")
                    append("✅ *CON TU PURIFICADOR PSA (${selectedPurifier.name}):*\n")
                    append("• Inversión del equipo: ${PsaCountryRegistry.formatCurrency(selectedPurifier.price, activeCountry)} (${selectedPurifier.installmentsCount} cuotas)\n")
                    append("• Costo por litro: SOLO ${activeCountry.currencySymbol}${String.format(Locale.US, "%.2f", costPerLiterPsa)} / L\n")
                    append("• *AHORRO NETO EN 3 AÑOS:* ${PsaCountryRegistry.formatCurrency(totalLifespanSavings, activeCountry)}\n")
                    append("🌱 Evitás $bottlesAvoidedYear envases plásticos descartables.\n")
                    append("━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                    append("📲 *Asesor PSA:* Consultame por planes de entrega inmediata e instalación.")
                }
            }

            Button(
                onClick = {
                    try {
                        val uri = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(shareMessage))
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir Ahorro PSA"))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enviar Comparativa por WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
        }
    }
}

// =========================================================================
// 2. DEMOSTRADOR VISUAL DE CLORO EN TIEMPO REAL (TUBO REACTIVO & ABSORCIÓN)
// =========================================================================

data class PsaChlorineDemoStage(
    val currentColor: Color,
    val statusTitle: String,
    val statusColor: Color,
    val diagnosisText: String,
    val foodEffectText: String,
    val thmHealthText: String,
    val salesArgumentText: String,
    val absorptionPct: Int,
    val thmRiskBadge: String
)

@Composable
fun PsaChlorineInteractiveCard(
    context: Context,
    modifier: Modifier = Modifier
) {
    var chlorineLevelPpm by remember { mutableFloatStateOf(0.5f) }

    val stage = remember(chlorineLevelPpm) {
        when {
            chlorineLevelPpm <= 0.15f -> PsaChlorineDemoStage(
                currentColor = Color(0xFFE0F7FA), // Cristalino
                statusTitle = "💧 0.0 ppm - AGUA PURIFICADA PSA (CRISTALINA)",
                statusColor = EmeraldSuccess,
                diagnosisText = "Pureza molecular total. El purificador PSA retiene por adsorción en carbón activado en bloque el 100% del cloro libre, sedimentos de óxido y metales pesados.",
                foodEffectText = "Al hervir verduras, arroz, fideos o preparar café y mate, los alimentos conservan el 100% de sus nutrientes, vitaminas y sabor genuino sin absorber químicos.",
                thmHealthText = "🛡️ 0% Trihalometanos (THMs). Cero cloroformo volátil en la cocina. El agua es 100% segura y saludable para toda la familia y bebés.",
                salesArgumentText = "«Así debería salir el agua en toda la casa: pura, cristalina y sin olor a químicos. Cero gasto en bidones.»",
                absorptionPct = 0,
                thmRiskBadge = "Riesgo Nulo (Agua Pura)"
            )
            chlorineLevelPpm <= 0.65f -> PsaChlorineDemoStage(
                currentColor = Color(0xFFFFF59D), // Amarillo suave translúcido
                statusTitle = "🟡 0.5 ppm - CLORO MÍNIMO DE RED POTABLE",
                statusColor = Color(0xFFFFB300),
                diagnosisText = "Dosificación estándar de planta potabilizadora. Mantiene el agua bacteriológicamente potable en el tendido subterráneo de caños, pero deposita cloro residual en tu canilla.",
                foodEffectText = "Al cocinar fideos o arroz, los almidones absorben entre el 70% y 80% de este cloro caliente en los primeros 3 minutos de hervor.",
                thmHealthText = "⚠️ Inicio de reacción térmica: El cloro al calentarse con la materia orgánica genera los primeros Trihalometanos (THMs) y resecamiento en la piel.",
                salesArgumentText = "«El cloro es necesario para desinfectar caños subterráneos, pero no fue diseñado para que lo tomes en tu plato todos los días.»",
                absorptionPct = 75,
                thmRiskBadge = "Riesgo Moderado"
            )
            chlorineLevelPpm <= 1.25f -> PsaChlorineDemoStage(
                currentColor = Color(0xFFFFEE58), // Amarillo oro brillante
                statusTitle = "🟠 1.0 ppm - CONCENTRACIÓN ELEVADA (OLOR A LAVANDINA)",
                statusColor = Color(0xFFFF8F00),
                diagnosisText = "Sobredosificación habitual por lluvias, purgas o turbidez en tomas de río. Olor y sabor perceptible a lavandina al abrir la canilla de la cocina.",
                foodEffectText = "Modifica drásticamente el sabor de mate, café y comidas. Los alimentos absorben hasta un 85% de químicos clorados y sedimentos oxidados de la cañería.",
                thmHealthText = "☣️ Alerta Digestiva: Los subproductos clorados destruyen la flora bacteriana benéfica del estómago, provocando acidez, reflujo y gastritis recurrente.",
                salesArgumentText = "«¿Tomarías un vaso si sintieras olor a lavandina? En el mate, en la sopa y en los fideos entra exactamente esa misma cantidad de cloro.»",
                absorptionPct = 85,
                thmRiskBadge = "Riesgo Notorio"
            )
            chlorineLevelPpm <= 2.05f -> PsaChlorineDemoStage(
                currentColor = Color(0xFFFFA726), // Ámbar anaranjado profundo
                statusTitle = "🚨 2.0 ppm - NIVEL CRÍTICO DE QUÍMICOS DE RED",
                statusColor = RoseError,
                diagnosisText = "Cloración de choque aplicada por la empresa ante roturas de caños o contaminación bacteriana. El agua reacciona instantáneamente al reactivo OTO tornándose ámbar.",
                foodEffectText = "Los alimentos actúan como verdaderas esponjas químicas en la olla absorbiendo más del 90% del cloro concentrado. Hervir NO lo quita: evapora agua limpia y concentra el veneno químico.",
                thmHealthText = "🚨 Alerta Sanitaria OMS: Alta formación de cloroformo y trihalometanos (THMs), clasificados como cancerígenos acumulativos para riñones e hígado.",
                salesArgumentText = "«Hervir el agua evapora el agua pura y deja todo el químico concentrado en la comida que comen los chicos. Con PSA frenás el 100% en la canilla.»",
                absorptionPct = 90,
                thmRiskBadge = "Riesgo Crítico"
            )
            else -> PsaChlorineDemoStage(
                currentColor = Color(0xFFE64A19), // Naranja rojizo / óxido
                statusTitle = "☠️ 3.0+ ppm - CONCENTRACIÓN EXTREMA (EMERGENCIA)",
                statusColor = Color(0xFFB71C1C),
                diagnosisText = "Sobrecarga química extrema con arrastre de óxidos de hierro, plomo y sedimentos de tanques sin mantenimiento o caños de fundición viejos.",
                foodEffectText = "Absorción del 95% de toxinas químicas. Sabor metálico repugnante y decoloración evidente en la cocción de alimentos y legumbres.",
                thmHealthText = "☠️ Toxicidad Crítica Aguda: Máxima concentración de compuestos organoclorados y haloacéticos altamente tóxicos para órganos digestivos y piel.",
                salesArgumentText = "«¡Alerta Roja en el hogar! Es urgente proteger a la familia colocando una barrera purificadora PSA en la canilla de inmediato.»",
                absorptionPct = 95,
                thmRiskBadge = "Emergencia Máxima"
            )
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Test Químico de Cloro (Ortotoluidina)",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "Coloca 4 gotas de ortotoluidina en el vaso de la canilla del cliente y compara el color visual en pantalla:",
                color = TextSecondary,
                fontSize = 11.sp
            )

            // Visual Glass Tube representation with real-time reactive color
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .border(1.dp, stage.statusColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glass Tube representation
                Box(
                    modifier = Modifier
                        .size(54.dp, 78.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                        .background(stage.currentColor)
                        .border(2.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.US, "%.1f", chlorineLevelPpm),
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "ppm",
                            color = Color.Black.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stage.statusTitle,
                        color = stage.statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stage.diagnosisText,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // Preset buttons for quick demonstrations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    0.0f to "💧 PSA",
                    0.5f to "🟡 0.5",
                    1.0f to "🟠 1.0",
                    2.0f to "🚨 2.0",
                    3.0f to "☠️ 3.0"
                ).forEach { (ppm, label) ->
                    val isSelected = Math.abs(chlorineLevelPpm - ppm) < 0.25f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) PsaCyanPrimary else Color(0xFF0F172A))
                            .border(1.dp, if (isSelected) PsaCyanGlow else SlateCardBorder, RoundedCornerShape(6.dp))
                            .clickable { chlorineLevelPpm = ppm }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextSecondary
                        )
                    }
                }
            }

            Text(text = "Desliza para calibrar exactamente el tono del reactivo:", color = TextMuted, fontSize = 10.sp)
            Slider(
                value = chlorineLevelPpm,
                onValueChange = { chlorineLevelPpm = it },
                valueRange = 0.0f..3.0f,
                steps = 5,
                colors = SliderDefaults.colors(
                    thumbColor = stage.statusColor,
                    activeTrackColor = stage.statusColor,
                    inactiveTrackColor = SlateCardBorder
                )
            )

            // Dynamic Health, Cooking Absorption and Trihalomethanes (THMs) Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (chlorineLevelPpm <= 0.15f) EmeraldSuccess.copy(alpha = 0.12f)
                        else stage.statusColor.copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (chlorineLevelPpm <= 0.15f) EmeraldSuccess.copy(alpha = 0.45f)
                        else stage.statusColor.copy(alpha = 0.55f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Header with Risk Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (chlorineLevelPpm <= 0.15f) "🛡️ Análisis Sanitario: Agua Segura"
                            else "⚠️ Diagnóstico de Daño en Cocina & Salud",
                            color = stage.statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(stage.statusColor.copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stage.thmRiskBadge,
                                color = stage.statusColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Food Absorption Meter
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🍲 Absorción en alimentos al hervir:",
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                            Text(
                                text = "${stage.absorptionPct}%",
                                color = if (stage.absorptionPct == 0) EmeraldSuccess else RoseError,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { stage.absorptionPct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (stage.absorptionPct == 0) EmeraldSuccess else stage.statusColor,
                            trackColor = SlateBackground
                        )
                    }

                    // Food effect explanation
                    Text(
                        text = "• En los alimentos: ${stage.foodEffectText}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    // Health / THMs explanation
                    Text(
                        text = "• Trihalometanos (THMs) y Salud: ${stage.thmHealthText}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    // Sales Closing Argument (Quote)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "💬 Argumento de Cierre en la Mesa:",
                                color = PsaCyanGlow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stage.salesArgumentText,
                                color = TextPrimary,
                                fontSize = 10.5.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Share / Copy Report Button for Client
                    Button(
                        onClick = {
                            val reportText = "💧 *INFORME DE TEST DE AGUA PSA*\n" +
                                    "📍 Nivel detectado: ${String.format(Locale.US, "%.1f", chlorineLevelPpm)} ppm\n" +
                                    "🧪 Diagnóstico: ${stage.statusTitle}\n" +
                                    "🍲 Absorción en cocción: ${stage.absorptionPct}%\n" +
                                    "⚠️ Salud & THMs: ${stage.thmHealthText}\n\n" +
                                    "🛡️ *SOLUCIÓN:* Con el Purificador PSA eliminás el 100% del cloro, óxido y sedimentos de tu canilla para tomar y cocinar saludablemente."
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Informe PSA", reportText))
                            Toast.makeText(context, "¡Informe de cloro copiado para WhatsApp!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copiar Informe para WhatsApp", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. SIMULADOR VISUAL DE SARRO Y AGUA DURA (CAÑERÍAS & ELECTRODOMÉSTICOS)
// =========================================================================

@Composable
fun PsaHardnessSimulatorCard(
    context: Context,
    modifier: Modifier = Modifier
) {
    var hardnessPpm by remember { mutableFloatStateOf(140f) }

    val stage = remember(hardnessPpm) {
        when {
            hardnessPpm < 60f -> PsaHardnessStage(
                ppmRange = "< 60 ppm",
                title = "💧 Agua Blanda / Sin Sarro",
                hardnessCategory = "Blanda",
                icon = "🟢",
                energyOverconsumptionPct = 0,
                pipeObstructionPct = 0,
                description = "El agua fluye libremente sin depositar sales cálcicas. Cañerías y griferías impecables.",
                healthEffect = "La piel y el cabello retienen su suavidad natural. Sin irritaciones.",
                solutionText = "Ideal para consumo. Solo se recomienda purificador de cocina PSA Senior 4 para eliminar el cloro."
            )
            hardnessPpm < 120f -> PsaHardnessStage(
                ppmRange = "60 - 120 ppm",
                title = "🟡 Moderadamente Dura",
                hardnessCategory = "Moderada",
                icon = "🟡",
                energyOverconsumptionPct = 8,
                pipeObstructionPct = 15,
                description = "Aparición de manchas blancas en griferías y fondo de la pava eléctrica. Inicio de sarro.",
                healthEffect = "Ligero resecamiento en la piel después de ducharse con agua caliente.",
                solutionText = "Recomendado: Filtro PSA Ducha para proteger el baño y PSA Senior 4 en cocina."
            )
            hardnessPpm < 180f -> PsaHardnessStage(
                ppmRange = "120 - 180 ppm",
                title = "🟠 Agua Dura (Incrustaciones Frecuentes)",
                hardnessCategory = "Dura",
                icon = "🟠",
                energyOverconsumptionPct = 18,
                pipeObstructionPct = 35,
                description = "Capas gruesas de sarro en resistencias de termotanque, caldera y lavarropas. Caudal de agua reducido.",
                healthEffect = "Pelo opaco y quebradizo, jabón que no hace espuma, eccemas y piel tirante.",
                solutionText = "Recomendado: PSA Descalcificador R-200 en entrada + PSA Ducha + Purificador de Cocina."
            )
            else -> PsaHardnessStage(
                ppmRange = "> 180 ppm",
                title = "🚨 Agua Muy Dura (Sarro Crítico)",
                hardnessCategory = "Muy Dura",
                icon = "🔴",
                energyOverconsumptionPct = 32,
                pipeObstructionPct = 60,
                description = "Cañerías semiobstruidas. Caldera o termotanque consumen hasta un 32% MÁS de gas o luz para calentar.",
                healthEffect = "Daño severo en cuero cabelludo y piel reseca. Roturas reiteradas de electrodomésticos.",
                solutionText = "¡Urgente! PSA Descalcificador magnético permanente en entrada de red + equipo de cocina."
            )
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WaterDamage, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Simulador de Sarro & Cañerías",
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PsaCyanPrimary.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${hardnessPpm.toInt()} ppm (${stage.hardnessCategory})",
                        color = PsaCyanGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Desliza para simular la dureza del agua del barrio y ver su impacto en las cañerías del cliente:",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Slider(
                value = hardnessPpm,
                onValueChange = { hardnessPpm = it },
                valueRange = 30f..250f,
                steps = 7,
                colors = SliderDefaults.colors(thumbColor = PsaCyanGlow, activeTrackColor = PsaCyanPrimary)
            )

            // Dynamic Visual Pipe & Appliance Impact
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF031E2C))
                    .border(1.dp, PsaCyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${stage.icon} ${stage.title} (${stage.ppmRange})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = stage.description,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    // Impact Bars
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🧱 Obstrucción en cañerías y grifería:", color = TextSecondary, fontSize = 10.5.sp)
                            Text("${stage.pipeObstructionPct}%", color = RoseError, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        LinearProgressIndicator(
                            progress = { stage.pipeObstructionPct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (stage.pipeObstructionPct < 20) EmeraldSuccess else RoseError,
                            trackColor = SlateBackground
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("⚡ Sobreconsumo de Gas/Luz (Termotanque/Caldera):", color = TextSecondary, fontSize = 10.5.sp)
                            Text("+${stage.energyOverconsumptionPct}%", color = AmberDelay, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        LinearProgressIndicator(
                            progress = { stage.energyOverconsumptionPct / 40f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AmberDelay,
                            trackColor = SlateBackground
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("💡 Solución PSA recomendada:", color = PsaCyanGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(stage.solutionText, color = TextPrimary, fontSize = 10.5.sp, lineHeight = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. GUÍA INTERACTIVA: PRUEBA DEL REACTIVO Y ABSORCIÓN EN ALIMENTOS
// =========================================================================

@Composable
fun PsaReactiveDemoGuideCard(
    context: Context,
    modifier: Modifier = Modifier
) {
    var activeStep by remember { mutableIntStateOf(1) }

    val steps = listOf(
        Triple(
            1,
            "Vaso Testigo (Agua de Canilla)",
            "Llenar un vaso de agua directa de la canilla. Colocar 4 gotas de ortotoluidina (reactivo OTO). Al instante se torna amarillo/ámbar por el cloro libre residual y cloraminas de red."
        ),
        Triple(
            2,
            "Vaso Purificado PSA",
            "Llenar un segundo vaso con agua del purificador PSA. Colocar las mismas 4 gotas de reactivo. El agua permanece 100% transparente y cristalina, comprobando la eliminación inmediata del químico."
        ),
        Triple(
            3,
            "Prueba de Absorción en Alimentos (El Clímax)",
            "Tomar el primer vaso que estaba amarillo con cloro. Poner una rodaja de manzana o una cucharada de arroz crudo y revolver 5 segundos. ¡El agua se vuelve transparente porque el alimento absorbió todo el cloro que luego comemos!"
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Protocolo de Prueba del Reactivo",
                        color = PurpleNeon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Text(text = "Paso $activeStep de 3", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Step Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                steps.forEach { (stepNum, title, _) ->
                    val isSelected = (activeStep == stepNum)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PurpleNeon.copy(alpha = 0.25f) else Color(0xFF0F172A))
                            .border(1.dp, if (isSelected) PurpleNeon else SlateCardBorder, RoundedCornerShape(8.dp))
                            .clickable { activeStep = stepNum }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Paso $stepNum",
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val currentStep = steps.first { it.first == activeStep }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF031E2C))
                    .border(1.dp, PurpleNeon.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = currentStep.second,
                        color = PurpleNeon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = currentStep.third,
                        color = TextPrimary,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )

                    if (activeStep == 3) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(RoseError.copy(alpha = 0.15f))
                                .border(1.dp, RoseError.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "💬 Frase de Cierre: «Al hervir fideos, arroz o verduras con agua de la canilla, el agua queda limpia porque el alimento absorbió el 100% del cloro que después comemos. Con PSA lo frenás antes.»",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. CATÁLOGO OFICIAL PSA CON CERTIFICACIONES ANMAT & PRECIOS EDITABLES
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaOfficialCatalogCard(
    context: Context,
    pricingConfig: PsaPricingConfig,
    onSelectPurifier: (PsaPurifierModel) -> Unit,
    onOpenPriceEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCountry = remember(context) {
        PsaCountryRegistry.getByCode(PsaPortalSyncManager.getSelectedCountryCode(context))
    }
    val allModels = remember(pricingConfig) { pricingConfig.getAllPurifiers() }
    var selectedCategory by remember { mutableStateOf("Todas") }

    val categories = listOf("Todas", "Cocina", "Baño", "Hogar", "Entrada", "Repuestos")
    val filteredModels = remember(allModels, selectedCategory) {
        if (selectedCategory == "Todas") allModels else allModels.filter { it.category == selectedCategory }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Catálogo PSA (${activeCountry.flagEmoji} ${activeCountry.code})",
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }

                OutlinedButton(
                    onClick = onOpenPriceEditor,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = PsaCyanGlow)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar Precios", fontSize = 10.sp, color = PsaCyanGlow)
                }
            }

            // Categories horizontal chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { cat ->
                    val isSelected = (selectedCategory == cat)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 10.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PsaCyanPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = PsaCyanGlow,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = SlateCardBorder,
                            selectedBorderColor = PsaCyanGlow
                        )
                    )
                }
            }

            // Models list
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredModels.forEach { model ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF031E2C))
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = model.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )

                                Text(
                                    text = PsaCountryRegistry.formatCurrency(model.price, activeCountry),
                                    color = PsaCyanGlow,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }

                            Text(
                                text = model.description,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(EmeraldSuccess.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(model.anmatRegistration, color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "${model.installmentsCount}x ${PsaCountryRegistry.formatCurrency(model.getMonthlyInstallment(), activeCountry)}",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(model.officialUrl))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Abriendo ${model.officialUrl}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = "Ver web PSA", tint = PsaCyanGlow, modifier = Modifier.size(16.dp))
                                    }

                                    Button(
                                        onClick = { onSelectPurifier(model) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.heightIn(min = 32.dp)
                                    ) {
                                        Text("Cotizar", color = Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 5. ASESOR GUIADO: "¿CUÁL ES TU PSA IDEAL?" (DIAGNÓSTICO RÁPIDO EN 3 PASOS)
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaAdvisorQuizDialog(
    pricingConfig: PsaPricingConfig,
    onDismiss: () -> Unit,
    onSelectRecommended: (PsaPurifierModel) -> Unit
) {
    val context = LocalContext.current
    val activeCountry = remember(context) {
        PsaCountryRegistry.getByCode(PsaPortalSyncManager.getSelectedCountryCode(context))
    }
    var peopleCount by remember { mutableIntStateOf(3) }
    var locationType by remember { mutableStateOf("Cocina Sobre Mesada") }
    var mainProblem by remember { mutableStateOf("Cloro y sabor a químicos") }

    val allModels = remember(pricingConfig) { pricingConfig.getAllPurifiers() }

    val recommendedModel = remember(peopleCount, locationType, mainProblem) {
        when {
            locationType.contains("Bajo Mesada") -> allModels.find { it.id == "vero" } ?: allModels.first()
            locationType.contains("Baño") -> allModels.find { it.id == "ducha" } ?: allModels.first()
            locationType.contains("Entrada") -> allModels.find { it.id == "maxima" } ?: allModels.first()
            mainProblem.contains("Sarro") -> allModels.find { it.id == "descalcificador" } ?: allModels.first()
            peopleCount >= 5 -> allModels.find { it.id == "quantum" } ?: allModels.first()
            else -> allModels.find { it.id == "senior4" } ?: allModels.first()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, PsaCyanGlow),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Asesor Guiado: ¿Cuál es tu PSA?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // 1. Personas
                Text("1. Personas en el hogar:", color = TextSecondary, fontSize = 11.sp)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1 to "1-2 pers.", 3 to "3-4 pers.", 5 to "5+ pers.").forEach { (cnt, lbl) ->
                        val isSelected = (peopleCount == cnt)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PsaCyanPrimary else Color(0xFF0F172A))
                                .clickable { peopleCount = cnt }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(lbl, color = if (isSelected) Color.Black else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 2. Ubicación
                Text("2. Lugar de instalación:", color = TextSecondary, fontSize = 11.sp)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Sobre Mesada", "Bajo Mesada", "Baño/Ducha").forEach { loc ->
                        val isSelected = locationType.contains(loc)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PsaCyanPrimary else Color(0xFF0F172A))
                                .clickable { locationType = loc }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(loc, color = if (isSelected) Color.Black else TextSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. Problema
                Text("3. Problema principal:", color = TextSecondary, fontSize = 11.sp)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Cloro/Químicos", "Sarro/Cañerías", "Resequedad Piel").forEach { prob ->
                        val isSelected = mainProblem.contains(prob.split("/").first())
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PsaCyanPrimary else Color(0xFF0F172A))
                                .clickable { mainProblem = prob }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(prob, color = if (isSelected) Color.Black else TextSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Recommended Product Result
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF031E2C))
                        .border(1.5.dp, EmeraldSuccess, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("⭐ EQUIPO IDEAL:", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text(
                                PsaCountryRegistry.formatCurrency(recommendedModel.price, activeCountry),
                                color = PsaCyanGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text(recommendedModel.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(recommendedModel.description, color = TextSecondary, fontSize = 11.sp)
                        Text("• ${recommendedModel.highlightFeature}", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = {
                        onSelectRecommended(recommendedModel)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp)
                ) {
                    Text("Cargar en Demostrador", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// =========================================================================
// 6. MODO PRESENTACIÓN LIMPIA (PANTALLA COMPLETA PARA MOSTRAR AL CLIENTE)
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaPresentationModeDialog(
    pricingConfig: PsaPricingConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val purifiers = remember(pricingConfig) { pricingConfig.getAllPurifiers() }
    var selectedPurifier by remember { mutableStateOf(purifiers.first()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF02121E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Exit Header
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PsaCyanPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("PSA PURIFICADORES", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text("Presentación Familiar", color = PsaCyanGlow, fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, RoseError.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 34.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = RoseError, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salir", color = RoseError, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        PsaChlorineInteractiveCard(context = context)
                    }

                    item {
                        PsaHardnessSimulatorCard(context = context)
                    }

                    item {
                        PsaConfigurableSavingsCard(
                            context = context,
                            pricingConfig = pricingConfig,
                            selectedPurifier = selectedPurifier
                        )
                    }

                    item {
                        PsaReactiveDemoGuideCard(context = context)
                    }
                }
            }
        }
    }
}

// =========================================================================
// 7. POSTVENTA Y FIDELIZACIÓN: RECORDATORIOS DE RECAMBIO DE FILTRO CON 1 TOQUE
// =========================================================================

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PsaPostSaleTrackingCard(
    context: Context,
    modifier: Modifier = Modifier
) {
    var customerNameInput by remember { mutableStateOf("") }
    var customerPhoneInput by remember { mutableStateOf("") }
    var modelInstalledInput by remember { mutableStateOf("PSA Senior 4") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Seed realistic customers for demonstration
    var customerList by remember {
        mutableStateOf(
            listOf(
                PsaPostSaleCustomer(
                    id = "c1",
                    name = "Laura Martínez",
                    phone = "1145678901",
                    address = "Av. Santa Fe 3420, CABA",
                    installedModelName = "PSA Senior 4",
                    installDate = "2025-12-10",
                    monthsElapsed = 11,
                    targetRecambioMonths = 12,
                    notes = "Recambio anual de prefiltro Fipor urgente"
                ),
                PsaPostSaleCustomer(
                    id = "c2",
                    name = "Carlos Benítez",
                    phone = "1133221144",
                    address = "Calle Mitre 850, Quilmes",
                    installedModelName = "PSA Quantum",
                    installDate = "2023-11-15",
                    monthsElapsed = 35,
                    targetRecambioMonths = 36,
                    notes = "Renovación completa de núcleo por vencimiento de 3 años"
                ),
                PsaPostSaleCustomer(
                    id = "c3",
                    name = "Mariana Rossi",
                    phone = "1199887766",
                    address = "Belgrano R, CABA",
                    installedModelName = "PSA Ducha",
                    installDate = "2026-05-20",
                    monthsElapsed = 6,
                    targetRecambioMonths = 18,
                    notes = "En óptimas condiciones"
                )
            )
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = AmberDelay, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Fidelización & Recambio",
                        color = AmberDelay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberDelay),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Cliente", color = Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                customerList.forEach { customer ->
                    val isNearDue = customer.monthsElapsed >= (customer.targetRecambioMonths - 1)
                    val progress = (customer.monthsElapsed.toFloat() / customer.targetRecambioMonths.toFloat()).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF031E2C))
                            .border(
                                1.dp,
                                if (isNearDue) AmberDelay.copy(alpha = 0.7f) else SlateCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Column {
                                    Text(
                                        text = customer.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                    Text(
                                        text = "📍 ${customer.address} • ${customer.installedModelName}",
                                        color = TextSecondary,
                                        fontSize = 10.5.sp
                                    )
                                }

                                if (isNearDue) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AmberDelay.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("⚠️ ¡Recambio!", color = AmberDelay, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Uso: ${customer.monthsElapsed}/${customer.targetRecambioMonths} meses",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}% vida útil",
                                    color = if (isNearDue) AmberDelay else EmeraldSuccess,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.5.dp)),
                                color = if (isNearDue) AmberDelay else EmeraldSuccess,
                                trackColor = SlateBackground
                            )

                            val msgTemplate = remember(customer) {
                                "¡Hola ${customer.name}! Te escribo desde el servicio oficial PSA 💧.\n" +
                                        "Queríamos recordarte que tu equipo *${customer.installedModelName}* cumple ${customer.monthsElapsed} meses de uso.\n" +
                                        "Para mantener la pureza al 100% y eliminar el cloro de raíz, te recomendamos coordinar la visita de recambio de núcleo y mantenimiento.\n" +
                                        "¿Te queda cómodo que pasemos esta semana?"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            val url = "https://api.whatsapp.com/send?phone=${customer.phone}&text=" + Uri.encode(msgTemplate)
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("recambio_psa", msgTemplate))
                                            Toast.makeText(context, "Aviso copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isNearDue) AmberDelay else PsaCyanPrimary
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.heightIn(min = 32.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isNearDue) "Avisar Recambio" else "Consultar Service",
                                        color = Color.Black,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Registrar Cliente PSA Instalado", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customerNameInput,
                        onValueChange = { customerNameInput = it },
                        label = { Text("Nombre y Apellido", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customerPhoneInput,
                        onValueChange = { customerPhoneInput = it },
                        label = { Text("Teléfono de WhatsApp", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = modelInstalledInput,
                        onValueChange = { modelInstalledInput = it },
                        label = { Text("Modelo Instalado (ej: PSA Senior 4)", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customerNameInput.isNotBlank()) {
                            val newCust = PsaPostSaleCustomer(
                                id = "cust_${System.currentTimeMillis()}",
                                name = customerNameInput,
                                phone = customerPhoneInput,
                                installedModelName = modelInstalledInput,
                                installDate = "2026-11-09",
                                monthsElapsed = 1,
                                targetRecambioMonths = 12
                            )
                            customerList = customerList + newCust
                            customerNameInput = ""
                            customerPhoneInput = ""
                            showAddDialog = false
                            Toast.makeText(context, "Cliente registrado para seguimiento", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = SlateCard
        )
    }
}
