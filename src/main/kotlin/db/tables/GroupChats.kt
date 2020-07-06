package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import org.jetbrains.exposed.sql.*

/** The [GroupChatUsers] table contains the participants. [GroupChats] have [Messages]. */
object GroupChats : Table() {
    override val tableName get() = "group_chats"
    val id: Column<Int> = integer("id").uniqueIndex().references(Chats.id)
    private val adminId: Column<String> = varchar("admin_id", USER_ID_LENGTH)

    /** Titles cannot exceed this length. */
    const val MAX_TITLE_LENGTH = 70

    /** Can have at most [MAX_TITLE_LENGTH]. */
    private val title: Column<String> = varchar("title", MAX_TITLE_LENGTH)

    /** Descriptions cannot exceed this length. */
    const val MAX_DESCRIPTION_LENGTH = 1000

    /** Can have at most [MAX_DESCRIPTION_LENGTH]. */
    private val description: Column<String> = varchar("description", MAX_DESCRIPTION_LENGTH)

    /** Whether the [userId] is the admin of [chatId] (assumed to exist). */
    fun isAdmin(userId: String, chatId: Int): Boolean = transact {
        select { GroupChats.id eq chatId }.first()[adminId] == userId
    }

    /**
     * Sets the [userId] as the admin of the [chatId]. An [IllegalArgumentException] will be thrown if the [userId]
     * isn't in the chat.
     */
    fun setAdmin(chatId: Int, userId: String) {
        val userIdList = GroupChatUsers.readUserIdList(chatId)
        if (userId !in userIdList)
            throw IllegalArgumentException("The new admin (ID: $userId) isn't in the chat (users: $userIdList).")
        transact {
            update({ GroupChats.id eq chatId }) { it[adminId] = userId }
        }
    }

    /**
     * [Broker.notify]s the [NewGroupChat.userIdList], excluding the admin, of the [NewGroupChat] via [newGroupChatsBroker].
     *
     * @return the [chat]'s ID after creating it.
     */
    fun create(adminId: String, chat: NewGroupChat): Int {
        val chatId = transact {
            insert {
                it[id] = Chats.create()
                it[GroupChats.adminId] = adminId
                it[title] = chat.title.value
                it[description] = chat.description.value
            }[GroupChats.id]
        }
        GroupChatUsers.addUsers(chatId, chat.userIdList + adminId)
        newGroupChatsBroker.notify(GroupChatId(chatId)) { it.userId in chat.userIdList - adminId }
        return chatId
    }

    /** @param[messagesPagination] pagination for [GroupChat.messages]. */
    fun readChat(
        id: Int,
        usersPagination: ForwardPagination? = null,
        messagesPagination: BackwardPagination? = null
    ): GroupChat = transact {
        select { GroupChats.id eq id }.first()
    }.let { buildGroupChat(it, id, usersPagination, messagesPagination) }

    /**
     * Returns the [userId]'s chats.
     *
     * @param[usersPagination] pagination for [GroupChat.users].
     * @param[messagesPagination] pagination for [GroupChat.messages].
     * @see [GroupChatUsers.readChatIdList]
     */
    fun readUserChats(
        userId: String,
        usersPagination: ForwardPagination? = null,
        messagesPagination: BackwardPagination? = null
    ): List<GroupChat> = transact {
        GroupChatUsers.readChatIdList(userId).map { readChat(it, usersPagination, messagesPagination) }
    }

    /**
     * [update]s the chat.
     *
     * Users in the [GroupChatUpdate.newUserIdList] who are already in the chat are ignored.
     *
     * Users in the [GroupChatUpdate.removedUserIdList] who aren't in the chat are ignored. Removed users will be
     * [Broker.unsubscribe]d via [updatedChatsBroker]. The chat is deleted if every user is removed.
     *
     * A [UpdatedGroupChat] is sent to clients who have [Broker.subscribe]d via [updatedChatsBroker]. Clients who have
     * [Broker.subscribe]d via [newGroupChatsBroker] will be [Broker.notify]d of the [GroupChat].
     *
     * @see [GroupChatUsers.addUsers]
     * @see [GroupChatUsers.removeUsers]
     * @see [updateTitleAndDescription]
     */
    fun update(update: GroupChatUpdate) {
        val existingUserIdList =
            readChat(update.chatId, messagesPagination = BackwardPagination(last = 0)).users.edges.map { it.node.id }
        val removedUserIdList =
            if (update.removedUserIdList == null) null
            else update.removedUserIdList.intersect(existingUserIdList).toList()
        if (removedUserIdList != null) {
            GroupChatUsers.removeUsers(update.chatId, removedUserIdList)
            if (removedUserIdList.containsAll(existingUserIdList)) return
        }
        updateTitleAndDescription(update)
        with(update) { if (newAdminId != null) setAdmin(chatId, newAdminId) }
        val newUserIdList = if (update.newUserIdList == null) null else update.newUserIdList - existingUserIdList
        if (newUserIdList != null) {
            GroupChatUsers.addUsers(update.chatId, newUserIdList)
            newGroupChatsBroker.notify(GroupChatId(update.chatId)) { it.userId in newUserIdList }
        }
        operateInfoBroker(update.copy(newUserIdList = newUserIdList, removedUserIdList = removedUserIdList))
    }

