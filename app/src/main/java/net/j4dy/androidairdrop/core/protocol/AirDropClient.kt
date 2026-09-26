package net.j4dy.androidairdrop.core.protocol

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.j4dy.androidairdrop.core.archive.CpioArchiveUtil
import net.j4dy.androidairdrop.core.model.ShareEntity
import net.j4dy.androidairdrop.core.network.AirDropTrustManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.IOException
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

class AirDropClient(networkInterface: NetworkInterface? = null) {

    private val socketFactory = AirDropTrustManager.LinkLocalSocketFactory(networkInterface)

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .socketFactory(socketFactory)
        .sslSocketFactory(
            AirDropTrustManager.createClientSSLSocketFactory(),
            AirDropTrustManager.trustAllCerts
        )
        .hostnameVerifier { _, _ -> true }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS) // Allow time for Mac user to accept notification prompt
        .writeTimeout(300, TimeUnit.SECONDS) // Allow streaming large files
        .build()

    fun setNetworkInterface(iface: NetworkInterface?) {
        socketFactory.setNetworkInterface(iface)
    }

    suspend fun discover(baseUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val payload = PlistHelper.createDiscoverPayload()
            val request = Request.Builder()
                .url("$baseUrl/Discover")
                .post(payload.toRequestBody("application/octet-stream".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("Discover failed with HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ask(
        baseUrl: String,
        files: List<ShareEntity>,
        senderName: String = Build.MODEL
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val payload = PlistHelper.createAskPayload(senderName = senderName, files = files)
            val request = Request.Builder()
                .url("$baseUrl/Ask")
                .post(payload.toRequestBody("application/octet-stream".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else if (response.code == 401 || response.code == 403 || response.code == 500) {
                    Result.failure(IOException("Transfer was declined by receiver (${response.code})"))
                } else {
                    Result.failure(IOException("Ask failed with HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upload(
        baseUrl: String,
        files: List<ShareEntity>,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val totalBytes = files.sumOf { it.size.coerceAtLeast(0) }
            var transferredBytes = 0L

            val streamingBody = object : RequestBody() {
                override fun contentType() = "application/x-cpio".toMediaType()

                override fun writeTo(sink: BufferedSink) {
                    CpioArchiveUtil.pack(files, sink.outputStream()) { readChunk ->
                        transferredBytes += readChunk
                        onProgress(transferredBytes, totalBytes)
                    }
                }
            }

            val request = Request.Builder()
                .url("$baseUrl/Upload")
                .post(streamingBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("Upload failed with HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
