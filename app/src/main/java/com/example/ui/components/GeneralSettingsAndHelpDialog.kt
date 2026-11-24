package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.FontScaleLevel
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.LocalIsLargeFontMode
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.PsaCountryConfig
import com.example.util.PsaCountryRegistry

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneralSettingsAndHelpDialog(
    context: Context,
    currentFontLevel: FontScaleLevel,
    activeCountry: PsaCountryConfig,
    hasDismissedSetupGuide: Boolean,
    isAppDistributionSignedIn: Boolean = false,
    onSelectFontLevel: (FontScaleLevel) -> Unit,
    onSelectCountry: (String) -> Unit,
    onShowSetupGuideAgain: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onCheckAppDistributionUpdates: () -> Unit = {},
    onOpenPsaPricing: () -> Unit = {},
    onOpenPsaDipPortal: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var activeTabIndex by remember { mutableIntStateOf(0) }
    var pendingFontLevel by remember(currentFontLevel) { mutableStateOf(currentFontLevel) }
    val isLargeFont = LocalIsLargeFontMode.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        shape = RoundedCornerShape(18.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (activeTabIndex == 0) Icons.Default.Settings else Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = PurpleNeon,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Configuración y Ayuda",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 17.sp
                    )
                }

                TabRow(
                    selectedTabIndex = activeTabIndex,
                    containerColor = SlateBackground,
                    contentColor = PurpleNeon,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTabIndex]),
                            color = PurpleNeon,
                            height = 2.5.dp
                        )
                    }
                ) {
                    Tab(
                        selected = activeTabIndex == 0,
                        onClick = { activeTabIndex = 0 },
                        text = {
                            Text(
                                text = "⚙️ Ajustes",
                                fontSize = 12.sp,
                                fontWeight = if (activeTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTabIndex == 0) PurpleNeon else TextSecondary
                            )
                        },
                        modifier = Modifier.testTag("settings_tab_general")
                    )
                    Tab(
                        selected = activeTabIndex == 1,
                        onClick = { activeTabIndex = 1 },
                        text = {
                            Text(
                                text = "❓ Ayuda",
                                fontSize = 12.sp,
                                fontWeight = if (activeTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTabIndex == 1) PsaCyanGlow else TextSecondary
                            )
                        },
                        modifier = Modifier.testTag("settings_tab_help")
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeTabIndex == 0) {
                    // --- GENERAL SETTINGS TAB ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = null,
                            tint = PurpleNeon,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tamaño de Letra y Diseño",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    FontScaleLevel.entries.forEach { level ->
                        val isSelected = level == pendingFontLevel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PurpleNeon.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.35f))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) PurpleNeon else SlateCardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    pendingFontLevel = level
                                    onSelectFontLevel(level)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("font_option_${level.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = level.icon, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = level.label,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = level.description,
                                        color = if (isSelected) TextSecondary else TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) PurpleNeon else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Active Country Selector inside General Settings
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = PsaCyanGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "País Oficial PSA (${activeCountry.shortBadge})",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PsaCountryRegistry.countries.forEach { country ->
                            val selected = country.code == activeCountry.code
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) PsaCyanPrimary.copy(alpha = 0.2f) else SlateBackground)
                                    .border(
                                        1.dp,
                                        if (selected) PsaCyanGlow else SlateCardBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectCountry(country.code) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("settings_country_${country.code}")
                            ) {
                                Text(
                                    text = "${country.flag} ${country.name}",
                                    color = if (selected) PsaCyanGlow else TextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Comercial PSA: Precios y Portal DIP Oficial
                    Spacer(modifier = Modifier.height(2.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateBackground),
                        border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Comercial PSA",
                                color = PsaCyanGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )

                            if (isLargeFont) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            onDismiss()
                                            onOpenPsaPricing()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("settings_btn_psa_pricing")
                                    ) {
                                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp), tint = PsaCyanGlow)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Precios y Cuotas PSA", fontSize = 12.sp, color = PsaCyanGlow, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onDismiss()
                                            onOpenPsaDipPortal()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("settings_btn_psa_dip_portal")
                                    ) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldSuccess)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Portal DIP Oficial", fontSize = 12.sp, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            } else {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onDismiss()
                                            onOpenPsaPricing()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, PsaCyanGlow.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("settings_btn_psa_pricing")
                                    ) {
                                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp), tint = PsaCyanGlow)
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Precios y Cuotas", fontSize = 12.sp, color = PsaCyanGlow, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onDismiss()
                                            onOpenPsaDipPortal()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("settings_btn_psa_dip_portal")
                                    ) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldSuccess)
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Portal DIP Oficial", fontSize = 12.sp, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    // Initial Guide & System Shortcuts
                    Spacer(modifier = Modifier.height(2.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateBackground),
                        border = BorderStroke(1.dp, SlateCardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Accesos de Sistema",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )

                            if (isLargeFont) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = onOpenAccessibilitySettings,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Permisos del Teléfono", fontSize = 12.sp, color = TextPrimary)
                                    }
                                    if (hasDismissedSetupGuide) {
                                        OutlinedButton(
                                            onClick = {
                                                onShowSetupGuideAgain()
                                                onDismiss()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = PsaCyanGlow)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Mostrar Guía Inicial", fontSize = 12.sp, color = PsaCyanGlow)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onCheckAppDistributionUpdates()
                                            onDismiss()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("settings_btn_app_distribution_updates"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isAppDistributionSignedIn) {
                                                "🔔 Buscar Actualización (Firebase)"
                                            } else {
                                                "🔔 Activar Avisos Automáticos (Firebase)"
                                            },
                                            fontSize = 12.sp,
                                            color = EmeraldSuccess,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onOpenAccessibilitySettings,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Permisos Android", fontSize = 11.5.sp, color = TextPrimary)
                                    }
                                    if (hasDismissedSetupGuide) {
                                        OutlinedButton(
                                            onClick = {
                                                onShowSetupGuideAgain()
                                                onDismiss()
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp), tint = PsaCyanGlow)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ver Guía Inicial", fontSize = 11.5.sp, color = PsaCyanGlow)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onCheckAppDistributionUpdates()
                                            onDismiss()
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("settings_btn_app_distribution_updates")
                                    ) {
                                        Text(
                                            text = if (isAppDistributionSignedIn) {
                                                "🔔 Buscar Actualización"
                                            } else {
                                                "🔔 Avisos Firebase"
                                            },
                                            fontSize = 11.5.sp,
                                            color = EmeraldSuccess,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // --- HELP & GUIDES ACCORDION TAB ---
                    Text(
                        text = "Guías rápidas de cada módulo:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    HelpAccordionItem(
                        title = "🚀 Primeros Pasos y Permisos",
                        summary = "Accesibilidad, notificaciones y burbuja flotante",
                        body = "Para automatizar envíos y responder rápido necesitas activar:\n" +
                            "1. Servicio de Accesibilidad (Ajustes > Apps instaladas > Como On).\n" +
                            "2. Notificaciones del sistema para avisos de fin de campaña.\n" +
                            "3. Mostrar sobre otras apps para usar la burbuja flotante en WhatsApp o CRM."
                    )

                    HelpAccordionItem(
                        title = "🛡️ Modo Banco Seguro / Kill Switch",
                        summary = "Qué hacer antes de abrir Banco Nación (BNA+) u otros bancos",
                        body = "Las apps bancarias (Banco Nación BNA+, Galicia, Mercado Pago, etc.) detectan servicios de accesibilidad por seguridad y muestran 'app trabajando en remoto'.\n\n" +
                            "No necesitas desinstalar Como On: toca 'Pausar Servicio' desde el Panel o Automatización antes de abrir tu banco. Al terminar, toca 'Reactivar'."
                    )

                    HelpAccordionItem(
                        title = "💧 Precios Reales PSA, 9 Países y DIP",
                        summary = "Sincronización en vivo, monedas locales y auto-login mi.psa.com.ar",
                        body = "• El selector de país cambia moneda, sitio oficial y lista de precios independiente para los 9 países PSA (AR, UY, CL, BO, PY, PE, CO, EC, ES).\n" +
                            "• En Argentina y Uruguay la app sincroniza automáticamente desde la Tienda Oficial PSA.\n" +
                            "• En el diálogo DIP guarda tu Centro, últimos 8 dígitos y Clave PSA para autocompletar el ingreso oficial en mi.psa.com.ar/login o pegar listas actualizadas."
                    )

                    HelpAccordionItem(
                        title = "📞 Contactos, Metas y Regla de 2 Llamadas",
                        summary = "Registro diario de gestiones, aciertos y selección múltiple",
                        body = "• Registra cada gestión como Llamada o Mensaje y marca el resultado (Acierto/Cita, No respondió, Reprogramar).\n" +
                            "• Cuando un contacto acumula 2 llamadas sin respuesta, la app sugiere pasar a WhatsApp en el siguiente contacto.\n" +
                            "• Usa el botón 'Varios' o mantén presionada una tarjeta para seleccionar múltiples contactos y cambiarles estado, reprogramar o eliminar en lote."
                    )

                    HelpAccordionItem(
                        title = "💬 Automatización, Scripts y Memoria IA",
                        summary = "Modo Libre vs Campañas y aprendizaje con estrellas ⭐",
                        body = "• Modo Libre muestra la burbuja flotante para que elijas manualmente en qué chat escanear o responder.\n" +
                            "• En Memoria IA de PSA, califica con ⭐ Ganadora las respuestas que mejor funcionan para priorizarlas en futuros cierres."
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Listo", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )
}

@Composable
private fun HelpAccordionItem(
    title: String,
    summary: String,
    body: String
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateBackground),
        border = BorderStroke(1.dp, if (expanded) PsaCyanGlow.copy(alpha = 0.5f) else SlateCardBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = if (expanded) PsaCyanGlow else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (!expanded) {
                        Text(
                            text = summary,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Ocultar" else "Ver explicación",
                    tint = if (expanded) PsaCyanGlow else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = body,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
