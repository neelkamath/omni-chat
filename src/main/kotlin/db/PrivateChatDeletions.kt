package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.USER_ID_LENGTH
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime

/**
 * Stores when users delete their chats.
 *
 * When a user deletes a chat, the chat is only deleted for themselves. The person they were chatting with still has the
 * chat in its original condition. If the person they were chatting with sends them a message, it will appear as the
 * first message in a new chat for the user.
 */
object PrivateChatDeletions : IntIdTable() {
    override val tableName get() = "private_chat_deletions"
    private val chatId: Column<Int> = integer("chat_id").references(PrivateChats.id)
    private val dateTime: Column<LocalDateTime> = datetime("date_time").clientDefault { LocalDateTime.now() }
    private val userId: Column<String> = varchar("user_id", USER_ID_LENGTH)

    /**
     * Deletes the [chatId] for the [userId].
     *
     * Commonly deleted [Messages] and [MessageStatuses] are deleted. The user's older [PrivateChatDeletions] are
     * deleted. If both the users have deleted the [chatId], and there has been no activity in the [chatId] after
     * they've deleted it, the [chatId] is deleted from [PrivateChats].
     */
    fun create(chatId: Int, userId: String) {
        insert(chatId, userId)
        deleteUnusedChatData(chatId, userId)
    }

    /** Records in the DB that the [userId] deleted the [chatId]. */
    private fun insert(chatId: Int, userId: String): Unit = transact {
        insert {
            it[this.chatId] = chatId
            it[this.userId] = userId
        }
    }

    /**
     * Commonly deleted [Messages] and [MessageStatuses] are deleted. The user's older [PrivateChatDeletions] are
     * deleted. If both the users have deleted the [chatId], and there has been no activity in the [chatId] after
     * they've deleted it, the [chatId] is deleted from [PrivateChats].
     */
    private fun deleteUnusedChatData(chatId: Int, userId: String) {
        deleteCommonlyDeletedMessages(chatId)
        deletePreviousDeletionRecords(chatId, userId)
        if (isChatDeleted(chatId)) {
            delete(chatId)
            PrivateChats.delete(chatId)
        }
    }

    /**
     * Whether both the users have deleted the [chatId], and there has been no activity in the chat after they've
     * deleted it.
     */
    private fun isChatDeleted(chatId: Int): Boolean {
        val (user1Id, user2Id) = PrivateChats.readUsers(chatId)
        return isDeleted(user1Id, chatId) && isDeleted(user2Id, chatId)
    }

    /** Deletes [Messages] and [MessageStatuses] deleted by both users. */
    private fun deleteCommonlyDeletedMessages(chatId: Int) {
        readLastDeletion(chatId)?.let { Messages.delete(chatId, until = it) }
    }

    /** Deletes every private chat deletion record the [userId] has in the [chatId] except for the latest one. */
    private fun deletePreviousDeletionRecords(chatId: Int, userId: String): Unit = transact {
        val idList = select { (PrivateChatDeletions.chatId eq chatId) and (PrivateChatDeletions.userId eq userId) }
            .toList()
            .dropLast(1)
            .map { it[PrivateChatDeletions.id].value }
        deleteWhere { PrivateChatDeletions.id inList idList }
    }

    /** Returns the last [LocalDateTime] both users deleted the [chatId], if both of them have. */
    private fun readLastDeletion(chatId: Int): LocalDateTime? = transact {
        val deletions = select { PrivateChatDeletions.chatId eq chatId }
        val userIdList = deletions.map { it[userId] }.toSet()
        if (userIdList.size < 2) return@transact null
        val getDateTime = { index: Int ->
            deletions.last { it[userId] == userIdList.elementAt(index) }[dateTime]
        }
        listOf(getDateTime(0), getDateTime(1)).min()
    }

    /** Returns the last time the [userId] deleted the [chatId], if they ever did. */
    fun readLastDeletion(chatId: Int, userId: String): LocalDateTime? = transact {
        select { (PrivateChatDeletions.chatId eq chatId) and (PrivateChatDeletions.userId eq userId) }
            .lastOrNull()
            ?.get(dateTime)
    }

    /**
     * Whether the [userId] has deleted the [chatId]. This will be `true` only if the user has deleted the chat, and
     * neither user in the chat have messaged after that.
     */
    fun isDeleted(userId: String, chatId: Int): Boolean = transact {
        val deletions = select { (PrivateChatDeletions.chatId eq chatId) and (PrivateChatDeletions.userId eq userId) }
        if (deletions.empty())
            false
        else {
            val lastDeletion = deletions.last { it[PrivateChatDeletions.userId] == userId }
            !Messages.existsFrom(chatId, lastDeletion[dateTime])
        }
    }

    /** Deletes every record of chat deletions for the [chatId]. */
    fun delete(chatId: Int): Unit = transact {
        deleteWhere { PrivateChatDeletions.chatId eq chatId }
    }
}