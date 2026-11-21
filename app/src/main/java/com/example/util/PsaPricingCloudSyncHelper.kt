package com.example.util

import android.content.Context
import com.example.data.model.PsaComparisonType
import com.example.data.model.PsaPricingConfig
import com.example.data.model.PsaPurifierModel
import com.example.data.repository.PsaFinancingConfig
import org.json.JSONArray
import org.json.JSONObject

data class PsaCountryPricingSnapshot(
    val countryCode: String,
    val versionMs: Long,
    val updatedBy: String,
    val pricingConfig: PsaPricingConfig,
    val financingConfig: PsaFinancingConfig
)

enum class AdminPricingSyncDecision {
    AUTO_APPLY,
    PROMPT_SELLER,
    NO_OP
}

/**
 * Serializes, tracks, and evaluates cloud-synced PSA prices & financing per country.
 * - Sellers who have NOT locally modified values receive admin updates automatically.
 * - Sellers who DID edit values locally receive a prompt to accept or keep their own values.
 */
object PsaPricingCloudSyncHelper {

    private const val PREFS_NAME = "psa_cloud_pricing_sync_prefs"
    private const val KEY_LATEST_CLOUD_PAYLOAD_JSON = "latest_cloud_payload_json"

    fun evaluateSyncDecision(
        hasLocalOverride: Boolean,
        lastAppliedAdminVersionMs: Long,
        dismissedAdminVersionMs: Long,
        incomingVersionMs: Long
    ): AdminPricingSyncDecision {
        if (incomingVersionMs <= 0L || incomingVersionMs <= lastAppliedAdminVersionMs) {
            return AdminPricingSyncDecision.NO_OP
        }
        return if (!hasLocalOverride) {
            AdminPricingSyncDecision.AUTO_APPLY
        } else if (incomingVersionMs > dismissedAdminVersionMs) {
            AdminPricingSyncDecision.PROMPT_SELLER
        } else {
            AdminPricingSyncDecision.NO_OP
        }
    }

    private fun overrideKey(countryCode: String) = "local_override_${countryCode.uppercase()}"
    private fun appliedVersionKey(countryCode: String) = "applied_admin_ver_${countryCode.uppercase()}"
    private fun dismissedVersionKey(countryCode: String) = "dismissed_admin_ver_${countryCode.uppercase()}"

