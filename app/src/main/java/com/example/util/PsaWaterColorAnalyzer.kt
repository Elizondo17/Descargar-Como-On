package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ChlorineAnalysisResult(
    val detectedPpm: Float,
    val confidenceLabel: String,
    val sampledColorInt: Int,
    val sampledHexLabel: String,
    val qualityStatusTitle: String,
    val requiresFiltering: Boolean,
    val recommendedEquipmentLabel: String,
    val recommendationSummary: String,
    val recommendedCartIds: List<String>
)

data class HardnessAnalysisResult(
    val stageIndex: Int,
    val stage: PsaHardnessStripStage,
    val confidenceLabel: String,
    val sampledColorInt: Int,
    val sampledHexLabel: String,
    val qualityStatusTitle: String,
    val requiresFiltering: Boolean,
    val recommendedEquipmentLabel: String,
    val recommendationSummary: String,
    val recommendedCartIds: List<String>
)

data class WaterPhotoAnalysisOutcome(
    val savedPhotoPath: String,
    val sampledColorInt: Int,
    val sampledHexLabel: String,
    val chlorineResult: ChlorineAnalysisResult? = null,
    val hardnessResult: HardnessAnalysisResult? = null
)

data class WaterQualityRecommendation(
    val qualityStatusTitle: String,
    val requiresFiltering: Boolean,
    val recommendedEquipmentLabel: String,
    val recommendationSummary: String,
    val recommendedCartIds: List<String>
)

object PsaWaterColorAnalyzer {

    fun interpretChlorineLevel(ppm: Float): WaterQualityRecommendation {
        return when {
            ppm <= 0.15f -> WaterQualityRecommendation(
                qualityStatusTitle = "Nivel óptimo (Sin cloro residual)",
                requiresFiltering = false,
                recommendedEquipmentLabel = "PSA Senior 4 (Prevención / Mantenimiento)",
                recommendationSummary = "Agua cristalina sin cloro libre visible en el centro del vaso. Ideal mantener filtración PSA continua frente a arrastre de cañerías.",
                recommendedCartIds = listOf("senior4")
            )
            ppm <= 0.65f -> WaterQualityRecommendation(
                qualityStatusTitle = "Requiere filtrado (Cloro residual leve)",
                requiresFiltering = true,
                recommendedEquipmentLabel = "PSA Senior 4 o PSA Mini",
                recommendationSummary = "Se detecta cloro libre (0.5 ppm) de red. Un purificador de mesada con carbón activado PSA lo retiene al 100% al instante.",
                recommendedCartIds = listOf("senior4")
            )
            ppm <= 1.25f -> WaterQualityRecommendation(
                qualityStatusTitle = "Requiere filtrado (Cloro medio / alto)",
                requiresFiltering = true,
                recommendedEquipmentLabel = "PSA Senior 4 + PSA Ducha",
                recommendationSummary = "Concentración amarilla notoria (1.0 ppm): altera bebidas/comidas y libera vapores en la ducha. Se recomienda cocina + baño.",
                recommendedCartIds = listOf("senior4", "ducha")
            )
            ppm <= 2.05f -> WaterQualityRecommendation(
                qualityStatusTitle = "Requiere filtrado urgente (Carga elevada)",
                requiresFiltering = true,
                recommendedEquipmentLabel = "PSA Senior 4 + PSA Ducha",
                recommendationSummary = "Tono ámbar fuerte (2.0 ppm): carga química elevada en consumo diario y alto riesgo de trihalometanos al hervir o ducharse.",
                recommendedCartIds = listOf("senior4", "ducha")
            )
            else -> WaterQualityRecommendation(
                qualityStatusTitle = "Requiere filtrado crítico (Exceso de cloro)",
                requiresFiltering = true,
                recommendedEquipmentLabel = "PSA Quantum + PSA Ducha",
                recommendationSummary = "Coloración naranja profunda (3.0 ppm): exceso severo de desinfectantes en canilla. Requiere bloque de máxima retención y decloración en ducha.",
                recommendedCartIds = listOf("quantum", "ducha")
            )
        }
    }

