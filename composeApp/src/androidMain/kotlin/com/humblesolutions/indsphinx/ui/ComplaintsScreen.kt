package com.humblesolutions.indsphinx.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Person2
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Plumbing
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.humblesolutions.indsphinx.model.Complaint
import com.humblesolutions.indsphinx.model.ComplaintTemplate
import com.humblesolutions.indsphinx.viewmodel.ComplaintsUiState
import com.humblesolutions.indsphinx.viewmodel.ComplaintsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.humblesolutions.indsphinx.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val NavyBlue = Color(0xFF1E2D6B)
private val BgGray = Color(0xFFF2F4F8)

// ─── Category visuals ─────────────────────────────────────────────────────────

private data class CategoryVisuals(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val subtitle: String
)

private fun categoryVisuals(category: String): CategoryVisuals = when {
    category.contains("AC", ignoreCase = true) ->
        CategoryVisuals(Icons.Outlined.AcUnit, Color(0xFFE3F0FF), Color(0xFF2196F3), "Cooling issue")
    category.contains("Electrical", ignoreCase = true) ->
        CategoryVisuals(Icons.Outlined.FlashOn, Color(0xFFFFF3E0), Color(0xFFFF9800), "Power problem")
    category.contains("Kitchen", ignoreCase = true) ->
        CategoryVisuals(Icons.Outlined.Kitchen, Color(0xFFE8F5E9), Color(0xFF4CAF50), "Kitchen repair")
    category.contains("Carpenter", ignoreCase = true) || category.contains("Furniture", ignoreCase = true) ->
        CategoryVisuals(Icons.Outlined.Handyman, Color(0xFFFBE9E7), Color(0xFF795548), "Furniture repair")
    category.contains("Fire", ignoreCase = true) ->
        CategoryVisuals(Icons.Outlined.LocalFireDepartment, Color(0xFFFFEBEE), Color(0xFFF44336), "Fire safety")
    category.contains("Geyser", ignoreCase = true) ->
        CategoryVisuals(Icons.Outlined.WaterDrop, Color(0xFFE0F7FA), Color(0xFF00ACC1), "Hot water issue")
    category.contains("Mason", ignoreCase = true) ->
        CategoryVisuals(Icons.Outlined.Construction, Color(0xFFECEFF1), Color(0xFF607D8B), "Wall & floor")
    category.contains("Paint", ignoreCase = true) ->
        CategoryVisuals(Icons.Outlined.Brush, Color(0xFFF3E5F5), Color(0xFF9C27B0), "Painting work")
    category.contains("Plumbing", ignoreCase = true) ->
        CategoryVisuals(Icons.Outlined.Plumbing, Color(0xFFE3F2FD), Color(0xFF1565C0), "Water & drainage")
    category.contains("Welder", ignoreCase = true) ->
        CategoryVisuals(Icons.Outlined.Build, Color(0xFFFFF8E1), Color(0xFFFF8F00), "Metal work")
    category.contains("Pest", ignoreCase = true) || category.contains("Hygiene", ignoreCase = true) ->
        CategoryVisuals(Icons.Outlined.BugReport, Color(0xFFE8F5E9), Color(0xFF2E7D32), "Cleaning & pests")
    else ->
        CategoryVisuals(Icons.Outlined.Build, Color(0xFFF5F5F5), Color(0xFF9E9E9E), "Maintenance")
}

// ─── Entry point ──────────────────────────────────────────────────────────────

