package net.j4dy.androidairdrop.ui

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.j4dy.androidairdrop.core.discovery.DiscoveryEngine
import net.j4dy.androidairdrop.core.model.AirDropPeer
import net.j4dy.androidairdrop.core.model.ShareEntity
import net.j4dy.androidairdrop.core.model.TransferState
import net.j4dy.androidairdrop.core.protocol.AirDropClient
import net.j4dy.androidairdrop.service.TransferService
import java.io.InputStream

class TransferViewModel(application: Application) : AndroidViewModel(application) {

    private val discoveryEngine = DiscoveryEngine(application)
    private val airDropClient = AirDropClient()

    val peers: StateFlow<List<AirDropPeer>> = discoveryEngine.peersFlow
    val isScanning: StateFlow<Boolean> = discoveryEngine.isScanning

    private val _transferState = MutableStateFlow<TransferState>(TransferState.Idle)
    val transferState: StateFlow<TransferState> = _transferState.asStateFlow()

    private val _selectedFiles = MutableStateFlow<List<ShareEntity>>(emptyList())
    val selectedFiles: StateFlow<List<ShareEntity>> = _selectedFiles.asStateFlow()

    init {
        discoveryEngine.startDiscovery()
    }

    override fun onCleared() {
        super.onCleared()
        discoveryEngine.stopDiscovery()
    }

    fun restartDiscovery() {
        discoveryEngine.stopDiscovery()
        discoveryEngine.startDiscovery()
    }

    fun setFilesFromUris(uris: List<Uri>, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val entities = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri -> resolveUriToEntity(uri, contentResolver) }
            }
            _selectedFiles.value = entities
        }
    }

    fun clearSelectedFiles() {
        _selectedFiles.value = emptyList()
    }

    fun resetTransferState() {
        _transferState.value = TransferState.Idle
    }

    fun sendToPeer(peer: AirDropPeer) {
        val files = _selectedFiles.value
        if (files.isEmpty()) return

        viewModelScope.launch {
            try {
                TransferService.start(getApplication(), peer.name, files.size)

                _transferState.value = TransferState.Connecting
                val discoverResult = airDropClient.discover(peer.baseUrl)
                if (discoverResult.isFailure) {
                    _transferState.value = TransferState.Failed(
                        peer.name,
                        discoverResult.exceptionOrNull()?.message ?: "Device unreachable"
                    )
                    TransferService.stop(getApplication())
                    return@launch
                }

                _transferState.value = TransferState.WaitingForAcceptance(peer.name)
                val askResult = airDropClient.ask(
                    baseUrl = peer.baseUrl,
                    files = files,
                    senderName = Build.MODEL ?: "Android"
                )

                if (askResult.isFailure) {
                    _transferState.value = TransferState.Failed(
                        peer.name,
                        askResult.exceptionOrNull()?.message ?: "Declined by receiver"
                    )
                    TransferService.stop(getApplication())
                    return@launch
                }

                val totalBytes = files.sumOf { it.size.coerceAtLeast(0) }
                _transferState.value = TransferState.Uploading(peer.name, 0, totalBytes)

                var lastTime = System.currentTimeMillis()
                var lastBytes = 0L

                val uploadResult = airDropClient.upload(peer.baseUrl, files) { bytesRead, total ->
                    val now = System.currentTimeMillis()
                    val timeDelta = (now - lastTime).coerceAtLeast(1)
                    val speed = if (timeDelta > 500) {
                        val currentSpeed = ((bytesRead - lastBytes) * 1000) / timeDelta
                        lastTime = now
                        lastBytes = bytesRead
                        currentSpeed
                    } else {
                        0L
                    }

                    _transferState.value = TransferState.Uploading(
                        targetName = peer.name,
                        bytesTransferred = bytesRead,
                        totalBytes = total,
                        speedBytesPerSec = speed
                    )
                }

                if (uploadResult.isSuccess) {
                    _transferState.value = TransferState.Success(peer.name, files.size)
                } else {
                    _transferState.value = TransferState.Failed(
                        peer.name,
                        uploadResult.exceptionOrNull()?.message ?: "Upload failed"
                    )
                }
            } catch (e: Exception) {
                _transferState.value = TransferState.Failed(peer.name, e.message ?: "Transfer error")
            } finally {
                TransferService.stop(getApplication())
            }
        }
    }

    private fun resolveUriToEntity(uri: Uri, contentResolver: ContentResolver): ShareEntity? {
        var name = "file"
        var size = -1L
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }

        return ShareEntity(
            uri = uri,
            name = name,
            size = size,
            mimeType = mimeType,
            openStream = { contentResolver.openInputStream(uri) ?: InputStream.nullInputStream() }
        )
    }
}
