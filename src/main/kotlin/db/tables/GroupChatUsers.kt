package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.groupChatsNotifier
import com.neelkamath.omniChatBackend.db.messagesNotifier
import com.neelkamath.omniChatBackend.db.readUserIdList
import com.neelkamath.omniChatBackend.graphql.routing.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

/** The users in [GroupChats]. */
object GroupChatUsers : IntIdTable() {
    override val tableName get() = "group_chat_users"
    private val groupChatId: Column<Int> = integer("group_chat_id").references(GroupChats.id)
    private val userId: Column<Int> = integer("user_id").references(Users.id)
    private val isAdmin: Column<Boolean> = bool("is_admin")

    private fun isUserInChat(userId: Int, chatId: Int): Boolean = transaction {
        select((groupChatId eq chatId) and (GroupChatUsers.userId eq userId)).empty().not()
    }

    /**
     * Makes the [userIdList] admins of the [chatId]. An [IllegalArgumentException] will be thrown if the a user isn't
     * in the chat.
     *
     * If [shouldNotify], subscribers will receive the [UpdatedGroupChat] via [groupChatsNotifier].
     */
    fun makeAdmins(chatId: Int, userIdList: Collection<Int>, shouldNotify: Boolean = true) {
        val invalidUsers = userIdList.filterNot { isUserInChat(it, chatId) }
        require(invalidUsers.isEmpty()) { "$invalidUsers aren't in the chat (ID: $chatId)." }
        transaction {
            update({ (groupChatId eq chatId) and (userId inList userIdList) }) { it[isAdmin] = true }
        }
        if (shouldNotify) {
            val update = UpdatedGroupChat(chatId, adminIdList = readAdminIdList(chatId).toList())
            groupChatsNotifier.publish(update, readUserIdList(chatId))
        }
    }

    /** Returns the ID of every user the [userId] has a chat with, excluding their own ID. */
    fun readFellowParticipants(userId: Int): Set<Int> =
        readChatIdList(userId).flatMap(::readUserIdList).toSet() - userId

    /** Whether the [userId] is an admin of the [chatId]. */
    fun isAdmin(userId: Int, chatId: Int): Boolean = transaction {
        select((groupChatId eq chatId) and (GroupChatUsers.userId eq userId))
            .firstOrNull()
            ?.get(isAdmin) ?: false
    }

    /**
     * The user ID list from the specified [chatId].
     *
     * @see [readUsers]
     * @see [readAdminIdList]
     */
    fun readUserIdList(chatId: Int): Set<Int> = transaction {
        select(groupChatId eq chatId).map { it[userId] }.toSet()
    }

    fun readAdminIdList(chatId: Int): Set<Int> = transaction {
        select((groupChatId eq chatId) and (isAdmin eq true)).map { it[userId] }.toSet()
    }

    private fun readAccountEdges(chatId: Int): Set<AccountEdge> = transaction {
        select(groupChatId eq chatId)
            .map {
                val account = Users.read(it[userId]).toAccount()
                AccountEdge(account, cursor = it[GroupChatUsers.id].value)
            }
            .toSet()
    }

    /** @see [readUserIdList] */
    fun readUsers(chatId: Int, pagination: ForwardPagination? = null): AccountsConnection =
        AccountsConnection.build(readAccountEdges(chatId), pagination)

    /**
     * Adds the [users] who aren't already in the [chatId]. Notifies existing users of the [UpdatedGroupChat] via
     * [groupChatsNotifier], and new users of the [GroupChatId] via [groupChatsNotifier].
     */
    fun addUsers(chatId: Int, users: Collection<Int>) {
        val newUserIdList = users.filterNot { isUserInChat(it, chatId) }.toSet()
        transaction {
            batchInsert(newUserIdList) {
                this[groupChatId] = chatId
                this[userId] = it
                this[isAdmin] = false
            }
        }
        groupChatsNotifier.publish(GroupChatId(chatId), newUserIdList)
        val update = UpdatedGroupChat(chatId, newUsers = newUserIdList.map { Users.read(it).toAccount() })
        groupChatsNotifier.publish(update, readUserIdList(chatId).minus(newUserIdList))
    }

    fun addUsers(chatId: Int, vararg users: Int): Unit = addUsers(chatId, users.toList())

    /** If the [userId] is in the chat having the [inviteCode], nothing happens. Otherwise, [addUsers] is called. */
    fun addUserViaInvite(userId: Int, inviteCode: UUID) {
        val chatId = GroupChats.readChatFromInvite(inviteCode)
        if (isUserInChat(userId, chatId)) return
        addUsers(chatId, userId)
    }

    /**
     * Whether the [userIdList] can be removed from the [chatId]. Returns `false` if there would be users sans admins
     * left in the [chatId]. Users who aren't in the chat are ignored.
     */
    private fun canUsersLeave(chatId: Int, userIdList: Collection<Int>): Boolean {
        val existing = readUserIdList(chatId).toSet()
        val supplied = userIdList.filter { it in existing }.toSet()
        return supplied == existing || (existing - supplied).any { isAdmin(it, chatId) }
    }

    fun canUsersLeave(chatId: Int, vararg userIdList: Int): Boolean = canUsersLeave(chatId, userIdList.toSet())

    /**
     * Removes users in the [userIdList] from the [chatId]. Users who aren't in the chat are ignored. An
     * [IllegalArgumentException] will be thrown if not [canUsersLeave]. If every user is removed, the [chatId] will be
     * [GroupChats.delete]d. Returns whether the chat was deleted.
     *
     * Subscribers in the chat (including the [userIdList]) will be notified of the [ExitedUsers]s via
     * [groupChatsNotifier]. Removed users will be notified of the [UnstarredChat] via [messagesNotifier].
     */
    fun removeUsers(chatId: Int, userIdList: Set<Int>): Boolean {
        require(canUsersLeave(chatId, userIdList)) {
            "The users ($userIdList) cannot leave because the chat needs an admin."
        }
        val originalIdList = readUserIdList(chatId)
        val removedIdList = originalIdList.intersect(userIdList).toList()
        transaction {
            deleteWhere { (groupChatId eq chatId) and (userId inList removedIdList) }
        }
        removedIdList.forEach { Stargazers.deleteUserChat(it, chatId) }
        groupChatsNotifier.publish(ExitedUsers(chatId, removedIdList), originalIdList)
        if (readUserIdList(chatId).isEmpty()) {
            GroupChats.delete(chatId)
            return true
        }
        return false
    }

    fun removeUsers(chatId: Int, vararg userIdList: Int): Boolean = removeUsers(chatId, userIdList.toSet())

    /**
     * Whether the [userId] can leave every chat they're in. Returns `false` only if they're the last admin of a chat
     * with other users in it.
     */
    fun canUserLeave(userId: Int): Boolean = readChatIdList(userId).all { canUsersLeave(it, userId) }

    /** Calls [removeUsers] on the [userId] for every chat they're in. The [userId] needn't exist. */
    fun removeUser(userId: Int): Unit = readChatIdList(userId).forEach { removeUsers(it, userId) }

    /** The chat ID list of every chat the [userId] is in. Returns an empty list if the [userId] doesn't exist. */
    fun readChatIdList(userId: Int): Set<Int> = transaction {
        select(GroupChatUsers.userId eq userId).map { it[groupChatId] }.toSet()
    }
}
