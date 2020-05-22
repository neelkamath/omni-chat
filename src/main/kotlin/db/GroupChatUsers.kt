package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.USER_ID_LENGTH
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*

object GroupChatUsers : IntIdTable() {
    override val tableName get() = "group_chat_users"
    private val userId: Column<String> = varchar("user_id", USER_ID_LENGTH)
    private val groupChatId: Column<Int> = integer("group_chat_id").references(GroupChats.id)

    private fun isUserInChat(groupChatId: Int, userId: String): Boolean = transact {
        !select { (GroupChatUsers.groupChatId eq groupChatId) and (GroupChatUsers.userId eq userId) }.empty()
    }

    /** Returns the user ID list from the specified [groupChatId]. */
    fun readUserIdList(groupChatId: Int): Set<String> = transact {
        select { GroupChatUsers.groupChatId eq groupChatId }.map { it[userId] }.toSet()
    }

    /** Adds every user in the [userIdList] to the [groupChatId] if they aren't in it. */
    fun addUsers(groupChatId: Int, userIdList: Set<String>) {
        val users = userIdList.filterNot { isUserInChat(groupChatId, it) }
        transact {
            batchInsert(users) {
                this[GroupChatUsers.groupChatId] = groupChatId
                this[userId] = it
            }
        }
    }

    /**
     * Removes users in the [userIdList] from the [chatId], ignoring the IDs of users who aren't in the chat. If every
     * user is removed, the [chatId] will be [GroupChats.delete]d.
     *
     * If the chat is deleted, it will be deleted from [GroupChats], [GroupChatUsers], [Messages], and
     * [MessageStatuses]. Users will be [unsubscribeFromMessageUpdates]d.
     */
    fun removeUsers(chatId: Int, userIdList: Set<String>) {
        transact {
            deleteWhere { (groupChatId eq chatId) and (userId inList userIdList) }
        }
        userIdList.forEach { unsubscribeFromMessageUpdates(it, chatId) }
        if (readUserIdList(chatId).isEmpty()) GroupChats.delete(chatId)
    }

    /** Returns the chat ID list of every chat the [userId] is in. */
    fun readChatIdList(userId: String): List<Int> = transact {
        select { GroupChatUsers.userId eq userId }.map { it[groupChatId] }
    }
}