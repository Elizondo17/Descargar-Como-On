package com.example.ui.screens.psa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PsaPricingConfig
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.BusinessPresentationEngine
import com.example.util.BusinessPresentationSessionState
import com.example.util.HomeZone
import com.example.util.PsaCountryConfig
import com.example.util.WeeklyPlanMode
import java.text.NumberFormat

@Composable
internal fun SlideBlockBRenderer(
    slideIndex: Int,
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit,
    pricingConfig: PsaPricingConfig,
    activeCountry: PsaCountryConfig,
    currencyFormat: NumberFormat,
    onSaveFollowUpMeeting: (date: String, time: String, score: Int, gapReason: String) -> Unit,
    onGoToQuickSignup: () -> Unit,
    onSendMaterialsWhatsApp: () -> Unit,
    onCopyMaterialsMessage: () -> Unit
) {
    when (slideIndex) {
        9 -> Slide10TeamsIdentity()
        10 -> Slide11InteractiveHomeProducts(
            state = state,
            onStateChange = onStateChange,
            pricingConfig = pricingConfig,
            activeCountry = activeCountry,
            currencyFormat = currencyFormat
        )
        11 -> Slide12BenefitsAndCertifications(state = state, onStateChange = onStateChange)
        12 -> Slide13BCorpCertification()
        13 -> Slide14PotentialMarket(state = state, onStateChange = onStateChange)
        14 -> Slide15MethodActionsAndQuiz(state = state, onStateChange = onStateChange)
        15 -> Slide16And17WeeklyPlan(state = state, onStateChange = onStateChange, defaultMode = WeeklyPlanMode.FULL_TIME)
        16 -> Slide16And17WeeklyPlan(state = state, onStateChange = onStateChange, defaultMode = WeeklyPlanMode.HALF_TIME)
        17 -> Slide18FourTrainingPillars(state = state, onStateChange = onStateChange)
        18 -> Slide19WinWinSystem(state = state, onStateChange = onStateChange)
        19 -> Slide20ValueOfTime()
        20 -> Slide21ReflectionSevenQuestions(state = state, onStateChange = onStateChange)
        21 -> Slide22ClosingThermometer(
            state = state,
            onStateChange = onStateChange,
            onSaveFollowUpMeeting = onSaveFollowUpMeeting,
            onGoToQuickSignup = onGoToQuickSignup
        )
        22 -> Slide23ShareableMaterials(
            state = state,
            onStateChange = onStateChange,
            onSendMaterialsWhatsApp = onSendMaterialsWhatsApp,
            onCopyMaterialsMessage = onCopyMaterialsMessage
        )
    }
}

