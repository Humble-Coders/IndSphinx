package com.humblesolutions.indsphinx.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.indsphinx.model.ComplaintTemplate
import com.humblesolutions.indsphinx.viewmodel.ComplaintsUiState
import com.humblesolutions.indsphinx.viewmodel.ComplaintsViewModel

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
                onAddComplaint = { viewModel.onAddComplaintClick() }
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
                onSubmit = { problem, description, priority ->
                    viewModel.submitComplaint(
                        problem = problem,
                        description = description,
                        priority = priority,
                        occupantName = occupantName,
                        occupantEmail = occupantEmail,
                        occupantId = occupantDocId,
                        flatNumber = flatNumber,
                        flatId = flatId
                    )
                }
            )
        }

        is ComplaintsUiState.Submitting -> {
            SubmitComplaintScreen(
                template = ComplaintTemplate(),
                isSubmitting = true,
                onBack = {},
                onSubmit = { _, _, _ -> }
            )
        }

        is ComplaintsUiState.Success -> {
            ComplaintsLandingScreen(
                onMenuClick = onMenuClick,
                onAddComplaint = { viewModel.onAddComplaintClick() }
            )
            SuccessDialog(onDismiss = { viewModel.dismissSuccess() })
        }

        is ComplaintsUiState.Error -> {
            ComplaintsLandingScreen(
                onMenuClick = onMenuClick,
                onAddComplaint = { viewModel.onAddComplaintClick() }
            )
            ErrorDialog(
                message = state.message,
                onDismiss = { viewModel.dismissError() }
            )
        }
    }
}

// ─── Landing ──────────────────────────────────────────────────────────────────

@Composable
private fun ComplaintsLandingScreen(onMenuClick: () -> Unit, onAddComplaint: () -> Unit) {
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
                onClick = {}
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
            items(templates) { template ->
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

// ─── Submit Complaint Form ────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubmitComplaintScreen(
    template: ComplaintTemplate,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSubmit: (problem: String, description: String, priority: String) -> Unit
) {
    var selectedProblem by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("") }
    val visuals = categoryVisuals(template.category)
    val canSubmit = selectedProblem.isNotBlank() && selectedPriority.isNotBlank() && !isSubmitting

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
                                        val isEmergency = p == "Emergency"
                                        val baseBg = if (isEmergency) Color(0xFFFFEBEE) else Color(0xFFF5F5F5)
                                        val baseTint = if (isEmergency) Color(0xFFE53935) else Color(0xFF444444)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    when {
                                                        selected && isEmergency -> Color(0xFFE53935)
                                                        selected -> NavyBlue
                                                        else -> baseBg
                                                    }
                                                )
                                                .border(
                                                    1.dp,
                                                    when {
                                                        selected && isEmergency -> Color(0xFFE53935)
                                                        selected -> NavyBlue
                                                        isEmergency -> Color(0xFFE53935)
                                                        else -> Color.Transparent
                                                    },
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable { selectedPriority = if (selected) "" else p }
                                                .padding(vertical = 14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                p,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = when {
                                                    selected -> Color.White
                                                    else -> baseTint
                                                }
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
                                modifier = Modifier.weight(1f)
                            )
                            MediaButton(
                                icon = Icons.Outlined.PlayCircle,
                                label = "Add Video",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // Submit button
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Button(
                    onClick = { onSubmit(selectedProblem, description, selectedPriority) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = canSubmit,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyBlue,
                        disabledContainerColor = Color(0xFFCCCCCC)
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Submit Complaint", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaButton(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF5F5F5))
            .clickable {}
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
