package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.PsaPurifierModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

enum class DemoWaterSource(val label: String, val badge: String) {
    AGUA_CORRIENTE("Agua Corriente (Red)", "Red Pública Clorada"),
    AGUA_DE_POZO("Agua de Pozo (Napa)", "Perforación / Napa")
}

enum class DemoBudgetPaymentMode(val label: String) {
    CARD("Financiación con Tarjeta"),
    CASH("Presupuesto en Efectivo")
}

data class DemoCartSelection(
    val purifierId: String,
    val quantity: Int = 1
)

data class DemoCartLineDetail(
    val model: PsaPurifierModel,
    val quantity: Int = 1
) {
    val subtotal: Double
        get() = model.price * quantity.coerceAtLeast(1)
}

data class DemoToBudgetHandoff(
    val purifierId: String,
    val items: List<DemoCartSelection> = listOf(DemoCartSelection(purifierId, 1)),
    val paymentMode: DemoBudgetPaymentMode,
    val requestSeq: Long = System.currentTimeMillis()
)

enum class DemoContainerOption(val label: String, val litersPerUnit: Double) {
    BIDON_20L("Bidón 20L", 20.0),
    BOTELLA_2L("Botella 2L", 2.0)
}

data class PsaHardnessStripStage(
    val index: Int,
    val colorName: String,
    val swatchHex: Long,
    val ppmCaCO3: String,
    val ppmNumeric: Int,
    val mmolPerLiter: String,
    val hardnessLevel: String,
    val dissolvedParticlesNote: String,
    val homeImpact: String,
    val recommendedAction: String
)

object PsaHardnessStripCatalog {
    /**
     * Escala colorimétrica verificada de la tira reactiva de dureza total PSA (Verde -> Marrón/Rosado -> Fucsia):
     * inmersión de 3 segundos en muestra sin tratar vs tratada con PSA.
     */
    val stages: List<PsaHardnessStripStage> = listOf(
        PsaHardnessStripStage(
            index = 0,
            colorName = "Verde brillante",
            swatchHex = 0xFF2E7D32L,
            ppmCaCO3 = "0 ppm",
            ppmNumeric = 0,
            mmolPerLiter = "0.00 mmol/L",
            hardnessLevel = "Muy Blanda (Tratada PSA)",
            dissolvedParticlesNote = "Mínima presencia de sales de calcio y magnesio disueltas (< 10 mg/L CaCO3).",
            homeImpact = "Cero incrustación calcárea; grifería, pavas y cañerías protegidas.",
            recommendedAction = "Nivel óptimo alcanzado luego de tratar con sistema de ósmosis inversa / ablandador PSA."
        ),
        PsaHardnessStripStage(
            index = 1,
            colorName = "Verde claro",
            swatchHex = 0xFF66BB6AL,
            ppmCaCO3 = "25 ppm",
            ppmNumeric = 25,
            mmolPerLiter = "0.25 mmol/L",
            hardnessLevel = "Agua Blanda",
            dissolvedParticlesNote = "~25 mg/L de carbonato de calcio (CaCO3) disuelto.",
            homeImpact = "Bajo riesgo de sarro; jabones y champú hacen espuma normalmente.",
            recommendedAction = "Control preventivo en napa con PSA ROtech·UV si existe riesgo microbiológico o nitratos."
        ),
        PsaHardnessStripStage(
            index = 2,
            colorName = "Verde pálido / Oliva",
            swatchHex = 0xFF9CCC65L,
            ppmCaCO3 = "50 ppm",
            ppmNumeric = 50,
            mmolPerLiter = "0.50 mmol/L",
            hardnessLevel = "Dureza Leve",
            dissolvedParticlesNote = "~50 mg/L CaCO3; al hervir comienza a precipitar residuo blanco fino.",
            homeImpact = "Marcas leves en cristalería y pava eléctrica con el uso diario.",
            recommendedAction = "Para consumo humano y cocción segura en napa: PSA Ropot o PSA ROtech·UV."
        ),
        PsaHardnessStripStage(
            index = 3,
            colorName = "Marrón claro / Rosado tenue",
            swatchHex = 0xFFC48B7FL,
            ppmCaCO3 = "120 ppm",
            ppmNumeric = 120,
            mmolPerLiter = "1.20 mmol/L",
            hardnessLevel = "Moderadamente Dura",
            dissolvedParticlesNote = "~120 mg/L CaCO3 y sales minerales disueltas de napa freática.",
            homeImpact = "Sarro visible en pava, cafeteras, picos de canilla y menor eficiencia térmica (+12% energía).",
            recommendedAction = "Recomendado: PSA Ropot o ROtech·UV en cocina + PSA Domus en entrada de la casa."
        ),
        PsaHardnessStripStage(
            index = 4,
            colorName = "Rosado / Magenta",
            swatchHex = 0xFFEC407AL,
            ppmCaCO3 = "250 ppm",
            ppmNumeric = 250,
            mmolPerLiter = "2.50 mmol/L",
            hardnessLevel = "Agua Dura",
            dissolvedParticlesNote = "Alta carga mineral (~250 mg/L CaCO3, sulfatos, sodio y posible arsénico/nitratos).",
            homeImpact = "Incrustación acelerada en termotanque, lavarropas y duchas (+18% consumo eléctrico/gas).",
            recommendedAction = "Doble protección recomendada: PSA ROtech·UV / Ropot (agua de beber) + PSA Domus (cañerías)."
        ),
        PsaHardnessStripStage(
            index = 5,
            colorName = "Fucsia intenso / Rojo oscuro",
            swatchHex = 0xFFC2185BL,
            ppmCaCO3 = "425+ ppm",
            ppmNumeric = 425,
            mmolPerLiter = "4.25+ mmol/L",
            hardnessLevel = "Muy Dura (Crítica)",
            dissolvedParticlesNote = "Saturación extrema (≥425 mg/L CaCO3); sabor salobre/metálico y alta salinidad disuelta.",
            homeImpact = "Taponamiento progresivo de cañerías (+28% gasto energético), piel reseca y riesgo de arsénico/nitratos en napa.",
            recommendedAction = "Solución integral urgente: PSA ROtech·UV (Ósmosis + UV en cocina) + PSA Domus (ablandador automático)."
        )
    )
}