@Composable
private fun Slide10TeamsIdentity() {
    val teams = listOf(
        Triple("🐝 Bee", "Colaboración activa y trabajo en colmena", AmberDelay),
        Triple("☀️ MasterSOL", "Energía, liderazgo y crecimiento sostenido", PsaCyanPrimary),
        Triple("⛰️ Los Alpinistas", "Superación constante hacia la cima en equipo", PurpleNeon)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Formás parte de una comunidad de equipos comerciales con cultura de apoyo mutuo.",
            color = TextSecondary,
            fontSize = 13.sp
        )
        teams.forEach { (name, desc, color) ->
            Surface(
                color = SlateBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(desc, color = TextSecondary, fontSize = 12.5.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Slide11InteractiveHomeProducts(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit,
    pricingConfig: PsaPricingConfig,
    activeCountry: PsaCountryConfig,
    currencyFormat: NumberFormat
) {
    val spots = remember(pricingConfig, activeCountry) {
        BusinessPresentationEngine.buildHomeProductMap(pricingConfig, activeCountry)
    }
    val filtered = remember(spots, state.activeHomeZone) {
        if (state.activeHomeZone == null) spots else spots.filter { it.zone == state.activeHomeZone }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Mapa Interactivo del Hogar — Toca cada ambiente y marca si ya usas o te interesa el equipo (precios vigentes):",
            color = TextSecondary,
            fontSize = 12.sp
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = state.activeHomeZone == null,
                onClick = { onStateChange(state.copy(activeHomeZone = null)) },
                label = { Text("Toda la casa (${spots.size})", fontSize = 11.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("home_zone_all")
            )
            HomeZone.entries.forEach { zone ->
                FilterChip(
                    selected = state.activeHomeZone == zone,
                    onClick = { onStateChange(state.copy(activeHomeZone = zone)) },
                    label = { Text(zone.label, fontSize = 11.sp) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("home_zone_${zone.name.lowercase()}")
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filtered.forEach { item ->
                val isOwned = item.id in state.ownedProductIds
                val isInterested = item.id in state.selectedProductInterestIds
                val border = when {
                    isInterested -> EmeraldSuccess
                    isOwned -> PsaCyanPrimary
                    else -> SlateCardBorder
                }
                Surface(
                    color = SlateBackground,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(if (isInterested || isOwned) 1.5.dp else 1.dp, border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_product_${item.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.displayName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${item.zone.label} • ${item.benefitLine}", color = TextSecondary, fontSize = 11.5.sp)
                            }
                            Text(
                                text = currencyFormat.format(item.catalogPrice),
                                color = PsaCyanGlow,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = isOwned,
                                onClick = {
                                    val updated = if (isOwned) state.ownedProductIds - item.id else state.ownedProductIds + item.id
                                    onStateChange(state.copy(ownedProductIds = updated))
                                },
                                label = { Text("Lo uso", fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PsaCyanPrimary.copy(alpha = 0.22f),
                                    selectedLabelColor = PsaCyanGlow
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("product_own_${item.id}")
                            )
                            FilterChip(
                                selected = isInterested,
                                onClick = {
                                    val updated = if (isInterested) state.selectedProductInterestIds - item.id else state.selectedProductInterestIds + item.id
                                    onStateChange(state.copy(selectedProductInterestIds = updated))
                                },
                                label = { Text("Me interesa", fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldSuccess.copy(alpha = 0.22f),
                                    selectedLabelColor = EmeraldSuccess
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("product_interest_${item.id}")
                            )
                        }
                    }
                }
            }
        }

        PromptQuestionBanner("¿Qué purificadores estas usando y cuáles te gustaría empezar a usar?")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Slide12BenefitsAndCertifications(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit
) {
    val activeBenefitDesc = BusinessPresentationEngine.productBenefitsList
        .firstOrNull { it.first == state.selectedProductBenefit }?.second.orEmpty()
    val activeCert = BusinessPresentationEngine.certificationsList
        .getOrElse(state.selectedCertIndex) { BusinessPresentationEngine.certificationsList.first() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("BENEFICIOS DE LOS PRODUCTOS", color = RoseError, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BusinessPresentationEngine.productBenefitsList.forEach { (benefit, _) ->
                val selected = state.selectedProductBenefit == benefit
                FilterChip(
                    selected = selected,
                    onClick = { onStateChange(state.copy(selectedProductBenefit = benefit)) },
                    label = { Text(benefit, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
        Surface(
            color = SlateBackground,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${state.selectedProductBenefit}: $activeBenefitDesc",
                color = TextPrimary,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(10.dp)
            )
        }

        Text("CERTIFICACIONES Y AVALES • Inscriptos en AESAN", color = RoseError, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BusinessPresentationEngine.certificationsList.forEachIndexed { idx, (certName, _) ->
                val selected = state.selectedCertIndex == idx
                FilterChip(
                    selected = selected,
                    onClick = { onStateChange(state.copy(selectedCertIndex = idx)) },
                    leadingIcon = { Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = { Text(certName, fontSize = 11.5.sp) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
        Surface(
            color = SlateBackground,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${activeCert.first}: ${activeCert.second}",
                color = EmeraldSuccess,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(10.dp)
            )
        }

        PromptQuestionBanner("¿Quiénes te parece que necesitan nuestros purificadores?")
    }
}

@Composable
private fun Slide13BCorpCertification() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = SlateBackground,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Empresa B Certificada", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text("B. significa beneficios para todos.", color = PsaCyanGlow, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Las Empresas B aspiramos a ser las mejores para el mundo y no solo del mundo.", color = TextSecondary, fontSize = 12.5.sp)
                Text("• Medimos nuestro impacto ambiental y social.", color = TextPrimary, fontSize = 12.5.sp)
                Text("• Logramos un puntaje de 107,6 superando ampliamente el mínimo requerido.", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                Text("En el mundo + de 7619  |  En latinoamérica + de 1120", color = TextSecondary, fontSize = 12.sp)
            }
        }

        Surface(
            color = SlateBackground,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Best for the World 2022 • Comunidad", color = PurpleNeon, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text("Fuimos reconocidas por estar dentro del 5% de las empresas B con mejor puntaje en el Área de Comunidad.", color = TextSecondary, fontSize = 12.5.sp)
                Text("¿Por qué? Porque generamos impacto positivo en las personas a través de nuestro modelo de negocio.", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                Text("En el mundo 224  |  En latinoamérica 29", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Slide14PotentialMarket(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit
) {
    val activeStat = BusinessPresentationEngine.potentialMarketCountries
        .firstOrNull { it.code == state.selectedMarketCountryCode }
        ?: BusinessPresentationEngine.potentialMarketCountries.first()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BusinessPresentationEngine.potentialMarketCountries.forEach { stat ->
                FilterChip(
                    selected = stat.code == activeStat.code,
                    onClick = { onStateChange(state.copy(selectedMarketCountryCode = stat.code)) },
                    label = { Text("${stat.flag} ${stat.countryName}", fontSize = 11.5.sp) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        Surface(
            color = SlateBackground,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, PsaCyanPrimary.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = PsaCyanGlow)
                    Text("${activeStat.flag} ${activeStat.countryName}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
                Text("MERCADO POTENCIAL: ${activeStat.potentialMarket}", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Usuarios / Red activa de referencia: ${activeStat.activeNetworkOrUsers}", color = EmeraldSuccess, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BusinessPresentationEngine.potentialMarketCountries.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${row.flag} ${row.countryName}", color = TextSecondary, fontSize = 12.sp)
                    Text("${row.potentialMarket}  (${row.activeNetworkOrUsers})", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Slide15MethodActionsAndQuiz(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("8 Acciones Productivas (toca cada paso)", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BusinessPresentationEngine.eightProductiveActions.forEach { (num, title) ->
                val selected = state.activeActionStepNumber == num
                FilterChip(
                    selected = selected,
                    onClick = { onStateChange(state.copy(activeActionStepNumber = num)) },
                    label = { Text("$num. $title", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PsaCyanPrimary.copy(alpha = 0.22f),
                        selectedLabelColor = PsaCyanGlow
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        Text("4 Acciones Soporte", color = AmberDelay, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BusinessPresentationEngine.fourSupportActions.forEach { support ->
                Surface(
                    color = AmberDelay.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = support,
                        color = AmberDelay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Quick Quiz of Comprehension
        Surface(
            color = SlateBackground,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("presentation_quick_quiz")
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Quiz, contentDescription = null, tint = PurpleNeon)
                    Text("Quiz Rápido de Chequeo", color = PurpleNeon, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
                }
                BusinessPresentationEngine.quickQuizQuestions.forEach { q ->
                    val chosen = state.quizAnswers[q.id]
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(q.question, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            q.options.forEachIndexed { idx, opt ->
                                val isChosen = chosen == idx
                                val isCorrect = idx == q.correctIndex
                                FilterChip(
                                    selected = isChosen,
                                    onClick = {
                                        onStateChange(state.copy(quizAnswers = state.quizAnswers + (q.id to idx)))
                                    },
                                    label = { Text(opt, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isCorrect) EmeraldSuccess.copy(alpha = 0.24f) else RoseError.copy(alpha = 0.24f),
                                        selectedLabelColor = if (isCorrect) EmeraldSuccess else RoseError
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("quiz_${q.id}_opt_$idx")
                                )
                            }
                        }
                        if (chosen != null) {
                            val ok = chosen == q.correctIndex
                            Text(
                                text = (if (ok) "✅ ¡Correcto! " else "💡 Dato clave: ") + q.explanation,
                                color = if (ok) EmeraldSuccess else AmberDelay,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Slide16And17WeeklyPlan(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit,
    defaultMode: WeeklyPlanMode
) {
    // Allow interactive switching between Full Time & Half Time directly while honoring the active slide context
    var userToggled by remember(defaultMode) { mutableStateOf(false) }
    val activeMode = if (userToggled) state.weeklyPlanMode else defaultMode
    val displayedDemos = if (userToggled || state.weeklyPlanMode == defaultMode) state.weeklyDemosGoal else activeMode.demosCount
    val displayedCloses = if (userToggled || state.weeklyPlanMode == defaultMode) state.weeklyClosesGoal else activeMode.closesCount
    val displayedPns = if (userToggled || state.weeklyPlanMode == defaultMode) state.weeklyPnsGoal else activeMode.pnsCount
    val displayedFollowUps = if (userToggled || state.weeklyPlanMode == defaultMode) state.weeklyFollowUpsGoal else activeMode.followUpsCount

    val scheduleSlots = listOf(
        "Mañana (09:00 - 13:00)",
        "Tarde (14:00 - 18:00)",
        "Noche (18:00 - 21:30)",
        "Fines de semana"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeeklyPlanMode.entries.forEach { mode ->
                val selected = activeMode == mode
                FilterChip(
                    selected = selected,
                    onClick = {
                        userToggled = true
                        onStateChange(
                            state.copy(
                                weeklyPlanMode = mode,
                                weeklyDemosGoal = mode.demosCount,
                                weeklyClosesGoal = mode.closesCount,
                                weeklyPnsGoal = mode.pnsCount,
                                weeklyFollowUpsGoal = mode.followUpsCount,
                                weeklyActiveDays = if (mode == WeeklyPlanMode.FULL_TIME) 6 else 5
                            )
                        )
                    },
                    label = { Text(mode.label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("weekly_mode_${mode.name.lowercase()}")
                )
            }
        }

        Surface(
            color = SlateBackground,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, PsaCyanPrimary.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(activeMode.title, color = PsaCyanGlow, fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeeklyStatBox("$displayedDemos Demostraciones", "$displayedCloses Cierres de purificadores", PsaCyanGlow, Modifier.weight(1f))
                    WeeklyStatBox("$displayedPns Presentaciones del Negocio (PN)", "$displayedFollowUps Seguimientos de Invitados", EmeraldSuccess, Modifier.weight(1f))
                }
                Text("• Fechar Demos y PNs", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("• Reunión de Equipo • Talleres • VIVO PSA • AutoFormación", color = TextSecondary, fontSize = 12.5.sp)
            }
        }

        Text("Tu disponibilidad elegida:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            scheduleSlots.forEach { slot ->
                FilterChip(
                    selected = state.selectedScheduleSlot == slot,
                    onClick = { onStateChange(state.copy(selectedScheduleSlot = slot)) },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = { Text(slot, fontSize = 11.5.sp) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        PromptQuestionBanner("¿En qué horarios podrías hacer las Acciones del negocio PSA?")
    }
}

@Composable
private fun WeeklyStatBox(top: String, bottom: String, accent: Color, modifier: Modifier) {
    Surface(
        color = accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(top, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
            Text(bottom, color = TextPrimary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Slide18FourTrainingPillars(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BusinessPresentationEngine.trainingPillars.forEachIndexed { idx, (title, subtitle, detail) ->
            val selected = state.selectedTrainingPillarIndex == idx
            Surface(
                color = if (selected) PsaCyanPrimary.copy(alpha = 0.16f) else SlateBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) PsaCyanPrimary else SlateCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onStateChange(state.copy(selectedTrainingPillarIndex = idx)) }
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$title — $subtitle", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(detail, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        Text(
            text = "Las Herramientas están a tu alcance, el negocio depende de vos",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        PromptQuestionBanner("¿Cuán importante te parece que es adquirir las Habilidades del Negocio?")
    }
}

@Composable
private fun Slide19WinWinSystem(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BusinessPresentationEngine.winWinCascade.forEachIndexed { idx, line ->
            val selected = state.selectedWinWinStepIndex == idx
            Surface(
                color = if (selected) EmeraldSuccess.copy(alpha = 0.16f) else SlateBackground,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (selected) EmeraldSuccess else PsaCyanPrimary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onStateChange(state.copy(selectedWinWinStepIndex = idx)) }
            ) {
                Text(
                    text = line,
                    color = if (selected) EmeraldSuccess else TextPrimary,
                    fontWeight = if (idx == 0 || selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
        PromptQuestionBanner("¿Cómo podes ayudar a las personas de tu red a crecer?")
    }
}

@Composable
private fun Slide20ValueOfTime() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "El desarrollo de una Red de Distribuidores en PSA te permite hacer la transición a la libertad de tiempo y dinero",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Surface(
            color = SlateBackground,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SISTEMA LABORAL TRADICIONAL", color = AmberDelay, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("NIÑEZ → EDAD LABORAL (50 AÑOS) → VEJEZ / JUBILACIÓN", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Surface(
            color = SlateBackground,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, EmeraldSuccess.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("DESARROLLO DE UNA RED PSA", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("5 AÑOS → SOLIDEZ FINANCIERA + LIBERTAD DE TIEMPO", color = EmeraldSuccess, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }

        PromptQuestionBanner("De 1 a 10, ¿Te parece que el negocio puede ser para vos?")
    }
}

@Composable
private fun Slide21ReflectionSevenQuestions(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Respecto de lo que estuvimos hablando:",
            color = PsaCyanGlow,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp
        )
        BusinessPresentationEngine.reflectionSevenQuestions.forEachIndexed { idx, question ->
            val checked = idx in state.reflectionCheckedIndices
            Surface(
                color = if (checked) EmeraldSuccess.copy(alpha = 0.14f) else SlateBackground,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (checked) EmeraldSuccess else SlateCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        val updated = if (checked) state.reflectionCheckedIndices - idx else state.reflectionCheckedIndices + idx
                        onStateChange(state.copy(reflectionCheckedIndices = updated))
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (checked) EmeraldSuccess else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(question, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Slide22ClosingThermometer(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit,
    onSaveFollowUpMeeting: (date: String, time: String, score: Int, gapReason: String) -> Unit,
    onGoToQuickSignup: () -> Unit
) {
    var meetingSavedFeedback by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1 • No me interesa el Negocio", color = RoseError, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            Text("10 • Me gustaría arrancar ahora", color = EmeraldSuccess, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            (1..10).forEach { score ->
                val selected = state.closingScore == score
                val tint = when {
                    score >= 8 -> EmeraldSuccess
                    score >= 5 -> AmberDelay
                    else -> RoseError
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (selected) tint else SlateBackground)
                        .border(1.5.dp, if (selected) tint else SlateCardBorder, CircleShape)
                        .clickable { onStateChange(state.copy(closingScore = score)) }
                        .testTag("thermometer_score_$score")
                ) {
                    Text(
                        text = "$score",
                        color = if (selected) SlateBackground else TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.closingGapReason,
            onValueChange = { onStateChange(state.copy(closingGapReason = it)) },
            label = { Text("¿Qué te separa del 10?", fontSize = 11.5.sp) },
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_closing_gap_reason"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PsaCyanPrimary,
                unfocusedBorderColor = SlateCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Surface(
            color = SlateBackground,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Te proponemos un segundo encuentro, agendemos fecha y hora",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.followUpDate,
                        onValueChange = { onStateChange(state.copy(followUpDate = it)) },
                        label = { Text("Fecha (ej: 2026-12-02)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_followup_date"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanPrimary,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = state.followUpTime,
                        onValueChange = { onStateChange(state.copy(followUpTime = it)) },
                        label = { Text("Hora (ej: 19:00)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.75f)
                            .testTag("input_followup_time"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanPrimary,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            meetingSavedFeedback = true
                            onSaveFollowUpMeeting(
                                state.followUpDate.trim(),
                                state.followUpTime.trim(),
                                state.closingScore,
                                state.closingGapReason.trim()
                            )
                        },
                        border = BorderStroke(1.dp, PsaCyanPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_save_second_meeting")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (meetingSavedFeedback) "Agendado ✓" else "Guardar 2º encuentro",
                            color = PsaCyanGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (state.closingScore >= 8) {
                        Button(
                            onClick = onGoToQuickSignup,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_thermometer_to_signup")
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = SlateBackground, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Alta Rápida", color = SlateBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Slide23ShareableMaterials(
    state: BusinessPresentationSessionState,
    onStateChange: (BusinessPresentationSessionState) -> Unit,
    onSendMaterialsWhatsApp: () -> Unit,
    onCopyMaterialsMessage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Selecciona los materiales para compartir con el invitado (${state.selectedMaterialIds.size} elegidos):",
            color = TextSecondary,
            fontSize = 12.5.sp
        )

        BusinessPresentationEngine.shareableMaterials.forEach { material ->
            val checked = material.id in state.selectedMaterialIds
            Surface(
                color = if (checked) PsaCyanPrimary.copy(alpha = 0.14f) else SlateBackground,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (checked) PsaCyanPrimary else SlateCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        val updated = if (checked) {
                            state.selectedMaterialIds - material.id
                        } else {
                            state.selectedMaterialIds + material.id
                        }
                        onStateChange(state.copy(selectedMaterialIds = updated))
                    }
                    .testTag("material_check_${material.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = EmeraldSuccess,
                            uncheckedColor = TextMuted
                        )
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(material.title, color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        Text(material.category, color = TextMuted, fontSize = 10.5.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSendMaterialsWhatsApp,
                enabled = state.selectedMaterialIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .testTag("btn_share_materials_whatsapp")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = SlateBackground, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enviar por WhatsApp", color = SlateBackground, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }
            OutlinedButton(
                onClick = onCopyMaterialsMessage,
                border = BorderStroke(1.dp, PsaCyanPrimary),
                modifier = Modifier
                    .heightIn(min = 50.dp)
                    .testTag("btn_copy_materials_text")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copiar", color = PsaCyanGlow, fontSize = 12.sp)
            }
        }
    }
}