@Composable
fun ComplaintsScreen(
    occupantName: String,
    occupantEmail: String,
    occupantDocId: String,
    flatNumber: String,
    flatId: String,
    onMenuClick: () -> Unit
) {
    val viewModel: ComplaintsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is ComplaintsUiState.Landing -> {
            ComplaintsLandingScreen(
                onMenuClick = onMenuClick,
                onAddComplaint = { viewModel.onAddComplaintClick() },
                onViewComplaints = { viewModel.onViewComplaintsClick(occupantDocId) }
            )
        }

        is ComplaintsUiState.LoadingTemplates -> {
            ComplaintsLoadingScreen()
        }

        is ComplaintsUiState.SelectCategory -> {
            BackHandler { viewModel.onBackFromCategory() }
            CategorySelectionScreen(
                templates = state.templates,
                onBack = { viewModel.onBackFromCategory() },
                onSelectCategory = { viewModel.onCategorySelected(it) }
            )
        }

        is ComplaintsUiState.SubmitForm -> {
            BackHandler { viewModel.onBackFromForm() }
            SubmitComplaintScreen(
                template = state.selectedTemplate,
                isSubmitting = false,
                onBack = { viewModel.onBackFromForm() },
                onSubmit = { problem, description, priority, mediaUris ->
                    viewModel.submitComplaint(
                        problem = problem,
                        description = description,
                        priority = priority,
                        occupantName = occupantName,
                        occupantEmail = occupantEmail,
                        occupantId = occupantDocId,
                        flatNumber = flatNumber,
                        flatId = flatId,
                        mediaUris = mediaUris
                    )
                }
            )
        }

        is ComplaintsUiState.Submitting -> {
            SubmitComplaintScreen(
                template = ComplaintTemplate(),
                isSubmitting = true,
                onBack = {},
                onSubmit = { _, _, _, _ -> }
            )
        }

        is ComplaintsUiState.Success -> {
            ComplaintsLandingScreen(
                onMenuClick = onMenuClick,
                onAddComplaint = { viewModel.onAddComplaintClick() },
                onViewComplaints = { viewModel.onViewComplaintsClick(occupantDocId) }
            )
            SuccessDialog(onDismiss = { viewModel.dismissSuccess() })
        }

        is ComplaintsUiState.Error -> {
            ComplaintsLandingScreen(
                onMenuClick = onMenuClick,
                onAddComplaint = { viewModel.onAddComplaintClick() },
                onViewComplaints = { viewModel.onViewComplaintsClick(occupantDocId) }
            )
            ErrorDialog(
                message = state.message,
                onDismiss = { viewModel.dismissError() }
            )
        }

        is ComplaintsUiState.LoadingComplaints -> {
            ComplaintsLoadingScreen()
        }

        is ComplaintsUiState.ViewComplaints -> {
            BackHandler { viewModel.onBackFromComplaints() }
            ViewComplaintsScreen(
                complaints = state.complaints,
                onBack = { viewModel.onBackFromComplaints() },
                onComplaintClick = { viewModel.onComplaintSelected(it) }
            )
        }

        is ComplaintsUiState.ComplaintDetail -> {
            BackHandler { viewModel.onBackFromDetail() }
            ComplaintDetailScreen(
                complaint = state.complaint,
                occupantId = occupantDocId,
                onBack = { viewModel.onBackFromDetail() },
                onClose = { id, oId -> viewModel.closeComplaint(id, oId) }
            )
        }
    }
}

// ─── Landing ──────────────────────────────────────────────────────────────────

@Composable
private fun ComplaintsLandingScreen(
    onMenuClick: () -> Unit,
    onAddComplaint: () -> Unit,
    onViewComplaints: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray)
    ) {
        // App bar
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
                    modifier = Modifier.size(24.dp).clickable { onMenuClick() }
                )
                Text(
                    "Complaints",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(Icons.Outlined.Person, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // View Complaints card
            ComplaintActionCard(
                icon = Icons.Outlined.Description,
                iconBg = Color(0xFFEEF2FF),
                iconTint = NavyBlue,
                title = "View Complaints",
                subtitle = "Track and manage your submitted complaints",
                onClick = onViewComplaints
            )

            // Add Complaint card
            ComplaintActionCard(
                icon = Icons.Outlined.CheckCircle,
                iconBg = NavyBlue,
                iconTint = Color.White,
                title = "Add Complaint",
                subtitle = "Submit a new maintenance or service request",
                onClick = onAddComplaint
            )
        }
    }

    // FAB
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .padding(end = 16.dp, bottom = 24.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(NavyBlue)
                .clickable { onAddComplaint() },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun ComplaintActionCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 13.sp, color = Color(0xFF888888))
            }
        }
    }
}

// ─── Loading ──────────────────────────────────────────────────────────────────

@Composable
private fun ComplaintsLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = NavyBlue)
    }
}

// ─── Category Selection ───────────────────────────────────────────────────────

