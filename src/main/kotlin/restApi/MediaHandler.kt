package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.db.Audio
import com.neelkamath.omniChatBackend.db.ImageType
import com.neelkamath.omniChatBackend.db.ProcessedImage
import com.neelkamath.omniChatBackend.db.isUserInChat
import com.neelkamath.omniChatBackend.db.tables.Doc
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.db.tables.Mp4
import com.neelkamath.omniChatBackend.userId
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import java.io.File
import javax.annotation.processing.Generated

/** The [bytes] are the file's contents. An example [extension] is `"mp4"`. */
data class TypedFile(val extension: String, val bytes: ByteArray) {
    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TypedFile

        if (extension != other.extension) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        var result = extension.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

/**
 * The `imageType` argument passed to the [bytesReader] will be `null` if no `image-type` query parameter was passed.
 */
inline fun getMediaMessage(
    route: Route,
    crossinline bytesReader: (messageId: Int, imageType: ImageType?) -> ByteArray,
): Unit = with(route) {
    get {
        val messageId = call.parameters["message-id"]!!.toInt()
        val imageType = call.parameters["image-type"]?.let(ImageType::valueOf)
        if (Messages.isVisible(call.userId, messageId)) call.respondBytes(bytesReader(messageId, imageType))
        else call.respond(HttpStatusCode.Unauthorized)
    }
}

fun <T> postMediaMessage(
    route: Route,
    messageReader: suspend PipelineContext<Unit, ApplicationCall>.() -> T?,
    creator: (userId: Int, chatId: Int, message: T, contextMessageId: Int?) -> Unit,
): Unit = with(route) {
    post {
        val chatId = call.parameters["chat-id"]!!.toInt()
        val contextMessageId = call.parameters["context-message-id"]?.toInt()
        val message = messageReader(this)
        when {
            !isUserInChat(call.userId!!, chatId) -> call.respond(
                HttpStatusCode.BadRequest,
                InvalidMediaMessage(InvalidMediaMessage.Reason.USER_NOT_IN_CHAT),
            )

            message == null ->
                call.respond(HttpStatusCode.BadRequest, InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_FILE))

            !Messages.isValidContext(call.userId!!, chatId, contextMessageId) -> call.respond(
                HttpStatusCode.BadRequest,
                InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_CONTEXT_MESSAGE),
            )

            Messages.isInvalidBroadcast(call.userId!!, chatId) ->
                call.respond(HttpStatusCode.BadRequest, InvalidMediaMessage(InvalidMediaMessage.Reason.MUST_BE_ADMIN))

            else -> {
                creator(call.userId!!, chatId, message, contextMessageId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. `null` will be returned if
 * the [Mp4] is invalid.
 */
suspend fun PipelineContext<Unit, ApplicationCall>.readMultipartMp4(): Mp4? {
    val (extension, bytes) = readMultipartFile()
    return if (extension.lowercase() != "mp4" || bytes.size > Mp4.MAX_BYTES) null else Mp4(bytes)
}

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. `null` will be returned if
 * the [Doc] is invalid.
 */
suspend fun PipelineContext<Unit, ApplicationCall>.readMultipartDoc(): Doc? {
    val (_, bytes) = readMultipartFile()
    return if (bytes.size > Doc.MAX_BYTES) null else Doc(bytes)
}

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. `null` will be returned if
 * the [Audio] is invalid.
 */
suspend fun PipelineContext<Unit, ApplicationCall>.readMultipartAudio(): Audio? {
    val (extension, bytes) = readMultipartFile()
    if (!Audio.isValidExtension(extension) || bytes.size > Audio.MAX_BYTES) return null
    return Audio(bytes)
}

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. `null` will be returned if
 * the [ProcessedImage] is invalid.
 */
suspend fun PipelineContext<Unit, ApplicationCall>.readMultipartImage(): ProcessedImage? {
    val (extension, bytes) = readMultipartFile()
    return try {
        ProcessedImage.build(extension, bytes)
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** Receives a multipart request with only one part, where the part is a [PartData.FileItem]. */
private suspend fun PipelineContext<Unit, ApplicationCall>.readMultipartFile(): TypedFile {
    val part = call.receiveMultipart().readPart()!! as PartData.FileItem
    val extension = File(part.originalFileName!!).extension
    val bytes = part.streamProvider().use { it.readBytes() }
    part.dispose()
    return TypedFile(extension, bytes)
}
