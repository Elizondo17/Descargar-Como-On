package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CloudAccessToken
import com.example.data.model.CloudAccessTokenStatus
import com.example.data.model.LicenseExpirationSnapshot
import com.example.data.model.LicenseExpirationState
import com.example.data.model.LicenseStatusFormatter
import com.example.data.model.UserAccount
import com.example.data.repository.EzoRepository
import com.example.data.repository.FirestoreTeamRepository
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

private enum class LicenseBoardFilter(val label: String) {
    ALL("Todas"),
    ACTIVE("Activas"),
    EXPIRING("Por Vencer"),
    EXPIRED("Expiradas"),
    BLOCKED("Bloqueadas")
}

private enum class TokenStatusFilter(val label: String) {
    ALL("Todos"),
    AVAILABLE("Disponibles"),
    REDEEMED("Canjeados"),
    REVOKED("Revocados")
}

@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.superuserTokensSection(
    context: Context,
    viewModel: MainViewModel,
    secondaryUsers: List<UserAccount>,
    cloudAccessTokens: List<CloudAccessToken>,
    isCloudGoogleConnected: Boolean,
    isRefreshingTokens: Boolean,
    effectiveInviteUrl: String,
    isLargeFont: Boolean,
    onEditUser: (UserAccount) -> Unit
) {
    item {
        SuperuserTokenSecurityBanner(
            isRefreshingTokens = isRefreshingTokens,
            onRefresh = { viewModel.refreshCloudAccessTokens() }
        )
    }

    item {
        val nowMs = System.currentTimeMillis()
        val licenseSnapshots = secondaryUsers.map { user ->
            user to LicenseStatusFormatter.evaluateUserLicense(user, nowMs)
        }
        val activeCount = licenseSnapshots.count { (_, s) ->
            s.state == LicenseExpirationState.ACTIVE
        }
        val expiringSoonCount = licenseSnapshots.count { (_, s) ->
            s.state == LicenseExpirationState.EXPIRING_SOON ||
                s.state == LicenseExpirationState.CRITICAL_24H
        }
        val restrictedCount = licenseSnapshots.count { (_, s) ->
            s.state == LicenseExpirationState.EXPIRED ||
                s.state == LicenseExpirationState.BLOCKED
        }
        val availableTokensCount = cloudAccessTokens.count {
            it.parsedStatus == CloudAccessTokenStatus.AVAILABLE
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TokenKpiTile(
                value = "$activeCount",
                label = "Activas",
                accent = EmeraldSuccess,
                modifier = if (isLargeFont) Modifier.fillMaxWidth() else Modifier.weight(1f)
            )
            TokenKpiTile(
                value = "$expiringSoonCount",
                label = "Por Vencer",
                accent = AmberDelay,
                modifier = if (isLargeFont) Modifier.fillMaxWidth() else Modifier.weight(1f)
            )
            TokenKpiTile(
                value = "$restrictedCount",
                label = "Venc./Bloq.",
                accent = if (restrictedCount > 0) RoseError else TextMuted,
                modifier = if (isLargeFont) Modifier.fillMaxWidth() else Modifier.weight(1f)
            )
            TokenKpiTile(
                value = "$availableTokensCount",
                label = "Tokens Libres",
                accent = PsaCyanGlow,
                modifier = if (isLargeFont) Modifier.fillMaxWidth() else Modifier.weight(1f)
            )
        }
    }

    item {
        TokenGeneratorCard(
            context = context,
            viewModel = viewModel,
            isCloudGoogleConnected = isCloudGoogleConnected,
            effectiveInviteUrl = effectiveInviteUrl,
            isLargeFont = isLargeFont
        )
    }

    item {
        ActiveLicensesExpirationSection(
            context = context,
            viewModel = viewModel,
            secondaryUsers = secondaryUsers,
            isLargeFont = isLargeFont,
            onEditUser = onEditUser
        )
    }

    item {
        IssuedFirebaseTokensSection(
            context = context,
            viewModel = viewModel,
            tokens = cloudAccessTokens,
            effectiveInviteUrl = effectiveInviteUrl
        )
    }
}

