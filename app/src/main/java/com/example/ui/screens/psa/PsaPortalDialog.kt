package com.example.ui.screens.psa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PsaPricingConfig
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalIsLargeFontMode
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
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PsaSyncUiState
import com.example.util.PsaCountryConfig
import com.example.util.PsaCountryRegistry
import com.example.util.PsaPortalSyncManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsaPortalCredentialsDialog(
    context: Context,
    viewModel: MainViewModel,
    pricingConfig: PsaPricingConfig,
    activeCountry: PsaCountryConfig,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var credentials by remember {
        mutableStateOf(PsaPortalSyncManager.getCredentials(context))
    }

    var centerInput by remember { mutableStateOf(credentials.dipCenter) }
    var dipInput by remember { mutableStateOf(credentials.dipNumber) }
    var passwordInput by remember { mutableStateOf(credentials.password) }
    var rememberCredentials by remember { mutableStateOf(credentials.rememberCredentials) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isSyncing by remember { mutableStateOf(false) }
    var syncFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var syncSuccess by remember { mutableStateOf<Boolean?>(null) }
    var activePromos by remember { mutableStateOf(PsaPortalSyncManager.getActivePromotions()) }
    var showCountryPicker by remember { mutableStateOf(false) }

    val isLargeFont = LocalIsLargeFontMode.current

    AlertDialog(
        onDismissRequest = { if (!isSyncing) onDismiss() },
        containerColor = Color(0xFF021625),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PsaCyanPrimary.copy(alpha = 0.2f))
                        .border(1.dp, PsaCyanGlow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = PsaCyanGlow,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Acceso DIP • ${activeCountry.flag} ${activeCountry.name}",
                        color = PsaCyanGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    )
                    Text(
                        text = "mi.psa.com.ar/login • ${activeCountry.currencyCode}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Country Selector Chip inside DIP Dialog
                OutlinedButton(
                    onClick = { showCountryPicker = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.55f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dip_dialog_country_selector")
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp), tint = PsaCyanGlow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "País activo: ${activeCountry.flag} ${activeCountry.name} (${activeCountry.currencyCode})",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Cambiar", color = PsaCyanGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Connection Status Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (credentials.isConnected) Color(0xFF04281E) else Color(0xFF041E2F)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (credentials.isConnected) EmeraldSuccess.copy(alpha = 0.5f) else PsaCyanPrimary.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (credentials.isConnected) Icons.Default.CheckCircle else Icons.Default.Badge,
                            contentDescription = null,
                            tint = if (credentials.isConnected) EmeraldSuccess else PsaCyanGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (credentials.isConnected && credentials.dipNumber.isNotBlank())
                                    "DIP #${credentials.dipNumber} guardado"
                                else
                                    "Configura tu ingreso oficial PSA",
                                color = if (credentials.isConnected) EmeraldSuccess else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            if (credentials.lastSyncDate.isNotEmpty()) {
                                Text(
                                    text = "Última verificación: ${credentials.lastSyncDate}",
                                    color = TextSecondary,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }
                }

                // Centro + 8 últimos dígitos (responsive stack on 140%/160%)
                if (isLargeFont) {
                    OutlinedTextField(
                        value = centerInput,
                        onValueChange = { centerInput = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Centro (1-99)", fontSize = 12.sp) },
                        placeholder = { Text("Opcional", fontSize = 11.5.sp, color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_portal_dip_center")
                    )

                    OutlinedTextField(
                        value = dipInput,
                        onValueChange = { dipInput = it.filter { ch -> ch.isDigit() }.take(10) },
                        label = { Text("8 últimos dígitos DIP", fontSize = 12.sp) },
                        placeholder = { Text("Ej. 12345678", fontSize = 11.5.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanGlow,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_portal_dip")
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = centerInput,
                            onValueChange = { centerInput = it.filter { ch -> ch.isDigit() }.take(2) },
                            label = { Text("Centro", fontSize = 11.5.sp) },
                            placeholder = { Text("1-99", fontSize = 11.sp, color = TextMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PsaCyanGlow,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(0.35f)
                                .testTag("input_portal_dip_center")
                        )

                        OutlinedTextField(
                            value = dipInput,
                            onValueChange = { dipInput = it.filter { ch -> ch.isDigit() }.take(10) },
                            label = { Text("8 dígitos DIP", fontSize = 11.5.sp) },
                            placeholder = { Text("12345678", fontSize = 11.sp, color = TextMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PsaCyanGlow,
                                unfocusedBorderColor = SlateCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(0.65f)
                                .testTag("input_portal_dip")
                        )
                    }
                }

                // Clave PSA
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Clave PSA", fontSize = 12.sp) },
                    placeholder = { Text("Contraseña de mi.psa.com.ar", fontSize = 11.5.sp, color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Ocultar" else "Mostrar",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsaCyanGlow,
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_portal_password")
                )

                // Remember Credentials Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Autocompletar ingreso DIP",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = rememberCredentials,
                        onCheckedChange = { rememberCredentials = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = PsaCyanPrimary
                        )
                    )
                }

                // Sync Feedback message
                if (syncFeedbackMessage != null) {
                    val isSuccess = syncSuccess == true
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) EmeraldSuccess.copy(alpha = 0.15f) else AmberDelay.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, if (isSuccess) EmeraldSuccess else AmberDelay),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isSuccess) EmeraldSuccess else AmberDelay,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = syncFeedbackMessage ?: "",
                                color = if (isSuccess) EmeraldSuccess else AmberDelay,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Active Promotions Section
                if (activePromos.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = AmberDelay, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Estado de Catálogo", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        activePromos.forEach { promo ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = BorderStroke(1.dp, SlateCardBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = promo.title, color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                    Text(text = promo.description, color = TextSecondary, fontSize = 10.5.sp)
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
                    coroutineScope.launch {
                        isSyncing = true
                        syncFeedbackMessage = null
                        val result = PsaPortalSyncManager.executePortalSync(
                            context = context,
                            dipCenter = centerInput,
                            dipNumber = dipInput,
                            password = passwordInput,
                            rememberCredentials = rememberCredentials,
                            currentPricing = pricingConfig,
                            countryCode = activeCountry.code
                        )
                        isSyncing = false
                        syncSuccess = result.success
                        syncFeedbackMessage = result.message
                        credentials = PsaPortalSyncManager.getCredentials(context)
                        if (result.activePromotions.isNotEmpty()) {
                            activePromos = result.activePromotions
                        }
                        if (result.success && result.updatedPricingConfig != null) {
                            viewModel.savePsaPricingConfig(context, result.updatedPricingConfig)
                            Toast.makeText(context, "Precios oficiales actualizados", Toast.LENGTH_SHORT).show()
                        } else if (dipInput.isNotBlank() && passwordInput.isNotBlank()) {
                            Toast.makeText(context, "Acceso DIP guardado para mi.psa.com.ar", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_execute_portal_sync")
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sincronizando...", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar y Sincronizar", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSyncing,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cerrar", color = TextSecondary, fontSize = 12.sp)
            }
        }
    )

    if (showCountryPicker) {
        PsaCountryPickerDialog(
            activeCountry = activeCountry,
            onSelectCountry = { code ->
                viewModel.switchPsaCountry(context, code)
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false }
        )
    }
}

@Composable
fun PsaCountryPickerDialog(
    activeCountry: PsaCountryConfig,
    onSelectCountry: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF021625),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Public, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("País Oficial PSA", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PsaCountryRegistry.countries.forEach { country ->
                    val selected = country.code == activeCountry.code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) PsaCyanPrimary.copy(alpha = 0.2f) else SlateBackground)
                            .border(
                                1.dp,
                                if (selected) PsaCyanGlow else SlateCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectCountry(country.code) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("country_option_${country.code}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(country.flag, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = country.name,
                                    color = if (selected) Color.White else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${country.currencyCode} (${country.currencySymbol}) • ${country.officialSiteUrl.removePrefix("https://").trimEnd('/')}",
                                    color = TextSecondary,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cerrar", color = TextSecondary)
            }
        }
    )
}
