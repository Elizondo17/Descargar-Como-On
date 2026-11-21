package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AutomationConfig
import com.example.data.model.DeliveryLog
import com.example.data.model.DeliveryStats
import com.example.data.model.LiveAutomationProgress
import com.example.data.model.ScannedChat
import com.example.data.model.ScriptFolder
import com.example.data.model.ScriptItem
import com.example.data.model.UserAccount
import com.example.data.repository.EzoRepository
import com.example.service.AutomationManager
import com.example.service.EzoAccessibilityService
import com.example.service.NotificationHelper
import com.example.data.model.BusinessProfile
import com.example.data.model.ChatLearningInsight
import com.example.data.model.SalesCoachingResult
import com.example.data.model.SmartResponseOption
import com.example.data.model.SalesKnowledgeItem
import com.example.data.model.MemoryEnhancedSalesAdvice
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaTimeSlot
import com.example.data.model.PsaVisitStatus
import com.example.data.model.PsaSavingsResult
import com.example.data.model.PsaMessageTone
import com.example.data.model.PsaContactLead
import com.example.data.model.PsaContactInteraction
import com.example.data.model.PsaDailyGoalStats
import com.example.data.model.PsaAchievementBadge
import com.example.data.model.PsaPostSaleInstallation
import com.example.data.model.PsaPostSaleStatus
import com.example.data.model.SellerPerformanceStats
import com.example.data.model.calculateNextReplacementTimestamp
import com.example.BuildConfig
import com.example.util.AccessLockReason
import com.example.util.AdminPricingSyncDecision
import com.example.util.AppUpdateChecker
import com.example.util.AppUpdateInfo
import com.example.util.FirebaseAppDistributionHelper
import com.example.util.FirebaseAppDistributionOutcome
import com.example.util.LicenseEvaluationResult
import com.example.util.LicenseSecurityGuard
import com.example.util.PsaCountryPricingSnapshot
import com.example.util.PsaPricingCloudSyncHelper
import com.example.util.PsaScheduleManager
import com.example.data.repository.CloudAuthOutcome
import com.example.data.repository.CloudSyncRepository
import com.example.service.GeminiSalesService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class GoogleAuthUiState {
    data object Idle : GoogleAuthUiState()
    data class Loading(val message: String = "Verificando acceso...") : GoogleAuthUiState()
    data class NotAuthorized(val email: String, val displayName: String) : GoogleAuthUiState()
    data class DeviceLimitReached(val user: UserAccount, val currentDeviceLabel: String) : GoogleAuthUiState()
    data class Error(val message: String) : GoogleAuthUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = EzoRepository(database, application)
    private val notificationHelper = NotificationHelper(application)
    private val automationManager = AutomationManager(repository, notificationHelper)
    private val geminiService = GeminiSalesService(customApiKeyProvider = { repository.getGeminiApiKey() })

    private val _googleAuthUiState = MutableStateFlow<GoogleAuthUiState>(GoogleAuthUiState.Idle)
    val googleAuthUiState: StateFlow<GoogleAuthUiState> = _googleAuthUiState.asStateFlow()

    private val _isCloudGoogleConnected = MutableStateFlow(
        repository.cloudTeamRepository?.getCurrentFirebaseUser() != null
    )
    val isCloudGoogleConnected: StateFlow<Boolean> = _isCloudGoogleConnected.asStateFlow()

    private val _isNetworkOnline = MutableStateFlow(
        LicenseSecurityGuard.hasInternetConnectivity(application)
    )
    val isNetworkOnline: StateFlow<Boolean> = _isNetworkOnline.asStateFlow()

    private val _licenseEvaluation = MutableStateFlow(
        LicenseSecurityGuard.evaluateAccess(application, null)
    )
    val licenseEvaluation: StateFlow<LicenseEvaluationResult> = _licenseEvaluation.asStateFlow()

    private val _lockVerificationFeedback = MutableStateFlow<String?>(null)
    val lockVerificationFeedback: StateFlow<String?> = _lockVerificationFeedback.asStateFlow()

    private val _cloudPricingSnapshots = MutableStateFlow(
        PsaPricingCloudSyncHelper.getCachedCloudSnapshots(application)
    )
    val cloudPricingSnapshots: StateFlow<Map<String, PsaCountryPricingSnapshot>> = _cloudPricingSnapshots.asStateFlow()

    private val _pendingAdminPricingUpdate = MutableStateFlow<PsaCountryPricingSnapshot?>(null)
    val pendingAdminPricingUpdate: StateFlow<PsaCountryPricingSnapshot?> = _pendingAdminPricingUpdate.asStateFlow()

    private val _pricingSyncBannerNotice = MutableStateFlow<String?>(null)
    val pricingSyncBannerNotice: StateFlow<String?> = _pricingSyncBannerNotice.asStateFlow()

    private val _availableAppUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableAppUpdate: StateFlow<AppUpdateInfo?> = _availableAppUpdate.asStateFlow()

    private val _serverAppUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val serverAppUpdateInfo: StateFlow<AppUpdateInfo?> = _serverAppUpdateInfo.asStateFlow()

    private val _isCheckingAppUpdate = MutableStateFlow(false)
    val isCheckingAppUpdate: StateFlow<Boolean> = _isCheckingAppUpdate.asStateFlow()

    private val _appUpdateCheckFeedback = MutableStateFlow<String?>(null)
    val appUpdateCheckFeedback: StateFlow<String?> = _appUpdateCheckFeedback.asStateFlow()

    private val _isAppDistributionTesterSignedIn = MutableStateFlow(
        FirebaseAppDistributionHelper.isTesterSignedIn()
    )
    val isAppDistributionTesterSignedIn: StateFlow<Boolean> = _isAppDistributionTesterSignedIn.asStateFlow()

    private var userListenerJob: Job? = null
    private var allUsersListenerJob: Job? = null
    private var roleMatrixListenerJob: Job? = null
    private var pricingSyncListenerJob: Job? = null
    private var appUpdateCheckJob: Job? = null
    private var workspaceBootstrapJob: Job? = null
    private var contactsSyncJob: Job? = null
    private var visitsSyncJob: Job? = null
    private var postSaleSyncJob: Job? = null
    private var sellerStatsListenerJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var wasAutoPausedByLicenseLock: Boolean = false
    private var hasCheckedAppUpdateOnLaunch: Boolean = false
    private var dismissedUpdateVersionCode: Int = 0

    val currentUser: StateFlow<UserAccount?> = repository.currentUser
    val allUsers: StateFlow<List<UserAccount>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUserTokens: StateFlow<List<com.example.data.model.UserToken>> = repository.allUserTokens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<ScriptFolder>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allScripts: StateFlow<List<ScriptItem>> = repository.allScripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<DeliveryLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allScannedChats: StateFlow<List<ScannedChat>> = repository.allScannedChats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedScannedChats: StateFlow<List<ScannedChat>> = repository.selectedScannedChats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deliveryStats: StateFlow<DeliveryStats> = repository.deliveryStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DeliveryStats())

    val businessProfile: StateFlow<BusinessProfile> = repository.businessProfileFlow
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessProfile())

    val allPresets: StateFlow<List<com.example.data.model.BusinessPreset>> = repository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSalesKnowledge: StateFlow<List<SalesKnowledgeItem>> = repository.allSalesKnowledge
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPsaVisits: StateFlow<List<PsaHomeVisit>> = repository.allPsaVisits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPsaLeads: StateFlow<List<PsaContactLead>> = repository.allPsaLeads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPsaInteractions: StateFlow<List<PsaContactInteraction>> = repository.allPsaInteractions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPostSaleInstallations: StateFlow<List<PsaPostSaleInstallation>> = repository.allPostSaleInstallations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sellerPerformanceBoard = MutableStateFlow<List<SellerPerformanceStats>>(emptyList())
    val sellerPerformanceBoard: StateFlow<List<SellerPerformanceStats>> = _sellerPerformanceBoard.asStateFlow()

    private val _apkDownloadUrl = MutableStateFlow(repository.getApkDownloadUrl())
    val apkDownloadUrl: StateFlow<String> = _apkDownloadUrl.asStateFlow()

    private val _githubUsername = MutableStateFlow(repository.getGithubUsername())
    val githubUsername: StateFlow<String> = _githubUsername.asStateFlow()

    private val _githubRepo = MutableStateFlow(repository.getGithubRepo())
    val githubRepo: StateFlow<String> = _githubRepo.asStateFlow()

    private val _psaDailyGoal = MutableStateFlow(
        application.getSharedPreferences("psa_goal_prefs", Context.MODE_PRIVATE).getInt("daily_goal", 20)
    )
    val psaDailyGoal: StateFlow<Int> = _psaDailyGoal.asStateFlow()

    fun setPsaDailyGoal(goal: Int) {
        val safe = goal.coerceIn(5, 200)
        _psaDailyGoal.value = safe
        val p = getApplication<Application>().getSharedPreferences("psa_goal_prefs", Context.MODE_PRIVATE)
        p.edit().putInt("daily_goal", safe).apply()
    }

    val dailyContactStats: StateFlow<PsaDailyGoalStats> = combine(
        allPsaInteractions,
        _psaDailyGoal
    ) { interactions, goal ->
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayInteractions = interactions.filter { it.dateKey == todayKey }

        val callsCount = todayInteractions.count { it.channel == "CALL" }
        val messagesCount = todayInteractions.count { it.channel == "MESSAGE" }
        val successCount = todayInteractions.count { it.result == "SUCCESS" }
        val noAnswerCount = todayInteractions.count { it.result == "NO_ANSWER" }
        val rescheduledCount = todayInteractions.count { it.result == "RESCHEDULED" }
        val total = todayInteractions.size

        // Streak computation
        val datesWithActivity = interactions.map { it.dateKey }.distinct().sortedDescending()
        var streak = 0
        val cal = java.util.Calendar.getInstance()
        var checkDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        if (!datesWithActivity.contains(checkDate)) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            checkDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        }
        while (datesWithActivity.contains(checkDate)) {
            streak++
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            checkDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        }
        val currentStreak = if (streak == 0 && total > 0) 1 else streak

        val badges = listOf(
            PsaAchievementBadge(
                id = "sniper",
                title = "Francotirador",
                icon = "🎯",
                description = "Concretó al menos 1 cita o venta hoy",
                isUnlocked = successCount >= 1
            ),
            PsaAchievementBadge(
                id = "on_fire",
                title = "En Llamas",
                icon = "🔥",
                description = "Alcanzó 10 gestiones en el día",
                isUnlocked = total >= 10
            ),
            PsaAchievementBadge(
                id = "goal_master",
                title = "Meta Cumplida",
                icon = "👑",
                description = "Cumplió el 100% de la meta diaria ($goal contactos)",
                isUnlocked = total >= goal
            ),
            PsaAchievementBadge(
                id = "multichannel",
                title = "Multicanal",
                icon = "📱",
                description = "Combinó llamadas y mensajes hoy",
                isUnlocked = callsCount >= 1 && messagesCount >= 1
            ),
            PsaAchievementBadge(
                id = "streak_legend",
                title = "Racha Imparable",
                icon = "⚡",
                description = "3 o más días consecutivos contactando",
                isUnlocked = currentStreak >= 3
            )
        )

        PsaDailyGoalStats(
            dateKey = todayKey,
            totalContactsToday = total,
            callsCount = callsCount,
            messagesCount = messagesCount,
            successCount = successCount,
            noAnswerCount = noAnswerCount,
            rescheduledCount = rescheduledCount,
            dailyGoal = goal,
            progressPercent = if (goal > 0) (total.toFloat() / goal).coerceIn(0f, 1f) else 0f,
            currentStreakDays = currentStreak,
            unlockedBadges = badges
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PsaDailyGoalStats(
            dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            totalContactsToday = 0,
            callsCount = 0,
            messagesCount = 0,
            successCount = 0,
            noAnswerCount = 0,
            rescheduledCount = 0,
            dailyGoal = 20,
            progressPercent = 0f,
            currentStreakDays = 1,
            unlockedBadges = emptyList()
        )
    )

    private val _psaScheduleSlots = MutableStateFlow<List<PsaTimeSlot>>(emptyList())
    val psaScheduleSlots: StateFlow<List<PsaTimeSlot>> = _psaScheduleSlots.asStateFlow()

    private val _selectedPsaDateKey = MutableStateFlow<String>("")
    val selectedPsaDateKey: StateFlow<String> = _selectedPsaDateKey.asStateFlow()

    private val _psaPricingConfig = MutableStateFlow(com.example.data.model.PsaPricingConfig())
    val psaPricingConfig: StateFlow<com.example.data.model.PsaPricingConfig> = _psaPricingConfig.asStateFlow()

    private val _isGlobalLegacyEnabled = MutableStateFlow(false)
    val isGlobalLegacyEnabled: StateFlow<Boolean> = _isGlobalLegacyEnabled.asStateFlow()

    private val _dailySlots = MutableStateFlow<List<Pair<String, String>>>(PsaScheduleManager.DEFAULT_HOURLY_RANGES)
    val dailySlots: StateFlow<List<Pair<String, String>>> = _dailySlots.asStateFlow()

    private val _roleVisibilityConfig = MutableStateFlow(com.example.data.model.RoleVisibilityConfig())
    val roleVisibilityConfig: StateFlow<com.example.data.model.RoleVisibilityConfig> = _roleVisibilityConfig.asStateFlow()

    private val _fontScaleLevel = MutableStateFlow(repository.getFontScaleLevel())
    val fontScaleLevel: StateFlow<com.example.ui.theme.FontScaleLevel> = _fontScaleLevel.asStateFlow()

    fun setFontScaleLevel(level: com.example.ui.theme.FontScaleLevel) {
        _fontScaleLevel.value = level
        repository.saveFontScaleLevel(level)
    }

    private val _hasSeenSetupGuide = MutableStateFlow(repository.hasSeenSetupGuide())
    val hasSeenSetupGuide: StateFlow<Boolean> = _hasSeenSetupGuide.asStateFlow()

    fun setSetupGuideDismissed(dismissed: Boolean) {
        _hasSeenSetupGuide.value = dismissed
        repository.saveHasSeenSetupGuide(dismissed)
    }

    private val _activePsaCountry = MutableStateFlow(com.example.util.PsaPortalSyncManager.getSelectedCountry(application))
    val activePsaCountry: StateFlow<com.example.util.PsaCountryConfig> = _activePsaCountry.asStateFlow()

    private val _psaSyncUiState = MutableStateFlow(PsaSyncUiState())
    val psaSyncUiState: StateFlow<PsaSyncUiState> = _psaSyncUiState.asStateFlow()

    private val _psaSyncVariations = MutableStateFlow<List<com.example.ui.screens.psa.PsaPriceVariation>>(emptyList())
    val psaSyncVariations: StateFlow<List<com.example.ui.screens.psa.PsaPriceVariation>> = _psaSyncVariations.asStateFlow()

    private val _pendingOfficialPvpConfirmation = MutableStateFlow<com.example.util.PsaPriceTextParser.PsaParsedPriceResult?>(null)
    val pendingOfficialPvpConfirmation: StateFlow<com.example.util.PsaPriceTextParser.PsaParsedPriceResult?> =
        _pendingOfficialPvpConfirmation.asStateFlow()

    fun dismissPsaSyncVariations() {
        _psaSyncVariations.value = emptyList()
    }

    fun applyPendingOfficialPvp(
        context: Context,
        selectedItems: List<com.example.util.PsaPriceTextParser.PsaParsedPriceItem>,
        fromDipPortal: Boolean = false
    ) {
        val pending = _pendingOfficialPvpConfirmation.value
        val updated = com.example.util.PsaPriceTextParser.applySelectedItemsToPricingConfig(
            currentConfig = _psaPricingConfig.value,
            items = selectedItems,
            detectedInstallments = pending?.detectedInstallments
        )
        savePsaPricingConfig(context, updated, fromDipPortal = fromDipPortal)
        if (!fromDipPortal) {
            val country = _activePsaCountry.value
            com.example.util.PsaPortalSyncManager.markCountrySynced(
                context,
                country.code,
                "Portal Oficial ${country.name}"
            )
            refreshSyncSummaryForCountry(context, country.code)
        }
        _pendingOfficialPvpConfirmation.value = null
    }

    fun dismissPendingOfficialPvp() {
        _pendingOfficialPvpConfirmation.value = null
    }

    private val _psaFinancingConfig = MutableStateFlow(repository.getPsaFinancingConfig())
    val psaFinancingConfig: StateFlow<com.example.data.repository.PsaFinancingConfig> = _psaFinancingConfig.asStateFlow()

    fun savePsaFinancingConfig(
        config: com.example.data.repository.PsaFinancingConfig,
        isLocalSellerEdit: Boolean = true
    ) {
        _psaFinancingConfig.value = config
        repository.savePsaFinancingConfig(config)
        val ctx = getApplication<Application>()
        val code = _activePsaCountry.value.code
        if (currentUser.value?.role == "SUPERUSER") {
            publishCurrentCountryPricingToCloud(code)
        } else if (isLocalSellerEdit) {
            PsaPricingCloudSyncHelper.setLocalPricingOverride(ctx, code, true)
        }
    }

    // AI Sales & Learning states
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _smartResponses = MutableStateFlow<List<SmartResponseOption>>(emptyList())
    val smartResponses: StateFlow<List<SmartResponseOption>> = _smartResponses.asStateFlow()

    private val _salesCoachingResult = MutableStateFlow<SalesCoachingResult?>(null)
    val salesCoachingResult: StateFlow<SalesCoachingResult?> = _salesCoachingResult.asStateFlow()

    private val _memorySalesAdvice = MutableStateFlow<MemoryEnhancedSalesAdvice?>(null)
    val memorySalesAdvice: StateFlow<MemoryEnhancedSalesAdvice?> = _memorySalesAdvice.asStateFlow()

    private val _accountBlockedNotice = MutableStateFlow<String?>(null)
    val accountBlockedNotice: StateFlow<String?> = _accountBlockedNotice.asStateFlow()

    private val _chatLearningInsight = MutableStateFlow<ChatLearningInsight?>(null)
    val chatLearningInsight: StateFlow<ChatLearningInsight?> = _chatLearningInsight.asStateFlow()

    private val _aiErrorMessage = MutableStateFlow<String?>(null)
    val aiErrorMessage: StateFlow<String?> = _aiErrorMessage.asStateFlow()

    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _isTestingApiKey = MutableStateFlow(false)
    val isTestingApiKey: StateFlow<Boolean> = _isTestingApiKey.asStateFlow()

    private val _apiKeyTestStatus = MutableStateFlow<String?>(null)
    val apiKeyTestStatus: StateFlow<String?> = _apiKeyTestStatus.asStateFlow()

    val automationProgress: StateFlow<LiveAutomationProgress> = automationManager.progress
    val automationConfig: StateFlow<AutomationConfig> = automationManager.config
    val isAccessibilityConnected: StateFlow<Boolean> = EzoAccessibilityService.isServiceConnected
    val isBubbleVisible: StateFlow<Boolean> = EzoAccessibilityService.isBubbleVisible
    val isBankingMode: StateFlow<Boolean> = EzoAccessibilityService.isBankingMode
    val isAppPaused: StateFlow<Boolean> = EzoAccessibilityService.isAppPaused
    private val _killSwitchTriggered = MutableStateFlow(false)
    val killSwitchTriggered: StateFlow<Boolean> = _killSwitchTriggered.asStateFlow()
    val selectedChatsReadyCount: StateFlow<Int> = EzoAccessibilityService.selectedChatsReadyCount

    val detectedCrmApp: StateFlow<String> = EzoAccessibilityService.detectedCrmApp
    val activeCrmPackage: StateFlow<String> = EzoAccessibilityService.activeCrmPackage
    val isCrmWindowActive: StateFlow<Boolean> = EzoAccessibilityService.isCrmWindowActive
    val visibleChatsCount: StateFlow<Int> = EzoAccessibilityService.visibleChatsCount
    val lastDetectedWindowTitle: StateFlow<String> = EzoAccessibilityService.lastDetectedWindowTitle
    val activeCrmFolder: StateFlow<String> = EzoAccessibilityService.activeCrmFolder

    val scannedCrmFolders = EzoAccessibilityService.scannedFolders
    val scannedCrmChats = EzoAccessibilityService.scannedChats

    init {
        val securityPrefs = application.getSharedPreferences("app_security_prefs", Context.MODE_PRIVATE)
        _isGlobalLegacyEnabled.value = securityPrefs.getBoolean("global_legacy_modules_enabled", false)
        _dailySlots.value = PsaScheduleManager.getDailySlots(application)
        _roleVisibilityConfig.value = repository.getRoleVisibilityConfig()

        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
            _geminiApiKey.value = repository.getGeminiApiKey()
            _psaPricingConfig.value = PsaScheduleManager.getPsaPricingConfig(application)
        }
        viewModelScope.launch {
            currentUser.collect { user ->
                evaluateLicenseNow(user)
            }
        }
        networkCallback = LicenseSecurityGuard.registerNetworkCallback(
            context = application,
            onNetworkAvailable = {
                _isNetworkOnline.value = true
                evaluateLicenseNow()
                if (currentUser.value != null) {
                    recheckCloudAccessNow(silentIfNotLocked = true)
                }
            },
            onNetworkLost = {
                _isNetworkOnline.value = false
                evaluateLicenseNow()
            }
        )
        viewModelScope.launch {
            selectedScannedChats.collect { selected ->
                val readyCount = selected.count { !it.isSent }
                EzoAccessibilityService.updateSelectedChatsCount(readyCount)
            }
        }
        // Continuous Cloud Sync for multi-device sync, instant block, added days, and alerts
        CloudSyncRepository.getInstance(application).startContinuousSync(
            scope = viewModelScope,
            usernameProvider = { currentUser.value?.username },
            onUserUpdated = { isBlocked, newExpiresAtMs, newDays ->
                val current = currentUser.value
                if (current != null) {
                    repository.syncRemoteUserStatusLocally(current.username, isBlocked, newExpiresAtMs, newDays)
                    if (isBlocked || (newExpiresAtMs > 0 && newExpiresAtMs <= System.currentTimeMillis())) {
                        shutdownBackgroundServicesOnLock()
                        _accountBlockedNotice.value = "Tu cuenta ha sido bloqueada o tu periodo de servicio ha expirado. Contacta al administrador."
                    }
                }
            },
            onEmergencyPauseRequired = {
                automationManager.pauseCampaign()
            },
            onKnowledgeSynced = { remoteList ->
                repository.syncRemoteKnowledgeLocally(remoteList)
            },
            onRoleMatrixSynced = { remoteMatrix ->
                _roleVisibilityConfig.value = remoteMatrix
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        LicenseSecurityGuard.unregisterNetworkCallback(getApplication(), networkCallback)
        networkCallback = null
    }

    fun evaluateLicenseNow(user: UserAccount? = currentUser.value): LicenseEvaluationResult {
        val eval = LicenseSecurityGuard.evaluateAccess(getApplication(), user)
        _licenseEvaluation.value = eval
        enforceLockSideEffects(eval)
        return eval
    }

    private fun shutdownBackgroundServicesOnLock() {
        val ctx = getApplication<Application>()
        automationManager.stopCampaign()
        EzoAccessibilityService.toggleFloatingBubble(ctx, false)
        EzoAccessibilityService.setAppPaused(ctx, true)
        wasAutoPausedByLicenseLock = true
    }

    private fun enforceLockSideEffects(eval: LicenseEvaluationResult) {
        val ctx = getApplication<Application>()
        if (eval.isLocked) {
            shutdownBackgroundServicesOnLock()
        } else if (wasAutoPausedByLicenseLock) {
            EzoAccessibilityService.setAppPaused(ctx, false)
            wasAutoPausedByLicenseLock = false
        }
    }

    fun onAppForegrounded(activityContext: Context? = null) {
        val online = LicenseSecurityGuard.hasInternetConnectivity(getApplication())
        _isNetworkOnline.value = online
        evaluateLicenseNow()
        val user = currentUser.value
        if (user != null) {
            if (online) {
                recheckCloudAccessNow(silentIfNotLocked = true)
                if (!hasCheckedAppUpdateOnLaunch) {
                    checkForAppUpdate(isAutomaticStartup = true)
                }
            }
        } else if (activityContext != null) {
            trySilentCloudSignIn(activityContext)
        }
    }

    fun checkForAppUpdate(isAutomaticStartup: Boolean = false) {
        val cloudRepo = repository.cloudTeamRepository ?: return
        if (cloudRepo.getCurrentFirebaseUser() == null) return
        val online = LicenseSecurityGuard.hasInternetConnectivity(getApplication())
        if (!online) {
            if (!isAutomaticStartup) {
                _appUpdateCheckFeedback.value = "Sin conexión a internet para consultar la última versión."
            }
            return
        }
        if (isAutomaticStartup && hasCheckedAppUpdateOnLaunch) return
        hasCheckedAppUpdateOnLaunch = true
        if (!isAutomaticStartup) {
            dismissedUpdateVersionCode = 0
        }

        appUpdateCheckJob?.cancel()
        appUpdateCheckJob = viewModelScope.launch {
            _isCheckingAppUpdate.value = true
            try {
                val serverInfo = cloudRepo.fetchAppVersionFromServer()
                _serverAppUpdateInfo.value = serverInfo
                val updateReady = AppUpdateChecker.isUpdateAvailable(
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    serverInfo = serverInfo,
                    dismissedVersionCode = if (isAutomaticStartup) dismissedUpdateVersionCode else 0
                )
                if (updateReady && serverInfo != null) {
                    _availableAppUpdate.value = serverInfo
                } else if (!isAutomaticStartup) {
                    _appUpdateCheckFeedback.value = if (serverInfo == null) {
                        "Aún no se publicó una versión en el servidor."
                    } else {
                        "✅ App actualizada (v${BuildConfig.VERSION_NAME} • Comp. ${BuildConfig.VERSION_CODE})."
                    }
                }
            } finally {
                _isCheckingAppUpdate.value = false
            }
            if (isAutomaticStartup && FirebaseAppDistributionHelper.isTesterSignedIn()) {
                _isAppDistributionTesterSignedIn.value = true
                val distOutcome = FirebaseAppDistributionHelper.checkAndPromptInAppUpdate(
                    interactiveSignInIfNeeded = false
                )
                if (distOutcome is FirebaseAppDistributionOutcome.NotSignedIn) {
                    _isAppDistributionTesterSignedIn.value = false
                }
            }
        }
    }

    fun triggerFirebaseAppDistributionCheck() {
        val online = LicenseSecurityGuard.hasInternetConnectivity(getApplication())
        if (!online) {
            _appUpdateCheckFeedback.value = "Sin conexión a internet para consultar Firebase App Distribution."
            return
        }
        viewModelScope.launch {
            _isCheckingAppUpdate.value = true
            try {
                when (val outcome = FirebaseAppDistributionHelper.checkAndPromptInAppUpdate(interactiveSignInIfNeeded = true)) {
                    is FirebaseAppDistributionOutcome.UpdateAvailablePrompted -> {
                        _isAppDistributionTesterSignedIn.value = true
                        val ver = outcome.release?.displayVersion.orEmpty()
                        if (ver.isNotBlank()) {
                            _appUpdateCheckFeedback.value = "🚀 Descargando actualización v$ver desde Firebase App Distribution."
                        }
                    }
                    is FirebaseAppDistributionOutcome.UpToDate -> {
                        _isAppDistributionTesterSignedIn.value = true
                        _appUpdateCheckFeedback.value = "✅ App Distribution activo: ya tienes la última versión (v${BuildConfig.VERSION_NAME})."
                    }
                    is FirebaseAppDistributionOutcome.NotImplementedInReleaseBuild -> {
                        _isCheckingAppUpdate.value = false
                        checkForAppUpdate(isAutomaticStartup = false)
                        return@launch
                    }
                    is FirebaseAppDistributionOutcome.NotSignedIn -> {
                        _isAppDistributionTesterSignedIn.value = false
                        _appUpdateCheckFeedback.value = "Inicia sesión como tester para recibir notificaciones automáticas."
                    }
                    is FirebaseAppDistributionOutcome.Failed -> {
                        _isAppDistributionTesterSignedIn.value = FirebaseAppDistributionHelper.isTesterSignedIn()
                        _appUpdateCheckFeedback.value = outcome.message
                    }
                }
            } finally {
                _isCheckingAppUpdate.value = false
            }
        }
    }

    fun dismissAppUpdateDialog() {
        val currentOffer = _availableAppUpdate.value
        if (currentOffer != null) {
            dismissedUpdateVersionCode = currentOffer.latestVersionCode
        }
        _availableAppUpdate.value = null
    }

    fun clearAppUpdateCheckFeedback() {
        _appUpdateCheckFeedback.value = null
    }

    fun publishAppVersionToServer(
        versionCode: Int,
        versionName: String,
        apkDownloadUrl: String,
        releaseNotes: String
    ) {
        val cloudRepo = repository.cloudTeamRepository ?: return
        viewModelScope.launch {
            _isCheckingAppUpdate.value = true
            try {
                val result = cloudRepo.publishAppVersionToCloud(
                    versionCode = versionCode,
                    versionName = versionName,
                    apkDownloadUrl = apkDownloadUrl,
                    releaseNotes = releaseNotes
                )
                result.onSuccess { published ->
                    _serverAppUpdateInfo.value = published
                    _appUpdateCheckFeedback.value =
                        "✅ Versión v${published.latestVersionName} (Comp. ${published.latestVersionCode}) publicada en el servidor."
                }.onFailure { err ->
                    _appUpdateCheckFeedback.value = err.message ?: "No se pudo publicar la actualización."
                }
            } finally {
                _isCheckingAppUpdate.value = false
            }
        }
    }

    fun enableBankingMode() {
        EzoAccessibilityService.enableBankingMode(fromAutoDetection = false)
    }

    fun disableBankingMode() {
        EzoAccessibilityService.disableBankingMode()
        _killSwitchTriggered.value = false
    }

    fun setAppPaused(context: android.content.Context, paused: Boolean) {
        if (paused) {
            automationManager.pauseCampaign()
        }
        EzoAccessibilityService.setAppPaused(context, paused)
        if (!paused) {
            _killSwitchTriggered.value = false
        }
    }

    fun executeKillSwitch(context: android.content.Context): Boolean {
        automationManager.stopCampaign()
        notificationHelper.cancelAllNotifications()
        val disabled = EzoAccessibilityService.executeKillSwitch(context)
        _killSwitchTriggered.value = true
        return disabled
    }

    fun disableServiceForBanking(context: android.content.Context): Boolean {
        return executeKillSwitch(context)
    }

    fun openAccessibilitySettings(context: android.content.Context) {
        EzoAccessibilityService.openAccessibilitySettings(context)
    }

    fun scanCrmScreenNow(autoScroll: Boolean = true) {
        val service = EzoAccessibilityService.instance
        if (service != null) {
            viewModelScope.launch {
                service.scanScreenAndPersist(autoScroll = autoScroll)
            }
        }
    }

    fun toggleChatSelection(id: String, selected: Boolean) {
        viewModelScope.launch {
            repository.toggleChatSelection(id, selected)
        }
    }

    fun setSelectedForChatIds(ids: List<String>, selected: Boolean) {
        viewModelScope.launch {
            repository.setSelectedForChatIds(ids, selected)
        }
    }

    fun selectOnlyPendingChats(pendingIds: List<String>) {
        viewModelScope.launch {
            repository.setAllChatsSelected(false)
            if (pendingIds.isNotEmpty()) {
                repository.setSelectedForChatIds(pendingIds, true)
            }
        }
    }

    fun setAllChatsSelected(selected: Boolean) {
        viewModelScope.launch {
            repository.setAllChatsSelected(selected)
        }
    }

    fun clearScannedChats() {
        viewModelScope.launch {
            repository.clearScannedChats()
        }
    }

    fun toggleFloatingBubble(context: Context, show: Boolean) {
        EzoAccessibilityService.toggleFloatingBubble(context, show)
    }

    fun syncScannedFolder(folderName: String) {
        viewModelScope.launch {
            val existing = allFolders.value.any { it.name.equals(folderName, ignoreCase = true) }
            if (!existing && folderName.isNotBlank()) {
                repository.addFolder(
                    name = folderName,
                    description = "Carpeta detectada automáticamente desde CRM",
                    colorHex = "#6366F1"
                )
            }
        }
    }

    fun trySilentCloudSignIn(activityContext: Context) {
        val cloudRepo = repository.cloudTeamRepository ?: return
        val existingFb = cloudRepo.getCurrentFirebaseUser()
        _isCloudGoogleConnected.value = existingFb != null
        val online = LicenseSecurityGuard.hasInternetConnectivity(getApplication())
        _isNetworkOnline.value = online
        if (currentUser.value != null && existingFb != null) {
            evaluateLicenseNow()
            if (online) {
                recheckCloudAccessNow(silentIfNotLocked = true)
                checkForAppUpdate(isAutomaticStartup = true)
            }
            return
        }
        viewModelScope.launch {
            _googleAuthUiState.value = GoogleAuthUiState.Loading("Verificando sesión segura...")
            val firebaseUser = existingFb ?: cloudRepo.trySilentGoogleSignIn(activityContext)
            _isCloudGoogleConnected.value = firebaseUser != null
            if (firebaseUser == null || firebaseUser.email.isNullOrBlank()) {
                _googleAuthUiState.value = GoogleAuthUiState.Idle
                return@launch
            }
            val pendingTamper = LicenseSecurityGuard.getPendingTamperReportMs(getApplication())
            val outcome = cloudRepo.verifyAndClaimAccess(
                emailRaw = firebaseUser.email.orEmpty(),
                firebaseUid = firebaseUser.uid,
                displayName = firebaseUser.displayName.orEmpty(),
                pendingTamperAlertMs = pendingTamper
            )
            applyCloudAuthOutcome(outcome)
        }
    }

    fun signInWithGoogle(activityContext: Context, onSuccess: () -> Unit = {}) {
        val cloudRepo = repository.cloudTeamRepository ?: return
        viewModelScope.launch {
            _googleAuthUiState.value = GoogleAuthUiState.Loading("Conectando con Google...")
            val signInResult = cloudRepo.signInInteractiveWithGoogle(activityContext)
            val firebaseUser = signInResult.getOrElse { err ->
                _isCloudGoogleConnected.value = cloudRepo.getCurrentFirebaseUser() != null
                _googleAuthUiState.value = GoogleAuthUiState.Error(err.message ?: "No se pudo iniciar sesión.")
                return@launch
            }
            _isCloudGoogleConnected.value = true
            val pendingTamper = LicenseSecurityGuard.getPendingTamperReportMs(getApplication())
            val outcome = cloudRepo.verifyAndClaimAccess(
                emailRaw = firebaseUser.email.orEmpty(),
                firebaseUid = firebaseUser.uid,
                displayName = firebaseUser.displayName.orEmpty(),
                pendingTamperAlertMs = pendingTamper
            )
            applyCloudAuthOutcome(outcome, onAuthorized = onSuccess)
        }
    }

    fun recheckCloudAccessNow(silentIfNotLocked: Boolean = false) {
        val cloudRepo = repository.cloudTeamRepository ?: return
        val firebaseUser = cloudRepo.getCurrentFirebaseUser()
        _isCloudGoogleConnected.value = firebaseUser != null
        val online = LicenseSecurityGuard.hasInternetConnectivity(getApplication())
        _isNetworkOnline.value = online
        if (!online) {
            evaluateLicenseNow()
            if (!silentIfNotLocked) {
                _lockVerificationFeedback.value = "Sin conexión a internet. Activa Wi-Fi o datos móviles para validar tu cuenta."
            }
            return
        }
        if (firebaseUser == null) return
        viewModelScope.launch {
            if (!silentIfNotLocked) {
                _lockVerificationFeedback.value = null
                _googleAuthUiState.value = GoogleAuthUiState.Loading("Verificando tu acceso...")
            }
            val pendingTamper = LicenseSecurityGuard.getPendingTamperReportMs(getApplication())
            val outcome = cloudRepo.verifyAndClaimAccess(
                emailRaw = firebaseUser.email.orEmpty(),
                firebaseUid = firebaseUser.uid,
                displayName = firebaseUser.displayName.orEmpty(),
                pendingTamperAlertMs = pendingTamper
            )
            applyCloudAuthOutcome(outcome, silentError = silentIfNotLocked)
        }
    }

    private suspend fun applyCloudAuthOutcome(
        outcome: CloudAuthOutcome,
        onAuthorized: (() -> Unit)? = null,
        silentError: Boolean = false
    ) {
        _isCloudGoogleConnected.value = repository.cloudTeamRepository?.getCurrentFirebaseUser() != null
        when (outcome) {
            is CloudAuthOutcome.Authorized -> {
                _googleAuthUiState.value = GoogleAuthUiState.Idle
                _lockVerificationFeedback.value = null
                LicenseSecurityGuard.recordOnlineVerification(
                    context = getApplication(),
                    user = outcome.user,
                    serverWallMs = outcome.serverWallMs,
                    clearPendingTamperReport = true
                )
                repository.upsertUserLocally(outcome.user)
                repository.setCurrentUser(outcome.user)
                evaluateLicenseNow(outcome.user)
                startCloudTeamListeners(outcome.user)
                checkForAppUpdate(isAutomaticStartup = true)
                onAuthorized?.invoke()
            }
            is CloudAuthOutcome.Restricted -> {
                _googleAuthUiState.value = GoogleAuthUiState.Idle
                _lockVerificationFeedback.value = if (outcome.user.isBlocked) {
                    "Acceso bloqueado por el Administrador."
                } else {
                    "Tus días de uso finalizaron. Contacta al Administrador para habilitar más días."
                }
                LicenseSecurityGuard.recordOnlineVerification(
                    context = getApplication(),
                    user = outcome.user,
                    serverWallMs = outcome.serverWallMs,
                    clearPendingTamperReport = true
                )
                repository.upsertUserLocally(outcome.user)
                repository.setCurrentUser(outcome.user)
                evaluateLicenseNow(outcome.user)
                startCloudTeamListeners(outcome.user)
                checkForAppUpdate(isAutomaticStartup = true)
            }
            is CloudAuthOutcome.NotAuthorized -> {
                repository.setCurrentUser(null)
                stopCloudTeamListeners()
                repository.wipeSellerLocalWorkspaceAndCaches()
                evaluateLicenseNow(null)
                _googleAuthUiState.value = GoogleAuthUiState.NotAuthorized(outcome.email, outcome.displayName)
            }
            is CloudAuthOutcome.DeviceLimitReached -> {
                repository.setCurrentUser(null)
                stopCloudTeamListeners()
                repository.wipeSellerLocalWorkspaceAndCaches()
                evaluateLicenseNow(null)
                _googleAuthUiState.value = GoogleAuthUiState.DeviceLimitReached(outcome.user, outcome.currentDeviceLabel)
            }
            is CloudAuthOutcome.Error -> {
                evaluateLicenseNow()
                if (!silentError) {
                    _lockVerificationFeedback.value = outcome.message
                    _googleAuthUiState.value = GoogleAuthUiState.Error(outcome.message)
                }
            }
        }
    }

    private fun startCloudTeamListeners(signedUser: UserAccount) {
        val cloudRepo = repository.cloudTeamRepository ?: return
        val deviceId = cloudRepo.getOrCreateDeviceId()
        val firebaseUid = cloudRepo.getCurrentFirebaseUser()?.uid.orEmpty()
        _isCloudGoogleConnected.value = firebaseUid.isNotBlank()

        userListenerJob?.cancel()
        userListenerJob = viewModelScope.launch {
            cloudRepo.observeUserDocument(signedUser.email).collect { event ->
                if (!event.exists || event.user == null) {
                    shutdownBackgroundServicesOnLock()
                    repository.setCurrentUser(null)
                    stopCloudTeamListeners()
                    repository.wipeSellerLocalWorkspaceAndCaches()
                    _accountBlockedNotice.value = "Tu acceso fue revocado en la nube por el Administrador."
                    _googleAuthUiState.value = GoogleAuthUiState.NotAuthorized(signedUser.email, signedUser.username)
                } else {
                    val updated = event.user
                    val stillBoundHere = updated.isDeviceAllowed(deviceId) ||
                        (updated.isBlocked || updated.isExpired)
                    if (!stillBoundHere && updated.role != "SUPERUSER") {
                        shutdownBackgroundServicesOnLock()
                        repository.setCurrentUser(null)
                        stopCloudTeamListeners()
                        repository.wipeSellerLocalWorkspaceAndCaches()
                        _googleAuthUiState.value = GoogleAuthUiState.DeviceLimitReached(
                            updated,
                            cloudRepo.getDeviceHardwareLabel()
                        )
                    } else {
                        if (event.isFromServer) {
                            _isNetworkOnline.value = true
                            LicenseSecurityGuard.recordOnlineVerification(
                                context = getApplication(),
                                user = updated,
                                serverWallMs = event.serverWallMs
                            )
                        }
                        repository.upsertUserLocally(updated)
                        repository.setCurrentUser(updated)
                        evaluateLicenseNow(updated)
                    }
                }
            }
        }

        roleMatrixListenerJob?.cancel()
        roleMatrixListenerJob = viewModelScope.launch {
            cloudRepo.observeRoleMatrix().collect { remoteMatrix ->
                if (remoteMatrix != null) {
                    _roleVisibilityConfig.value = remoteMatrix
                }
            }
        }

        pricingSyncListenerJob?.cancel()
        pricingSyncListenerJob = viewModelScope.launch {
            cloudRepo.observeGlobalPsaPricingSync().collect { (rawJson, snapshotsMap) ->
                PsaPricingCloudSyncHelper.saveCachedCloudPayloadJson(getApplication(), rawJson)
                _cloudPricingSnapshots.value = snapshotsMap
                handleIncomingCloudPricingSnapshots(snapshotsMap)
            }
        }

        allUsersListenerJob?.cancel()
        sellerStatsListenerJob?.cancel()
        if (signedUser.role == "SUPERUSER") {
            allUsersListenerJob = viewModelScope.launch {
                cloudRepo.observeAllAuthorizedUsers().collect { users ->
                    repository.replaceCloudUsersInCache(users)
                }
            }
            sellerStatsListenerJob = viewModelScope.launch {
                cloudRepo.observeAllSellerStats().collect { stats ->
                    _sellerPerformanceBoard.value = stats
                }
            }
        } else {
            _sellerPerformanceBoard.value = emptyList()
        }

        workspaceBootstrapJob?.cancel()
        contactsSyncJob?.cancel()
        visitsSyncJob?.cancel()
        postSaleSyncJob?.cancel()
        if (firebaseUid.isNotBlank()) {
            workspaceBootstrapJob = viewModelScope.launch {
                val lastUid = cloudRepo.getLastSyncedUid()
                if (lastUid.isNotBlank() && lastUid != firebaseUid) {
                    repository.wipeSellerLocalWorkspace()
                } else {
                    withTimeoutOrNull(2500L) {
                        repository.syncLocalWorkspaceUpToCloud(firebaseUid)
                    }
                }
                cloudRepo.setLastSyncedUid(firebaseUid)

                contactsSyncJob = launch {
                    cloudRepo.observeSellerContacts(firebaseUid).collect { remoteLeads ->
                        repository.replaceRemoteContacts(remoteLeads)
                    }
                }
                visitsSyncJob = launch {
                    cloudRepo.observeSellerVisits(firebaseUid).collect { remoteVisits ->
                        repository.replaceRemoteVisits(remoteVisits)
                        refreshPsaSchedule(getApplication())
                    }
                }
                postSaleSyncJob = launch {
                    cloudRepo.observeSellerPostSale(firebaseUid).collect { remotePostSale ->
                        repository.replaceRemotePostSale(remotePostSale)
                    }
                }
            }
        }
    }

    private fun stopCloudTeamListeners() {
        userListenerJob?.cancel()
        allUsersListenerJob?.cancel()
        roleMatrixListenerJob?.cancel()
        pricingSyncListenerJob?.cancel()
        workspaceBootstrapJob?.cancel()
        contactsSyncJob?.cancel()
        visitsSyncJob?.cancel()
        postSaleSyncJob?.cancel()
        sellerStatsListenerJob?.cancel()
        userListenerJob = null
        allUsersListenerJob = null
        roleMatrixListenerJob = null
        pricingSyncListenerJob = null
        workspaceBootstrapJob = null
        contactsSyncJob = null
        visitsSyncJob = null
        postSaleSyncJob = null
        sellerStatsListenerJob = null
    }

    fun refreshSellerPerformanceBoard() {
        val cloudRepo = repository.cloudTeamRepository ?: return
        viewModelScope.launch {
            repository.publishCurrentSellerStats()
            if (currentUser.value?.role == "SUPERUSER") {
                _sellerPerformanceBoard.value = cloudRepo.fetchAllSellerStatsOnce()
            }
        }
    }

    fun loginWithCredentials(
        usernameInput: String,
        keyInput: String,
        remember: Boolean = true,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.loginWithCredentials(usernameInput, keyInput, remember)
            result.fold(
                onSuccess = { user ->
                    onResult(true, "Bienvenido, ${user.username}")
                },
                onFailure = { error ->
                    onResult(false, error.message ?: "Credenciales no válidas")
                }
            )
        }
    }

    fun logout() {
        stopCloudTeamListeners()
        _isCloudGoogleConnected.value = false
        _sellerPerformanceBoard.value = emptyList()
        viewModelScope.launch {
            repository.cloudTeamRepository?.signOutGoogle()
        }
        repository.logout()
        _googleAuthUiState.value = GoogleAuthUiState.Idle
    }

    fun createAuthorizedGoogleUser(
        email: String,
        name: String,
        days: Int,
        notes: String,
        maxDevices: Int,
        onSuccess: (UserAccount) -> Unit,
        onError: (String) -> Unit
    ) {
        val cloudRepo = repository.cloudTeamRepository
        if (cloudRepo == null) {
            onError("Repositorio cloud no disponible.")
            return
        }
        if (email.isBlank() || !email.contains("@")) {
            onError("Ingresa un email de Google válido.")
            return
        }
        if (name.isBlank()) {
            onError("Ingresa el nombre del operador.")
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.createOrAuthorizeOperator(
                emailRaw = email,
                username = name,
                daysGranted = days,
                notes = notes,
                maxDevices = maxDevices
            )
            res.fold(
                onSuccess = { created ->
                    repository.upsertUserLocally(created)
                    onSuccess(created)
                },
                onFailure = { err ->
                    onError(err.message ?: "Error guardando usuario en Firestore.")
                }
            )
        }
    }

    fun unlinkUserDevice(
        user: UserAccount,
        deviceId: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val cloudRepo = repository.cloudTeamRepository ?: return
        viewModelScope.launch {
            val res = cloudRepo.unlinkDeviceFromUser(user, deviceId)
            res.fold(
                onSuccess = { updated ->
                    repository.upsertUserLocally(updated)
                    onComplete(true, "Dispositivo desvinculado.")
                },
                onFailure = { err ->
                    onComplete(false, err.message ?: "No se pudo desvincular el dispositivo.")
                }
            )
        }
    }

    fun createSecondaryUser(
        name: String,
        customKey: String?,
        days: Int,
        notes: String,
        onSuccess: (UserAccount) -> Unit,
        onError: (String) -> Unit
    ) {
        if (name.isBlank()) {
            onError("El nombre de usuario no puede estar vacío.")
            return
        }
        if (days <= 0) {
            onError("La cantidad de días debe ser mayor a 0.")
            return
        }

        viewModelScope.launch {
            try {
                val user = repository.createSecondaryUser(name, customKey, days, notes)
                onSuccess(user)
            } catch (e: Exception) {
                onError(e.message ?: "Error al crear usuario")
            }
        }
    }

    fun addDaysToUser(userId: String, extraDays: Int) {
        viewModelScope.launch {
            repository.addDaysToUser(userId, extraDays)
        }
    }

    fun adjustUserDays(userId: String, dayDelta: Int) {
        viewModelScope.launch {
            repository.adjustUserDays(userId, dayDelta)
        }
    }

    fun updateUserDetails(
        userId: String,
        newName: String,
        newNotes: String,
        remainingDays: Int,
        maxDevices: Int? = null
    ) {
        viewModelScope.launch {
            repository.updateUserDetails(userId, newName, newNotes, remainingDays, maxDevices)
        }
    }

    fun toggleBlockUser(userId: String, shouldBlock: Boolean) {
        viewModelScope.launch {
            repository.toggleBlockUser(userId, shouldBlock)
        }
    }

    fun deleteUser(user: UserAccount) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    fun changeOperatorPassword(newPassword: String, onResult: (Boolean, String?) -> Unit) {
        val user = currentUser.value
        if (user == null) {
            onResult(false, "No hay sesión iniciada.")
            return
        }
        viewModelScope.launch {
            val result = repository.updateCustomPassword(user.id, newPassword)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Error al actualizar contraseña")
            }
        }
    }

    fun addFolder(name: String, description: String, colorHex: String) {
        viewModelScope.launch {
            repository.addFolder(name, description, colorHex)
        }
    }

    fun deleteFolder(folder: ScriptFolder) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
        }
    }

    fun addScript(folderId: Long, title: String, content: String, isAutoResponse: Boolean, triggerKeyword: String) {
        viewModelScope.launch {
            repository.addScript(folderId, title, content, isAutoResponse, triggerKeyword)
        }
    }

    fun deleteScript(script: ScriptItem) {
        viewModelScope.launch {
            repository.deleteScript(script)
        }
    }

    fun updateConfig(config: AutomationConfig) {
        automationManager.updateConfig(config)
    }

    fun startAutomation(script: ScriptItem) {
        automationManager.startCampaign(script)
    }

    fun pauseAutomation() {
        automationManager.pauseCampaign()
    }

    fun stopAutomation() {
        automationManager.stopCampaign()
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllDeliveryLogs()
        }
    }

    // AI & Business Profile Configuration
    fun saveBusinessProfile(profile: BusinessProfile, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveBusinessProfile(profile)
            onComplete?.invoke(true)
        }
    }

    fun toggleAiForOperators(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAiEnabledForOperators(enabled)
        }
    }

    fun clearAiState() {
        _aiErrorMessage.value = null
        _smartResponses.value = emptyList()
        _salesCoachingResult.value = null
        _chatLearningInsight.value = null
    }

    fun generateAiSmartResponses(chatContext: String) {
        if (chatContext.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiErrorMessage.value = null
            val profile = repository.getBusinessProfile()
            val result = geminiService.generateSmartResponses(chatContext, profile)
            _isAiLoading.value = false
            if (result.isSuccess) {
                _smartResponses.value = result.getOrNull() ?: emptyList()
            } else {
                _aiErrorMessage.value = result.exceptionOrNull()?.message ?: "Error al generar respuestas"
            }
        }
    }

    fun evaluateSalesConversation(conversationText: String) {
        if (conversationText.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiErrorMessage.value = null
            val profile = repository.getBusinessProfile()
            val result = geminiService.evaluateSalesConversation(conversationText, profile)
            _isAiLoading.value = false
            if (result.isSuccess) {
                _salesCoachingResult.value = result.getOrNull()
            } else {
                _aiErrorMessage.value = result.exceptionOrNull()?.message ?: "Error al auditar la conversación"
            }
        }
    }

    fun learnFromScannedChats() {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiErrorMessage.value = null
            val chats = repository.allScannedChats.first()
            val profile = repository.getBusinessProfile()
            val result = geminiService.learnFromScannedChats(chats, profile)
            _isAiLoading.value = false
            if (result.isSuccess) {
                _chatLearningInsight.value = result.getOrNull()
            } else {
                _aiErrorMessage.value = result.exceptionOrNull()?.message ?: "Error al analizar chats del CRM"
            }
        }
    }

    fun saveGeminiApiKey(key: String) {
        val cleanKey = key.trim()
        repository.saveGeminiApiKey(cleanKey)
        _geminiApiKey.value = cleanKey
        _apiKeyTestStatus.value = if (cleanKey.isNotBlank()) "Clave guardada exitosamente" else "Clave eliminada"
        _aiErrorMessage.value = null
    }

    fun testGeminiApiKey(keyToTest: String) {
        viewModelScope.launch {
            _isTestingApiKey.value = true
            _apiKeyTestStatus.value = null
            _aiErrorMessage.value = null
            val effectiveKey = keyToTest.trim().ifBlank { _geminiApiKey.value }
            val result = geminiService.testConnection(effectiveKey)
            _isTestingApiKey.value = false
            if (result.isSuccess) {
                _apiKeyTestStatus.value = "✓ Conexión exitosa: Clave válida para Gemini AI"
                _aiErrorMessage.value = null
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error al conectar con la API de Gemini"
                _apiKeyTestStatus.value = "✗ $err"
                _aiErrorMessage.value = err
            }
        }
    }

    fun clearApiKeyTestStatus() {
        _apiKeyTestStatus.value = null
    }

    fun applyNichePreset(presetKey: String) {
        viewModelScope.launch {
            val current = repository.getBusinessProfile()
            val updated = when (presetKey) {
                "AUTOS" -> current.copy(
                    businessName = "Agencia de Autos & Seminuevos",
                    businessType = "Venta de Vehículos y Planes de Financiación",
                    offeringDescription = "Vehículos 0km y usados seleccionados con peritaje mecánico garantizado, financiación bancaria hasta en 60 cuotas, tomamos usados en parte de pago y gestionamos transferencias inmediatas con papeles al día.",
                    toneAndStyle = "Profesional, seguro, transparente, dinámico y enfocado en generar visita presencial al showroom o agendar una prueba de manejo.",
                    salesObjective = "Coordinar día y hora para ver la unidad, enviar cotización formal y solicitar datos para pre-aprobar la financiación.",
                    frequentQuestionsAndObjections = "¿Aceptan permutas? ¿Cuál es el anticipo mínimo? ¿El precio publicado es final? ¿Tienen stock para entrega inmediata?"
                )
                "CORTINAS" -> current.copy(
                    businessName = "Fábrica & Deco Cortinas",
                    businessType = "Cortinas Roller, Bandas Verticales y Toldos a Medida",
                    offeringDescription = "Cortinas Roller Blackout (100% oscuridad) y Sunscreen (control solar microperforado), telas importadas ignífugas y lavables, sistemas reforzados de aluminio, colocación a domicilio y garantía escrita de 2 años.",
                    toneAndStyle = "Cálido, estético, asesor de diseño de interiores, detallista y orientado a la armonía y calidez del hogar.",
                    salesObjective = "Solicitar el ancho x alto de la ventana para cotizar de inmediato o agendar visita técnica a domicilio para mostrar el muestrario de telas.",
                    frequentQuestionsAndObjections = "¿Cómo mido mi ventana? ¿Qué diferencia hay entre Blackout y Sunscreen? ¿Hacen envíos al interior? ¿Cuánto demora la confección?"
                )
                "TAROT" -> current.copy(
                    businessName = "Tarot Espiritual & Lecturas Místicas",
                    businessType = "Lectura de Tarot, Videncia y Guía Espiritual",
                    offeringDescription = "Lecturas profundas de Tarot en vivo por WhatsApp, Instagram y Facebook, tiradas de amor, retorno de pareja, destrabe laboral y económico, limpiezas energéticas con total reserva y certeza.",
                    toneAndStyle = "Místico, revelador, empático y cautivador; genera intriga y curiosidad emocional con preguntas clave que despiertan el deseo urgente de consultar.",
                    salesObjective = "Generar intriga sobre lo que marcan las cartas, conectar con la duda de la persona y cerrar el turno para la lectura completa hoy mismo.",
                    frequentQuestionsAndObjections = "¿Cómo es la modalidad de la lectura? ¿Cuánto dura? ¿Qué datos míos necesitas? ¿Por qué medio se abona la seña?"
                )
                else -> current.copy(
                    businessName = "Comercio & Servicios",
                    businessType = "Ventas y Atención Comercial",
                    offeringDescription = "Productos y servicios garantizados, stock inmediato, facilidades de pago en cuotas y atención personalizada.",
                    toneAndStyle = "Amable, persuasivo, directo y atento.",
                    salesObjective = "Cerrar la venta o coordinar la entrega/servicio.",
                    frequentQuestionsAndObjections = "¿Formas de pago? ¿Costos de envío? ¿Tienen stock?"
                )
            }
            repository.saveBusinessProfile(updated)
        }
    }

    fun applyPreset(preset: com.example.data.model.BusinessPreset) {
        viewModelScope.launch {
            val current = repository.getBusinessProfile()
            val updated = current.copy(
                businessName = preset.businessName,
                businessType = preset.businessType,
                offeringDescription = preset.offeringDescription,
                toneAndStyle = preset.toneAndStyle,
                salesObjective = preset.salesObjective,
                frequentQuestionsAndObjections = preset.frequentQuestionsAndObjections
            )
            repository.saveBusinessProfile(updated)
        }
    }

    fun generateBusinessProfileWithAi(rawIdea: String, onComplete: (BusinessProfile?) -> Unit) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiErrorMessage.value = null
            val result = repository.generateBusinessProfileWithAi(rawIdea)
            _isAiLoading.value = false
            if (result.isSuccess) {
                val profile = result.getOrNull()
                if (profile != null) {
                    val current = repository.getBusinessProfile()
                    val toSave = profile.copy(
                        isAiEnabledForOperators = current.isAiEnabledForOperators
                    )
                    repository.saveBusinessProfile(toSave)
                    onComplete(toSave)
                } else {
                    onComplete(null)
                }
            } else {
                _aiErrorMessage.value = result.exceptionOrNull()?.message ?: "Error al generar perfil con IA"
                onComplete(null)
            }
        }
    }

    fun saveCustomPreset(
        title: String,
        businessName: String,
        businessType: String,
        offeringDescription: String,
        toneAndStyle: String,
        salesObjective: String,
        frequentQuestionsAndObjections: String,
        onDone: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val preset = com.example.data.model.BusinessPreset(
                id = "preset_custom_" + java.util.UUID.randomUUID().toString().substring(0, 8),
                title = title.ifBlank { businessName.ifBlank { "Plantilla Personalizada" } },
                businessName = businessName,
                businessType = businessType,
                offeringDescription = offeringDescription,
                toneAndStyle = toneAndStyle,
                salesObjective = salesObjective,
                frequentQuestionsAndObjections = frequentQuestionsAndObjections,
                isCustom = true
            )
            repository.saveCustomPreset(preset)
            onDone?.invoke()
        }
    }

    fun saveCustomPreset(title: String, profile: BusinessProfile, onDone: (() -> Unit)? = null) {
        saveCustomPreset(
            title = title,
            businessName = profile.businessName,
            businessType = profile.businessType,
            offeringDescription = profile.offeringDescription,
            toneAndStyle = profile.toneAndStyle,
            salesObjective = profile.salesObjective,
            frequentQuestionsAndObjections = profile.frequentQuestionsAndObjections,
            onDone = onDone
        )
    }

    fun deletePreset(preset: com.example.data.model.BusinessPreset) {
        viewModelScope.launch {
            repository.deletePreset(preset)
        }
    }

    fun deletePreset(presetId: String) {
        viewModelScope.launch {
            val target = allPresets.value.find { it.id == presetId }
            if (target != null) {
                repository.deletePreset(target)
            }
        }
    }

    fun dismissAccountBlockedNotice() {
        _accountBlockedNotice.value = null
    }

    fun generateMemoryEnhancedAdvice(chatContext: String) {
        if (chatContext.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiErrorMessage.value = null
            val result = repository.generateMemoryEnhancedAdvice(chatContext, geminiService)
            _isAiLoading.value = false
            if (result.isSuccess) {
                _memorySalesAdvice.value = result.getOrNull()
            } else {
                _aiErrorMessage.value = result.exceptionOrNull()?.message ?: "Error al generar sugerencias persuasivas"
            }
        }
    }

    fun rateSalesKnowledge(id: String, increment: Int = 1) {
        viewModelScope.launch {
            repository.rateSalesKnowledge(id, increment)
        }
    }

    fun addCustomWinningKnowledge(category: String, triggerContext: String, content: String, isQuestion: Boolean) {
        viewModelScope.launch {
            repository.addWinningSalesKnowledge(
                content = content,
                category = category,
                isQuestion = isQuestion,
                triggerContext = triggerContext
            )
        }
    }

    fun deleteSalesKnowledge(id: String) {
        viewModelScope.launch {
            repository.deleteSalesKnowledge(id)
        }
    }

    // --- PSA PURIFICADORES WORKSPACE METHODS ---

    fun refreshPsaSchedule(context: Context) {
        val slots = PsaScheduleManager.generateScheduleForDays(context, 7, allPsaVisits.value)
        _psaScheduleSlots.value = slots
        if (_selectedPsaDateKey.value.isEmpty() && slots.isNotEmpty()) {
            _selectedPsaDateKey.value = slots.first().dateKey
        }
    }

    fun setSelectedPsaDate(dateKey: String) {
        _selectedPsaDateKey.value = dateKey
    }

    fun togglePsaSlotBlocked(context: Context, slotId: String) {
        PsaScheduleManager.toggleSlotBlocked(context, slotId)
        refreshPsaSchedule(context)
    }

    fun bookPsaVisit(
        context: Context,
        customerName: String,
        phone: String,
        address: String,
        neighborhood: String,
        scheduledDate: String,
        scheduledTime: String,
        modelInterest: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val visit = PsaHomeVisit(
                id = "psa_visit_${System.currentTimeMillis()}",
                customerName = customerName.trim(),
                phone = phone.trim(),
                address = address.trim(),
                neighborhood = neighborhood.trim(),
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                modelInterest = modelInterest,
                status = PsaVisitStatus.AGENDADA,
                notes = notes
            )
            repository.insertPsaVisit(visit)
            refreshPsaSchedule(context)
        }
    }

    fun updatePsaVisitStatus(visit: PsaHomeVisit, newStatus: PsaVisitStatus) {
        viewModelScope.launch {
            repository.updatePsaVisit(visit.copy(status = newStatus))
        }
    }

    fun saveLiveDemoResult(
        visit: PsaHomeVisit,
        chlorinePpm: Float,
        quotedPlan: String,
        status: PsaVisitStatus = PsaVisitStatus.CERRADA_EXITO,
        notes: String
    ) {
        viewModelScope.launch {
            repository.updatePsaVisit(
                visit.copy(
                    chlorinePpm = chlorinePpm,
                    quotedPlan = quotedPlan,
                    status = status,
                    notes = notes
                )
            )
        }
    }

    fun deletePsaVisit(context: Context, visitId: String) {
        viewModelScope.launch {
            repository.deletePsaVisit(visitId)
            refreshPsaSchedule(context)
        }
    }

    fun saveDemoSnapshotToVisit(
        context: Context,
        visit: PsaHomeVisit,
        chlorinePpm: Float,
        waterSource: String,
        hardnessPpm: Int,
        realLitersPerDay: Double,
        cartSummary: String,
        testPhotoPath: String,
        quotedPlan: String = visit.quotedPlan,
        newStatus: PsaVisitStatus = if (visit.status == PsaVisitStatus.CERRADA_EXITO) PsaVisitStatus.CERRADA_EXITO else PsaVisitStatus.EN_DEMOSTRACION
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = visit.copy(
                chlorinePpm = chlorinePpm,
                waterSource = waterSource,
                hardnessPpm = hardnessPpm,
                realLitersPerDay = realLitersPerDay,
                cartSummary = cartSummary,
                testPhotoPath = testPhotoPath,
                quotedPlan = quotedPlan.ifBlank { cartSummary },
                diagnosticTimestampMs = now,
                status = newStatus,
                updatedAtMs = now
            )
            repository.updatePsaVisit(updated)
            refreshPsaSchedule(context)
        }
    }

    // --- PSA POST-SALE FILTER REPLACEMENTS ---

    fun registerPostSaleInstallation(
        customerName: String,
        customerPhone: String,
        purifierId: String,
        purifierName: String,
        replacementCycleMonths: Int,
        installedAtMs: Long = System.currentTimeMillis(),
        visitId: String = "",
        contactId: String = "",
        notes: String = ""
    ) {
        val safeName = customerName.trim()
        if (safeName.isBlank()) return
        val safeCycle = when (replacementCycleMonths) {
            6, 12, 24, 36 -> replacementCycleMonths
            else -> 12
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val nextMs = calculateNextReplacementTimestamp(installedAtMs, safeCycle)
            val record = PsaPostSaleInstallation(
                id = "ps_${now}_${(100..999).random()}",
                contactId = contactId,
                visitId = visitId,
                customerName = safeName,
                customerPhone = customerPhone.trim(),
                purifierId = purifierId.ifBlank { "senior4" },
                purifierName = purifierName.ifBlank { "PSA Senior 4" },
                installedAtMs = installedAtMs,
                replacementCycleMonths = safeCycle,
                nextReplacementAtMs = nextMs,
                lastNotifiedDayKey = "",
                status = PsaPostSaleStatus.ACTIVO,
                notes = notes.trim(),
                createdAtMs = now,
                updatedAtMs = now
            )
            repository.insertPostSaleInstallation(record)
        }
    }

    fun markPostSaleReplacementCompleted(record: PsaPostSaleInstallation) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val nextMs = calculateNextReplacementTimestamp(now, record.replacementCycleMonths)
            val updated = record.copy(
                installedAtMs = now,
                nextReplacementAtMs = nextMs,
                lastNotifiedDayKey = "",
                status = PsaPostSaleStatus.ACTIVO,
                updatedAtMs = now
            )
            repository.updatePostSaleInstallation(updated)
        }
    }

    fun updatePostSaleCycle(record: PsaPostSaleInstallation, newCycleMonths: Int) {
        val safeCycle = when (newCycleMonths) {
            6, 12, 24, 36 -> newCycleMonths
            else -> 12
        }
        viewModelScope.launch {
            val nextMs = calculateNextReplacementTimestamp(record.installedAtMs, safeCycle)
            val updated = record.copy(
                replacementCycleMonths = safeCycle,
                nextReplacementAtMs = nextMs,
                updatedAtMs = System.currentTimeMillis()
            )
            repository.updatePostSaleInstallation(updated)
        }
    }

    fun deletePostSaleInstallation(recordId: String) {
        viewModelScope.launch {
            repository.deletePostSaleInstallation(recordId)
        }
    }

    fun checkAndNotifyDueReplacements(context: Context) {
        viewModelScope.launch {
            val records = repository.allPostSaleInstallations.first()
            val now = System.currentTimeMillis()
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
            val dueRecords = records.filter {
                it.status == PsaPostSaleStatus.ACTIVO &&
                    it.daysUntilReplacement(now) <= 15 &&
                    it.lastNotifiedDayKey != todayKey
            }.sortedBy { it.nextReplacementAtMs }

            if (dueRecords.isEmpty()) return@launch

            dueRecords.take(3).forEach { rec ->
                val days = rec.daysUntilReplacement(now)
                notificationHelper.showPostSaleReplacementNotification(
                    recordId = rec.id,
                    customerName = rec.customerName,
                    purifierName = rec.purifierName,
                    daysRemaining = days,
                    isOverdue = days <= 0
                )
            }
            dueRecords.forEach { rec ->
                repository.updatePostSaleInstallation(
                    rec.copy(
                        lastNotifiedDayKey = todayKey,
                        updatedAtMs = now
                    )
                )
            }
        }
    }

    fun getPsaSavings(
        consumptionPerWeek: Int,
        unitPrice: Double = _psaPricingConfig.value.bidonPrice,
        purifierPrice: Double = _psaPricingConfig.value.senior4Price,
        installmentsCount: Int = _psaPricingConfig.value.installmentsCount,
        comparisonType: com.example.data.model.PsaComparisonType = _psaPricingConfig.value.defaultComparisonType,
        lifespanMonths: Int = _psaPricingConfig.value.defaultLifespanMonths,
        containerLiters: Double = 0.0
    ): PsaSavingsResult {
        return PsaScheduleManager.calculateSavings(
            bottlesPerWeek = consumptionPerWeek,
            bottlePrice = unitPrice,
            purifierPrice = purifierPrice,
            installmentsCount = installmentsCount,
            comparisonType = comparisonType,
            lifespanMonths = lifespanMonths,
            containerLiters = containerLiters
        )
    }

    fun savePsaPricingConfig(
        context: Context,
        config: com.example.data.model.PsaPricingConfig,
        fromDipPortal: Boolean = false,
        isLocalSellerEdit: Boolean = true
    ) {
        val code = _activePsaCountry.value.code
        PsaScheduleManager.savePsaPricingConfigForCountry(context, code, config)
        com.example.util.PsaStockManager.syncWithOfficialPvp(context, config, code)
        if (fromDipPortal) {
            com.example.util.PsaPortalSyncManager.markCountryDipConfirmed(context, code)
        }
        _psaPricingConfig.value = config
        refreshSyncSummaryForCountry(context, code)
        if (currentUser.value?.role == "SUPERUSER") {
            publishCurrentCountryPricingToCloud(code)
        } else if (isLocalSellerEdit) {
            PsaPricingCloudSyncHelper.setLocalPricingOverride(context, code, true)
        }
    }

    fun restorePsaBaselinePricing(context: Context) {
        val code = _activePsaCountry.value.code
        val baseline = PsaScheduleManager.restoreBaselinePricingForCountry(context, code)
        _psaPricingConfig.value = baseline
        if (currentUser.value?.role == "SUPERUSER") {
            publishCurrentCountryPricingToCloud(code)
        } else {
            PsaPricingCloudSyncHelper.setLocalPricingOverride(context, code, true)
        }
    }

    fun reloadPsaPricingConfig(context: Context) {
        val code = _activePsaCountry.value.code
        _psaPricingConfig.value = PsaScheduleManager.getPsaPricingConfigForCountry(context, code)
        refreshSyncSummaryForCountry(context, code)
    }

    fun switchPsaCountry(context: Context, countryCode: String) {
        val nextCountry = com.example.util.PsaPortalSyncManager.saveSelectedCountryCode(context, countryCode)
        _activePsaCountry.value = nextCountry
        _psaPricingConfig.value = PsaScheduleManager.getPsaPricingConfigForCountry(context, nextCountry.code)
        refreshSyncSummaryForCountry(context, nextCountry.code)
        handleIncomingCloudPricingSnapshots(_cloudPricingSnapshots.value)
        refreshOfficialPsaPrices(context, force = false)
    }

    private fun publishCurrentCountryPricingToCloud(countryCode: String) {
        val cloudRepo = repository.cloudTeamRepository ?: return
        if (currentUser.value?.role != "SUPERUSER") return
        val snapshot = PsaCountryPricingSnapshot(
            countryCode = countryCode.uppercase(),
            versionMs = System.currentTimeMillis(),
            updatedBy = currentUser.value?.email?.ifBlank {
                com.example.data.repository.FirestoreTeamRepository.BOOTSTRAP_ADMIN_EMAIL
            } ?: com.example.data.repository.FirestoreTeamRepository.BOOTSTRAP_ADMIN_EMAIL,
            pricingConfig = _psaPricingConfig.value,
            financingConfig = _psaFinancingConfig.value
        )
        viewModelScope.launch {
            val res = cloudRepo.publishGlobalPsaPricingToCloud(snapshot)
            res.onSuccess { mergedJson ->
                val parsed = PsaPricingCloudSyncHelper.parseAllCountriesPayload(mergedJson)
                PsaPricingCloudSyncHelper.saveCachedCloudPayloadJson(getApplication(), mergedJson)
                _cloudPricingSnapshots.value = parsed
                _pricingSyncBannerNotice.value = "Precios y cuotas publicados para todo tu equipo (${countryCode.uppercase()})."
            }
        }
    }

    private fun handleIncomingCloudPricingSnapshots(snapshotsMap: Map<String, PsaCountryPricingSnapshot>) {
        val ctx = getApplication<Application>()
        val code = _activePsaCountry.value.code.uppercase()
        val snapshot = snapshotsMap[code] ?: return
        if (currentUser.value?.role == "SUPERUSER") {
            _pendingAdminPricingUpdate.value = null
            return
        }
        val hasLocalOverride = PsaPricingCloudSyncHelper.hasLocalPricingOverride(ctx, code)
        val lastApplied = PsaPricingCloudSyncHelper.getAppliedAdminVersion(ctx, code)
        val dismissed = PsaPricingCloudSyncHelper.getDismissedAdminVersion(ctx, code)
        when (
            PsaPricingCloudSyncHelper.evaluateSyncDecision(
                hasLocalOverride = hasLocalOverride,
                lastAppliedAdminVersionMs = lastApplied,
                dismissedAdminVersionMs = dismissed,
                incomingVersionMs = snapshot.versionMs
            )
        ) {
            AdminPricingSyncDecision.AUTO_APPLY -> {
                applyAdminPricingSnapshotLocally(ctx, snapshot, showToastNotice = true)
            }
            AdminPricingSyncDecision.PROMPT_SELLER -> {
                _pendingAdminPricingUpdate.value = snapshot
            }
            AdminPricingSyncDecision.NO_OP -> {
                if (_pendingAdminPricingUpdate.value?.countryCode != code) {
                    _pendingAdminPricingUpdate.value = null
                }
            }
        }
    }

    private fun applyAdminPricingSnapshotLocally(
        context: Context,
        snapshot: PsaCountryPricingSnapshot,
        showToastNotice: Boolean
    ) {
        val code = snapshot.countryCode.uppercase()
        PsaScheduleManager.savePsaPricingConfigForCountry(context, code, snapshot.pricingConfig)
        com.example.util.PsaStockManager.syncWithOfficialPvp(context, snapshot.pricingConfig, code)
        repository.savePsaFinancingConfig(snapshot.financingConfig)
        if (_activePsaCountry.value.code.equals(code, ignoreCase = true)) {
            _psaPricingConfig.value = snapshot.pricingConfig
            _psaFinancingConfig.value = snapshot.financingConfig
        }
        PsaPricingCloudSyncHelper.setAppliedAdminVersion(context, code, snapshot.versionMs)
        PsaPricingCloudSyncHelper.setLocalPricingOverride(context, code, false)
        _pendingAdminPricingUpdate.value = null
        refreshSyncSummaryForCountry(context, code)
        if (showToastNotice) {
            _pricingSyncBannerNotice.value = "El Administrador actualizó los precios y cuotas ($code)."
        }
    }

    fun acceptPendingAdminPricingUpdate(context: Context) {
        val snapshot = _pendingAdminPricingUpdate.value ?: return
        applyAdminPricingSnapshotLocally(context, snapshot, showToastNotice = true)
    }

    fun dismissPendingAdminPricingUpdate(context: Context) {
        val snapshot = _pendingAdminPricingUpdate.value ?: return
        PsaPricingCloudSyncHelper.setDismissedAdminVersion(context, snapshot.countryCode, snapshot.versionMs)
        _pendingAdminPricingUpdate.value = null
    }

    fun applyLatestCloudAdminPricingManually(context: Context): Boolean {
        val code = _activePsaCountry.value.code.uppercase()
        val snapshot = _cloudPricingSnapshots.value[code] ?: return false
        applyAdminPricingSnapshotLocally(context, snapshot, showToastNotice = true)
        return true
    }

    fun dismissPricingSyncBannerNotice() {
        _pricingSyncBannerNotice.value = null
    }

    private fun refreshSyncSummaryForCountry(context: Context, countryCode: String) {
        val country = com.example.util.PsaCountryRegistry.getByCode(countryCode)
        val (date, source) = com.example.util.PsaPortalSyncManager.getCountryLastSyncSummary(context, country.code)
        _psaSyncUiState.value = PsaSyncUiState(
            isSyncing = false,
            lastSyncedDate = date,
            lastSyncedSource = source,
            statusMessage = if (date.isNotBlank()) "Actualizado: $date" else "",
            isLiveVerified = date.isNotBlank(),
            requiresPrivatePortal = !country.hasPublicStore
        )
    }

    fun refreshOfficialPsaPrices(context: Context, force: Boolean = false) {
        val country = _activePsaCountry.value
        if (!force && !com.example.util.PsaPortalSyncManager.needsAutoSync(context, country.code)) {
            refreshSyncSummaryForCountry(context, country.code)
            return
        }
        if (_psaSyncUiState.value.isSyncing) return

        viewModelScope.launch {
            _psaSyncUiState.value = _psaSyncUiState.value.copy(
                isSyncing = true,
                statusMessage = "Consultando PSA ${country.name}..."
            )
            val result = com.example.util.PsaOfficialCatalogFetcher.fetchLiveCatalog(
                country = country,
                currentPricing = _psaPricingConfig.value
            )
            if (result.success && result.updatedPricing != null) {
                val diffResult = com.example.util.PsaPriceTextParser.buildDiffResultFromCatalog(
                    currentConfig = _psaPricingConfig.value,
                    catalogProducts = result.extractedProducts,
                    countryCode = country.code,
                    detectedInstallments = result.updatedPricing.installmentsCount,
                    sourceNote = "DOM Portal Oficial (${country.name})"
                )
                if (diffResult.items.isNotEmpty()) {
                    _pendingOfficialPvpConfirmation.value = diffResult
                } else {
                    com.example.util.PsaPortalSyncManager.markCountrySynced(
                        context,
                        country.code,
                        "Portal Oficial ${country.name}"
                    )
                }
                val (date, source) = com.example.util.PsaPortalSyncManager.getCountryLastSyncSummary(context, country.code)
                _psaSyncUiState.value = PsaSyncUiState(
                    isSyncing = false,
                    lastSyncedDate = date,
                    lastSyncedSource = source,
                    statusMessage = result.message,
                    isLiveVerified = true,
                    requiresPrivatePortal = false
                )
            } else {
                val (date, source) = com.example.util.PsaPortalSyncManager.getCountryLastSyncSummary(context, country.code)
                _psaSyncUiState.value = PsaSyncUiState(
                    isSyncing = false,
                    lastSyncedDate = date,
                    lastSyncedSource = source,
                    statusMessage = result.message,
                    isLiveVerified = date.isNotBlank(),
                    requiresPrivatePortal = result.requiresPrivateDipPortal
                )
            }
        }
    }

    fun setGlobalLegacyEnabled(enabled: Boolean) {
        val securityPrefs = getApplication<Application>().getSharedPreferences("app_security_prefs", Context.MODE_PRIVATE)
        securityPrefs.edit().putBoolean("global_legacy_modules_enabled", enabled).apply()
        _isGlobalLegacyEnabled.value = enabled
        viewModelScope.launch {
            repository.saveRoleVisibilityConfig(_roleVisibilityConfig.value, globalLegacyEnabled = enabled)
        }
    }

    fun saveApkDownloadUrl(url: String) {
        _apkDownloadUrl.value = repository.saveApkDownloadUrl(url)
    }

    fun saveGithubDistributionConfig(username: String, repo: String) {
        val (cleanUser, cleanRepo, autoApkUrl) = repository.saveGithubConfig(username, repo)
        _githubUsername.value = cleanUser
        _githubRepo.value = cleanRepo
        if (autoApkUrl.isNotBlank()) {
            _apkDownloadUrl.value = autoApkUrl
        } else if (_apkDownloadUrl.value.contains("github.com/", ignoreCase = true)) {
            _apkDownloadUrl.value = repository.saveApkDownloadUrl("")
        }
    }

    fun saveDailySlots(context: Context, slots: List<Pair<String, String>>) {
        PsaScheduleManager.saveDailySlots(context, slots)
        _dailySlots.value = slots
        refreshPsaSchedule(context)
    }

    fun toggleUserLegacyAccess(userId: String, allowLegacy: Boolean) {
        viewModelScope.launch {
            val user = allUsers.value.find { it.id == userId }
            if (user != null) {
                repository.updateUserLocally(user.copy(canAccessLegacyModules = allowLegacy))
            }
        }
    }

    fun generatePsaProposalMessage(
        clientName: String,
        neighborhood: String,
        tone: PsaMessageTone = PsaMessageTone.CALIDO_HUMANO
    ): String {
        val nextSlots = PsaScheduleManager.getNextAvailableSlots(_psaScheduleSlots.value, 2)
        return PsaScheduleManager.generateAppointmentProposalMessage(clientName, neighborhood, nextSlots, tone)
    }

    fun saveCustomPsaProposal(context: Context, text: String) {
        PsaScheduleManager.saveCustomProposal(context, text)
    }

    fun getCustomPsaProposal(context: Context): String? {
        return PsaScheduleManager.getCustomProposal(context)
    }

    fun clearCustomPsaProposal(context: Context) {
        PsaScheduleManager.clearCustomProposal(context)
    }

    fun saveCustomPsaScript(context: Context, scriptId: String, newBody: String) {
        PsaScheduleManager.saveCustomScript(context, scriptId, newBody)
    }

    fun getCustomPsaScript(context: Context, scriptId: String, defaultText: String): String {
        return PsaScheduleManager.getCustomScript(context, scriptId, defaultText)
    }

    fun saveRoleVisibilityConfig(config: com.example.data.model.RoleVisibilityConfig) {
        _roleVisibilityConfig.value = config
        viewModelScope.launch {
            repository.saveRoleVisibilityConfig(config, globalLegacyEnabled = _isGlobalLegacyEnabled.value)
        }
    }

    fun syncAllUsersFromCloud() {
        viewModelScope.launch {
            repository.syncAllUsersFromCloud()
        }
    }

    fun refreshUserCloudStatus(username: String, onComplete: ((Boolean) -> Unit)? = null) {
        val cloudRepo = repository.cloudTeamRepository
        if (cloudRepo?.getCurrentFirebaseUser() != null) {
            recheckCloudAccessNow()
            onComplete?.invoke(currentUser.value?.isBlocked == true)
            return
        }
        viewModelScope.launch {
            CloudSyncRepository.getInstance(getApplication()).syncCurrentUserStatus(
                username = username,
                onUserUpdated = { isBlocked, newExpiresAtMs, newDays ->
                    repository.syncRemoteUserStatusLocally(username, isBlocked, newExpiresAtMs, newDays)
                    onComplete?.invoke(isBlocked)
                }
            )
        }
    }

    // --- PSA CONTACT LEADS & ACHIEVEMENTS ---

    fun addPsaLead(
        name: String,
        phone: String,
        address: String = "",
        neighborhood: String = "",
        source: String = "Referido",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val lead = PsaContactLead(
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                neighborhood = neighborhood.trim(),
                source = source.trim().ifBlank { "Referido" },
                notes = notes.trim()
            )
            repository.insertPsaLead(lead)
        }
    }

    fun updatePsaLead(lead: PsaContactLead) {
        viewModelScope.launch {
            repository.updatePsaLead(lead)
        }
    }

    fun deletePsaLead(lead: PsaContactLead) {
        viewModelScope.launch {
            repository.deletePsaLead(lead)
        }
    }

    fun deletePsaLeadById(id: String) {
        viewModelScope.launch {
            repository.deletePsaLeadById(id)
        }
    }

    fun logPsaLeadInteraction(
        lead: PsaContactLead,
        channel: String, // "CALL", "MESSAGE"
        result: String,  // "SUCCESS", "NO_ANSWER", "RESCHEDULED", "REJECTED"
        note: String = "",
        rescheduleDate: String = ""
    ) {
        viewModelScope.launch {
            repository.logContactInteraction(lead, channel, result, note, rescheduleDate)
        }
    }

    fun convertLeadToAppointment(
        context: Context,
        lead: PsaContactLead,
        scheduledDate: String,
        scheduledTime: String,
        modelInterest: String = "PSA Senior 4",
        notes: String = ""
    ) {
        viewModelScope.launch {
            // 1. Log interaction as SUCCESS / Cita
            repository.logContactInteraction(
                lead = lead,
                channel = lead.lastContactChannel.ifBlank { "CALL" },
                result = "SUCCESS",
                note = "Cita agendada para el $scheduledDate $scheduledTime. $notes".trim(),
                rescheduleDate = scheduledDate
            )
            // 2. Book visit in Agenda
            bookPsaVisit(
                context = context,
                customerName = lead.name,
                phone = lead.phone,
                address = lead.address,
                neighborhood = lead.neighborhood,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                modelInterest = modelInterest,
                notes = "Prospecto originado de ${lead.source}. ${lead.notes} $notes".trim()
            )
        }
    }

    fun batchUpdatePsaLeadsStatus(
        leadIds: Set<String>,
        newStatus: com.example.data.model.PsaLeadStatus,
        rescheduleDate: String = "",
        rescheduleNote: String = ""
    ) {
        if (leadIds.isEmpty()) return
        viewModelScope.launch {
            repository.batchUpdatePsaLeadsStatus(
                leadIds = leadIds.toList(),
                newStatus = newStatus,
                rescheduleDate = rescheduleDate,
                rescheduleNote = rescheduleNote
            )
        }
    }

    fun batchReschedulePsaLeads(
        leadIds: Set<String>,
        rescheduleDate: String,
        rescheduleNote: String = ""
    ) {
        if (leadIds.isEmpty() || rescheduleDate.isBlank()) return
        viewModelScope.launch {
            repository.batchReschedulePsaLeads(
                leadIds = leadIds.toList(),
                rescheduleDate = rescheduleDate,
                rescheduleNote = rescheduleNote
            )
        }
    }

    fun batchDeletePsaLeads(leadIds: Set<String>) {
        if (leadIds.isEmpty()) return
        viewModelScope.launch {
            repository.batchDeletePsaLeads(leadIds.toList())
        }
    }
}

data class PsaSyncUiState(
    val isSyncing: Boolean = false,
    val lastSyncedDate: String = "",
    val lastSyncedSource: String = "",
    val statusMessage: String = "",
    val isLiveVerified: Boolean = false,
    val requiresPrivatePortal: Boolean = false
)


