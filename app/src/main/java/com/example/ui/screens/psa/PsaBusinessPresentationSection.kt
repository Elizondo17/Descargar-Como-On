package com.example.ui.screens.psa

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessInviteEntity
import com.example.data.model.BusinessInviteStatus
import com.example.data.model.PsaPricingConfig
import com.example.ui.components.PsaCareerEnterpriseComponent
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.BusinessPresentationEngine
import com.example.util.BusinessPresentationSessionState
import com.example.util.PresentationBlock
import com.example.util.PsaCountryConfig
import com.example.util.PsaCountryRegistry
import com.example.util.WeeklyProspectingPlanState
import kotlin.math.roundToInt

private enum class PresentationStep(val index: Int, val label: String) {
    SLIDES(0, "1. Diapositivas"),
    CALCULATOR(1, "2. Carrera y Metas"),
    QUICK_SIGNUP(2, "3. Alta Rápida")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaBusinessPresentationSection(
    pricingConfig: PsaPricingConfig,
    activeCountry: PsaCountryConfig,
    activeInvite: BusinessInviteEntity? = null,
    sellerDisplayName: String = "",
    isHierarchyBusy: Boolean = false,
    hierarchyFeedback: String? = null,
    onClearInvite: () -> Unit = {},
    onSaveInviteFollowUp: (BusinessInviteEntity) -> Unit = {},
    onSubmitQuickRecruit: (
        inviteId: String?,
        targetName: String,
        targetEmail: String,
        phone: String,
        suggestedDays: Int,
        notes: String
    ) -> Unit
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    val currencyFormat = remember(activeCountry.code) {
        PsaCountryRegistry.currencyNumberFormat(activeCountry)
    }
    val dynamicMetrics = remember(pricingConfig, activeCountry) {
        BusinessPresentationEngine.calculateDynamicMetrics(pricingConfig, activeCountry)
    }

    var currentStep by remember { mutableStateOf(PresentationStep.SLIDES) }
    var weeklyPlanState by remember(context) {
        mutableStateOf(WeeklyProspectingPlanState.loadFromPrefs(context))
    }
    var sessionState by remember(activeInvite?.id, activeCountry.code) {
        mutableStateOf(
            BusinessPresentationSessionState(
                selectedCareerRank = weeklyPlanState.targetRank,
                selectedMarketCountryCode = activeCountry.code,
                weeklyPlanMode = weeklyPlanState.mode,
                weeklyDemosGoal = weeklyPlanState.demosPerWeek,
                weeklyClosesGoal = weeklyPlanState.closesPerWeek,
                weeklyPnsGoal = weeklyPlanState.pnsPerWeek,
                weeklyFollowUpsGoal = weeklyPlanState.followUpsPerWeek,
                weeklyActiveDays = weeklyPlanState.activeDaysPerWeek,
                selectedScheduleSlot = weeklyPlanState.selectedScheduleSlot,
                followUpDate = activeInvite?.presentationDate.orEmpty(),
                followUpTime = activeInvite?.presentationTime.orEmpty()
            )
        )
    }

    fun updateWeeklyPlan(updated: WeeklyProspectingPlanState) {
        weeklyPlanState = updated
        sessionState = sessionState.copy(
            selectedCareerRank = updated.targetRank,
            weeklyPlanMode = updated.mode,
            weeklyDemosGoal = updated.demosPerWeek,
            weeklyClosesGoal = updated.closesPerWeek,
            weeklyPnsGoal = updated.pnsPerWeek,
            weeklyFollowUpsGoal = updated.followUpsPerWeek,
            weeklyActiveDays = updated.activeDaysPerWeek,
            selectedScheduleSlot = updated.selectedScheduleSlot
        )
    }

    fun updateSessionFromSlides(updated: BusinessPresentationSessionState) {
        sessionState = updated
        weeklyPlanState = weeklyPlanState.copy(
            targetRank = updated.selectedCareerRank,
            mode = updated.weeklyPlanMode,
            demosPerWeek = updated.weeklyDemosGoal,
            closesPerWeek = updated.weeklyClosesGoal,
            pnsPerWeek = updated.weeklyPnsGoal,
            followUpsPerWeek = updated.weeklyFollowUpsGoal,
            activeDaysPerWeek = updated.weeklyActiveDays,
            selectedScheduleSlot = updated.selectedScheduleSlot
        )
    }

    // Interactive Earnings & Commissions Calculator state (Step 2)
    val sampleProducts = remember(pricingConfig) {
        pricingConfig.getAllPurifiers().take(4)
    }
    var selectedTicketPrice by remember(pricingConfig) {
        mutableStateOf(pricingConfig.senior4Price.coerceAtLeast(1000.0))
    }
    var monthlyPersonalUnits by remember { mutableIntStateOf(6) }
    var directCommissionPercent by remember { mutableFloatStateOf(30f) }
    var teamSellersCount by remember { mutableIntStateOf(3) }
    var avgUnitsPerTeamSeller by remember { mutableIntStateOf(4) }
    var teamOverridePercent by remember { mutableFloatStateOf(5f) }

    val directSalesVolume = selectedTicketPrice * monthlyPersonalUnits
    val directMonthlyEarnings = directSalesVolume * (directCommissionPercent / 100.0)
    val teamSalesVolume = selectedTicketPrice * teamSellersCount * avgUnitsPerTeamSeller
    val teamMonthlyEarnings = teamSalesVolume * (teamOverridePercent / 100.0)
    val totalMonthlyIncome = directMonthlyEarnings + teamMonthlyEarnings
    val totalAnnualProjection = totalMonthlyIncome * 12.0

    // Step 3 Quick Registration State
    var recruitName by remember(activeInvite?.id) { mutableStateOf(activeInvite?.name.orEmpty()) }
    var recruitEmail by remember(activeInvite?.id) { mutableStateOf(activeInvite?.email.orEmpty()) }
    var recruitPhone by remember(activeInvite?.id) { mutableStateOf(activeInvite?.phone.orEmpty()) }
    var recruitDays by remember { mutableIntStateOf(30) }
    var recruitNotes by remember(activeInvite?.id) {
        mutableStateOf(activeInvite?.notes?.ifBlank { "Alta tras Presentación de Negocios" } ?: "Alta tras Presentación de Negocios")
    }
    var submittedInSession by remember(activeInvite?.id) { mutableStateOf(false) }

    fun openQuickSignupWithSummary() {
        recruitNotes = BusinessPresentationEngine.buildSummaryNotesForInvite(
            state = sessionState,
            existingNotes = activeInvite?.notes.orEmpty()
        )
        currentStep = PresentationStep.QUICK_SIGNUP
    }

    fun shareSelectedMaterialsViaWhatsApp() {
        val selectedMaterials = BusinessPresentationEngine.shareableMaterials.filter {
            it.id in sessionState.selectedMaterialIds
        }
        val text = BusinessPresentationEngine.buildWhatsAppMaterialMessage(
            inviteName = activeInvite?.name ?: recruitName,
            sellerName = sellerDisplayName,
            selectedMaterials = selectedMaterials,
            ratingScore = sessionState.closingScore,
            followUpDate = sessionState.followUpDate,
            followUpTime = sessionState.followUpTime
        )
        val url = BusinessPresentationEngine.buildWhatsAppUrl(
            phone = activeInvite?.phone ?: recruitPhone,
            message = text
        )
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            clipboardManager.setText(AnnotatedString(text))
            Toast.makeText(context, "Mensaje copiado al portapapeles para enviar", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyMaterialsMessageToClipboard() {
        val selectedMaterials = BusinessPresentationEngine.shareableMaterials.filter {
            it.id in sessionState.selectedMaterialIds
        }
        val text = BusinessPresentationEngine.buildWhatsAppMaterialMessage(
            inviteName = activeInvite?.name ?: recruitName,
            sellerName = sellerDisplayName,
            selectedMaterials = selectedMaterials,
            ratingScore = sessionState.closingScore,
            followUpDate = sessionState.followUpDate,
            followUpTime = sessionState.followUpTime
        )
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "Resumen y materiales copiados", Toast.LENGTH_SHORT).show()
    }

    val allSlides = BusinessPresentationEngine.allSlides
    val currentSlide = allSlides[sessionState.activeSlideIndex.coerceIn(allSlides.indices)]
    val slideAccent = when (currentSlide.block) {
        PresentationBlock.OPPORTUNITY -> PsaCyanGlow
        PresentationBlock.EARNINGS_CAREER -> EmeraldSuccess
        PresentationBlock.PRODUCTS_TRUST -> PsaCyanPrimary
        PresentationBlock.METHOD_TRAINING -> AmberDelay
        PresentationBlock.VISION_CLOSE -> PurpleNeon
    }

    val listState = rememberLazyListState()
    LaunchedEffect(currentStep, sessionState.activeSlideIndex) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("psa_business_presentation_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + 3-Step Switcher
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Presentación de Negocios",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (activeInvite != null && activeInvite.name.isNotBlank()) {
                                    "Prospecto en sala: ${activeInvite.name} • ${activeCountry.flag} ${activeCountry.currencyCode}"
                                } else {
                                    "23 láminas oficiales en 5 bloques + cifras en vivo (${activeCountry.currencyCode})"
                                },
                                color = if (activeInvite != null && activeInvite.name.isNotBlank()) PsaCyanGlow else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (activeInvite != null && activeInvite.name.isNotBlank()) {
                            OutlinedButton(
                                onClick = onClearInvite,
                                border = BorderStroke(1.dp, SlateCardBorder),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Cambiar", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresentationStep.entries.forEach { step ->
                            val selected = currentStep == step
                            FilterChip(
                                selected = selected,
                                onClick = { currentStep = step },
                                label = {
                                    Text(
                                        text = step.label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurpleNeon.copy(alpha = 0.24f),
                                    selectedLabelColor = PurpleNeon
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("biz_pres_step_${step.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        item {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "business_presentation_step"
            ) { step ->
                when (step) {
                    PresentationStep.SLIDES -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // 5-Block Quick Jump Strip
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PresentationBlock.entries.forEach { block ->
                                    val selected = currentSlide.block == block
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            sessionState = sessionState.copy(
                                                activeSlideIndex = BusinessPresentationEngine.firstSlideIndexOfBlock(block)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = block.shortTitle,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = slideAccent.copy(alpha = 0.24f),
                                            selectedLabelColor = slideAccent
                                        ),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("block_tab_${block.id}")
                                    )
                                }
                            }

                            // Active Slide Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = BorderStroke(1.5.dp, slideAccent.copy(alpha = 0.55f)),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("biz_pres_slide_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(slideAccent.copy(alpha = 0.14f), SlateCard)
                                            )
                                        )
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = slideAccent.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, slideAccent.copy(alpha = 0.55f))
                                        ) {
                                            Text(
                                                text = "${currentSlide.block.fullTitle} • ${currentSlide.badge}",
                                                color = slideAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    LinearProgressIndicator(
                                        progress = { (currentSlide.index + 1).toFloat() / allSlides.size.toFloat() },
                                        color = slideAccent,
                                        trackColor = SlateBackground,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )

                                    Text(
                                        text = currentSlide.title,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp,
                                        lineHeight = 24.sp
                                    )

                                    if (currentSlide.subtitle.isNotBlank()) {
                                        Text(
                                            text = currentSlide.subtitle,
                                            color = slideAccent,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.5.sp
                                        )
                                    }

                                    if (currentSlide.index <= 8) {
                                        SlideBlockARenderer(
                                            slideIndex = currentSlide.index,
                                            state = sessionState,
                                            onStateChange = { updateSessionFromSlides(it) },
                                            dynamicMetrics = dynamicMetrics,
                                            pricingConfig = pricingConfig,
                                            activeCountry = activeCountry,
                                            currencyFormat = currencyFormat,
                                            activeInviteName = activeInvite?.name.orEmpty()
                                        )
                                    } else {
                                        SlideBlockBRenderer(
                                            slideIndex = currentSlide.index,
                                            state = sessionState,
                                            onStateChange = { updateSessionFromSlides(it) },
                                            pricingConfig = pricingConfig,
                                            activeCountry = activeCountry,
                                            currencyFormat = currencyFormat,
                                            onSaveFollowUpMeeting = { date, time, score, gap ->
                                                val baseInvite = activeInvite ?: BusinessInviteEntity(
                                                    name = recruitName.ifBlank { "Invitado Presentación" },
                                                    phone = recruitPhone,
                                                    email = recruitEmail
                                                )
                                                val updated = baseInvite.copy(
                                                    status = BusinessInviteStatus.ATTENDED.name,
                                                    presentationDate = date,
                                                    presentationTime = time,
                                                    notes = BusinessPresentationEngine.buildSummaryNotesForInvite(
                                                        state = sessionState.copy(closingScore = score, closingGapReason = gap),
                                                        existingNotes = baseInvite.notes
                                                    ),
                                                    updatedAtMs = System.currentTimeMillis()
                                                )
                                                onSaveInviteFollowUp(updated)
                                                Toast.makeText(
                                                    context,
                                                    "2º encuentro y evaluación guardados ($score/10)",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onGoToQuickSignup = { openQuickSignupWithSummary() },
                                            onSendMaterialsWhatsApp = { shareSelectedMaterialsViaWhatsApp() },
                                            onCopyMaterialsMessage = { copyMaterialsMessageToClipboard() }
                                        )
                                    }
                                }
                            }

                            // Slide Navigation Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (sessionState.activeSlideIndex > 0) {
                                            sessionState = sessionState.copy(activeSlideIndex = sessionState.activeSlideIndex - 1)
                                        }
                                    },
                                    enabled = sessionState.activeSlideIndex > 0,
                                    border = BorderStroke(1.dp, SlateCardBorder),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_slide_prev")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Anterior")
                                }

                                Text(
                                    text = "${sessionState.activeSlideIndex + 1} / ${allSlides.size}",
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.testTag("slide_counter_label")
                                )

                                if (sessionState.activeSlideIndex < allSlides.lastIndex) {
                                    Button(
                                        onClick = {
                                            sessionState = sessionState.copy(activeSlideIndex = sessionState.activeSlideIndex + 1)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_slide_next")
                                    ) {
                                        Text("Siguiente", color = SlateBackground, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SlateBackground, modifier = Modifier.size(16.dp))
                                    }
                                } else {
                                    Button(
                                        onClick = { currentStep = PresentationStep.CALCULATOR },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_slides_to_calculator")
                                    ) {
                                        Text("Carrera y Metas", color = SlateBackground, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Calculate, contentDescription = null, tint = SlateBackground, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    PresentationStep.CALCULATOR -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.testTag("biz_pres_calculator_panel")
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = BorderStroke(1.5.dp, EmeraldSuccess.copy(alpha = 0.55f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.AttachMoney, contentDescription = null, tint = EmeraldSuccess)
                                        Text(
                                            text = "Proyección Mensual (${activeCountry.currencyCode})",
                                            color = EmeraldSuccess,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Text(
                                        text = currencyFormat.format(totalMonthlyIncome),
                                        color = TextPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 26.sp,
                                        modifier = Modifier.testTag("biz_calc_monthly_total")
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CalcSubKpi(
                                            label = "Venta Directa",
                                            value = currencyFormat.format(directMonthlyEarnings),
                                            accent = PsaCyanGlow,
                                            modifier = Modifier.weight(1f)
                                        )
                                        CalcSubKpi(
                                            label = "Bono Equipo",
                                            value = currencyFormat.format(teamMonthlyEarnings),
                                            accent = PurpleNeon,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Surface(
                                        color = SlateBackground,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, SlateCardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Proyección Anual (12 meses)", color = TextSecondary, fontSize = 12.sp)
                                            Text(
                                                text = currencyFormat.format(totalAnnualProjection),
                                                color = EmeraldSuccess,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            )
                                        }
                                    }
                                }
                            }

                            PsaCareerEnterpriseComponent(
                                pricingConfig = pricingConfig,
                                activeCountry = activeCountry,
                                planState = weeklyPlanState,
                                onPlanStateChange = { updated ->
                                    updateWeeklyPlan(updated)
                                    val matchingPurifier = pricingConfig.getAllPurifiers().firstOrNull {
                                        it.id == updated.selectedReferencePurifierId
                                    }
                                    if (matchingPurifier != null && matchingPurifier.price > 0.0) {
                                        selectedTicketPrice = matchingPurifier.price
                                    }
                                    monthlyPersonalUnits = (updated.closesPerWeek * 4).coerceIn(1, 30)
                                },
                                onSavePlan = { saved ->
                                    WeeklyProspectingPlanState.saveToPrefs(context, saved)
                                    Toast.makeText(
                                        context,
                                        "Plan semanal (${saved.mode.label}) guardado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = BorderStroke(1.dp, SlateCardBorder),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Equipo de referencia", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        sampleProducts.forEach { model ->
                                            FilterChip(
                                                selected = selectedTicketPrice == model.price,
                                                onClick = { selectedTicketPrice = model.price.coerceAtLeast(1000.0) },
                                                label = {
                                                    Text(
                                                        text = model.name.replace("PSA ", "").take(18),
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Tus ventas mensuales", color = TextSecondary, fontSize = 12.5.sp)
                                            Text("$monthlyPersonalUnits unidades", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Slider(
                                            value = monthlyPersonalUnits.toFloat(),
                                            onValueChange = { monthlyPersonalUnits = it.roundToInt().coerceIn(1, 30) },
                                            valueRange = 1f..30f,
                                            colors = SliderDefaults.colors(thumbColor = PsaCyanGlow, activeTrackColor = PsaCyanPrimary)
                                        )
                                    }

                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Comisión / Margen directo", color = TextSecondary, fontSize = 12.5.sp)
                                            Text("${directCommissionPercent.roundToInt()}%", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Slider(
                                            value = directCommissionPercent,
                                            onValueChange = { directCommissionPercent = it.roundToInt().toFloat().coerceIn(15f, 45f) },
                                            valueRange = 15f..45f,
                                            colors = SliderDefaults.colors(thumbColor = EmeraldSuccess, activeTrackColor = EmeraldSuccess)
                                        )
                                    }

                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Vendedores en tu equipo", color = TextSecondary, fontSize = 12.5.sp)
                                            Text("$teamSellersCount vendedores", color = PurpleNeon, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Slider(
                                            value = teamSellersCount.toFloat(),
                                            onValueChange = { teamSellersCount = it.roundToInt().coerceIn(0, 20) },
                                            valueRange = 0f..20f,
                                            colors = SliderDefaults.colors(thumbColor = PurpleNeon, activeTrackColor = PurpleNeon)
                                        )
                                    }

                                    if (teamSellersCount > 0) {
                                        Column {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Promedio por vendedor / Bono equipo", color = TextSecondary, fontSize = 12.sp)
                                                Text("${avgUnitsPerTeamSeller} u. • ${teamOverridePercent.roundToInt()}%", color = AmberDelay, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                            }
                                            Slider(
                                                value = avgUnitsPerTeamSeller.toFloat(),
                                                onValueChange = { avgUnitsPerTeamSeller = it.roundToInt().coerceIn(1, 12) },
                                                valueRange = 1f..12f,
                                                colors = SliderDefaults.colors(thumbColor = AmberDelay, activeTrackColor = AmberDelay)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Simulación ilustrativa conectada al catálogo cargado en ${activeCountry.currencyCode}.",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Button(
                                onClick = { openQuickSignupWithSummary() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("btn_calc_to_signup")
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = SlateBackground)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Registrar Nuevo Vendedor Ahora", color = SlateBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    PresentationStep.QUICK_SIGNUP -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("biz_pres_quick_signup_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Groups, contentDescription = null, tint = EmeraldSuccess)
                                    Text(
                                        text = "Registro Rápido al Terminar la Charla",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.5.sp
                                    )
                                }
                                Text(
                                    text = "Completa los datos del invitado interesado para enviarlo como nuevo vendedor de tu equipo (con el resumen de sus respuestas en la presentación).",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )

                                OutlinedTextField(
                                    value = recruitName,
                                    onValueChange = { recruitName = it },
                                    label = { Text("Nombre y Apellido *", fontSize = 11.5.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("biz_pres_recruit_name"),
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
                                    label = { Text("Correo Google de acceso *", fontSize = 11.5.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("biz_pres_recruit_email"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldSuccess,
                                        unfocusedBorderColor = SlateCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                OutlinedTextField(
                                    value = recruitPhone,
                                    onValueChange = { recruitPhone = it },
                                    label = { Text("Teléfono / WhatsApp", fontSize = 11.5.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldSuccess,
                                        unfocusedBorderColor = SlateCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(15, 30, 60, 90).forEach { days ->
                                        FilterChip(
                                            selected = recruitDays == days,
                                            onClick = { recruitDays = days },
                                            label = { Text("$days días iniciales", fontSize = 11.sp) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = recruitNotes,
                                    onValueChange = { recruitNotes = it },
                                    label = { Text("Resumen / Horario de inicio", fontSize = 11.5.sp) },
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldSuccess,
                                        unfocusedBorderColor = SlateCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                if (!hierarchyFeedback.isNullOrBlank()) {
                                    Surface(
                                        color = SlateBackground,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = hierarchyFeedback,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                Button(
                                    enabled = !isHierarchyBusy && recruitName.trim().isNotBlank() && recruitEmail.trim().contains("@"),
                                    onClick = {
                                        submittedInSession = true
                                        onSubmitQuickRecruit(
                                            activeInvite?.id?.takeIf { it.isNotBlank() },
                                            recruitName.trim(),
                                            recruitEmail.trim().lowercase(),
                                            recruitPhone.trim(),
                                            recruitDays,
                                            recruitNotes.trim()
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp)
                                        .testTag("btn_biz_pres_submit_recruit")
                                ) {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null, tint = SlateBackground)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when {
                                            isHierarchyBusy -> "Registrando..."
                                            submittedInSession -> "Solicitud Enviada"
                                            else -> "Sumar Vendedor a mi Equipo"
                                        },
                                        color = SlateBackground,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun CalcSubKpi(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SlateBackground)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(
            text = value,
            color = accent,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
