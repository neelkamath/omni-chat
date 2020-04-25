package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.GroupChat
import com.neelkamath.omniChat.GroupChatUpdate
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.GroupChats.adminUserId
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateStatement

/** The [GroupChatUsers] table contains the participants, including the [adminUserId], of a particular chat. */
object GroupChats : IntIdTable() {
    override val tableName get() = "group_chats"
    private val adminUserId = varchar("admin_user_id", Auth.userIdLength)
    const val maxTitleLength = 70
    private val title = varchar("title", maxTitleLength)
    const val maxDescriptionLength = 1000
    private val description = varchar("description", maxDescriptionLength).nullable()

    fun isUserInChat(userId: String, chatId: Int): Boolean = chatId in read(userId).map { it.id }

    /** Whether the [userId] is the admin of [chatId] (assumed to exist). */
    fun isAdmin(userId: String, chatId: Int): Boolean = Db.transact {
        select { id eq chatId }.first()[adminUserId] == userId
    }

    /**
     * Converts the current admin of [chatId] to a regular user, and sets the [newAdminUserId] as the new admin.
     *
     * It is assumed that the [newAdminUserId] is valid.
     */
    fun switchAdmin(chatId: Int, newAdminUserId: String): Unit = Db.transact {
        update({ id eq chatId }) { it[adminUserId] = newAdminUserId }
    }

    /** Returns the chat ID after creating it. */
    fun create(adminUserId: String, chat: NewGroupChat): Int = Db.transact {
        val groupId = insertAndGetId {
            it[this.adminUserId] = adminUserId
            it[title] = chat.title
            it[description] = chat.description
        }.value
        GroupChatUsers.create(groupId, chat.userIdList + adminUserId)
        groupId
    }

    fun read(chatId: Int): GroupChat = Db.transact {
        val row = select { id eq chatId }.first()
        val userIdList = GroupChatUsers.readUserIdList(chatId)
        GroupChat(chatId, row[adminUserId], userIdList, row[title], row[description])
    }

    /** Returns every chat the user is in. */
    fun read(userId: String): List<GroupChat> = Db.transact {
        GroupChatUsers.getChatIdList(userId).map { read(it) }
    }

    /** If every user is removed, the chat is deleted. */
    fun update(update: GroupChatUpdate): Unit = Db.transact {
        update({ id eq update.chatId }) { statement: UpdateStatement ->
            update.title?.let { statement[title] = it }
            update.description?.let { statement[description] = it }
        }
        update.newUserIdList.let { GroupChatUsers.addUsers(update.chatId, it) }
        update.removedUserIdList.let { GroupChatUsers.removeUsers(update.chatId, it) }
        update.newAdminId?.let { switchAdmin(update.chatId, update.newAdminId) }
        if (GroupChatUsers.readUserIdList(update.chatId).isEmpty()) deleteWhere { id eq update.chatId }
    }

    fun delete(chatId: Int): Unit = Db.transact {
        deleteWhere { id eq chatId }
    }

    /**
     * Searches the chats the [userId] is in.
     *
     * Returns the chat ID list by searching for the [query] in every chat's title case-insensitively.
     */
    fun search(userId: String, query: String): List<GroupChat> = Db.transact {
        val chatIdList = GroupChatUsers.getChatIdList(userId)
        selectAll()
            .filter { it[id].value in chatIdList && it[title].contains(query, ignoreCase = true) }
            .map {
                val chatId = it[id].value
                GroupChat(chatId, it[adminUserId], GroupChatUsers.readUserIdList(chatId), it[title], it[description])
            }
    }
}