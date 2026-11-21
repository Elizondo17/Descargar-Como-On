package com.example.ui.screens.psa

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PsaPricingConfig
import com.example.data.model.PsaStockItem
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.RoseError
import com.example.ui.theme.LocalIsLargeFontMode
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.PsaCountryConfig
import com.example.util.PsaCountryRegistry
import com.example.util.PsaStockManager
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaStockTabContent(
    context: Context,
    pricingConfig: PsaPricingConfig,
    activeCountry: PsaCountryConfig = PsaCountryRegistry.defaultCountry(),
    onOpenPortalSync: () -> Unit
) {
    val isLargeFont = LocalIsLargeFontMode.current
    val currencyFormat = remember(activeCountry.code) {
        PsaCountryRegistry.currencyNumberFormat(activeCountry)
    }

    var stockItems by remember(activeCountry.code, pricingConfig) {
        mutableStateOf(PsaStockManager.getStockItems(context, pricingConfig, activeCountry.code))
    }

    var selectedFilterCategory by remember { mutableStateOf("Todos") }
    val categories = listOf("Todos", "Cocina", "Baño", "Pileta", "Aire", "Hogar", "Entrada")

    val summary = remember(stockItems) {
        PsaStockManager.calculateSummary(stockItems)
    }

    val filteredItems = remember(stockItems, selectedFilterCategory) {
        if (selectedFilterCategory == "Todos") {
            stockItems
        } else {
            stockItems.filter { it.category.equals(selectedFilterCategory, ignoreCase = true) }
        }
    }

    var editingItem by remember { mutableStateOf<PsaStockItem?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. KPI FINANCIAL DASHBOARD ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PsaCyanPrimary.copy(alpha = 0.16f))
                                    .border(1.dp, PsaCyanGlow.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = PsaCyanGlow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Inventario & Capital Invertido",
                                    color = PsaCyanGlow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${summary.totalUnits} unidades disponibles",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // WhatsApp Export Button
                        IconButton(
                            onClick = {
                                val message = generateStockWhatsAppReport(stockItems, summary, currencyFormat)
                                shareViaWhatsApp(context, message)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess.copy(alpha = 0.14f))
                                .border(1.dp, EmeraldSuccess.copy(alpha = 0.55f), CircleShape)
                                .testTag("btn_share_stock_whatsapp")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Compartir balance por WhatsApp",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Metric cards: Column stack at 140%–160%, 3-column Row on compact scale
                    if (isLargeFont) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StockKpiCard("Inversión Costo", currencyFormat.format(summary.totalInvestment), "Costo distribuidor", TextPrimary, SlateCardBorder, Modifier.fillMaxWidth())
                            val globalMargin = if (summary.totalInvestment > 0) ((summary.totalExpectedProfit / summary.totalInvestment) * 100).toInt() else 0
                            StockKpiCard("Ganancia Futura", "+${currencyFormat.format(summary.totalExpectedProfit)}", "+$globalMargin% margen est.", EmeraldSuccess, EmeraldSuccess.copy(alpha = 0.4f), Modifier.fillMaxWidth())
                            StockKpiCard("Valor en Calle", currencyFormat.format(summary.totalGrossSalesValue), "PVP Oficial (${activeCountry.code})", PsaCyanGlow, SlateCardBorder, Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StockKpiCard("Inversión Costo", currencyFormat.format(summary.totalInvestment), "Costo distribuidor", TextPrimary, SlateCardBorder, Modifier.weight(1f))
                            val globalMargin = if (summary.totalInvestment > 0) ((summary.totalExpectedProfit / summary.totalInvestment) * 100).toInt() else 0
                            StockKpiCard("Ganancia Futura", "+${currencyFormat.format(summary.totalExpectedProfit)}", "+$globalMargin% margen est.", EmeraldSuccess, EmeraldSuccess.copy(alpha = 0.4f), Modifier.weight(1f))
                            StockKpiCard("Valor en Calle", currencyFormat.format(summary.totalGrossSalesValue), "PVP Oficial (${activeCountry.code})", PsaCyanGlow, SlateCardBorder, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // --- 2. CATEGORY FILTERS ---
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = selectedFilterCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilterCategory = category },
                        label = { Text(category, fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PsaCyanPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = SlateCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) PsaCyanGlow else SlateCardBorder
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // --- 3. ITEMS LIST ---
        items(filteredItems, key = { it.id }) { item ->
            StockItemCard(
                item = item,
                currencyFormat = currencyFormat,
                onAddUnit = {
                    val updated = PsaStockManager.updateItemQuantity(context, item.id, +1, pricingConfig)
                    stockItems = updated
                },
                onRemoveUnit = {
                    val updated = PsaStockManager.updateItemQuantity(context, item.id, -1, pricingConfig)
                    stockItems = updated
                },
                onEditPrices = {
                    editingItem = item
                }
            )
        }
    }

    // Edit Item Modal Dialog
    if (editingItem != null) {
        val target = editingItem!!
        EditStockItemDialog(
            item = target,
            currencyFormat = currencyFormat,
            onDismiss = { editingItem = null },
            onSave = { newQty, newCost, newSalePrice ->
                PsaStockManager.setItemQuantity(context, target.id, newQty, pricingConfig)
                val updated = PsaStockManager.updateItemPrices(context, target.id, newCost, newSalePrice, pricingConfig)
                stockItems = updated
                editingItem = null
                Toast.makeText(context, "Valores actualizados para ${target.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StockItemCard(
    item: PsaStockItem,
    currencyFormat: NumberFormat,
    onAddUnit: () -> Unit,
    onRemoveUnit: () -> Unit,
    onEditPrices: () -> Unit
) {
    val isOutOfStock = item.quantity <= 0
    val isLowStock = item.quantity in 1..item.minStockAlert
    val isLargeFont = LocalIsLargeFontMode.current

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(
            1.dp,
            if (isOutOfStock) RoseError.copy(alpha = 0.30f) else if (isLowStock) AmberDelay.copy(alpha = 0.38f) else SlateCardBorder.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PsaCyanPrimary.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = item.category,
                                color = PsaCyanGlow,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (isOutOfStock) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RoseError.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("Sin stock", color = RoseError, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (isLowStock) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AmberDelay.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("Stock bajo (${item.quantity} u.)", color = AmberDelay, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Edit prices button
                IconButton(
                    onClick = onEditPrices,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar costo y precio",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val stepperBlock: @Composable () -> Unit = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF0A1424), RoundedCornerShape(10.dp))
                        .border(1.dp, SlateCardBorder.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = onRemoveUnit,
                        enabled = item.quantity > 0,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Restar unidad",
                            tint = if (item.quantity > 0) RoseError else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "${item.quantity} u.",
                        color = if (item.quantity > 0) PsaCyanGlow else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .clickable { onEditPrices() }
                    )

                    IconButton(
                        onClick = onAddUnit,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Sumar unidad",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            val financialColumnBlock: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Costo: ${currencyFormat.format(item.effectiveCost)}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        if (item.customCost != null) {
                            Text(" (editado)", color = PurpleNeon, fontSize = 10.sp)
                        }
                    }

                    Text(
                        text = "PVP: ${currencyFormat.format(item.effectiveSalePrice)}",
                        color = PsaCyanGlow,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Ganancia u.: +${currencyFormat.format(item.unitProfit)}",
                        color = EmeraldSuccess,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isLargeFont) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    stepperBlock()
                    financialColumnBlock()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    stepperBlock()
                    financialColumnBlock()
                }
            }

            if (item.quantity > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B1626), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Capital: ${currencyFormat.format(item.totalInvestment)}",
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                    Text(
                        text = "Ganancia est.: +${currencyFormat.format(item.totalExpectedProfit)}",
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StockKpiCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = TextMuted, fontSize = 10.5.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 9.5.sp
            )
        }
    }
}

@Composable
fun EditStockItemDialog(
    item: PsaStockItem,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit,
    onSave: (newQuantity: Int, customCost: Double?, customSalePrice: Double?) -> Unit
) {
    var quantityText by remember { mutableStateOf(item.quantity.toString()) }
    var costText by remember { mutableStateOf(if (item.customCost != null) item.customCost.toInt().toString() else "") }
    var salePriceText by remember { mutableStateOf(if (item.customSalePrice != null) item.customSalePrice.toInt().toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = {
            Column {
                Text(
                    text = "Editar Stock & Precios",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = item.name,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { char -> char.isDigit() } },
                    label = { Text("Unidades en mano", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsaCyanGlow,
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it.filter { char -> char.isDigit() } },
                    label = { Text("Costo pagado por unidad ($)", fontSize = 12.sp) },
                    placeholder = { Text("Oficial: ${currencyFormat.format(item.standardDistributorCost)}", fontSize = 11.sp, color = TextMuted) },
                    supportingText = { Text("Dejá vacío para usar el costo oficial PSA con descuento", fontSize = 10.sp, color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsaCyanGlow,
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = salePriceText,
                    onValueChange = { salePriceText = it.filter { char -> char.isDigit() } },
                    label = { Text("Precio de venta pactado ($)", fontSize = 12.sp) },
                    placeholder = { Text("Oficial PVP: ${currencyFormat.format(item.officialPvp)}", fontSize = 11.sp, color = TextMuted) },
                    supportingText = { Text("Dejá vacío para usar el PVP oficial PSA vigente", fontSize = 10.sp, color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsaCyanGlow,
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
                    val q = quantityText.toIntOrNull() ?: item.quantity
                    val c = costText.toDoubleOrNull()
                    val s = salePriceText.toDoubleOrNull()
                    onSave(q, c, s)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary)
            ) {
                Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

private fun generateStockWhatsAppReport(
    items: List<PsaStockItem>,
    summary: com.example.data.model.PsaStockSummary,
    currencyFormat: NumberFormat
): String {
    val sb = StringBuilder()
    sb.append("📦 *BALANCE DE STOCK E INVERSIÓN PSA*\n")
    sb.append("═══════════════════════════════\n\n")

    val activeItems = items.filter { it.quantity > 0 }
    if (activeItems.isEmpty()) {
        sb.append("⚠️ Sin unidades cargadas en stock actualmente.\n\n")
    } else {
        activeItems.forEach { item ->
            sb.append("🔹 *${item.name}*: ${item.quantity} u.\n")
            sb.append("   • Costo u: ${currencyFormat.format(item.effectiveCost)} | Venta: ${currencyFormat.format(item.effectiveSalePrice)}\n")
            sb.append("   • Capital: ${currencyFormat.format(item.totalInvestment)} | Ganancia: +${currencyFormat.format(item.totalExpectedProfit)}\n\n")
        }
    }

    sb.append("═══════════════════════════════\n")
    sb.append("📊 *RESUMEN CONSOLIDADO:*\n")
    sb.append("• *Unidades totales:* ${summary.totalUnits}\n")
    sb.append("• *Capital invertido:* ${currencyFormat.format(summary.totalInvestment)}\n")
    sb.append("• *Valor de venta proyectado:* ${currencyFormat.format(summary.totalGrossSalesValue)}\n")
    sb.append("• *Ganancia bruta esperada:* +${currencyFormat.format(summary.totalExpectedProfit)}\n")
    sb.append("═══════════════════════════════\n")
    sb.append("Generado automáticamente con Como On • Espacio PSA")

    return sb.toString()
}

private fun shareViaWhatsApp(context: Context, message: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(message))
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir balance de stock"))
    }
}
