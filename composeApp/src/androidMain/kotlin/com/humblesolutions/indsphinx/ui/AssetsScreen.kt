package com.humblesolutions.indsphinx.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.indsphinx.model.OccupantAsset
import com.humblesolutions.indsphinx.viewmodel.AssetsUiState
import com.humblesolutions.indsphinx.viewmodel.AssetsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AssetNavy = Color(0xFF1E2D6B)
private val AssetBackground = Color(0xFFF2F4F8)
private val AssetBorder = Color(0xFFE5E7EB)
private val AssetMuted = Color(0xFF6B7280)

private fun formatAssetDate(millis: Long): String =
    if (millis <= 0L) "-"
    else SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(millis))

/** Status chip colours: assigned reads as active, the closed states as neutral/warning. */
private fun statusColors(status: String): Pair<Color, Color> = when (status.uppercase()) {
    OccupantAsset.STATUS_RETURNED -> Color(0xFFE7F6EF) to Color(0xFF0F7B4F)
    OccupantAsset.STATUS_DAMAGED  -> Color(0xFFFDF3E2) to Color(0xFF9A6207)
    OccupantAsset.STATUS_MISSING  -> Color(0xFFFDECEC) to Color(0xFFB3261E)
    else                          -> Color(0xFFE8ECF7) to AssetNavy
}

/**
 * The occupant's own assets: what they hold now, and what they held before.
 *
 * Read-only. Assets are issued and taken back from the admin portal; this
 * screen just reflects the live record.
 */
@Composable
fun AssetsScreen(
    authUid: String,
    onBack: () -> Unit,
    viewModel: AssetsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(authUid) { viewModel.start(authUid) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AssetBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AssetNavy)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.Close, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "My Assets",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        when (val state = uiState) {
            is AssetsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AssetNavy)
            }

            is AssetsUiState.Error -> Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(state.message, color = Color(0xFFB3261E), fontSize = 15.sp)
            }

            is AssetsUiState.Loaded -> {
                val snapshot = state.snapshot
                if (snapshot.isEmpty) {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = Color(0xFFC7CBD4),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No assets assigned to you yet.",
                                color = AssetMuted,
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (snapshot.current.isNotEmpty()) {
                            item { AssetSectionHeader("Currently Held", snapshot.current.size) }
                            items(snapshot.current) { asset -> AssetCard(asset) }
                        }
                        if (snapshot.past.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(if (snapshot.current.isEmpty()) 0.dp else 6.dp))
                                AssetSectionHeader("Previously Held", snapshot.past.size)
                            }
                            items(snapshot.past) { asset -> AssetCard(asset) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetSectionHeader(title: String, count: Int) {
    Text(
        "$title ($count)",
        color = AssetMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
private fun AssetCard(asset: OccupantAsset) {
    val (chipBg, chipFg) = statusColors(asset.status)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    asset.assetName.ifEmpty { "Asset" },
                    color = Color(0xFF111827),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "No. ${asset.assetNumber}",
                    color = AssetMuted,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(chipBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    OccupantAsset.statusLabel(asset.status),
                    color = chipFg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(AssetBorder))
        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            AssetDateColumn("Assigned on", formatAssetDate(asset.assignedDate), Modifier.weight(1f))
            if (!asset.isOpen) {
                AssetDateColumn(
                    when (asset.status.uppercase()) {
                        OccupantAsset.STATUS_MISSING -> "Reported on"
                        else -> "Returned on"
                    },
                    formatAssetDate(asset.returnedDate),
                    Modifier.weight(1f)
                )
            }
        }

        // Remarks are the handover note while the item is with the occupant.
        // Once closed, the field holds the admin's closing note, which is not
        // surfaced in the app.
        if (asset.isOpen && asset.remarks.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AssetBackground)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(asset.remarks, color = Color(0xFF4B5563), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AssetDateColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = AssetMuted, fontSize = 11.sp)
        Text(value, color = Color(0xFF111827), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