@Composable
private fun CategorySelectionScreen(
    templates: List<ComplaintTemplate>,
    onBack: () -> Unit,
    onSelectCategory: (ComplaintTemplate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray)
    ) {
        // App bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack, null,
                        tint = Color(0xFF1A1A2E),
                        modifier = Modifier.size(24.dp).clickable { onBack() }
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Select Issue Category",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A2E)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "    Choose the category that best describes your complaint",
                    fontSize = 13.sp,
                    color = Color(0xFF888888)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
            gridItems(templates) { template ->
                val visuals = categoryVisuals(template.category)
                CategoryCard(
                    category = template.category,
                    subtitle = visuals.subtitle,
                    icon = visuals.icon,
                    iconBg = visuals.iconBg,
                    iconTint = visuals.iconTint,
                    onClick = { onSelectCategory(template) }
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                category,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 12.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Media helpers ────────────────────────────────────────────────────────────

private data class MediaItem(val uri: Uri, val isVideo: Boolean)

private fun createTempUri(context: android.content.Context, isVideo: Boolean): Uri {
    val ext = if (isVideo) ".mp4" else ".jpg"
    val prefix = if (isVideo) "video_" else "photo_"
    val file = File.createTempFile(prefix, ext, context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun hasCameraPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

@Composable
private fun MediaThumbnail(item: MediaItem, onTap: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(item.uri) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(item.uri) {
        withContext(Dispatchers.IO) {
            bitmap = if (item.isVideo) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, item.uri)
                    val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    retriever.release()
                    frame
                } catch (e: Exception) { null }
            } else {
                context.contentResolver.openInputStream(item.uri)?.use { BitmapFactory.decodeStream(it) }
            }
        }
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onTap() }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item.isVideo) Icons.Outlined.VideoLibrary else Icons.Outlined.CameraAlt,
                    null,
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        // Play button overlay for videos
        if (item.isVideo) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x44000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PlayCircle, null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        // Remove button — separate clickable so it doesn't trigger onTap
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0x99000000))
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun ImagePreviewDialog(uri: Uri, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            } else {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MediaSourceDialog(title: String, onCamera: () -> Unit, onGallery: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A2E),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                HorizontalDivider(color = Color(0xFFF0F0F0))
                TextButton(
                    onClick = { onCamera(); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.CameraAlt, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Camera", fontSize = 14.sp, color = Color(0xFF1A1A2E))
                    Spacer(Modifier.weight(1f))
                }
                TextButton(
                    onClick = { onGallery(); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.PlayCircle, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Gallery", fontSize = 14.sp, color = Color(0xFF1A1A2E))
                    Spacer(Modifier.weight(1f))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", fontSize = 14.sp, color = Color(0xFF888888))
                }
            }
        }
    }
}

