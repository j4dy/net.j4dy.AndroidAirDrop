package net.j4dy.androidairdrop.core.model

import android.net.Uri
import java.io.InputStream

data class ShareEntity(
    val uri: Uri? = null,
    val name: String,
    val size: Long,
    val mimeType: String,
    val openStream: () -> InputStream
)

data class AirDropPeer(
    val id: String,
    val name: String,
    val hostAddress: String,
    val port: Int = 8770,
    val model: String? = null,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
) {
    val baseUrl: String
        get() = "https://$hostAddress:$port"
}

sealed interface TransferState {
    data object Idle : TransferState
    data object Connecting : TransferState
    data class WaitingForAcceptance(val targetName: String) : TransferState
    data class Uploading(
        val targetName: String,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long = 0
    ) : TransferState
    data class Success(val targetName: String, val fileCount: Int) : TransferState
    data class Failed(val targetName: String, val reason: String) : TransferState
}
