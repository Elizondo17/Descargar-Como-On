package com.example.util

import com.example.data.model.PsaPricingConfig
import java.util.Locale
import kotlin.math.abs

/**
 * Analizador estricto de precios oficiales al público (PVP de lista) de PSA.
 * - Exige símbolo de moneda o etiqueta/columna explícita de precio PVP/Lista.
 * - Descarta cuotas, descuentos contado, costos DIP/netos, puntos PV, códigos SKU y capacidades en litros.
 * - Usa límites de palabra exactos (\b) para evitar falsos positivos dentro de palabras normales.
 */
object PsaPriceTextParser {

    data class PsaPriceCandidateOption(
        val price: Double,
        val label: String = "PVP",
        val isPreferredPvp: Boolean = false
    )

    data class DomPriceRowInput(
        val productName: String,
        val candidates: List<PsaPriceCandidateOption>,
        val rawSnippet: String = ""
    )

    data class PsaParsedPriceItem(
        val modelKey: String,
        val modelName: String,
        val detectedPrice: Double,
        val previousPrice: Double,
        val variationPct: Double,
        val rawSnippet: String,
        val alternateCandidates: List<PsaPriceCandidateOption> = emptyList(),
        val isOutlierWarning: Boolean = false
    )

    data class PsaParsedPriceResult(
        val items: List<PsaParsedPriceItem>,
        val detectedInstallments: Int? = null,
        val detectedCuotaSimpleFree: Boolean = false,
        val detectedNote: String = "",
        val rawTextLength: Int = 0
    )

    private data class ProductMatcher(
        val key: String,
        val name: String,
        val currentPrice: Double,
        val baselinePrice: Double,
        val pattern: Regex,
        val rejectIfContains: List<String> = emptyList(),
        val isAccessoryOrPart: Boolean = false
    )

    private val CURRENCY_NUMBER_REGEX = Regex(
        """(?:(?:U\${'$'}S|US\${'$'}|\${'$'}U|ARS|UYU|CLP|BOB|PYG|PEN|COP|USD|EUR|Bs\.?|S/|€|₲|\${'$'})\s*:?\s*)(\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?|\d{3,9}(?:[.,]\d{1,2})?)(?:\s*(?:ARS|UYU|CLP|BOB|PYG|PEN|COP|USD|EUR))?""",
        RegexOption.IGNORE_CASE
    )

