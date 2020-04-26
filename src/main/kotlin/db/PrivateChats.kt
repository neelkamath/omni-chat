package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.PrivateChat
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.keycloak.representations.idm.UserRepresentation

data class PrivateChatMetadata(val id: Int, val creatorUserId: String, val invitedUserId: String)

private data class PrivateUserChat(val user: UserRepresentation, val chatId: Int)

/**
 * Private chats.
 *
 * [PrivateChatClears] holds the data on when each user deleted the chat for themselves. [Messages] holds chat messages.
 */
object PrivateChats : IntIdTable() {
    override val tableName get() = "private_chats"
    private val creatorUserId = varchar("creator_user_id", Auth.userIdLength)
    private val invitedUserId = varchar("invited_user_id", Auth.userIdLength)

    /** Returns the chat ID after creating it. */
    fun create(creatorUserId: String, invitedUserId: String): Int = Db.transact {
        insertAndGetId {
            it[this.creatorUserId] = creatorUserId
            it[this.invitedUserId] = invitedUserId
        }.value
    }

    /** Returns the private chats the [userId] is in. */
    fun read(userId: String): List<PrivateChatMetadata> = Db.transact {
        select { (creatorUserId eq userId) or (invitedUserId eq userId) }
            .map { PrivateChatMetadata(it[id].value, it[creatorUserId], it[invitedUserId]) }
    }

    /** Whether [userId1] and [userId2] are in a chat with each other. */
    fun exists(userId1: String, userId2: String): Boolean = Db.transact {
        val userIdPairs = selectAll().map { Pair(it[creatorUserId], it[invitedUserId]) }
        Pair(userId1, userId2) in userIdPairs || Pair(userId2, userId1) in userIdPairs
    }

    /**
     * Searches chats the user (specified by the [userId]) is in.
     *
     * The other users the specified user is chatting with have their username, first name, and last name matched with
     * the [query].
     */
    fun search(userId: String, query: String): List<PrivateChat> = Db.transact {
        select { (creatorUserId eq userId) or (invitedUserId eq userId) }
            .fold(mutableListOf()) { chats: MutableList<PrivateChat>, row: ResultRow ->
                val user = if (row[creatorUserId] == userId) row[invitedUserId] else userId
                if (matchesUser(Auth.findUserById(user), query)) chats.add(PrivateChat(row[id].value, user))
                chats
            }
    }

    fun isCreator(chatId: Int, userId: String): Boolean = Db.transact {
        select { id eq chatId }.first()[creatorUserId] == userId
    }

    /** Deletes every chat the [userId] is in from [PrivateChats], [PrivateChatClears], and [Messages]. */
    fun delete(userId: String) {
        read(userId).map { it.id }.forEach { chatId ->
            PrivateChatClears.delete(chatId)
            Messages.delete(chatId)
        }
        Db.transact {
            deleteWhere { (creatorUserId eq userId) or (invitedUserId eq userId) }
        }
    }

    fun isUserInChat(userId: String, chatId: Int): Boolean = chatId in read(userId).map { it.id }

    /**
     * Whether the [query] matches the [user] (case-insensitive).
     *
     * Checks if there's a match between the [user]'s [UserRepresentation.username], [UserRepresentation.firstName], or
     * [UserRepresentation.lastName].
     */
    private fun matchesUser(user: UserRepresentation, query: String): Boolean = with(user) {
        containsQuery(username, query) || containsQuery(firstName, query) || containsQuery(lastName, query)
    }

    /** Whether the [string] contains the [query] (case-insensitive). */
    private fun containsQuery(string: String?, query: String): Boolean =
        string != null && query.toLowerCase() in string.toLowerCase()
}