    fun interpretHardnessStage(stage: PsaHardnessStripStage): WaterQualityRecommendation {
        return when (stage.index) {
            0 -> WaterQualityRecommendation(
                qualityStatusTitle = "Nivel óptimo (Agua blanda sin sarro)",
                requiresFiltering = false,
                recommendedEquipmentLabel = "PSA Senior 4",
                recommendationSummary = "Dureza mínima (${stage.ppmCaCO3}). No requiere ósmosis; se recomienda purificador PSA de cocina para barrera química y física.",
                recommendedCartIds = listOf("senior4")
            )
            1 -> WaterQualityRecommendation(
                qualityStatusTitle = "Nivel óptimo / moderado (Baja dureza)",
                requiresFiltering = false,
                recommendedEquipmentLabel = "PSA Senior 4",
                recommendationSummary = "Mineralización moderada (${stage.ppmCaCO3}). Recomendado PSA Senior 4 para proteger sabor diario y retener sedimentos.",
                recommendedCartIds = listOf("senior4")
            )
            2 -> WaterQualityRecommendation(
                qualityStatusTitle = "Requiere filtrado antisarro (Dureza media)",
                requiresFiltering = true,
                recommendedEquipmentLabel = "PSA Senior 4 + Descalcificador R-200",
                recommendationSummary = "Nivel ${stage.ppmCaCO3}: comienzo de sarro en pava y grifería. Conviene sumar protección antisarro.",
                recommendedCartIds = listOf("senior4", "descalcificador")
            )
            3 -> WaterQualityRecommendation(
                qualityStatusTitle = "Requiere filtrado (Agua dura)",
                requiresFiltering = true,
                recommendedEquipmentLabel = "PSA Ropot + PSA Domus",
                recommendationSummary = "Dureza alta (${stage.ppmCaCO3}): genera incrustaciones y recarga electrodomésticos. Requiere ósmosis inversa en cocina y ablandador.",
                recommendedCartIds = listOf("ropot", "domus")
            )
            else -> WaterQualityRecommendation(
                qualityStatusTitle = "Requiere filtrado urgente (Muy dura / Extrema)",
                requiresFiltering = true,
                recommendedEquipmentLabel = "PSA ROtech·UV + PSA Domus",
                recommendationSummary = "Napa muy mineralizada (${stage.ppmCaCO3}): solución integral con ósmosis inversa + luz UV bactericida y ablandador automático.",
                recommendedCartIds = listOf("rotech", "domus")
            )
        }
    }

    /**
     * Analiza un array de píxeles RGB centrales de la foto del vaso con reactivo OTO.
     * Filtra reflejos blancos puros y sombras oscuras, promedia H/S/V y ajusta al nivel
     * más cercano de la escala OTO PSA: 0.0, 0.5, 1.0, 2.0, 3.0 ppm.
     */
    fun analyzeChlorineFromRgbPixels(pixels: IntArray): ChlorineAnalysisResult {
        val avgColor = computeRobustAverageColor(pixels)
        val hsv = rgbIntToHsv(avgColor)
        val hue = hsv[0]
        val sat = hsv[1]
        val value = hsv[2]

        val snappedPpm = when {
            sat < 0.14f || value < 0.18f -> 0.0f
            hue in 70f..260f -> 0.0f // Azulado/cristalino/gris neutro -> sin amarillo OTO
            hue in 48f..69f && sat < 0.42f -> 0.5f
            hue in 44f..69f -> 1.0f
            hue in 26f..43.9f -> 2.0f
            hue in 0f..25.9f || hue >= 335f -> 3.0f
            else -> 0.5f
        }

        val interp = interpretChlorineLevel(snappedPpm)
        val confidence = if (pixels.size >= 16) {
            "Centro del vaso: ${String.format(java.util.Locale.US, "%.1f", snappedPpm)} ppm OTO"
        } else {
            "Muestra aproximada: ${String.format(java.util.Locale.US, "%.1f", snappedPpm)} ppm"
        }

        return ChlorineAnalysisResult(
            detectedPpm = snappedPpm,
            confidenceLabel = confidence,
            sampledColorInt = avgColor,
            sampledHexLabel = String.format("#%06X", 0xFFFFFF and avgColor),
            qualityStatusTitle = interp.qualityStatusTitle,
            requiresFiltering = interp.requiresFiltering,
            recommendedEquipmentLabel = interp.recommendedEquipmentLabel,
            recommendationSummary = interp.recommendationSummary,
            recommendedCartIds = interp.recommendedCartIds
        )
    }

    /**
     * Analiza píxeles RGB centrales de la almohadilla de la tira reactiva de dureza PSA
     * y selecciona la etapa 0..5 más cercana perceptual e históricamente en HSV.
     */
    fun analyzeHardnessFromRgbPixels(pixels: IntArray): HardnessAnalysisResult {
        val avgColor = computeRobustAverageColor(pixels)
        val sampleHsv = rgbIntToHsv(avgColor)
        val stages = PsaHardnessStripCatalog.stages

        var bestIndex = 3
        var bestDistance = Float.MAX_VALUE

        stages.forEachIndexed { idx, stage ->
            val refColor = stage.swatchHex.toInt()
            val refHsv = rgbIntToHsv(refColor)
            val dHue = circularHueDistance(sampleHsv[0], refHsv[0]) / 180f
            val dSat = abs(sampleHsv[1] - refHsv[1])
            val dVal = abs(sampleHsv[2] - refHsv[2])

            val dr = abs(Color.red(avgColor) - Color.red(refColor)) / 255f
            val dg = abs(Color.green(avgColor) - Color.green(refColor)) / 255f
            val db = abs(Color.blue(avgColor) - Color.blue(refColor)) / 255f

            val score = (dHue * 3.0f) + (dSat * 1.1f) + (dVal * 0.6f) + ((dr + dg + db) * 0.6f)
            if (score < bestDistance) {
                bestDistance = score
                bestIndex = idx
            }
        }

        val chosen = stages[bestIndex]
        val interp = interpretHardnessStage(chosen)
        return HardnessAnalysisResult(
            stageIndex = bestIndex,
            stage = chosen,
            confidenceLabel = "Centro tira: ${chosen.colorName} (${chosen.ppmCaCO3})",
            sampledColorInt = avgColor,
            sampledHexLabel = String.format("#%06X", 0xFFFFFF and avgColor),
            qualityStatusTitle = interp.qualityStatusTitle,
            requiresFiltering = interp.requiresFiltering,
            recommendedEquipmentLabel = interp.recommendedEquipmentLabel,
            recommendationSummary = interp.recommendationSummary,
            recommendedCartIds = interp.recommendedCartIds
        )
    }