    private val LABELED_PRICE_NUMBER_REGEX = Regex(
        """(?:\b(?:pvp|p[úu]blico|precio(?:\s+de)?\s+lista|lista|sugerido)\b\s*:?\s*(?:\$)?\s*)(\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?|\d{3,9}(?:[.,]\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val NEGATIVE_BEFORE_REGEX = Regex(
        """(?:cuotas?\s+de|cuota\s+fija|pagos?\s+de|x\s*$|c/u|costo|neto|dip|mayorista|margen|comisi[óo]n|bonific|descuento|dto|ahorro|se[ñn]a|anticipo|entrega\s+de|flete|env[íi]o|puntos|pv\b|c[óo]d|sku)""",
        RegexOption.IGNORE_CASE
    )

    private val NEGATIVE_AFTER_REGEX = Regex(
        """^\s*(?:litros?|lts?\b|l\b|cuotas?|pagos?|meses?|%|\bpv\b|pts?\b|puntos?)""",
        RegexOption.IGNORE_CASE
    )

    private val POSITIVE_PVP_TAG_REGEX = Regex(
        """(?:pvp|p[úu]blico|publico|precio(?:\s+de)?\s+lista|lista\s+p[úu]blico|sugerido)""",
        RegexOption.IGNORE_CASE
    )

    private val CANJE_BEFORE_REGEX = Regex(
        """\bcanje\b\s*:?\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val KIT_POSV_SUFFIX_REGEX = Regex(
        """\+?\s*kit\s*posv\.?""",
        RegexOption.IGNORE_CASE
    )

    private val COLOR_WORDS_REGEX = Regex(
        """\b(?:bianco|nero|grigio|blanco|negro|gris)\b""",
        RegexOption.IGNORE_CASE
    )

    private val HTML_TAG_REGEX = Regex("""<[^>]+>""")
    private val HTML_SUP_ZERO_REGEX = Regex("""<sup[^>]*>\s*00\s*</sup>""", RegexOption.IGNORE_CASE)
    private val HTML_SUP_DEC_REGEX = Regex("""<sup[^>]*>\s*(\d{2})\s*</sup>""", RegexOption.IGNORE_CASE)

    private val SPARE_PART_OR_ACCESSORY_REGEX = Regex(
        """\b(?:repuesto|recambio|fipor|n[úu]cleo|nucleo|cartucho(?:\s+de)?\s+repuesto|prefiltro|pre[-\s]?filtro|botella|bid[óo]n|bidon|jarra\s+(?:de\s+)?repuesto|kit\s+de\s+repuesto|kit\s+recambio|membrana\s+de\s+repuesto|electrodo\s+de\s+repuesto)\b""",
        RegexOption.IGNORE_CASE
    )

    private val ACCESSORY_START_REGEX = Regex(
        """^(?:repuesto|recambio|cartucho|prefiltro|pre[-\s]?filtro|fipor|n[úu]cleo|nucleo|botella|bid[óo]n|bidon|manguera|adaptador|v[áa]lvula|valvula|soporte|pico|vaso|electrodo|membrana|o[-\s]?ring|kit\s+)\b""",
        RegexOption.IGNORE_CASE
    )

    private val MAIN_EQUIPMENT_HINT_REGEX = Regex(
        """\b(?:senior|senik|quantum|croma|bianco|grigio|nero|vero|quanta|s[-\s]?1000|s[-\s]?3\b|mini|c3|ducha|rinnova|declorador|domus|ablandador|iontrix|pileta|piscina|air\s*purifier|purificador|descalcificador|r[-\s]?200|m[áa]xima|maxima|ropot|rotech(?:\s*[·.]?\s*uv)?|hidro)\b""",
        RegexOption.IGNORE_CASE
    )

    private val UI_NOISE_ROW_REGEX = Regex(
        """^(?:total|subtotal|env[íi]o|flete|descuento|ver\s+planes|ver\s+detalle|canje|pb\s+a\s+tu\s+cuenta|c[óo]digo|sku|categor[íi]a|lista\s+de\s+precios|producto)$""",
        RegexOption.IGNORE_CASE
    )

    private fun normalizeHtmlAndSpecialChars(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(HTML_SUP_ZERO_REGEX, "")
            .replace(HTML_SUP_DEC_REGEX, ".$1")
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&#160;", " ")
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace(HTML_TAG_REGEX, " ")
            .replace('\u00A0', ' ')
            .replace(Regex("[\u200B-\u200D\uFEFF]"), "")
            .replace(Regex("""[^\S\r\n]+"""), " ")
            .trim()
    }

    private fun isSparePartOrAccessory(name: String): Boolean {
        val cleaned = KIT_POSV_SUFFIX_REGEX.replace(normalizeHtmlAndSpecialChars(name), " ")
            .lowercase(Locale.ROOT)
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (cleaned.isBlank()) return true
        val hasMainEquip = MAIN_EQUIPMENT_HINT_REGEX.containsMatchIn(cleaned)
        if (SPARE_PART_OR_ACCESSORY_REGEX.containsMatchIn(cleaned)) return true
        if (!hasMainEquip && ACCESSORY_START_REGEX.containsMatchIn(cleaned)) return true
        return false
    }

    private fun buildMatchers(currentConfig: PsaPricingConfig, countryCode: String): List<ProductMatcher> {
        val baseline = PsaCountryRegistry.defaultPricingForCountry(countryCode)
        return listOf(
            ProductMatcher(
                key = "senior4_bm",
                name = "PSA Senior 4 Bajo Mesada",
                currentPrice = currentConfig.liveCatalogItems.firstOrNull { it.id == "senior4_bm" }?.price ?: 0.0,
                baselinePrice = baseline.senior4Price * 0.88,
                pattern = Regex("""\b(?:psa\s+)?senior\s*(?:4|iv)\s*(?:bm|bajo\s*mesada)\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "núcleo", "nucleo", "fipor", "combo", "kit de repuesto", "kit recambio")
            ),
            ProductMatcher(
                key = "senior4",
                name = "PSA Senior 4",
                currentPrice = currentConfig.senior4Price,
                baselinePrice = baseline.senior4Price,
                pattern = Regex("""\b(?:psa\s+)?senior\s*(?:4|iv)\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "núcleo", "nucleo", "fipor", "bajo mesada", "bm", "combo", "kit de repuesto", "kit recambio")
            ),
            ProductMatcher(
                key = "senik",
                name = "PSA Senik",
                currentPrice = currentConfig.senikPrice,
                baselinePrice = baseline.senikPrice,
                pattern = Regex("""\b(?:psa\s+)?senik\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "núcleo", "nucleo", "combo", "kit de repuesto", "kit recambio")
            ),
            ProductMatcher(
                key = "quantum",
                name = "PSA Quantum",
                currentPrice = currentConfig.quantumPrice,
                baselinePrice = baseline.quantumPrice,
                pattern = Regex("""\b(?:psa\s+)?quantum(?:\s*[·.]?\s*2)?\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "núcleo", "nucleo", "combo", "kit de repuesto", "kit recambio")
            ),
            ProductMatcher(
                key = "croma",
                name = "PSA Senior Clásico / Croma",
                currentPrice = currentConfig.cromaPrice,
                baselinePrice = baseline.cromaPrice,
                pattern = Regex("""\b(?:psa\s+)?(?:croma|senior\s*(?:cl[áa]sico|clasico|bianco|grigio|nero))\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("senior 4", "senior4", "bajo mesada", "bm", "repuesto", "núcleo", "nucleo", "combo", "kit de repuesto")
            ),
            ProductMatcher(
                key = "vero",
                name = "PSA Vero",
                currentPrice = currentConfig.veroPrice,
                baselinePrice = baseline.veroPrice,
                pattern = Regex("""\b(?:psa\s+)?vero\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "núcleo", "nucleo", "combo", "kit de repuesto", "kit recambio")
            ),
            ProductMatcher(
                key = "s1000",
                name = "PSA S-1000 II",
                currentPrice = currentConfig.s1000Price,
                baselinePrice = baseline.s1000Price,
                pattern = Regex("""\b(?:psa\s+)?s[-\s]?1000(?:\s*ii)?\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "núcleo", "nucleo", "combo", "kit de repuesto")
            ),
            ProductMatcher(
                key = "mini",
                name = "PSA Mini",
                currentPrice = currentConfig.miniPrice,
                baselinePrice = baseline.miniPrice,
                pattern = Regex("""\b(?:psa\s+)?mini\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "núcleo", "nucleo", "combo", "kit de repuesto")
            ),
            ProductMatcher(
                key = "c3",
                name = "PSA C3 (Baño / Lavatorio)",
                currentPrice = currentConfig.c3Price,
                baselinePrice = baseline.c3Price,
                pattern = Regex("""\b(?:psa\s+)?c3(?:\s+(?:ba[ñn]o|lavatorio))?\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "cartucho", "recambio", "combo")
            ),
            ProductMatcher(
                key = "ducha",
                name = "PSA Ducha (Declorador)",
                currentPrice = currentConfig.duchaPrice,
                baselinePrice = baseline.duchaPrice,
                pattern = Regex("""\b(?:psa\s+)?(?:ducha(?:\s+(?:ii|2|declorador|ba[ñn]o|rinnova))?|rinnova(?:\s+ducha)?|declorador(?:\s+de)?\s+ducha)\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "cartucho", "recambio", "combo")
            ),
            ProductMatcher(
                key = "pileta",
                name = "PSA Iontrix3 (Pileta)",
                currentPrice = currentConfig.piletaPrice,
                baselinePrice = baseline.piletaPrice,
                pattern = Regex("""\b(?:psa\s+)?(?:iontrix\s*3?|pileta|piscina)\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "electrodo de repuesto", "combo")
            ),
            ProductMatcher(
                key = "aire",
                name = "PSA AirPurifier (Aire)",
                currentPrice = currentConfig.airePrice,
                baselinePrice = baseline.airePrice,
                pattern = Regex("""\b(?:psa\s+)?(?:air\s*purifier|purificador\s+de\s+aire)\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "filtro de repuesto", "hepa de repuesto", "combo")
            ),
            ProductMatcher(
                key = "descalcificador",
                name = "PSA Descalcificador R-200",
                currentPrice = currentConfig.descalcificadorPrice,
                baselinePrice = baseline.descalcificadorPrice,
                pattern = Regex("""\b(?:descalcificador|r[-\s]?200)\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "combo")
            ),
            ProductMatcher(
                key = "domus",
                name = "PSA Domus (Ablandador Automático)",
                currentPrice = currentConfig.domusPrice,
                baselinePrice = baseline.domusPrice,
                pattern = Regex("""\b(?:psa\s+)?domus\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "resina de repuesto", "sal", "combo")
            ),
            ProductMatcher(
                key = "maxima",
                name = "PSA Máxima",
                currentPrice = currentConfig.maximaPrice,
                baselinePrice = baseline.maximaPrice,
                pattern = Regex("""\b(?:psa\s+)?m[áa]xima\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "combo")
            ),
            ProductMatcher(
                key = "ropot",
                name = "PSA Ropot (Ósmosis Inversa)",
                currentPrice = currentConfig.ropotPrice,
                baselinePrice = baseline.ropotPrice,
                pattern = Regex("""\b(?:psa\s+)?ropot\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "membrana de repuesto", "combo")
            ),
            ProductMatcher(
                key = "rotech",
                name = "PSA ROtech·UV (Ósmosis + UV)",
                currentPrice = currentConfig.rotechPrice,
                baselinePrice = baseline.rotechPrice,
                pattern = Regex("""\b(?:psa\s+)?rotech(?:\s*[·.]?\s*uv)?\b""", RegexOption.IGNORE_CASE),
                rejectIfContains = listOf("repuesto", "membrana de repuesto", "lámpara", "lampara", "combo")
            )
        )
    }

    private fun shouldRejectName(rawName: String, rejectTokens: List<String>): Boolean {
        val cleaned = KIT_POSV_SUFFIX_REGEX.replace(normalizeHtmlAndSpecialChars(rawName), " ")
            .lowercase(Locale.ROOT)
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (cleaned.startsWith("kit ")) return true
        if (isSparePartOrAccessory(cleaned)) return true
        return rejectTokens.any { token ->
            val escaped = Regex.escape(token.lowercase(Locale.ROOT))
            Regex("""(?<!\w)$escaped(?!\w)""").containsMatchIn(cleaned)
        }
    }

    private fun normalizeCatalogDisplayName(rawName: String): String {
        val noKit = KIT_POSV_SUFFIX_REGEX.replace(normalizeHtmlAndSpecialChars(rawName), " ")
        val noColor = COLOR_WORDS_REGEX.replace(noKit, " ")
        return noColor.replace(Regex("""\s+"""), " ")
            .trim(' ', '.', '-', ':')
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                if (word.equals("PSA", ignoreCase = true) || word.equals("BM", ignoreCase = true) || word.equals("II", ignoreCase = true) || word.equals("C3", ignoreCase = true)) {
                    word.uppercase(Locale.ROOT)
                } else {
                    word.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
                }
            }
    }

