package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
import com.example.ui.viewmodel.GoogleAuthUiState
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by viewModel.googleAuthUiState.collectAsState()
    val isRedeemingToken by viewModel.isRedeemingToken.collectAsState()
    val accessTokenFeedback by viewModel.accessTokenActionFeedback.collectAsState()
    var tokenInput by remember { mutableStateOf("") }
    val isLoading = authState is GoogleAuthUiState.Loading || isRedeemingToken

    fun openWhatsAppRequest(message: String) {
        try {
            val uri = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(message))
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(share, "Contactar Administrador"))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(138.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(
                            listOf(
                                PurpleNeon.copy(alpha = 0.6f),
                                IndigoPrimary.copy(alpha = 0.85f),
                                PurpleNeon.copy(alpha = 0.6f)
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_como_on_official),
                    contentDescription = "Como On Logo Oficial",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Plataforma Comercial y Demostración PSA",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SlateCard)
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(18.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = PsaCyanGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Acceso Seguro en la Nube",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Inicia sesión con tu cuenta de Google habilitada por el Administrador. Tu perfil, vigencia y cupo de dispositivos se validan directamente en la nube.",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )

                when (val state = authState) {
                    is GoogleAuthUiState.Loading -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(IndigoPrimary.copy(alpha = 0.14f))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PsaCyanGlow,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.message,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    is GoogleAuthUiState.NotAuthorized -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(RoseError.copy(alpha = 0.12f))
                                .border(1.dp, RoseError.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                                .testTag("auth_not_authorized_box"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = RoseError, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cuenta aún no autorizada",
                                    color = RoseError,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "El correo ${state.email} todavía no figura habilitado por el Administrador en Firestore.",
                                color = TextPrimary,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SlateBackground)
                                    .border(1.dp, IndigoPrimary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "¿Tienes un Token de Acceso?",
                                    color = PsaCyanGlow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                                OutlinedTextField(
                                    value = tokenInput,
                                    onValueChange = {
                                        tokenInput = it.uppercase()
                                        if (accessTokenFeedback != null) {
                                            viewModel.clearAccessTokenFeedback()
                                        }
                                    },
                                    label = { Text("Ingresa tu token (ej: PSA-30D-...)") },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = PsaCyanGlow)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PsaCyanGlow,
                                        unfocusedBorderColor = SlateCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_token_redeem_input")
                                )
                                accessTokenFeedback?.let { errMsg ->
                                    Text(
                                        text = errMsg,
                                        color = RoseError,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.redeemAccessToken(tokenInput) {
                                            Toast.makeText(
                                                context,
                                                "Licencia activada en Firebase",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onLoginSuccess()
                                        }
                                    },
                                    enabled = !isRedeemingToken && tokenInput.trim().length >= 6,
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("auth_token_redeem_button")
                                ) {
                                    if (isRedeemingToken) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Validando token...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    } else {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Canjear Token y Activar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    openWhatsAppRequest(
                                        "Hola Admin! Me registré con Google en Como On y necesito que habilites mi correo: ${state.email}"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Solicitar Alta por WhatsApp", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.recheckCloudAccessNow() },
                                border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.55f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ya me habilitaron • Verificar ahora", color = PsaCyanGlow, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    }

                    is GoogleAuthUiState.DeviceLimitReached -> {
                        val u = state.user
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AmberDelay.copy(alpha = 0.12f))
                                .border(1.dp, AmberDelay.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                                .testTag("auth_device_limit_box"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DevicesOther, contentDescription = null, tint = AmberDelay, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Límite de Dispositivos Alcanzado (${u.boundDevicesCount}/${u.maxDevices})",
                                    color = AmberDelay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                            Text(
                                text = "Tu cuenta (${u.email}) ya alcanzó el máximo de ${u.maxDevices} dispositivo(s) vinculado(s): ${u.boundDeviceLabels.joinToString(", ").ifBlank { "Equipo registrado" }}.",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                            Text(
                                text = "Equipo actual: ${state.currentDeviceLabel}",
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                            Button(
                                onClick = {
                                    openWhatsAppRequest(
                                        "Hola Admin! Alcanzé mi límite de dispositivos (${u.boundDevicesCount}/${u.maxDevices}) con ${u.email} desde ${state.currentDeviceLabel}. ¿Podrías habilitar un equipo adicional o desvincular el anterior?"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberDelay),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pedir Cupo Extra al Admin", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.recheckCloudAccessNow() },
                                border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.55f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reintentar Verificación", color = PsaCyanGlow, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    }

                    is GoogleAuthUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(RoseError.copy(alpha = 0.14f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = state.message,
                                color = RoseError,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    GoogleAuthUiState.Idle -> Unit
                }

                Button(
                    onClick = {
                        viewModel.signInWithGoogle(context) {
                            Toast.makeText(context, "Sesión verificada en la nube", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("google_signin_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (authState is GoogleAuthUiState.Idle) "Continuar con Google" else "Elegir otra cuenta Google",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (authState !is GoogleAuthUiState.Idle && !isLoading) {
                    OutlinedButton(
                        onClick = { viewModel.logout() },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SlateCardBorder),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text("Cerrar Sesión Actual", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Seguridad activa: validación en Firebase Firestore + vinculación por dispositivo.",
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