    fun hasLocalPricingOverride(context: Context, countryCode: String): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(overrideKey(countryCode), false)
    }

    fun setLocalPricingOverride(context: Context, countryCode: String, hasOverride: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(overrideKey(countryCode), hasOverride)
            .apply()
    }

    fun getAppliedAdminVersion(context: Context, countryCode: String): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(appliedVersionKey(countryCode), 0L)
    }

    fun setAppliedAdminVersion(context: Context, countryCode: String, versionMs: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(appliedVersionKey(countryCode), versionMs)
            .apply()
    }

    fun getDismissedAdminVersion(context: Context, countryCode: String): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(dismissedVersionKey(countryCode), 0L)
    }

    fun setDismissedAdminVersion(context: Context, countryCode: String, versionMs: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(dismissedVersionKey(countryCode), versionMs)
            .apply()
    }

    fun saveCachedCloudPayloadJson(context: Context, payloadJson: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LATEST_CLOUD_PAYLOAD_JSON, payloadJson)
            .apply()
    }

    fun getCachedCloudSnapshots(context: Context): Map<String, PsaCountryPricingSnapshot> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LATEST_CLOUD_PAYLOAD_JSON, "")
            .orEmpty()
        return parseAllCountriesPayload(raw)
    }

    fun clearSellerOverrideState(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun mergeAndSerializeCountrySnapshot(
        existingPayloadJson: String,
        snapshot: PsaCountryPricingSnapshot
    ): String {
        val map = parseAllCountriesPayload(existingPayloadJson).toMutableMap()
        map[snapshot.countryCode.uppercase()] = snapshot
        val root = JSONObject()
        val countriesObj = JSONObject()
        map.forEach { (code, snap) ->
            countriesObj.put(code, serializeSnapshot(snap))
        }
        root.put("countries", countriesObj)
        return root.toString()
    }

    fun parseAllCountriesPayload(payloadJson: String): Map<String, PsaCountryPricingSnapshot> {
        if (payloadJson.isBlank()) return emptyMap()
        val result = mutableMapOf<String, PsaCountryPricingSnapshot>()
        try {
            val root = JSONObject(payloadJson)
            val countriesObj = root.optJSONObject("countries") ?: return emptyMap()
            val keys = countriesObj.keys()
            while (keys.hasNext()) {
                val code = keys.next().uppercase()
                val obj = countriesObj.optJSONObject(code) ?: continue
                parseSnapshot(code, obj)?.let { snap ->
                    result[code] = snap
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun serializePurifierList(list: List<PsaPurifierModel>): JSONArray {
        val array = JSONArray()
        list.forEach { m ->
            array.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("price", m.price)
                    put("installmentsCount", m.installmentsCount)
                    put("lifespanMonths", m.lifespanMonths)
                    put("description", m.description)
                    put("category", m.category)
                    put("officialUrl", m.officialUrl)
                    put("anmatRegistration", m.anmatRegistration)
                    put("highlightFeature", m.highlightFeature)
                }
            )
        }
        return array
    }

    private fun deserializePurifierList(arr: JSONArray?): List<PsaPurifierModel> {
        if (arr == null) return emptyList()
        val list = mutableListOf<PsaPurifierModel>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            list.add(
                PsaPurifierModel(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    price = obj.optDouble("price", 0.0),
                    installmentsCount = obj.optInt("installmentsCount", 12),
                    lifespanMonths = obj.optInt("lifespanMonths", 36),
                    description = obj.optString("description", ""),
                    category = obj.optString("category", "Cocina"),
                    officialUrl = obj.optString("officialUrl", "https://www.psa.com.ar"),
                    anmatRegistration = obj.optString("anmatRegistration", "Oficial PSA"),
                    highlightFeature = obj.optString("highlightFeature", "Catálogo oficial PSA")
                )
            )
        }
        return list
    }

    private fun serializeSnapshot(snapshot: PsaCountryPricingSnapshot): JSONObject {
        val p = snapshot.pricingConfig
        val f = snapshot.financingConfig
        return JSONObject().apply {
            put("countryCode", snapshot.countryCode.uppercase())
            put("versionMs", snapshot.versionMs)
            put("updatedBy", snapshot.updatedBy)
            put("pricing", JSONObject().apply {
                put("bidonPrice", p.bidonPrice)
                put("bottlePrice", p.bottlePrice)
                put("senior4Price", p.senior4Price)
                put("quantumPrice", p.quantumPrice)
                put("senikPrice", p.senikPrice)
                put("cromaPrice", p.cromaPrice)
                put("veroPrice", p.veroPrice)
                put("s1000Price", p.s1000Price)
                put("miniPrice", p.miniPrice)
                put("c3Price", p.c3Price)
                put("duchaPrice", p.duchaPrice)
                put("descalcificadorPrice", p.descalcificadorPrice)
                put("domusPrice", p.domusPrice)
                put("maximaPrice", p.maximaPrice)
                put("piletaPrice", p.piletaPrice)
                put("airePrice", p.airePrice)
                put("ropotPrice", p.ropotPrice)
                put("rotechPrice", p.rotechPrice)
                put("botellaTermicaPrice", p.botellaTermicaPrice)
                put("repuestoSeniorPrice", p.repuestoSeniorPrice)
                put("installmentsCount", p.installmentsCount)
                put("defaultLifespanMonths", p.defaultLifespanMonths)
                put("defaultComparisonType", p.defaultComparisonType.name)
                put("weeklyVisitsGoal", p.weeklyVisitsGoal)
                put("customPurifiers", serializePurifierList(p.customPurifiers))
                put("liveCatalogItems", serializePurifierList(p.liveCatalogItems))
            })
            put("financing", JSONObject().apply {
                put("interest3", f.defaultInterest3Months.toDouble())
                put("interest6", f.defaultInterest6Months.toDouble())
                put("interest12", f.defaultInterest12Months.toDouble())
                put("interest18", f.defaultInterest18Months.toDouble())
                put("advancePct", f.suggestedAdvancePercentage.toDouble())
            })
        }
    }

    private fun parseSnapshot(code: String, obj: JSONObject): PsaCountryPricingSnapshot? {
        val pricingObj = obj.optJSONObject("pricing") ?: return null
        val financingObj = obj.optJSONObject("financing")
        val baseline = PsaCountryRegistry.defaultPricingForCountry(code)
        val comparisonRaw = pricingObj.optString("defaultComparisonType", baseline.defaultComparisonType.name)
        val comparisonType = try {
            PsaComparisonType.valueOf(comparisonRaw)
        } catch (_: Exception) {
            baseline.defaultComparisonType
        }
        val pricing = PsaPricingConfig(
            bidonPrice = pricingObj.optDouble("bidonPrice", baseline.bidonPrice),
            bottlePrice = pricingObj.optDouble("bottlePrice", baseline.bottlePrice),
            senior4Price = pricingObj.optDouble("senior4Price", baseline.senior4Price),
            quantumPrice = pricingObj.optDouble("quantumPrice", baseline.quantumPrice),
            senikPrice = pricingObj.optDouble("senikPrice", baseline.senikPrice),
            cromaPrice = pricingObj.optDouble("cromaPrice", baseline.cromaPrice),
            veroPrice = pricingObj.optDouble("veroPrice", baseline.veroPrice),
            s1000Price = pricingObj.optDouble("s1000Price", baseline.s1000Price),
            miniPrice = pricingObj.optDouble("miniPrice", baseline.miniPrice),
            c3Price = pricingObj.optDouble("c3Price", baseline.c3Price),
            duchaPrice = pricingObj.optDouble("duchaPrice", baseline.duchaPrice),
            descalcificadorPrice = pricingObj.optDouble("descalcificadorPrice", baseline.descalcificadorPrice),
            domusPrice = pricingObj.optDouble("domusPrice", baseline.domusPrice),
            maximaPrice = pricingObj.optDouble("maximaPrice", baseline.maximaPrice),
            piletaPrice = pricingObj.optDouble("piletaPrice", baseline.piletaPrice),
            airePrice = pricingObj.optDouble("airePrice", baseline.airePrice),
            ropotPrice = pricingObj.optDouble("ropotPrice", baseline.ropotPrice),
            rotechPrice = pricingObj.optDouble("rotechPrice", baseline.rotechPrice),
            botellaTermicaPrice = pricingObj.optDouble("botellaTermicaPrice", baseline.botellaTermicaPrice),
            repuestoSeniorPrice = pricingObj.optDouble("repuestoSeniorPrice", baseline.repuestoSeniorPrice),
            installmentsCount = pricingObj.optInt("installmentsCount", baseline.installmentsCount).coerceIn(1, 36),
            defaultLifespanMonths = pricingObj.optInt("defaultLifespanMonths", baseline.defaultLifespanMonths).coerceIn(6, 120),
            defaultComparisonType = comparisonType,
            customPurifiers = deserializePurifierList(pricingObj.optJSONArray("customPurifiers")),
            liveCatalogItems = deserializePurifierList(pricingObj.optJSONArray("liveCatalogItems")),
            weeklyVisitsGoal = pricingObj.optInt("weeklyVisitsGoal", baseline.weeklyVisitsGoal).coerceIn(1, 200)
        )
        val financing = if (financingObj != null) {
            PsaFinancingConfig(
                defaultInterest3Months = financingObj.optDouble("interest3", 10.0).toFloat(),
                defaultInterest6Months = financingObj.optDouble("interest6", 20.0).toFloat(),
                defaultInterest12Months = financingObj.optDouble("interest12", 35.0).toFloat(),
                defaultInterest18Months = financingObj.optDouble("interest18", 50.0).toFloat(),
                suggestedAdvancePercentage = financingObj.optDouble("advancePct", 20.0).toFloat()
            )
        } else {
            PsaFinancingConfig()
        }
        return PsaCountryPricingSnapshot(
            countryCode = code,
            versionMs = obj.optLong("versionMs", 0L),
            updatedBy = obj.optString("updatedBy", ""),
            pricingConfig = pricing,
            financingConfig = financing
        )
    }
}
