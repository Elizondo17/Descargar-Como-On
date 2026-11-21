package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.PsaCountryRegistry
import com.example.util.PsaPriceTextParser
import com.example.util.PsaScheduleManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PsaPriceParserTest {

    @Test
    fun `parser extracts strict AR PVP and ignores installments, DIP cost, liters, and substring collisions`() {
        val sampleText = """
            Administrar cuenta distribuidor
            Suministro de red 12000 litros
            Listado severo de accesorios
            PSA Senior 4 - Sobre Mesada | Costo DIP $ 570.000 | PVP Lista $ 950.000 | 18 cuotas de $ 52.777 | PV 1450
            PSA Senik Bajo Mesada: 12 cuotas sin interés de $ 108.166 | Precio Lista $ 1.298.000,00
            PSA Vero Jarra Purificadora - Venta al público: $ 549.000
            PSA Croma - PVP $ 781.000
        """.trimIndent()

        val base = PsaCountryRegistry.defaultPricingForCountry("AR")
        val result = PsaPriceTextParser.parseText(sampleText, base, "AR")

        val byKey = result.items.associateBy { it.modelKey }
        assertEquals(950000.0, byKey["senior4"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(1298000.0, byKey["senik"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(549000.0, byKey["vero"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(781000.0, byKey["croma"]?.detectedPrice ?: -1.0, 0.01)
        assertFalse("Should not false-match 'administrar' as PSA Mini", byKey.containsKey("mini"))
    }

    @Test
    fun `structured rows prefer PVP Lista over promo and expose candidates`() {
        val base = PsaCountryRegistry.defaultPricingForCountry("AR")
        val rows = listOf(
            PsaPriceTextParser.DomPriceRowInput(
                productName = "Purificador PSA Senior 4",
                candidates = listOf(
                    PsaPriceTextParser.PsaPriceCandidateOption(price = 950000.0, label = "PVP Lista", isPreferredPvp = true),
                    PsaPriceTextParser.PsaPriceCandidateOption(price = 785123.0, label = "Promo Online", isPreferredPvp = false)
                )
            ),
            PsaPriceTextParser.DomPriceRowInput(
                productName = "PSA Mini Purificador",
                candidates = listOf(
                    PsaPriceTextParser.PsaPriceCandidateOption(price = 427000.0, label = "PVP Lista", isPreferredPvp = true)
                )
            )
        )

        val result = PsaPriceTextParser.parseStructuredRows(rows, base, "AR")
        val senior = result.items.find { it.modelKey == "senior4" }
        val mini = result.items.find { it.modelKey == "mini" }

        assertEquals(950000.0, senior?.detectedPrice ?: -1.0, 0.01)
        assertEquals(2, senior?.alternateCandidates?.size)
        assertEquals(427000.0, mini?.detectedPrice ?: -1.0, 0.01)
    }

    @Test
    fun `corrupted extreme saved prices auto-sanitize to country baseline`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val corrupted = PsaCountryRegistry.defaultPricingForCountry("AR").copy(
            senior4Price = 42.0,          // absurdly low corruption (< 0.22x baseline)
            veroPrice = 999999999.0       // absurdly high corruption (> 6.0x baseline)
        )
        PsaScheduleManager.savePsaPricingConfigForCountry(context, "AR", corrupted)

        val loaded = PsaScheduleManager.getPsaPricingConfigForCountry(context, "AR")
        val baseline = PsaCountryRegistry.defaultPricingForCountry("AR")

        assertEquals(baseline.senior4Price, loaded.senior4Price, 0.01)
        assertEquals(baseline.veroPrice, loaded.veroPrice, 0.01)
        assertTrue(loaded.cromaPrice == baseline.cromaPrice)
    }

    @Test
    fun `buildDiffResultFromCatalog surfaces only changed prices with previous vs detected values`() {
        val current = PsaCountryRegistry.defaultPricingForCountry("AR")
        val catalogProducts = listOf(
            com.example.data.model.PsaPurifierModel(
                id = "senior4",
                name = "Purificador PSA Senior 4",
                price = current.senior4Price + 50000.0
            ),
            com.example.data.model.PsaPurifierModel(
                id = "vero",
                name = "PSA Vero",
                price = current.veroPrice // unchanged -> should not appear in diff
            )
        )

        val diff = PsaPriceTextParser.buildDiffResultFromCatalog(
            currentConfig = current,
            catalogProducts = catalogProducts,
            countryCode = "AR"
        )

        assertEquals(1, diff.items.size)
        val item = diff.items.first()
        assertEquals("PSA Senior 4", item.modelName)
        assertEquals(current.senior4Price, item.previousPrice, 0.01)
        assertEquals(current.senior4Price + 50000.0, item.detectedPrice, 0.01)
    }

    @Test
    fun `parser extracts mi_psa_com_ar screenshot cards with KIT POSV, superscript 00, and optional Canje`() {
        val portalText = """
            PSA SENIOR 4 BIANCO + KIT POSV.
            + 3.3 Pb a tu cuenta. (2.95 Pb por canje)
            Ver planes de pago
            $ 950,00000
            canje: $ 855,00000

            PSA SENIOR 4 NERO + KIT POSV.
            + 3.3 Pb a tu cuenta. (2.95 Pb por canje)
            Ver planes de pago
            $ 950,000 00
            canje: $ 855,000 00

            PSA SENIOR 4 BM + KIT POSV
            + 2.9 Pb a tu cuenta. (2.6 Pb por canje)
            Ver planes de pago
            $ 840,000 00
            canje: $ 756,000 00

            PSA SENIOR BIANCO + KIT POSV.
            + 2.7 Pb a tu cuenta. (2.4 Pb por canje)
            Ver planes de pago
            $ 781,000 00
            canje: $ 702,900 00

            PSA SENIK BIANCO + KIT POSV.
            + 4.5 Pb a tu cuenta.
            Ver planes de pago
            $ 1,298,000 00
            canje: $ 1,168,200 00
        """.trimIndent()

        val base = PsaCountryRegistry.defaultPricingForCountry("AR")
        val result = PsaPriceTextParser.parseText(portalText, base, "AR")
        val byKey = result.items.associateBy { it.modelKey }

        // Normal PVP chosen over Canje, and Canje exposed as alternate candidate
        assertEquals(950000.0, byKey["senior4"]?.detectedPrice ?: -1.0, 0.01)
        assertTrue(byKey["senior4"]?.alternateCandidates?.any { it.label == "Canje" && it.price == 855000.0 } == true)

        // Senior 4 BM stays separate and never overwrites Sobre Mesada
        assertEquals(840000.0, byKey["senior4_bm"]?.detectedPrice ?: -1.0, 0.01)

        // PSA SENIOR BIANCO (without 4) maps to Senior Clásico / Croma
        assertEquals(781000.0, byKey["croma"]?.detectedPrice ?: -1.0, 0.01)

        // Senik extracted cleanly despite + KIT POSV.
        assertEquals(1298000.0, byKey["senik"]?.detectedPrice ?: -1.0, 0.01)
    }

    @Test
    fun `structured rows capture extra catalog models and upsert liveCatalogItems`() {
        val base = PsaCountryRegistry.defaultPricingForCountry("AR")
        val rows = listOf(
            PsaPriceTextParser.DomPriceRowInput(
                productName = "PSA SENIOR 4 BIANCO + KIT POSV.",
                candidates = listOf(
                    PsaPriceTextParser.PsaPriceCandidateOption(950000.0, "PVP Lista", isPreferredPvp = true),
                    PsaPriceTextParser.PsaPriceCandidateOption(855000.0, "Canje", isPreferredPvp = false)
                )
            ),
            PsaPriceTextParser.DomPriceRowInput(
                productName = "PSA SENIOR 4 BM + KIT POSV",
                candidates = listOf(
                    PsaPriceTextParser.PsaPriceCandidateOption(840000.0, "PVP Lista", isPreferredPvp = true),
                    PsaPriceTextParser.PsaPriceCandidateOption(756000.0, "Canje", isPreferredPvp = false)
                )
            ),
            PsaPriceTextParser.DomPriceRowInput(
                productName = "PSA VERO BIANCO + KIT POSV.",
                candidates = listOf(
                    PsaPriceTextParser.PsaPriceCandidateOption(549000.0, "PVP Lista", isPreferredPvp = true)
                )
            ),
            PsaPriceTextParser.DomPriceRowInput(
                productName = "PSA QUANTA BIANCO + KIT POSV.",
                candidates = listOf(
                    PsaPriceTextParser.PsaPriceCandidateOption(680000.0, "PVP Lista", isPreferredPvp = true)
                )
            )
        )

        val parsed = PsaPriceTextParser.parseStructuredRows(rows, base, "AR")
        assertEquals(4, parsed.items.size)

        val updated = PsaPriceTextParser.applySelectedItemsToPricingConfig(base, parsed.items)
        assertEquals(950000.0, updated.senior4Price, 0.01)
        assertEquals(549000.0, updated.veroPrice, 0.01)
        assertEquals(840000.0, updated.liveCatalogItems.firstOrNull { it.id == "senior4_bm" }?.price ?: -1.0, 0.01)
        assertEquals(680000.0, updated.liveCatalogItems.firstOrNull { it.id == "live_psa_quanta" }?.price ?: -1.0, 0.01)
    }

    @Test
    fun `dom bridge captures and saves DIP credentials automatically`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val base = PsaCountryRegistry.defaultPricingForCountry("AR")
        var savedDipCallback: String? = null
        val bridge = com.example.ui.screens.psa.PsaDomExtractorBridge(
            context = context,
            currentPricing = base,
            countryCode = "AR",
            onCredentialsSaved = { savedDipCallback = it },
            onCandidatesForConfirmation = {}
        )

        bridge.onDipCredentialsCaptured("12", "12345678", "MiClave2026")
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        val creds = com.example.util.PsaPortalSyncManager.getCredentials(context)

        assertEquals("12", creds.dipCenter)
        assertEquals("12345678", creds.dipNumber)
        assertEquals("MiClave2026", creds.password)
        assertTrue(creds.rememberCredentials)
        assertEquals("12345678", savedDipCallback)
    }

    @Test
    fun `parser separates C3 vs Ducha, Senik vs Quantum, Pileta, Aire, Ropot and hides spare parts or stray low values`() {
        val dipListaHtmlNormalized = """
            LISTA DE PRECIOS DISTRIBUIDOR DIP
            PSA C3&nbsp;Purificador&nbsp;Lavatorio | Lista $ 248.000<sup>00</sup> | Canje $ 223.200<sup>00</sup> | Costo DIP $ 148.800
            PSA DUCHA Declorador | Lista $ 216.000<sup>00</sup> | Canje $ 194.400<sup>00</sup> | Costo DIP $ 129.600
            PSA IONTRIX3 Pileta Solar | Lista $ 890.000<sup>00</sup> | Canje $ 801.000<sup>00</sup>
            PSA AIRPURIFIER Purificador de Aire | Lista $ 690.000<sup>00</sup>
            PSA QUANTUM Sobre Mesada | Lista $ 1.310.000<sup>00</sup> | Canje $ 1.179.000<sup>00</sup>
            PSA SENIK Bajo Mesada | Lista $ 1.298.000<sup>00</sup> | Canje $ 1.168.200<sup>00</sup>
            PSA ROPOT Osmosis Inversa | Lista $ 1.490.000<sup>00</sup>
            Repuesto FIPOR Senior x3 | Lista $ 45.000<sup>00</sup>
            Botella Térmica PSA 500ml | Lista $ 32.000<sup>00</sup>
            Nucleo de Recambio Ducha | Lista $ 68.000<sup>00</sup>
        """.trimIndent()

        val base = PsaCountryRegistry.defaultPricingForCountry("AR")
        val result = PsaPriceTextParser.parseText(dipListaHtmlNormalized, base, "AR")
        val byKey = result.items.associateBy { it.modelKey }

        assertEquals(248000.0, byKey["c3"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(216000.0, byKey["ducha"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(890000.0, byKey["pileta"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(690000.0, byKey["aire"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(1310000.0, byKey["quantum"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(1298000.0, byKey["senik"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(1490000.0, byKey["ropot"]?.detectedPrice ?: -1.0, 0.01)

        // Ensure cheap spare parts / accessories are hidden
        assertFalse(byKey.containsKey("repuesto_senior"))
        assertFalse(result.items.any { it.modelName.contains("FIPOR", ignoreCase = true) })
        assertFalse(result.items.any { it.modelName.contains("Botella", ignoreCase = true) })
        assertFalse(result.items.any { it.modelName.contains("Recambio", ignoreCase = true) })
        assertTrue(result.items.all { it.detectedPrice >= 100000.0 })

        // Ensure applying updates separated config fields independently
        val applied = PsaPriceTextParser.applySelectedItemsToPricingConfig(base, result.items)
        assertEquals(248000.0, applied.c3Price, 0.01)
        assertEquals(216000.0, applied.duchaPrice, 0.01)
        assertEquals(890000.0, applied.piletaPrice, 0.01)
        assertEquals(690000.0, applied.airePrice, 0.01)
        assertEquals(1310000.0, applied.quantumPrice, 0.01)
        assertEquals(1298000.0, applied.senikPrice, 0.01)
        assertEquals(1490000.0, applied.ropotPrice, 0.01)
    }

    @Test
    fun `structured rows reject cheap accessories and stray low values while keeping KIT POSV main units`() {
        val base = PsaCountryRegistry.defaultPricingForCountry("AR")
        val rows = listOf(
            PsaPriceTextParser.DomPriceRowInput(
                productName = "PSA C3 + KIT POSV.",
                candidates = listOf(
                    PsaPriceTextParser.PsaPriceCandidateOption(259000.0, "PVP Lista", isPreferredPvp = true),
                    PsaPriceTextParser.PsaPriceCandidateOption(233100.0, "Canje", isPreferredPvp = false)
                )
            ),
            PsaPriceTextParser.DomPriceRowInput(
                productName = "PSA DUCHA + KIT POSV.",
                candidates = listOf(
                    PsaPriceTextParser.PsaPriceCandidateOption(224000.0, "PVP Lista", isPreferredPvp = true),
                    PsaPriceTextParser.PsaPriceCandidateOption(201600.0, "Canje", isPreferredPvp = false)
                )
            ),
            PsaPriceTextParser.DomPriceRowInput(
                productName = "FIPOR REPUESTO UNITARIO",
                candidates = listOf(
                    PsaPriceTextParser.PsaPriceCandidateOption(28000.0, "PVP Lista", isPreferredPvp = true)
                )
            ),
            PsaPriceTextParser.DomPriceRowInput(
                productName = "PSA IONTRIX 3 PILETA",
                candidates = listOf(
                    PsaPriceTextParser.PsaPriceCandidateOption(36.0, "Capacidad", isPreferredPvp = false),
                    PsaPriceTextParser.PsaPriceCandidateOption(915000.0, "PVP Lista", isPreferredPvp = true)
                )
            )
        )

        val parsed = PsaPriceTextParser.parseStructuredRows(rows, base, "AR")
        val byKey = parsed.items.associateBy { it.modelKey }

        assertEquals(3, parsed.items.size)
        assertEquals(259000.0, byKey["c3"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(224000.0, byKey["ducha"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(915000.0, byKey["pileta"]?.detectedPrice ?: -1.0, 0.01)
        assertTrue(byKey["c3"]?.alternateCandidates?.any { it.label == "Canje" && it.price == 233100.0 } == true)
    }

    @Test
    fun `parser detects Ducha variants, Domus, and unprefixed DIP purifiers even with fittings mentioned`() {
        val dipText = """
            Ducha II con manguera y soporte | Lista $ 216.000 | Canje $ 194.400
            Domus + Kit Posv. | Lista $ 1.650.000 | Canje $ 1.485.000
            PSA S3 Purificador Sobre Mesada | Lista $ 640.000
            Cartucho de Recambio Ducha | Lista $ 48.000
            Manguera Repuesto Cromada | Lista $ 22.000
        """.trimIndent()

        val base = PsaCountryRegistry.defaultPricingForCountry("AR")
        val result = PsaPriceTextParser.parseText(dipText, base, "AR")
        val byKey = result.items.associateBy { it.modelKey }

        assertEquals(216000.0, byKey["ducha"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(1650000.0, byKey["domus"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(640000.0, result.items.firstOrNull { it.modelName.contains("S3", ignoreCase = true) }?.detectedPrice ?: -1.0, 0.01)
        assertFalse(result.items.any { it.modelName.contains("Cartucho", ignoreCase = true) })
        assertFalse(result.items.any { it.modelName.contains("Manguera", ignoreCase = true) })

        val applied = PsaPriceTextParser.applySelectedItemsToPricingConfig(base, result.items)
        assertEquals(216000.0, applied.duchaPrice, 0.01)
        assertEquals(1650000.0, applied.domusPrice, 0.01)
        assertTrue(applied.getAllPurifiers().any { it.id == "domus" && it.price == 1650000.0 })
    }

    @Test
    fun `dom bridge merges structured rows with fallback body so Ducha and Domus are never dropped`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val base = PsaCountryRegistry.defaultPricingForCountry("AR")
        var capturedResult: PsaPriceTextParser.PsaParsedPriceResult? = null

        val bridge = com.example.ui.screens.psa.PsaDomExtractorBridge(
            context = context,
            currentPricing = base,
            countryCode = "AR",
            onCandidatesForConfirmation = { capturedResult = it }
        )

        val payload = """
            {
              "rows": [
                {
                  "name": "Senior 4",
                  "candidates": [
                    { "raw": "950.000", "label": "PVP Lista", "preferred": true }
                  ]
                },
                {
                  "name": "Ducha (compatible C3)",
                  "candidates": [
                    { "raw": "216.000", "label": "PVP Lista", "preferred": true }
                  ]
                }
              ],
              "fallbackText": "Senior 4 $ 950.000\nDomus $ 1.650.000\nRinnova Ducha $ 230.000"
            }
        """.trimIndent()

        bridge.onStrictRowsExtracted(payload)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        val items = capturedResult?.items.orEmpty().associateBy { it.modelKey }
        assertEquals(950000.0, items["senior4"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(216000.0, items["ducha"]?.detectedPrice ?: -1.0, 0.01)
        assertEquals(1650000.0, items["domus"]?.detectedPrice ?: -1.0, 0.01)
    }
}
