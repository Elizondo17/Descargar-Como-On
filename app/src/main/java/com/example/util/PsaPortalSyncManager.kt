package com.example.util

import android.content.Context
import com.example.data.model.PsaPricingConfig
import com.example.data.repository.PsaFinancingConfig
import com.example.ui.screens.psa.PsaPriceVariation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

data class PsaPortalCredentials(
    val dipCenter: String = "",
    val dipNumber: String = "",
    val password: String = "",
    val rememberCredentials: Boolean = true,
    val lastSyncDate: String = "",
    val lastSyncTimestampMs: Long = 0L,
    val lastSyncSource: String = "",
    val isConnected: Boolean = false,
    val distributorName: String = "",
    val distributorCategory: String = "Distribuidor Independiente PSA (DIP)",
    val selectedCountryCode: String = "AR"
)

data class PsaPromotionInfo(
    val title: String,
    val description: String,
    val discountPercent: Float = 0f,
    val validUntil: String = "Fin de mes",
    val promoType: String = "Financiación"
)

data class PsaPortalSyncResult(
    val success: Boolean,
    val message: String,
    val distributorName: String = "",
    val updatedPricingConfig: PsaPricingConfig? = null,
    val updatedFinancingConfig: PsaFinancingConfig? = null,
    val activePromotions: List<PsaPromotionInfo> = emptyList(),
    val variations: List<PsaPriceVariation> = emptyList(),
    val requiresPrivateDipPortal: Boolean = false
)

object PsaPortalSyncManager {
    private const val PREFS_NAME = "psa_portal_distributor_prefs"
    private const val KEY_DIP_CENTER = "portal_dip_center"
    private const val KEY_DIP = "portal_dip_number"
    private const val KEY_PASS = "portal_password"
    private const val KEY_REMEMBER = "portal_remember"
    private const val KEY_LAST_SYNC = "portal_last_sync_date"
    private const val KEY_LAST_SYNC_MS = "portal_last_sync_ms"
    private const val KEY_LAST_SYNC_SOURCE = "portal_last_sync_source"
    private const val KEY_IS_CONNECTED = "portal_is_connected"
    private const val KEY_DISTRIBUTOR_NAME = "portal_dist_name"
    private const val KEY_COUNTRY_CODE = "portal_country_code"

    @Volatile
    private var cachedPromotions: List<PsaPromotionInfo> = emptyList()