    private fun buildExtraCatalogKey(displayName: String): String {
        val slug = displayName.lowercase(Locale.ROOT)
            .replace(Regex("""[^a-z0-9]+"""), "_")
            .trim('_')
            .take(36)
        return "live_$slug"
    }

    /**
     * Parses structured rows extracted from HTML tables/cards in Portal DIP or official stores.
     * Solo incluye equipos principales y oculta repuestos/accesorios baratos.
     */
    fun parseStructuredRows(
        rows: List<DomPriceRowInput>,
        currentConfig: PsaPricingConfig,
        countryCode: String = "AR"
    ): PsaParsedPriceResult {
        if (rows.isEmpty()) return PsaParsedPriceResult(emptyList())

        val matchers = buildMatchers(currentConfig, countryCode)
        val baseline = PsaCountryRegistry.defaultPricingForCountry(countryCode)
        val detectedItems = linkedMapOf<String, PsaParsedPriceItem>()
        // Piso mínimo plausible para ocultar valores muy bajos / repuestos chicos en equipos principales
        val mainEquipmentMinFloor = minOf(baseline.duchaPrice, baseline.c3Price) * 0.35

        for (row in rows) {
            val cleanName = normalizeHtmlAndSpecialChars(row.productName)
            if (cleanName.isBlank() || isSparePartOrAccessory(cleanName)) continue

            // Si menciona ducha explícitamente como nombre principal (p. ej. "Ducha (compatible C3)"), no dejar que C3 robe la fila
            val orderedMatchers = if (Regex("""^\s*(?:psa\s+)?(?:ducha|rinnova|declorador)""", RegexOption.IGNORE_CASE).containsMatchIn(cleanName)) {
                matchers.sortedByDescending { it.key == "ducha" }
            } else {
                matchers
            }

            var matchedKnown = false
            for (matcher in orderedMatchers) {
                if (!matcher.pattern.containsMatchIn(cleanName)) continue
                if (shouldRejectName(cleanName, matcher.rejectIfContains)) continue
                matchedKnown = true
                if (detectedItems.containsKey(matcher.key)) break

                val minValid = maxOf(matcher.baselinePrice * 0.30, mainEquipmentMinFloor)
                val maxValid = matcher.baselinePrice * 3.5

                val plausibleCandidates = row.candidates
                    .filter { it.price in minValid..maxValid }
                    .distinctBy { kotlin.math.round(it.price).toLong() }
                    .sortedWith(
                        compareByDescending<PsaPriceCandidateOption> { it.isPreferredPvp }
                            .thenByDescending { !it.label.equals("Canje", ignoreCase = true) }
                            .thenByDescending { it.price }
                    )

                val chosen = plausibleCandidates.firstOrNull { it.isPreferredPvp && !it.label.equals("Canje", ignoreCase = true) }
                    ?: plausibleCandidates.firstOrNull { !it.label.equals("Canje", ignoreCase = true) }
                    ?: plausibleCandidates.maxByOrNull { it.price }

                if (chosen != null && chosen.price >= mainEquipmentMinFloor) {
                    val variation = if (matcher.currentPrice > 0.0) {
                        ((chosen.price - matcher.currentPrice) / matcher.currentPrice) * 100.0
                    } else 0.0

                    detectedItems[matcher.key] = PsaParsedPriceItem(
                        modelKey = matcher.key,
                        modelName = matcher.name,
                        detectedPrice = chosen.price,
                        previousPrice = matcher.currentPrice,
                        variationPct = variation,
                        rawSnippet = row.rawSnippet.ifBlank { cleanName },
                        alternateCandidates = plausibleCandidates,
                        isOutlierWarning = chosen.price !in (matcher.baselinePrice * 0.35)..(matcher.baselinePrice * 3.0)
                    )
                }
                break
            }

            // Captura otros equipos principales de la lista oficial (Cocina, Baño, Aire, Pileta, Hogar) incluso si omiten "PSA"
            val looksLikeEquipmentRow = cleanName.contains("psa", ignoreCase = true) ||
                MAIN_EQUIPMENT_HINT_REGEX.containsMatchIn(cleanName) ||
                !UI_NOISE_ROW_REGEX.containsMatchIn(cleanName.trim())
            if (!matchedKnown && looksLikeEquipmentRow) {
                val displayName = normalizeCatalogDisplayName(cleanName)
                if (displayName.length >= 3 &&
                    !UI_NOISE_ROW_REGEX.containsMatchIn(displayName.trim()) &&
                    !displayName.contains("ver planes", ignoreCase = true) &&
                    !isSparePartOrAccessory(displayName)
                ) {
                    val extraKey = buildExtraCatalogKey(displayName)
                    if (!detectedItems.containsKey(extraKey)) {
                        val validCands = row.candidates
                            .filter { it.price >= mainEquipmentMinFloor }
                            .distinctBy { kotlin.math.round(it.price).toLong() }
                            .sortedWith(
                                compareByDescending<PsaPriceCandidateOption> { it.isPreferredPvp }
                                    .thenByDescending { !it.label.equals("Canje", ignoreCase = true) }
                                    .thenByDescending { it.price }
                            )
                        val chosen = validCands.firstOrNull { it.isPreferredPvp && !it.label.equals("Canje", ignoreCase = true) }
                            ?: validCands.firstOrNull { !it.label.equals("Canje", ignoreCase = true) }
                            ?: validCands.firstOrNull()
                        if (chosen != null) {
                            val prevPrice = currentConfig.liveCatalogItems.firstOrNull { it.id == extraKey }?.price ?: 0.0
                            val variation = if (prevPrice > 0.0) ((chosen.price - prevPrice) / prevPrice) * 100.0 else 0.0
                            detectedItems[extraKey] = PsaParsedPriceItem(
                                modelKey = extraKey,
                                modelName = displayName,
                                detectedPrice = chosen.price,
                                previousPrice = prevPrice,
                                variationPct = variation,
                                rawSnippet = row.rawSnippet.ifBlank { cleanName },
                                alternateCandidates = validCands,
                                isOutlierWarning = false
                            )
                        }
                    }
                }
            }
        }

        return PsaParsedPriceResult(
            items = detectedItems.values.toList(),
            detectedNote = if (detectedItems.isNotEmpty()) {
                "${detectedItems.size} equipos principales detectados"
            } else {
                "Sin coincidencias de PVP para equipos principales"
            }
        )
    }

