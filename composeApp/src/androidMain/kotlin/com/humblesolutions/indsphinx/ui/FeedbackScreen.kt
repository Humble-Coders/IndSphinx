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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Title
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.indsphinx.model.Feedback
import com.humblesolutions.indsphinx.viewmodel.FeedbackUiState
import com.humblesolutions.indsphinx.viewmodel.FeedbackViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NavyBlue = Color(0xFF1E2D6B)
private val BackgroundGray = Color(0xFFF2F4F8)

@Composable
fun FeedbackScreen(
    occupantId: String,
    occupantName: String,
    onBack: () -> Unit
) {
    val viewModel: FeedbackViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(occupantId) { viewModel.start(occupantId) }

    when (val state = uiState) {
        is FeedbackUiState.Loading -> {
            Column(Modifier.fillMaxSize()) {
                FeedbackHeader(title = "Feedback", onBack = onBack)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyBlue)
                }
            }
        }
        is FeedbackUiState.Loaded -> {
            Column(Modifier.fillMaxSize()) {
                FeedbackHeader(title = "Feedback", onBack = onBack, trailingIcon = true)
                FeedbackListContent(
                    feedbacks = state.feedbacks,
                    onSubmitTapped = { viewModel.onSubmitTapped() },
                    onFeedbackSelected = { viewModel.onFeedbackSelected(it) }
                )
            }
        }
        is FeedbackUiState.SubmitForm -> {
            Column(Modifier.fillMaxSize()) {
                FeedbackHeader(title = "Submit Feedback", onBack = { viewModel.onBackFromForm() })
                FeedbackFormContent(
                    isSubmitting = false,
                    onSubmit = { title, desc ->
                        viewModel.submit(occupantId, occupantName, title, desc)
                    }
                )
            }
        }
        is FeedbackUiState.Submitting -> {
            Column(Modifier.fillMaxSize()) {
                FeedbackHeader(title = "Submit Feedback", onBack = {})
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyBlue)
                }
            }
        }
        is FeedbackUiState.Detail -> {
            Column(Modifier.fillMaxSize()) {
                FeedbackHeader(title = "Feedback Details", onBack = { viewModel.onBackFromDetail() })
                FeedbackDetailContent(feedback = state.feedback)
            }
        }
        is FeedbackUiState.Error -> {
            Column(Modifier.fillMaxSize()) {
                FeedbackHeader(title = "Feedback", onBack = { viewModel.dismissError() })
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Color(0xFF999999), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun FeedbackHeader(title: String, onBack: () -> Unit, trailingIcon: Boolean = false) {
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
            if (trailingIcon) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, tint = NavyBlue, modifier = Modifier.size(22.dp))
            }
        }
    }
    HorizontalDivider(color = Color(0xFFF0F0F0))
}

// MARK: - List

@Composable
private fun FeedbackListContent(
    feedbacks: List<Feedback>,
    onSubmitTapped: () -> Unit,
    onFeedbackSelected: (Feedback) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(BackgroundGray)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        // Subtitle
        Text(
            "Share suggestions or ideas to improve residential facilities.",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        // Submit button
        Button(
            onClick = onSubmitTapped,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
        ) {
            Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Submit Feedback", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))

        if (feedbacks.isNotEmpty()) {
            Text(
                "Previously Submitted",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E)
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                feedbacks.forEach { fb ->
                    FeedbackCard(feedback = fb, onClick = { onFeedbackSelected(fb) })
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FeedbackCard(feedback: Feedback, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarToday, null, tint = Color(0xFF999999), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(formatFbDate(feedback.date), fontSize = 12.sp, color = Color(0xFF999999))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                feedback.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                feedback.description,
                fontSize = 13.sp,
                color = Color(0xFF555555),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF2F4F8))
                    .clickable { onClick() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("View Details", color = NavyBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// MARK: - Form

@Composable
private fun FeedbackFormContent(isSubmitting: Boolean, onSubmit: (String, String) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    val isValid = title.isNotBlank() && description.isNotBlank()

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
                // Title
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Title, null, tint = NavyBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Title", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF1A1A2E))
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter feedback title", fontSize = 14.sp, color = Color(0xFFAAAAAA)) },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = NavyBlue,
                            unfocusedContainerColor = Color(0xFFF2F4F8),
                            focusedContainerColor = Color(0xFFF2F4F8)
                        )
                    )
                }
                // Description
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Description, null, tint = NavyBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Description", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF1A1A2E))
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Describe your feedback in detail", fontSize = 14.sp, color = Color(0xFFAAAAAA)) },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = false,
                        minLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = NavyBlue,
                            unfocusedContainerColor = Color(0xFFF2F4F8),
                            focusedContainerColor = Color(0xFFF2F4F8)
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (isValid && !isSubmitting) onSubmit(title.trim(), description.trim())
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
                Text("Submit Feedback", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// MARK: - Detail

@Composable
private fun FeedbackDetailContent(feedback: Feedback) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarToday, null, tint = Color(0xFF999999), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(formatFbDate(feedback.date), fontSize = 13.sp, color = Color(0xFF999999))
                }
                Spacer(Modifier.height(10.dp))
                Text(feedback.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(12.dp))
                Text(
                    feedback.description,
                    fontSize = 14.sp,
                    color = Color(0xFF333333),
                    lineHeight = 22.sp
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun formatFbDate(millis: Long): String {
    if (millis == 0L) return "—"
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
}
