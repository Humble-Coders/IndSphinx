package com.humblesolutions.indsphinx.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.indsphinx.model.VisitorPass
import com.humblesolutions.indsphinx.viewmodel.VisitorPassUiState
import com.humblesolutions.indsphinx.viewmodel.VisitorPassViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NavyBlue = Color(0xFF1E2D6B)
private val BackgroundGray = Color(0xFFF2F4F8)

@Composable
fun VisitorPassScreen(
    occupantId: String,
    occupantName: String,
    flatId: String,
    flatNumber: String,
    onBack: () -> Unit
) {
    val viewModel: VisitorPassViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(occupantId) { viewModel.start(occupantId) }

    when (val state = uiState) {
        is VisitorPassUiState.Loading -> {
            Column(Modifier.fillMaxSize()) {
                VisitorPassHeader(title = "Visitor Pass", onBack = onBack)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyBlue)
                }
            }
        }
        is VisitorPassUiState.Loaded -> {
            Column(Modifier.fillMaxSize()) {
                VisitorPassHeader(title = "Visitor Pass", onBack = onBack)
                PassListContent(
                    passes = state.passes,
                    onRequestTapped = { viewModel.onRequestPassTapped() },
                    onPassSelected = { viewModel.onPassSelected(it) }
                )
            }
        }
        is VisitorPassUiState.RequestForm -> {
            Column(Modifier.fillMaxSize()) {
                VisitorPassHeader(
                    title = "Visitor Entry",
                    onBack = { viewModel.onBackFromForm() },
                    trailingIcon = Icons.Outlined.PersonAdd
                )
                PassFormContent(
                    isSubmitting = false,
                    onSubmit = { vName, vPhone, purpose, rel, visitDate ->
                        viewModel.submitPass(
                            occupantId = occupantId,
                            occupantName = occupantName,
                            flatId = flatId,
                            flatNumber = flatNumber,
                            visitorName = vName,
                            visitorPhone = vPhone,
                            purposeOfVisit = purpose,
                            relationshipWithVisitor = rel,
                            visitDateMillis = visitDate
                        )
                    }
                )
            }
        }
        is VisitorPassUiState.Submitting -> {
            Column(Modifier.fillMaxSize()) {
                VisitorPassHeader(title = "Visitor Entry", onBack = {}, trailingIcon = Icons.Outlined.PersonAdd)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyBlue)
                }
            }
        }
        is VisitorPassUiState.PassDetail -> {
            Column(Modifier.fillMaxSize()) {
                VisitorPassHeader(title = "Pass Details", onBack = { viewModel.onBackFromDetail() })
                PassDetailContent(pass = state.pass)
            }
        }
        is VisitorPassUiState.Error -> {
            Column(Modifier.fillMaxSize()) {
                VisitorPassHeader(title = "Visitor Pass", onBack = { viewModel.dismissError() })
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Color(0xFF999999), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun VisitorPassHeader(
    title: String,
    onBack: () -> Unit,
    trailingIcon: ImageVector? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack, null,
                tint = NavyBlue,
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
            Spacer(Modifier.width(16.dp))
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E),
                modifier = Modifier.weight(1f)
            )
            if (trailingIcon != null) {
                Icon(trailingIcon, null, tint = NavyBlue, modifier = Modifier.size(24.dp))
            }
        }
    }
    HorizontalDivider(color = Color(0xFFF0F0F0))
}

// MARK: - List

