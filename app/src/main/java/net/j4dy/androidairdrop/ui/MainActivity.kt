package net.j4dy.androidairdrop.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.j4dy.androidairdrop.R
import net.j4dy.androidairdrop.core.model.AirDropPeer
import net.j4dy.androidairdrop.core.model.TransferState
import net.j4dy.androidairdrop.ui.theme.AccentGreen
import net.j4dy.androidairdrop.ui.theme.AccentRed
import net.j4dy.androidairdrop.ui.theme.AndroidAirDropTheme
import net.j4dy.androidairdrop.ui.theme.BluePrimary

class MainActivity : ComponentActivity() {

    private val viewModel: TransferViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AndroidAirDropTheme {
                val peers by viewModel.peers.collectAsState()
                val isScanning by viewModel.isScanning.collectAsState()
                val selectedFiles by viewModel.selectedFiles.collectAsState()
                val transferState by viewModel.transferState.collectAsState()

                // File picker launcher
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenMultipleDocuments()
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        viewModel.setFilesFromUris(uris, contentResolver)
                    }
                }

                // Permissions launcher
                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { _ ->
                    viewModel.restartDiscovery()
                }

                LaunchedEffect(Unit) {
                    val permissions = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (permissions.isNotEmpty()) {
                        permissionsLauncher.launch(permissions.toTypedArray())
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(R.string.app_name),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                IconButton(onClick = { viewModel.restartDiscovery() }) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh scan"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Wi-Fi AirDrop Requirement Banner
                        AirDropRequirementCard()

                        Spacer(modifier = Modifier.height(16.dp))

                        // Selected Files Bar
                        SelectedFilesCard(
                            fileCount = selectedFiles.size,
                            totalSize = selectedFiles.sumOf { it.size.coerceAtLeast(0) },
                            onSelectFiles = { filePickerLauncher.launch(arrayOf("*/*")) },
                            onClearFiles = { viewModel.clearSelectedFiles() }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Radar Scanner / Devices List
                        Text(
                            text = if (peers.isEmpty()) "Nearby Macs" else "Found ${peers.size} Mac(s)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (peers.isEmpty()) {
                            RadarEmptyView(isScanning = isScanning)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(peers) { peer ->
                                    DeviceItem(
                                        peer = peer,
                                        enabled = selectedFiles.isNotEmpty(),
                                        onSend = { viewModel.sendToPeer(peer) }
                                    )
                                }
                            }
                        }
                    }

                    // Transfer State BottomSheet
                    if (transferState !is TransferState.Idle) {
                        TransferBottomSheet(
                            state = transferState,
                            onDismiss = { viewModel.resetTransferState() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AirDropRequirementCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BluePrimary.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "💡 Quick Setup",
                fontWeight = FontWeight.Bold,
                color = BluePrimary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "1. Connect Android and Mac to the SAME Wi-Fi.\n2. On Mac: open Finder → AirDrop → Allow discovery by \"Everyone\".",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun SelectedFilesCard(
    fileCount: Int,
    totalSize: Long,
    onSelectFiles: () -> Unit,
    onClearFiles: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.UploadFile,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (fileCount == 0) {
                    Text("No files selected", fontWeight = FontWeight.SemiBold)
                    Text("Choose photos, videos or documents", fontSize = 12.sp, color = Color.Gray)
                } else {
                    Text("$fileCount file(s) ready", fontWeight = FontWeight.SemiBold)
                    Text(formatBytes(totalSize), fontSize = 12.sp, color = BluePrimary)
                }
            }
            if (fileCount > 0) {
                IconButton(onClick = onClearFiles) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear files",
                        tint = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Button(
                onClick = onSelectFiles,
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text(if (fileCount == 0) "Pick Files" else "Change")
            }
        }
    }
}

@Composable
fun RadarEmptyView(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            Box(
                modifier = Modifier
                    .size((110 * pulseScale).dp)
                    .clip(CircleShape)
                    .background(BluePrimary.copy(alpha = 0.15f))
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BluePrimary.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Laptop,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isScanning) "Searching for nearby Macs…" else "Scan stopped",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
fun DeviceItem(
    peer: AirDropPeer,
    enabled: Boolean,
    onSend: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onSend() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BluePrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Laptop,
                    contentDescription = null,
                    tint = BluePrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = peer.hostAddress,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Button(
                onClick = onSend,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text("Send")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferBottomSheet(
    state: TransferState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                is TransferState.Connecting -> {
                    CircularProgressIndicator(color = BluePrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Connecting to Mac…", fontWeight = FontWeight.Bold)
                }

                is TransferState.WaitingForAcceptance -> {
                    Icon(
                        Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Waiting for acceptance…", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Please click \"Accept\" on ${state.targetName}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }

                is TransferState.Uploading -> {
                    val progress = if (state.totalBytes > 0) {
                        state.bytesTransferred.toFloat() / state.totalBytes.toFloat()
                    } else 0f
                    val percent = (progress * 100).toInt()

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = BluePrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Sending to ${state.targetName} ($percent%)",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${formatBytes(state.bytesTransferred)} / ${formatBytes(state.totalBytes)}" +
                                if (state.speedBytesPerSec > 0) " (${formatBytes(state.speedBytesPerSec)}/s)" else "",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                is TransferState.Success -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Files Sent Successfully!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${state.fileCount} file(s) saved to ${state.targetName}'s Downloads.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Text("Done")
                    }
                }

                is TransferState.Failed -> {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Transfer Failed", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        state.reason,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Text("Dismiss")
                    }
                }

                TransferState.Idle -> {}
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(
        "%.1f %s",
        bytes / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups]
    )
}
