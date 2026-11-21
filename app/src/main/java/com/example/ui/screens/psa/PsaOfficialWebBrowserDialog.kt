package com.example.ui.screens.psa

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PsaPricingConfig
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalIsLargeFontMode
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.PsaCountryConfig
import com.example.util.PsaCountryRegistry
import com.example.util.PsaPdfTextExtractor
import com.example.util.PsaPortalSyncManager
import com.example.util.PsaPriceTextParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PsaOfficialWebBrowserDialog(
    context: Context,
    viewModel: MainViewModel,
    pricingConfig: PsaPricingConfig,
    activeCountry: PsaCountryConfig,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val credentials = remember { PsaPortalSyncManager.getCredentials(context) }
    val savedDipListUrl = remember(activeCountry.code) {
        PsaPortalSyncManager.getSavedDipPriceListUrl(context, activeCountry.code)
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember(activeCountry.code) {
        mutableStateOf(
            savedDipListUrl
                ?: if (credentials.dipNumber.isNotBlank() && credentials.password.isNotBlank()) {
                    activeCountry.dipLoginUrl
                } else if (activeCountry.hasPublicStore) {
                    activeCountry.officialStoreUrl
                } else {
                    activeCountry.dipLoginUrl
                }
        )
    }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var isExtracting by remember { mutableStateOf(false) }
    var hasSubmittedAutoLogin by remember { mutableStateOf(false) }
    var pageLoadError by remember { mutableStateOf<String?>(null) }

    var pendingParseResult by remember { mutableStateOf<PsaPriceTextParser.PsaParsedPriceResult?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isExtracting = true
                val parsed = withContext(Dispatchers.IO) {
                    val text = PsaPdfTextExtractor.extractTextFromUri(context, uri)
                    PsaPriceTextParser.parseText(text, pricingConfig, activeCountry.code)
                }
                isExtracting = false
                if (parsed.items.isNotEmpty()) {
                    pendingParseResult = parsed
                } else {
                    Toast.makeText(
                        context,
                        "El PDF no contiene precios PVP legibles. Copia la tabla o usa Lista DIP.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val bridge = remember(pricingConfig, activeCountry.code) {
        PsaDomExtractorBridge(
            context = context,
            currentPricing = pricingConfig,
            countryCode = activeCountry.code,
            onCredentialsSaved = { cleanDip ->
                Toast.makeText(
                    context,
                    "Acceso DIP #$cleanDip guardado automáticamente",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onCandidatesForConfirmation = { result ->
                isExtracting = false
                if (result.items.isNotEmpty()) {
                    pendingParseResult = result
                } else {
                    Toast.makeText(
                        context,
                        "No se detectaron PVP en esta vista. Abre la Lista de Precios o usa PDF.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            color = Color(0xFF011422)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- CLEAN TOP ACTION BAR ---
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF021727))
                        .border(1.dp, PsaCyanPrimary.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = PsaCyanGlow
                            )
                        }
                        Text(
                            text = "${activeCountry.flag} Portal DIP",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { webViewInstance?.reload() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Recargar",
                                tint = PsaCyanGlow,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Button(
                            onClick = {
                                isExtracting = true
                                webViewInstance?.evaluateJavascript(
                                    PsaDomExtractorBridge.EXTRACT_STRICT_PVP_ON_DEMAND_JS,
                                    null
                                )
                            },
                            enabled = !isExtracting,
                            colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("btn_extract_psa_live_web")
                        ) {
                            if (isExtracting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Leyendo...", color = Color.Black, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PriceCheck,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Leer PVP", color = Color.Black, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // --- MINIMAL 1-ROW QUICK BAR ---
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF02111E))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            pageLoadError = null
                            hasSubmittedAutoLogin = false
                            val target = PsaPortalSyncManager.getSavedDipPriceListUrl(context, activeCountry.code)
                                ?: activeCountry.dipLoginUrl
                            currentUrl = target
                            webViewInstance?.loadUrl(target)
                        },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 32.dp),
                        border = BorderStroke(1.dp, if (currentUrl.contains("mi.psa.com.ar")) PsaCyanGlow else SlateCardBorder)
                    ) {
                        Text("Lista DIP", fontSize = 10.5.sp, color = if (currentUrl.contains("mi.psa.com.ar")) PsaCyanGlow else TextSecondary, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (currentUrl.startsWith("http")) {
                                PsaPortalSyncManager.saveDipPriceListUrl(context, activeCountry.code, currentUrl)
                                Toast.makeText(context, "URL de Lista DIP guardada", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 32.dp),
                        border = BorderStroke(1.dp, SlateCardBorder)
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(13.dp), tint = PsaCyanGlow)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fijar URL", fontSize = 10.5.sp, color = TextSecondary)
                    }

                    if (activeCountry.hasPublicStore) {
                        OutlinedButton(
                            onClick = {
                                pageLoadError = null
                                currentUrl = activeCountry.officialStoreUrl
                                webViewInstance?.loadUrl(currentUrl)
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 32.dp),
                            border = BorderStroke(1.dp, if (currentUrl.contains("tienda.psa")) EmeraldSuccess else SlateCardBorder)
                        ) {
                            Text("Tienda", fontSize = 10.5.sp, color = if (currentUrl.contains("tienda.psa")) EmeraldSuccess else TextSecondary)
                        }
                    }

                    OutlinedButton(
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 32.dp),
                        border = BorderStroke(1.dp, SlateCardBorder)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(13.dp), tint = AmberDelay)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", fontSize = 10.5.sp, color = AmberDelay, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadingProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = PsaCyanGlow,
                        trackColor = Color(0xFF021727)
                    )
                }

                // --- FULLSCREEN WEBVIEW (ZERO FLOATING CLUTTER) ---
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    loadsImagesAutomatically = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
                                }

                                addJavascriptInterface(bridge, "PsaNativeBridge")

                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        loadingProgress = newProgress / 100f
                                        isLoading = newProgress < 100
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                        pageLoadError = null
                                        if (url != null) currentUrl = url
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        if (url != null) currentUrl = url
                                        try {
                                            CookieManager.getInstance().flush()
                                        } catch (_: Exception) {}

                                        if (url != null && url.contains("mi.psa.com", ignoreCase = true)) {
                                            // Always attach passive auto-capture listener so logging in saves Centro/DIP/Clave automatically
                                            view?.evaluateJavascript(PsaDomExtractorBridge.CAPTURE_DIP_LOGIN_JS, null)

                                            if (!hasSubmittedAutoLogin) {
                                                val creds = PsaPortalSyncManager.getCredentials(context)
                                                if (creds.dipNumber.isNotBlank() && creds.password.isNotBlank()) {
                                                    view?.evaluateJavascript(
                                                        PsaDomExtractorBridge.buildAutoLoginScript(
                                                            dipCenter = creds.dipCenter,
                                                            dipNumber = creds.dipNumber,
                                                            password = creds.password
                                                        )
                                                    ) { result ->
                                                        if (result != null && result.contains("SUBMITTED")) {
                                                            hasSubmittedAutoLogin = true
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val dest = request?.url?.toString() ?: return false
                                        return if (dest.startsWith("http://") || dest.startsWith("https://")) {
                                            false
                                        } else {
                                            try {
                                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(dest)))
                                                true
                                            } catch (_: Exception) {
                                                false
                                            }
                                        }
                                    }

                                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                        handler?.proceed()
                                    }

                                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                        if (request?.isForMainFrame == true) {
                                            pageLoadError = error?.description?.toString() ?: "Sin conexión"
                                            isLoading = false
                                        }
                                    }
                                }

                                loadUrl(currentUrl)
                                webViewInstance = this
                            }
                        },
                        update = { webViewInstance = it }
                    )

                    if (pageLoadError != null) {
                        Button(
                            onClick = {
                                pageLoadError = null
                                webViewInstance?.reload()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reintentar conexión", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    pendingParseResult?.let { result ->
        PsaConfirmPvpDialog(
            parsedResult = result,
            activeCountry = activeCountry,
            onConfirm = { selectedItems ->
                val updatedConfig = PsaPriceTextParser.applySelectedItemsToPricingConfig(
                    currentConfig = pricingConfig,
                    items = selectedItems,
                    detectedInstallments = result.detectedInstallments
                )
                viewModel.savePsaPricingConfig(context, updatedConfig, fromDipPortal = true)
                pendingParseResult = null
                Toast.makeText(
                    context,
                    "${selectedItems.size} precios PVP guardados",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { pendingParseResult = null }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                CookieManager.getInstance().flush()
            } catch (_: Exception) {}
            webViewInstance?.stopLoading()
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }
}

/**
 * Pantalla de confirmación rápida con resumen: [Producto] -> [Precio Anterior] vs [Precio Nuevo Detectado]
 * y acciones Aplicar / Descartar tras lectura estricta del DOM oficial.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaConfirmPvpDialog(
    parsedResult: PsaPriceTextParser.PsaParsedPriceResult,
    activeCountry: PsaCountryConfig,
    onConfirm: (List<PsaPriceTextParser.PsaParsedPriceItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val currencyFormat = remember(activeCountry.code) {
        PsaCountryRegistry.currencyNumberFormat(activeCountry)
    }
    val selectedMap = remember(parsedResult) {
        mutableStateMapOf<String, Boolean>().apply {
            parsedResult.items.forEach { put(it.modelKey, !it.isOutlierWarning) }
        }
    }
    val chosenPriceMap = remember(parsedResult) {
        mutableStateMapOf<String, Double>().apply {
            parsedResult.items.forEach { put(it.modelKey, it.detectedPrice) }
        }
    }
    val isLargeFont = LocalIsLargeFontMode.current

    val selectedCount = parsedResult.items.count { selectedMap[it.modelKey] == true }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF021625),
        modifier = Modifier.imePadding(),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PriceCheck,
                        contentDescription = null,
                        tint = PsaCyanGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Confirmar PVP (${activeCountry.flag} ${activeCountry.code})",
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = parsedResult.detectedNote.ifBlank { "Lectura estricta del portal oficial" } +
                        " • $selectedCount de ${parsedResult.items.size}",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 390.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(parsedResult.items, key = { it.modelKey }) { item ->
                    val isChecked = selectedMap[item.modelKey] == true
                    val chosenPrice = chosenPriceMap[item.modelKey] ?: item.detectedPrice
                    val hasChanged = abs(chosenPrice - item.previousPrice) >= 1.0
                    val diffPct = if (item.previousPrice > 0.0) {
                        ((chosenPrice - item.previousPrice) / item.previousPrice) * 100.0
                    } else 0.0

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isChecked) SlateCard else Color(0xFF0B1420))
                            .border(
                                1.dp,
                                if (item.isOutlierWarning) AmberDelay.copy(alpha = 0.7f)
                                else if (isChecked) PsaCyanGlow.copy(alpha = 0.45f)
                                else SlateCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedMap[item.modelKey] = !isChecked }
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                            .testTag("pvp_confirm_row_${item.modelKey}"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Row 1: Checkbox + [Producto] + % Delta Badge
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { selectedMap[item.modelKey] = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PsaCyanPrimary,
                                        checkmarkColor = Color.Black
                                    ),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.modelName,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }

                            if (hasChanged && item.previousPrice > 0.0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                item.isOutlierWarning -> AmberDelay.copy(alpha = 0.2f)
                                                diffPct > 0 -> EmeraldSuccess.copy(alpha = 0.18f)
                                                else -> PsaCyanPrimary.copy(alpha = 0.18f)
                                            }
                                        )
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${if (diffPct > 0) "+" else ""}${String.format(Locale.US, "%.1f", diffPct)}%",
                                        color = if (item.isOutlierWarning) AmberDelay else if (diffPct > 0) EmeraldSuccess else PsaCyanGlow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Row 2: [Precio Anterior] vs [Precio Nuevo Detectado]
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Anterior",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = if (item.previousPrice > 0.0) currencyFormat.format(item.previousPrice) else "—",
                                    color = TextSecondary,
                                    fontSize = if (isLargeFont) 13.5.sp else 12.5.sp,
                                    textDecoration = if (hasChanged && item.previousPrice > 0.0) TextDecoration.LineThrough else TextDecoration.None
                                )
                            }

                            Text(
                                text = "vs",
                                color = PsaCyanGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )

                            Column {
                                Text(
                                    text = "Nuevo detectado",
                                    color = if (item.isOutlierWarning) AmberDelay else EmeraldSuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = currencyFormat.format(chosenPrice),
                                    color = if (item.isOutlierWarning) AmberDelay else EmeraldSuccess,
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (isLargeFont) 15.sp else 14.sp
                                )
                            }
                        }

                        // Quick candidate selector chips when more than one amount appeared on the row
                        if (item.alternateCandidates.size > 1) {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                item.alternateCandidates.take(3).forEach { cand ->
                                    val isCandSelected = abs(cand.price - chosenPrice) < 1.0
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isCandSelected) PsaCyanPrimary else Color(0xFF0A1D2C))
                                            .border(
                                                1.dp,
                                                if (isCandSelected) PsaCyanGlow else SlateCardBorder,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                chosenPriceMap[item.modelKey] = cand.price
                                                selectedMap[item.modelKey] = true
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "${cand.label}: ${currencyFormat.format(cand.price)}",
                                            color = if (isCandSelected) Color.Black else TextSecondary,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val confirmed = parsedResult.items.mapNotNull { item ->
                        if (selectedMap[item.modelKey] == true) {
                            item.copy(detectedPrice = chosenPriceMap[item.modelKey] ?: item.detectedPrice)
                        } else null
                    }
                    onConfirm(confirmed)
                },
                enabled = selectedCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_apply_pvp_prices")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Aplicar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_discard_pvp_prices")
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Descartar", color = TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
