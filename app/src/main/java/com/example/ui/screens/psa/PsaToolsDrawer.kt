package com.example.ui.screens.psa

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppTab
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.PsaCountryConfig

@Composable
fun PsaToolsDrawerSheet(
    activeTab: PsaWorkspaceTab,
    activeCountry: PsaCountryConfig,
    onSelectTab: (PsaWorkspaceTab) -> Unit,
    onOpenAdvisorQuiz: () -> Unit,
    onOpenHelpGuide: () -> Unit,
    onOpenWelcomeTutorial: () -> Unit = {},
    onOpenPricingSettings: () -> Unit = {},
    onOpenDipPortal: () -> Unit = {},
    showDashboardNav: Boolean = false,
    showSuperuserNav: Boolean = false,
    showScriptsNav: Boolean = false,
    showAutomationNav: Boolean = false,
    onNavigateAppTab: (AppTab) -> Unit = {},
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF0A1322),
        drawerContentColor = TextPrimary,
        windowInsets = DrawerDefaults.windowInsets,
        modifier = Modifier
            .widthIn(min = 260.dp, max = 310.dp)
            .testTag("psa_tools_drawer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Brand header inside lateral drawer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SlateCard)
                    .border(1.dp, PsaCyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PsaCyanPrimary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Herramientas PSA",
                        tint = PsaCyanGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Herramientas PSA",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                    Text(
                        text = "${activeCountry.flag} ${activeCountry.name} • ${activeCountry.currencyCode}",
                        color = PsaCyanGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(
                    onClick = onCloseDrawer,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("psa_drawer_btn_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar menú lateral",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            DrawerSectionLabel(title = "PRINCIPAL")

            PsaDrawerItemRow(
                label = "Demostración",
                subtitle = "Vista principal • Cloro y sarro",
                icon = Icons.Default.Science,
                selected = activeTab == PsaWorkspaceTab.LIVE_DEMO,
                accentColor = PsaCyanGlow,
                testTag = "psa_drawer_item_demo",
                onClick = { onSelectTab(PsaWorkspaceTab.LIVE_DEMO) }
            )

            PsaDrawerItemRow(
                label = "Agenda",
                subtitle = "Turnos libres, búsqueda y visitas",
                icon = Icons.Default.CalendarMonth,
                selected = activeTab == PsaWorkspaceTab.AGENDA,
                accentColor = PsaCyanGlow,
                testTag = "psa_drawer_item_agenda",
                onClick = { onSelectTab(PsaWorkspaceTab.AGENDA) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = SlateCardBorder.copy(alpha = 0.65f)
            )

            DrawerSectionLabel(title = "GESTIÓN COMERCIAL")

            PsaDrawerItemRow(
                label = "Cotizar",
                subtitle = "Presupuestos y cuotas",
                icon = Icons.Default.AttachMoney,
                selected = activeTab == PsaWorkspaceTab.BUDGET,
                accentColor = EmeraldSuccess,
                testTag = "psa_drawer_item_budget",
                onClick = { onSelectTab(PsaWorkspaceTab.BUDGET) }
            )

            PsaDrawerItemRow(
                label = "Contactos",
                subtitle = "Seguimiento de clientes",
                icon = Icons.Default.Contacts,
                selected = activeTab == PsaWorkspaceTab.CONTACTS,
                accentColor = IndigoPrimary,
                testTag = "psa_drawer_item_contacts",
                onClick = { onSelectTab(PsaWorkspaceTab.CONTACTS) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = SlateCardBorder.copy(alpha = 0.65f)
            )

            DrawerSectionLabel(title = "GUÍA & ASISTENCIA IA")

            PsaDrawerItemRow(
                label = "Guiones PSA",
                subtitle = "Mensajes y horarios listos",
                icon = Icons.AutoMirrored.Filled.Chat,
                selected = activeTab == PsaWorkspaceTab.SCRIPTS,
                accentColor = IndigoPrimary,
                testTag = "psa_drawer_item_scripts",
                onClick = { onSelectTab(PsaWorkspaceTab.SCRIPTS) }
            )

            PsaDrawerItemRow(
                label = "Asesor Guiado",
                subtitle = "¿Cuál es tu PSA ideal?",
                icon = Icons.Default.Explore,
                selected = false,
                accentColor = PsaCyanGlow,
                testTag = "psa_drawer_item_advisor_quiz",
                onClick = onOpenAdvisorQuiz
            )

            PsaDrawerItemRow(
                label = "IA & Memoria",
                subtitle = "Respuestas y conocimiento",
                icon = Icons.Default.Psychology,
                selected = activeTab == PsaWorkspaceTab.AI_MEMORY,
                accentColor = PurpleNeon,
                testTag = "psa_drawer_item_ai_memory",
                onClick = { onSelectTab(PsaWorkspaceTab.AI_MEMORY) }
            )

            PsaDrawerItemRow(
                label = "Guía Inicial y Ayuda",
                subtitle = "Letra, país y soporte",
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                selected = false,
                accentColor = TextSecondary,
                testTag = "psa_drawer_item_help_guide",
                onClick = onOpenHelpGuide
            )

            PsaDrawerItemRow(
                label = "Tutorial de Bienvenida",
                subtitle = "Demo de agua y Cotizador",
                icon = Icons.Default.PlayCircleOutline,
                selected = false,
                accentColor = PsaCyanGlow,
                testTag = "psa_drawer_item_welcome_tutorial",
                onClick = onOpenWelcomeTutorial
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = SlateCardBorder.copy(alpha = 0.65f)
            )

            DrawerSectionLabel(title = "CONFIGURACIÓN PSA")

            PsaDrawerItemRow(
                label = "Precios y Cuotas",
                subtitle = "Lista oficial, planes y horarios",
                icon = Icons.Default.Settings,
                selected = false,
                accentColor = PsaCyanGlow,
                testTag = "psa_drawer_item_pricing",
                onClick = onOpenPricingSettings
            )

            PsaDrawerItemRow(
                label = "Portal DIP Oficial",
                subtitle = "Web mi.psa.com.ar y PVP",
                icon = Icons.Default.CloudSync,
                selected = false,
                accentColor = EmeraldSuccess,
                testTag = "psa_drawer_item_dip_portal",
                onClick = onOpenDipPortal
            )

            if (showDashboardNav || showSuperuserNav || showScriptsNav || showAutomationNav) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = SlateCardBorder.copy(alpha = 0.65f)
                )

                DrawerSectionLabel(title = "NAVEGACIÓN Y EQUIPO")

                if (showDashboardNav) {
                    PsaDrawerItemRow(
                        label = "Panel y Estadísticas",
                        subtitle = "Métricas y equipo comercial",
                        icon = Icons.Default.Assessment,
                        selected = false,
                        accentColor = IndigoPrimary,
                        testTag = "psa_drawer_item_nav_dashboard",
                        onClick = { onNavigateAppTab(AppTab.DASHBOARD) }
                    )
                }

                if (showSuperuserNav) {
                    PsaDrawerItemRow(
                        label = "Administración",
                        subtitle = "Licencias, usuarios y jerarquía",
                        icon = Icons.Default.AdminPanelSettings,
                        selected = false,
                        accentColor = PurpleNeon,
                        testTag = "psa_drawer_item_nav_superuser",
                        onClick = { onNavigateAppTab(AppTab.SUPERUSER) }
                    )
                }

                if (showScriptsNav) {
                    PsaDrawerItemRow(
                        label = "Scripts",
                        subtitle = "Plantillas de contacto",
                        icon = Icons.Default.Description,
                        selected = false,
                        accentColor = IndigoPrimary,
                        testTag = "psa_drawer_item_nav_scripts",
                        onClick = { onNavigateAppTab(AppTab.SCRIPTS) }
                    )
                }

                if (showAutomationNav) {
                    PsaDrawerItemRow(
                        label = "Envíos",
                        subtitle = "Automatización de campañas",
                        icon = Icons.Default.Bolt,
                        selected = false,
                        accentColor = EmeraldSuccess,
                        testTag = "psa_drawer_item_nav_automation",
                        onClick = { onNavigateAppTab(AppTab.AUTOMATION) }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = SlateCardBorder.copy(alpha = 0.65f)
            )

            var moreToolsExpanded by remember(activeTab) {
                mutableStateOf(activeTab == PsaWorkspaceTab.STOCK)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { moreToolsExpanded = !moreToolsExpanded }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("psa_drawer_more_tools_toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "MÁS HERRAMIENTAS",
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Icon(
                    imageVector = if (moreToolsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (moreToolsExpanded) "Ocultar más herramientas" else "Mostrar más herramientas",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            AnimatedVisibility(visible = moreToolsExpanded) {
                PsaDrawerItemRow(
                    label = "Stock",
                    subtitle = "Equipos y repuestos",
                    icon = Icons.Default.Inventory2,
                    selected = activeTab == PsaWorkspaceTab.STOCK,
                    accentColor = PsaCyanGlow,
                    testTag = "psa_drawer_item_stock",
                    onClick = { onSelectTab(PsaWorkspaceTab.STOCK) }
                )
            }
        }
    }
}

@Composable
private fun DrawerSectionLabel(title: String) {
    Text(
        text = title,
        color = TextMuted,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun PsaDrawerItemRow(
    label: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) accentColor else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        },
        label = {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = label,
                    color = if (selected) TextPrimary else TextSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = accentColor.copy(alpha = 0.16f),
            unselectedContainerColor = Color.Transparent,
            selectedIconColor = accentColor,
            unselectedIconColor = TextSecondary,
            selectedTextColor = TextPrimary,
            unselectedTextColor = TextSecondary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(testTag)
    )
}

@Composable
fun PsaSecondaryToolContextBar(
    activeTab: PsaWorkspaceTab,
    lastPrimaryTab: PsaWorkspaceTab,
    onBackToPrimary: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0C1524),
        border = BorderStroke(1.dp, SlateCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("psa_secondary_context_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onBackToPrimary,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SlateCardBorder),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("psa_btn_back_from_tool")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver a vista principal",
                    tint = PsaCyanGlow,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (lastPrimaryTab == PsaWorkspaceTab.LIVE_DEMO) "Demostración" else lastPrimaryTab.label,
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlateCard)
                    .border(1.dp, PsaCyanPrimary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = activeTab.label,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("psa_btn_switch_tool_drawer")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Cambiar herramienta",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