    /**
     * Strict plain-text / PDF / clipboard parser for PSA price lists.
     */
    fun parseText(
        rawText: String,
        currentConfig: PsaPricingConfig,
        countryCode: String = "AR"
    ): PsaParsedPriceResult {
        if (rawText.isBlank()) {
            return PsaParsedPriceResult(emptyList(), rawTextLength = 0)
        }

        val normalizedRaw = rawText.lines().joinToString("\n") { normalizeHtmlAndSpecialChars(it) }
        val lines = normalizedRaw.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val matchers = buildMatchers(currentConfig, countryCode)
        val detectedItems = linkedMapOf<String, PsaParsedPriceItem>()
        val country = PsaCountryRegistry.getByCode(countryCode)
        val baseline = PsaCountryRegistry.defaultPricingForCountry(countryCode)
        val mainEquipmentMinFloor = minOf(baseline.duchaPrice, baseline.c3Price) * 0.35

        for (i in lines.indices) {
            val line = lines[i]
            if (isSparePartOrAccessory(line)) continue

            var matchedKnownLine = false
            val orderedMatchers = if (Regex("""^\s*(?:psa\s+)?(?:ducha|rinnova|declorador)""", RegexOption.IGNORE_CASE).containsMatchIn(line)) {
                matchers.sortedByDescending { it.key == "ducha" }
            } else {
                matchers
            }

            for (matcher in orderedMatchers) {
                if (!matcher.pattern.containsMatchIn(line)) continue
                if (shouldRejectName(line, matcher.rejectIfContains)) continue
                matchedKnownLine = true
                if (detectedItems.containsKey(matcher.key)) break

                // Look ahead up to 6 lines within the same product card until next product starts
                val windowBuilder = StringBuilder(line)
                val maxLookAhead = minOf(i + 6, lines.lastIndex)
                for (j in (i + 1)..maxLookAhead) {
                    val nextL = lines[j]
                    val startsAnotherCard = nextL.startsWith("PSA ", ignoreCase = true) ||
                        matchers.any { other ->
                            other.key != matcher.key &&
                                other.pattern.containsMatchIn(nextL) &&
                                !shouldRejectName(nextL, other.rejectIfContains)
                        }
                    if (startsAnotherCard) break
                    windowBuilder.append(' ').append(nextL)
                }
                val searchWindow = windowBuilder.toString()

                val minValid = maxOf(matcher.baselinePrice * 0.30, mainEquipmentMinFloor)
                val candidates = extractStrictCandidatesFromSnippet(
                    snippet = searchWindow,
                    baselinePrice = matcher.baselinePrice,
                    minFloorOverride = minValid,
                    country = country
                )

                if (candidates.isNotEmpty()) {
                    val chosen = candidates.firstOrNull { it.isPreferredPvp && !it.label.equals("Canje", ignoreCase = true) }
                        ?: candidates.firstOrNull { !it.label.equals("Canje", ignoreCase = true) }
                        ?: candidates.maxByOrNull { it.price }
                        ?: candidates.first()

                    val maxValid = matcher.baselinePrice * 3.5
                    val variation = if (matcher.currentPrice > 0.0) {
                        ((chosen.price - matcher.currentPrice) / matcher.currentPrice) * 100.0
                    } else 0.0

                    detectedItems[matcher.key] = PsaParsedPriceItem(
                        modelKey = matcher.key,
                        modelName = matcher.name,
                        detectedPrice = chosen.price,
                        previousPrice = matcher.currentPrice,
                        variationPct = variation,
                        rawSnippet = line,
                        alternateCandidates = candidates,
                        isOutlierWarning = chosen.price !in minValid..maxValid
                    )
                }
                break
            }

            // Extra fallback for unlisted purifiers (e.g. S3, Quanta, Hidro) present in DIP text/tables
            if (!matchedKnownLine &&
                (line.startsWith("PSA ", ignoreCase = true) || MAIN_EQUIPMENT_HINT_REGEX.containsMatchIn(line)) &&
                !UI_NOISE_ROW_REGEX.containsMatchIn(line)
            ) {
                val titlePart = line.substringBefore('|').substringBefore('$').trim()
                val displayName = normalizeCatalogDisplayName(titlePart.ifBlank { line })
                if (displayName.length >= 4 && !isSparePartOrAccessory(displayName) && !UI_NOISE_ROW_REGEX.containsMatchIn(displayName)) {
                    val extraKey = buildExtraCatalogKey(displayName)
                    if (!detectedItems.containsKey(extraKey)) {
                        val windowBuilder = StringBuilder(line)
                        val maxLookAhead = minOf(i + 5, lines.lastIndex)
                        for (j in (i + 1)..maxLookAhead) {
                            val nextL = lines[j]
                            if (nextL.startsWith("PSA ", ignoreCase = true) ||
                                matchers.any { it.pattern.containsMatchIn(nextL) && !shouldRejectName(nextL, it.rejectIfContains) }
                            ) break
                            windowBuilder.append(' ').append(nextL)
                        }
                        val candidates = extractStrictCandidatesFromSnippet(
                            snippet = windowBuilder.toString(),
                            baselinePrice = baseline.senior4Price,
                            minFloorOverride = mainEquipmentMinFloor,
                            country = country
                        )
                        if (candidates.isNotEmpty()) {
                            val chosen = candidates.firstOrNull { it.isPreferredPvp && !it.label.equals("Canje", ignoreCase = true) }
                                ?: candidates.firstOrNull { !it.label.equals("Canje", ignoreCase = true) }
                                ?: candidates.first()
                            val prevPrice = currentConfig.liveCatalogItems.firstOrNull { it.id == extraKey }?.price ?: 0.0
                            val variation = if (prevPrice > 0.0) ((chosen.price - prevPrice) / prevPrice) * 100.0 else 0.0
                            detectedItems[extraKey] = PsaParsedPriceItem(
                                modelKey = extraKey,
                                modelName = displayName,
                                detectedPrice = chosen.price,
                                previousPrice = prevPrice,
                                variationPct = variation,
                                rawSnippet = line,
                                alternateCandidates = candidates,
                                isOutlierWarning = false
                            )
                        }
                    }
                }
            }
        }

        val fullTextLower = normalizedRaw.lowercase(Locale.ROOT)
        var detectedInstallments: Int? = null
        val cuotasRegex = Regex("""(?:en\s+)?(3|6|12|18)\s+(?:cuotas|pagos)""", RegexOption.IGNORE_CASE)
        cuotasRegex.find(normalizedRaw)?.let {
            detectedInstallments = it.groupValues[1].toIntOrNull()
        }

        val isCuotaSimpleFree = fullTextLower.contains("cuota simple") ||
            fullTextLower.contains("sin interés") ||
            fullTextLower.contains("sin interes")

        return PsaParsedPriceResult(
            items = detectedItems.values.toList(),
            detectedInstallments = detectedInstallments,
            detectedCuotaSimpleFree = isCuotaSimpleFree,
            detectedNote = if (detectedItems.isNotEmpty()) {
                "${detectedItems.size} equipos principales detectados"
            } else {
                "No se detectaron PVP oficiales válidos"
            },
            rawTextLength = rawText.length
        )
    }

