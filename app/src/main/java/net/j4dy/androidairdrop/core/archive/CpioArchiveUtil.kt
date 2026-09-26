package net.j4dy.androidairdrop.core.archive

import net.j4dy.androidairdrop.core.model.ShareEntity
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry
import org.apache.commons.compress.archivers.cpio.CpioArchiveOutputStream
import org.apache.commons.compress.archivers.cpio.CpioConstants.C_IRGRP
import org.apache.commons.compress.archivers.cpio.CpioConstants.C_IROTH
import org.apache.commons.compress.archivers.cpio.CpioConstants.C_IRUSR
import org.apache.commons.compress.archivers.cpio.CpioConstants.C_ISREG
import org.apache.commons.compress.archivers.cpio.CpioConstants.C_IWUSR
import org.apache.commons.compress.archivers.cpio.CpioConstants.FORMAT_OLD_ASCII
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

object CpioArchiveUtil {

    fun pack(
        entities: List<ShareEntity>,
        output: OutputStream,
        onBytesRead: (bytesRead: Long) -> Unit = {}
    ) {
        GzipCompressorOutputStream(output).use { gzip ->
            CpioArchiveOutputStream(gzip, FORMAT_OLD_ASCII, 512, "UTF-8").use { cpio ->
                val buffer = ByteArray(64 * 1024)
                for (entity in entities) {
                    val entry = CpioArchiveEntry(FORMAT_OLD_ASCII, entity.name)
                    entry.mode = (C_ISREG or C_IRUSR or C_IWUSR or C_IRGRP or C_IROTH).toLong()
                    entry.time = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())

                    if (entity.size >= 0) {
                        entry.size = entity.size
                    }

                    cpio.putArchiveEntry(entry)
                    entity.openStream().use { inputStream ->
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            cpio.write(buffer, 0, read)
                            onBytesRead(read.toLong())
                        }
                    }
                    cpio.closeArchiveEntry()
                }
            }
        }
    }
}