data class RealWaterConsumptionStats(
    val adultsCount: Int,
    val childrenCount: Int,
    val baseHydrationLitersDay: Double,
    val infusionsExtraLitersDay: Double,
    val cookingExtraLitersDay: Double,
    val totalRealLitersDay: Double,
    val totalRealLitersWeek: Double,
    val totalRealLitersMonth: Double,
    val containerOption: DemoContainerOption,
    val containerUnitPrice: Double,
    val equivalentWeeklyContainers: Int,
    val monthlyBottledSpend: Double,
    val annualBottledSpend: Double,
    val threeYearBottledSpend: Double
)

data class PsaDemoDiagnosticSummary(
    val clientLabel: String = "",
    val country: PsaCountryConfig,
    val waterSource: DemoWaterSource,
    // Tap water metrics
    val chlorinePpm: Float = 1.0f,
    val chlorineRiskTitle: String = "",
    val chlorineColorHex: Long = 0xFFFFEE58L,
    // Well water metrics
    val hardnessStage: PsaHardnessStripStage? = null,
    val arsenicRiskSummary: String = "",
    val boilingSummary: String = "",
    // Shared hydration & financial metrics
    val consumptionStats: RealWaterConsumptionStats,
    // Recommendation metrics
    val primaryModel: PsaPurifierModel,
    val secondaryModel: PsaPurifierModel? = null,
    val cartItems: List<DemoCartLineDetail> = emptyList(),
    val annualPsaCostAmortized: Double,
    val annualNetSavings: Double,
    val threeYearNetSavings: Double,
    val plasticBottlesAvoidedYear: Int,
    val showListPrice: Boolean = false,
    val testPhotoPath: String = ""
) {
    val resolvedCartItems: List<DemoCartLineDetail>
        get() = if (cartItems.isNotEmpty()) {
            cartItems
        } else {
            buildList {
                add(DemoCartLineDetail(primaryModel, 1))
                secondaryModel?.let { sec ->
                    if (sec.id != primaryModel.id) add(DemoCartLineDetail(sec, 1))
                }
            }
        }

    val totalCartListPrice: Double
        get() = resolvedCartItems.sumOf { it.subtotal }
}

object PsaDemoReportExporter {