    fun applyToPricingConfig(
        currentConfig: PsaPricingConfig,
        parsedResult: PsaParsedPriceResult
    ): PsaPricingConfig {
        return applySelectedItemsToPricingConfig(currentConfig, parsedResult.items, parsedResult.detectedInstallments)
    }

    fun applySelectedItemsToPricingConfig(
        currentConfig: PsaPricingConfig,
        items: List<PsaParsedPriceItem>,
        detectedInstallments: Int? = null
    ): PsaPricingConfig {
        var updated = currentConfig
        val updatedLiveCatalog = currentConfig.liveCatalogItems
            .filterNot { it.category.equals("Repuestos", ignoreCase = true) }
            .toMutableList()
        for (item in items) {
            if (item.detectedPrice <= 10.0) continue
            when (item.modelKey) {
                "senior4" -> updated = updated.copy(senior4Price = item.detectedPrice)
                "senik" -> updated = updated.copy(senikPrice = item.detectedPrice)
                "quantum" -> updated = updated.copy(quantumPrice = item.detectedPrice)
                "croma" -> updated = updated.copy(cromaPrice = item.detectedPrice)
                "vero" -> updated = updated.copy(veroPrice = item.detectedPrice)
                "s1000" -> updated = updated.copy(s1000Price = item.detectedPrice)
                "mini" -> updated = updated.copy(miniPrice = item.detectedPrice)
                "c3" -> updated = updated.copy(c3Price = item.detectedPrice)
                "ducha" -> updated = updated.copy(duchaPrice = item.detectedPrice)
                "pileta" -> updated = updated.copy(piletaPrice = item.detectedPrice)
                "aire" -> updated = updated.copy(airePrice = item.detectedPrice)
                "descalcificador" -> updated = updated.copy(descalcificadorPrice = item.detectedPrice)
                "domus" -> updated = updated.copy(domusPrice = item.detectedPrice)
                "maxima" -> updated = updated.copy(maximaPrice = item.detectedPrice)
                "ropot" -> updated = updated.copy(ropotPrice = item.detectedPrice)
                "rotech" -> updated = updated.copy(rotechPrice = item.detectedPrice)
                else -> {
                    if (isSparePartOrAccessory(item.modelName)) continue
                    val category = when {
                        item.modelName.contains("c3", ignoreCase = true) || item.modelName.contains("ducha", ignoreCase = true) || item.modelName.contains("rinnova", ignoreCase = true) -> "Baño"
                        item.modelName.contains("iontrix", ignoreCase = true) || item.modelName.contains("pileta", ignoreCase = true) -> "Pileta"
                        item.modelName.contains("air", ignoreCase = true) || item.modelName.contains("aire", ignoreCase = true) -> "Aire"
                        item.modelName.contains("domus", ignoreCase = true) || item.modelName.contains("descalcificador", ignoreCase = true) -> "Hogar"
                        else -> "Cocina"
                    }
                    val existingIdx = updatedLiveCatalog.indexOfFirst { it.id == item.modelKey }
                    val newModel = if (existingIdx >= 0) {
                        updatedLiveCatalog[existingIdx].copy(
                            name = item.modelName,
                            price = item.detectedPrice,
                            category = category,
                            installmentsCount = updated.installmentsCount
                        )
                    } else {
                        com.example.data.model.PsaPurifierModel(
                            id = item.modelKey,
                            name = item.modelName,
                            price = item.detectedPrice,
                            installmentsCount = updated.installmentsCount,
                            lifespanMonths = 36,
                            description = "Importado desde lista oficial Portal DIP",
                            category = category,
                            officialUrl = "https://mi.psa.com.ar",
                            anmatRegistration = "Oficial PSA",
                            highlightFeature = "PVP Portal DIP"
                        )
                    }
                    if (existingIdx >= 0) {
                        updatedLiveCatalog[existingIdx] = newModel
                    } else {
                        updatedLiveCatalog.add(newModel)
                    }
                }
            }
        }
        updated = updated.copy(liveCatalogItems = updatedLiveCatalog)
        if (detectedInstallments != null && detectedInstallments in 3..24) {
            updated = updated.copy(installmentsCount = detectedInstallments)
        }
        return updated
    }