    fun getSelectedCountryCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_COUNTRY_CODE, "AR") ?: "AR"
    }

    fun getSelectedCountry(context: Context): PsaCountryConfig {
        return PsaCountryRegistry.getByCode(getSelectedCountryCode(context))
    }

    fun saveSelectedCountryCode(context: Context, countryCode: String): PsaCountryConfig {
        val normalized = PsaCountryRegistry.getByCode(countryCode)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COUNTRY_CODE, normalized.code)
            .apply()
        return normalized
    }

    fun getCredentials(context: Context): PsaPortalCredentials {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val remember = prefs.getBoolean(KEY_REMEMBER, true)
        val center = prefs.getString(KEY_DIP_CENTER, "") ?: ""
        val dip = prefs.getString(KEY_DIP, "") ?: ""
        val pass = if (remember) prefs.getString(KEY_PASS, "") ?: "" else ""
        val lastSync = prefs.getString(KEY_LAST_SYNC, "") ?: ""
        val lastSyncMs = prefs.getLong(KEY_LAST_SYNC_MS, 0L)
        val lastSource = prefs.getString(KEY_LAST_SYNC_SOURCE, "") ?: ""
        val isConnected = prefs.getBoolean(KEY_IS_CONNECTED, false)
        val name = prefs.getString(KEY_DISTRIBUTOR_NAME, "Distribuidor PSA") ?: "Distribuidor PSA"
        val countryCode = prefs.getString(KEY_COUNTRY_CODE, "AR") ?: "AR"

        return PsaPortalCredentials(
            dipCenter = center,
            dipNumber = dip,
            password = pass,
            rememberCredentials = remember,
            lastSyncDate = lastSync,
            lastSyncTimestampMs = lastSyncMs,
            lastSyncSource = lastSource,
            isConnected = isConnected,
            distributorName = name,
            selectedCountryCode = countryCode
        )
    }

    fun saveCredentials(context: Context, credentials: PsaPortalCredentials) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DIP_CENTER, credentials.dipCenter)
            .putString(KEY_DIP, credentials.dipNumber)
            .putString(KEY_PASS, if (credentials.rememberCredentials) credentials.password else "")
            .putBoolean(KEY_REMEMBER, credentials.rememberCredentials)
            .putString(KEY_LAST_SYNC, credentials.lastSyncDate)
            .putLong(KEY_LAST_SYNC_MS, credentials.lastSyncTimestampMs)
            .putString(KEY_LAST_SYNC_SOURCE, credentials.lastSyncSource)
            .putBoolean(KEY_IS_CONNECTED, credentials.isConnected)
            .putString(KEY_DISTRIBUTOR_NAME, credentials.distributorName)
            .putString(KEY_COUNTRY_CODE, PsaCountryRegistry.getByCode(credentials.selectedCountryCode).code)
            .apply()
    }

    fun needsAutoSync(context: Context, countryCode: String): Boolean {
        val country = PsaCountryRegistry.getByCode(countryCode)
        if (!country.hasPublicStore) return false
        if (hasRecentDipConfirmation(context, country.code)) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val countrySyncMs = prefs.getLong("country_sync_ms_${country.code}", 0L)
        val sixHoursMs = 6L * 60L * 60L * 1000L
        return (System.currentTimeMillis() - countrySyncMs) > sixHoursMs
    }

    fun getSavedDipPriceListUrl(context: Context, countryCode: String): String? {
        val code = PsaCountryRegistry.getByCode(countryCode).code
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString("dip_price_list_url_$code", null)
        return saved?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }

    fun saveDipPriceListUrl(context: Context, countryCode: String, url: String) {
        val code = PsaCountryRegistry.getByCode(countryCode).code
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("dip_price_list_url_$code", url.trim())
            .apply()
    }

    fun markCountryDipConfirmed(context: Context, countryCode: String) {
        val code = PsaCountryRegistry.getByCode(countryCode).code
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong("country_dip_confirmed_ms_$code", System.currentTimeMillis())
            .apply()
        markCountrySynced(context, code, "Portal DIP (Confirmado)")
    }

    fun hasRecentDipConfirmation(context: Context, countryCode: String): Boolean {
        val code = PsaCountryRegistry.getByCode(countryCode).code
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val confirmedMs = prefs.getLong("country_dip_confirmed_ms_$code", 0L)
        val fourteenDaysMs = 14L * 24L * 60L * 60L * 1000L
        return confirmedMs > 0L && (System.currentTimeMillis() - confirmedMs) < fourteenDaysMs
    }

    fun markCountrySynced(context: Context, countryCode: String, sourceLabel: String) {
        val country = PsaCountryRegistry.getByCode(countryCode)
        val nowMs = System.currentTimeMillis()
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", country.toLocale()).format(Date(nowMs))
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("country_sync_ms_${country.code}", nowMs)
            .putString("country_sync_date_${country.code}", nowStr)
            .putString("country_sync_source_${country.code}", sourceLabel)
            .putString(KEY_LAST_SYNC, nowStr)
            .putLong(KEY_LAST_SYNC_MS, nowMs)
            .putString(KEY_LAST_SYNC_SOURCE, sourceLabel)
            .apply()
    }

    fun getCountryLastSyncSummary(context: Context, countryCode: String): Pair<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val date = prefs.getString("country_sync_date_${countryCode.uppercase()}", "") ?: ""
        val source = prefs.getString("country_sync_source_${countryCode.uppercase()}", "") ?: ""
        return date to source
    }

    /**
     * Synchronizes real official catalog pricing for the active country and saves DIP credentials if provided.
     * NEVER overwrites prices with hardcoded fake numbers.
     */
    suspend fun executePortalSync(
        context: Context,
        dipCenter: String,
        dipNumber: String,
        password: String,
        rememberCredentials: Boolean,
        currentPricing: PsaPricingConfig,
        countryCode: String = getSelectedCountryCode(context)
    ): PsaPortalSyncResult = withContext(Dispatchers.IO) {
        val country = PsaCountryRegistry.getByCode(countryCode)
        val cleanCenter = dipCenter.trim().filter { it.isDigit() }
        val cleanDip = dipNumber.trim().filter { it.isDigit() }
        val cleanPass = password.trim()

        val hasCredentials = cleanDip.length >= 4 && cleanPass.isNotEmpty()
        val existing = getCredentials(context)

        val liveResult = PsaOfficialCatalogFetcher.fetchLiveCatalog(country, currentPricing)
        val nowMs = System.currentTimeMillis()
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", country.toLocale()).format(Date(nowMs))
        val distributorName = if (cleanDip.isNotBlank()) "Distribuidor DIP #$cleanDip" else existing.distributorName

        if (liveResult.success && liveResult.updatedPricing != null) {
            val updatedPricing = liveResult.updatedPricing
            PsaScheduleManager.savePsaPricingConfigForCountry(context, country.code, updatedPricing)
            PsaStockManager.syncWithOfficialPvp(context, updatedPricing, country.code)
            markCountrySynced(context, country.code, "Tienda Oficial ${country.name}")

            if (liveResult.promotions.isNotEmpty()) {
                cachedPromotions = liveResult.promotions
            }

            saveCredentials(
                context,
                existing.copy(
                    dipCenter = cleanCenter.ifBlank { existing.dipCenter },
                    dipNumber = cleanDip.ifBlank { existing.dipNumber },
                    password = if (hasCredentials) cleanPass else existing.password,
                    rememberCredentials = rememberCredentials,
                    lastSyncDate = nowStr,
                    lastSyncTimestampMs = nowMs,
                    lastSyncSource = "En vivo • ${country.officialStoreUrl.removePrefix("https://").trimEnd('/')}",
                    isConnected = hasCredentials || existing.isConnected,
                    distributorName = distributorName,
                    selectedCountryCode = country.code
                )
            )

            return@withContext PsaPortalSyncResult(
                success = true,
                message = liveResult.message,
                distributorName = distributorName,
                updatedPricingConfig = updatedPricing,
                activePromotions = liveResult.promotions,
                variations = liveResult.variations
            )
        } else {
            // Even if public HTTP catalog isn't available (e.g. CL/BO/PY/PE/CO/EC/ES), still save valid DIP credentials for WebView auto-login!
            if (hasCredentials) {
                saveCredentials(
                    context,
                    existing.copy(
                        dipCenter = cleanCenter,
                        dipNumber = cleanDip,
                        password = cleanPass,
                        rememberCredentials = rememberCredentials,
                        isConnected = true,
                        distributorName = distributorName,
                        selectedCountryCode = country.code
                    )
                )
            }
            return@withContext PsaPortalSyncResult(
                success = false,
                message = liveResult.message,
                distributorName = distributorName,
                requiresPrivateDipPortal = liveResult.requiresPrivateDipPortal
            )
        }
    }

    fun getActivePromotions(): List<PsaPromotionInfo> {
        return cachedPromotions.ifEmpty {
            listOf(
                PsaPromotionInfo(
                    title = "💳 Cuotas en Portal y Tienda PSA",
                    description = "Consulta la vigencia exacta de cuotas sin interés en el catálogo oficial o desde Portal DIP.",
                    discountPercent = 0f,
                    validUntil = "Consultar en vivo",
                    promoType = "Financiación"
                )
            )
        }
    }
}