    fun calculateRealConsumptionStats(
        adultsCount: Int,
        childrenCount: Int,
        hasHotWeatherOrSport: Boolean,
        hasPregnancyOrNursing: Boolean,
        includeInfusions: Boolean,
        includeCooking: Boolean,
        containerOption: DemoContainerOption,
        containerUnitPrice: Double
    ): RealWaterConsumptionStats {
        val safeAdults = adultsCount.coerceIn(1, 12)
        val safeChildren = childrenCount.coerceIn(0, 12)
        val totalPeople = safeAdults + safeChildren

        val perAdultDay = 2.5 + (if (hasHotWeatherOrSport) 0.5 else 0.0)
        val perChildDay = 1.5 + (if (hasHotWeatherOrSport) 0.3 else 0.0)
        val pregnancyExtra = if (hasPregnancyOrNursing) 0.5 else 0.0

        val baseHydration = (safeAdults * perAdultDay) + (safeChildren * perChildDay) + pregnancyExtra
        // Mate, té y café promedio: ~0.9 L por adulto al día
        val infusionsExtra = if (includeInfusions) (safeAdults * 0.9) + (safeChildren * 0.25) else 0.0
        // Sopas, fideos, arroz, lavado y cocción: ~1.2 L base + 0.55 L por persona
        val cookingExtra = if (includeCooking) 1.2 + (totalPeople * 0.55) else 0.0

        val totalDay = baseHydration + infusionsExtra + cookingExtra
        val totalWeek = totalDay * 7.0
        val totalMonth = totalDay * 30.0

        val safeUnitPrice = containerUnitPrice.coerceAtLeast(1.0)
        val weeklyUnits = ceil(totalWeek / containerOption.litersPerUnit).toInt().coerceAtLeast(1)
        val monthlySpend = (totalMonth / containerOption.litersPerUnit) * safeUnitPrice
        val annualSpend = monthlySpend * 12.0
        val threeYearSpend = annualSpend * 3.0

        return RealWaterConsumptionStats(
            adultsCount = safeAdults,
            childrenCount = safeChildren,
            baseHydrationLitersDay = baseHydration,
            infusionsExtraLitersDay = infusionsExtra,
            cookingExtraLitersDay = cookingExtra,
            totalRealLitersDay = totalDay,
            totalRealLitersWeek = totalWeek,
            totalRealLitersMonth = totalMonth,
            containerOption = containerOption,
            containerUnitPrice = safeUnitPrice,
            equivalentWeeklyContainers = weeklyUnits,
            monthlyBottledSpend = monthlySpend,
            annualBottledSpend = annualSpend,
            threeYearBottledSpend = threeYearSpend
        )
    }