    fun buildDiffResultFromCatalog(
        currentConfig: PsaPricingConfig,
        catalogProducts: List<com.example.data.model.PsaPurifierModel>,
        countryCode: String,
        detectedInstallments: Int? = null,
        sourceNote: String = "Lectura estricta del portal oficial"
    ): PsaParsedPriceResult {
        val baseline = PsaCountryRegistry.defaultPricingForCountry(countryCode)
        val items = mutableListOf<PsaParsedPriceItem>()
        val seenKeys = mutableSetOf<String>()

        for (product in catalogProducts) {
            val (modelKey, displayName, pricePair) = when (product.id) {
                "senior4" -> Triple("senior4", "PSA Senior 4", currentConfig.senior4Price to baseline.senior4Price)
                "senik" -> Triple("senik", "PSA Senik", currentConfig.senikPrice to baseline.senikPrice)
                "quantum" -> Triple("quantum", "PSA Quantum", currentConfig.quantumPrice to baseline.quantumPrice)
                "croma" -> Triple("croma", "PSA Senior Clásico / Croma", currentConfig.cromaPrice to baseline.cromaPrice)
                "vero" -> Triple("vero", "PSA Vero", currentConfig.veroPrice to baseline.veroPrice)
                "s1000" -> Triple("s1000", "PSA S-1000 II", currentConfig.s1000Price to baseline.s1000Price)
                "mini" -> Triple("mini", "PSA Mini", currentConfig.miniPrice to baseline.miniPrice)
                "c3" -> Triple("c3", "PSA C3 (Baño / Lavatorio)", currentConfig.c3Price to baseline.c3Price)
                "ducha" -> Triple("ducha", "PSA Ducha (Declorador)", currentConfig.duchaPrice to baseline.duchaPrice)
                "pileta" -> Triple("pileta", "PSA Iontrix3 (Pileta)", currentConfig.piletaPrice to baseline.piletaPrice)
                "aire" -> Triple("aire", "PSA AirPurifier (Aire)", currentConfig.airePrice to baseline.airePrice)
                "descalcificador" -> Triple("descalcificador", "PSA Descalcificador R-200", currentConfig.descalcificadorPrice to baseline.descalcificadorPrice)
                "domus" -> Triple("domus", "PSA Domus (Ablandador Automático)", currentConfig.domusPrice to baseline.domusPrice)
                "maxima" -> Triple("maxima", "PSA Máxima", currentConfig.maximaPrice to baseline.maximaPrice)
                "ropot" -> Triple("ropot", "PSA Ropot (Ósmosis Inversa)", currentConfig.ropotPrice to baseline.ropotPrice)
                "rotech" -> Triple("rotech", "PSA ROtech·UV (Ósmosis + UV)", currentConfig.rotechPrice to baseline.rotechPrice)
                else -> continue
            }

            if (!seenKeys.add(modelKey)) continue
            val (previousPrice, baselinePrice) = pricePair
            val newPrice = product.price
            if (newPrice <= 10.0) continue
            if (abs(newPrice - previousPrice) < 1.0) continue

            val isOutlier = newPrice !in (baselinePrice * 0.40)..(baselinePrice * 3.5)
            val variation = if (previousPrice > 0.0) ((newPrice - previousPrice) / previousPrice) * 100.0 else 0.0

            items.add(
                PsaParsedPriceItem(
                    modelKey = modelKey,
                    modelName = displayName,
                    detectedPrice = newPrice,
                    previousPrice = previousPrice,
                    variationPct = variation,
                    rawSnippet = product.name,
                    alternateCandidates = listOf(
                        PsaPriceCandidateOption(
                            price = newPrice,
                            label = product.highlightFeature.ifBlank { "PVP Oficial" },
                            isPreferredPvp = true
                        )
                    ),
                    isOutlierWarning = isOutlier
                )
            )
        }

        return PsaParsedPriceResult(
            items = items,
            detectedInstallments = detectedInstallments,
            detectedNote = sourceNote
        )
    }