    /**
     * [update]s the [GroupChatUpdate.chatId]'s title and description if they aren't `null`.
     *
     * @see [GroupChats.update]
     */
    private fun updateTitleAndDescription(update: GroupChatUpdate): Unit = transact {
        update({ GroupChats.id eq update.chatId }) { statement ->
            if (update.title != null) statement[title] = update.title.value
            if (update.description != null) statement[description] = update.description.value
        }
    }

    /** [Broker.notify]s users in the [GroupChatUpdate.chatId] of the [UpdatedGroupChat] via [updatedChatsBroker]. */
    private fun operateInfoBroker(update: GroupChatUpdate): Unit = with(update) {
        val updatedChat = UpdatedGroupChat(
            chatId,
            title,
            description,
            newUserIdList?.map(::readUserById),
            removedUserIdList?.map(::readUserById),
            newAdminId
        )
        updatedChatsBroker.notify(updatedChat) { isUserInChat(it.userId, chatId) }
    }

    /**
     * Deletes the [chatId] from [GroupChats]. [Messages], and [MessageStatuses]. Clients who have
     * [Broker.subscribe]d to [MessagesSubscription]s via [messagesBroker] will receive a [DeletionOfEveryMessage].
     *
     * An [IllegalArgumentException] will be thrown if the [chatId] has users in it.
     */
    fun delete(chatId: Int) {
        val userIdList = GroupChatUsers.readUserIdList(chatId)
        if (userIdList.isNotEmpty())
            throw IllegalArgumentException("The chat (ID: $chatId) is not empty (users: $userIdList).")
        transact {
            deleteWhere { GroupChats.id eq chatId }
        }
        Messages.deleteChat(chatId)
    }

    /**
     * Returns chats after case-insensitively [query]ing the title of every chat the [userId] is in.
     *
     * @param[usersPagination] pagination for [GroupChat.messages].
     * @param[messagesPagination] pagination for [GroupChat.messages].
     */
    fun search(
        userId: String,
        query: String,
        usersPagination: ForwardPagination? = null,
        messagesPagination: BackwardPagination? = null
    ): List<GroupChat> = transact {
        select { (GroupChats.id inList GroupChatUsers.readChatIdList(userId)) and (title iLike query) }
            .map { buildGroupChat(it, it[GroupChats.id], usersPagination, messagesPagination) }
    }

    /**
     * Builds the [chatId] from the [row].
     *
     * @param[messagesPagination] pagination for [GroupChat.messages].
     * @param[usersPagination] pagination for [GroupChat.users].
     */
    private fun buildGroupChat(
        row: ResultRow,
        chatId: Int,
        usersPagination: ForwardPagination? = null,
        messagesPagination: BackwardPagination? = null
    ): GroupChat = GroupChat(
        chatId,
        row[adminId],
        GroupChatUsers.readUsers(chatId, usersPagination),
        GroupChatTitle(row[title]),
        GroupChatDescription(row[description]),
        Messages.readGroupChatConnection(chatId, messagesPagination)
    )

    /** Whether the [userId] is the admin of a group chat containing members other than themselves. */
    fun isNonemptyChatAdmin(userId: String): Boolean = readUserChats(
        userId,
        usersPagination = ForwardPagination(first = 2),
        messagesPagination = BackwardPagination(last = 0)
    ).filter { it.users.edges.size > 1 }.map { it.adminId }.contains(userId)

    /**
     * Case-insensitively [query]s the messages in the chats the [userId] is in. Only chats having messages matching the
     * [query] will be returned. Only the matched message [ChatEdges.edges] will be returned.
     */
    fun queryUserChatEdges(userId: String, query: String): List<ChatEdges> =
        GroupChatUsers.readChatIdList(userId)
            .associateWith { Messages.searchGroupChat(it, query) }
            .filter { (_, edges) -> edges.isNotEmpty() }
            .map { (chatId, edges) -> ChatEdges(chatId, edges) }
}