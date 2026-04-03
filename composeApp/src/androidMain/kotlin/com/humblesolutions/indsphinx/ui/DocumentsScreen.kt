package com.humblesolutions.indsphinx.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.indsphinx.model.Document
import com.humblesolutions.indsphinx.viewmodel.DocumentsUiState
import com.humblesolutions.indsphinx.viewmodel.DocumentsViewModel

private val DocNavyBlue = Color(0xFF1E2D6B)
private val DocBackground = Color(0xFFF2F4F8)
private val DocBorder = Color(0xFFE5E7EB)

@Composable
fun DocumentsScreen(
    onBack: () -> Unit,
    viewModel: DocumentsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDoc by remember { mutableStateOf<Document?>(null) }

    val current = selectedDoc
    if (current != null) {
        DocumentDetailScreen(document = current, onBack = { selectedDoc = null })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DocBackground)
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DocNavyBlue)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.Close, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Documents",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        when (val state = uiState) {
            is DocumentsUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DocNavyBlue)
                }
            }
            is DocumentsUiState.Error -> {
                Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = Color.Red, fontSize = 15.sp)
                }
            }
            is DocumentsUiState.Ready -> {
                if (state.documents.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No documents available.", color = Color(0xFF9E9E9E), fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        item { Spacer(Modifier.height(12.dp)) }
                        items(state.documents) { doc ->
                            DocumentListItem(doc = doc, onClick = { selectedDoc = doc })
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentListItem(doc: Document, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEEF0FA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Description,
                contentDescription = null,
                tint = DocNavyBlue,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            doc.name.ifBlank { "Untitled" },
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A2E),
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Outlined.NavigateNext, null, tint = Color(0xFFBBBBBB), modifier = Modifier.size(20.dp))
    }
    HorizontalDivider(color = DocBorder, modifier = Modifier.padding(start = 74.dp))
}

@Composable
private fun DocumentDetailScreen(document: Document, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DocNavyBlue)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.Close, contentDescription = "Back", tint = Color.White)
            }
            Text(
                document.name.ifBlank { "Document" },
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp),
                maxLines = 1
            )
        }
        HtmlWebView(
            html = document.htmlContent,
            modifier = Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()
        )
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
            val styled = """
                <html><head>
                <meta name='viewport' content='width=device-width,initial-scale=1'>
                <style>body{font-family:sans-serif;font-size:15px;color:#374151;line-height:1.7;margin:16px;padding:0;}b{color:#1F2937;}</style>
                </head><body>$html</body></html>
            """.trimIndent()
            webView.loadDataWithBaseURL(null, styled, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}
