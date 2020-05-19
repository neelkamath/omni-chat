package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Messages.chatId
import com.neelkamath.omniChat.db.Messages.id
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime

/**
 * The messages for [PrivateChats] and [GroupChats]. When messages were delivered or read are stored in
 * [MessageStatuses]. You can [subscribeToMessageUpdates].
 */
object Messages : IntIdTable() {
    private val chatId: Column<Int> = integer("chat_id")
    private val sent: Column<LocalDateTime> = datetime("sent").clientDefault { LocalDateTime.now() }
    private val senderId: Column<String> = varchar("sender_id", USER_ID_LENGTH)

    /** Text messages cannot exceed this length. */
    const val MAX_TEXT_LENGTH = 10_000

    /** Can have at most [MAX_TEXT_LENGTH]. */
    private val text: Column<String> = varchar("text", MAX_TEXT_LENGTH)

    private data class ChatMessage(val chatId: Int, val messageId: Int)

    /** Clients who have [subscribeToMessageUpdates] will be notified. */
    fun create(chatId: Int, userId: String, text: String) {
        val row = transact {
            insert {
                it[this.chatId] = chatId
                it[this.text] = text
                it[senderId] = userId
            }.resultedValues!![0]
        }
        notifyMessageUpdate(chatId, buildMessage(row))
    }

    /** Returns the [chatId]'s messages in the order of creation. */
    fun read(chatId: Int): List<Message> = transact {
        select { Messages.chatId eq chatId }.map(::buildMessage)
    }

    /**
     * Returns the [chatId]'s messages which haven't been deleted (such as through [PrivateChatDeletions]) by the
     * [userId].
     */
    fun read(chatId: Int, userId: String): List<Message> = transact {
        val deletion = PrivateChatDeletions.readLastDeletion(chatId, userId) ?: return@transact read(chatId)
        select { (Messages.chatId eq chatId) and (sent greaterEq deletion) }.map(::buildMessage)
    }

    fun readMessage(messageId: Int): Message = transact {
        select { Messages.id eq messageId }.first().let(::buildMessage)
    }

    /** Returns the ID of the chat which contains the [messageId]. */
    fun findChatFromMessage(messageId: Int): Int = transact {
        select { Messages.id eq messageId }.first()[chatId]
    }

    /** Returns the ID of the user who sent the [messageId]. */
    fun readSender(messageId: Int): String = transact {
        select { Messages.id eq messageId }.first()[senderId]
    }

    /**
     * Deletes all [Messages] and [MessageStatuses] in the [chatId]. Clients will be notified of a
     * [DeletionOfEveryMessage], and then [unsubscribeFromMessageUpdates].
     */
    fun deleteChat(chatId: Int) {
        val messageIdList = readMessageIdList(chatId)
        MessageStatuses.delete(messageIdList)
        transact {
            deleteWhere { Messages.chatId eq chatId }
        }
        notifyMessageUpdate(chatId, DeletionOfEveryMessage())
        unsubscribeFromMessageUpdates(chatId)
    }

    /** Deletes all [Messages] and [MessageStatuses] in the [chatId] [until] the specified [LocalDateTime]. */
    fun delete(chatId: Int, until: LocalDateTime) {
        val idList = readMessageIdList(chatId, sent less until)
        MessageStatuses.delete(idList)
        transact {
            deleteWhere { (Messages.chatId eq chatId) and (sent less until) }
        }
    }

    /**
     * Deletes all [Messages] and [MessageStatuses] the [userId] has in the [chatId]. Clients who have
     * [subscribeToMessageUpdates] will be be notified of the [UserChatMessagesRemoval].
     */
    fun delete(chatId: Int, userId: String) {
        val idList = readMessageIdList(chatId, senderId eq userId)
        MessageStatuses.delete(idList)
        transact {
            deleteWhere { Messages.id inList idList }
        }
        notifyMessageUpdate(chatId, UserChatMessagesRemoval(userId))
    }

    /**
     * Deletes all [Messages] and [MessageStatuses] the [userId] has sent. Clients who have [subscribeToMessageUpdates]
     * will be notified of the [UserChatMessagesRemoval].
     */
    fun delete(userId: String) {
        val messages = readChatMessages(userId)
        MessageStatuses.delete(messages.map { it.messageId })
        transact {
            deleteWhere { senderId eq userId }
        }
        messages.map { it.chatId }.toSet().forEach { notifyMessageUpdate(it, UserChatMessagesRemoval(userId)) }
    }

    /**
     * Deletes the message [id] in the [chatId] from [Messages] and [MessageStatuses]. Clients who have
     * [subscribeToMessageUpdates] be notified of the [DeletedMessage].
     */
    fun delete(id: Int) {
        MessageStatuses.delete(id)
        val chatId = findChatFromMessage(id)
        transact {
            deleteWhere { Messages.id eq id }
        }
        notifyMessageUpdate(chatId, DeletedMessage(id))
    }

    /** Returns every [ChatMessage] the [userId] created. */
    private fun readChatMessages(userId: String): List<ChatMessage> = transact {
        select { senderId eq userId }.map { ChatMessage(it[chatId], it[Messages.id].value) }
    }

    /** Whether there are messages in the [chatId] [from] the [LocalDateTime]. */
    fun existsFrom(chatId: Int, from: LocalDateTime): Boolean = transact {
        !select { (Messages.chatId eq chatId) and (sent greaterEq from) }.empty()
    }

    /** Returns the [id] list for the [chatId], which are optionally filtered by the [op]. */
    private fun readMessageIdList(chatId: Int, op: Op<Boolean>? = null): List<Int> = transact {
        val chatOp = Messages.chatId eq chatId
        val where = if (op == null) chatOp else chatOp and op
        select(where).map { it[Messages.id].value }
    }

    /** Whether the message [id] exists in the [chatId]. */
    fun exists(id: Int, chatId: Int): Boolean = transact {
        !select { (Messages.chatId eq chatId) and (Messages.id eq id) }.empty()
    }

    private fun buildMessage(row: ResultRow): Message {
        val id = row[id].value
        val dateTimes = MessageDateTimes(row[sent], MessageStatuses.read(id))
        return Message(id, row[senderId], row[text], dateTimes)
    }
}