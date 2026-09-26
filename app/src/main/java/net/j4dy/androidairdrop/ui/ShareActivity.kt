package net.j4dy.androidairdrop.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.j4dy.androidairdrop.core.model.TransferState
import net.j4dy.androidairdrop.ui.theme.AndroidAirDropTheme

class ShareActivity : ComponentActivity() {

    private val viewModel: TransferViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIncomingShareIntent(intent)

        setContent {
            AndroidAirDropTheme {
                val peers by viewModel.peers.collectAsState()
                val isScanning by viewModel.isScanning.collectAsState()
                val selectedFiles by viewModel.selectedFiles.collectAsState()
                val transferState by viewModel.transferState.collectAsState()

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                // Close activity when transfer succeeds or user dismisses
                LaunchedEffect(transferState) {
                    if (transferState is TransferState.Success) {
                        // Keep open for 2s then close
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    ModalBottomSheet(
                        onDismissRequest = { finish() },
                        sheetState = sheetState
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "AirDrop to Mac",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sharing ${selectedFiles.size} file(s)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (transferState !is TransferState.Idle) {
                                TransferBottomSheet(
                                    state = transferState,
                                    onDismiss = {
                                        viewModel.resetTransferState()
                                        if (transferState is TransferState.Success) {
                                            finish()
                                        }
                                    }
                                )
                            } else {
                                if (peers.isEmpty()) {
                                    RadarEmptyView(isScanning = isScanning)
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                        items(peers) { peer ->
                                            DeviceItem(
                                                peer = peer,
                                                enabled = true,
                                                onSend = { viewModel.sendToPeer(peer) }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null) return
        val uris = mutableListOf<Uri>()

        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                }
                if (uri != null) uris.add(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.filterIsInstance<Uri>()
                }
                if (list != null) uris.addAll(list)
            }
        }

        if (uris.isNotEmpty()) {
            viewModel.setFilesFromUris(uris, contentResolver)
        } else {
            finish()
        }
    }
}