    private fun extractStrictCandidatesFromSnippet(
        snippet: String,
        baselinePrice: Double,
        minFloorOverride: Double,
        country: PsaCountryConfig
    ): List<PsaPriceCandidateOption> {
        val minValid = maxOf(baselinePrice * 0.40, minFloorOverride)
        val maxValid = baselinePrice * 3.5

        val candidates = mutableListOf<PsaPriceCandidateOption>()
        val matches = (CURRENCY_NUMBER_REGEX.findAll(snippet) + LABELED_PRICE_NUMBER_REGEX.findAll(snippet))
            .distinctBy { it.range.first }
            .sortedBy { it.range.first }

        for (match in matches) {
            val startIdx = (match.range.first - 28).coerceAtLeast(0)
            val endIdx = (match.range.last + 28).coerceAtMost(snippet.length)
            val beforeText = snippet.substring(startIdx, match.range.first)
            val afterText = snippet.substring((match.range.last + 1).coerceAtMost(snippet.length), endIdx)

            val isCanje = CANJE_BEFORE_REGEX.containsMatchIn(beforeText)
            val hasPositivePvpTag = POSITIVE_PVP_TAG_REGEX.containsMatchIn(beforeText)
            val hasNegativeBefore = NEGATIVE_BEFORE_REGEX.containsMatchIn(beforeText)
            val hasNegativeAfter = NEGATIVE_AFTER_REGEX.containsMatchIn(afterText)

            if ((hasNegativeBefore || hasNegativeAfter) && !hasPositivePvpTag && !isCanje) {
                continue
            }

            val rawNum = match.groupValues[1]
            val parsed = parseLocalizedNumber(rawNum, country) ?: continue
            if (parsed in minValid..maxValid) {
                val label = when {
                    isCanje -> "Canje"
                    hasPositivePvpTag -> "PVP Lista"
                    beforeText.lowercase(Locale.ROOT).contains("contado") -> "Contado"
                    else -> "PVP Normal"
                }
                val isPreferred = hasPositivePvpTag || (!isCanje && !beforeText.lowercase(Locale.ROOT).contains("contado"))
                if (candidates.none { abs(it.price - parsed) < 1.0 }) {
                    candidates.add(
                        PsaPriceCandidateOption(
                            price = parsed,
                            label = label,
                            isPreferredPvp = isPreferred
                        )
                    )
                }
            }
        }

        // Priorizar siempre PVP Normal / Lista por sobre Canje
        return candidates.sortedWith(
            compareByDescending<PsaPriceCandidateOption> { it.isPreferredPvp }
                .thenByDescending { !it.label.equals("Canje", ignoreCase = true) }
                .thenByDescending { it.price }
        )
    }