    fun analyzeTestPhotoUri(
        context: Context,
        sourceUri: Uri,
        isWellWater: Boolean
    ): Result<WaterPhotoAnalysisOutcome> {
        return runCatching {
            val bitmap = decodeScaledBitmap(context, sourceUri, maxDimension = 960)
                ?: throw IllegalStateException("No se pudo leer la imagen seleccionada.")

            val savedFile = savePersistentPhoto(context, bitmap)
            val centerPixels = extractCenterRoiPixels(bitmap, roiFraction = 0.18f)

            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }

            if (isWellWater) {
                val res = analyzeHardnessFromRgbPixels(centerPixels)
                WaterPhotoAnalysisOutcome(
                    savedPhotoPath = savedFile.absolutePath,
                    sampledColorInt = res.sampledColorInt,
                    sampledHexLabel = res.sampledHexLabel,
                    hardnessResult = res
                )
            } else {
                val res = analyzeChlorineFromRgbPixels(centerPixels)
                WaterPhotoAnalysisOutcome(
                    savedPhotoPath = savedFile.absolutePath,
                    sampledColorInt = res.sampledColorInt,
                    sampledHexLabel = res.sampledHexLabel,
                    chlorineResult = res
                )
            }
        }
    }

    internal fun extractCenterRoiPixels(bitmap: Bitmap, roiFraction: Float = 0.18f): IntArray {
        val w = bitmap.width.coerceAtLeast(1)
        val h = bitmap.height.coerceAtLeast(1)
        val roiW = max(8, (w * roiFraction).roundToInt()).coerceAtMost(w)
        val roiH = max(8, (h * roiFraction).roundToInt()).coerceAtMost(h)
        val startX = ((w - roiW) / 2).coerceIn(0, w - 1)
        val startY = ((h - roiH) / 2).coerceIn(0, h - 1)
        val out = IntArray(roiW * roiH)
        bitmap.getPixels(out, 0, roiW, startX, startY, roiW, roiH)
        return out
    }

    private fun computeRobustAverageColor(pixels: IntArray): Int {
        if (pixels.isEmpty()) return Color.rgb(255, 238, 88)
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0

        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val luma = (r * 299 + g * 587 + b * 114) / 1000
            // Excluir reflejos quemados (>245) y sombras muy negras (<25)
            if (luma in 25..245) {
                rSum += r
                gSum += g
                bSum += b
                count++
            }
        }

        if (count == 0) {
            for (p in pixels) {
                rSum += (p shr 16) and 0xFF
                gSum += (p shr 8) and 0xFF
                bSum += p and 0xFF
                count++
            }
        }

        val rAvg = (rSum / count).toInt().coerceIn(0, 255)
        val gAvg = (gSum / count).toInt().coerceIn(0, 255)
        val bAvg = (bSum / count).toInt().coerceIn(0, 255)
        return Color.rgb(rAvg, gAvg, bAvg)
    }

    internal fun rgbIntToHsv(colorInt: Int): FloatArray {
        val r = ((colorInt shr 16) and 0xFF) / 255f
        val g = ((colorInt shr 8) and 0xFF) / 255f
        val b = (colorInt and 0xFF) / 255f
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC

        val hue = when {
            delta < 1e-5f -> 0f
            maxC == r -> 60f * (((g - b) / delta) % 6f)
            maxC == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }

        val sat = if (maxC <= 1e-5f) 0f else delta / maxC
        return floatArrayOf(hue, sat, maxC)
    }

    private fun circularHueDistance(h1: Float, h2: Float): Float {
        val diff = abs(h1 - h2)
        return min(diff, 360f - diff)
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val origW = boundsOpts.outWidth
        val origH = boundsOpts.outHeight
        var sampleSize = 1
        if (origW > 0 && origH > 0) {
            while ((origW / sampleSize) > maxDimension || (origH / sampleSize) > maxDimension) {
                sampleSize *= 2
            }
        }
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { exifStream ->
                ExifInterface(exifStream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return decoded

        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated != decoded && !decoded.isRecycled) {
            decoded.recycle()
        }
        return rotated
    }

    private fun savePersistentPhoto(context: Context, bitmap: Bitmap): File {
        val dir = File(context.filesDir, "psa_saved_photos").apply { mkdirs() }
        val file = File(dir, "psa_test_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        return file
    }
}
