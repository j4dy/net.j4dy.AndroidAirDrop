package net.j4dy.androidairdrop

import android.net.Uri
import net.j4dy.androidairdrop.core.archive.CpioArchiveUtil
import net.j4dy.androidairdrop.core.model.ShareEntity
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class CpioArchiveUtilTest {

    @Test
    fun testPackAndUnpackFiles() {
        val sample1Content = "Hello AirDrop from Android!".toByteArray(Charsets.UTF_8)
        val sample2Content = "Second test file content".toByteArray(Charsets.UTF_8)

        val files = listOf(
            ShareEntity(
                name = "hello.txt",
                size = sample1Content.size.toLong(),
                mimeType = "text/plain",
                openStream = { ByteArrayInputStream(sample1Content) }
            ),
            ShareEntity(
                name = "second.txt",
                size = sample2Content.size.toLong(),
                mimeType = "text/plain",
                openStream = { ByteArrayInputStream(sample2Content) }
            )
        )

        val out = ByteArrayOutputStream()
        var totalBytesReported = 0L

        CpioArchiveUtil.pack(files, out) { bytesRead ->
            totalBytesReported += bytesRead
        }

        val packedBytes = out.toByteArray()
        assertEquals((sample1Content.size + sample2Content.size).toLong(), totalBytesReported)

        // Read back the archive to verify integrity
        GzipCompressorInputStream(ByteArrayInputStream(packedBytes)).use { gzip ->
            CpioArchiveInputStream(gzip, "UTF-8").use { cpio ->
                val entry1 = cpio.nextCPIOEntry
                assertNotNull(entry1)
                assertEquals("hello.txt", entry1.name)
                assertEquals(sample1Content.size.toLong(), entry1.size)
                val read1 = cpio.readNBytes(sample1Content.size)
                assertArrayEquals(sample1Content, read1)

                val entry2 = cpio.nextCPIOEntry
                assertNotNull(entry2)
                assertEquals("second.txt", entry2.name)
                assertEquals(sample2Content.size.toLong(), entry2.size)
                val read2 = cpio.readNBytes(sample2Content.size)
                assertArrayEquals(sample2Content, read2)
            }
        }
    }
}
