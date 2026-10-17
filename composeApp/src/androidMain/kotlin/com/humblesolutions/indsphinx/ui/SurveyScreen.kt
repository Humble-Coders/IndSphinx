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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.indsphinx.model.Notice
import com.humblesolutions.indsphinx.model.QuestionAnswer
import com.humblesolutions.indsphinx.model.Survey
import com.humblesolutions.indsphinx.model.SurveyAnswer
import com.humblesolutions.indsphinx.model.SurveyQuestion
import com.humblesolutions.indsphinx.viewmodel.SurveyUiState
import com.humblesolutions.indsphinx.viewmodel.SurveyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val SurveyNavy = Color(0xFF1E2D6B)
private val SurveyBackground = Color(0xFFF2F4F8)
private val SurveyRed = Color(0xFFE53935)
private val SurveyGreen = Color(0xFF2E7D32)
private val SurveyInk = Color(0xFF1A1A2E)

@Composable
fun SurveyScreen(
    noticeId: String,
    /** True for a survey sent to selected residents (see Survey.collectionFor). */
    forSelectedResidents: Boolean = false,
    displayName: String,
    flatNo: String,
    recipientType: String,
    onBack: () -> Unit
) {
    // Keyed apart from NoticeQuestionViewModel, which uses the bare id, and
    // by collection: the two kinds of survey are separate documents.
    val viewModel: SurveyViewModel = viewModel(
        key = "survey-${if (forSelectedResidents) "selected" else "board"}-$noticeId",
        factory = SurveyViewModel.factory(noticeId, forSelectedResidents)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // The deadline timer does not run while the phone sleeps: check again
    // whenever the screen comes back.
    LifecycleResumeEffect(viewModel) {
        viewModel.recheckDeadline()
        onPauseOrDispose { }
    }
    // Held here, not in the form: Firestore shows a submitted response as
    // saved before the server answers, so a refused submit passes through
    // "Response Recorded" and back. Kept out here, the form returns where
    // the resident left it, at the Submit button.
    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxSize().background(SurveyBackground)) {
        SurveyHeader(onBack = onBack)

        when (val state = uiState) {
            is SurveyUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SurveyNavy)
                }
            }
            is SurveyUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        state.message,
                        color = Color(0xFF5F6368),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.retry() }) {
                        Text("Try again", color = SurveyNavy, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            is SurveyUiState.AlreadyResponded -> SurveyRecordedContent(notice = state.notice, sending = state.sending)
            is SurveyUiState.Ready -> SurveyForm(
                notice = state.notice,
                answers = viewModel.answers,
                scrollState = scrollState,
                isSubmitting = state.isSubmitting,
                isDeadlinePassed = state.isDeadlinePassed,
                onToggleOption = { questionId, option -> viewModel.toggleOption(questionId, option) },
                onTextChange = { questionId, text -> viewModel.setText(questionId, text) },
                onSubmit = { viewModel.submit(displayName, flatNo, recipientType) }
            )
        }
    }
}

