package com.humblesolutions.indsphinx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.humblesolutions.indsphinx.model.Complaint
import com.humblesolutions.indsphinx.model.Notice
import com.humblesolutions.indsphinx.repository.BackendComplaintRepository
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.indsphinx.viewmodel.HomeUiState
import com.humblesolutions.indsphinx.viewmodel.HomeViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val NavyBlue = Color(0xFF1E2D6B)
private val BackgroundGray = Color(0xFFF2F4F8)

@Composable
fun HomeScreen(onSignOut: () -> Unit) {
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestNotice by viewModel.latestNotice.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var ongoingComplaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var showVisitorPass by remember { mutableStateOf(false) }
    var showFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is HomeUiState.AccessDenied) onSignOut()
    }

    val ready = uiState as? HomeUiState.Ready
    val name = ready?.name ?: ""
    val greeting = ready?.greeting ?: ""
    val email = ready?.email ?: ""
    val role = ready?.role ?: ""
    val empId = ready?.empId ?: ""
    val flatNumber = ready?.flatNumber ?: ""
    val occupantFrom = ready?.occupantFrom ?: 0L
    val isCoordinator = ready?.isCoordinator ?: false
    val occupantDocId = ready?.occupantDocId ?: ""
    val flatId = ready?.flatId ?: ""

    LaunchedEffect(occupantDocId) {
        if (occupantDocId.isNotEmpty()) {
            try {
                val all = BackendComplaintRepository().fetchByOccupant(occupantDocId)
                ongoingComplaints = all.filter { it.status != "CLOSED" }.take(4)
            } catch (_: Exception) {}
        }
    }

    if (showVisitorPass) {
        VisitorPassScreen(
            occupantId = occupantDocId,
            occupantName = name,
            flatId = flatId,
            flatNumber = flatNumber,
            onBack = { showVisitorPass = false }
        )
        return
    }

    if (showFeedback) {
        FeedbackScreen(
            occupantId = occupantDocId,
            occupantName = name,
            onBack = { showFeedback = false }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                windowInsets = WindowInsets(0),
                modifier = Modifier.width(300.dp)
            ) {
                HomeDrawerContent(
                    name = name,
                    flatNumber = flatNumber,
                    onNavigateToComplaints = {
                        scope.launch { drawerState.close() }
                        selectedTab = 1
                    },
                    onNavigateToVisitorPass = {
                        scope.launch { drawerState.close() }
                        showVisitorPass = true
                    },
                    onNavigateToFeedback = {
                        scope.launch { drawerState.close() }
                        showFeedback = true
                    },
                    onNavigateToNoticeboard = {
                        scope.launch { drawerState.close() }
                        selectedTab = 2
                    },
                    onSignOut = {
                        viewModel.signOut()
                        onSignOut()
                    }
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    val tabs = listOf(
                        "Home" to Icons.Outlined.Home,
                        "Complaints" to Icons.Outlined.Description,
                        "Noticeboard" to Icons.Outlined.NotificationsNone,
                        "Profile" to Icons.Outlined.Person
                    )
                    tabs.forEachIndexed { index, (label, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp)) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NavyBlue,
                                selectedTextColor = NavyBlue,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = Color(0xFF9E9E9E),
                                unselectedTextColor = Color(0xFF9E9E9E)
                            )
                        )
                    }
                }
            },
            containerColor = BackgroundGray,
            contentWindowInsets = WindowInsets(0)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                when (selectedTab) {
                    0 -> {
                        HomeHeader(
                            name = name,
                            greeting = greeting,
                            flatNumber = flatNumber,
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                        ) {
                            Spacer(Modifier.height(16.dp))
                            QuickShortcutsSection(
                                onAddComplaint = { selectedTab = 1 },
                                onNoticeboard = { selectedTab = 2 },
                                onVisitorPass = { showVisitorPass = true },
                                onFeedback = { showFeedback = true }
                            )
                            Spacer(Modifier.height(20.dp))
                            NewNoticesSection(
                                notice = latestNotice,
                                onViewAll = { selectedTab = 2 }
                            )
                            Spacer(Modifier.height(20.dp))
                            OngoingComplaintsSection(
                                complaints = ongoingComplaints,
                                onViewAll = { selectedTab = 1 }
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                    1 -> ComplaintsScreen(
                        occupantName = name,
                        occupantEmail = email,
                        occupantDocId = occupantDocId,
                        flatNumber = flatNumber,
                        flatId = flatId,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                    2 -> NoticeboardScreen(
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                    3 -> ProfileContent(
                        name = name,
                        email = email,
                        role = role,
                        empId = empId,
                        flatNumber = flatNumber,
                        occupantFrom = occupantFrom,
                        isCoordinator = isCoordinator,
                        onSignOut = {
                            viewModel.signOut()
                            onSignOut()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    )
                    else -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Coming Soon", color = Color(0xFF999999), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// MARK: - Header

@Composable
private fun HomeHeader(name: String, greeting: String, flatNumber: String, onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyBlue)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Menu, null,
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onMenuClick() }
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (greeting.isNotEmpty()) {
                    Text(greeting, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
                Text(
                    text = name.ifEmpty { "Loading..." },
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Home, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(flatNumber.ifEmpty { "—" }, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
    }
}

// MARK: - Drawer

@Composable
private fun HomeDrawerContent(
    name: String,
    flatNumber: String,
    onNavigateToComplaints: () -> Unit,
    onNavigateToVisitorPass: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onNavigateToNoticeboard: () -> Unit = {},
    onSignOut: () -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight()) {
        // Navy header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyBlue)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(name.ifEmpty { "—" }, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Home, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(flatNumber.ifEmpty { "—" }, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }

        // Menu items
        Column(
            modifier = Modifier
                .weight(1f)
                .background(Color.White)
                .padding(vertical = 8.dp)
        ) {
            DrawerMenuItem(icon = Icons.Outlined.Description, label = "Complaint", onClick = onNavigateToComplaints)
            DrawerMenuItem(icon = Icons.Outlined.PersonAdd, label = "Visitor Pass", onClick = onNavigateToVisitorPass)
            DrawerMenuItem(icon = Icons.Outlined.NotificationsNone, label = "Notice Board", onClick = onNavigateToNoticeboard)
            DrawerMenuItem(icon = Icons.Outlined.ChatBubbleOutline, label = "Feedback", onClick = onNavigateToFeedback)
        }

        // Logout
        HorizontalDivider(color = Color(0xFFEEEEEE))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSignOut() }
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFEEEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text("Logout", color = Color(0xFFE53935), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DrawerMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color(0xFF555555), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 15.sp, color = Color(0xFF1A1A2E), modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.NavigateNext, null, tint = Color(0xFFBBBBBB), modifier = Modifier.size(20.dp))
    }
}

// MARK: - Profile

@Composable
private fun ProfileContent(
    name: String,
    email: String,
    role: String,
    empId: String,
    flatNumber: String,
    occupantFrom: Long,
    isCoordinator: Boolean,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val occupantFromFormatted = remember(occupantFrom) {
        if (occupantFrom > 0L) {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            sdf.format(java.util.Date(occupantFrom))
        } else "—"
    }
    Column(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(NavyBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, null, tint = Color.White, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(name.ifEmpty { "—" }, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
        Spacer(Modifier.height(4.dp))
        Text(role.ifEmpty { "—" }, fontSize = 14.sp, color = NavyBlue, fontWeight = FontWeight.Medium)

        Spacer(Modifier.height(28.dp))

        // Details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                ProfileDetailRow(
                    icon = Icons.Outlined.Person,
                    label = "Full Name",
                    value = name.ifEmpty { "—" }
                )
                HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                ProfileDetailRow(
                    icon = Icons.Outlined.Numbers,
                    label = "Employee ID",
                    value = empId.ifEmpty { "—" }
                )
                HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                ProfileDetailRow(
                    icon = Icons.Outlined.Email,
                    label = "Email",
                    value = email.ifEmpty { "—" }
                )
                HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                ProfileDetailRow(
                    icon = Icons.Outlined.Home,
                    label = "Flat Number",
                    value = flatNumber.ifEmpty { "—" }
                )
                HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                ProfileDetailRow(
                    icon = Icons.Outlined.Badge,
                    label = "Role",
                    value = role.ifEmpty { "—" }
                )
                HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                ProfileDetailRow(
                    icon = Icons.Outlined.CalendarToday,
                    label = "Occupant Since",
                    value = occupantFromFormatted
                )
                HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                ProfileDetailRow(
                    icon = Icons.Outlined.StarBorder,
                    label = "Coordinator",
                    value = if (isCoordinator) "Yes" else "No"
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Sign out button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFEEEE))
                .clickable { onSignOut() }
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sign Out", color = Color(0xFFE53935), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF0F4FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = NavyBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label, fontSize = 12.sp, color = Color(0xFF999999))
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 15.sp, color = Color(0xFF1A1A2E), fontWeight = FontWeight.Medium)
        }
    }
}

// MARK: - Home tab content

@Composable
private fun QuickShortcutsSection(onAddComplaint: () -> Unit, onNoticeboard: () -> Unit = {}, onVisitorPass: () -> Unit = {}, onFeedback: () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.FlashOn, null, tint = NavyBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Quick Shortcuts", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF1A1A2E))
    }
    Spacer(Modifier.height(12.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShortcutItem(
                    label = "Add Complaint",
                    icon = Icons.Outlined.Add,
                    iconBg = Color(0xFFEEF2FF),
                    iconTint = Color(0xFF3B4FD8),
                    modifier = Modifier.weight(1f),
                    onClick = onAddComplaint
                )
                ShortcutItem(
                    label = "Notice Board",
                    icon = Icons.Outlined.NotificationsNone,
                    iconBg = Color(0xFFDDE3FF),
                    iconTint = NavyBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onNoticeboard
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShortcutItem(
                    label = "Visitor Pass",
                    icon = Icons.Outlined.PersonAdd,
                    iconBg = Color(0xFFECFDF5),
                    iconTint = Color(0xFF059669),
                    modifier = Modifier.weight(1f),
                    onClick = onVisitorPass
                )
                ShortcutItem(
                    label = "Feedback",
                    icon = Icons.Outlined.ChatBubbleOutline,
                    iconBg = Color(0xFFFFF7ED),
                    iconTint = Color(0xFFD97706),
                    modifier = Modifier.weight(1f),
                    onClick = onFeedback
                )
            }
        }
    }
}

@Composable
private fun ShortcutItem(
    label: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 13.sp, color = Color(0xFF333333), textAlign = TextAlign.Center)
    }
}

@Composable
private fun NewNoticesSection(notice: Notice?, onViewAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.NotificationsNone, null, tint = NavyBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("New Notices", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF1A1A2E))
        }
        Text(
            "View All >",
            color = NavyBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onViewAll() }
        )
    }
    Spacer(Modifier.height(12.dp))
    if (notice != null) {
        val dateFormatted = remember(notice.publishedAt) {
            if (notice.publishedAt > 0L) {
                val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                sdf.format(java.util.Date(notice.publishedAt))
            } else ""
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(NavyBlue)
                )
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDDE3FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.NotificationsNone, null, tint = NavyBlue, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            notice.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF1A1A2E),
                            maxLines = 2
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            notice.description,
                            fontSize = 12.sp,
                            color = Color(0xFF555555),
                            maxLines = 3
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(dateFormatted, fontSize = 11.sp, color = Color(0xFF999999))
                    }
                }
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Text(
                "No new notices",
                fontSize = 14.sp,
                color = Color(0xFF999999),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun daysOpen(dateMillis: Long): String {
    val days = (System.currentTimeMillis() - dateMillis) / (1000L * 60 * 60 * 24)
    return when {
        days <= 0L -> "Today"
        days == 1L -> "1d open"
        else -> "${days}d open"
    }
}

@Composable
private fun OngoingComplaintsSection(complaints: List<Complaint>, onViewAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, null, tint = NavyBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Ongoing Complaints", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF1A1A2E))
        }
        Text(
            "View All >",
            color = NavyBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onViewAll() }
        )
    }
    Spacer(Modifier.height(12.dp))
    if (complaints.isEmpty()) {
        Text("No ongoing complaints", fontSize = 13.sp, color = Color(0xFF999999))
    } else {
        complaints.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { complaint ->
                    ComplaintCard(
                        timeOpen = daysOpen(complaint.date),
                        title = complaint.problem.ifEmpty { complaint.category },
                        category = complaint.category,
                        status = complaint.status,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ComplaintCard(
    timeOpen: String,
    title: String,
    category: String,
    status: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⏱", fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                Text(timeOpen, fontSize = 12.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
            Spacer(Modifier.height(2.dp))
            Text(category, fontSize = 12.sp, color = Color(0xFF888888))
            Spacer(Modifier.height(8.dp))
            StatusBadge(status)
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "In Progress" -> Color(0xFFEEF2FF) to Color(0xFF3B4FD8)
        "Assigned" -> Color(0xFFF5EEFF) to Color(0xFF7C3AED)
        "Closed", "CLOSED" -> Color(0xFFECFDF5) to Color(0xFF059669)
        "Pending", "OPEN" -> Color(0xFFFFF7ED) to Color(0xFFD97706)
        else -> Color(0xFFF5F5F5) to Color(0xFF616161)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(status, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
