package com.humblesolutions.indsphinx.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.indsphinx.model.FlatVacantRequest
import com.humblesolutions.indsphinx.viewmodel.FlatVacantRequestUiState
import com.humblesolutions.indsphinx.viewmodel.FlatVacantRequestViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NavyBlue = Color(0xFF1E2D6B)
private val BackgroundGray = Color(0xFFF2F4F8)

@Composable
fun FlatVacantRequestScreen(
    occupantId: String,
    occupantName: String,
    flatId: String,
    flatNumber: String,
    onBack: () -> Unit,
) {
    val viewModel: FlatVacantRequestViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(occupantId) { viewModel.start(occupantId) }

    val flatAssigned = flatId.isNotBlank() && flatNumber.isNotBlank()

    when (val state = uiState) {
        is FlatVacantRequestUiState.Loading -> {
            Column(Modifier.fillMaxSize().background(BackgroundGray)) {
                Header(title = "Flat Vacant Request", onBack = onBack)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyBlue)
                }
            }
        }
        is FlatVacantRequestUiState.Loaded -> {
            Column(Modifier.fillMaxSize().background(BackgroundGray)) {
                Header(title = "Flat Vacant Request", onBack = onBack)
                ListContent(
                    requests = state.requests,
                    flatAssigned = flatAssigned,
                    flatNumber = flatNumber,
                    hasPending = state.requests.any { it.status.equals("PENDING", true) },
                    onSubmitTapped = { viewModel.onSubmitTapped() },
                    onRequestSelected = { viewModel.onRequestSelected(it) },
                )
            }
        }
        is FlatVacantRequestUiState.SubmitForm -> {
            BackHandler { viewModel.onBackFromForm() }
            Column(Modifier.fillMaxSize().background(BackgroundGray)) {
                Header(title = "New Vacant Request", onBack = { viewModel.onBackFromForm() })
                FormContent(
                    flatNumber = flatNumber,
                    isSubmitting = false,
                    onSubmit = { reason ->
                        viewModel.submit(occupantId, occupantName, flatId, flatNumber, reason)
                    },
                )
            }
        }
        is FlatVacantRequestUiState.Submitting -> {
            Column(Modifier.fillMaxSize().background(BackgroundGray)) {
                Header(title = "New Vacant Request", onBack = {})
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyBlue)
                }
            }
        }
        is FlatVacantRequestUiState.Detail -> {
            BackHandler { viewModel.onBackFromDetail() }
            Column(Modifier.fillMaxSize().background(BackgroundGray)) {
                Header(title = "Request Details", onBack = { viewModel.onBackFromDetail() })
                DetailContent(request = state.request)
            }
        }
        is FlatVacantRequestUiState.Error -> {
            Column(Modifier.fillMaxSize().background(BackgroundGray)) {
                Header(title = "Flat Vacant Request", onBack = onBack)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            state.message,
                            color = Color(0xFF999999),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("OK", color = NavyBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack, null,
                tint = NavyBlue,
                modifier = Modifier.size(24.dp).clickable { onBack() },
            )
            Spacer(Modifier.width(16.dp))
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E),
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.AutoMirrored.Outlined.ExitToApp, null, tint = NavyBlue, modifier = Modifier.size(22.dp))
        }
    }
    HorizontalDivider(color = Color(0xFFF0F0F0))
}

// MARK: - List