// ─── Submit Complaint Form ────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubmitComplaintScreen(
    template: ComplaintTemplate,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSubmit: (problem: String, description: String, priority: String, mediaUris: List<Uri>) -> Unit
) {
    var selectedProblem by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("") }
    val visuals = categoryVisuals(template.category)
    val canSubmit = selectedProblem.isNotBlank() && selectedPriority.isNotBlank() && !isSubmitting

    // Media
    val context = LocalContext.current
    var mediaItems by remember { mutableStateOf(listOf<MediaItem>()) }
    var previewImageUri by remember { mutableStateOf<Uri?>(null) }
    var playVideoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var showVideoSourceDialog by remember { mutableStateOf(false) }
    var pendingCameraAction by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> mediaItems = mediaItems + uris.map { MediaItem(it, false) } }

    val galleryVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> mediaItems = mediaItems + uris.map { MediaItem(it, true) } }

    val cameraPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) pendingCameraUri?.let { mediaItems = mediaItems + MediaItem(it, false) } }

    val cameraVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success -> if (success) pendingCameraUri?.let { mediaItems = mediaItems + MediaItem(it, true) } }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempUri(context, pendingCameraAction == "video")
            pendingCameraUri = uri
            if (pendingCameraAction == "video") cameraVideoLauncher.launch(uri)
            else cameraPhotoLauncher.launch(uri)
        }
        pendingCameraAction = ""
    }

    fun launchCamera(isVideo: Boolean) {
        if (hasCameraPermission(context)) {
            val uri = createTempUri(context, isVideo)
            pendingCameraUri = uri
            if (isVideo) cameraVideoLauncher.launch(uri) else cameraPhotoLauncher.launch(uri)
        } else {
            pendingCameraAction = if (isVideo) "video" else "photo"
            cameraPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(BgGray)) {
            // App bar
            Box(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack, null,
                        tint = Color(0xFF1A1A2E),
                        modifier = Modifier.size(24.dp).clickable(enabled = !isSubmitting) { onBack() }
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Submit Complaint",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A2E)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Selected category card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Selected Issue Type", fontSize = 12.sp, color = Color(0xFF888888))
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(visuals.iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(visuals.icon, null, tint = visuals.iconTint, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(template.category, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                        }
                    }
                }

                // Problem selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select Problem", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            template.problems.forEach { problem ->
                                val selected = selectedProblem == problem
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (selected) NavyBlue else Color.White)
                                        .border(1.dp, if (selected) NavyBlue else Color(0xFFDDDDDD), RoundedCornerShape(20.dp))
                                        .clickable { selectedProblem = if (selected) "" else problem }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        problem,
                                        fontSize = 13.sp,
                                        color = if (selected) Color.White else Color(0xFF444444)
                                    )
                                }
                            }
                        }
                    }
                }

                // Description
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Description", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text("Describe your complaint in detail...", color = Color(0xFFAAAAAA), fontSize = 13.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFEEEEEE),
                                focusedBorderColor = NavyBlue
                            )
                        )
                    }
                }

                // Priority
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Priority Level", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                        Spacer(Modifier.height(12.dp))
                        val priorities = listOf("Low", "Medium", "High", "Emergency")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (row in priorities.chunked(2)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    row.forEach { p ->
                                        val selected = selectedPriority == p
                                        val activeColor = when (p) {
                                            "Low" -> Color(0xFF4CAF50)
                                            "Medium" -> Color(0xFFFFC107)
                                            "High" -> Color(0xFFFF9800)
                                            "Emergency" -> Color(0xFFF44336)
                                            else -> NavyBlue
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (selected) activeColor else Color(0xFFF5F5F5))
                                                .clickable { selectedPriority = if (selected) "" else p }
                                                .padding(vertical = 14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                p,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (selected) Color.White else Color(0xFF666666)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Media (optional)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add Media (Optional)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MediaButton(
                                icon = Icons.Outlined.CameraAlt,
                                label = "Add Photo",
                                modifier = Modifier.weight(1f),
                                onClick = { showPhotoSourceDialog = true }
                            )
                            MediaButton(
                                icon = Icons.Outlined.PlayCircle,
                                label = "Add Video",
                                modifier = Modifier.weight(1f),
                                onClick = { showVideoSourceDialog = true }
                            )
                        }
                        if (mediaItems.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                mediaItems.forEachIndexed { index, item ->
                                    MediaThumbnail(
                                        item = item,
                                        onTap = {
                                            if (item.isVideo) playVideoUri = item.uri
                                            else previewImageUri = item.uri
                                        },
                                        onRemove = {
                                            mediaItems = mediaItems.toMutableList().also { it.removeAt(index) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // Submit button
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Button(
                    onClick = { onSubmit(selectedProblem, description, selectedPriority, mediaItems.map { it.uri }) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = canSubmit,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyBlue,
                        disabledContainerColor = Color(0xFFCCCCCC)
                    )
                ) {
                    Text("Submit Complaint", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Lottie submitting overlay
        if (isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loader))
                val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(200.dp)
                )
            }
        }

        // Image full-screen preview
        previewImageUri?.let { uri ->
            ImagePreviewDialog(uri = uri, onDismiss = { previewImageUri = null })
        }

        // Video player
        playVideoUri?.let { uri ->
            ExoPlayerDialog(url = uri.toString(), onDismiss = { playVideoUri = null })
        }

        // Source selection dialogs
        if (showPhotoSourceDialog) {
            MediaSourceDialog(
                title = "Add Photo",
                onCamera = { launchCamera(false) },
                onGallery = {
                    galleryPhotoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onDismiss = { showPhotoSourceDialog = false }
            )
        }
        if (showVideoSourceDialog) {
            MediaSourceDialog(
                title = "Add Video",
                onCamera = { launchCamera(true) },
                onGallery = {
                    galleryVideoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onDismiss = { showVideoSourceDialog = false }
            )
        }
    }
}

@Composable
private fun MediaButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF5F5F5))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = Color(0xFF666666), modifier = Modifier.size(18.dp))
            Text(label, fontSize = 13.sp, color = Color(0xFF444444))
        }
    }
}

