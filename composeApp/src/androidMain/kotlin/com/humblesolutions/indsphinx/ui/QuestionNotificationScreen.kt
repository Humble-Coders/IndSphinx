package com.humblesolutions.indsphinx.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.indsphinx.model.QuestionNotification
import com.humblesolutions.indsphinx.viewmodel.QuestionNotificationUiState
import com.humblesolutions.indsphinx.viewmodel.QuestionNotificationViewModel

private val QnNavyBlue = Color(0xFF1E2D6B)
private val QnBackgroundGray = Color(0xFFF2F4F8)

@Composable
fun QuestionNotificationScreen(
    qnId: String,
    displayName: String,
    flatNo: String,
    recipientType: String,
    onBack: () -> Unit
) {
    val viewModel: QuestionNotificationViewModel = viewModel(
        key = qnId,
        factory = QuestionNotificationViewModel.factory(qnId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(QnBackgroundGray)) {
        QnHeader(onBack = onBack)

        when (val state = uiState) {
            is QuestionNotificationUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = QnNavyBlue)
                }
            }
            is QuestionNotificationUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Color(0xFF999999), fontSize = 14.sp)
                }
            }
            is QuestionNotificationUiState.AlreadyResponded -> {
                QnAlreadyRespondedContent(qn = state.qn)
            }
            is QuestionNotificationUiState.Ready -> {
                QnContent(
                    qn = state.qn,
                    selectedOptions = state.selectedOptions,
                    textInput = state.textInput,
                    isSubmitting = state.isSubmitting,
                    isDeadlinePassed = state.isDeadlinePassed,
                    onToggleOption = { viewModel.toggleOption(it) },
                    onTextChange = { viewModel.setTextInput(it) },
                    onSubmit = { viewModel.submit(displayName, flatNo, recipientType) }
                )
            }
        }
    }
}

@Composable
private fun QnHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(QnNavyBlue)
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
                tint = Color.White,
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
            Spacer(Modifier.width(16.dp))
            Text("Question", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun QnContent(
    qn: QuestionNotification,
    selectedOptions: Set<String>,
    textInput: String,
    isSubmitting: Boolean,
    isDeadlinePassed: Boolean,
    onToggleOption: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(qn.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                Spacer(Modifier.height(8.dp))
                Text(qn.description, fontSize = 14.sp, color = Color(0xFF555555), lineHeight = 22.sp)

                if (isDeadlinePassed) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF3F3))
                            .padding(12.dp)
                    ) {
                        Text("Response deadline has passed.", fontSize = 13.sp, color = Color(0xFFE53935))
                    }
                    return@Column
                }

                Spacer(Modifier.height(20.dp))

                if (qn.options.isNotEmpty()) {
                    Text(
                        if (qn.allowMultipleChoices) "Select all that apply:" else "Select one:",
                        fontSize = 13.sp,
                        color = Color(0xFF888888),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        qn.options.forEach { option ->
                            val selected = option in selectedOptions
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) Color(0xFFEEF2FF) else Color(0xFFF8F9FA))
                                    .border(
                                        width = 1.5.dp,
                                        color = if (selected) QnNavyBlue else Color(0xFFE0E0E0),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onToggleOption(option) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    option,
                                    fontSize = 14.sp,
                                    color = if (selected) QnNavyBlue else Color(0xFF333333),
                                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selected) {
                                    Icon(
                                        Icons.Outlined.CheckCircle, null,
                                        tint = QnNavyBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text("Your response:", fontSize = 13.sp, color = Color(0xFF888888), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("Type your response here…", fontSize = 13.sp, color = Color(0xFFBBBBBB)) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QnNavyBlue,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                }

                Spacer(Modifier.height(24.dp))

                val canSubmit = if (qn.options.isNotEmpty()) selectedOptions.isNotEmpty() else textInput.isNotBlank()
                Button(
                    onClick = onSubmit,
                    enabled = canSubmit && !isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QnNavyBlue)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Submit Response", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QnAlreadyRespondedContent(qn: QuestionNotification) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(52.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Response Recorded",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A2E),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Thank you. Your response to “${qn.title}” has been saved.",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}
