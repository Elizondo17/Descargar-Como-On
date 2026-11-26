package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.data.model.FlattenedTreeRow
import com.example.data.model.HierarchyRequest
import com.example.data.model.HierarchyRequestStatus
import com.example.data.model.HierarchyRequestType
import com.example.data.model.HierarchyTreeBuilder
import com.example.data.model.HierarchyTreeNode
import com.example.data.model.SellerPerformanceStats
import com.example.data.model.UserAccount
import com.example.ui.components.formatRelativeActivityTime
import com.example.ui.theme.AmberDelay
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PsaCyanGlow
import com.example.ui.theme.PsaCyanPrimary
import com.example.ui.theme.PurpleNeon
import com.example.ui.theme.RoseError
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

private enum class TreeStatusFilter(val label: String) {
    ALL("Todos"),
    ACTIVE("Activos"),
    PENDING("Pendientes"),
    BLOCKED("Bloqueados"),
    MANAGERS("Pueden Sub-Gestionar")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HierarchyManagementScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    initialInspectSellerEmail: String? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val visibleUsers by viewModel.visibleHierarchyUsers.collectAsState()
    val visibleStats by viewModel.visibleSubordinateStats.collectAsState()
    val hierarchyRequests by viewModel.hierarchyRequests.collectAsState()
    val actionFeedback by viewModel.hierarchyActionFeedback.collectAsState()
    val isBusy by viewModel.isHierarchyActionBusy.collectAsState()

    val viewer = currentUser ?: return
    val isSuperuser = viewer.role == "SUPERUSER"

    val sourceUsersForTree = remember(isSuperuser, allUsers, visibleUsers) {
        if (isSuperuser) allUsers else visibleUsers
    }
    val statsByEmail = remember(visibleStats) {
        visibleStats.associateBy { it.sellerEmail.trim().lowercase() }
    }

    val context = LocalContext.current
    val dayKeyFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dayDisplayFmt = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    val last30DayOptions = remember {
        val list = mutableListOf<Pair<String, String>>()
        val cal = Calendar.getInstance()
        for (offset in 0 until 30) {
            val d = cal.time
            val key = dayKeyFmt.format(d)
            val label = when (offset) {
                0 -> "Hoy"
                1 -> "Ayer"
                else -> dayDisplayFmt.format(d)
            }
            list.add(key to label)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        list
    }
    var selectedDateKey by remember { mutableStateOf(last30DayOptions.first().first) }
    val selectedDateLabel = remember(selectedDateKey, last30DayOptions) {
        last30DayOptions.firstOrNull { it.first == selectedDateKey }?.second ?: selectedDateKey
    }

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(TreeStatusFilter.ALL) }
    var expandedEmails by remember { mutableStateOf(setOf<String>()) }
    var hasAutoExpandedOnce by remember { mutableStateOf(false) }

    var showProposeDialog by remember { mutableStateOf(false) }
    var approveDialogTarget by remember { mutableStateOf<HierarchyRequest?>(null) }
    var permissionsDialogTarget by remember { mutableStateOf<UserAccount?>(null) }
    var reparentDialogTarget by remember { mutableStateOf<UserAccount?>(null) }
    var coachingDialogTarget by remember { mutableStateOf<Pair<UserAccount?, SellerPerformanceStats?>?>(null) }

    val forest = remember(viewer, sourceUsersForTree, statsByEmail, isSuperuser) {
        HierarchyTreeBuilder.buildHierarchyForest(
            viewer = viewer,
            allUsers = sourceUsersForTree,
            statsByEmail = statsByEmail,
            enforceDepthLimit = !isSuperuser
        )
    }

    // Auto-expand root levels once when tree first loads
    LaunchedEffect(forest) {
        if (!hasAutoExpandedOnce && forest.isNotEmpty()) {
            expandedEmails = forest.map { it.user.normalizedEmail }.toSet()
            hasAutoExpandedOnce = true
        }
    }

    // Support opening coaching directly if navigated from analytics bar tap
    LaunchedEffect(initialInspectSellerEmail, sourceUsersForTree, visibleStats) {
        val target = initialInspectSellerEmail?.trim()?.lowercase().orEmpty()
        if (target.isNotBlank()) {
            val matchedUser = sourceUsersForTree.firstOrNull { it.normalizedEmail == target }
            val matchedStat = statsByEmail[target]
            if (matchedUser != null || matchedStat != null) {
                coachingDialogTarget = matchedUser to matchedStat
            }
        }
    }

    val allFlattenedRows = remember(forest, expandedEmails, searchQuery) {
        HierarchyTreeBuilder.flattenForDisplay(
            roots = forest,
            expandedEmails = expandedEmails,
            searchQuery = searchQuery
        )
    }