    fun buildStructuredWhatsAppSummary(summary: PsaDemoDiagnosticSummary): String {
        val c = summary.country
        val stats = summary.consumptionStats
        val totalPeople = stats.adultsCount + stats.childrenCount
        val dateText = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(Date())

        return buildString {
            append("💧 *DIAGNÓSTICO DE CALIDAD DEL AGUA • PSA* 💧\n")
            if (summary.clientLabel.isNotBlank()) {
                append("🏠 *Hogar / Cliente:* ${summary.clientLabel.trim()}\n")
            }
            append("📅 *Fecha:* $dateText • ${c.flag} ${c.name}\n")
            append("🚰 *Fuente analizada:* ${summary.waterSource.label}\n")
            append("━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

            if (summary.waterSource == DemoWaterSource.AGUA_CORRIENTE) {
                append("🧪 *1. TEST DE CLORO (ORTOTOLUIDINA)*\n")
                append("• Medición en canilla: *${String.format(Locale.US, "%.1f", summary.chlorinePpm)} ppm*\n")
                append("• Resultado: ${summary.chlorineRiskTitle}\n")
                append("• Con Purificador PSA: *0.0 ppm* (retención >99.5% de cloro, sedimentos, metales y olores)\n")
                if (summary.boilingSummary.isNotBlank()) {
                    append("• Hervor / Cocción: ${summary.boilingSummary}\n")
                }
            } else {
                val strip = summary.hardnessStage ?: PsaHardnessStripCatalog.stages[3]
                append("🧬 *1. TEST TIRA REACTIVA DE DUREZA PSA*\n")
                append("• Color medido: *${strip.colorName}* (${strip.hardnessLevel})\n")
                append("• Dureza y partículas: *${strip.ppmCaCO3}* (${strip.mmolPerLiter})\n")
                append("• Impacto detectado: ${strip.homeImpact}\n")
                if (summary.arsenicRiskSummary.isNotBlank()) {
                    append("• Riesgos invisibles de napa: ${summary.arsenicRiskSummary}\n")
                }
                if (summary.boilingSummary.isNotBlank()) {
                    append("• Advertencia al hervir: ${summary.boilingSummary}\n")
                }
            }

            append("\n👨‍👩‍👧‍👦 *2. HIDRATACIÓN Y CONSUMO REAL DEL HOGAR*\n")
            append("• Integrantes: $totalPeople personas (${stats.adultsCount} adultos, ${stats.childrenCount} niños)\n")
            append("• Hidratación diaria base: ${String.format(Locale.US, "%.1f", stats.baseHydrationLitersDay)} L/día\n")
            if (stats.infusionsExtraLitersDay > 0.0) {
                append("• Infusiones (mate / té / café): +${String.format(Locale.US, "%.1f", stats.infusionsExtraLitersDay)} L/día\n")
            }
            if (stats.cookingExtraLitersDay > 0.0) {
                append("• Cocina (sopa, fideos, arroz, verduras): +${String.format(Locale.US, "%.1f", stats.cookingExtraLitersDay)} L/día\n")
            }
            append("• *Consumo real total:* *${String.format(Locale.US, "%.1f", stats.totalRealLitersDay)} L/día* (~${stats.totalRealLitersMonth.toInt()} L/mes)\n")

            append("\n💰 *3. GASTO EN AGUA ENVASADA VS PSA*\n")
            append("• Envase calculado: ${stats.containerOption.label} a ${PsaCountryRegistry.formatCurrency(stats.containerUnitPrice, c)} c/u (~${stats.equivalentWeeklyContainers} por semana)\n")
            append("• Gasto mensual actual estimado: ${PsaCountryRegistry.formatCurrency(stats.monthlyBottledSpend, c)}\n")
            append("• Gasto anual envasado: *${PsaCountryRegistry.formatCurrency(stats.annualBottledSpend, c)}*\n")
            append("• En 3 años de vida útil: *${PsaCountryRegistry.formatCurrency(stats.threeYearBottledSpend, c)}*\n")
            if (summary.threeYearNetSavings > 0.0) {
                append("• ✅ *Ahorro neto estimado en 3 años con PSA:* *${PsaCountryRegistry.formatCurrency(summary.threeYearNetSavings, c)}*\n")
            }
            append("• 🌱 Envases evitados al año: ${summary.plasticBottlesAvoidedYear} botellas de plástico\n")

            append("\n🛡️ *4. SOLUCIÓN PSA RECOMENDADA*\n")
            val cartLines = summary.resolvedCartItems
            if (cartLines.size == 1 && cartLines.first().quantity == 1) {
                val item = cartLines.first()
                append("• Equipo: *${item.model.name}*\n")
                append("• Beneficio clave: ${item.model.highlightFeature}\n")
            } else {
                append("• Equipos seleccionados (${cartLines.sumOf { it.quantity }} unid.):\n")
                cartLines.forEach { line ->
                    append("  - *${line.quantity}x ${line.model.name}* — ${line.model.highlightFeature}\n")
                }
            }
            if (summary.showListPrice) {
                val totalLabel = if (cartLines.sumOf { it.quantity } > 1) "Total lista (${c.currencyCode})" else "Valor de lista (${c.currencyCode})"
                append("• $totalLabel: ${PsaCountryRegistry.formatCurrency(summary.totalCartListPrice, c)}\n")
            }
            append("\n📲 Consultame por presupuesto en efectivo o financiación según tu banco y tarjeta.")
        }
    }

    fun shareDiagnosticViaWhatsApp(context: Context, summary: PsaDemoDiagnosticSummary): Result<Unit> {
        return runCatching {
            val message = buildStructuredWhatsAppSummary(summary)
            val whatsappIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(message))
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(whatsappIntent)
            } catch (_: Exception) {
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                    },
                    "Compartir Diagnóstico PSA"
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        }
    }

    fun generateDiagnosticPdf(context: Context, summary: PsaDemoDiagnosticSummary): Result<File> {
        return runCatching {
            val reportsDir = File(context.cacheDir, "psa_reports").apply { mkdirs() }
            val fileStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = File(reportsDir, "diagnostico_psa_$fileStamp.pdf")

            val nativeRendered = runCatching {
                renderNativeAndroidPdf(summary, outFile)
            }.isSuccess

            if (!nativeRendered || !outFile.exists() || outFile.length() == 0L) {
                writePortablePdfFallback(summary, outFile)
            }
            outFile
        }
    }

    private fun renderNativeAndroidPdf(summary: PsaDemoDiagnosticSummary, outFile: File) {
        val pdfDoc = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val c = summary.country
            val stats = summary.consumptionStats
            val dateText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES")).format(Date())

            val bgPaint = Paint().apply { color = Color.parseColor("#F8FAFC") }
            canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

            // Header bar
            val headerPaint = Paint().apply { color = Color.parseColor("#0B2545") }
            canvas.drawRect(0f, 0f, 595f, 96f, headerPaint)

            val accentLinePaint = Paint().apply { color = Color.parseColor("#00B4D8") }
            canvas.drawRect(0f, 92f, 595f, 96f, accentLinePaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#BAE6FD")
                textSize = 11f
            }
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                textSize = 10f
            }

            canvas.drawText("DIAGNÓSTICO DE CALIDAD DEL AGUA • PSA", 28f, 34f, titlePaint)
            canvas.drawText(
                "Fuente analizada: ${summary.waterSource.label}  |  ${c.name} (${c.currencyCode})",
                28f,
                56f,
                subHeaderPaint
            )
            val clientLine = if (summary.clientLabel.isNotBlank()) {
                "Hogar / Cliente: ${summary.clientLabel.trim()}   •   Fecha: $dateText"
            } else {
                "Fecha de diagnóstico: $dateText"
            }
            canvas.drawText(clientLine, 28f, 76f, metaPaint)

            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0B2545")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 10.5f
            }
            val boldBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 10.8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            val cardStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }

            fun drawBox(top: Float, height: Float, borderColor: Int = Color.parseColor("#CBD5E1")) {
                val rect = RectF(28f, top, 567f, top + height)
                canvas.drawRoundRect(rect, 10f, 10f, cardFill)
                cardStroke.color = borderColor
                canvas.drawRoundRect(rect, 10f, 10f, cardStroke)
            }

            fun drawWrappedText(text: String, x: Float, y: Float, width: Int, paint: TextPaint): Float {
                val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(2f, 1.12f)
                    .build()
                canvas.save()
                canvas.translate(x, y)
                layout.draw(canvas)
                canvas.restore()
                return y + layout.height
            }

            var cursorY = 114f

            // SECTION 1: WATER TEST RESULT
            val photoBitmap: Bitmap? = if (summary.testPhotoPath.isNotBlank()) {
                runCatching {
                    val f = File(summary.testPhotoPath)
                    if (f.exists() && f.length() > 0L) {
                        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                        BitmapFactory.decodeFile(f.absolutePath, opts)
                    } else null
                }.getOrNull()
            } else null

            drawBox(cursorY, 178f, Color.parseColor("#0284C7"))
            if (summary.waterSource == DemoWaterSource.AGUA_CORRIENTE) {
                canvas.drawText("1. RESULTADO TEST DE CLORO (ORTOTOLUIDINA)", 42f, cursorY + 24f, sectionPaint)

                val swatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = summary.chlorineColorHex.toInt()
                }
                canvas.drawRoundRect(RectF(42f, cursorY + 36f, 114f, cursorY + 90f), 8f, 8f, swatchPaint)
                canvas.drawRoundRect(RectF(42f, cursorY + 36f, 114f, cursorY + 90f), 8f, 8f, cardStroke)
                canvas.drawText(
                    "${String.format(Locale.US, "%.1f", summary.chlorinePpm)} ppm",
                    52f,
                    cursorY + 67f,
                    boldBodyPaint
                )

                val rightColWidth = if (photoBitmap != null) 300 else 425
                var textY = cursorY + 38f
                textY = drawWrappedText(
                    "Nivel medido en canilla: ${summary.chlorineRiskTitle}",
                    128f,
                    textY,
                    rightColWidth,
                    boldBodyPaint
                )
                textY = drawWrappedText(
                    "Comparativa PSA: 0.0 ppm libre de cloro residual, olores, sedimentos y subproductos químicos (THMs).",
                    128f,
                    textY + 4f,
                    rightColWidth,
                    bodyPaint
                )

                if (photoBitmap != null) {
                    val photoRect = RectF(442f, cursorY + 32f, 554f, cursorY + 112f)
                    canvas.drawBitmap(photoBitmap, null, photoRect, null)
                    canvas.drawRect(photoRect, cardStroke)
                    val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#475569")
                        textSize = 8.5f
                    }
                    canvas.drawText("Foto real del test", 458f, cursorY + 123f, capPaint)
                }

                drawWrappedText(
                    summary.boilingSummary.ifBlank {
                        "Al hervir el agua sin filtrar se evaporan líquidos y se concentran sales y compuestos químicos."
                    },
                    42f,
                    maxOf(textY + 8f, if (photoBitmap != null) cursorY + 130f else cursorY + 104f),
                    510,
                    bodyPaint
                )
            } else {
                val strip = summary.hardnessStage ?: PsaHardnessStripCatalog.stages[3]
                canvas.drawText("1. TEST TIRA REACTIVA DE DUREZA PSA (POZO / NAPA)", 42f, cursorY + 24f, sectionPaint)

                val padWidth = if (photoBitmap != null) 60f else 78f
                val padGap = if (photoBitmap != null) 5f else 6f
                PsaHardnessStripCatalog.stages.forEachIndexed { idx, st ->
                    val left = 42f + idx * (padWidth + padGap)
                    val rect = RectF(left, cursorY + 34f, left + padWidth, cursorY + 66f)
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = st.swatchHex.toInt() }
                    canvas.drawRoundRect(rect, 5f, 5f, p)
                    if (st.index == strip.index) {
                        val activeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.parseColor("#0F172A")
                            style = Paint.Style.STROKE
                            strokeWidth = 2.6f
                        }
                        canvas.drawRoundRect(rect, 5f, 5f, activeBorder)
                    }
                    val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        textSize = if (photoBitmap != null) 7.5f else 8.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    canvas.drawText(st.ppmCaCO3, left + 5f, cursorY + 54f, labelP)
                }

                if (photoBitmap != null) {
                    val photoRect = RectF(442f, cursorY + 32f, 554f, cursorY + 112f)
                    canvas.drawBitmap(photoBitmap, null, photoRect, null)
                    canvas.drawRect(photoRect, cardStroke)
                    val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#475569")
                        textSize = 8.5f
                    }
                    canvas.drawText("Foto tira PSA", 466f, cursorY + 123f, capPaint)
                }

                val wellTextWidth = if (photoBitmap != null) 388 else 510
                var textY = cursorY + 76f
                textY = drawWrappedText(
                    "Color registrado: ${strip.colorName} • ${strip.hardnessLevel} (${strip.ppmCaCO3} / ${strip.mmolPerLiter})",
                    42f,
                    textY,
                    wellTextWidth,
                    boldBodyPaint
                )
                textY = drawWrappedText(
                    "Partículas e impacto: ${strip.dissolvedParticlesNote} ${strip.homeImpact}",
                    42f,
                    textY + 3f,
                    if (textY < cursorY + 122f) wellTextWidth else 510,
                    bodyPaint
                )
                if (summary.arsenicRiskSummary.isNotBlank()) {
                    drawWrappedText(
                        "Riesgos invisibles de napa: ${summary.arsenicRiskSummary}",
                        42f,
                        maxOf(textY + 3f, if (photoBitmap != null) cursorY + 128f else textY + 3f),
                        510,
                        bodyPaint
                    )
                }
            }

            if (photoBitmap != null && !photoBitmap.isRecycled) {
                photoBitmap.recycle()
            }

            cursorY += 192f

            // SECTION 2: HYDRATION + REAL WATER CONSUMPTION
            drawBox(cursorY, 134f)
            canvas.drawText("2. HIDRATACIÓN Y CONSUMO REAL DEL HOGAR", 42f, cursorY + 24f, sectionPaint)
            val peopleStr = "${stats.adultsCount + stats.childrenCount} personas (${stats.adultsCount} adultos, ${stats.childrenCount} niños)"
            var y2 = cursorY + 36f
            y2 = drawWrappedText("• Grupo familiar: $peopleStr", 42f, y2, 510, bodyPaint)
            y2 = drawWrappedText(
                "• Hidratación recomendada (OMS): ${String.format(Locale.US, "%.1f", stats.baseHydrationLitersDay)} L/día",
                42f,
                y2 + 2f,
                510,
                bodyPaint
            )
            val extrasParts = mutableListOf<String>()
            if (stats.infusionsExtraLitersDay > 0.0) {
                extrasParts.add("Infusiones (mate/té/café): +${String.format(Locale.US, "%.1f", stats.infusionsExtraLitersDay)} L/día")
            }
            if (stats.cookingExtraLitersDay > 0.0) {
                extrasParts.add("Cocina (sopa/fideos/arroz): +${String.format(Locale.US, "%.1f", stats.cookingExtraLitersDay)} L/día")
            }
            if (extrasParts.isNotEmpty()) {
                y2 = drawWrappedText("• ${extrasParts.joinToString("  |  ")}", 42f, y2 + 2f, 510, bodyPaint)
            }
            drawWrappedText(
                "• Consumo REAL total del hogar: ${String.format(Locale.US, "%.1f", stats.totalRealLitersDay)} L/día  (${String.format(Locale.US, "%.1f", stats.totalRealLitersWeek)} L/semana • ${stats.totalRealLitersMonth.toInt()} L/mes)",
                42f,
                y2 + 4f,
                510,
                boldBodyPaint
            )

            cursorY += 148f

            // SECTION 3: ECONOMIC COMPARISON
            drawBox(cursorY, 152f, Color.parseColor("#059669"))
            canvas.drawText("3. GASTO EN AGUA ENVASADA VS AHORRO CON PSA", 42f, cursorY + 24f, sectionPaint)
            var y3 = cursorY + 36f
            y3 = drawWrappedText(
                "• Referencia de envase: ${stats.containerOption.label} (${PsaCountryRegistry.formatCurrency(stats.containerUnitPrice, c)} c/u) → ~${stats.equivalentWeeklyContainers} por semana",
                42f,
                y3,
                510,
                bodyPaint
            )
            y3 = drawWrappedText(
                "• Gasto mensual actual en envases: ${PsaCountryRegistry.formatCurrency(stats.monthlyBottledSpend, c)}   |   Gasto anual: ${PsaCountryRegistry.formatCurrency(stats.annualBottledSpend, c)}",
                42f,
                y3 + 4f,
                510,
                bodyPaint
            )
            y3 = drawWrappedText(
                "• Gasto proyectado a 3 años sin purificador: ${PsaCountryRegistry.formatCurrency(stats.threeYearBottledSpend, c)}",
                42f,
                y3 + 4f,
                510,
                boldBodyPaint
            )
            val savingsPaint = TextPaint(boldBodyPaint).apply { color = Color.parseColor("#047857") }
            val savingsMsg = if (summary.threeYearNetSavings > 0.0) {
                "• Ahorro neto estimado en 3 años con PSA: +${PsaCountryRegistry.formatCurrency(summary.threeYearNetSavings, c)} (${summary.plasticBottlesAvoidedYear} botellas plásticas evitadas/año)"
            } else {
                "• Agua pura ilimitada para tomar, cocinar y lavar verduras evitando ${summary.plasticBottlesAvoidedYear} envases plásticos/año"
            }
            drawWrappedText(savingsMsg, 42f, y3 + 6f, 510, savingsPaint)

            cursorY += 166f

            // SECTION 4: RECOMMENDED PSA SOLUTION
            drawBox(cursorY, 150f, Color.parseColor("#0284C7"))
            canvas.drawText("4. SOLUCIÓN PSA RECOMENDADA PARA ESTE HOGAR", 42f, cursorY + 24f, sectionPaint)
            var y4 = cursorY + 38f
            val pdfCartLines = summary.resolvedCartItems
            if (pdfCartLines.size == 1 && pdfCartLines.first().quantity == 1) {
                val single = pdfCartLines.first()
                y4 = drawWrappedText(
                    "Equipo seleccionado: ${single.model.name} — ${single.model.highlightFeature}",
                    42f,
                    y4,
                    510,
                    boldBodyPaint
                )
                y4 = drawWrappedText(
                    single.model.description,
                    42f,
                    y4 + 3f,
                    510,
                    bodyPaint
                )
            } else {
                y4 = drawWrappedText(
                    "Combinación elegida (${pdfCartLines.sumOf { it.quantity }} equipos):",
                    42f,
                    y4,
                    510,
                    boldBodyPaint
                )
                pdfCartLines.take(4).forEach { line ->
                    y4 = drawWrappedText(
                        "• ${line.quantity}x ${line.model.name} — ${line.model.highlightFeature}",
                        42f,
                        y4 + 2f,
                        510,
                        bodyPaint
                    )
                }
            }
            if (summary.showListPrice) {
                y4 = drawWrappedText(
                    "Valor de lista total (${c.currencyCode}): ${PsaCountryRegistry.formatCurrency(summary.totalCartListPrice, c)}",
                    42f,
                    y4 + 4f,
                    510,
                    boldBodyPaint
                )
            }
            drawWrappedText(
                "Opciones disponibles: presupuesto en efectivo con bonificación o financiación en cuotas según banco/tarjeta.",
                42f,
                y4 + 6f,
                510,
                bodyPaint
            )

            // Footer
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 9.5f
            }
            canvas.drawText(
                "Reporte generado en demostración domiciliaria PSA • Para uso informativo del cliente",
                28f,
                818f,
                footerPaint
            )

            pdfDoc.finishPage(page)
            FileOutputStream(outFile).use { fos ->
                pdfDoc.writeTo(fos)
            }
        } finally {
            runCatching { pdfDoc.close() }
        }
    }

    private fun writePortablePdfFallback(summary: PsaDemoDiagnosticSummary, outFile: File) {
        val c = summary.country
        val stats = summary.consumptionStats
        val rawLines = buildList {
            add("DIAGNOSTICO DE CALIDAD DEL AGUA - PSA")
            add("Fuente: ${summary.waterSource.label} | ${c.name} (${c.currencyCode})")
            if (summary.clientLabel.isNotBlank()) {
                add("Hogar / Cliente: ${summary.clientLabel.trim()}")
            }
            add("------------------------------------------------------------")
            if (summary.waterSource == DemoWaterSource.AGUA_CORRIENTE) {
                add("1. TEST DE CLORO: ${String.format(Locale.US, "%.1f", summary.chlorinePpm)} ppm (${summary.chlorineRiskTitle})")
                add("Comparativa PSA: 0.0 ppm libre de cloro, sedimentos y THMs.")
            } else {
                val strip = summary.hardnessStage ?: PsaHardnessStripCatalog.stages[3]
                add("1. TIRA REACTIVA PSA: ${strip.colorName} - ${strip.hardnessLevel} (${strip.ppmCaCO3} / ${strip.mmolPerLiter})")
                add("Impacto: ${strip.homeImpact}")
                if (summary.arsenicRiskSummary.isNotBlank()) {
                    add("Riesgos napa: ${summary.arsenicRiskSummary}")
                }
            }
            add("------------------------------------------------------------")
            add("2. HIDRATACION Y CONSUMO REAL DEL HOGAR")
            add("Integrantes: ${stats.adultsCount + stats.childrenCount} (${stats.adultsCount} adultos, ${stats.childrenCount} ninos)")
            add("Consumo real total: ${String.format(Locale.US, "%.1f", stats.totalRealLitersDay)} L/dia (${stats.totalRealLitersMonth.toInt()} L/mes)")
            add("------------------------------------------------------------")
            add("3. GASTO EN AGUA ENVASADA VS PSA")
            add("Envase: ${stats.containerOption.label} (${PsaCountryRegistry.formatCurrency(stats.containerUnitPrice, c)} c/u)")
            add("Gasto anual: ${PsaCountryRegistry.formatCurrency(stats.annualBottledSpend, c)}")
            add("Gasto 3 anos: ${PsaCountryRegistry.formatCurrency(stats.threeYearBottledSpend, c)}")
            if (summary.threeYearNetSavings > 0.0) {
                add("Ahorro neto 3 anos con PSA: ${PsaCountryRegistry.formatCurrency(summary.threeYearNetSavings, c)}")
            }
            add("------------------------------------------------------------")
            add("4. SOLUCION PSA RECOMENDADA")
            summary.resolvedCartItems.forEach { line ->
                add("- ${line.quantity}x ${line.model.name} (${line.model.highlightFeature})")
            }
            if (summary.showListPrice) {
                add("Valor de lista total: ${PsaCountryRegistry.formatCurrency(summary.totalCartListPrice, c)}")
            }
        }

        val streamContent = buildString {
            append("BT\n/F1 11 Tf\n42 790 Td\n16 TL\n")
            rawLines.forEachIndexed { idx, line ->
                val escaped = line
                    .replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace("•", "-")
                    .replace("·", ".")
                if (idx == 0) {
                    append("($escaped) Tj\n")
                } else {
                    append("T* ($escaped) Tj\n")
                }
            }
            append("ET\n")
        }.toByteArray(Charsets.ISO_8859_1)

        val objects = listOf(
            "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n",
            "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n",
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n",
            "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n",
            "5 0 obj\n<< /Length ${streamContent.size} >>\nstream\n${String(streamContent, Charsets.ISO_8859_1)}endstream\nendobj\n"
        )

        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        val offsets = mutableListOf<Int>()
        objects.forEach { obj ->
            offsets.add(sb.length)
            sb.append(obj)
        }
        val xrefStart = sb.length
        sb.append("xref\n0 ${objects.size + 1}\n")
        sb.append("0000000000 65535 f \n")
        offsets.forEach { offset ->
            sb.append(String.format(Locale.US, "%010d 00000 n \n", offset))
        }
        sb.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\n")
        sb.append("startxref\n$xrefStart\n%%EOF\n")

        FileOutputStream(outFile).use { fos ->
            fos.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
        }
    }

    fun shareDiagnosticPdf(context: Context, pdfFile: File): Result<Unit> {
        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Diagnóstico de Calidad del Agua PSA")
                putExtra(Intent.EXTRA_TEXT, "Te comparto el diagnóstico personalizado de calidad del agua PSA en formato PDF.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Exportar o Compartir Diagnóstico PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }
}
