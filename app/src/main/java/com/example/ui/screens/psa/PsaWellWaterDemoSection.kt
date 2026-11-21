package com.example.ui.screens.psa

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaPurifierModel
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DemoBudgetPaymentMode
import com.example.util.DemoCartLineDetail
import com.example.util.DemoCartSelection
import com.example.util.DemoContainerOption
import com.example.util.PsaCountryConfig
import com.example.util.PsaCountryRegistry
import com.example.util.PsaDemoDiagnosticSummary
import com.example.util.PsaDemoReportExporter
import com.example.util.PsaHardnessStripCatalog
import com.example.util.PsaWaterColorAnalyzer
import com.example.util.RealWaterConsumptionStats
import com.example.util.WaterPhotoAnalysisOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class WellDemoModuleFilter(val label: String, val tag: String) {
    ALL("Todos", "all"),
    TIRA_DUREZA("1. Tira Reactiva PSA", "tira"),
    RIESGOS_NAPA("2. Riesgos Napa", "riesgos"),
    HERVOR_POZO("3. Hervor Pozo", "hervor"),
    HIDRATACION_GASTO("4. Hidratación y Gasto", "gasto"),
    SOLUCIONES_POZO("5. Equipos Pozo", "equipos")
}

private data class WellInvisibilityRiskInfo(
    val title: String,
    val badge: String,
    val severityPct: Float,
    val accent: Color,
    val explanation: String,
    val whySimpleFilterFails: String,
    val psaSolution: String
)

private data class WellBoilingStageInfo(
    val label: String,
    val minutesLabel: String,
    val waterRemainingPct: Float,
    val mineralConcentrationBadge: String,
    val accent: Color,
    val explanation: String
)

@Composable
internal fun DemoSharedCard(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    accent: Color,
    padding: Dp,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, SlateCardBorder.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            content()
        }
    }
}

