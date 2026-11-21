package com.example.util

import com.example.data.model.PsaPricingConfig
import com.example.data.model.PsaPurifierModel
import com.example.ui.screens.psa.PsaPriceVariation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs

data class LiveOfficialCatalogResult(
    val success: Boolean,
    val country: PsaCountryConfig,
    val message: String,
    val updatedPricing: PsaPricingConfig? = null,
    val extractedProducts: List<PsaPurifierModel> = emptyList(),
    val variations: List<PsaPriceVariation> = emptyList(),
    val promotions: List<PsaPromotionInfo> = emptyList(),
    val requiresPrivateDipPortal: Boolean = false
)

object PsaOfficialCatalogFetcher {

    private val PRODUCT_CARD_SPLIT_REGEX = Regex("""<li[^>]*class="[^"]*product-item[^"]*"[^>]*>""", RegexOption.IGNORE_CASE)
    private val TITLE_REGEX = Regex("""class="product-item-link"[^>]*>(.*?)</a>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val OLD_PRICE_REGEX = Regex("""data-price-type="oldPrice"[^>]*data-price-amount="([0-9.]+)"|data-price-amount="([0-9.]+)"[^>]*data-price-type="oldPrice"""", RegexOption.IGNORE_CASE)
    private val FINAL_PRICE_REGEX = Regex("""data-price-type="finalPrice"[^>]*data-price-amount="([0-9.]+)"|data-price-amount="([0-9.]+)"[^>]*data-price-type="finalPrice"""", RegexOption.IGNORE_CASE)
    private val GTM_DATA_REGEX = Regex("""<script\s+type="text/x-gtm-data"[^>]*>(.*?)</script>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val CUOTAS_REGEX = Regex("""(\d{1,2})\s+cuotas\s+(?:sin\s+inter[eé]s|fijas)""", RegexOption.IGNORE_CASE)

    suspend fun fetchLiveCatalog(
        country: PsaCountryConfig,
        currentPricing: PsaPricingConfig
    ): LiveOfficialCatalogResult = withContext(Dispatchers.IO) {
        if (!country.hasPublicStore) {
            return@withContext LiveOfficialCatalogResult(
                success = false,
                country = country,
                message = "PSA ${country.name} utiliza Lista de Precios en el Portal DIP.",
                requiresPrivateDipPortal = true
            )
        }

        val endpoints = when (country.code) {
            "AR" -> listOf(
                "https://tienda.psa.com.ar/purificadores-de-agua.html",
                "https://tienda.psa.com.ar/repuestos-y-accesorios.html"
            )
            "UY" -> listOf(
                "https://tienda.psa.com.uy/purificadores-de-agua.html",
                "https://tienda.psa.com.uy/repuestos-y-accesorios.html"
            )
            else -> listOf(country.officialStoreUrl)
        }

        val discoveredMap = linkedMapOf<String, PsaPurifierModel>()
        var detectedZeroInterestInstallments: Int? = null
        var anyFetchSucceeded = false
        var lastError = ""

        for (endpoint in endpoints) {
            try {
                val html = downloadHtml(endpoint)
                if (html.isNotBlank()) {
                    anyFetchSucceeded = true
                    parseProductsFromHtml(html, endpoint, country, discoveredMap)

                    if (detectedZeroInterestInstallments == null) {
                        val cMatch = CUOTAS_REGEX.find(html)
                        val cuotas = cMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
                        if (cuotas != null && cuotas in 3..24) {
                            detectedZeroInterestInstallments = cuotas
                        }
                    }
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Error de conexión"
            }
        }

        if (!anyFetchSucceeded || discoveredMap.isEmpty()) {
            val errorDetail = if (lastError.isNotBlank()) " ($lastError)" else ""
            return@withContext LiveOfficialCatalogResult(
                success = false,
                country = country,
                message = "No se pudo leer la tienda de ${country.name}$errorDetail. Usa Lista de Precios en Portal DIP."
            )
        }

        val products = discoveredMap.values.toList()
        val updatedPricing = mergeProductsIntoPricing(
            current = currentPricing,
            products = products,
            countryCode = country.code,
            detectedInstallments = detectedZeroInterestInstallments
        )
        val variations = computeVariations(currentPricing, updatedPricing)

        val promotions = buildList {
            if (detectedZeroInterestInstallments != null) {
                add(
                    PsaPromotionInfo(
                        title = "Financiación oficial: $detectedZeroInterestInstallments cuotas",
                        description = "Consultada en ${country.officialStoreUrl.removePrefix("https://").trimEnd('/')}.",
                        discountPercent = 0f,
                        validUntil = "Vigente",
                        promoType = "Financiación"
                    )
                )
            }
        }

        LiveOfficialCatalogResult(
            success = true,
            country = country,
            message = "Sincronizados ${products.size} PVP oficiales (${country.name}).",
            updatedPricing = updatedPricing,
            extractedProducts = products,
            variations = variations,
            promotions = promotions,
            requiresPrivateDipPortal = false
        )
    }

    private fun downloadHtml(urlString: String): String {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 9000
            readTimeout = 10000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/130.0 Mobile Safari/537.36")
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            setRequestProperty("Accept-Language", "es-AR,es;q=0.9")
        }
        return try {
            val code = conn.responseCode
            if (code in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            } else {
                ""
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseProductsFromHtml(
        html: String,
        sourceUrl: String,
        country: PsaCountryConfig,
        output: MutableMap<String, PsaPurifierModel>
    ) {
        val isAccessories = sourceUrl.contains("repuesto", ignoreCase = true)
        val baseline = PsaCountryRegistry.defaultPricingForCountry(country.code)

        // 1. Primary: Extract per-product card prioritizing official PVP list price (oldPrice) over promo contado (finalPrice)
        val cards = PRODUCT_CARD_SPLIT_REGEX.split(html)
        if (cards.size > 1) {
            for (i in 1 until cards.size) {
                val cardHtml = cards[i].take(6000)
                val rawTitle = TITLE_REGEX.find(cardHtml)?.groupValues?.getOrNull(1)
                    ?.replace(Regex("<[^>]+>"), " ")
                    ?.replace(Regex("\\s+"), " ")
                    ?.trim()
                    .orEmpty()
                if (rawTitle.isBlank()) continue

                val oldPriceMatch = OLD_PRICE_REGEX.find(cardHtml)
                val oldPrice = (oldPriceMatch?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
                    ?: oldPriceMatch?.groupValues?.getOrNull(2))?.toDoubleOrNull()

                val finalPriceMatch = FINAL_PRICE_REGEX.find(cardHtml)
                val finalPrice = (finalPriceMatch?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
                    ?: finalPriceMatch?.groupValues?.getOrNull(2))?.toDoubleOrNull()

                // Strictly prefer oldPrice (official PVP list) if it's >= finalPrice
                val chosenPvp = when {
                    oldPrice != null && finalPrice != null -> maxOf(oldPrice, finalPrice)
                    oldPrice != null -> oldPrice
                    finalPrice != null -> finalPrice
                    else -> null
                } ?: continue

                val modelId = canonicalModelId(rawTitle, "") ?: continue
                if (!isPlausiblePrice(modelId, chosenPvp, baseline)) continue

                val category = inferCategory(rawTitle, isAccessories)
                if (!output.containsKey(modelId)) {
                    output[modelId] = PsaPurifierModel(
                        id = modelId,
                        name = rawTitle,
                        price = chosenPvp,
                        installmentsCount = 12,
                        lifespanMonths = if (category == "Repuestos") 12 else 36,
                        description = "PVP oficial PSA ${country.name}",
                        category = category,
                        capacityLiters = if (category == "Repuestos") 12000 else 36000,
                        officialUrl = sourceUrl,
                        anmatRegistration = "Oficial PSA ${country.code}",
                        highlightFeature = "PVP Lista"
                    )
                }
            }
        }

        // 2. Fallback GTM structured blocks only for models not yet discovered from product cards
        for (match in GTM_DATA_REGEX.findAll(html)) {
            val rawJson = match.groupValues[1].trim()
            if (!rawJson.startsWith("{")) continue
            try {
                val obj = JSONObject(rawJson)
                val name = obj.optString("item_name", "").replace(Regex("\\s+"), " ").trim()
                val sku = obj.optString("item_id", "").trim()
                val price = obj.optDouble("item_price", -1.0)
                if (name.isNotBlank() && price > 0.0) {
                    val modelId = canonicalModelId(name, sku) ?: continue
                    if (!isPlausiblePrice(modelId, price, baseline)) continue
                    val category = inferCategory(name, isAccessories)
                    if (!output.containsKey(modelId)) {
                        output[modelId] = PsaPurifierModel(
                            id = modelId,
                            name = name,
                            price = price,
                            installmentsCount = 12,
                            lifespanMonths = if (category == "Repuestos") 12 else 36,
                            description = "Precio oficial tienda PSA ${country.name}",
                            category = category,
                            capacityLiters = if (category == "Repuestos") 12000 else 36000,
                            officialUrl = sourceUrl,
                            anmatRegistration = if (sku.isNotBlank()) "SKU $sku" else "Oficial PSA ${country.code}",
                            highlightFeature = "Tienda PSA"
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun canonicalModelId(name: String, sku: String): String? {
        val lower = name.lowercase(Locale.ROOT)
        // Nunca permitir que combos o accesorios baratos sobreescriban los equipos principales
        if (lower.contains("combo") || lower.contains("pack") || lower.contains("canje")) {
            return null
        }
        val isPart = lower.contains("repuesto") ||
            lower.contains("recambio") ||
            lower.contains("fipor") ||
            lower.contains("núcleo") ||
            lower.contains("nucleo") ||
            lower.contains("cartucho") ||
            lower.contains("botella") ||
            lower.contains("bidón") ||
            lower.contains("bidon") ||
            lower.contains("manguera") ||
            lower.contains("adaptador") ||
            lower.contains("válvula") ||
            lower.contains("valvula")
        if (isPart) return null

        return when {
            Regex("""\bsenior\s*(?:4|iv)\s*(?:bm|bajo\s*mesada)\b""").containsMatchIn(lower) -> "senior4_bm"
            Regex("""\bsenior\s*(?:4|iv)\b""").containsMatchIn(lower) -> "senior4"
            Regex("""\bsenik\b""").containsMatchIn(lower) -> "senik"
            Regex("""\bquantum\b""").containsMatchIn(lower) -> "quantum"
            Regex("""\bs[-\s]?1000(?:\s*ii)?\b""").containsMatchIn(lower) -> "s1000"
            Regex("""\bvero\b""").containsMatchIn(lower) -> "vero"
            Regex("""\bmini\b""").containsMatchIn(lower) -> "mini"
            Regex("""\b(?:ducha|rinnova)\b""").containsMatchIn(lower) -> "ducha"
            Regex("""\bc3\b""").containsMatchIn(lower) -> "c3"
            Regex("""\bdomus\b""").containsMatchIn(lower) -> "domus"
            Regex("""\b(?:iontrix|pileta|piscina)\b""").containsMatchIn(lower) -> "pileta"
            Regex("""\b(?:air\s*purifier|purificador\s+de\s+aire)\b""").containsMatchIn(lower) -> "aire"
            Regex("""\b(?:descalcificador|r[-\s]?200)\b""").containsMatchIn(lower) -> "descalcificador"
            Regex("""\bm[áa]xima\b""").containsMatchIn(lower) -> "maxima"
            Regex("""\brotech(?:\s*[·.]?\s*uv)?\b""").containsMatchIn(lower) -> "rotech"
            Regex("""\bropot\b""").containsMatchIn(lower) -> "ropot"
            Regex("""\b(?:croma|senior\s*(?:cl[áa]sico|clasico|bianco|grigio|nero))\b""").containsMatchIn(lower) -> "croma"
            else -> null
        }
    }

    private fun isPlausiblePrice(modelId: String, price: Double, baseline: PsaPricingConfig): Boolean {
        val ref = when (modelId) {
            "senior4" -> baseline.senior4Price
            "senior4_bm" -> baseline.senior4Price * 0.88
            "senik" -> baseline.senikPrice
            "quantum" -> baseline.quantumPrice
            "croma" -> baseline.cromaPrice
            "vero" -> baseline.veroPrice
            "s1000" -> baseline.s1000Price
            "mini" -> baseline.miniPrice
            "c3" -> baseline.c3Price
            "ducha" -> baseline.duchaPrice
            "pileta" -> baseline.piletaPrice
            "aire" -> baseline.airePrice
            "descalcificador" -> baseline.descalcificadorPrice
            "domus" -> baseline.domusPrice
            "maxima" -> baseline.maximaPrice
            "ropot" -> baseline.ropotPrice
            "rotech" -> baseline.rotechPrice
            else -> return price >= minOf(baseline.duchaPrice, baseline.c3Price) * 0.35
        }
        return price in (ref * 0.30)..(ref * 3.5)
    }

    private fun inferCategory(name: String, isAccessoriesPage: Boolean): String {
        val lower = name.lowercase(Locale.ROOT)
        return when {
            isAccessoriesPage || lower.contains("repuesto") || lower.contains("fipor") -> "Repuestos"
            lower.contains("c3") || lower.contains("ducha") || lower.contains("rinnova") -> "Baño"
            lower.contains("iontrix") || lower.contains("pileta") || lower.contains("piscina") -> "Pileta"
            lower.contains("airpurifier") || lower.contains("air purifier") || lower.contains("aire") -> "Aire"
            lower.contains("descalcificador") || lower.contains("r-200") || lower.contains("domus") -> "Hogar"
            lower.contains("máxima") || lower.contains("maxima") -> "Entrada"
            else -> "Cocina"
        }
    }

    private fun mergeProductsIntoPricing(
        current: PsaPricingConfig,
        products: List<PsaPurifierModel>,
        countryCode: String,
        detectedInstallments: Int?
    ): PsaPricingConfig {
        val baseline = PsaCountryRegistry.defaultPricingForCountry(countryCode)
        var updated = current
        for (item in products) {
            if (!isPlausiblePrice(item.id, item.price, baseline)) continue
            updated = when (item.id) {
                "senior4" -> updated.copy(senior4Price = item.price)
                "senik" -> updated.copy(senikPrice = item.price)
                "quantum" -> updated.copy(quantumPrice = item.price)
                "croma" -> updated.copy(cromaPrice = item.price)
                "vero" -> updated.copy(veroPrice = item.price)
                "s1000" -> updated.copy(s1000Price = item.price)
                "mini" -> updated.copy(miniPrice = item.price)
                "c3" -> updated.copy(c3Price = item.price)
                "ducha" -> updated.copy(duchaPrice = item.price)
                "pileta" -> updated.copy(piletaPrice = item.price)
                "aire" -> updated.copy(airePrice = item.price)
                "descalcificador" -> updated.copy(descalcificadorPrice = item.price)
                "domus" -> updated.copy(domusPrice = item.price)
                "maxima" -> updated.copy(maximaPrice = item.price)
                "ropot" -> updated.copy(ropotPrice = item.price)
                "rotech" -> updated.copy(rotechPrice = item.price)
                else -> updated
            }
        }
        return updated.copy(
            installmentsCount = detectedInstallments ?: updated.installmentsCount,
            liveCatalogItems = products.filterNot { it.category.equals("Repuestos", ignoreCase = true) }
        )
    }

    private fun computeVariations(oldCfg: PsaPricingConfig, newCfg: PsaPricingConfig): List<PsaPriceVariation> {
        val list = mutableListOf<PsaPriceVariation>()
        fun check(name: String, oldP: Double, newP: Double) {
            if (oldP > 0 && newP > 0 && abs(newP - oldP) >= 1.0) {
                val diff = newP - oldP
                list.add(
                    PsaPriceVariation(
                        productName = name,
                        previousPrice = oldP,
                        newPrice = newP,
                        difference = diff,
                        percentageChange = (diff / oldP) * 100.0
                    )
                )
            }
        }
        check("PSA Senior 4", oldCfg.senior4Price, newCfg.senior4Price)
        check("PSA Senik", oldCfg.senikPrice, newCfg.senikPrice)
        check("PSA Quantum", oldCfg.quantumPrice, newCfg.quantumPrice)
        check("PSA Senior Clásico", oldCfg.cromaPrice, newCfg.cromaPrice)
        check("PSA Vero", oldCfg.veroPrice, newCfg.veroPrice)
        check("PSA S-1000 II", oldCfg.s1000Price, newCfg.s1000Price)
        check("PSA Mini", oldCfg.miniPrice, newCfg.miniPrice)
        check("PSA C3 (Lavatorio)", oldCfg.c3Price, newCfg.c3Price)
        check("PSA Ducha", oldCfg.duchaPrice, newCfg.duchaPrice)
        check("PSA Iontrix3 (Pileta)", oldCfg.piletaPrice, newCfg.piletaPrice)
        check("PSA AirPurifier (Aire)", oldCfg.airePrice, newCfg.airePrice)
        check("PSA Descalcificador R-200", oldCfg.descalcificadorPrice, newCfg.descalcificadorPrice)
        check("PSA Domus", oldCfg.domusPrice, newCfg.domusPrice)
        check("PSA Máxima", oldCfg.maximaPrice, newCfg.maximaPrice)
        check("PSA Ropot", oldCfg.ropotPrice, newCfg.ropotPrice)
        check("PSA ROtech·UV", oldCfg.rotechPrice, newCfg.rotechPrice)
        return list
    }
}