    private val SUPERSCRIPT_ZEROS_REGEX = Regex("""^(\d{1,3}(?:[.,]\d{3})+)00$""")

    fun parseLocalizedNumber(numStr: String, country: PsaCountryConfig): Double? {
        return try {
            var s = normalizeHtmlAndSpecialChars(numStr)
                .replace(Regex("""[^\d.,]"""), "")
            if (s.isBlank()) return null

            // Manejar centavos superíndices concatenados ej. "950,00000" -> "950,000"
            SUPERSCRIPT_ZEROS_REGEX.matchEntire(s)?.let { m ->
                s = m.groupValues[1]
            }

            if (s.contains(".") && s.contains(",")) {
                val lastDot = s.lastIndexOf('.')
                val lastComma = s.lastIndexOf(',')
                s = if (lastComma > lastDot) {
                    s.replace(".", "").replace(",", ".")
                } else {
                    s.replace(",", "")
                }
            } else if (s.contains(".")) {
                val parts = s.split(".")
                if (parts.last().length == 3 || parts.size > 2) {
                    s = s.replace(".", "")
                }
            } else if (s.contains(",")) {
                val parts = s.split(",")
                s = if (parts.last().length == 3 || parts.size > 2) {
                    s.replace(",", "")
                } else {
                    s.replace(",", ".")
                }
            } else {
                if (!country.usesDecimalSeparatorComma) {
                    // Sin cambios necesarios
                }
            }
            s.toDoubleOrNull()
        } catch (_: Exception) {
            null
        }
    }
}