@Composable
private fun SurveyHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurveyNavy)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("Survey", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun SurveyForm(
    notice: Notice,
    answers: Map<String, SurveyAnswer>,
    scrollState: ScrollState,
    isSubmitting: Boolean,
    isDeadlinePassed: Boolean,
    onToggleOption: (questionId: String, option: String) -> Unit,
    onTextChange: (questionId: String, text: String) -> Unit,
    onSubmit: () -> Unit
) {
    val questions = notice.questions
    val scope = rememberCoroutineScope()
    // Where each question card sits in the column, for jumping to the first
    // unanswered one. Not state: positions never change what is drawn.
    val cardOffsets = remember { mutableMapOf<String, Int>() }
    // Set by the first Submit tap with questions still open. From then on
    // every unanswered question says so, until it is answered.
    var showMissing by rememberSaveable { mutableStateOf(false) }

    val answeredCount = Survey.answeredCount(questions, answers)
    val remaining = questions.size - answeredCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SurveyIntroCard(notice = notice, isDeadlinePassed = isDeadlinePassed)
        if (isDeadlinePassed) return@Column

        SurveyProgress(answered = answeredCount, total = questions.size)

        questions.forEachIndexed { index, question ->
            val answer = answers[question.id]
            SurveyQuestionCard(
                question = question,
                number = index + 1,
                total = questions.size,
                answer = answer,
                showMissing = showMissing && !Survey.isAnswered(question, answer),
                enabled = !isSubmitting,
                onToggleOption = { onToggleOption(question.id, it) },
                onTextChange = { onTextChange(question.id, it) },
                modifier = Modifier.onGloballyPositioned {
                    cardOffsets[question.id] = it.positionInParent().y.roundToInt()
                }
            )
        }

        if (showMissing && remaining > 0) {
            Text(
                if (remaining == 1) "1 question still needs an answer."
                else "$remaining questions still need an answer.",
                fontSize = 13.sp,
                color = SurveyRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = {
                if (Survey.isComplete(questions, answers)) {
                    onSubmit()
                } else {
                    showMissing = true
                    val first = questions.firstOrNull { !Survey.isAnswered(it, answers[it.id]) }
                    val target = first?.let { cardOffsets[it.id] }
                    if (target != null) scope.launch { scrollState.animateScrollTo(target) }
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SurveyNavy)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Submit Survey", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SurveyIntroCard(notice: Notice, isDeadlinePassed: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (notice.forSelectedResidents) {
                SelectedResidentsChip()
                Spacer(Modifier.height(10.dp))
            }
            Text(notice.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SurveyInk)
            if (notice.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(notice.description, fontSize = 14.sp, color = Color(0xFF555555), lineHeight = 22.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Poll, null, tint = SurveyNavy, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                val count = notice.questions.size
                Text(
                    if (count == 1) "1 question" else "$count questions",
                    fontSize = 12.sp,
                    color = SurveyNavy,
                    fontWeight = FontWeight.Medium
                )
            }
            val deadline = notice.responseDeadline
            if (deadline != null && !isDeadlinePassed) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, tint = Color(0xFF888888), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Please respond by ${formatDeadline(deadline)}",
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
            if (isDeadlinePassed) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3F3))
                        .padding(12.dp)
                ) {
                    Text(
                        "This survey is closed: the response deadline has passed. " +
                            "If the deadline is extended, you can answer it here again.",
                        fontSize = 13.sp,
                        color = SurveyRed,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

/** "Sent to selected residents": this survey reached only the residents the admin chose. */
@Composable
fun SelectedResidentsChip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFEDE7F6))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Groups, null, tint = Color(0xFF5E35B1), modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text("Sent to selected residents", fontSize = 11.sp, color = Color(0xFF5E35B1), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SurveyProgress(answered: Int, total: Int) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text(
            "$answered of $total answered",
            fontSize = 12.sp,
            color = Color(0xFF666666),
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else answered.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = SurveyNavy,
            trackColor = Color(0xFFDDE3F0),
            drawStopIndicator = {}
        )
    }
}

@Composable
private fun SurveyQuestionCard(
    question: SurveyQuestion,
    number: Int,
    total: Int,
    answer: SurveyAnswer?,
    showMissing: Boolean,
    enabled: Boolean,
    onToggleOption: (String) -> Unit,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val answered = Survey.isAnswered(question, answer)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (showMissing) Modifier.border(1.5.dp, SurveyRed, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "QUESTION $number OF $total",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SurveyNavy,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )
                if (answered) {
                    Icon(Icons.Filled.CheckCircle, "Answered", tint = SurveyGreen, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(question.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SurveyInk, lineHeight = 21.sp)
            Spacer(Modifier.height(12.dp))

            if (question.isChoice) {
                Text(
                    if (question.allowsMultiple) "Select all that apply:" else "Select one:",
                    fontSize = 13.sp,
                    color = Color(0xFF888888),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    // A single choice is announced as one radio group by TalkBack.
                    modifier = if (question.allowsMultiple) Modifier else Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val selected = answer?.selectedOptions.orEmpty()
                    question.options.forEach { option ->
                        SurveyOptionRow(
                            option = option,
                            selected = option in selected,
                            multiple = question.allowsMultiple,
                            enabled = enabled,
                            onClick = { onToggleOption(option) }
                        )
                    }
                }
            } else {
                val text = answer?.textResponse.orEmpty()
                Text("Your answer:", fontSize = 13.sp, color = Color(0xFF888888), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Type your answer here…", fontSize = 13.sp, color = Color(0xFFBBBBBB)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SurveyNavy,
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${text.length}/${QuestionAnswer.MAX_TEXT_LENGTH}",
                    fontSize = 11.sp,
                    color = if (text.length >= QuestionAnswer.MAX_TEXT_LENGTH) SurveyRed else Color(0xFF999999),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showMissing) {
                Spacer(Modifier.height(8.dp))
                Text("Please answer this question.", fontSize = 12.sp, color = SurveyRed, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SurveyOptionRow(
    option: String,
    selected: Boolean,
    multiple: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    // Radio buttons for one choice, checkboxes for several, so the resident
    // can tell before tapping whether a second tap adds or replaces.
    val interaction = if (multiple) {
        Modifier.toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        Modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Color(0xFFEEF2FF) else Color(0xFFF8F9FA))
            .border(1.5.dp, if (selected) SurveyNavy else Color(0xFFE0E0E0), shape)
            .then(interaction)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                multiple && selected -> Icons.Filled.CheckBox
                multiple -> Icons.Outlined.CheckBoxOutlineBlank
                selected -> Icons.Filled.RadioButtonChecked
                else -> Icons.Outlined.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = if (selected) SurveyNavy else Color(0xFF9E9E9E),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            option,
            fontSize = 14.sp,
            color = if (selected) SurveyNavy else Color(0xFF333333),
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SurveyRecordedContent(notice: Notice, sending: Boolean) {
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
                .background(if (sending) Color(0xFFE8EAF6) else Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            if (sending) {
                Icon(Icons.Outlined.CloudUpload, null, tint = SurveyNavy, modifier = Modifier.size(48.dp))
            } else {
                Icon(Icons.Outlined.CheckCircle, null, tint = SurveyGreen, modifier = Modifier.size(52.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            if (sending) "Sending Your Answers" else "Response Recorded",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = SurveyInk,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (sending) {
                "Your answers to “${notice.title}” are saved on this phone and will be sent as soon as it is online."
            } else {
                "Thank you. Your answers to “${notice.title}” have been saved."
            },
            fontSize = 14.sp,
            color = Color(0xFF666666),
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** DD-MM-YYYY, as dates are shown across My Nest; same text as the iOS app. */
private fun formatDeadline(millis: Long): String =
    SimpleDateFormat("dd-MM-yyyy, hh:mm a", Locale.US).format(Date(millis))