@Composable
private fun ListContent(
    requests: List<FlatVacantRequest>,
    flatAssigned: Boolean,
    flatNumber: String,
    hasPending: Boolean,
    onSubmitTapped: () -> Unit,
    onRequestSelected: (FlatVacantRequest) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            "Request to vacate your assigned flat. Admin will review and respond.",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))

        if (!flatAssigned) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Info, null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "No flat is currently assigned to you. Please contact admin.",
                        fontSize = 13.sp,
                        color = Color(0xFF92400E),
                    )
                }
            }
        } else {
            Button(
                onClick = onSubmitTapped,
                enabled = !hasPending,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyBlue,
                    disabledContainerColor = Color(0xFFBBBBBB),
                ),
            ) {
                Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("New Vacant Request", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            if (hasPending) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "You already have a pending request. Wait for admin response before submitting another.",
                    fontSize = 12.sp,
                    color = Color(0xFFD97706),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Home, null, tint = Color(0xFF666666), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Assigned flat: $flatNumber", fontSize = 13.sp, color = Color(0xFF666666))
            }
        }

        Spacer(Modifier.height(24.dp))

        if (requests.isNotEmpty()) {
            Text(
                "Your Requests",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E),
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                requests.forEach { req ->
                    RequestCard(request = req, onClick = { onRequestSelected(req) })
                }
            }
        } else if (flatAssigned) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Text(
                    "No previous requests",
                    fontSize = 14.sp,
                    color = Color(0xFF999999),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RequestCard(request: FlatVacantRequest, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarToday, null, tint = Color(0xFF999999), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatRequestDate(request.dateCreated), fontSize = 12.sp, color = Color(0xFF999999))
                }
                StatusBadge(request.status)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Flat ${request.flatNumber.ifEmpty { "—" }}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                request.reason,
                fontSize = 13.sp,
                color = Color(0xFF555555),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, fg, label) = when (status.uppercase()) {
        "PENDING" -> Triple(Color(0xFFFFF7ED), Color(0xFFD97706), "Pending")
        "ACCEPTED" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Accepted")
        "REJECTED" -> Triple(Color(0xFFFFEBEE), Color(0xFFB71C1C), "Rejected")
        else -> Triple(Color(0xFFF5F5F5), Color(0xFF616161), status)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// MARK: - Form

@Composable
private fun FormContent(
    flatNumber: String,
    isSubmitting: Boolean,
    onSubmit: (String) -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf("") }
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    val trimmed = reason.trim()
    val isValid = trimmed.length >= 5

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirm Request", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
            text = {
                Text(
                    "You are requesting to vacate Flat $flatNumber. Continue?",
                    fontSize = 14.sp,
                    color = Color(0xFF555555),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onSubmit(trimmed)
                }) {
                    Text("Submit", color = NavyBlue, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel", color = Color(0xFF555555))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Home, null, tint = NavyBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Flat", fontSize = 12.sp, color = Color(0xFF999999))
                        Text(
                            flatNumber.ifEmpty { "—" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1A1A2E),
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Description, null, tint = NavyBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Reason for Vacancy",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color(0xFF1A1A2E),
                        )
                    }
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { if (it.length <= 500) reason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Describe why you want to vacate the flat",
                                fontSize = 14.sp,
                                color = Color(0xFFAAAAAA),
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = false,
                        minLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = NavyBlue,
                            unfocusedContainerColor = Color(0xFFF2F4F8),
                            focusedContainerColor = Color(0xFFF2F4F8),
                        ),
                    )
                    Text(
                        "${reason.length} / 500",
                        fontSize = 11.sp,
                        color = Color(0xFF999999),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { if (isValid && !isSubmitting) showConfirm = true },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isValid) NavyBlue else Color(0xFFBBBBBB),
            ),
            enabled = isValid && !isSubmitting,
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Submit Request", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// MARK: - Detail

@Composable
private fun DetailContent(request: FlatVacantRequest) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarToday, null, tint = Color(0xFF999999), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(formatRequestDate(request.dateCreated), fontSize = 13.sp, color = Color(0xFF999999))
                    }
                    StatusBadge(request.status)
                }
                Spacer(Modifier.height(14.dp))
                Text("Flat", fontSize = 12.sp, color = Color(0xFF999999))
                Spacer(Modifier.height(2.dp))
                Text(
                    request.flatNumber.ifEmpty { "—" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E),
                )
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(14.dp))
                Text("Reason", fontSize = 12.sp, color = Color(0xFF999999))
                Spacer(Modifier.height(4.dp))
                Text(
                    request.reason,
                    fontSize = 14.sp,
                    color = Color(0xFF333333),
                    lineHeight = 22.sp,
                )
            }
        }

        if (!request.status.equals("PENDING", ignoreCase = true)) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Admin Remarks", fontSize = 12.sp, color = Color(0xFF999999))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (request.adminRemarks.isBlank()) "No remarks provided." else request.adminRemarks,
                        fontSize = 14.sp,
                        color = Color(0xFF333333),
                        lineHeight = 22.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun formatRequestDate(millis: Long): String {
    if (millis == 0L) return "—"
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
}