    val filteredRows = remember(allFlattenedRows, statusFilter) {
        when (statusFilter) {
            TreeStatusFilter.ALL -> allFlattenedRows
            TreeStatusFilter.ACTIVE -> allFlattenedRows.filter { it.node.user.isActive }
            TreeStatusFilter.PENDING -> allFlattenedRows.filter { it.node.user.isPendingApproval }
            TreeStatusFilter.BLOCKED -> allFlattenedRows.filter { it.node.user.isBlocked || it.node.user.isExpired }
            TreeStatusFilter.MANAGERS -> allFlattenedRows.filter { it.node.user.canManageSubUsers }
        }
    }

    val pendingRequests = remember(hierarchyRequests) {
        hierarchyRequests.filter { it.parsedStatus == HierarchyRequestStatus.PENDING }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .testTag("hierarchy_management_screen")
    ) {
        // Top Header Bar
        Surface(
            color = SlateCard,
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, SlateCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("hierarchy_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver al Panel",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isSuperuser) "Gestión de Equipo" else "Mi Equipo y Coaching",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val depthSummary = when {
                        isSuperuser -> "${allUsers.count { it.role != "SUPERUSER" }} cuenta(s) en equipo"
                        viewer.visibilityDepth <= 0 -> "${visibleUsers.size} sub-usuario(s) • Toda tu rama"
                        else -> "${visibleUsers.size} sub-usuario(s) • Hasta ${viewer.visibilityDepth} nivel(es)"
                    }
                    Text(
                        text = depthSummary,
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }

                if (forest.isNotEmpty()) {
                    val allExpanded = remember(forest, expandedEmails) {
                        val allParents = collectAllParentEmails(forest)
                        allParents.isNotEmpty() && allParents.all { it in expandedEmails }
                    }
                    IconButton(
                        onClick = {
                            expandedEmails = if (allExpanded) {
                                emptySet()
                            } else {
                                collectAllParentEmails(forest)
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("hierarchy_toggle_expand_all")
                    ) {
                        Icon(
                            imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                            contentDescription = if (allExpanded) "Contraer árbol" else "Expandir árbol",
                            tint = PsaCyanPrimary
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Feedback banner
            if (!actionFeedback.isNullOrBlank()) {
                item {
                    Surface(
                        color = IndigoPrimary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hierarchy_feedback_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = actionFeedback ?: "",
                                color = TextPrimary,
                                fontSize = 12.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearHierarchyActionFeedback() },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar aviso",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Permission & Propose Sub-user CTA
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateCard)
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PsaCyanPrimary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = null,
                                tint = PsaCyanGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSuperuser) {
                                    "Control Total del Equipo"
                                } else if (viewer.canManageSubUsers) {
                                    "Gestión de Sub-Usuarios Habilitada"
                                } else {
                                    "Visualización de Sub-Usuarios"
                                },
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isSuperuser) {
                                    "Solo el Superusuario activa cuentas nuevas, asigna si pueden crear sub-usuarios y mueve vendedores entre líderes."
                                } else if (viewer.canManageSubUsers) {
                                    "Puedes proponer nuevos sub-usuarios o pedir vincular vendedores existentes. Quedarán pendientes hasta activación del Superusuario."
                                } else {
                                    "Consulta llamadas y mensajería de tu equipo para dar orientación comercial."
                                },
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    if (viewer.canManageSubUsers && !isSuperuser) {
                        Button(
                            onClick = { showProposeDialog = true },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("propose_sub_user_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Proponer Sub-Usuario o Vincular Existente",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Pending Requests Section (Superuser Approval Queue OR Manager's Request Tracker)
            if (isSuperuser && pendingRequests.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AmberDelay.copy(alpha = 0.08f))
                            .border(1.dp, AmberDelay.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⏳ Solicitudes Pendientes de Activación (${pendingRequests.size})",
                                color = AmberDelay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }

                        pendingRequests.forEach { req ->
                            PendingRequestApprovalCard(
                                request = req,
                                isBusy = isBusy,
                                onApprove = { approveDialogTarget = req },
                                onReject = { viewModel.rejectHierarchyRequest(req.id) }
                            )
                        }
                    }
                }
            } else if (!isSuperuser && hierarchyRequests.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SlateCard)
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Mis Solicitudes de Sub-Usuarios (${hierarchyRequests.size})",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        hierarchyRequests.take(5).forEach { req ->
                            ManagerRequestStatusRow(req)
                        }
                    }
                }
            }

            // Search + Status Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("Buscar vendedor por nombre o correo...", color = TextMuted, fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = PsaCyanPrimary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Limpiar búsqueda", tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsaCyanPrimary,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hierarchy_search_input")
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TreeStatusFilter.entries.forEach { filter ->
                            if (!isSuperuser && filter == TreeStatusFilter.PENDING && filteredRows.none { it.node.user.isPendingApproval }) {
                                // Still show chip for clarity, optional
                            }
                            FilterChip(
                                selected = statusFilter == filter,
                                onClick = { statusFilter = filter },
                                label = { Text(filter.label, fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }

            // 30-day Day Selector + Team Daily Totals Card
            item {
                val teamDayStatsList = remember(filteredRows) {
                    filteredRows.mapNotNull { it.node.stats }
                        .distinctBy { it.sellerEmail.trim().lowercase() }
                }
                val dayMsgsOk = remember(teamDayStatsList, selectedDateKey) {
                    teamDayStatsList.sumOf { HierarchyTreeBuilder.findBucketForDate(it, selectedDateKey).messagesSuccess }
                }
                val dayMsgsFail = remember(teamDayStatsList, selectedDateKey) {
                    teamDayStatsList.sumOf { HierarchyTreeBuilder.findBucketForDate(it, selectedDateKey).messagesFailed }
                }
                val dayCalls = remember(teamDayStatsList, selectedDateKey) {
                    teamDayStatsList.sumOf { HierarchyTreeBuilder.findBucketForDate(it, selectedDateKey).callsCount }
                }
                val dayAppointments = remember(teamDayStatsList, selectedDateKey) {
                    teamDayStatsList.sumOf { HierarchyTreeBuilder.findBucketForDate(it, selectedDateKey).appointmentsCount }
                }
                val dayDemos = remember(teamDayStatsList, selectedDateKey) {
                    teamDayStatsList.sumOf { HierarchyTreeBuilder.findBucketForDate(it, selectedDateKey).demosCount }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SlateCard)
                        .border(1.dp, PsaCyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                        .testTag("team_daily_summary_card"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 Gestiones y Citas • $selectedDateLabel",
                            color = PsaCyanGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                runCatching {
                                    dayKeyFmt.parse(selectedDateKey)?.let { cal.time = it }
                                }
                                val dialog = android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val picked = Calendar.getInstance().apply {
                                            set(year, month, dayOfMonth, 12, 0, 0)
                                        }
                                        selectedDateKey = dayKeyFmt.format(picked.time)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                )
                                val nowMs = System.currentTimeMillis()
                                dialog.datePicker.maxDate = nowMs
                                dialog.datePicker.minDate = nowMs - (29L * 24L * 60L * 60L * 1000L)
                                dialog.show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .testTag("team_pick_date_button")
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Elegir fecha", color = PsaCyanGlow, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        last30DayOptions.take(7).forEach { (key, label) ->
                            FilterChip(
                                selected = selectedDateKey == key,
                                onClick = { selectedDateKey = key },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PsaCyanPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = SlateBackground,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Surface(
                        color = SlateBackground,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SlateCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("✉️ $dayMsgsOk ok / $dayMsgsFail fail", color = TextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            Text("📞 $dayCalls llam.", color = PsaCyanGlow, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            Text("📅 $dayAppointments citas", color = EmeraldSuccess, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text("🧪 $dayDemos demos", color = AmberDelay, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (isBusy) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = PsaCyanPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Actualizando estructura en nube...", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            // Tree Nodes List
            if (filteredRows.isEmpty()) {
                item {
                    Surface(
                        color = SlateCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SlateCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank() || statusFilter != TreeStatusFilter.ALL) {
                                    "Ningún vendedor coincide con el filtro actual."
                                } else if (isSuperuser) {
                                    "Aún no hay operadores autorizados en el árbol."
                                } else {
                                    "Todavía no tienes sub-usuarios asignados en tu rama."
                                },
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredRows,
                    key = { it.node.user.normalizedEmail.ifBlank { it.node.user.id } }
                ) { row ->
                    HierarchyTreeRowCard(
                        row = row,
                        selectedDateKey = selectedDateKey,
                        selectedDateLabel = selectedDateLabel,
                        isSuperuserViewer = isSuperuser,
                        isBusy = isBusy,
                        onToggleExpand = {
                            val key = row.node.user.normalizedEmail
                            expandedEmails = if (key in expandedEmails) {
                                expandedEmails - key
                            } else {
                                expandedEmails + key
                            }
                        },
                        onInspectCoaching = {
                            coachingDialogTarget = row.node.user to row.node.stats
                        },
                        onEditPermissions = {
                            permissionsDialogTarget = row.node.user
                        },
                        onReassignParent = {
                            reparentDialogTarget = row.node.user
                        },
                        onToggleActivation = { enable ->
                            viewModel.toggleSellerActivationFromTree(row.node.user, enable)
                        }
                    )
                }
            }
        }
    }

    // Propose Sub-User / Link Existing User Dialog (for Authorized Managers)
    if (showProposeDialog) {
        ProposeSubUserDialog(
            onDismiss = { showProposeDialog = false },
            onSubmitNew = { email, name, days, devices, notes ->
                showProposeDialog = false
                viewModel.submitNewSubUserRequest(email, name, days, devices, notes)
            },
            onSubmitLinkExisting = { email, name, notes ->
                showProposeDialog = false
                viewModel.submitLinkExistingUserRequest(email, name, notes)
            }
        )
    }

    // Superuser Approve Request & Configure Permissions Dialog
    val reqToApprove = approveDialogTarget
    if (reqToApprove != null) {
        ApproveHierarchyRequestDialog(
            request = reqToApprove,
            onDismiss = { approveDialogTarget = null },
            onConfirmApprove = { days, allowSub, depth ->
                approveDialogTarget = null
                viewModel.approveHierarchyRequest(
                    request = reqToApprove,
                    overrideDays = days,
                    allowSubManagement = allowSub,
                    visibilityDepth = depth
                )
            }
        )
    }

    // Superuser Edit Permissions Dialog (canManageSubUsers + visibilityDepth)
    val userToEditPerms = permissionsDialogTarget
    if (userToEditPerms != null) {
        EditHierarchyPermissionsDialog(
            user = userToEditPerms,
            onDismiss = { permissionsDialogTarget = null },
            onSave = { canManage, depth ->
                permissionsDialogTarget = null
                viewModel.updateUserHierarchyPermissions(
                    email = userToEditPerms.normalizedEmail,
                    canManageSubUsers = canManage,
                    visibilityDepth = depth
                )
            }
        )
    }

    // Superuser Re-Parent / Link Existing Seller Under Superior Dialog
    val userToReparent = reparentDialogTarget
    if (userToReparent != null) {
        ReassignParentDialog(
            movingUser = userToReparent,
            allUsers = allUsers,
            onDismiss = { reparentDialogTarget = null },
            onSelectNewParent = { newParentEmail ->
                reparentDialogTarget = null
                viewModel.reassignUserParent(
                    targetEmail = userToReparent.normalizedEmail,
                    newParentEmail = newParentEmail
                )
            }
        )
    }

    // Coaching & Detailed Metrics Modal (calls + messaging + 30-day breakdown + coaching message)
    val coachingTarget = coachingDialogTarget
    if (coachingTarget != null) {
        SellerCoachingDetailDialog(
            user = coachingTarget.first,
            stats = coachingTarget.second,
            selectedDateKey = selectedDateKey,
            selectedDateLabel = selectedDateLabel,
            onSendCoachingMessage = { email, title, body ->
                viewModel.sendLeaderCoachingMessage(targetEmail = email, title = title, body = body)
            },
            onDismiss = { coachingDialogTarget = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HierarchyTreeRowCard(
    row: FlattenedTreeRow,
    selectedDateKey: String,
    selectedDateLabel: String,
    isSuperuserViewer: Boolean,
    isBusy: Boolean,
    onToggleExpand: () -> Unit,
    onInspectCoaching: () -> Unit,
    onEditPermissions: () -> Unit,
    onReassignParent: () -> Unit,
    onToggleActivation: (Boolean) -> Unit
) {
    val node = row.node
    val user = node.user
    val stats = node.stats
    val indentDp = (node.relativeLevel.coerceIn(0, 5) * 14).dp

    val (statusLabel, statusColor) = when {
        user.isPendingApproval -> "PENDIENTE ACTIVACIÓN" to AmberDelay
        user.isBlocked -> "BLOQUEADA" to RoseError
        user.isExpired -> "EXPIRADA" to RoseError
        else -> "ACTIVA (${user.remainingDays}d)" to EmeraldSuccess
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentDp)
    ) {
        if (node.relativeLevel > 0) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, end = 6.dp)
                    .width(3.dp)
                    .height(82.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PsaCyanPrimary.copy(alpha = 0.45f))
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(if (row.matchesSearch) SlateSurface else SlateCard)
                .border(
                    width = if (row.matchesSearch || user.isPendingApproval) 1.5.dp else 1.dp,
                    color = when {
                        user.isPendingApproval -> AmberDelay.copy(alpha = 0.65f)
                        row.matchesSearch -> PsaCyanPrimary
                        else -> SlateCardBorder
                    },
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(12.dp)
                .testTag("tree_node_${user.normalizedEmail}"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Node header row: expand arrow + avatar/name + activation badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (row.hasChildren) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("tree_expand_${user.normalizedEmail}")
                    ) {
                        Icon(
                            imageVector = if (row.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (row.isExpanded) "Contraer rama" else "Expandir rama",
                            tint = PsaCyanGlow
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.username.trim().firstOrNull()?.uppercase() ?: "V",
                            color = PsaCyanGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.username,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.normalizedEmail.ifBlank { user.token },
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Hierarchy metadata pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SmallBadgeChip(
                    text = "Nivel ${user.hierarchyDepth}",
                    color = IndigoPrimary
                )
                if (node.totalSubtreeCount > 0) {
                    SmallBadgeChip(
                        text = "${node.directSubCount} dir • ${node.totalSubtreeCount} rama",
                        color = PsaCyanPrimary
                    )
                }
                if (user.canManageSubUsers) {
                    val depthTxt = if (user.visibilityDepth <= 0) "Ve toda rama" else "Ve ${user.visibilityDepth} niv."
                    SmallBadgeChip(
                        text = "Sub-Gestión ✓ ($depthTxt)",
                        color = EmeraldSuccess
                    )
                }
                if (user.parentEmail.isNotBlank()) {
                    SmallBadgeChip(
                        text = "Bajo: ${user.parentEmail.substringBefore("@")}",
                        color = PurpleNeon
                    )
                }
            }

            // Seller Selected-Day & Total Calls / Appointments / Messaging Snapshot
            val dayBucket = HierarchyTreeBuilder.findBucketForDate(stats, selectedDateKey)
            Surface(
                color = SlateBackground.copy(alpha = 0.7f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$selectedDateLabel: ✉️ ${dayBucket.messagesSuccess} • 📞 ${dayBucket.callsCount}",
                            color = TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "📅 ${dayBucket.appointmentsCount} citas • 🧪 ${dayBucket.demosCount} demos",
                            color = EmeraldSuccess,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val ok = stats?.messagesSuccess ?: 0
                    val fail = stats?.messagesFailed ?: 0
                    val rate = stats?.messagingSuccessRate ?: 0f
                    val callsOk = stats?.callsConnected ?: 0
                    val callsTot = stats?.callsAttempted ?: 0
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Acumulado: ✉️ $ok ok / $fail fail (${"%.0f".format(rate)}%)",
                            color = if (fail > ok && fail > 0) RoseError else TextMuted,
                            fontSize = 10.5.sp
                        )
                        Text(
                            text = "📞 $callsOk/$callsTot llam.",
                            color = PsaCyanGlow,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            // Action Buttons (Coaching + Superuser Permissions & Tree Moves)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onInspectCoaching,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("tree_coaching_${user.normalizedEmail}")
                ) {
                    Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = PsaCyanGlow, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Métricas y Coaching", color = PsaCyanGlow, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }

                if (isSuperuserViewer) {
                    OutlinedButton(
                        onClick = onEditPermissions,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("tree_perms_${user.normalizedEmail}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Permisos", color = IndigoPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onReassignParent,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("tree_reparent_${user.normalizedEmail}")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Superior", color = PurpleNeon, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (user.isPendingApproval || user.isBlocked || user.isExpired) {
                        Button(
                            onClick = { onToggleActivation(true) },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("tree_activate_${user.normalizedEmail}")
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Habilitar", color = Color.Black, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallBadgeChip(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PendingRequestApprovalCard(
    request: HierarchyRequest,
    isBusy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isLink = request.parsedType == HierarchyRequestType.LINK_EXISTING_USER
    Surface(
        color = SlateCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLink) "🔗 Vincular Vendedor Existente" else "👤 Nuevo Sub-Usuario Propuesto",
                    color = PsaCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
                Text(
                    text = formatRelativeActivityTime(request.createdAtMs),
                    color = TextMuted,
                    fontSize = 10.5.sp
                )
            }

            Text(
                text = "${request.targetUsername.ifBlank { request.normalizedTargetEmail }} (${request.normalizedTargetEmail})",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )
            Text(
                text = "Solicitado por: ${request.requestedParentName.ifBlank { request.normalizedParentEmail }} (${request.normalizedParentEmail})",
                color = TextSecondary,
                fontSize = 11.5.sp
            )
            if (!isLink) {
                Text(
                    text = "Vigencia sugerida: ${request.suggestedDays} días • ${request.maxDevices} dispositivo(s)",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            if (request.notes.isNotBlank()) {
                Text(
                    text = "\"${request.notes}\"",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onApprove,
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("approve_request_${request.id}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aprobar y Habilitar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onReject,
                    enabled = !isBusy,
                    border = BorderStroke(1.dp, RoseError.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("reject_request_${request.id}")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = RoseError, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rechazar", color = RoseError, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ManagerRequestStatusRow(req: HierarchyRequest) {
    val (label, color) = when (req.parsedStatus) {
        HierarchyRequestStatus.PENDING -> "Pendiente de Superusuario" to AmberDelay
        HierarchyRequestStatus.APPROVED -> "Aprobado y Activo" to EmeraldSuccess
        HierarchyRequestStatus.REJECTED -> "Rechazado" to RoseError
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SlateBackground.copy(alpha = 0.65f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = req.targetUsername.ifBlank { req.normalizedTargetEmail },
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp
            )
            Text(
                text = req.normalizedTargetEmail,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
        Surface(
            color = color.copy(alpha = 0.16f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = label,
                color = color,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProposeSubUserDialog(
    onDismiss: () -> Unit,
    onSubmitNew: (email: String, name: String, days: Int, devices: Int, notes: String) -> Unit,
    onSubmitLinkExisting: (email: String, name: String, notes: String) -> Unit
) {
    var mode by remember { mutableStateOf(HierarchyRequestType.NEW_SUB_USER) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var daysStr by remember { mutableStateOf("30") }
    var maxDevices by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Solicitar Sub-Usuario",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = mode == HierarchyRequestType.NEW_SUB_USER,
                        onClick = { mode = HierarchyRequestType.NEW_SUB_USER },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        label = { Text("Nuevo Usuario", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PsaCyanPrimary,
                            selectedLabelColor = Color.Black,
                            selectedLeadingIconColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = mode == HierarchyRequestType.LINK_EXISTING_USER,
                        onClick = { mode = HierarchyRequestType.LINK_EXISTING_USER },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        label = { Text("Ya Tiene Cuenta", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PsaCyanPrimary,
                            selectedLabelColor = Color.Black,
                            selectedLeadingIconColor = Color.Black
                        )
                    )
                }

                Text(
                    text = if (mode == HierarchyRequestType.NEW_SUB_USER) {
                        "La cuenta quedará creada en estado pendiente. Solo el Superusuario puede activarla para ingresar."
                    } else {
                        "Si el vendedor ya ingresaba antes a la app, envía su correo Google para que el Superusuario lo vincule debajo de ti."
                    },
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMsg = null },
                    label = { Text("Correo Google del vendedor") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Nombre visible") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (mode == HierarchyRequestType.NEW_SUB_USER) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = daysStr,
                            onValueChange = { daysStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Días sugeridos") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxDevices.toString(),
                            onValueChange = {
                                val parsed = it.toIntOrNull()
                                if (parsed != null) maxDevices = parsed.coerceIn(1, 5)
                            },
                            label = { Text("Equipos (1-5)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Nota para el Superusuario (opcional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!errorMsg.isNullOrBlank()) {
                    Text(text = errorMsg ?: "", color = RoseError, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanEmail = email.trim().lowercase()
                    if (!cleanEmail.contains("@") || cleanEmail.length < 5) {
                        errorMsg = "Ingresa un correo de Google válido."
                        return@Button
                    }
                    if (name.isBlank()) {
                        errorMsg = "Ingresa el nombre del vendedor."
                        return@Button
                    }
                    if (mode == HierarchyRequestType.NEW_SUB_USER) {
                        val days = (daysStr.toIntOrNull() ?: 30).coerceIn(1, 3650)
                        onSubmitNew(cleanEmail, name.trim(), days, maxDevices, notes.trim())
                    } else {
                        onSubmitLinkExisting(cleanEmail, name.trim(), notes.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Enviar al Superusuario", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SlateCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApproveHierarchyRequestDialog(
    request: HierarchyRequest,
    onDismiss: () -> Unit,
    onConfirmApprove: (days: Int?, allowSubManagement: Boolean, visibilityDepth: Int) -> Unit
) {
    var daysStr by remember { mutableStateOf(request.suggestedDays.coerceAtLeast(1).toString()) }
    var allowSub by remember { mutableStateOf(false) }
    var depth by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Habilitar Cuenta en el Equipo",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Vendedor: ${request.targetUsername} (${request.normalizedTargetEmail})\nSuperior asignado: ${request.normalizedParentEmail}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                OutlinedTextField(
                    value = daysStr,
                    onValueChange = { daysStr = it.filter { c -> c.isDigit() } },
                    label = {
                        Text(
                            if (request.parsedType == HierarchyRequestType.LINK_EXISTING_USER)
                                "Días de vigencia (vacío = conservar actuales)"
                            else
                                "Días de vigencia a otorgar"
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Permitir tener sub-usuarios",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Podrá ver métricas de su rama y proponer altas",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = allowSub,
                        onCheckedChange = { allowSub = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = EmeraldSuccess)
                    )
                }

                AnimatedVisibility(visible = allowSub) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Alcance de visualización en el equipo:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        DepthSelectorRow(selectedDepth = depth, onSelectDepth = { depth = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedDays = daysStr.toIntOrNull()?.takeIf { it > 0 }
                    onConfirmApprove(parsedDays, allowSub, depth)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Aprobar y Activar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SlateCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditHierarchyPermissionsDialog(
    user: UserAccount,
    onDismiss: () -> Unit,
    onSave: (canManageSubUsers: Boolean, visibilityDepth: Int) -> Unit
) {
    var canManage by remember(user) { mutableStateOf(user.canManageSubUsers) }
    var depth by remember(user) { mutableIntStateOf(user.visibilityDepth.coerceIn(0, 10)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Permisos de ${user.username}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = user.normalizedEmail,
                    color = TextMuted,
                    fontSize = 11.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Puede gestionar sub-usuarios",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Habilita acceso al árbol y a métricas/coaching de sus subordinados.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = canManage,
                        onCheckedChange = { canManage = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = EmeraldSuccess)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "¿Hasta qué nivel debajo puede ver?",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
                    )
                    DepthSelectorRow(selectedDepth = depth, onSelectDepth = { depth = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(canManage, depth) },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Guardar Permisos", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SlateCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DepthSelectorRow(
    selectedDepth: Int,
    onSelectDepth: (Int) -> Unit
) {
    val options = listOf(
        0 to "Toda la rama",
        1 to "1 nivel",
        2 to "2 niveles",
        3 to "3 niveles"
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selectedDepth == value,
                onClick = { onSelectDepth(value) },
                label = { Text(label, fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PsaCyanPrimary,
                    selectedLabelColor = Color.Black
                )
            )
        }
    }
}

@Composable
private fun ReassignParentDialog(
    movingUser: UserAccount,
    allUsers: List<UserAccount>,
    onDismiss: () -> Unit,
    onSelectNewParent: (newParentEmail: String) -> Unit
) {
    val movingEmail = movingUser.normalizedEmail
    val validCandidates = remember(movingEmail, allUsers) {
        allUsers
            .filter { candidate ->
                val cEmail = candidate.normalizedEmail
                cEmail.isNotBlank() &&
                    cEmail != movingEmail &&
                    candidate.role != "SUPERUSER" &&
                    !HierarchyTreeBuilder.wouldCreateCycle(movingEmail, cEmail, allUsers)
            }
            .sortedBy { it.username.lowercase() }
    }

    var selectedParent by remember(movingUser) { mutableStateOf(movingUser.parentEmail.trim().lowercase()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Asignar Superior para ${movingUser.username}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Selecciona quién supervisará a ${movingUser.username} (sus sub-usuarios y estadísticas se actualizarán automáticamente en cascada).",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )

                ParentCandidateRow(
                    title = "Raíz Principal (Sin superior intermedio)",
                    subtitle = "Directo bajo el Superusuario",
                    isSelected = selectedParent.isBlank(),
                    onClick = { selectedParent = "" }
                )

                validCandidates.forEach { candidate ->
                    val cEmail = candidate.normalizedEmail
                    ParentCandidateRow(
                        title = candidate.username,
                        subtitle = "$cEmail • Nivel ${candidate.hierarchyDepth}",
                        isSelected = selectedParent == cEmail,
                        onClick = { selectedParent = cEmail }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectNewParent(selectedParent) },
                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Confirmar Ubicación", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SlateCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun ParentCandidateRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) PsaCyanPrimary.copy(alpha = 0.16f) else SlateBackground.copy(alpha = 0.7f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isSelected) PsaCyanPrimary else SlateCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                Text(text = subtitle, color = TextMuted, fontSize = 11.sp)
            }
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = PsaCyanGlow, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SellerCoachingDetailDialog(
    user: UserAccount?,
    stats: SellerPerformanceStats?,
    selectedDateKey: String = "",
    selectedDateLabel: String = "Hoy",
    onSendCoachingMessage: (targetEmail: String, title: String, body: String) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit
) {
    val name = user?.username?.ifBlank { stats?.sellerName } ?: stats?.sellerName ?: "Vendedor"
    val email = user?.normalizedEmail?.ifBlank { stats?.sellerEmail } ?: stats?.sellerEmail.orEmpty()
    val msgOk = stats?.messagesSuccess ?: 0
    val msgFail = stats?.messagesFailed ?: 0
    val msgTotal = stats?.messagesTotal ?: (msgOk + msgFail)
    val msgRate = stats?.messagingSuccessRate ?: 0f
    val callsAttempted = stats?.callsAttempted ?: 0
    val callsConnected = stats?.callsConnected ?: 0
    val callRate = stats?.callConnectionRate ?: 0f
    val buckets = stats?.parsedRecentBuckets.orEmpty()
    val selectedDayBucket = HierarchyTreeBuilder.findBucketForDate(stats, selectedDateKey)

    var coachTitle by remember { mutableStateOf("") }
    var coachBody by remember { mutableStateOf("") }
    var sentMessageConfirmation by remember { mutableStateOf(false) }

    val recommendations = buildList {
        if (msgTotal > 0 && msgRate < 75f) {
            add("• Mensajería con ${"%.0f".format(100f - msgRate)}% de fallos: revisar pausas entre envíos, calidad de números y guion inicial.")
        }
        if (callsAttempted == 0) {
            add("• 0 llamadas registradas: acompañar al vendedor a convertir respuestas de chat en llamadas de 2 minutos.")
        } else if (callRate < 40f) {
            add("• Baja conexión telefónica (${"%.0f".format(callRate)}%): probar envío previo de audio/WhatsApp antes de llamar.")
        }
        if ((stats?.completedDemos ?: 0) == 0 && (stats?.totalContacts ?: 0) > 3) {
            add("• Tiene contactos sin demos completadas: practicar el cierre de visita domiciliaria PSA.")
        }
        if (isEmpty()) {
            add("• Buen ritmo general de mensajería y llamadas. Reforzar seguimiento posventa y referidos en cada visita.")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Coaching y Métricas • $name",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (email.isNotBlank()) {
                    Text(text = email, color = TextMuted, fontSize = 11.5.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedDateKey.isNotBlank()) {
                    Surface(
                        color = PsaCyanPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PsaCyanPrimary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Gestiones del día ($selectedDateLabel)", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("✉️ ${selectedDayBucket.messagesSuccess} ok / ${selectedDayBucket.messagesFailed} fallos", color = TextPrimary, fontSize = 12.sp)
                                Text("📞 ${selectedDayBucket.callsCount} llam.", color = PsaCyanGlow, fontSize = 12.sp)
                                Text("📅 ${selectedDayBucket.appointmentsCount} citas", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("🧪 ${selectedDayBucket.demosCount} demos", color = AmberDelay, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Surface(
                    color = SlateBackground,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Rendimiento de Mensajería", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Exitosos: $msgOk", color = TextPrimary, fontSize = 12.sp)
                            Text("Fallos: $msgFail", color = RoseError, fontSize = 12.sp)
                            Text("Éxito: ${"%.1f".format(msgRate)}%", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Surface(
                    color = SlateBackground,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = PsaCyanPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rendimiento de Llamadas y PSA", color = PsaCyanGlow, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Intentos: $callsAttempted", color = TextPrimary, fontSize = 12.sp)
                            Text("Conectadas: $callsConnected (${"%.0f".format(callRate)}%)", color = PsaCyanPrimary, fontSize = 12.sp)
                            Text("Contactos: ${stats?.totalContacts ?: 0}", color = TextSecondary, fontSize = 12.sp)
                            Text("Demos: ${stats?.completedDemos ?: 0} • Ventas: ${stats?.closedSales ?: 0}", color = EmeraldSuccess, fontSize = 12.sp)
                        }
                    }
                }

                if (buckets.isNotEmpty() && buckets.any { it.totalEvents > 0 }) {
                    Surface(
                        color = SlateBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Historial diario (últimos 30 días)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            buckets.filter { it.totalEvents > 0 }.reversed().take(15).forEach { b ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(b.label.ifBlank { b.dayKey }, color = TextSecondary, fontSize = 11.sp)
                                    Text(
                                        "✉️ ${b.messagesSuccess} • 📞 ${b.callsCount} • 📅 ${b.appointmentsCount} • 🧪 ${b.demosCount}",
                                        color = TextPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    color = AmberDelay.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AmberDelay.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("💡 Orientación Sugerida", color = AmberDelay, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        recommendations.forEach { tip ->
                            Text(text = tip, color = TextPrimary, fontSize = 11.5.sp, lineHeight = 16.sp)
                        }
                    }
                }

                if (email.isNotBlank()) {
                    Surface(
                        color = IndigoPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💬 Enviar Mensaje / Incentivo al Vendedor",
                                color = IndigoPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "¡Vamos hoy!" to "¡No bajes los brazos! Con una tanda de 10 llamadas hoy sumás citas seguras.",
                                    "Tip horario" to "Probá hacer tu bloque principal entre las 18:00 y 20:30, suele tener mejor respuesta.",
                                    "Enfoque demos" to "Excelente movimiento, enfocate ahora en confirmar horario de visita en cada respuesta."
                                ).forEach { (chipLabel, presetText) ->
                                    FilterChip(
                                        selected = coachBody == presetText,
                                        onClick = {
                                            coachTitle = chipLabel
                                            coachBody = presetText
                                            sentMessageConfirmation = false
                                        },
                                        label = { Text(chipLabel, fontSize = 11.sp) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = coachTitle,
                                onValueChange = { coachTitle = it; sentMessageConfirmation = false },
                                label = { Text("Título (opcional)", fontSize = 11.5.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = coachBody,
                                onValueChange = { coachBody = it; sentMessageConfirmation = false },
                                label = { Text("Mensaje o consejo para $name", fontSize = 11.5.sp) },
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    onSendCoachingMessage(email, coachTitle, coachBody)
                                    coachTitle = ""
                                    coachBody = ""
                                    sentMessageConfirmation = true
                                },
                                enabled = coachBody.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .testTag("btn_send_leader_coaching_msg")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enviar Mensaje", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            if (sentMessageConfirmation) {
                                Text(
                                    text = "✅ Mensaje enviado como banner y notificación al vendedor.",
                                    color = EmeraldSuccess,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PsaCyanPrimary),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cerrar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SlateCard,
        shape = RoundedCornerShape(16.dp)
    )
}

private fun collectAllParentEmails(roots: List<HierarchyTreeNode>): Set<String> {
    val result = mutableSetOf<String>()
    fun walk(node: HierarchyTreeNode) {
        if (node.children.isNotEmpty()) {
            result.add(node.user.normalizedEmail)
            node.children.forEach { walk(it) }
        }
    }
    roots.forEach { walk(it) }
    return result
}
