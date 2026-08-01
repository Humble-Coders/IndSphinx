package com.humblesolutions.indsphinx.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.humblesolutions.indsphinx.model.AppNotification
import com.humblesolutions.indsphinx.model.Complaint
import com.humblesolutions.indsphinx.model.Notice
import com.humblesolutions.indsphinx.model.OccupantAsset
import com.humblesolutions.indsphinx.repository.BackendComplaintRepository
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.indsphinx.viewmodel.HomeUiState
import com.humblesolutions.indsphinx.viewmodel.HomeViewModel
import com.humblesolutions.indsphinx.viewmodel.RevisedFormState
import androidx.compose.material3.ButtonDefaults
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
private const val NOTIFICATIONS_TAG = "NotificationsFlow"

private sealed class HomeOverlay {
    object None : HomeOverlay()
    object VisitorPass : HomeOverlay()
    object Feedback : HomeOverlay()
    object Documents : HomeOverlay()
    object CoordinatorForm : HomeOverlay()
    object FlatVacant : HomeOverlay()
    object Notifications : HomeOverlay()
    object Assets : HomeOverlay()
    data class NoticeQuestion(val noticeId: String) : HomeOverlay()
    data class QuestionNotification(val qnId: String) : HomeOverlay()
}

@Composable
fun HomeScreen(
    onSignOut: () -> Unit,
    pendingDeepLink: com.humblesolutions.indsphinx.PendingDeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestNotice by viewModel.latestNotice.collectAsStateWithLifecycle()
    val formDueStatus by viewModel.formDueStatus.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    val revisedFormState by viewModel.revisedFormState.collectAsStateWithLifecycle()

    // Blocking check — must accept revised form before accessing home
    when (val rfs = revisedFormState) {
        is RevisedFormState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().background(BackgroundGray),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NavyBlue)
            }
            return
        }
        is RevisedFormState.Ready -> {
            BackHandler { /* block back — form must be completed */ }
            RevisedAmenitiesScreen(
                commonAmenities = rfs.commonAmenities,
                roomAmenities = rfs.roomAmenities,
                selectedAmenities = rfs.selectedAmenities,
                isSubmitting = rfs.isSubmitting,
                canSubmit = rfs.canSubmit,
                onToggleAmenity = { viewModel.toggleRevisedAmenity(it) },
                onSubmit = { viewModel.submitRevisedForm() }
            )
            return
        }
        is RevisedFormState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().background(BackgroundGray),
                contentAlignment = Alignment.Center
            ) {
                Text(rfs.message, color = Color(0xFFE53935), fontSize = 14.sp)
            }
            return
        }
        RevisedFormState.Hidden -> Unit
    }

    var selectedTab by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var ongoingComplaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var overlay by remember { mutableStateOf<HomeOverlay>(HomeOverlay.None) }
    var pendingComplaintAction by remember { mutableStateOf<ComplaintStartAction?>(null) }
    var pendingNotice by remember { mutableStateOf<Notice?>(null) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is HomeUiState.AccessDenied) onSignOut()
    }

    LaunchedEffect(pendingDeepLink) {
        when (val dl = pendingDeepLink) {
            is com.humblesolutions.indsphinx.PendingDeepLink.NoticeQuestion -> {
                selectedTab = 2
                overlay = HomeOverlay.NoticeQuestion(dl.noticeId)
                onDeepLinkConsumed()
            }
            is com.humblesolutions.indsphinx.PendingDeepLink.QuestionNotification -> {
                overlay = HomeOverlay.QuestionNotification(dl.qnId)
                onDeepLinkConsumed()
            }
            com.humblesolutions.indsphinx.PendingDeepLink.Complaint -> {
                // Land on the Complaints tab; the existing list and titles
                // tell the user which complaint the push referred to.
                overlay = HomeOverlay.None
                selectedTab = 1
                onDeepLinkConsumed()
            }
            com.humblesolutions.indsphinx.PendingDeepLink.VisitorPass -> {
                overlay = HomeOverlay.VisitorPass
                onDeepLinkConsumed()
            }
            com.humblesolutions.indsphinx.PendingDeepLink.FlatVacantRequest -> {
                overlay = HomeOverlay.FlatVacant
                onDeepLinkConsumed()
            }
            com.humblesolutions.indsphinx.PendingDeepLink.Assets -> {
                overlay = HomeOverlay.Assets
                onDeepLinkConsumed()
            }
            com.humblesolutions.indsphinx.PendingDeepLink.OpenNotifications -> {
                overlay = HomeOverlay.Notifications
                onDeepLinkConsumed()
            }
            null -> Unit
        }
    }

    val due = formDueStatus
    if (due != null && due.isDue && overlay is HomeOverlay.None) {
        FormDueDialog(
            frequencyMonths = due.frequencyMonths,
            onFillForm = {
                viewModel.dismissFormDue()
                overlay = HomeOverlay.CoordinatorForm
            },
            onDismiss = { viewModel.dismissFormDue() }
        )
    }

    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text("Log Out", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
            text = { Text("Are you sure you want to log out?", fontSize = 14.sp, color = Color(0xFF555555)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirmation = false
                    viewModel.signOut()
                    onSignOut()
                }) {
                    Text("Log Out", color = Color(0xFFE53935), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("Cancel", color = Color(0xFF555555))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
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
    val authUid = ready?.authUid ?: ""
    val currentAssets by viewModel.currentAssets.collectAsStateWithLifecycle()

    LaunchedEffect(occupantDocId) {
        if (occupantDocId.isNotEmpty()) {
            BackendComplaintRepository().observeByOccupant(occupantDocId).collect { all ->
                ongoingComplaints = all.filter { it.status != "CLOSED" }.take(4)
            }
        }
    }

    // Tab back: goes to Home tab; overlay back: dismisses overlay. Overlay handler is last → highest priority.
    BackHandler(enabled = overlay is HomeOverlay.None && selectedTab != 0) {
        selectedTab = 0
    }
    BackHandler(enabled = overlay !is HomeOverlay.None) {
        overlay = HomeOverlay.None
    }

    AnimatedContent(
        targetState = overlay,
        transitionSpec = {
            if (targetState !is HomeOverlay.None) {
                (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))).togetherWith(
                    slideOutHorizontally(tween(250)) { -it / 4 } + fadeOut(tween(200))
                )
            } else {
                (slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300))).togetherWith(
                    slideOutHorizontally(tween(300)) { it } + fadeOut(tween(250))
                )
            }
        },
        label = "OverlayAnimation"
    ) { currentOverlay ->
        when (currentOverlay) {
            is HomeOverlay.VisitorPass -> VisitorPassScreen(
                occupantId = occupantDocId,
                occupantName = name,
                flatId = flatId,
                flatNumber = flatNumber,
                onBack = { overlay = HomeOverlay.None }
            )
            is HomeOverlay.Feedback -> FeedbackScreen(
                occupantId = occupantDocId,
                occupantName = name,
                onBack = { overlay = HomeOverlay.None }
            )
            is HomeOverlay.Documents -> DocumentsScreen(
                onBack = { overlay = HomeOverlay.None }
            )
            is HomeOverlay.Assets -> AssetsScreen(
                authUid = authUid,
                onBack = { overlay = HomeOverlay.None }
            )
            is HomeOverlay.CoordinatorForm -> CoordinatorFormScreen(
                occupantId = occupantDocId,
                flatId = flatId,
                coordinatorName = name,
                flatNumber = flatNumber,
                onBack = { overlay = HomeOverlay.None }
            )
            is HomeOverlay.FlatVacant -> FlatVacantRequestScreen(
                occupantId = occupantDocId,
                occupantName = name,
                flatId = flatId,
                flatNumber = flatNumber,
                onBack = { overlay = HomeOverlay.None }
            )
            is HomeOverlay.Notifications -> NotificationsScreen(
                notifications = notifications,
                onMarkRead = { viewModel.markNotificationRead(it) },
                onOpenQuestion = { qnId -> overlay = HomeOverlay.QuestionNotification(qnId) },
                onOpenComplaint = {
                    // Land on the Complaints tab with the existing list; the
                    // notification title makes it obvious which complaint
                    // the row refers to. No extra Firestore read needed.
                    overlay = HomeOverlay.None
                    selectedTab = 1
                },
                onOpenVisitorPass = {
                    overlay = HomeOverlay.VisitorPass
                },
                onOpenVacantRequest = {
                    overlay = HomeOverlay.FlatVacant
                },
                onBack = { overlay = HomeOverlay.None }
            )
            is HomeOverlay.NoticeQuestion -> NoticeQuestionScreen(
                noticeId = currentOverlay.noticeId,
                displayName = name,
                flatNo = flatNumber,
                recipientType = role,
                onBack = { overlay = HomeOverlay.None }
            )
            is HomeOverlay.QuestionNotification -> QuestionNotificationScreen(
                qnId = currentOverlay.qnId,
                displayName = name,
                flatNo = flatNumber,
                recipientType = role,
                onBack = { overlay = HomeOverlay.None }
            )
            is HomeOverlay.None -> ModalNavigationDrawer(
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
                            isCoordinator = isCoordinator,
                            onNavigateToComplaints = {
                                scope.launch { drawerState.close() }
                                selectedTab = 1
                            },
                            onNavigateToVisitorPass = {
                                scope.launch { drawerState.close() }
                                overlay = HomeOverlay.VisitorPass
                            },
                            onNavigateToFeedback = {
                                scope.launch { drawerState.close() }
                                overlay = HomeOverlay.Feedback
                            },
                            onNavigateToNoticeboard = {
                                scope.launch { drawerState.close() }
                                selectedTab = 2
                            },
                            onNavigateToDocuments = {
                                scope.launch { drawerState.close() }
                                overlay = HomeOverlay.Documents
                            },
                            onNavigateToCoordinatorForm = {
                                scope.launch { drawerState.close() }
                                overlay = HomeOverlay.CoordinatorForm
                            },
                            onNavigateToAssets = {
                                scope.launch { drawerState.close() }
                                overlay = HomeOverlay.Assets
                            },
                            onNavigateToFlatVacant = {
                                scope.launch { drawerState.close() }
                                overlay = HomeOverlay.FlatVacant
                            },
                            onSignOut = { showLogoutConfirmation = true }
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
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                val toRight = targetState > initialState
                                (slideInHorizontally(tween(280)) { if (toRight) it else -it } + fadeIn(tween(280))).togetherWith(
                                    slideOutHorizontally(tween(280)) { if (toRight) -it else it } + fadeOut(tween(280))
                                )
                            },
                            label = "TabAnimation",
                            modifier = Modifier.fillMaxSize()
                        ) { tab ->
                            when (tab) {
                                0 -> Column(modifier = Modifier.fillMaxSize()) {
                                    HomeTopBar(
                                        unreadCount = unreadCount,
                                        onMenuClick = { scope.launch { drawerState.open() } },
                                        onNotificationsClick = {
                                            Log.d(NOTIFICATIONS_TAG, "HomeTopBar: open notifications overlay")
                                            overlay = HomeOverlay.Notifications
                                        }
                                    )
                                    HomeGreetingCard(
                                        greeting = greeting,
                                        name = name,
                                        flatNumber = flatNumber,
                                        onProfileClick = { selectedTab = 3 }
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Spacer(Modifier.height(16.dp))
                                        QuickShortcutsSection(
                                            onAddComplaint = {
                                                pendingComplaintAction = ComplaintStartAction.AddComplaint(flatId)
                                                selectedTab = 1
                                            },
                                            onNoticeboard = { selectedTab = 2 },
                                            onVisitorPass = { overlay = HomeOverlay.VisitorPass },
                                            onFeedback = { overlay = HomeOverlay.Feedback }
                                        )
                                        Spacer(Modifier.height(20.dp))
                                        NewNoticesSection(
                                            notice = latestNotice,
                                            onViewAll = { selectedTab = 2 },
                                            onNoticeClick = { notice ->
                                                pendingNotice = notice
                                                selectedTab = 2
                                            }
                                        )
                                        Spacer(Modifier.height(20.dp))
                                        OngoingComplaintsSection(
                                            complaints = ongoingComplaints,
                                            onViewAll = {
                                                pendingComplaintAction = ComplaintStartAction.ViewComplaints(occupantDocId)
                                                selectedTab = 1
                                            },
                                            onComplaintClick = { complaint ->
                                                pendingComplaintAction = ComplaintStartAction.OpenComplaint(complaint, occupantDocId)
                                                selectedTab = 1
                                            }
                                        )
                                        Spacer(Modifier.height(20.dp))
                                        MyAssetsSection(
                                            assets = currentAssets,
                                            onViewAll = { overlay = HomeOverlay.Assets }
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
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                    startAction = pendingComplaintAction
                                )
                                2 -> NoticeboardScreen(
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                    initialNotice = pendingNotice,
                                    displayName = name,
                                    flatNo = flatNumber,
                                    recipientType = role
                                )
                                3 -> ProfileContent(
                                    name = name,
                                    email = email,
                                    role = role,
                                    empId = empId,
                                    flatNumber = flatNumber,
                                    occupantFrom = occupantFrom,
                                    isCoordinator = isCoordinator,
                                    onSignOut = { showLogoutConfirmation = true },
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
        }
    }
}

// MARK: - Header

@Composable
private fun HomeTopBar(
    unreadCount: Int,
    onMenuClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyBlue)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Menu, null,
                tint = Color.White,
                modifier = Modifier.size(24.dp).clickable { onMenuClick() }
            )
            Text("Home", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier.size(40.dp).clickable { onNotificationsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.NotificationsNone, null, tint = Color.White, modifier = Modifier.size(24.dp))
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-2).dp)
                            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFE53935))
                            .border(width = 1.5.dp, color = Color.White, shape = RoundedCornerShape(50))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeGreetingCard(
    greeting: String,
    name: String,
    flatNumber: String,
    onProfileClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onProfileClick)
        ) {
            if (greeting.isNotEmpty()) {
                Text(
                    greeting,
                    color = Color(0xFF8892AA),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(3.dp))
            }
            Text(
                name.ifEmpty { "Loading..." },
                color = Color(0xFF1A1A2E),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color(0xFFEEF2FF))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Home, null, tint = NavyBlue, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    "Flat ${flatNumber.ifEmpty { "—" }}",
                    color = NavyBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(NavyBlue)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// MARK: - Drawer

@Composable
private fun HomeDrawerContent(
    name: String,
    flatNumber: String,
    isCoordinator: Boolean = false,
    onNavigateToComplaints: () -> Unit,
    onNavigateToVisitorPass: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onNavigateToNoticeboard: () -> Unit = {},
    onNavigateToDocuments: () -> Unit = {},
    onNavigateToCoordinatorForm: () -> Unit = {},
    onNavigateToFlatVacant: () -> Unit = {},
    onNavigateToAssets: () -> Unit = {},
    onSignOut: () -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight()) {
        // Navy header — matches HomeTopBar height (40dp content + 14dp vertical padding)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyBlue)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .height(40.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(name.ifEmpty { "—" }, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
            DrawerMenuItem(icon = Icons.Outlined.Info, label = "Documents", onClick = onNavigateToDocuments)
            DrawerMenuItem(icon = Icons.Outlined.Inventory2, label = "My Assets", onClick = onNavigateToAssets)
            DrawerMenuItem(icon = Icons.AutoMirrored.Outlined.ExitToApp, label = "Flat Vacant Request", onClick = onNavigateToFlatVacant)
            if (isCoordinator) {
                HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 4.dp))
                DrawerMenuItem(icon = Icons.Outlined.StarBorder, label = "Monthly Form", onClick = onNavigateToCoordinatorForm)
            }
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
private fun NewNoticesSection(notice: Notice?, onViewAll: () -> Unit, onNoticeClick: (Notice) -> Unit = {}) {
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
            modifier = Modifier.fillMaxWidth().clickable { onNoticeClick(notice) },
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
    val days = ((System.currentTimeMillis() - dateMillis) / (1000L * 60 * 60 * 24)).coerceAtLeast(1L)
    return if (days == 1L) "Active since 1 day" else "Active since $days days"
}

