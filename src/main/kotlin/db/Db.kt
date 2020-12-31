package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.db.Pic.Companion.MAX_BYTES
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.routing.DeletedContact
import com.neelkamath.omniChat.graphql.routing.DeletionOfEveryMessage
import com.neelkamath.omniChat.graphql.routing.MessageEdge
import com.neelkamath.omniChat.graphql.routing.UserChatMessagesRemoval
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.LikeOp
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.lowerCase
import org.postgresql.util.PGobject
import javax.annotation.processing.Generated

val db: Database by lazy {
    val url = System.getenv("POSTGRES_URL")
    val db = System.getenv("POSTGRES_DB")
    Database.connect(
            "jdbc:postgresql://$url/$db?reWriteBatchedInserts=true",
            "org.postgresql.Driver",
            System.getenv("POSTGRES_USER"),
            System.getenv("POSTGRES_PASSWORD")
    )
}

data class ChatEdges(val chatId: Int, val edges: List<MessageEdge>)

data class ForwardPagination(val first: Int? = null, val after: Int? = null)

data class BackwardPagination(val last: Int? = null, val before: Int? = null)

/** Throws an [IllegalArgumentException] if the [bytes] exceeds [Audio.MAX_BYTES]. */
data class Audio(
        /** At most [Audio.MAX_BYTES]. */
        val bytes: ByteArray,
        val type: Type
) {
    init {
        if (bytes.size > MAX_BYTES) throw IllegalArgumentException("The audio mustn't exceed $MAX_BYTES bytes.")
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Audio

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    enum class Type {
        MP3, MP4;

        companion object {
            /** Throws an [IllegalArgumentException] if the [extension] (e.g., `"m4a"`) isn't one of the [Type]s. */
            fun build(extension: String): Type = when (extension.toLowerCase()) {
                "mp3" -> MP3
                "mp4", "m4a", "m4p", "m4b", "m4r", "m4v" -> MP4
                else ->
                    throw IllegalArgumentException("The audio ($extension) must be one of ${values().joinToString()}.")
            }
        }
    }

    companion object {
        const val MAX_BYTES = 25 * 1024 * 1024
    }
}

/** Throws an [IllegalArgumentException] if the [bytes] exceeds [Pic.MAX_BYTES]. */
data class Pic(
        /** At most [MAX_BYTES]. */
        val bytes: ByteArray,
        val type: Type
) {
    init {
        if (bytes.size > MAX_BYTES) throw IllegalArgumentException("The pic mustn't exceed $MAX_BYTES bytes.")
    }

    enum class Type {
        PNG, JPEG;

        companion object {
            /** Throws an [IllegalArgumentException] if the [extension] (e.g., `"pjpeg"`) isn't one of the [Type]s. */
            fun build(extension: String): Type = when (extension.toLowerCase()) {
                "png" -> PNG
                "jpg", "jpeg", "jfif", "pjpeg", "pjp" -> JPEG
                else ->
                    throw IllegalArgumentException("The pic ($extension) must be one of ${values().joinToString()}.")
            }
        }
    }

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pic

        if (!bytes.contentEquals(other.bytes)) return false
        if (type != other.type) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    companion object {
        const val MAX_BYTES = 25 * 1024 * 1024
    }
}

enum class MessageType { TEXT, ACTION, PIC, AUDIO, VIDEO, DOC, POLL, GROUP_CHAT_INVITE }

/**
 * Required for enums (see https://github.com/JetBrains/Exposed/wiki/DataTypes#how-to-use-database-enum-types). It's
 * assumed that all enum values are lowercase in the DB.
 */
class PostgresEnum<T : Enum<T>>(postgresName: String, kotlinName: T?) : PGobject() {
    init {
        type = postgresName
        value = kotlinName?.name?.toLowerCase()
    }
}

/** Connects to the DB. This is safe to call multiple times. */
fun setUpDb() {
    db
}

/** Case-insensitively checks if [this] contains the [pattern]. */
infix fun Expression<String>.iLike(pattern: String): LikeOp = lowerCase() like "%${pattern.toLowerCase()}%"

/**
 * Whether the [userId] is in the specified private or group chat (the [chatId] needn't be valid). Private chats the
 * [userId] deleted are included.
 */
fun isUserInChat(userId: Int, chatId: Int): Boolean =
        chatId in PrivateChats.readIdList(userId) + GroupChatUsers.readChatIdList(userId)

fun readUserIdList(chatId: Int): List<Int> =
        if (PrivateChats.exists(chatId)) PrivateChats.readUserIdList(chatId) else GroupChatUsers.readUserIdList(chatId)

/** Returns the ID of every user who the [userId] has a chat with (deleted private chats aren't included). */
fun readChatSharers(userId: Int): List<Int> =
        PrivateChats.readOtherUserIdList(userId) + GroupChatUsers.readFellowParticipants(userId)

/**
 * Deletes the [userId]'s data from the DB, and [Notifier.unsubscribe]s them from all notifiers. An
 * [IllegalArgumentException] will be thrown if the not [GroupChatUsers.canUserLeave]. Nothing will happen if the
 * [userId] doesn't exist.
 *
 * ## Users
 *
 * - The [userId] will be deleted from the [Users].
 * - Clients who have [Notifier.subscribe]d via [groupChatsNotifier] will be [Notifier.unsubscribe]d.
 *
 * ## Contacts
 *
 * - The user's [Contacts] will be deleted.
 * - Everyone's [Contacts] of the user will be deleted.
 * - Subscribers who have the [userId] in their contacts will be notified of this [DeletedContact] via [accountsNotifier].
 * - The [userId] will be unsubscribed via [accountsNotifier].
 *
 * ## Private Chats
 *
 * - Deletes every record the [userId] has in [PrivateChats] and [PrivateChatDeletions].
 * - Subscribers will be notified of a [DeletionOfEveryMessage] via [messagesNotifier].
 *
 * ## Group Chats
 *
 * - The [userId] will be removed from [GroupChats] they're in.
 * - If they're the last user in the group chat, the chat will be deleted from [GroupChats], [GroupChatUsers],
 *   [Messages], and [MessageStatuses].
 * - Clients will be [Notifier.unsubscribe]d via [groupChatsNotifier].
 *
 * ## Messages
 *
 * - Clients who have [Notifier.subscribe]d to [messagesNotifier]s will be notified of the [UserChatMessagesRemoval].
 * - Deletes all [Messages] and [MessageStatuses] the [userId] has sent.
 * - Clients will be [Notifier.unsubscribe]d via [messagesNotifier].
 *
 * ## Typing Statuses
 *
 * - Deletes [TypingStatuses] the [userId] created.
 * - The [userId] will be [Notifier.unsubscribe]d via [typingStatusesNotifier].
 */
fun deleteUser(userId: Int) {
    if (!GroupChatUsers.canUserLeave(userId))
        throw IllegalArgumentException(
                """
                The user's (ID: $userId) data can't be deleted because they're the last admin of a group chat with other 
                users.
                """
        )
    Contacts.deleteUserEntries(userId)
    PrivateChats.deleteUserChats(userId)
    GroupChatUsers.removeUser(userId)
    TypingStatuses.deleteUser(userId)
    Messages.deleteUserMessages(userId)
    Users.delete(userId)
    groupChatsNotifier.unsubscribe { it == userId }
    accountsNotifier.unsubscribe { it == userId }
    typingStatusesNotifier.unsubscribe { it == userId }
    messagesNotifier.unsubscribe { it == userId }
    onlineStatusesNotifier.unsubscribe { it == userId }
}