// ─── Dialogs ──────────────────────────────────────────────────────────────────

@Composable
private fun SuccessDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle, null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "Complaint Submitted!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your complaint has been registered successfully. You'll be notified once it's assigned to staff.",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                ) {
                    Text("Done", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Submission Failed", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                Spacer(Modifier.height(8.dp))
                Text(message, fontSize = 14.sp, color = Color(0xFF666666), textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("OK", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "—"
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}

@Composable
private fun StatusChip(status: String) {
    val (bg, fg) = when (status.uppercase()) {
        "OPEN" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "IN_PROGRESS" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "COMPLETED" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "CLOSED" -> Color(0xFFF5F5F5) to Color(0xFF757575)
        else -> Color(0xFFF5F5F5) to Color(0xFF757575)
    }
    val label = when (status.uppercase()) {
        "OPEN" -> "Open"
        "IN_PROGRESS" -> "In Progress"
        "COMPLETED" -> "Completed"
        "CLOSED" -> "Closed"
        else -> status
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}

@Composable
private fun PriorityBadge(priority: String) {
    val (bg, fg) = when (priority.lowercase()) {
        "low" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "medium" -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        "high" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "emergency" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        else -> Color(0xFFF5F5F5) to Color(0xFF757575)
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(priority.replaceFirstChar { it.uppercase() }, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}

// ─── View Complaints ──────────────────────────────────────────────────────────

@Composable
private fun ViewComplaintsScreen(
    complaints: List<Complaint>,
    onBack: () -> Unit,
    onComplaintClick: (Complaint) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val ongoing = complaints.filter { it.status.uppercase() != "CLOSED" }
    val closed = complaints.filter { it.status.uppercase() == "CLOSED" }
    val currentList = if (selectedTab == 0) ongoing else closed

    Column(modifier = Modifier.fillMaxSize().background(BgGray)) {
        Box(modifier = Modifier.fillMaxWidth().background(NavyBlue)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack, null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp).clickable { onBack() }
                )
                Spacer(Modifier.width(12.dp))
                Text("My Complaints", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = NavyBlue,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NavyBlue
                )
            }
        ) {
            listOf("Ongoing", "Closed").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        if (currentList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (selectedTab == 0) "No ongoing complaints" else "No closed complaints",
                    color = Color(0xFF888888),
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(12.dp)) }
                items(currentList) { complaint ->
                    ComplaintListCard(complaint = complaint, onClick = { onComplaintClick(complaint) })
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ComplaintListCard(complaint: Complaint, onClick: () -> Unit) {
    val visuals = categoryVisuals(complaint.category)
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(visuals.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(visuals.icon, null, tint = visuals.iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        complaint.category,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A2E),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusChip(status = complaint.status)
                }
                Spacer(Modifier.height(4.dp))
                Text(complaint.problem, fontSize = 13.sp, color = Color(0xFF555555), maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDate(complaint.date), fontSize = 12.sp, color = Color(0xFF888888))
                    Spacer(Modifier.width(8.dp))
                    PriorityBadge(priority = complaint.priority)
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(18.dp))
        }
    }
}

// ─── Complaint Detail ─────────────────────────────────────────────────────────

@Composable
private fun ComplaintDetailScreen(
    complaint: Complaint,
    occupantId: String,
    onBack: () -> Unit,
    onClose: (complaintId: String, occupantId: String) -> Unit
) {
    val visuals = categoryVisuals(complaint.category)
    var showCloseConfirm by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var playVideoUrl by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(BgGray)) {
            // App bar
            Box(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack, null,
                        tint = Color(0xFF1A1A2E),
                        modifier = Modifier.size(24.dp).clickable { onBack() }
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Complaint Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A2E)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(0.dp))

                // Status + category header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(visuals.iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(visuals.icon, null, tint = visuals.iconTint, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(complaint.category, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                            Spacer(Modifier.height(4.dp))
                            Text(complaint.problem, fontSize = 13.sp, color = Color(0xFF666666))
                        }
                        StatusChip(status = complaint.status)
                    }
                }

                // Details card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Details", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF888888))
                        DetailRow(label = "Submitted", value = formatDate(complaint.date))
                        DetailRow(label = "Priority") { PriorityBadge(priority = complaint.priority) }
                        if (complaint.description.isNotBlank()) {
                            DetailRow(label = "Description", value = complaint.description, multiLine = true)
                        }
                        if (complaint.resolveDate != 0L) {
                            DetailRow(label = "Resolved On", value = formatDate(complaint.resolveDate))
                        }
                    }
                }

                // Worker card — shown when ASSIGNED or COMPLETED
                val showWorker = complaint.workerName.isNotBlank() &&
                    (complaint.status.uppercase() == "ASSIGNED" || complaint.status.uppercase() == "COMPLETED")
                if (showWorker) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Person, null, tint = NavyBlue, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Assigned Worker", fontSize = 12.sp, color = Color(0xFF888888))
                                Text(complaint.workerName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                            }
                        }
                    }
                }

                // Worker remarks + media — shown only when COMPLETED
                if (complaint.status.uppercase() == "COMPLETED") {
                    if (complaint.workerRemarks.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FFF8)),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Worker Remarks", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                                Spacer(Modifier.height(8.dp))
                                Text(complaint.workerRemarks, fontSize = 14.sp, color = Color(0xFF1A1A2E))
                            }
                        }
                    }
                    if (complaint.workerMedia.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Work Done (${complaint.workerMedia.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF888888))
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    complaint.workerMedia.forEach { url ->
                                        val isVideo = url.contains(".mp4", ignoreCase = true)
                                        RemoteMediaThumbnail(
                                            url = url,
                                            isVideo = isVideo,
                                            onClick = {
                                                if (isVideo) playVideoUrl = url
                                                else previewImageUrl = url
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Occupant media card
                if (complaint.mediaUrls.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Attached Media (${complaint.mediaUrls.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF888888))
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                complaint.mediaUrls.forEach { url ->
                                    val isVideo = url.contains(".mp4", ignoreCase = true)
                                    RemoteMediaThumbnail(
                                        url = url,
                                        isVideo = isVideo,
                                        onClick = {
                                            if (isVideo) playVideoUrl = url
                                            else previewImageUrl = url
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // Mark as Closed button — only shown when COMPLETED
            if (complaint.status.uppercase() == "COMPLETED") {
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                    Button(
                        onClick = { showCloseConfirm = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("Mark as Closed", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (showCloseConfirm) {
            AlertDialog(
                onDismissRequest = { showCloseConfirm = false },
                title = { Text("Mark as Closed") },
                text = { Text("Are you sure this complaint has been resolved to your satisfaction?") },
                confirmButton = {
                    TextButton(onClick = {
                        showCloseConfirm = false
                        onClose(complaint.id, occupantId)
                    }) {
                        Text("Confirm", color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCloseConfirm = false }) {
                        Text("Cancel", color = Color(0xFF666666))
                    }
                }
            )
        }

        previewImageUrl?.let { url ->
            UrlImagePreviewDialog(url = url, onDismiss = { previewImageUrl = null })
        }

        playVideoUrl?.let { url ->
            ExoPlayerDialog(url = url, onDismiss = { playVideoUrl = null })
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String = "",
    multiLine: Boolean = false,
    content: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = if (multiLine) Alignment.Top else Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFF888888), modifier = Modifier.width(100.dp))
        if (content != null) {
            content()
        } else {
            Text(
                value,
                fontSize = 14.sp,
                color = Color(0xFF1A1A2E),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun UrlImagePreviewDialog(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                contentScale = ContentScale.Fit
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ExoPlayerDialog(url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun RemoteMediaThumbnail(url: String, isVideo: Boolean, onClick: () -> Unit) {
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) {
        if (isVideo) {
            withContext(Dispatchers.IO) {
                bitmap = try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(url, HashMap())
                    val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    retriever.release()
                    frame
                } catch (_: Exception) { null }
            }
        }
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFDDDDDD))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isVideo) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x55000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.PlayCircle, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
