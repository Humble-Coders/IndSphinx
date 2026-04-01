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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.humblesolutions.indsphinx.model.Notice
import com.humblesolutions.indsphinx.viewmodel.NoticeboardUiState
import com.humblesolutions.indsphinx.viewmodel.NoticeboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NavyBlue = Color(0xFF1E2D6B)
private val BackgroundGray = Color(0xFFF2F4F8)

@Composable
fun NoticeboardScreen(onMenuClick: () -> Unit, initialNotice: Notice? = null) {
    val viewModel: NoticeboardViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialNotice) {
        initialNotice?.let { viewModel.openNoticeDirectly(it) }
    }

    when (val state = uiState) {
        is NoticeboardUiState.Loading -> {
            NoticeboardHeader(onMenuClick = onMenuClick, onBack = null)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NavyBlue)
            }
        }
        is NoticeboardUiState.Loaded -> {
            Column(Modifier.fillMaxSize()) {
                NoticeboardHeader(onMenuClick = onMenuClick, onBack = null)
                NoticeListContent(
                    notices = state.notices,
                    onNoticeClick = { viewModel.onNoticeSelected(it) }
                )
            }
        }
        is NoticeboardUiState.Detail -> {
            BackHandler { viewModel.onBackFromDetail() }
            Column(Modifier.fillMaxSize()) {
                NoticeDetailHeader(onBack = { viewModel.onBackFromDetail() })
                NoticeDetailContent(notice = state.notice)
            }
        }
        is NoticeboardUiState.Error -> {
            NoticeboardHeader(onMenuClick = onMenuClick, onBack = null)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = Color(0xFF999999), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun NoticeboardHeader(onMenuClick: () -> Unit, onBack: (() -> Unit)?) {
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
                Icons.Outlined.Menu, null,
                tint = NavyBlue,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onMenuClick() }
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "Noticeboard",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E),
                modifier = Modifier.weight(1f)
            )
        }
    }
    HorizontalDivider(color = Color(0xFFF0F0F0))
}

@Composable
private fun NoticeDetailHeader(onBack: () -> Unit) {
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
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "Notice Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E),
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Outlined.PushPin, null,
                tint = NavyBlue,
                modifier = Modifier.size(22.dp)
            )
        }
    }
    HorizontalDivider(color = Color(0xFFF0F0F0))
}

@Composable
private fun NoticeListContent(notices: List<Notice>, onNoticeClick: (Notice) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(BackgroundGray)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            "All Notices",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A2E)
        )
        Spacer(Modifier.height(12.dp))

        if (notices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No notices yet", color = Color(0xFF999999), fontSize = 14.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                notices.forEach { notice ->
                    NoticeCard(notice = notice, onClick = { onNoticeClick(notice) })
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NoticeCard(notice: Notice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                notice.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E)
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CalendarToday, null,
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    formatDate(notice.publishedAt),
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                notice.description,
                fontSize = 13.sp,
                color = Color(0xFF555555),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onClick() }
            ) {
                Text("View Details", fontSize = 13.sp, color = NavyBlue, fontWeight = FontWeight.Medium)
                Icon(Icons.Outlined.NavigateNext, null, tint = NavyBlue, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun NoticeDetailContent(notice: Notice) {
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
                Text(
                    notice.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CalendarToday, null,
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        formatDate(notice.publishedAt),
                        fontSize = 13.sp,
                        color = Color(0xFF999999)
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFEEF2FF))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Notice", fontSize = 11.sp, color = NavyBlue, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(16.dp))
                Text(
                    notice.description,
                    fontSize = 14.sp,
                    color = Color(0xFF333333),
                    lineHeight = 22.sp
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun formatDate(millis: Long): String {
    if (millis == 0L) return "—"
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
}