@Composable
private fun PassListContent(
    passes: List<VisitorPass>,
    onRequestTapped: () -> Unit,
    onPassSelected: (VisitorPass) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(BackgroundGray)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Request button
        Button(
            onClick = onRequestTapped,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
        ) {
            Icon(Icons.Outlined.PersonAdd, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Request Visitor Pass", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(20.dp))

        if (passes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No visitor passes yet", color = Color(0xFF999999), fontSize = 14.sp)
            }
        } else {
            val grouped = mapOf(
                "PENDING" to passes.filter { it.status == "PENDING" },
                "ACCEPTED" to passes.filter { it.status == "ACCEPTED" },
                "REJECTED" to passes.filter { it.status == "REJECTED" }
            )
            grouped.forEach { (status, list) ->
                if (list.isNotEmpty()) {
                    Text(
                        status.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF555555)
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        list.forEach { pass ->
                            PassCard(pass = pass, onClick = { onPassSelected(pass) })
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PassCard(pass: VisitorPass, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pass.visitorName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A2E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarToday, null, tint = Color(0xFF999999), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatDate(pass.visitDate), fontSize = 12.sp, color = Color(0xFF999999))
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Outlined.People, null, tint = Color(0xFF999999), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(pass.relationshipWithVisitor, fontSize = 12.sp, color = Color(0xFF999999), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                StatusBadgeVP(pass.status)
            }
            Icon(Icons.Outlined.NavigateNext, null, tint = Color(0xFFBBBBBB), modifier = Modifier.size(20.dp))
        }
    }
}

// MARK: - Form

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassFormContent(isSubmitting: Boolean, onSubmit: (String, String, String, String, Long) -> Unit) {
    var visitorName by rememberSaveable { mutableStateOf("") }
    var visitorPhone by rememberSaveable { mutableStateOf("") }
    var purpose by rememberSaveable { mutableStateOf("") }
    var relationship by rememberSaveable { mutableStateOf("") }
    var visitDateMillis by remember { mutableLongStateOf(0L) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    visitDateMillis = datePickerState.selectedDateMillis ?: 0L
                    showDatePicker = false
                }) { Text("OK", color = NavyBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = NavyBlue) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                FormField(
                    label = "Visitor Name",
                    icon = Icons.Outlined.PersonAdd,
                    value = visitorName,
                    onValueChange = { visitorName = it },
                    placeholder = "Enter visitor's full name"
                )
                FormField(
                    label = "Phone Number",
                    icon = Icons.Outlined.Phone,
                    value = visitorPhone,
                    onValueChange = { visitorPhone = it },
                    placeholder = "Enter phone number",
                    keyboardType = KeyboardType.Phone
                )
                FormField(
                    label = "Purpose of Visit",
                    icon = Icons.Outlined.Description,
                    value = purpose,
                    onValueChange = { purpose = it },
                    placeholder = "Describe the purpose of visit",
                    singleLine = false,
                    minLines = 3
                )
                FormField(
                    label = "Relationship With Visitor",
                    icon = Icons.Outlined.People,
                    value = relationship,
                    onValueChange = { relationship = it },
                    placeholder = "Enter your relationship with visitor"
                )

                // Date picker field
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarToday, null, tint = NavyBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Visit Date", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF1A1A2E))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF2F4F8))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = if (visitDateMillis > 0L) formatDate(visitDateMillis) else "Select visit date",
                            color = if (visitDateMillis > 0L) Color(0xFF1A1A2E) else Color(0xFFAAAAAA),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        val isValid = visitorName.isNotBlank() && visitorPhone.isNotBlank() && visitDateMillis > 0L
        Button(
            onClick = {
                if (isValid && !isSubmitting) {
                    onSubmit(visitorName.trim(), visitorPhone.trim(), purpose.trim(), relationship.trim(), visitDateMillis)
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isValid) NavyBlue else Color(0xFFBBBBBB)
            ),
            enabled = isValid && !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Submit Visitor Pass", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FormField(
    label: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = NavyBlue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF1A1A2E))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, fontSize = 14.sp, color = Color(0xFFAAAAAA)) },
            shape = RoundedCornerShape(10.dp),
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = NavyBlue,
                unfocusedContainerColor = Color(0xFFF2F4F8),
                focusedContainerColor = Color(0xFFF2F4F8)
            )
        )
    }
}

// MARK: - Detail

@Composable
private fun PassDetailContent(pass: VisitorPass) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(BackgroundGray)
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(pass.visitorName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                    StatusBadgeVP(pass.status)
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(16.dp))
                DetailRow(Icons.Outlined.Phone, "Phone", pass.visitorPhone)
                Spacer(Modifier.height(12.dp))
                DetailRow(Icons.Outlined.People, "Relationship", pass.relationshipWithVisitor)
                Spacer(Modifier.height(12.dp))
                DetailRow(Icons.Outlined.CalendarToday, "Visit Date", formatDate(pass.visitDate))
                Spacer(Modifier.height(12.dp))
                DetailRow(Icons.Outlined.CalendarToday, "Request Date", formatDate(pass.requestDate))
                Spacer(Modifier.height(12.dp))
                DetailRow(Icons.Outlined.Description, "Purpose", pass.purposeOfVisit)
                Spacer(Modifier.height(12.dp))
                DetailRow(Icons.Outlined.Badge, "Flat", pass.flatNumber)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF0F4FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = NavyBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color(0xFF999999))
            Text(value.ifEmpty { "—" }, fontSize = 14.sp, color = Color(0xFF1A1A2E), fontWeight = FontWeight.Medium)
        }
    }
}

// MARK: - Shared

@Composable
private fun StatusBadgeVP(status: String) {
    val (bg, fg) = when (status) {
        "PENDING" -> Color(0xFFFFF7ED) to Color(0xFFD97706)
        "ACCEPTED" -> Color(0xFFECFDF5) to Color(0xFF059669)
        "REJECTED" -> Color(0xFFFFEEEE) to Color(0xFFE53935)
        else -> Color(0xFFF5F5F5) to Color(0xFF616161)
    }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(status, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatDate(millis: Long): String {
    if (millis == 0L) return "—"
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
}
