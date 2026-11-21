package com.example.util

import android.content.Context
import com.example.data.model.PsaPricingConfig
import com.example.data.model.PsaStockItem
import com.example.data.model.PsaStockSummary
import org.json.JSONArray
import org.json.JSONObject

object PsaStockManager {
    private const val KEY_STOCK_ITEMS = "stock_items_json"

    private fun getPrefs(context: Context, countryCode: String = PsaPortalSyncManager.getSelectedCountryCode(context)) =
        context.getSharedPreferences("psa_distributor_stock_${PsaCountryRegistry.getByCode(countryCode).code}", Context.MODE_PRIVATE)

    fun getStockItems(
        context: Context,
        pricingConfig: PsaPricingConfig,
        countryCode: String = PsaPortalSyncManager.getSelectedCountryCode(context)
    ): List<PsaStockItem> {
        val prefs = getPrefs(context, countryCode)
        val rawJson = prefs.getString(KEY_STOCK_ITEMS, null)
        if (rawJson.isNullOrBlank()) {
            val defaults = getDefaultStockList(pricingConfig)
            saveStockItems(context, defaults, countryCode)
            return defaults
        }

        return try {
            val array = JSONArray(rawJson)
            val items = mutableListOf<PsaStockItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val pvp = obj.optDouble("officialPvp", pricingConfig.senior4Price)
                items.add(
                    PsaStockItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        category = obj.optString("category", "Purificador"),
                        quantity = obj.optInt("quantity", 0),
                        officialPvp = pvp,
                        standardDistributorCost = obj.optDouble("standardDistributorCost", pvp * 0.60),
                        customCost = if (obj.has("customCost") && !obj.isNull("customCost")) obj.getDouble("customCost") else null,
                        customSalePrice = if (obj.has("customSalePrice") && !obj.isNull("customSalePrice")) obj.getDouble("customSalePrice") else null,
                        minStockAlert = obj.optInt("minStockAlert", 1),
                        notes = obj.optString("notes", "")
                    )
                )
            }
            if (items.isEmpty()) getDefaultStockList(pricingConfig) else items
        } catch (_: Exception) {
            getDefaultStockList(pricingConfig)
        }
    }

    fun saveStockItems(
        context: Context,
        items: List<PsaStockItem>,
        countryCode: String = PsaPortalSyncManager.getSelectedCountryCode(context)
    ) {
        val prefs = getPrefs(context, countryCode)
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("category", item.category)
            obj.put("quantity", item.quantity)
            obj.put("officialPvp", item.officialPvp)
            obj.put("standardDistributorCost", item.standardDistributorCost)
            if (item.customCost != null) obj.put("customCost", item.customCost) else obj.put("customCost", JSONObject.NULL)
            if (item.customSalePrice != null) obj.put("customSalePrice", item.customSalePrice) else obj.put("customSalePrice", JSONObject.NULL)
            obj.put("minStockAlert", item.minStockAlert)
            obj.put("notes", item.notes)
            array.put(obj)
        }
        prefs.edit().putString(KEY_STOCK_ITEMS, array.toString()).apply()
    }

    fun updateItemQuantity(context: Context, itemId: String, delta: Int, pricingConfig: PsaPricingConfig): List<PsaStockItem> {
        val current = getStockItems(context, pricingConfig).toMutableList()
        val index = current.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = current[index]
            val newQty = (item.quantity + delta).coerceAtLeast(0)
            current[index] = item.copy(quantity = newQty)
            saveStockItems(context, current)
        }
        return current
    }

    fun setItemQuantity(context: Context, itemId: String, quantity: Int, pricingConfig: PsaPricingConfig): List<PsaStockItem> {
        val current = getStockItems(context, pricingConfig).toMutableList()
        val index = current.indexOfFirst { it.id == itemId }
        if (index != -1) {
            current[index] = current[index].copy(quantity = quantity.coerceAtLeast(0))
            saveStockItems(context, current)
        }
        return current
    }

    fun updateItemPrices(
        context: Context,
        itemId: String,
        customCost: Double?,
        customSalePrice: Double?,
        pricingConfig: PsaPricingConfig
    ): List<PsaStockItem> {
        val current = getStockItems(context, pricingConfig).toMutableList()
        val index = current.indexOfFirst { it.id == itemId }
        if (index != -1) {
            current[index] = current[index].copy(
                customCost = customCost,
                customSalePrice = customSalePrice
            )
            saveStockItems(context, current)
        }
        return current
    }

    fun calculateSummary(items: List<PsaStockItem>): PsaStockSummary {
        val totalUnits = items.sumOf { it.quantity }
        val totalInvestment = items.sumOf { it.totalInvestment }
        val totalProfit = items.sumOf { it.totalExpectedProfit }
        val totalGross = items.sumOf { it.totalGrossSalesValue }
        val lowStock = items.count { it.quantity in 1..it.minStockAlert }
        return PsaStockSummary(
            totalUnits = totalUnits,
            totalInvestment = totalInvestment,
            totalExpectedProfit = totalProfit,
            totalGrossSalesValue = totalGross,
            lowStockCount = lowStock
        )
    }

    fun syncWithOfficialPvp(
        context: Context,
        pricingConfig: PsaPricingConfig,
        countryCode: String = PsaPortalSyncManager.getSelectedCountryCode(context)
    ): List<PsaStockItem> {
        val current = getStockItems(context, pricingConfig, countryCode).toMutableList()
        val officialCatalog = pricingConfig.getAllPurifiers()

        val updated = current.map { item ->
            val match = officialCatalog.find { it.id == item.id }
            if (match != null) {
                val updatedName = if (item.id == "ducha" && item.name.contains("C3", ignoreCase = true)) {
                    "PSA Ducha (Declorador)"
                } else {
                    item.name
                }
                item.copy(
                    name = updatedName,
                    officialPvp = match.price,
                    standardDistributorCost = match.price * 0.60
                )
            } else {
                item
            }
        }.toMutableList()

        // Asegurar que los equipos separados principales existan en la lista de stock
        val existingIds = updated.map { it.id }.toSet()
        for (model in officialCatalog) {
            if (model.id !in existingIds && !model.category.equals("Repuestos", ignoreCase = true)) {
                updated.add(
                    PsaStockItem(
                        id = model.id,
                        name = model.name,
                        category = model.category,
                        quantity = 0,
                        officialPvp = model.price,
                        standardDistributorCost = model.price * 0.60
                    )
                )
            }
        }

        saveStockItems(context, updated, countryCode)
        return updated
    }

    private fun getDefaultStockList(pricingConfig: PsaPricingConfig): List<PsaStockItem> {
        return listOf(
            PsaStockItem(
                id = "senior4",
                name = "PSA Senior 4 (Sobre Mesada)",
                category = "Cocina",
                quantity = 2,
                officialPvp = pricingConfig.senior4Price,
                standardDistributorCost = pricingConfig.senior4Price * 0.60
            ),
            PsaStockItem(
                id = "senik",
                name = "PSA Senik",
                category = "Cocina",
                quantity = 1,
                officialPvp = pricingConfig.senikPrice,
                standardDistributorCost = pricingConfig.senikPrice * 0.60
            ),
            PsaStockItem(
                id = "quantum",
                name = "PSA Quantum",
                category = "Cocina",
                quantity = 0,
                officialPvp = pricingConfig.quantumPrice,
                standardDistributorCost = pricingConfig.quantumPrice * 0.60
            ),
            PsaStockItem(
                id = "croma",
                name = "PSA Senior Clásico / Croma",
                category = "Cocina",
                quantity = 1,
                officialPvp = pricingConfig.cromaPrice,
                standardDistributorCost = pricingConfig.cromaPrice * 0.60
            ),
            PsaStockItem(
                id = "vero",
                name = "PSA Vero",
                category = "Cocina",
                quantity = 0,
                officialPvp = pricingConfig.veroPrice,
                standardDistributorCost = pricingConfig.veroPrice * 0.60
            ),
            PsaStockItem(
                id = "s1000",
                name = "PSA S-1000 II",
                category = "Cocina",
                quantity = 1,
                officialPvp = pricingConfig.s1000Price,
                standardDistributorCost = pricingConfig.s1000Price * 0.60
            ),
            PsaStockItem(
                id = "mini",
                name = "PSA Mini (Compacto)",
                category = "Cocina",
                quantity = 0,
                officialPvp = pricingConfig.miniPrice,
                standardDistributorCost = pricingConfig.miniPrice * 0.60
            ),
            PsaStockItem(
                id = "c3",
                name = "PSA C3 (Baño / Lavatorio)",
                category = "Baño",
                quantity = 1,
                officialPvp = pricingConfig.c3Price,
                standardDistributorCost = pricingConfig.c3Price * 0.60
            ),
            PsaStockItem(
                id = "ducha",
                name = "PSA Ducha (Declorador)",
                category = "Baño",
                quantity = 1,
                officialPvp = pricingConfig.duchaPrice,
                standardDistributorCost = pricingConfig.duchaPrice * 0.60
            ),
            PsaStockItem(
                id = "pileta",
                name = "PSA Iontrix3 (Pileta)",
                category = "Pileta",
                quantity = 0,
                officialPvp = pricingConfig.piletaPrice,
                standardDistributorCost = pricingConfig.piletaPrice * 0.60
            ),
            PsaStockItem(
                id = "aire",
                name = "PSA AirPurifier (Aire)",
                category = "Aire",
                quantity = 0,
                officialPvp = pricingConfig.airePrice,
                standardDistributorCost = pricingConfig.airePrice * 0.60
            ),
            PsaStockItem(
                id = "descalcificador",
                name = "PSA Descalcificador R-200",
                category = "Hogar",
                quantity = 0,
                officialPvp = pricingConfig.descalcificadorPrice,
                standardDistributorCost = pricingConfig.descalcificadorPrice * 0.60
            ),
            PsaStockItem(
                id = "domus",
                name = "PSA Domus (Ablandador Automático)",
                category = "Hogar",
                quantity = 0,
                officialPvp = pricingConfig.domusPrice,
                standardDistributorCost = pricingConfig.domusPrice * 0.60
            ),
            PsaStockItem(
                id = "maxima",
                name = "PSA Máxima (Entrada General)",
                category = "Entrada",
                quantity = 0,
                officialPvp = pricingConfig.maximaPrice,
                standardDistributorCost = pricingConfig.maximaPrice * 0.60
            ),
            PsaStockItem(
                id = "ropot",
                name = "PSA Ropot (Ósmosis Inversa)",
                category = "Cocina",
                quantity = 0,
                officialPvp = pricingConfig.ropotPrice,
                standardDistributorCost = pricingConfig.ropotPrice * 0.60
            ),
            PsaStockItem(
                id = "rotech",
                name = "PSA ROtech·UV (Ósmosis + UV)",
                category = "Cocina",
                quantity = 0,
                officialPvp = pricingConfig.rotechPrice,
                standardDistributorCost = pricingConfig.rotechPrice * 0.60
            )
        )
    }
}