@Composable
internal fun DemoSharedChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) selectedColor else SlateBackground)
            .border(1.dp, if (selected) selectedColor else SlateCardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else TextPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun PsaRealTestPhotoCard(
    isWellWater: Boolean,
    savedPhotoPath: String,
    detectedLabel: String,
    detectedSwatchColorInt: Int?,
    detectedHex: String,
    detectedQualityTitle: String = "",
    detectedRequiresFiltering: Boolean? = null,
    detectedRecommendedEquipment: String = "",
    detectedRecommendationReason: String = "",
    detectedRecommendedIds: List<String> = emptyList(),
    onApplyRecommendedCart: ((List<String>) -> Unit)? = null,
    onPhotoAnalyzed: (WaterPhotoAnalysisOutcome) -> Unit,
    onClearPhoto: () -> Unit,
    isLargeFont: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var recommendationApplied by remember(savedPhotoPath) { mutableStateOf(false) }

    val handleUri: (Uri) -> Unit = { uri ->
        scope.launch {
            isAnalyzing = true
            val res = withContext(Dispatchers.Default) {
                PsaWaterColorAnalyzer.analyzeTestPhotoUri(
                    context = context,
                    sourceUri = uri,
                    isWellWater = isWellWater
                )
            }
            isAnalyzing = false
            res.fold(
                onSuccess = { outcome ->
                    onPhotoAnalyzed(outcome)
                    val msg = outcome.chlorineResult?.qualityStatusTitle
                        ?: outcome.hardnessResult?.qualityStatusTitle
                        ?: "Color analizado"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onFailure = { err ->
                    Toast.makeText(context, "No se pudo analizar la foto: ${err.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            handleUri(uri)
        }
    }

    val launchCameraCapture: () -> Unit = {
        runCatching {
            val dir = File(context.cacheDir, "psa_test_photos").apply { mkdirs() }
            val tempFile = File(dir, "capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        }.onFailure { err ->
            Toast.makeText(context, "No se pudo abrir la cámara: ${err.message}", Toast.LENGTH_SHORT).show()
        }
        Unit
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            Toast.makeText(
                context,
                "Permiso de cámara denegado. Puedes usar Galería sin permisos.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            handleUri(uri)
        }
    }

    val previewBitmap = remember(savedPhotoPath) {
        if (savedPhotoPath.isNotBlank() && File(savedPhotoPath).exists()) {
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(savedPhotoPath, opts)
            }.getOrNull()
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateBackground)
            .border(1.dp, PsaCyanPrimary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isWellWater) "Foto real de tira PSA" else "Foto real del vaso OTO",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Apunta el centro al ${if (isWellWater) "cuadro reactivo" else "líquido del vaso"} para lectura automática",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (previewBitmap != null) {
                IconButton(
                    onClick = onClearPhoto,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("demo_photo_clear_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Quitar foto del test",
                        tint = RoseError
                    )
                }
            }
        }

        if (isAnalyzing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlateCard)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    color = PsaCyanGlow,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Analizando color central de la imagen...",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (previewBitmap != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 112.dp, height = 84.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, PsaCyanGlow, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Foto real del test de agua",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val rad = size.minDimension * 0.18f
                        drawCircle(
                            color = Color.White,
                            radius = rad,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawLine(
                            color = Color(0xFF00E5FF),
                            start = Offset(cx - rad - 4f, cy),
                            end = Offset(cx + rad + 4f, cy),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawLine(
                            color = Color(0xFF00E5FF),
                            start = Offset(cx, cy - rad - 4f),
                            end = Offset(cx, cy + rad + 4f),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (detectedSwatchColorInt != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(detectedSwatchColorInt))
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                            Text(
                                text = detectedHex.ifBlank { "Centro analizado" },
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (detectedLabel.isNotBlank()) {
                        Text(
                            text = detectedLabel,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (detectedQualityTitle.isNotBlank() || detectedRecommendedEquipment.isNotBlank()) {
                val qualityAccent = when (detectedRequiresFiltering) {
                    false -> EmeraldSuccess
                    true -> AmberDelay
                    null -> PsaCyanGlow
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(qualityAccent.copy(alpha = 0.12f))
                        .border(1.dp, qualityAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                        .testTag("demo_photo_interpretation_card"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (detectedQualityTitle.isNotBlank()) {
                        Text(
                            text = if (detectedRequiresFiltering == false) "✓ $detectedQualityTitle" else "⚠ $detectedQualityTitle",
                            color = qualityAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                    if (detectedRecommendedEquipment.isNotBlank()) {
                        Text(
                            text = "Equipo recomendado: $detectedRecommendedEquipment",
                            color = PsaCyanGlow,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    if (detectedRecommendationReason.isNotBlank()) {
                        Text(
                            text = detectedRecommendationReason,
                            color = TextPrimary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                    if (onApplyRecommendedCart != null && detectedRecommendedIds.isNotEmpty()) {
                        Button(
                            onClick = {
                                onApplyRecommendedCart(detectedRecommendedIds)
                                recommendationApplied = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (recommendationApplied) EmeraldSuccess else PsaCyanPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("demo_photo_apply_recommendation_btn")
                        ) {
                            Icon(
                                imageVector = if (recommendationApplied) Icons.Default.CheckCircle else Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (recommendationApplied) "Equipo aplicado al carrito" else "Aplicar recomendación al carrito",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        val onCameraClick: () -> Unit = {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                launchCameraCapture()
            } else {
                cameraPermLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        val onGalleryClick: () -> Unit = {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        if (isLargeFont) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCameraClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PsaCyanPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("demo_photo_camera_btn")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sacar Foto", color = PsaCyanGlow, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onGalleryClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SlateCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("demo_photo_gallery_btn")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Elegir Galería", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCameraClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PsaCyanPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("demo_photo_camera_btn")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Foto Cámara", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onGalleryClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SlateCardBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("demo_photo_gallery_btn")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Galería", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SharedHydrationByPeopleCard(
    moduleTitle: String,
    adultsCount: Int,
    onAdultsChange: (Int) -> Unit,
    childrenCount: Int,
    onChildrenChange: (Int) -> Unit,
    hasHotWeatherOrSport: Boolean,
    onToggleHotWeather: () -> Unit,
    hasPregnancyOrNursing: Boolean,
    onTogglePregnancy: () -> Unit,
    baseHydrationLitersDay: Double,
    isLargeFont: Boolean,
    cardPadding: Dp
) {
    val baseAdultLiters = 2.5 + (if (hasHotWeatherOrSport) 0.5 else 0.0)
    val baseChildLiters = 1.5 + (if (hasHotWeatherOrSport) 0.3 else 0.0)
    val glassesPerDay = kotlin.math.round(baseHydrationLitersDay * 4.0).toInt()
    val weeklyBidonsBase = kotlin.math.ceil((baseHydrationLitersDay * 7.0) / 20.0).toInt().coerceAtLeast(1)

    DemoSharedCard(
        icon = Icons.Default.FamilyRestroom,
        title = moduleTitle,
        accent = PsaCyanGlow,
        padding = cardPadding
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SlateBackground)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Referencia diaria (OMS):", color = PsaCyanGlow, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text("• Adulto: ${String.format(Locale.US, "%.1f", baseAdultLiters)} L/día   • Niño: ${String.format(Locale.US, "%.1f", baseChildLiters)} L/día", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
        }

        val memberStepper: @Composable (String, String, Int, Int, Int, String, (Int) -> Unit) -> Unit = { title, sub, count, minVal, maxVal, tagPrefix, onChange ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateBackground)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isLargeFont) {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(sub, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isLargeFont) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(sub, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateCard)
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 4.dp)
                    ) {
                        IconButton(
                            onClick = { if (count > minVal) onChange(count - 1) },
                            enabled = count > minVal,
                            modifier = Modifier.size(48.dp).testTag("${tagPrefix}_minus")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Restar $title", tint = if (count > minVal) RoseError else TextMuted)
                        }
                        Text(
                            text = "$count",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { if (count < maxVal) onChange(count + 1) },
                            enabled = count < maxVal,
                            modifier = Modifier.size(48.dp).testTag("${tagPrefix}_plus")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Sumar $title", tint = EmeraldSuccess)
                        }
                    }
                }
            }
        }

        memberStepper(
            "Adultos en el hogar",
            "~${String.format(Locale.US, "%.1f", baseAdultLiters)} L / persona",
            adultsCount,
            1,
            10,
            "demo_family_adults",
            onAdultsChange
        )

        memberStepper(
            "Niños / Adolescentes",
            "~${String.format(Locale.US, "%.1f", baseChildLiters)} L / chico",
            childrenCount,
            0,
            10,
            "demo_family_children",
            onChildrenChange
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DemoSharedChip(
                label = "Calor / Deporte (+0.5L)",
                selected = hasHotWeatherOrSport,
                selectedColor = AmberDelay,
                onClick = onToggleHotWeather,
                testTag = "demo_hydration_hot"
            )
            DemoSharedChip(
                label = "Embarazo / Lactancia",
                selected = hasPregnancyOrNursing,
                selectedColor = PsaCyanGlow,
                onClick = onTogglePregnancy,
                testTag = "demo_hydration_pregnancy"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(EmeraldSuccess.copy(alpha = 0.12f))
                .border(1.dp, EmeraldSuccess.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(14.dp)
                .testTag("demo_hydration_summary"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Hidratación diaria base (${adultsCount + childrenCount} personas):", color = EmeraldSuccess, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${String.format(Locale.US, "%.1f", baseHydrationLitersDay)} litros/día solo para beber ($glassesPerDay vasos)",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = if (isLargeFont) 17.sp else 16.sp
            )
            Text(
                text = "Solo bebida: ~$weeklyBidonsBase bidones de 20L / semana.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SharedRealBottledExpenseCard(
    moduleTitle: String,
    consumptionStats: RealWaterConsumptionStats,
    includeInfusions: Boolean,
    onToggleInfusions: () -> Unit,
    includeCooking: Boolean,
    onToggleCooking: () -> Unit,
    containerOption: DemoContainerOption,
    onSelectContainer: (DemoContainerOption) -> Unit,
    containerPriceText: String,
    onContainerPriceChange: (String) -> Unit,
    highlightedModel: PsaPurifierModel,
    collapsibleOtherModels: List<PsaPurifierModel>,
    onSelectComparisonModel: (String) -> Unit,
    activeCountry: PsaCountryConfig,
    isLargeFont: Boolean,
    cardPadding: Dp
) {
    var showOtherModelsList by remember { mutableStateOf(false) }

    val annualPsaAmortized = highlightedModel.price / 3.0
    val annualSavings = consumptionStats.annualBottledSpend - annualPsaAmortized
    val threeYearSavings = consumptionStats.threeYearBottledSpend - highlightedModel.price
    val plasticBottlesYear = ((consumptionStats.totalRealLitersMonth * 12.0) / 1.5).toInt()
    val costPerLiterContainer = consumptionStats.containerUnitPrice / containerOption.litersPerUnit
    val costPerLiterPsa = highlightedModel.price / (highlightedModel.capacityLiters.takeIf { it > 0 } ?: 36000).toDouble()

    DemoSharedCard(
        icon = Icons.Default.Eco,
        title = moduleTitle,
        accent = EmeraldSuccess,
        padding = cardPadding
    ) {
        // Checkboxes/toggles para sumar Infusiones y Cocina
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SlateBackground)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "¿Para qué más usan agua pura en casa?",
                color = PsaCyanGlow,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DemoSharedChip(
                    label = if (includeInfusions) "✓ Infusiones (mate/té/café)" else "+ Sumar Infusiones",
                    selected = includeInfusions,
                    selectedColor = AmberDelay,
                    onClick = onToggleInfusions,
                    testTag = "demo_toggle_infusions"
                )
                DemoSharedChip(
                    label = if (includeCooking) "✓ Cocina (sopa/fideos/arroz)" else "+ Sumar Cocina",
                    selected = includeCooking,
                    selectedColor = PsaCyanGlow,
                    onClick = onToggleCooking,
                    testTag = "demo_toggle_cooking"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Consumo REAL total del hogar:",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", consumptionStats.totalRealLitersDay)} L/día  •  ${String.format(Locale.US, "%.0f", consumptionStats.totalRealLitersMonth)} L/mes",
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
            Text(
                text = buildString {
                    append("Base beber: ${String.format(Locale.US, "%.1f", consumptionStats.baseHydrationLitersDay)}L")
                    if (consumptionStats.infusionsExtraLitersDay > 0.0) {
                        append("  +  Infusiones: ${String.format(Locale.US, "%.1f", consumptionStats.infusionsExtraLitersDay)}L")
                    }
                    if (consumptionStats.cookingExtraLitersDay > 0.0) {
                        append("  +  Cocina: ${String.format(Locale.US, "%.1f", consumptionStats.cookingExtraLitersDay)}L")
                    }
                },
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Selector de Envase (Bidón 20L vs Botella 2L) + Precio unitario editable
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DemoContainerOption.entries.forEach { option ->
                DemoSharedChip(
                    label = option.label,
                    selected = containerOption == option,
                    selectedColor = PsaCyanPrimary,
                    onClick = { onSelectContainer(option) },
                    testTag = "demo_container_${option.name.lowercase()}"
                )
            }
        }

        OutlinedTextField(
            value = containerPriceText,
            onValueChange = { onContainerPriceChange(it.filter { ch -> ch.isDigit() }) },
            label = {
                Text(
                    "Precio actual por ${containerOption.label} (${activeCountry.currencySymbol})",
                    fontSize = 12.sp
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("demo_container_price_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PsaCyanGlow,
                unfocusedBorderColor = SlateCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(10.dp)
        )

        // Equipo destacado + Lista plegable con otros purificadores
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SlateBackground)
                .border(1.dp, PsaCyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Comparando contra equipo:", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    Text(highlightedModel.name, color = PsaCyanGlow, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                if (collapsibleOtherModels.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showOtherModelsList = !showOtherModelsList },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SlateCardBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("demo_toggle_other_models")
                    ) {
                        Text(
                            text = if (showOtherModelsList) "Ocultar lista" else "Otros equipos",
                            color = TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showOtherModelsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = PsaCyanGlow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showOtherModelsList) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    collapsibleOtherModels.forEach { model ->
                        DemoSharedChip(
                            label = model.name.replace("PSA ", ""),
                            selected = highlightedModel.id == model.id,
                            selectedColor = PsaCyanPrimary,
                            onClick = { onSelectComparisonModel(model.id) },
                            testTag = "demo_table_model_${model.id}"
                        )
                    }
                }
            }
        }

        // Tabla de gasto real acumulado
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SlateBackground)
                .border(1.dp, EmeraldSuccess.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(12.dp)
                .testTag("demo_gasto_anual_table"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Equivalente a ~${consumptionStats.equivalentWeeklyContainers} ${if (containerOption == DemoContainerOption.BIDON_20L) "bidones de 20L" else "botellas de 2L"} por semana",
                color = AmberDelay,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall
            )

            @Composable
            fun spendRow(period: String, bottledCost: Double, savingsVal: Double?) {
                if (isLargeFont) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateCard)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(period, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Gasto agua envasada: ${PsaCountryRegistry.formatCurrency(bottledCost, activeCountry)}",
                            color = RoseError,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (savingsVal != null) {
                            Text(
                                text = if (savingsVal >= 0) "Ahorro real vs PSA: +${PsaCountryRegistry.formatCurrency(savingsVal, activeCountry)}"
                                else "Diferencia amortizada: ${PsaCountryRegistry.formatCurrency(savingsVal, activeCountry)}",
                                color = if (savingsVal >= 0) EmeraldSuccess else AmberDelay,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(period, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, modifier = Modifier.weight(1.1f))
                        Text(
                            PsaCountryRegistry.formatCurrency(bottledCost, activeCountry),
                            color = RoseError,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                        if (savingsVal != null) {
                            Text(
                                text = (if (savingsVal >= 0) "+" else "") + PsaCountryRegistry.formatCurrency(savingsVal, activeCountry),
                                color = if (savingsVal >= 0) EmeraldSuccess else AmberDelay,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.05f)
                            )
                        }
                    }
                }
            }

            if (!isLargeFont) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Período", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.weight(1.1f))
                    Text("Envasada", color = RoseError, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    Text("Ahorro PSA", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.05f))
                }
                HorizontalDivider(color = SlateCardBorder.copy(alpha = 0.5f))
            }

            spendRow("Mensual", consumptionStats.monthlyBottledSpend, consumptionStats.monthlyBottledSpend - (annualPsaAmortized / 12.0))
            HorizontalDivider(color = SlateCardBorder.copy(alpha = 0.35f))
            spendRow("1 Año (Anual)", consumptionStats.annualBottledSpend, annualSavings)
            HorizontalDivider(color = SlateCardBorder.copy(alpha = 0.35f))
            spendRow("3 Años (Vida útil)", consumptionStats.threeYearBottledSpend, threeYearSavings)
        }

        val statCard: @Composable (String, String, Color, Modifier) -> Unit = { title, valueTxt, col, mod ->
            Column(
                modifier = mod
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlateBackground)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(title, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Text(valueTxt, color = col, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (isLargeFont) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                statCard("Litro en ${containerOption.label}", PsaCountryRegistry.formatCurrency(costPerLiterContainer, activeCountry), RoseError, Modifier.fillMaxWidth())
                statCard("Litro purificado PSA", PsaCountryRegistry.formatCurrency(costPerLiterPsa, activeCountry), EmeraldSuccess, Modifier.fillMaxWidth())
                statCard("Impacto plástico anual", "$plasticBottlesYear botellas de 1.5L evitadas", PsaCyanGlow, Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                statCard("Litro ${containerOption.label}", PsaCountryRegistry.formatCurrency(costPerLiterContainer, activeCountry), RoseError, Modifier.weight(1f))
                statCard("Litro PSA", PsaCountryRegistry.formatCurrency(costPerLiterPsa, activeCountry), EmeraldSuccess, Modifier.weight(1f))
            }
            statCard("Plástico evitado al año", "$plasticBottlesYear envases de 1.5L menos en tu hogar", PsaCyanGlow, Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PsaAccordionEquipmentCartSelector(
    allPurifiers: List<PsaPurifierModel>,
    prioritizedIds: List<String>,
    cartQuantities: Map<String, Int>,
    onToggleItem: (String) -> Unit,
    onUpdateQuantity: (String, Int) -> Unit,
    country: PsaCountryConfig,
    isLargeFont: Boolean,
    showListPrice: Boolean,
    onToggleShowPrice: () -> Unit,
    onRequestCartBudget: (List<DemoCartSelection>, DemoBudgetPaymentMode) -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var expandedFichaId by remember { mutableStateOf<String?>(null) }

    val orderedModels = remember(allPurifiers, prioritizedIds) {
        val prioritySet = prioritizedIds.mapIndexed { index, id -> id to index }.toMap()
        val recommended = prioritizedIds.mapNotNull { id -> allPurifiers.firstOrNull { it.id == id } }
        val others = allPurifiers.filter { !prioritySet.containsKey(it.id) }
        recommended + others
    }

    val activeLines = remember(orderedModels, cartQuantities) {
        orderedModels.mapNotNull { model ->
            val qty = cartQuantities[model.id] ?: 0
            if (qty > 0) DemoCartLineDetail(model, qty) else null
        }
    }
    val totalUnits = activeLines.sumOf { it.quantity }
    val cartTotalList = activeLines.sumOf { it.subtotal }
    val prioritizedCount = prioritizedIds.count { id -> orderedModels.any { it.id == id } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Barra plegable del carrito
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SlateBackground)
                .border(
                    width = 1.2.dp,
                    color = if (isExpanded || totalUnits > 0) PsaCyanPrimary.copy(alpha = 0.65f) else SlateCardBorder,
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("demo_cart_accordion_toggle"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PsaCyanPrimary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Equipos en carrito",
                    tint = PsaCyanGlow,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (totalUnits > 0) "Equipos elegidos ($totalUnits)" else "Elegir equipos PSA",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                val summaryLine = if (activeLines.isEmpty()) {
                    "Toca para desplegar y marcar uno o más equipos"
                } else {
                    activeLines.joinToString(" • ") { line ->
                        "${line.model.name.replace("PSA ", "")} ×${line.quantity}"
                    }
                }
                Text(
                    text = summaryLine,
                    color = if (activeLines.isEmpty()) TextSecondary else PsaCyanGlow,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (activeLines.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Plegar lista" else "Desplegar lista",
                tint = PsaCyanGlow,
                modifier = Modifier.size(22.dp)
            )
        }

        // Lista plegable con checkboxes, ficha breve y cantidad (+/-)
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SlateBackground)
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                orderedModels.forEachIndexed { index, model ->
                    if (index == 0 && prioritizedCount > 0) {
                        Text(
                            text = "SUGERIDOS",
                            color = PsaCyanGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    } else if (index == prioritizedCount && prioritizedCount < orderedModels.size) {
                        HorizontalDivider(color = SlateCardBorder.copy(alpha = 0.5f))
                        Text(
                            text = "CATÁLOGO COMPLETO PSA",
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    val qty = cartQuantities[model.id] ?: 0
                    val isChecked = qty > 0
                    val showFicha = expandedFichaId == model.id

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isChecked) PsaCyanPrimary.copy(alpha = 0.10f) else SlateCard)
                            .border(
                                width = 1.dp,
                                color = if (isChecked) PsaCyanPrimary.copy(alpha = 0.55f) else SlateCardBorder.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onToggleItem(model.id) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("demo_cart_row_${model.id}"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onToggleItem(model.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PsaCyanPrimary,
                                    uncheckedColor = TextSecondary,
                                    checkmarkColor = Color.Black
                                ),
                                modifier = Modifier.testTag("demo_cart_check_${model.id}")
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.name,
                                    color = if (isChecked) PsaCyanGlow else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = model.highlightFeature,
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (showListPrice) {
                                    val linePrice = model.price * qty.coerceAtLeast(1)
                                    Text(
                                        text = if (qty > 1) {
                                            "${PsaCountryRegistry.formatCurrency(model.price, country)} c/u • Subtotal: ${PsaCountryRegistry.formatCurrency(linePrice, country)}"
                                        } else {
                                            PsaCountryRegistry.formatCurrency(model.price, country)
                                        },
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    expandedFichaId = if (showFicha) null else model.id
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("demo_cart_info_${model.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Ver ficha técnica de ${model.name}",
                                    tint = if (showFicha) PsaCyanGlow else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (isChecked) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 4.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cantidad",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SlateBackground)
                                        .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                                ) {
                                    IconButton(
                                        onClick = { onUpdateQuantity(model.id, -1) },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .testTag("demo_cart_minus_${model.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Restar ${model.name}",
                                            tint = RoseError,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Text(
                                        text = "$qty",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { onUpdateQuantity(model.id, 1) },
                                        enabled = qty < 10,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .testTag("demo_cart_plus_${model.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Sumar ${model.name}",
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = showFicha) {
                            Text(
                                text = model.description,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SlateBackground)
                                    .padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Resumen compacto tipo chips cuando hay carrito activo y está plegado
        if (activeLines.isNotEmpty() && !isExpanded) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeLines.forEach { line ->
                    val shortName = line.model.name.replace("PSA ", "")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PsaCyanPrimary.copy(alpha = 0.14f))
                            .border(1.dp, PsaCyanPrimary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${line.quantity}× $shortName",
                            color = PsaCyanGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Botón opcional para revelar u ocultar valor de lista del carrito
        if (activeLines.isNotEmpty()) {
            OutlinedButton(
                onClick = onToggleShowPrice,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp)
                    .testTag("demo_toggle_cart_price")
            ) {
                Icon(
                    imageVector = if (showListPrice) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = PsaCyanGlow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showListPrice) {
                        "Total lista (${country.currencyCode}): ${PsaCountryRegistry.formatCurrency(cartTotalList, country)}"
                    } else {
                        "Ver valor de lista (${country.currencyCode})"
                    },
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }
        }

        // Paso directo del carrito completo a Cotizar (Tarjeta o Efectivo)
        val launchCartBudget: (DemoBudgetPaymentMode) -> Unit = { mode ->
            if (activeLines.isEmpty()) {
                isExpanded = true
                Toast.makeText(context, "Marca al menos un equipo PSA para cotizar", Toast.LENGTH_SHORT).show()
            } else {
                val selections = activeLines.map { DemoCartSelection(it.model.id, it.quantity) }
                onRequestCartBudget(selections, mode)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SlateBackground)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "💳 En tarjeta, las cuotas sin interés dependen del banco y la tarjeta.",
                color = PsaCyanGlow,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )

            if (isLargeFont) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { launchCartBudget(DemoBudgetPaymentMode.CARD) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("demo_cart_quote_card")
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Financiación con Tarjeta", color = PurpleNeon, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { launchCartBudget(DemoBudgetPaymentMode.CASH) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("demo_cart_quote_cash")
                    ) {
                        Icon(Icons.Default.Paid, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Presupuesto en Efectivo", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { launchCartBudget(DemoBudgetPaymentMode.CARD) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("demo_cart_quote_card")
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Financiación Tarjeta", color = PurpleNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { launchCartBudget(DemoBudgetPaymentMode.CASH) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("demo_cart_quote_cash")
                    ) {
                        Icon(Icons.Default.Paid, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Presupuesto Efectivo", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PsaDiagnosticExportCard(
    summary: PsaDemoDiagnosticSummary,
    clientLabel: String,
    onClientLabelChange: (String) -> Unit,
    isLargeFont: Boolean,
    cardPadding: Dp,
    activeVisit: PsaHomeVisit? = null,
    onClearActiveVisit: (() -> Unit)? = null,
    onSaveDiagnosticToVisit: ((PsaHomeVisit) -> Unit)? = null
) {
    val context = LocalContext.current

    DemoSharedCard(
        icon = Icons.Default.PictureAsPdf,
        title = "Exportar Diagnóstico de Calidad del Agua",
        accent = EmeraldSuccess,
        padding = cardPadding
    ) {
        if (activeVisit != null && onSaveDiagnosticToVisit != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PsaCyanPrimary.copy(alpha = 0.12f))
                    .border(1.dp, PsaCyanPrimary.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Visita Activa: ${activeVisit.customerName}",
                            color = PsaCyanGlow,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "${activeVisit.scheduledDate} • ${activeVisit.scheduledTime} hs",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    if (onClearActiveVisit != null) {
                        IconButton(
                            onClick = onClearActiveVisit,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Desvincular visita", tint = TextMuted)
                        }
                    }
                }
                Button(
                    onClick = {
                        onSaveDiagnosticToVisit(activeVisit)
                        Toast.makeText(
                            context,
                            "Diagnóstico guardado en historial de ${activeVisit.customerName}",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PsaCyanGlow),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("demo_btn_save_visit_snapshot")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar Diagnóstico en Visita", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedTextField(
            value = clientLabel,
            onValueChange = onClientLabelChange,
            label = { Text("Hogar / Cliente (Opcional)", fontSize = 12.sp) },
            placeholder = { Text("Ej. Familia Martínez", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("demo_export_client_label"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldSuccess,
                unfocusedBorderColor = SlateCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SlateBackground)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "• Fuente: ${summary.waterSource.label}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodySmall
            )
            val testLine = if (summary.hardnessStage != null) {
                "• Tira PSA: ${summary.hardnessStage.colorName} (${summary.hardnessStage.ppmCaCO3} • ${summary.hardnessStage.mmolPerLiter})"
            } else {
                "• Clorímetro OTO: ${String.format(Locale.US, "%.1f", summary.chlorinePpm)} ppm vs 0.0 ppm PSA"
            }
            Text(text = testLine, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "• Consumo real: ${String.format(Locale.US, "%.1f", summary.consumptionStats.totalRealLitersDay)} L/día (${summary.consumptionStats.adultsCount + summary.consumptionStats.childrenCount} pers.) • Gasto anual: ${PsaCountryRegistry.formatCurrency(summary.consumptionStats.annualBottledSpend, summary.country)}",
                color = TextPrimary,
                style = MaterialTheme.typography.bodySmall
            )
            val equipmentLine = summary.resolvedCartItems.joinToString(", ") { "${it.quantity}× ${it.model.name}" }
            Text(
                text = "• Equipos PSA: $equipmentLine",
                color = EmeraldSuccess,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall
            )
            if (summary.testPhotoPath.isNotBlank()) {
                Text(
                    text = "• Foto real del test adjunta al PDF",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp
                )
            }
        }

        val exportPdfAction: () -> Unit = {
            val fileRes = PsaDemoReportExporter.generateDiagnosticPdf(context, summary)
            fileRes.fold(
                onSuccess = { pdfFile ->
                    PsaDemoReportExporter.shareDiagnosticPdf(context, pdfFile).onFailure { err ->
                        Toast.makeText(context, "PDF guardado, pero no se pudo abrir visor: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onFailure = { err ->
                    Toast.makeText(context, "Error generando PDF: ${err.message}", Toast.LENGTH_LONG).show()
                }
            )
            Unit
        }

        val shareWhatsappAction: () -> Unit = {
            PsaDemoReportExporter.shareDiagnosticViaWhatsApp(context, summary).onFailure { err ->
                Toast.makeText(context, "No se pudo compartir diagnóstico: ${err.message}", Toast.LENGTH_LONG).show()
            }
            Unit
        }

        if (isLargeFont) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = exportPdfAction,
                    colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("demo_btn_export_pdf")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar diagnóstico en PDF", tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar a PDF", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = shareWhatsappAction,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("demo_btn_share_whatsapp")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Compartir diagnóstico por WhatsApp", tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compartir por WhatsApp", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = exportPdfAction,
                    colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("demo_btn_export_pdf")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar diagnóstico en PDF", tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar PDF", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(
                    onClick = shareWhatsappAction,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("demo_btn_share_whatsapp")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Compartir diagnóstico por WhatsApp", tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WellWaterDemoModules(
    selectedModule: WellDemoModuleFilter,
    selectedStripIdx: Int,
    onSelectStripIdx: (Int) -> Unit,
    adultsCount: Int,
    onAdultsChange: (Int) -> Unit,
    childrenCount: Int,
    onChildrenChange: (Int) -> Unit,
    hasHotWeatherOrSport: Boolean,
    onToggleHotWeather: () -> Unit,
    hasPregnancyOrNursing: Boolean,
    onTogglePregnancy: () -> Unit,
    includeInfusions: Boolean,
    onToggleInfusions: () -> Unit,
    includeCooking: Boolean,
    onToggleCooking: () -> Unit,
    containerOption: DemoContainerOption,
    onSelectContainer: (DemoContainerOption) -> Unit,
    containerPriceText: String,
    onContainerPriceChange: (String) -> Unit,
    allPurifiers: List<PsaPurifierModel>,
    wellCartQuantities: Map<String, Int>,
    onToggleWellCartItem: (String) -> Unit,
    onUpdateWellCartQuantity: (String, Int) -> Unit,
    activeCountry: PsaCountryConfig,
    isLargeFont: Boolean,
    cardPadding: Dp,
    showListPrice: Boolean,
    onToggleShowPrice: () -> Unit,
    clientLabel: String,
    onClientLabelChange: (String) -> Unit,
    onRequestCartBudget: (List<DemoCartSelection>, DemoBudgetPaymentMode) -> Unit,
    wellPhotoPath: String = "",
    onWellPhotoPathChange: (String) -> Unit = {},
    onSetWellCartRecommended: ((List<String>) -> Unit)? = null,
    activeVisit: PsaHomeVisit? = null,
    onClearActiveVisit: (() -> Unit)? = null,
    onSaveDiagnosticToVisit: ((PsaHomeVisit, Int, Double, String, String) -> Unit)? = null
): List<@Composable () -> Unit> {
    var selectedRiskIdx by remember { mutableIntStateOf(0) }
    var wellBoilStageIdx by remember { mutableIntStateOf(2) }
    var wellComparisonModelId by remember { mutableStateOf("rotech") }
    var wellDetectedLabel by remember { mutableStateOf("") }
    var wellDetectedSwatchInt by remember { mutableStateOf<Int?>(null) }
    var wellDetectedHex by remember { mutableStateOf("") }
    var wellQualityTitle by remember { mutableStateOf("") }
    var wellRequiresFiltering by remember { mutableStateOf<Boolean?>(null) }
    var wellRecEquipment by remember { mutableStateOf("") }
    var wellRecReason by remember { mutableStateOf("") }
    var wellRecIds by remember { mutableStateOf<List<String>>(emptyList()) }

    val stages = PsaHardnessStripCatalog.stages
    val activeStrip = stages[selectedStripIdx.coerceIn(0, stages.lastIndex)]

    val consumptionStats = remember(
        adultsCount,
        childrenCount,
        hasHotWeatherOrSport,
        hasPregnancyOrNursing,
        includeInfusions,
        includeCooking,
        containerOption,
        containerPriceText
    ) {
        PsaDemoReportExporter.calculateRealConsumptionStats(
            adultsCount = adultsCount,
            childrenCount = childrenCount,
            hasHotWeatherOrSport = hasHotWeatherOrSport,
            hasPregnancyOrNursing = hasPregnancyOrNursing,
            includeInfusions = includeInfusions,
            includeCooking = includeCooking,
            containerOption = containerOption,
            containerUnitPrice = containerPriceText.toDoubleOrNull()?.coerceAtLeast(10.0) ?: 1000.0
        )
    }

    val wellModels = remember(allPurifiers) {
        val ids = listOf("rotech", "ropot", "domus")
        ids.mapNotNull { id -> allPurifiers.firstOrNull { it.id == id } }.ifEmpty { allPurifiers.take(3) }
    }
    val comparisonModel = wellModels.firstOrNull { it.id == wellComparisonModelId } ?: wellModels.first()
    val wellCartLines = remember(allPurifiers, wellCartQuantities) {
        allPurifiers.mapNotNull { model ->
            val qty = wellCartQuantities[model.id] ?: 0
            if (qty > 0) DemoCartLineDetail(model, qty) else null
        }
    }
    val primaryForWellReport = wellCartLines.firstOrNull()?.model ?: wellModels.first()
    val secondaryForWellReport = wellCartLines.getOrNull(1)?.model

    val show: (WellDemoModuleFilter) -> Boolean = { filter ->
        selectedModule == WellDemoModuleFilter.ALL || selectedModule == filter
    }

    val blocks = mutableListOf<@Composable () -> Unit>()

    if (show(WellDemoModuleFilter.TIRA_DUREZA)) {
        blocks.add {
            val animStripColor by animateColorAsState(
                targetValue = Color(activeStrip.swatchHex),
                animationSpec = tween(250),
                label = "well_strip_color"
            )
            val animPipeClog by animateFloatAsState(
                targetValue = (activeStrip.index.toFloat() / 5f) * 0.74f,
                animationSpec = tween(280),
                label = "well_pipe_clog"
            )

            DemoSharedCard(
                icon = Icons.Default.Science,
                title = "1. Tira Reactiva de Dureza PSA",
                accent = animStripColor,
                padding = cardPadding
            ) {

                // Vasos / Tiras lado a lado
                val untreatedStripBox: @Composable (Modifier) -> Unit = { mod ->
                    Column(
                        modifier = mod
                            .clip(RoundedCornerShape(14.dp))
                            .background(SlateBackground)
                            .border(1.dp, animStripColor.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Pozo Sin Tratar", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        // Visualización de tira blanca con almohadilla coloreada
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp)
                        ) {
                            Text("Tira PSA", color = Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .size(width = 68.dp, height = 34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(animStripColor)
                                    .border(1.5.dp, Color.Black.copy(alpha = 0.25f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(activeStrip.ppmCaCO3, color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.5.sp)
                            }
                        }
                        Text(activeStrip.colorName, color = animStripColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(activeStrip.mmolPerLiter, color = TextMuted, fontSize = 11.sp)
                    }
                }

                val treatedStripBox: @Composable (Modifier) -> Unit = { mod ->
                    val emeraldPad = Color(stages.first().swatchHex)
                    Column(
                        modifier = mod
                            .clip(RoundedCornerShape(14.dp))
                            .background(SlateBackground)
                            .border(1.dp, emeraldPad.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Tratada PSA (RO)", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp)
                        ) {
                            Text("Tira PSA", color = Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .size(width = 68.dp, height = 34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(emeraldPad),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("0 ppm", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.5.sp)
                            }
                        }
                        Text("Verde brillante", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("0.00 mmol/L • Pura", color = TextMuted, fontSize = 11.sp)
                    }
                }

                if (isLargeFont) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        untreatedStripBox(Modifier.fillMaxWidth().testTag("well_strip_untreated"))
                        treatedStripBox(Modifier.fillMaxWidth().testTag("well_strip_treated"))
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        untreatedStripBox(Modifier.weight(1f).testTag("well_strip_untreated"))
                        treatedStripBox(Modifier.weight(1f).testTag("well_strip_treated"))
                    }
                }

                PsaRealTestPhotoCard(
                    isWellWater = true,
                    savedPhotoPath = wellPhotoPath,
                    detectedLabel = wellDetectedLabel,
                    detectedSwatchColorInt = wellDetectedSwatchInt,
                    detectedHex = wellDetectedHex,
                    detectedQualityTitle = wellQualityTitle,
                    detectedRequiresFiltering = wellRequiresFiltering,
                    detectedRecommendedEquipment = wellRecEquipment,
                    detectedRecommendationReason = wellRecReason,
                    detectedRecommendedIds = wellRecIds,
                    onApplyRecommendedCart = onSetWellCartRecommended,
                    onPhotoAnalyzed = { outcome ->
                        onWellPhotoPathChange(outcome.savedPhotoPath)
                        wellDetectedSwatchInt = outcome.sampledColorInt
                        wellDetectedHex = outcome.sampledHexLabel
                        outcome.hardnessResult?.let { hr ->
                            wellDetectedLabel = hr.confidenceLabel
                            wellQualityTitle = hr.qualityStatusTitle
                            wellRequiresFiltering = hr.requiresFiltering
                            wellRecEquipment = hr.recommendedEquipmentLabel
                            wellRecReason = hr.recommendationSummary
                            wellRecIds = hr.recommendedCartIds
                            onSelectStripIdx(hr.stageIndex)
                        }
                    },
                    onClearPhoto = {
                        onWellPhotoPathChange("")
                        wellDetectedLabel = ""
                        wellDetectedSwatchInt = null
                        wellDetectedHex = ""
                        wellQualityTitle = ""
                        wellRequiresFiltering = null
                        wellRecEquipment = ""
                        wellRecReason = ""
                        wellRecIds = emptyList()
                    },
                    isLargeFont = isLargeFont
                )

                // Escala de colores oficial de 6 bloques
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateBackground)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Toca el color obtenido en la tira reactiva PSA:",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        stages.forEach { st ->
                            val isSelected = st.index == activeStrip.index
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectStripIdx(st.index) }
                                    .testTag("well_strip_pad_${st.index}"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(st.swatchHex))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else SlateCardBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = st.ppmCaCO3.replace(" ppm", ""),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = if (isSelected) "▲" else "ppm",
                                    color = if (isSelected) Color.White else TextMuted,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Lectura completa + cañería con sarro
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(animStripColor.copy(alpha = 0.14f))
                        .border(1.dp, animStripColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${activeStrip.colorName} • ${activeStrip.hardnessLevel} (${activeStrip.ppmCaCO3} / ${activeStrip.mmolPerLiter})",
                        color = animStripColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(activeStrip.dissolvedParticlesNote, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text("Impacto en el hogar: ${activeStrip.homeImpact}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val r = size.minDimension / 2f - 4.dp.toPx()
                                val center = Offset(size.width / 2f, size.height / 2f)
                                drawCircle(color = Color(0xFF475569), radius = r, center = center, style = Stroke(width = 6.dp.toPx()))
                                val sw = r * 0.72f * animPipeClog
                                if (sw > 1f) {
                                    drawCircle(
                                        color = Color(0xFFE2E8F0).copy(alpha = 0.85f),
                                        radius = r - 3.dp.toPx() - sw / 2f,
                                        center = center,
                                        style = Stroke(width = sw, cap = StrokeCap.Round)
                                    )
                                }
                                drawCircle(
                                    color = PsaCyanPrimary.copy(alpha = 0.75f),
                                    radius = (r - 4.dp.toPx() - sw).coerceAtLeast(4.dp.toPx()),
                                    center = center
                                )
                            }
                            Text(
                                text = "${(100f - animPipeClog * 75f).toInt()}%",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = activeStrip.recommendedAction,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (show(WellDemoModuleFilter.RIESGOS_NAPA)) {
        blocks.add {
            val risks = remember {
                listOf(
                    WellInvisibilityRiskInfo(
                        title = "Arsénico Natural (HACRE)",
                        badge = "Sin olor ni sabor",
                        severityPct = 0.94f,
                        accent = RoseError,
                        explanation = "En gran parte de Argentina y Sudamérica, el agua de napa arrastra arsénico disuelto de origen geológico. Es incoloro e insípido y se acumula con los años.",
                        whySimpleFilterFails = "El carbón activado común o un filtro de sedimentos NO retienen arsénico ni sales disueltas.",
                        psaSolution = "Ósmosis Inversa (PSA ROtech·UV o PSA Ropot): membrana semipermeable capaz de reducir hasta >95% de arsénico y metales pesados."
                    ),
                    WellInvisibilityRiskInfo(
                        title = "Nitratos y Agroquímicos",
                        badge = "Infiltración de pozo ciego",
                        severityPct = 0.86f,
                        accent = AmberDelay,
                        explanation = "Los nitratos provienen de napas superficiales, pozos absorbentes o fertilizantes. Resultan especialmente delicados en bebés, niños y embarazadas.",
                        whySimpleFilterFails = "Hervir el agua NO elimina nitratos: los concentra aún más por evaporación.",
                        psaSolution = "PSA ROtech·UV / PSA Ropot retienen nitratos disueltos y entregan agua liviana segura para mamaderas y comidas."
                    ),
                    WellInvisibilityRiskInfo(
                        title = "Bacterias y Virus de Napa",
                        badge = "Sin desinfección previa",
                        severityPct = 0.92f,
                        accent = PurpleNeon,
                        explanation = "A diferencia de la red pública, el pozo domiciliario no tiene control municipal diario ni barrera sanitaria constante frente a filtraciones de lluvia.",
                        whySimpleFilterFails = "Un filtro mecánico común puede dejar pasar bacterias microscópicas o virus.",
                        psaSolution = "PSA ROtech·UV suma lámpara UV bactericida de última generación tras la ósmosis inversa para doble seguridad microbiológica."
                    )
                )
            }
            val activeRisk = risks[selectedRiskIdx]
            val animRisk by animateFloatAsState(targetValue = activeRisk.severityPct, animationSpec = tween(250), label = "well_risk")

            DemoSharedCard(
                icon = Icons.Default.WarningAmber,
                title = "2. Riesgos Invisibles en Agua de Pozo",
                accent = RoseError,
                padding = cardPadding
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Arsénico (HACRE)", "Nitratos", "Bacterias / UV").forEachIndexed { idx, lbl ->
                        DemoSharedChip(
                            label = lbl,
                            selected = selectedRiskIdx == idx,
                            selectedColor = risks[idx].accent,
                            onClick = { selectedRiskIdx = idx },
                            testTag = "well_risk_chip_$idx"
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateBackground)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(activeRisk.title, color = activeRisk.accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(activeRisk.badge, color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                    LinearProgressIndicator(
                        progress = { animRisk },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = activeRisk.accent,
                        trackColor = SlateCardBorder
                    )
                    Text(activeRisk.explanation, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text("Por qué no alcanza un filtro simple: ${activeRisk.whySimpleFilterFails}", color = AmberDelay, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("Solución PSA: ${activeRisk.psaSolution}", color = EmeraldSuccess, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (show(WellDemoModuleFilter.HERVOR_POZO)) {
        blocks.add {
            val boilStages = remember {
                listOf(
                    WellBoilingStageInfo("Sin Hervir", "0 min", 1.0f, "100% sales base", PsaCyanGlow, "Mantiene el nivel original de dureza, nitratos y minerales de napa."),
                    WellBoilingStageInfo("Hervor Corto", "3 min", 0.88f, "+14% sales / arsénico", AmberDelay, "Se evapora agua pura (H2O), pero el sarro, nitratos y arsénico NO se evaporan: quedan un 14% más concentrados."),
                    WellBoilingStageInfo("Hervor Prolongado", "10 min", 0.74f, "+35% concentración tóxica", RoseError, "Hervir agua de pozo NO quita dureza ni químicos disueltos: concentra hasta 35% más los nitratos, sales y sarro en la pava y en la comida.")
                )
            }
            val currentBoil = boilStages[wellBoilStageIdx]

            DemoSharedCard(
                icon = Icons.Default.LocalFireDepartment,
                title = "3. ¿Sirve hervir el Agua de Pozo?",
                accent = AmberDelay,
                padding = cardPadding
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    boilStages.forEachIndexed { idx, st ->
                        DemoSharedChip(
                            label = "${st.label} (${st.minutesLabel})",
                            selected = wellBoilStageIdx == idx,
                            selectedColor = st.accent,
                            onClick = { wellBoilStageIdx = idx },
                            testTag = "well_boil_stage_$idx"
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateBackground)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Agua restante: ${(currentBoil.waterRemainingPct * 100).toInt()}%", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        Text(currentBoil.mineralConcentrationBadge, color = currentBoil.accent, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                    LinearProgressIndicator(
                        progress = { currentBoil.waterRemainingPct },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = currentBoil.accent,
                        trackColor = SlateCardBorder
                    )
                    Text(currentBoil.explanation, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text("Recomendación: Desalinizar primero con PSA ROtech·UV o Ropot antes de preparar mate, mamaderas o sopa.", color = EmeraldSuccess, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (show(WellDemoModuleFilter.HIDRATACION_GASTO)) {
        blocks.add {
            SharedHydrationByPeopleCard(
                moduleTitle = "4A. Agua Diaria Recomendada en Casa",
                adultsCount = adultsCount,
                onAdultsChange = onAdultsChange,
                childrenCount = childrenCount,
                onChildrenChange = onChildrenChange,
                hasHotWeatherOrSport = hasHotWeatherOrSport,
                onToggleHotWeather = onToggleHotWeather,
                hasPregnancyOrNursing = hasPregnancyOrNursing,
                onTogglePregnancy = onTogglePregnancy,
                baseHydrationLitersDay = consumptionStats.baseHydrationLitersDay,
                isLargeFont = isLargeFont,
                cardPadding = cardPadding
            )
        }
        blocks.add {
            SharedRealBottledExpenseCard(
                moduleTitle = "4B. Gasto Real en Envasada vs Ósmosis PSA",
                consumptionStats = consumptionStats,
                includeInfusions = includeInfusions,
                onToggleInfusions = onToggleInfusions,
                includeCooking = includeCooking,
                onToggleCooking = onToggleCooking,
                containerOption = containerOption,
                onSelectContainer = onSelectContainer,
                containerPriceText = containerPriceText,
                onContainerPriceChange = onContainerPriceChange,
                highlightedModel = comparisonModel,
                collapsibleOtherModels = wellModels,
                onSelectComparisonModel = { wellComparisonModelId = it },
                activeCountry = activeCountry,
                isLargeFont = isLargeFont,
                cardPadding = cardPadding
            )
        }
    }

    if (show(WellDemoModuleFilter.SOLUCIONES_POZO)) {
        blocks.add {
            DemoSharedCard(
                icon = Icons.Default.VerifiedUser,
                title = "5. Soluciones PSA para Agua de Pozo",
                accent = PsaCyanGlow,
                padding = cardPadding
            ) {
                PsaAccordionEquipmentCartSelector(
                    allPurifiers = allPurifiers,
                    prioritizedIds = listOf("rotech", "ropot", "domus"),
                    cartQuantities = wellCartQuantities,
                    onToggleItem = onToggleWellCartItem,
                    onUpdateQuantity = onUpdateWellCartQuantity,
                    country = activeCountry,
                    isLargeFont = isLargeFont,
                    showListPrice = showListPrice,
                    onToggleShowPrice = onToggleShowPrice,
                    onRequestCartBudget = onRequestCartBudget
                )
            }
        }

        blocks.add {
            val totalCartPrice = wellCartLines.sumOf { it.subtotal }.takeIf { it > 0.0 } ?: primaryForWellReport.price
            val annualPsaAmortized = totalCartPrice / 3.0
            val wellCartText = wellCartLines.joinToString(", ") { "${it.quantity}x ${it.model.name}" }
                .ifBlank { primaryForWellReport.name }
            val diagnosticSummary = PsaDemoDiagnosticSummary(
                clientLabel = clientLabel,
                country = activeCountry,
                waterSource = com.example.util.DemoWaterSource.AGUA_DE_POZO,
                hardnessStage = activeStrip,
                arsenicRiskSummary = "Agua de napa con posible presencia de arsénico, nitratos o carga microbiológica; hervir concentra sales y contaminantes.",
                consumptionStats = consumptionStats,
                primaryModel = primaryForWellReport,
                secondaryModel = secondaryForWellReport,
                cartItems = wellCartLines,
                annualPsaCostAmortized = annualPsaAmortized,
                annualNetSavings = consumptionStats.annualBottledSpend - annualPsaAmortized,
                threeYearNetSavings = consumptionStats.threeYearBottledSpend - totalCartPrice,
                plasticBottlesAvoidedYear = ((consumptionStats.totalRealLitersMonth * 12.0) / 1.5).toInt(),
                showListPrice = showListPrice,
                testPhotoPath = wellPhotoPath
            )

            PsaDiagnosticExportCard(
                summary = diagnosticSummary,
                clientLabel = clientLabel,
                onClientLabelChange = onClientLabelChange,
                isLargeFont = isLargeFont,
                cardPadding = cardPadding,
                activeVisit = activeVisit,
                onClearActiveVisit = onClearActiveVisit,
                onSaveDiagnosticToVisit = onSaveDiagnosticToVisit?.let { saver ->
                    { visit ->
                        saver(
                            visit,
                            activeStrip.ppmNumeric,
                            consumptionStats.totalRealLitersDay,
                            wellCartText,
                            wellPhotoPath
                        )
                    }
                }
            )
        }
    }

    return blocks
}
