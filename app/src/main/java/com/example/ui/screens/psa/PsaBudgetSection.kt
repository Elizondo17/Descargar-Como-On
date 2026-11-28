package com.example.ui.screens.psa

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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PsaPricingConfig
import com.example.data.model.PsaPurifierModel
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalIsLargeFontMode
import com.example.ui.theme.PsaBlueDark
import com.example.ui.theme.PsaCardBorder
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.PsaWaterBg
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.PsaCountryConfig
import com.example.util.PsaCountryRegistry
import java.text.NumberFormat
import java.util.Locale

import androidx.compose.runtime.LaunchedEffect
import com.example.util.DemoBudgetPaymentMode
import com.example.util.DemoToBudgetHandoff
import com.example.util.PsaDemoAiContext

data class PurifierOption(
    val id: String,
    val name: String,
    val basePrice: Double,
    val isCustom: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PsaBudgetTabContent(
    context: Context,
    viewModel: MainViewModel? = null,
    pricingConfig: PsaPricingConfig,
    activeCountry: PsaCountryConfig = PsaCountryRegistry.defaultCountry(),
    initialHandoff: DemoToBudgetHandoff? = null,
    onRegisterPostSale: ((customerName: String, purifierId: String, purifierName: String, cycleMonths: Int) -> Unit)? = null,
    onOpenPricingSettings: () -> Unit,
    onOpenAiAssistant: (() -> Unit)? = null,
    onAiContextChanged: ((PsaDemoAiContext) -> Unit)? = null
) {
    val financingConfig = viewModel?.psaFinancingConfig?.collectAsState()?.value
        ?: com.example.data.repository.PsaFinancingConfig()
    val isLargeFont = LocalIsLargeFontMode.current
    val currencyFormat = remember(activeCountry.code) {
        PsaCountryRegistry.currencyNumberFormat(activeCountry)
    }

    // Build purifiers list from full official PSA catalog
    val purifierOptions = remember(pricingConfig) {
        pricingConfig.getAllPurifiers().map { model ->
            PurifierOption(model.id, model.name, model.price, isCustom = model.category == "Personalizado")
        }
    }

    var cartQuantities by remember(purifierOptions) {
        mutableStateOf(mapOf(purifierOptions.first().id to 1))
    }
    var isCartAccordionExpanded by remember { mutableStateOf(false) }

    var clientName by remember { mutableStateOf("") }
    var extraNotes by remember { mutableStateOf("") }
    var extraCostText by remember { mutableStateOf("") }
    var advanceText by remember { mutableStateOf("") }
    var installmentsCount by remember { mutableIntStateOf(6) } // 1, 3, 6, 12, 18
    var customMonthsInput by remember { mutableStateOf("") }
    var customInterestPercentText by remember { mutableStateOf("") }
    var cashDiscountPercentText by remember { mutableStateOf("0") }
    var showPrivateProfit by remember { mutableStateOf(false) }
    var weeklyBidonesInput by remember { mutableIntStateOf(2) }
    var activeHandoffBannerMode by remember { mutableStateOf<DemoBudgetPaymentMode?>(null) }
    var showPostSaleDialog by remember { mutableStateOf(false) }
    var postSaleClientInput by remember { mutableStateOf("") }
    var postSaleCycleMonths by remember { mutableIntStateOf(12) }

    LaunchedEffect(initialHandoff?.requestSeq, purifierOptions) {
        if (initialHandoff != null) {
            val validIds = purifierOptions.map { it.id }.toSet()
            val incomingMap = initialHandoff.items
                .filter { it.purifierId in validIds && it.quantity > 0 }
                .associate { it.purifierId to it.quantity.coerceIn(1, 10) }
                .ifEmpty {
                    val fallbackId = if (initialHandoff.purifierId in validIds) {
                        initialHandoff.purifierId
                    } else {
                        purifierOptions.first().id
                    }
                    mapOf(fallbackId to 1)
                }
            cartQuantities = incomingMap
            activeHandoffBannerMode = initialHandoff.paymentMode
            when (initialHandoff.paymentMode) {
                DemoBudgetPaymentMode.CASH -> {
                    installmentsCount = 1
                    customInterestPercentText = "0"
                }
                DemoBudgetPaymentMode.CARD -> {
                    installmentsCount = 6
                    customInterestPercentText = "0"
                }
            }
        }
    }

    val cartLines = remember(purifierOptions, cartQuantities) {
        purifierOptions.mapNotNull { opt ->
            val qty = cartQuantities[opt.id] ?: 0
            if (qty > 0) opt to qty else null
        }
    }
    val totalUnitsCount = cartLines.sumOf { it.second }
    val cartSummaryTitle = remember(cartLines, totalUnitsCount) {
        when {
            cartLines.isEmpty() -> "Sin equipos seleccionados"
            cartLines.size == 1 && totalUnitsCount == 1 -> cartLines.first().first.name
            else -> cartLines.joinToString(" + ") { (opt, qty) -> "${qty}× ${opt.name.replace("PSA ", "")}" }
        }
    }

    // Resolve default interest for selected installment term
    val currentInterestPercent = remember(installmentsCount, financingConfig, customInterestPercentText) {
        if (customInterestPercentText.isNotBlank()) {
            customInterestPercentText.toFloatOrNull() ?: 0f
        } else {
            when (installmentsCount) {
                1 -> 0f
                3 -> financingConfig.defaultInterest3Months
                6 -> financingConfig.defaultInterest6Months
                12 -> financingConfig.defaultInterest12Months
                18 -> financingConfig.defaultInterest18Months
                else -> 20f
            }
        }
    }

    // Mathematical calculations
    val basePrice = cartLines.sumOf { (opt, qty) -> opt.basePrice * qty }
    val extraCost = extraCostText.toDoubleOrNull() ?: 0.0
    val totalGrossPrice = basePrice + extraCost

    // Distributor private net profit
    val distributorCost = basePrice * 0.60 + extraCost
    val netProfit = totalGrossPrice - distributorCost

    // ROI Amortization calculations
    val bidonPrice = pricingConfig.bidonPrice.coerceAtLeast(1000.0)
    val monthlyBidonExpense = (weeklyBidonesInput * 4.33 * bidonPrice).coerceAtLeast(1000.0)
    val monthsToAmortize = ((totalGrossPrice / monthlyBidonExpense).coerceAtLeast(1.0)).toInt().coerceIn(1, 36)
    val remainingFreeMonths = (36 - monthsToAmortize).coerceAtLeast(0)
    val total3YearBidonExpense = monthlyBidonExpense * 36
    val total3YearNetSavings = (total3YearBidonExpense - totalGrossPrice).coerceAtLeast(0.0)

    LaunchedEffect(cartLines, clientName, monthlyBidonExpense, cartSummaryTitle) {
        val wellIds = setOf("ropot", "rotech", "domus", "descalcificador")
        val ids = cartLines.map { it.first.id }
        val inferredSource = if (ids.any { it in wellIds }) "AGUA_DE_POZO" else "AGUA_CORRIENTE"
        onAiContextChanged?.invoke(
            PsaDemoAiContext(
                sourceOrigin = "Cotizador",
                waterSource = inferredSource,
                annualBottledSpend = monthlyBidonExpense * 12.0,
                cartModelIds = ids,
                cartSummary = cartSummaryTitle,
                clientName = clientName.trim()
            )
        )
    }

    val cashDiscountPercent = cashDiscountPercentText.toDoubleOrNull() ?: 0.0
    val cashDiscountAmount = totalGrossPrice * (cashDiscountPercent / 100.0)
    val cashTotalWithDiscount = (totalGrossPrice - cashDiscountAmount).coerceAtLeast(0.0)

    val advancePaid = remember(advanceText, totalGrossPrice) {
        val input = advanceText.toDoubleOrNull() ?: 0.0
        input.coerceIn(0.0, totalGrossPrice)
    }

    val financedBalance = (totalGrossPrice - advancePaid).coerceAtLeast(0.0)
    val interestAmount = if (installmentsCount > 1) {
        financedBalance * (currentInterestPercent / 100.0)
    } else {
        0.0
    }
    val totalFinanced = financedBalance + interestAmount
    val monthlyPayment = if (installmentsCount > 0) {
        totalFinanced / installmentsCount.toDouble()
    } else {
        totalFinanced
    }
    val grandTotal = advancePaid + totalFinanced

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("psa_budget_lazy_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Header Title Card ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = PsaCyanGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Cotizador (${activeCountry.code})",
                                    color = PsaCyanGlow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (showPrivateProfit) "Modo Ganancia Activo" else "Contado, anticipo y cuotas",
                                    color = if (showPrivateProfit) EmeraldSuccess else TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onOpenAiAssistant != null) {
                                OutlinedButton(
                                    onClick = onOpenAiAssistant,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.7f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("budget_open_psa_ai_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "Asistente IA PSA",
                                        tint = PsaCyanGlow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("IA PSA", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            IconButton(
                                onClick = { showPrivateProfit = !showPrivateProfit },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (showPrivateProfit) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Ganancia DIP privada",
                                    tint = if (showPrivateProfit) EmeraldSuccess else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (activeHandoffBannerMode != null) {
            item {
                val isCard = activeHandoffBannerMode == DemoBudgetPaymentMode.CARD
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCard) Color(0xFF1A1033) else Color(0xFF0A241B)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isCard) PurpleNeon.copy(alpha = 0.65f) else EmeraldSuccess.copy(alpha = 0.65f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("budget_handoff_banner")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isCard) {
                                "💳 Financiación con Tarjeta — $cartSummaryTitle"
                            } else {
                                "💵 Presupuesto en Efectivo — $cartSummaryTitle"
                            },
                            color = if (isCard) PurpleNeon else EmeraldSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isCard) {
                                "Las cuotas sin interés dependen del banco emisor y la tarjeta del cliente. Ajusta aquí el plan de cuotas y el recargo financiero aplicable."
                            } else if (cashDiscountPercent > 0) {
                                "Contado efectivo con ${cashDiscountPercent.toInt()}% OFF aplicado: ${currencyFormat.format(cashTotalWithDiscount)} (o personaliza adelanto y cuotas abajo)."
                            } else {
                                "Contado efectivo (precio de lista): ${currencyFormat.format(cashTotalWithDiscount)} (aplica un descuento abajo solo si lo deseas)."
                            },
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // --- MODO PRIVADO: TARJETA DE GANANCIA NETA DIP ---
        if (showPrivateProfit) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A241B)),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Paid, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tu Ganancia Limpia (DIP)", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                            Text(
                                text = "Costo distribuidor: ${currencyFormat.format(distributorCost)} (40%)",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = "+${currencyFormat.format(netProfit)}",
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                    }
                }
            }
        }

        // --- 1. CARRITO PLEGABLE DE EQUIPOS PSA ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateCardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. EQUIPOS PSA EN COTIZACIÓN ($totalUnitsCount)",
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0A1424))
                            .border(
                                1.dp,
                                if (isCartAccordionExpanded || cartLines.isNotEmpty()) PsaCyanGlow.copy(alpha = 0.55f) else SlateCardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { isCartAccordionExpanded = !isCartAccordionExpanded }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("budget_cart_accordion_toggle"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Carrito de equipos PSA",
                            tint = PsaCyanGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cartSummaryTitle,
                                color = if (cartLines.isEmpty()) TextSecondary else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Subtotal equipos: ${currencyFormat.format(basePrice)}",
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            )
                        }
                        Icon(
                            imageVector = if (isCartAccordionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isCartAccordionExpanded) "Plegar lista" else "Desplegar equipos",
                            tint = PsaCyanGlow
                        )
                    }

                    AnimatedVisibility(visible = isCartAccordionExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF091220))
                                .border(1.dp, SlateCardBorder, RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            purifierOptions.forEach { option ->
                                val qty = cartQuantities[option.id] ?: 0
                                val isChecked = qty > 0
                                val toggleOption: () -> Unit = {
                                    cartQuantities = if (isChecked) {
                                        cartQuantities - option.id
                                    } else {
                                        cartQuantities + (option.id to 1)
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isChecked) PsaCyanPrimary.copy(alpha = 0.12f) else SlateCard)
                                        .border(
                                            1.dp,
                                            if (isChecked) PsaCyanPrimary.copy(alpha = 0.55f) else SlateCardBorder.copy(alpha = 0.5f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable(onClick = toggleOption)
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("budget_cart_row_${option.id}"),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { toggleOption() },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = PsaCyanPrimary,
                                                uncheckedColor = TextSecondary,
                                                checkmarkColor = Color.Black
                                            ),
                                            modifier = Modifier.testTag("budget_cart_check_${option.id}")
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = option.name,
                                                color = if (isChecked) PsaCyanGlow else TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = if (qty > 1) {
                                                    "${currencyFormat.format(option.basePrice)} c/u • ${currencyFormat.format(option.basePrice * qty)}"
                                                } else {
                                                    currencyFormat.format(option.basePrice)
                                                },
                                                color = if (isChecked) EmeraldSuccess else TextSecondary,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                        if (isChecked) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF0A1424))
                                                    .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        val next = qty - 1
                                                        cartQuantities = if (next <= 0) cartQuantities - option.id else cartQuantities + (option.id to next)
                                                    },
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .testTag("budget_cart_minus_${option.id}")
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Restar ${option.name}", tint = PsaCyanGlow, modifier = Modifier.size(16.dp))
                                                }
                                                Text(
                                                    text = "$qty",
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val next = (qty + 1).coerceAtMost(10)
                                                        cartQuantities = cartQuantities + (option.id to next)
                                                    },
                                                    enabled = qty < 10,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .testTag("budget_cart_plus_${option.id}")
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Sumar ${option.name}", tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (cartLines.isNotEmpty() && !isCartAccordionExpanded) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            cartLines.forEach { (option, qty) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PsaCyanPrimary.copy(alpha = 0.14f))
                                        .border(1.dp, PsaCyanPrimary.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${qty}× ${option.name.replace("PSA ", "")} (${currencyFormat.format(option.basePrice * qty)})",
                                        color = PsaCyanGlow,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }

                    // Optional Client Name
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nombre del Cliente (Opcional)", fontSize = 11.sp) },
                        placeholder = { Text("Ej. Familia Gómez / Juan Pérez", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Optional Extra Amount (Repuesto, Instalación)
                    OutlinedTextField(
                        value = extraCostText,
                        onValueChange = { extraCostText = it },
                        label = { Text("Cargos Adicionales / Extras en $ (Opcional)", fontSize = 11.sp) },
                        placeholder = { Text("Ej. 15000 (Instalación especial / Filtro extra)", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // --- 2. ADELANTO EN EFECTIVO / SEÑA ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateCardBorder),
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
                        Text(
                            text = "2. ADELANTO / SEÑA EN EFECTIVO",
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.5.sp
                        )

                        if (advancePaid > 0) {
                            Text(
                                text = "Entregado: ${currencyFormat.format(advancePaid)}",
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = advanceText,
                        onValueChange = { advanceText = it },
                        label = { Text("Monto de adelanto entregado ($)", fontSize = 11.5.sp) },
                        placeholder = { Text("Ej. 50000", fontSize = 11.5.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldSuccess,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Quick percentage suggestion buttons (FlowRow)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "Sin adelanto", 10 to "10%", 20 to "20%", 30 to "30%").forEach { (pct, label) ->
                            OutlinedButton(
                                onClick = {
                                    val calc = if (pct == 0) 0 else ((totalGrossPrice * pct) / 100).toLong()
                                    advanceText = if (calc == 0L) "" else calc.toString()
                                },
                                modifier = Modifier.heightIn(min = 44.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (pct > 0) EmeraldSuccess.copy(alpha = 0.45f) else SlateCardBorder)
                            ) {
                                Text(label, fontSize = 11.5.sp, color = if (pct > 0) EmeraldSuccess else TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // --- 3. PLAN DE CUOTAS Y RECARGOS ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateCardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "3. FINANCIACIÓN Y MESES EN CUOTAS",
                        color = PurpleNeon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )

                    // Term chips (FlowRow)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            1 to "Contado",
                            3 to "3 Cuotas",
                            6 to "6 Cuotas",
                            12 to "12 Cuotas",
                            18 to "18 Cuotas"
                        ).forEach { (months, label) ->
                            val isSelected = installmentsCount == months
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    installmentsCount = months
                                    customInterestPercentText = ""
                                },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurpleNeon.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.Black.copy(alpha = 0.3f),
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = SlateCardBorder,
                                    selectedBorderColor = PurpleNeon
                                )
                            )
                        }
                    }

                    // Editable Interest Percentage & Installment Count (stacks cleanly at 140%–160%)
                    if (isLargeFont) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = if (customInterestPercentText.isNotEmpty()) customInterestPercentText else currentInterestPercent.toInt().toString(),
                                onValueChange = { customInterestPercentText = it },
                                label = { Text("Recargo financiero (%)", fontSize = 11.5.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurpleNeon,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = installmentsCount.toString(),
                                onValueChange = {
                                    val n = it.toIntOrNull()
                                    if (n != null && n > 0) installmentsCount = n
                                },
                                label = { Text("Cant. Cuotas (Meses)", fontSize = 11.5.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurpleNeon,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = if (customInterestPercentText.isNotEmpty()) customInterestPercentText else currentInterestPercent.toInt().toString(),
                                onValueChange = { customInterestPercentText = it },
                                label = { Text("Recargo financiero (%)", fontSize = 10.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurpleNeon,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = installmentsCount.toString(),
                                onValueChange = {
                                    val n = it.toIntOrNull()
                                    if (n != null && n > 0) installmentsCount = n
                                },
                                label = { Text("Cant. Cuotas (Meses)", fontSize = 10.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurpleNeon,
                                    unfocusedBorderColor = SlateCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Quick Recargo % suggestions (FlowRow)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "0% (S/Int)", 10 to "10%", 15 to "15%", 20 to "20%", 25 to "25%").forEach { (pct, label) ->
                            val isSelected = currentInterestPercent.toInt() == pct
                            OutlinedButton(
                                onClick = { customInterestPercentText = pct.toString() },
                                modifier = Modifier.heightIn(min = 44.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSelected) PurpleNeon else SlateCardBorder)
                            ) {
                                Text(label, fontSize = 11.5.sp, color = if (isSelected) PurpleNeon else TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // --- OPCIÓN DE CONTADO EN EFECTIVO CON DESCUENTO ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateCardBorder),
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
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CONTADO • DESCUENTO EFECTIVO",
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "Total: ${currencyFormat.format(cashTotalWithDiscount)}",
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.testTag("budget_cash_total_value")
                        )
                    }

                    OutlinedTextField(
                        value = cashDiscountPercentText,
                        onValueChange = { cashDiscountPercentText = it.filter { c -> c.isDigit() } },
                        label = { Text("Descuento contado (%)", fontSize = 10.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_cash_discount_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldSuccess,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "0%", 5 to "5%", 10 to "10%", 15 to "15%").forEach { (pct, label) ->
                            val isSelected = cashDiscountPercent.toInt() == pct
                            OutlinedButton(
                                onClick = { cashDiscountPercentText = pct.toString() },
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("budget_cash_discount_chip_$pct"),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSelected) EmeraldSuccess else SlateCardBorder)
                            ) {
                                Text(label, fontSize = 11.5.sp, color = if (isSelected) EmeraldSuccess else TextSecondary)
                            }
                        }
                    }

                    if (cashDiscountAmount > 0) {
                        Text(
                            text = "Ahorro directo para el cliente en un solo pago: ${currencyFormat.format(cashDiscountAmount)}",
                            color = EmeraldSuccess,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }

        // --- 4. CALCULADOR GRÁFICO DE AMORTIZACIÓN (ROI EN MESES) ---
        item {
            Card(
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
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AMORTIZACIÓN VS BIDONES",
                                color = PsaCyanGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EmeraldSuccess.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Se paga en $monthsToAmortize meses",
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Stepper for weekly bidones (stacks cleanly at 140%–160%)
                    val bidonSummaryBlock: @Composable () -> Unit = {
                        Column {
                            Text("Consumo semanal en bidones:", color = TextSecondary, fontSize = 11.5.sp)
                            Text("Gasto actual: ${currencyFormat.format(monthlyBidonExpense)}/mes", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                    val bidonStepperBlock: @Composable () -> Unit = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFF0A1424), RoundedCornerShape(10.dp))
                                .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { if (weeklyBidonesInput > 1) weeklyBidonesInput-- },
                                enabled = weeklyBidonesInput > 1,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Menos", tint = if (weeklyBidonesInput > 1) PsaCyanGlow else TextMuted, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "$weeklyBidonesInput bid/sem",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { weeklyBidonesInput++ },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Más", tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (isLargeFont) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            bidonSummaryBlock()
                            bidonStepperBlock()
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            bidonSummaryBlock()
                            bidonStepperBlock()
                        }
                    }

                    // Graphical Amortization Progress Bar
                    val progressRatio = (monthsToAmortize / 36f).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("0 meses", color = TextMuted, fontSize = 10.sp)
                            Text("Mes $monthsToAmortize (Amortizado)", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                            Text("36 meses", color = TextMuted, fontSize = 10.sp)
                        }

                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EmeraldSuccess,
                            trackColor = Color(0xFF0A1424)
                        )
                    }

                    // Closing Argument Highlight Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B1729))
                            .border(1.dp, PsaCyanGlow.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "💡 En vez de pagar ${currencyFormat.format(monthlyBidonExpense)} por mes en bidones para siempre, el purificador se paga solo en $monthsToAmortize meses. Los siguientes $remainingFreeMonths meses ahorrás ${currencyFormat.format(total3YearNetSavings)}.",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // --- 5. RESUMEN FINAL Y VALOR DE LA CUOTA ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "RESUMEN DEL PRESUPUESTO",
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )

                    // Breakdown lines (stacks label over value at 140%–160%)
                    @Composable
                    fun breakdownRow(label: String, value: String, labelColor: Color = TextSecondary, valueColor: Color = TextPrimary, boldValue: Boolean = false) {
                        if (isLargeFont) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(label, color = labelColor, fontSize = 12.sp)
                                Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, color = labelColor, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(value, color = valueColor, fontWeight = if (boldValue) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (cartLines.isEmpty()) {
                            breakdownRow("Equipos:", "Sin equipos seleccionados")
                        } else {
                            cartLines.forEach { (option, qty) ->
                                breakdownRow(
                                    label = if (qty > 1) "${qty}× ${option.name}:" else "Equipo (${option.name}):",
                                    value = currencyFormat.format(option.basePrice * qty)
                                )
                            }
                            if (cartLines.size > 1 || totalUnitsCount > 1) {
                                breakdownRow(
                                    label = "Subtotal Equipos ($totalUnitsCount):",
                                    value = currencyFormat.format(basePrice),
                                    boldValue = true
                                )
                            }
                        }
                        if (extraCost > 0) {
                            breakdownRow("Cargos adicionales:", currencyFormat.format(extraCost))
                        }
                        if (advancePaid > 0) {
                            breakdownRow("Adelanto entregado:", "- ${currencyFormat.format(advancePaid)}", labelColor = EmeraldSuccess, valueColor = EmeraldSuccess, boldValue = true)
                        }
                        breakdownRow("Saldo a Financiar:", currencyFormat.format(financedBalance), boldValue = true)
                        if (installmentsCount > 1 && interestAmount > 0) {
                            breakdownRow("Recargo cuotas (${currentInterestPercent.toInt()}%):", "+ ${currencyFormat.format(interestAmount)}", labelColor = TextMuted, valueColor = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Big Monthly Payment Display Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF042436))
                            .border(1.dp, PsaCyanGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (installmentsCount == 1) "TOTAL DE CONTADO" else "VALOR DE CADA CUOTA MENSUAL",
                                color = PsaCyanGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currencyFormat.format(monthlyPayment)} / mes",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            )
                            if (installmentsCount > 1) {
                                Text(
                                    text = "Plan de $installmentsCount cuotas fijas en efectivo",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Action buttons: WhatsApp & Copy
                    val budgetMessage = remember(
                        cartLines,
                        clientName,
                        basePrice,
                        extraCost,
                        totalGrossPrice,
                        cashDiscountPercent,
                        cashDiscountAmount,
                        cashTotalWithDiscount,
                        advancePaid,
                        financedBalance,
                        installmentsCount,
                        currentInterestPercent,
                        monthlyPayment,
                        grandTotal
                    ) {
                        buildString {
                            append("💧 *PRESUPUESTO OFICIAL PSA AGUA PURA* 💧\n")
                            append("━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                            if (clientName.isNotBlank()) {
                                append("👤 *Cliente:* $clientName\n")
                            }
                            if (cartLines.size <= 1) {
                                val single = cartLines.firstOrNull()
                                if (single != null) {
                                    val prefix = if (single.second > 1) "${single.second}× " else ""
                                    append("📦 *Equipo:* $prefix${single.first.name}\n")
                                }
                            } else {
                                append("🛒 *Equipos cotizados ($totalUnitsCount unidades):*\n")
                                cartLines.forEach { (option, qty) ->
                                    append("• ${qty}× ${option.name} — ${currencyFormat.format(option.basePrice * qty)}\n")
                                }
                            }
                            append("💵 *Precio de Lista Oficial:* ${currencyFormat.format(basePrice)}\n")
                            if (extraCost > 0) {
                                append("➕ *Adicionales:* ${currencyFormat.format(extraCost)}\n")
                            }

                            append("\n🟢 *OPCIÓN 1: PAGO CONTADO EFECTIVO*\n")
                            if (cashDiscountPercent > 0) {
                                append("• *Descuento Especial:* ${cashDiscountPercent.toInt()}% OFF (Ahorra ${currencyFormat.format(cashDiscountAmount)})\n")
                                append("• *Total a pagar contado:* *${currencyFormat.format(cashTotalWithDiscount)}*\n")
                            } else {
                                append("• *Total de contado:* *${currencyFormat.format(totalGrossPrice)}*\n")
                            }

                            append("\n🟣 *OPCIÓN 2: PLAN EN CUOTAS (CON O SIN ANTICIPO)*\n")
                            if (advancePaid > 0) {
                                append("• *Entrega Inicial / Anticipo:* ${currencyFormat.format(advancePaid)}\n")
                                append("• *Saldo a financiar:* ${currencyFormat.format(financedBalance)}\n")
                            }
                            append("• *Financiación:* $installmentsCount cuotas de *${currencyFormat.format(monthlyPayment)}*\n")
                            if (currentInterestPercent > 0) {
                                append("• *Recargo financiero:* ${currentInterestPercent.toInt()}%\n")
                            } else {
                                append("• *Recargo financiero:* 0% (¡Sin interés!)\n")
                            }
                            append("• *Total financiado:* ${currencyFormat.format(grandTotal)}\n\n")

                            append("✨ *Incluye:* Garantía oficial de fábrica PSA, kit de instalación y servicio posventa.\n")
                            append("━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                            append("📲 *Distribuidor PSA:* Contacto disponible para coordinar la entrega o instalación.")
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val uri = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(budgetMessage))
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, budgetMessage)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Enviar Presupuesto PSA"))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .heightIn(min = 46.dp)
                                .testTag("btn_share_budget_whatsapp")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compartir WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.5.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("presupuesto_psa", budgetMessage)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "✅ Presupuesto copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .heightIn(min = 46.dp)
                                .testTag("btn_copy_budget")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = PsaCyanGlow)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copiar", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Generador de Ficha Comercial para Estados de WhatsApp & Redes
                    val statusCardMessage = remember(cartSummaryTitle, cashTotalWithDiscount, monthlyPayment, installmentsCount, cashDiscountPercent) {
                        buildString {
                            append("💧 *¡AGUA PURA Y CRISTALINA EN TU COCINA!* 💧\n\n")
                            append("Olvidate para siempre de cargar bidones pesados y gastar de más. 🚫📦\n")
                            append("✨ *${cartSummaryTitle.uppercase()}*\n")
                            append("• Tecnología PSA con carbón activado y máxima purificación\n")
                            append("• Agua pura ilimitada al instante para beber, mate, café y cocinar 🚰\n\n")
                            append("💳 *Llevalo hoy en $installmentsCount cuotas de ${currencyFormat.format(monthlyPayment)}*\n")
                            if (cashDiscountPercent > 0) {
                                append("🎁 O Contado Efectivo con *${cashDiscountPercent.toInt()}% OFF*: ${currencyFormat.format(cashTotalWithDiscount)}\n\n")
                            }
                            append("📲 ¡Escribime al WhatsApp para coordinar tu demostración de cloro GRATIS en tu canilla! ⏱️")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val uri = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(statusCardMessage))
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, statusCardMessage)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Publicar Ficha en Estados"))
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_share_status_whatsapp")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = PurpleNeon)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📲 Publicar Ficha en Estados de WhatsApp", color = PurpleNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (onRegisterPostSale != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                postSaleClientInput = clientName.trim()
                                postSaleCycleMonths = 12
                                showPostSaleDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("btn_register_post_sale")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Registrar Venta / Posventa", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showPostSaleDialog && onRegisterPostSale != null) {
        val primaryOpt = cartLines.firstOrNull()?.first
        AlertDialog(
            onDismissRequest = { showPostSaleDialog = false },
            containerColor = SlateCard,
            title = {
                Text(
                    text = "Registrar Venta y Recambio",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Equipo(s): $cartSummaryTitle",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = postSaleClientInput,
                        onValueChange = { postSaleClientInput = it },
                        label = { Text("Nombre del Cliente") },
                        placeholder = { Text("Ej: Familia Gómez") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("post_sale_budget_client_input")
                    )
                    Text(
                        text = "Ciclo de recambio automático:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(6, 12, 24, 36).forEach { months ->
                            FilterChip(
                                selected = postSaleCycleMonths == months,
                                onClick = { postSaleCycleMonths = months },
                                label = { Text("$months meses", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PsaCyanPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val safeClient = postSaleClientInput.trim()
                        if (safeClient.isBlank()) {
                            Toast.makeText(context, "Ingresa el nombre del cliente", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onRegisterPostSale(
                            safeClient,
                            primaryOpt?.id ?: "senior4",
                            cartSummaryTitle,
                            postSaleCycleMonths
                        )
                        Toast.makeText(context, "Posventa registrada en Agenda (alerta a $postSaleCycleMonths meses)", Toast.LENGTH_LONG).show()
                        showPostSaleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Text("Guardar Posventa", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPostSaleDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}