@Composable
private fun SuperuserTokenSecurityBanner(
    isRefreshingTokens: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SlateCard)
            .border(1.dp, PsaCyanGlow.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("superuser_tokens_banner"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PsaCyanGlow.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "Panel exclusivo superusuario",
                tint = PsaCyanGlow,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Gestión de Tokens y Vigencia Firebase",
                color = TextPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Vista exclusiva Superusuario (${FirestoreTeamRepository.BOOTSTRAP_ADMIN_EMAIL}). Emite tokens con días personalizados y monitorea expiraciones en vivo.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onRefresh,
            enabled = !isRefreshingTokens,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(IndigoPrimary.copy(alpha = 0.18f))
                .testTag("token_refresh_button")
        ) {
            if (isRefreshingTokens) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = PsaCyanGlow,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sincronizar tokens desde Firebase",
                    tint = PsaCyanGlow
                )
            }
        }
    }
}

@Composable
private fun TokenKpiTile(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SlateCard)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TokenGeneratorCard(
    context: Context,
    viewModel: MainViewModel,
    isCloudGoogleConnected: Boolean,
    effectiveInviteUrl: String,
    isLargeFont: Boolean
) {
    var assignedUsername by remember { mutableStateOf("") }
    var targetEmail by remember { mutableStateOf("") }
    var customDaysInput by remember { mutableStateOf("30") }
    var maxDevices by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var lastIssuedToken by remember { mutableStateOf<CloudAccessToken?>(null) }

    val parsedDays = LicenseStatusFormatter.sanitizeCustomDays(customDaysInput)
    val previewExpiryStr = parsedDays?.let { days ->
        LicenseStatusFormatter.formatExpirationDate(
            LicenseStatusFormatter.computePreviewExpirationMs(days)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateCard)
            .border(1.dp, IndigoPrimary.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = PsaCyanGlow,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Emitir Nuevo Token de Acceso",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedTextField(
            value = assignedUsername,
            onValueChange = { assignedUsername = it },
            label = { Text("Nombre o Alias del Nuevo Usuario") },
            placeholder = { Text("Ej: Laura PSA Zona Norte") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = IndigoPrimary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PsaCyanGlow,
                unfocusedBorderColor = SlateCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("token_username_input")
        )

        OutlinedTextField(
            value = targetEmail,
            onValueChange = { targetEmail = it },
            label = { Text("Email Google específico (opcional)") },
            placeholder = { Text("Vacío = cualquier cuenta Google con el token") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldSuccess) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PsaCyanGlow,
                unfocusedBorderColor = SlateCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("token_target_email_input")
        )

        Text(
            text = "Días de vigencia a asignar (presets o número exacto):",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(7, 15, 30, 60, 90, 365).forEach { preset ->
                FilterChip(
                    selected = parsedDays == preset,
                    onClick = { customDaysInput = preset.toString() },
                    label = { Text("${preset}d", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customDaysInput,
                onValueChange = { incoming ->
                    val digitsOnly = incoming.filter { it.isDigit() }.take(4)
                    customDaysInput = digitsOnly
                },
                label = { Text("Días exactos (1 - 3650)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = AmberDelay) },
                isError = customDaysInput.isNotBlank() && parsedDays == null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PsaCyanGlow,
                    unfocusedBorderColor = SlateCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("token_custom_days_input")
            )

            // Max Devices Stepper (1..5)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlateBackground)
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (maxDevices > 1) maxDevices-- },
                    enabled = maxDevices > 1,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Restar dispositivo", tint = RoseError, modifier = Modifier.size(18.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$maxDevices", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("Eq.", color = TextMuted, fontSize = 9.5.sp)
                }
                IconButton(
                    onClick = { if (maxDevices < 5) maxDevices++ },
                    enabled = maxDevices < 5,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Sumar dispositivo", tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (previewExpiryStr != null) {
            Text(
                text = "⏳ Al canjearlo hoy vencería el: $previewExpiryStr ($parsedDays días)",
                color = EmeraldSuccess,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Text(
                text = "Ingresa un número válido entre 1 y 3650 días.",
                color = RoseError,
                fontSize = 11.5.sp
            )
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notas / Zona / Plan (opcional)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PsaCyanGlow,
                unfocusedBorderColor = SlateCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("token_notes_input")
        )

        Button(
            onClick = {
                val safeDays = parsedDays
                if (safeDays == null) {
                    Toast.makeText(context, "Ingresa días válidos entre 1 y 3650.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isCreating = true
                viewModel.createCloudAccessToken(
                    assignedUsername = assignedUsername,
                    targetEmail = targetEmail,
                    daysGranted = safeDays,
                    maxDevices = maxDevices,
                    notes = notes,
                    onSuccess = { created ->
                        isCreating = false
                        lastIssuedToken = created
                        assignedUsername = ""
                        targetEmail = ""
                        notes = ""
                        maxDevices = 1
                        copyToClipboard(context, "Token PSA", created.tokenCode)
                        Toast.makeText(
                            context,
                            "🔑 Token ${created.tokenCode} guardado y copiado al portapapeles",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onError = { err ->
                        isCreating = false
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            },
            enabled = isCloudGoogleConnected && !isCreating && parsedDays != null && assignedUsername.trim().length >= 2,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurpleNeon),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("token_create_button")
        ) {
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generando en Firebase...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCloudGoogleConnected) "Generar Token en Firebase" else "Conecta Google Admin arriba",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        lastIssuedToken?.let { issued ->
            val inviteMsg = EzoRepository.buildAccessTokenInviteMessage(
                username = issued.assignedUsername,
                tokenCode = issued.tokenCode,
                days = issued.daysGranted,
                maxDevices = issued.maxDevices,
                targetEmail = issued.targetEmail,
                apkDownloadUrl = effectiveInviteUrl
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldSuccess.copy(alpha = 0.12f))
                    .border(1.dp, EmeraldSuccess.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .testTag("token_created_result_card"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✔ Token Listo para Entregar:",
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = issued.tokenCode,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${issued.assignedUsername} • ${issued.daysGranted} días • ${issued.maxDevices} equipo(s)" +
                        if (issued.targetEmail.isNotBlank()) "\nAsignado a: ${issued.targetEmail}" else "\nUso libre (cualquier cuenta Google)",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            copyToClipboard(context, "Token Como On", issued.tokenCode)
                            Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp), tint = PsaCyanGlow)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copiar Token", color = PsaCyanGlow, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { shareTokenMessageViaWhatsApp(context, inviteMsg) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        modifier = Modifier
                            .let { if (isLargeFont) it.fillMaxWidth() else it.weight(1f) }
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enviar WhatsApp", color = Color.Black, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveLicensesExpirationSection(
    context: Context,
    viewModel: MainViewModel,
    secondaryUsers: List<UserAccount>,
    isLargeFont: Boolean,
    onEditUser: (UserAccount) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(LicenseBoardFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val nowMs = System.currentTimeMillis()
    val evaluatedUsers = remember(secondaryUsers, selectedFilter, searchQuery) {
        val cleanQ = searchQuery.trim().lowercase()
        secondaryUsers
            .map { user -> user to LicenseStatusFormatter.evaluateUserLicense(user, nowMs) }
            .filter { (user, snap) ->
                val matchesFilter = when (selectedFilter) {
                    LicenseBoardFilter.ALL -> true
                    LicenseBoardFilter.ACTIVE -> snap.state == LicenseExpirationState.ACTIVE
                    LicenseBoardFilter.EXPIRING -> snap.state == LicenseExpirationState.EXPIRING_SOON ||
                        snap.state == LicenseExpirationState.CRITICAL_24H
                    LicenseBoardFilter.EXPIRED -> snap.state == LicenseExpirationState.EXPIRED
                    LicenseBoardFilter.BLOCKED -> snap.state == LicenseExpirationState.BLOCKED
                }
                val matchesSearch = cleanQ.isBlank() ||
                    user.username.lowercase().contains(cleanQ) ||
                    user.email.lowercase().contains(cleanQ) ||
                    user.token.lowercase().contains(cleanQ)
                matchesFilter && matchesSearch
            }
            .sortedBy { (_, snap) -> snap.remainingMs }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateCard)
            .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Estado de Expiración de Licencias (${evaluatedUsers.size})",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar vendedor, correo o token...") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PsaCyanGlow) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PsaCyanGlow,
                unfocusedBorderColor = SlateCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("license_search_input")
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LicenseBoardFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label, fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurpleNeon,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (evaluatedUsers.isEmpty()) {
            Text(
                text = "No hay licencias que coincidan con este filtro.",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            evaluatedUsers.forEach { (user, snapshot) ->
                LicenseExpirationCard(
                    context = context,
                    user = user,
                    snapshot = snapshot,
                    isLargeFont = isLargeFont,
                    onAddDays = { days ->
                        viewModel.adjustUserDays(user.id, days)
                    },
                    onToggleBlock = {
                        viewModel.toggleBlockUser(user.id, !user.isBlocked)
                    },
                    onEdit = { onEditUser(user) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LicenseExpirationCard(
    context: Context,
    user: UserAccount,
    snapshot: LicenseExpirationSnapshot,
    isLargeFont: Boolean,
    onAddDays: (Int) -> Unit,
    onToggleBlock: () -> Unit,
    onEdit: () -> Unit
) {
    val accent = when (snapshot.state) {
        LicenseExpirationState.ACTIVE -> EmeraldSuccess
        LicenseExpirationState.EXPIRING_SOON,
        LicenseExpirationState.CRITICAL_24H -> AmberDelay
        LicenseExpirationState.EXPIRED,
        LicenseExpirationState.BLOCKED,
        LicenseExpirationState.REVOKED -> RoseError
        LicenseExpirationState.PENDING_ACTIVATION -> PsaCyanGlow
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateBackground)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("license_card_${user.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (user.email.isNotBlank()) {
                    Text(
                        text = user.email,
                        color = PsaCyanGlow,
                        fontSize = 11.5.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = snapshot.statusLabel,
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        LinearProgressIndicator(
            progress = { snapshot.progressFraction },
            color = accent,
            trackColor = SlateCardBorder,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = snapshot.countdownLabel,
                color = accent,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Vence: ${LicenseStatusFormatter.formatExpirationDate(user.expiresAtMs)}",
                color = TextSecondary,
                fontSize = 10.5.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔑 ${user.token}",
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Text(
                text = "📱 ${user.boundDevicesCount}/${user.maxDevices} eq. • ${user.daysGranted}d",
                color = TextMuted,
                fontSize = 10.5.sp
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(7, 15, 30).forEach { add ->
                OutlinedButton(
                    onClick = { onAddDays(add) },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("+${add}d", color = EmeraldSuccess, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = onEdit,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Editar", fontSize = 11.5.sp)
            }
            OutlinedButton(
                onClick = onToggleBlock,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (user.isBlocked) EmeraldSuccess else RoseError),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = if (user.isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                    contentDescription = null,
                    tint = if (user.isBlocked) EmeraldSuccess else RoseError,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (user.isBlocked) "Habilitar" else "Bloquear",
                    color = if (user.isBlocked) EmeraldSuccess else RoseError,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IssuedFirebaseTokensSection(
    context: Context,
    viewModel: MainViewModel,
    tokens: List<CloudAccessToken>,
    effectiveInviteUrl: String
) {
    var statusFilter by remember { mutableStateOf(TokenStatusFilter.ALL) }
    val filteredTokens = remember(tokens, statusFilter) {
        tokens.filter { tok ->
            when (statusFilter) {
                TokenStatusFilter.ALL -> true
                TokenStatusFilter.AVAILABLE -> tok.parsedStatus == CloudAccessTokenStatus.AVAILABLE
                TokenStatusFilter.REDEEMED -> tok.parsedStatus == CloudAccessTokenStatus.REDEEMED
                TokenStatusFilter.REVOKED -> tok.parsedStatus == CloudAccessTokenStatus.REVOKED
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateCard)
            .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Historial de Tokens Firebase (${filteredTokens.size})",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TokenStatusFilter.entries.forEach { f ->
                FilterChip(
                    selected = statusFilter == f,
                    onClick = { statusFilter = f },
                    label = { Text(f.label, fontSize = 11.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filteredTokens.isEmpty()) {
            Text(
                text = "Aún no hay tokens emitidos en esta categoría. Genera uno arriba para entregar acceso temporal.",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            val nowMs = System.currentTimeMillis()
            filteredTokens.forEach { token ->
                val eval = LicenseStatusFormatter.evaluateAccessToken(token, nowMs)
                val badgeColor = when (eval.state) {
                    LicenseExpirationState.PENDING_ACTIVATION -> PsaCyanGlow
                    LicenseExpirationState.ACTIVE -> EmeraldSuccess
                    LicenseExpirationState.EXPIRING_SOON,
                    LicenseExpirationState.CRITICAL_24H -> AmberDelay
                    LicenseExpirationState.EXPIRED,
                    LicenseExpirationState.BLOCKED,
                    LicenseExpirationState.REVOKED -> RoseError
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateBackground)
                        .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .testTag("firebase_token_card_${token.tokenCode}"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = token.tokenCode,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = eval.statusLabel,
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Text(
                        text = "${token.assignedUsername} • ${token.daysGranted} días • ${token.maxDevices} equipo(s)",
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (token.targetEmail.isNotBlank()) {
                        Text(
                            text = "Correo asignado: ${token.targetEmail}",
                            color = PsaCyanGlow,
                            fontSize = 11.sp
                        )
                    }

                    if (token.parsedStatus == CloudAccessTokenStatus.REDEEMED) {
                        Text(
                            text = "Canjeado por: ${token.redeemedByEmail.ifBlank { "Usuario Google" }}",
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        LinearProgressIndicator(
                            progress = { eval.progressFraction },
                            color = badgeColor,
                            trackColor = SlateCardBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(eval.countdownLabel, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Expira: ${LicenseStatusFormatter.formatExpirationDate(token.expiresAtMs)}",
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                    } else {
                        Text(
                            text = eval.countdownLabel,
                            color = badgeColor,
                            fontSize = 11.sp
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                copyToClipboard(context, "Token Como On", token.tokenCode)
                                Toast.makeText(context, "Token copiado", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = PsaCyanGlow)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copiar", color = PsaCyanGlow, fontSize = 11.sp)
                        }

                        if (token.parsedStatus == CloudAccessTokenStatus.AVAILABLE) {
                            val invite = EzoRepository.buildAccessTokenInviteMessage(
                                username = token.assignedUsername,
                                tokenCode = token.tokenCode,
                                days = token.daysGranted,
                                maxDevices = token.maxDevices,
                                targetEmail = token.targetEmail,
                                apkDownloadUrl = effectiveInviteUrl
                            )
                            Button(
                                onClick = { shareTokenMessageViaWhatsApp(context, invite) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.revokeCloudAccessToken(token.tokenCode) },
                                border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberDelay)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Revocar", color = AmberDelay, fontSize = 11.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.deleteCloudAccessToken(token.tokenCode) },
                                border = BorderStroke(1.dp, RoseError.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = RoseError)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Eliminar", color = RoseError, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun shareTokenMessageViaWhatsApp(context: Context, message: String) {
    try {
        val uri = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(message))
        val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(waIntent)
    } catch (_: Exception) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            val chooser = Intent.createChooser(sendIntent, "Compartir token").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "No se pudo abrir WhatsApp ni el selector para compartir", Toast.LENGTH_SHORT).show()
        }
    }
}

