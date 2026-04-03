package com.humblesolutions.indsphinx.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.indsphinx.viewmodel.RESPONSIBILITIES
import com.humblesolutions.indsphinx.viewmodel.ResidentialFormUiState
import com.humblesolutions.indsphinx.viewmodel.ResidentialFormViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PrimaryBlue = Color(0xFF2A3080)
private val LightBlue = Color(0xFFEEF0FA)
private val TextGray = Color(0xFF6B7280)
private val BorderColor = Color(0xFFE5E7EB)
private val TextDark = Color(0xFF1F2937)
private val TextBody = Color(0xFF374151)

@Composable
fun ResidentialFormScreen(
    onFormComplete: () -> Unit,
    viewModel: ResidentialFormViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ResidentialFormUiState.Submitted) onFormComplete()
    }

    when (val state = uiState) {
        is ResidentialFormUiState.Loading -> {
            Box(
                Modifier.fillMaxSize().background(Color(0xFFF8F9FA)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = PrimaryBlue) }
        }
        is ResidentialFormUiState.Error -> {
            Box(
                Modifier.fillMaxSize().background(Color(0xFFF8F9FA)).padding(24.dp),
                contentAlignment = Alignment.Center
            ) { Text(state.message, color = Color.Red, fontSize = 15.sp) }
        }
        is ResidentialFormUiState.Ready -> {
            ReadyContent(
                state = state,
                onToggleAmenity = viewModel::toggleAmenity,
                onToggleResponsibility = viewModel::toggleResponsibility,
                onTermsAccepted = viewModel::setTermsAccepted,
                onShowTermsDialog = viewModel::setShowTermsDialog,
                onSubmit = viewModel::submitForm
            )
        }
        is ResidentialFormUiState.Submitted -> {
            Box(
                Modifier.fillMaxSize().background(Color(0xFFF8F9FA)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = PrimaryBlue) }
        }
    }
}

@Composable
private fun ReadyContent(
    state: ResidentialFormUiState.Ready,
    onToggleAmenity: (String) -> Unit,
    onToggleResponsibility: (Int) -> Unit,
    onTermsAccepted: (Boolean) -> Unit,
    onShowTermsDialog: (Boolean) -> Unit,
    onSubmit: () -> Unit
) {
    if (state.showTermsDialog) {
        TermsDialog(
            html = state.termsHtml,
            onDismiss = { onShowTermsDialog(false) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Top bar with status bar padding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryBlue)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Residential Acceptance Form",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            SectionCard {
                Column {
                    Text("Residential Acceptance Form", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(Modifier.height(4.dp))
                    Text("Please review and confirm your accommodation details", fontSize = 13.sp, color = TextGray)
                }
            }

            // Flat Allocation Information
            SectionCard(title = "Flat Allocation Information") {
                InfoRow(Icons.Outlined.Person, "Resident Name", state.occupantName)
                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(Icons.Outlined.Badge, "Employee ID", state.empId)
                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(Icons.Outlined.Home, "Flat Number", state.flatNumber)
                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(
                    Icons.Outlined.CalendarToday,
                    "Move-In Date",
                    if (state.occupantFrom > 0L) formatDate(state.occupantFrom) else "—"
                )
            }

            // Items Received in Flat
            val allAmenities = state.roomAmenities + state.commonAmenities
            if (allAmenities.isNotEmpty()) {
                SectionCard(title = "Items Received in Flat") {
                    Text(
                        "Please confirm the items you have received in your allocated accommodation.",
                        fontSize = 13.sp,
                        color = TextGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val rows = allAmenities.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { amenity ->
                                    AmenityItem(
                                        name = amenity,
                                        selected = amenity in state.selectedAmenities,
                                        onToggle = { onToggleAmenity(amenity) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Resident Responsibilities (checkboxes)
            SectionCard(title = "Resident Responsibilities") {
                Text(
                    "Please confirm each responsibility by checking the boxes below.",
                    fontSize = 13.sp,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                RESPONSIBILITIES.forEachIndexed { index, text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleResponsibility(index) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = index in state.checkedResponsibilities,
                            onCheckedChange = { onToggleResponsibility(index) },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(text, fontSize = 14.sp, color = TextBody)
                    }
                    if (index < RESPONSIBILITIES.size - 1) {
                        Divider(color = BorderColor, modifier = Modifier.padding(start = 48.dp))
                    }
                }
            }

            // Terms & Conditions
            SectionCard(title = "Terms & Conditions") {
                if (state.termsHtml.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { onShowTermsDialog(true) },
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("View Terms & Conditions", color = PrimaryBlue, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.termsAccepted) LightBlue else Color(0xFFF9FAFB))
                        .border(1.dp, if (state.termsAccepted) PrimaryBlue else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { onTermsAccepted(!state.termsAccepted) }
                        .padding(end = 12.dp)
                        .fillMaxWidth()
                ) {
                    Checkbox(
                        checked = state.termsAccepted,
                        onCheckedChange = onTermsAccepted,
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                    )
                    Text(
                        "I accept the terms and conditions of the company residential accommodation.",
                        fontSize = 14.sp,
                        color = TextBody
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // Submit button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    disabledContainerColor = PrimaryBlue.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Outlined.CheckBox, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Submit Agreement", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "By submitting this form, you confirm that the provided information is accurate.",
                fontSize = 11.sp,
                color = TextGray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SectionCard(title: String? = null, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).background(LightBlue, shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = TextGray)
            Text(value.ifBlank { "—" }, fontSize = 15.sp, color = TextDark, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AmenityItem(
    name: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) LightBlue else Color.White)
            .border(1.5.dp, if (selected) PrimaryBlue else BorderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Outlined.MeetingRoom,
                contentDescription = null,
                tint = if (selected) PrimaryBlue else TextGray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                name,
                fontSize = 13.sp,
                color = if (selected) PrimaryBlue else TextBody,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        }
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    if (selected) PrimaryBlue else Color.White,
                    shape = RoundedCornerShape(4.dp)
                )
                .border(1.5.dp, if (selected) PrimaryBlue else BorderColor, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun TermsDialog(html: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryBlue)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Text(
                        "Terms & Conditions",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                HtmlWebView(
                    html = html,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HtmlWebView(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = false
            }
        },
        update = { webView ->
            val styledHtml = """
                <html><head><meta name='viewport' content='width=device-width,initial-scale=1'>
                <style>body{font-family:sans-serif;font-size:15px;color:#374151;line-height:1.7;margin:16px;padding:0;}b{color:#1F2937;}</style>
                </head><body>$html</body></html>
            """.trimIndent()
            webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
