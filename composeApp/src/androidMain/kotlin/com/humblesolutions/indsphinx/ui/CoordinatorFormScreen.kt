package com.humblesolutions.indsphinx.ui

import android.app.Application
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.indsphinx.viewmodel.BILL_ITEMS
import com.humblesolutions.indsphinx.viewmodel.CLEANLINESS_ITEMS
import com.humblesolutions.indsphinx.viewmodel.CoordinatorFormState
import com.humblesolutions.indsphinx.viewmodel.CoordinatorFormViewModel
import com.humblesolutions.indsphinx.viewmodel.REPAIR_ITEMS
import com.humblesolutions.indsphinx.viewmodel.SAFETY_ITEMS

private val FormBlue = Color(0xFF1E2D6B)
private val AccentBlue = Color(0xFF2563EB)
private val LightBlue = Color(0xFFEEF2FF)
private val FormBg = Color(0xFFF8F9FA)
private val BorderGray = Color(0xFFE5E7EB)
private val TextDark = Color(0xFF1F2937)
private val TextGray = Color(0xFF6B7280)

@Composable
fun CoordinatorFormScreen(
    occupantId: String,
    flatId: String,
    coordinatorName: String,
    flatNumber: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: CoordinatorFormViewModel = viewModel(
        factory = remember {
            object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return CoordinatorFormViewModel(
                        context.applicationContext as Application,
                        occupantId, flatId, coordinatorName, flatNumber
                    ) as T
                }
            }
        }
    )
    val state by viewModel.state.collectAsState()

    if (state.isSubmitted) {
        SuccessView(onBack = onBack)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormBg)
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FormBlue)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Monthly Flat Self-Check", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Annexure – C", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Flat Details
            FormCard(title = "Flat Details", accentColor = AccentBlue) {
                DetailRow("Flat Address", flatNumber)
                HorizontalDivider(color = BorderGray, modifier = Modifier.padding(vertical = 8.dp))
                DetailRow("Month", state.month)
                HorizontalDivider(color = BorderGray, modifier = Modifier.padding(vertical = 8.dp))
                DetailRow("Flat Coordinator Name", coordinatorName)
            }

            // 1. Cleanliness Status
            RadioSectionCard(
                number = "1", title = "Cleanliness Status",
                option1 = "Good", option2 = "Needs Attention",
                items = CLEANLINESS_ITEMS,
                selections = state.cleanliness,
                onSelect = viewModel::selectCleanliness
            )

            // 2. Repair & Maintenance Check
            RadioSectionCard(
                number = "2", title = "Repair & Maintenance Check",
                option1 = "OK", option2 = "Repair Needed",
                items = REPAIR_ITEMS,
                selections = state.repairs,
                onSelect = viewModel::selectRepair
            )

            // 3. Safety & Discipline
            RadioSectionCard(
                number = "3", title = "Safety & Discipline",
                option1 = "Yes", option2 = "No",
                items = SAFETY_ITEMS,
                selections = state.safety,
                onSelect = viewModel::selectSafety
            )

            // 4. Bill Payment Status
            RadioSectionCard(
                number = "4", title = "Bill Payment Status",
                option1 = "Paid", option2 = "Not Paid",
                items = BILL_ITEMS,
                selections = state.bills,
                onSelect = viewModel::selectBill
            )

            // 5. HR Issues
            FormCard(title = "5. Issues Reported to HR/Admin (if any)", accentColor = AccentBlue) {
                OutlinedTextField(
                    value = state.hrIssues,
                    onValueChange = viewModel::setHrIssues,
                    placeholder = { Text("Describe any issues...", color = TextGray, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderGray
                    ),
                    maxLines = 5
                )
            }

            // Declaration
            FormCard(title = "Coordinator Monthly Declaration", accentColor = AccentBlue) {
                Text(
                    "I confirm that I have checked the flat and the above information is correct.",
                    fontSize = 14.sp, color = TextDark,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.confirmed) LightBlue else FormBg)
                        .border(1.dp, if (state.confirmed) AccentBlue else BorderGray, RoundedCornerShape(8.dp))
                        .clickable { viewModel.setConfirmed(!state.confirmed) }
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.confirmed,
                        onCheckedChange = viewModel::setConfirmed,
                        colors = CheckboxDefaults.colors(checkedColor = AccentBlue)
                    )
                    Text(
                        "I confirm the above information is accurate.",
                        fontSize = 14.sp, color = TextDark
                    )
                }
                if (state.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.error!!, color = Color.Red, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // Submit
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FormBlue,
                    disabledContainerColor = FormBlue.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Submit Form", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SuccessView(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(FormBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                Icons.Default.CheckCircle, null,
                tint = Color(0xFF16A34A), modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Form Submitted!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(8.dp))
            Text(
                "Monthly self-check form has been successfully submitted.",
                fontSize = 14.sp, color = TextGray, modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = FormBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Done", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FormCard(
    title: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun RadioSectionCard(
    number: String,
    title: String,
    option1: String,
    option2: String,
    items: List<String>,
    selections: Map<String, String?>,
    onSelect: (String, String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$number. $title", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AccentBlue)
            Spacer(Modifier.height(10.dp))
            // Header row
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Text("", modifier = Modifier.weight(1f))
                Text(option1, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextGray,
                    modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text(option2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextGray,
                    modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            HorizontalDivider(color = BorderGray)
            items.forEachIndexed { index, item ->
                RadioRow(
                    label = item,
                    option1 = option1,
                    option2 = option2,
                    selected = selections[item],
                    onSelect = { onSelect(item, it) }
                )
                if (index < items.size - 1) HorizontalDivider(color = BorderGray)
            }
        }
    }
}

@Composable
private fun RadioRow(
    label: String,
    option1: String,
    option2: String,
    selected: String?,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = TextDark, modifier = Modifier.weight(1f))
        RadioToggle(
            selected = selected == option1,
            onClick = { onSelect(option1) },
            modifier = Modifier.width(80.dp)
        )
        RadioToggle(
            selected = selected == option2,
            onClick = { onSelect(option2) },
            modifier = Modifier.width(80.dp)
        )
    }
}

@Composable
private fun RadioToggle(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (selected) AccentBlue else Color.White)
                .border(2.dp, if (selected) AccentBlue else BorderGray, RoundedCornerShape(11.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = TextGray, modifier = Modifier.weight(1f))
        Text(value.ifBlank { "—" }, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
    }
}