/**
 * Dashboard section listing what the occupant currently holds. Mirrors the
 * layout of OngoingComplaintsSection: header with a "View All" affordance,
 * then the rows, with a quiet empty state so the section never looks broken.
 */
@Composable
private fun MyAssetsSection(assets: List<OccupantAsset>, onViewAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Inventory2, null, tint = NavyBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("My Assets", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF1A1A2E))
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
    if (assets.isEmpty()) {
        Text("No assets assigned to you", fontSize = 13.sp, color = Color(0xFF999999))
    } else {
        // Cap the dashboard preview; the full list lives behind "View All".
        assets.take(4).forEach { asset ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable { onViewAll() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE8ECF7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Inventory2, null, tint = NavyBlue, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        asset.assetName.ifEmpty { "Asset" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A2E)
                    )
                    Text("No. ${asset.assetNumber}", fontSize = 12.sp, color = Color(0xFF6B7280))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (assets.size > 4) {
            Text(
                "+${assets.size - 4} more",
                fontSize = 12.sp,
                color = NavyBlue,
                modifier = Modifier.clickable { onViewAll() }
            )
        }
    }
}

@Composable
private fun OngoingComplaintsSection(complaints: List<Complaint>, onViewAll: () -> Unit, onComplaintClick: (Complaint) -> Unit = {}) {
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
                        modifier = Modifier.weight(1f),
                        onClick = { onComplaintClick(complaint) }
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable { onClick() },
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                StatusBadge(status)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, fg) = when (status.uppercase()) {
        "OPEN" -> Color(0xFFFFF7ED) to Color(0xFFD97706)
        "ASSIGNED" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "COMPLETED" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "CLOSED" -> Color(0xFFFFEBEE) to Color(0xFFB71C1C)
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

// MARK: - Form Due Dialog

@Composable
private fun FormDueDialog(
    frequencyMonths: Int,
    onFillForm: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = when (frequencyMonths) {
        1 -> "Monthly Self Audit Required"
        2 -> "Bi-Monthly Self Audit Required"
        3 -> "Quarterly Self Audit Required"
        6 -> "Semi-Annual Self Audit Required"
        12 -> "Annual Self Audit Required"
        else -> "$frequencyMonths-Month Self Audit Required"
    }
    val frequencyLabel = when (frequencyMonths) {
        1 -> "every month"
        else -> "every $frequencyMonths months"
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            // Dismiss X
            Icon(
                Icons.Default.Close, contentDescription = "Dismiss",
                tint = Color(0xFF9E9E9E),
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .clickable { onDismiss() }
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color(0xFFEEF0FA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = NavyBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Please complete your flat maintenance self audit form.",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Required $frequencyLabel",
                    fontSize = 13.sp,
                    color = Color(0xFF9CA3AF),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onFillForm,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Fill Audit Form", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Remind me later",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
        }
    }
}

// MARK: - Revised Amenities Screen

@Composable
private fun RevisedAmenitiesScreen(
    commonAmenities: List<String>,
    roomAmenities: List<String>,
    selectedAmenities: Set<String>,
    isSubmitting: Boolean,
    canSubmit: Boolean,
    onToggleAmenity: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyBlue)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text("Amenity Confirmation", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Please review and reconfirm your amenities", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Outlined.Info, null, tint = NavyBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Our amenity list has been updated. Please select all amenities available in your accommodation to continue.",
                        fontSize = 13.sp,
                        color = Color(0xFF555555)
                    )
                }
            }

            if (commonAmenities.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Common Amenities", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        commonAmenities.forEachIndexed { index, amenity ->
                            if (index > 0) HorizontalDivider(color = Color(0xFFF0F0F0))
                            AmenityCheckRow(
                                amenity = amenity,
                                checked = amenity in selectedAmenities,
                                onToggle = { onToggleAmenity(amenity) }
                            )
                        }
                    }
                }
            }

            if (roomAmenities.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Room Amenities", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        roomAmenities.forEachIndexed { index, amenity ->
                            if (index > 0) HorizontalDivider(color = Color(0xFFF0F0F0))
                            AmenityCheckRow(
                                amenity = amenity,
                                checked = amenity in selectedAmenities,
                                onToggle = { onToggleAmenity(amenity) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NavyBlue)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Confirm Amenities", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AmenityCheckRow(amenity: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = NavyBlue)
        )
        Spacer(Modifier.width(8.dp))
        Text(amenity, fontSize = 14.sp, color = Color(0xFF1A1A2E))
    }
}

