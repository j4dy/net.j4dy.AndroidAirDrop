package net.j4dy.androidairdrop.core.protocol

import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.PropertyListParser
import net.j4dy.androidairdrop.core.model.ShareEntity
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

object PlistHelper {

    fun createDiscoverPayload(): ByteArray {
        val root = NSDictionary()
        val out = ByteArrayOutputStream()
        PropertyListParser.saveAsBinary(root, out)
        return out.toByteArray()
    }

    fun createAskPayload(
        senderName: String,
        senderModel: String = "Android",
        files: List<ShareEntity>,
        senderId: String = UUID.randomUUID().toString()
    ): ByteArray {
        val root = NSDictionary()
        root.put("SenderComputerName", senderName)
        root.put("SenderModelName", senderModel)
        root.put("SenderID", senderId)

        val filesArray = NSArray(files.size)
        files.forEachIndexed { index, file ->
            val fileDict = NSDictionary()
            fileDict.put("FileName", file.name)
            fileDict.put("FileLength", file.size)
            fileDict.put("FileType", file.mimeType.ifEmpty { "application/octet-stream" })
            filesArray.setValue(index, fileDict)
        }
        root.put("Files", filesArray)

        val out = ByteArrayOutputStream()
        PropertyListParser.saveAsBinary(root, out)
        return out.toByteArray()
    }

    fun parseResponse(inputStream: InputStream): NSDictionary {
        val parsed = PropertyListParser.parse(inputStream)
        return parsed as? NSDictionary ?: NSDictionary()
    }
}
