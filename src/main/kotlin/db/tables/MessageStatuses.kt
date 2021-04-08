package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.Notifier
import com.neelkamath.omniChatBackend.db.PostgresEnum
import com.neelkamath.omniChatBackend.db.messagesNotifier
import com.neelkamath.omniChatBackend.db.readUserIdList
import com.neelkamath.omniChatBackend.graphql.routing.MessageDateTimeStatus
import com.neelkamath.omniChatBackend.graphql.routing.MessageStatus
import com.neelkamath.omniChatBackend.graphql.routing.MessagesSubscription
import com.neelkamath.omniChatBackend.graphql.routing.UpdatedMessage
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
        fromDb = { MessageStatus.valueOf((it as String).toUpperCase()) },
        toDb = { PostgresEnum("message_status", it) },
    )

    /** The user recording the [status]. */
    private val userId: Column<Int> = integer("user_id").references(Users.id)

    /** When the [status] was recorded. */
    private val dateTime: Column<LocalDateTime> = datetime("date_time").clientDefault { LocalDateTime.now() }

    /**
     * Records that the [userId]'s [status] on the [messageId]. Clients who have [Notifier.subscribe]d to
     * [MessagesSubscription]s via [messagesNotifier] will be notified of the [messageId]'s [MessageStatus] update.
     *
     * If you record that the [userId] has [MessageStatus.READ] the [messageId], but haven't recorded that the [userId]
     * had a [messageId] [MessageStatus.DELIVERED], then it will also be recorded that the [userId] had a [messageId]
     * [MessageStatus.DELIVERED] too.
     *
     * An [IllegalArgumentException] will be thrown if:
     * - The [messageId] was sent by the [userId].
     * - The status has already been recorded (you can check if the [status] [isExisting]).
     * - The [messageId] isn't visible to the [userId] (you can check if the [Messages.isVisible]).
     */
    fun create(userId: Int, messageId: Int, status: MessageStatus) {
        require(Messages.isVisible(userId, messageId)) {
            """
            The user (ID: $userId) can't see the message (ID: $messageId) because it was sent before they deleted the
            chat.
            """
        }
        require(Messages.readMessage(userId, messageId).sender.id != userId) {
            "You cannot save a status for the user (ID: $userId) on their own message."
        }
        require(!isExisting(messageId, userId, status)) {
            val text = if (status == MessageStatus.DELIVERED) "delivered to" else "seen by"
            "The message (ID: $messageId) has already been $text the user (ID: $userId)."
        }
        if (status == MessageStatus.READ && !isExisting(messageId, userId, MessageStatus.DELIVERED))
            insertAndNotify(messageId, userId, MessageStatus.DELIVERED)
        insertAndNotify(messageId, userId, status)
    }

    /** Inserts the status into the table. Notifies subscribers of the [UpdatedMessage]s via [messagesNotifier]. */
    private fun insertAndNotify(messageId: Int, userId: Int, status: MessageStatus) {
        transaction {
            insert {
                it[this.messageId] = messageId
                it[this.userId] = userId
                it[this.status] = status
            }
        }
        val updates = readUserIdList(Messages.readChatIdFromMessageId(messageId))
            .associateWith { Messages.readMessage(it, messageId).toUpdatedMessage() }
        messagesNotifier.publish(updates)
    }

    /** Whether the [userId] has the specified [status] on the [messageId]. */
    fun isExisting(messageId: Int, userId: Int, status: MessageStatus): Boolean = transaction {
        select(
            (MessageStatuses.messageId eq messageId) and
                    (MessageStatuses.userId eq userId) and
                    (MessageStatuses.status eq status)
        ).empty().not()
    }

    /** Deletes [MessageStatuses] from the [messageIdList], ignoring invalid ones. */
    fun delete(messageIdList: Collection<Int>): Unit = transaction {
        deleteWhere { messageId inList messageIdList }
    }

    /** Convenience function for [delete]. */
    fun delete(vararg messageIdList: Int): Unit = delete(messageIdList.toList())

    /** Deletes every status the [userId] created in the [chatId]. */
    fun deleteUserChatStatuses(chatId: Int, userId: Int) = transaction {
        deleteWhere { (messageId inList Messages.readIdList(chatId)) and (MessageStatuses.userId eq userId) }
    }

    /** Deletes every status the [userId] created. Nothing happens if the [userId] doesn't exist. */
    fun deleteUserStatuses(userId: Int): Unit = transaction {
        deleteWhere { MessageStatuses.userId eq userId }
    }

    /** [messageId]'s [MessageDateTimeStatus]es. */
    fun read(messageId: Int): Set<MessageDateTimeStatus> = transaction {
        select(MessageStatuses.messageId eq messageId)
            .map { MessageDateTimeStatus(Users.read(it[userId]).toAccount(), it[dateTime], it[status]) }
            .toSet()
    }
}