// MARK: - Notifications Screen

@Composable
private fun NotificationsScreen(
    notifications: List<AppNotification>,
    onMarkRead: (String) -> Unit,
    onOpenQuestion:      (String) -> Unit,
    onOpenComplaint:     () -> Unit,
    onOpenVisitorPass:   () -> Unit,
    onOpenVacantRequest: () -> Unit,
    onBack: () -> Unit
) {
    val dateFormatter = remember { java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // Header
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.NavigateNext, null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onBack() }
                )
                Spacer(Modifier.width(12.dp))
                Text("Notifications", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.NotificationsNone, null, tint = Color(0xFFBBBBBB), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No notifications yet", color = Color(0xFF9E9E9E), fontSize = 15.sp)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                notifications.forEach { notification ->
                    // Classify the notification by its `source` (mapped from
                    // the Firestore `source` field) so the tap can deep-link
                    // to the right detail screen. Old notifications without
                    // a deep-link source just behave as before.
                    val source = notification.type
                    val isQuestion =
                        source == "question_notification" && notification.qnId.isNotEmpty()
                    val isComplaint     = source.startsWith("complaint_")
                    val isVisitorPass   = source.startsWith("visitor_pass_")
                    val isVacantRequest = source.startsWith("vacant_request_")
                    val isActionable    = isQuestion || isComplaint || isVisitorPass || isVacantRequest

                    NotificationItem(
                        notification = notification,
                        dateFormatter = dateFormatter,
                        isActionable = isActionable,
                        onClick = {
                            if (!notification.isRead) onMarkRead(notification.id)
                            when {
                                isQuestion      -> onOpenQuestion(notification.qnId)
                                isComplaint     -> onOpenComplaint()
                                isVisitorPass   -> onOpenVisitorPass()
                                isVacantRequest -> onOpenVacantRequest()
                            }
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: AppNotification,
    dateFormatter: java.text.SimpleDateFormat,
    isActionable: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFFEEF2FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (notification.isRead) Color(0xFFF5F5F5) else NavyBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.NotificationsNone, null,
                    tint = if (notification.isRead) Color(0xFF9E9E9E) else NavyBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    fontSize = 14.sp,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                    color = Color(0xFF1A1A2E)
                )
                if (notification.message.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = notification.message,
                        fontSize = 13.sp,
                        color = Color(0xFF555555)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (notification.createdAt > 0L)
                            dateFormatter.format(java.util.Date(notification.createdAt))
                        else "",
                        fontSize = 11.sp,
                        color = Color(0xFF9E9E9E)
                    )
                    if (isActionable) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Tap to respond →",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NavyBlue
                        )
                    }
                }
            }
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NavyBlue)
                )
            }
        }
    }
}

