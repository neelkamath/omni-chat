package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.MessageDateTimeStatus
import com.neelkamath.omniChat.MessageStatus
import com.neelkamath.omniChat.MessagesSubscription
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.readUserById
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/** When [Messages] were delivered and read. */
object MessageStatuses : Table() {
    override val tableName = "message_statuses"
    private val messageId: Column<Int> = integer("message_id").references(Messages.id)
    private val status: Column<MessageStatus> = customEnumeration(
        name = "status",
        sql = "message_status",
        fromDb = {
            val status = it as String
            MessageStatus.valueOf(status.toUpperCase())
        },
        toDb = { PostgresEnum("message_status", it) }
    )

    /** The user recording the [status]. */
    private val userId: Column<Int> = integer("user_id").references(Users.id)

    /** When the [status] was recorded. */
    private val dateTime: Column<LocalDateTime> = datetime("date_time").clientDefault { LocalDateTime.now() }

    /**
     * Records that the [userId]'s [status] on the [messageId]. Clients who have [Broker.subscribe]d to
     * [MessagesSubscription]s via [messagesBroker] will be notified of the [messageId]'s [MessageStatus] update.
     *
     * If you record that the [userId] has [MessageStatus.READ] the [messageId], but haven't recorded that the [userId]
     * had a [messageId] [MessageStatus.DELIVERED], then it will also be recorded that the [userId] had a [messageId]
     * [MessageStatus.DELIVERED] too.
     *
     * @throws [IllegalArgumentException] if:
     * - The [messageId] was sent by the [userId].
     * - The status has already been recorded (you can check if the [status] [exists]).
     * - The [messageId] isn't visible to the [userId] (you can check if the [Messages.isVisible]).
     */
    fun create(messageId: Int, userId: Int, status: MessageStatus) {
        if (!Messages.isVisible(messageId, userId))
            throw IllegalArgumentException(
                """
                The user (ID: $userId) can't see the message (ID: $messageId) because it was sent before they deleted 
                the chat.
                """.trimIndent()
            )
        if (Messages.read(messageId).sender.id == userId)
            throw IllegalArgumentException("You cannot save a status for the user (ID: $userId) on their own message.")
        if (exists(messageId, userId, status)) {
            val text = if (status == MessageStatus.DELIVERED) "delivered to" else "seen by"
            throw IllegalArgumentException(
                "The message (ID: $messageId) has already been $text the user (ID: $userId)."
            )
        }
        if (status == MessageStatus.READ && !exists(messageId, userId, MessageStatus.DELIVERED))
            insertAndNotify(messageId, userId, MessageStatus.DELIVERED)
        insertAndNotify(messageId, userId, status)
    }

    /**
     * Inserts the status into the DB, and notifies clients who have [Broker.subscribe]d to [MessagesSubscription]s via
     * [messagesBroker].
     */
    private fun insertAndNotify(messageId: Int, userId: Int, status: MessageStatus) {
        transaction {
            insert {
                it[MessageStatuses.messageId] = messageId
                it[MessageStatuses.userId] = userId
                it[MessageStatuses.status] = status
            }
        }
        val chatId = Messages.readChatFromMessage(messageId)
        messagesBroker.notify(Messages.read(messageId).toUpdatedMessage()) { isUserInChat(userId, chatId) }
    }

    /** Whether the [userId] has the specified [status] on the [messageId]. */
    fun exists(messageId: Int, userId: Int, status: MessageStatus): Boolean = !transaction {
        select {
            (MessageStatuses.messageId eq messageId) and
                    (MessageStatuses.userId eq userId) and
                    (MessageStatuses.status eq status)
        }.empty()
    }

    /** Deletes [MessageStatuses] from the [messageIdList], ignoring invalid ones. */
    fun delete(messageIdList: List<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }

    /** Convenience function for [delete]. */
    fun delete(vararg messageIdList: Int): Unit = delete(messageIdList.toList())

    /** Deletes every status the [userId] created in the [chatId]. */
    fun deleteUserChatStatuses(chatId: Int, userId: Int) = transaction {
        deleteWhere { (messageId inList Messages.readIdList(chatId)) and (MessageStatuses.userId eq userId) }
    }

    /** Deletes every status the [userId] created. */
    fun deleteUserStatuses(userId: Int): Unit = transaction {
        deleteWhere { MessageStatuses.userId eq userId }
    }

    /** [messageId]'s [MessageDateTimeStatus]es. */
    fun read(messageId: Int): List<MessageDateTimeStatus> = transaction {
        select { MessageStatuses.messageId eq messageId }
            .map { MessageDateTimeStatus(readUserById(it[userId]), it[dateTime], it[status]) }
    }
}