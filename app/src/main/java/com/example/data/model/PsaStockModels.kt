package com.example.data.model

data class PsaStockItem(
    val id: String,
    val name: String,
    val category: String, // "Purificador", "Repuesto", "Accesorio"
    val quantity: Int = 0,
    val officialPvp: Double,
    val standardDistributorCost: Double = officialPvp * 0.60, // 40% margen oficial estándar de distribuidor PSA
    val customCost: Double? = null,
    val customSalePrice: Double? = null,
    val minStockAlert: Int = 1,
    val notes: String = ""
) {
    val effectiveCost: Double
        get() = customCost ?: standardDistributorCost

    val effectiveSalePrice: Double
        get() = customSalePrice ?: officialPvp

    val unitProfit: Double
        get() = effectiveSalePrice - effectiveCost

    val profitMarginPct: Double
        get() = if (effectiveCost > 0) (unitProfit / effectiveCost) * 100.0 else 0.0

    val totalInvestment: Double
        get() = quantity * effectiveCost

    val totalExpectedProfit: Double
        get() = quantity * unitProfit

    val totalGrossSalesValue: Double
        get() = quantity * effectiveSalePrice
}

data class PsaStockSummary(
    val totalUnits: Int,
    val totalInvestment: Double,
    val totalExpectedProfit: Double,
    val totalGrossSalesValue: Double,
    val lowStockCount: Int
)
