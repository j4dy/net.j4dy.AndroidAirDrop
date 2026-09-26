package net.j4dy.androidairdrop

import android.net.Uri
import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.PropertyListParser
import net.j4dy.androidairdrop.core.model.ShareEntity
import net.j4dy.androidairdrop.core.protocol.PlistHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class PlistHelperTest {

    @Test
    fun testDiscoverPayloadHeader() {
        val bytes = PlistHelper.createDiscoverPayload()
        assertTrue("Payload should not be empty", bytes.isNotEmpty())
        val magic = String(bytes.copyOfRange(0, 8))
        assertEquals("Should start with bplist00 magic", "bplist00", magic)
    }

    @Test
    fun testAskPayloadStructure() {
        val files = listOf(
            ShareEntity(
                name = "test_image.jpg",
                size = 1024L,
                mimeType = "image/jpeg",
                openStream = { ByteArrayInputStream(ByteArray(0)) }
            ),
            ShareEntity(
                name = "document.pdf",
                size = 2048L,
                mimeType = "application/pdf",
                openStream = { ByteArrayInputStream(ByteArray(0)) }
            )
        )

        val bytes = PlistHelper.createAskPayload(
            senderName = "TestPixel",
            files = files
        )

        assertTrue(bytes.isNotEmpty())
        val magic = String(bytes.copyOfRange(0, 8))
        assertEquals("bplist00", magic)

        val parsed = PropertyListParser.parse(bytes) as NSDictionary
        assertEquals("TestPixel", parsed.objectForKey("SenderComputerName").toString())
        assertEquals("Android", parsed.objectForKey("SenderModelName").toString())

        val filesArray = parsed.objectForKey("Files") as NSArray
        assertEquals(2, filesArray.count())

        val firstFile = filesArray.objectAtIndex(0) as NSDictionary
        assertEquals("test_image.jpg", firstFile.objectForKey("FileName").toString())
        assertEquals("1024", firstFile.objectForKey("FileLength").toString())
    }
}
