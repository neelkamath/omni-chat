package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.db.tables.Stargazers
import com.neelkamath.omniChat.db.tables.Users
import org.keycloak.representations.idm.UserRepresentation
import java.time.LocalDateTime

typealias Cursor = Int

enum class InvalidGroupChatPicReason { NONEXISTENT_CHAT, PIC_TOO_BIG }

data class InvalidGroupChatPic(val reason: InvalidGroupChatPicReason)

/**
 * @throws [IllegalArgumentException] if the [value] isn't lowercase, isn't shorter than 256 characters, or doesn't
 * contain non-whitespace characters.
 */
data class Username(val value: String) {
    init {
        if (value.trim().isEmpty()) throw IllegalArgumentException("""The username ("$value") mustn't be empty.""")
        // The auth system disallows usernames longer than 255 characters.
        if (value.length > 255) throw IllegalArgumentException("The username($value) must be less than 256 characters.")
        // The auth system silently saves uppercase characters in usernames as lowercase.
        if (value != value.toLowerCase()) throw IllegalArgumentException("The username ($value) must be lowercase.")
    }
}

/** @throws [IllegalArgumentException] if the [value] exceeds [Users.MAX_BIO_LENGTH] */
data class Bio(val value: String) {
    init {
        if (value.length > Users.MAX_BIO_LENGTH)
            throw IllegalArgumentException("The value ($value) cannot exceed ${Users.MAX_BIO_LENGTH} characters.")
    }
}

/** @throws [IllegalArgumentException] if the [value] doesn't contain non-whitespace characters. */
data class Password(val value: String) {
    init {
        if (value.trim().isEmpty()) throw IllegalArgumentException("""The password ("$value") mustn't be empty.""")
    }
}

object Placeholder

data class GraphQlRequest(
    /** GraphQL document (e.g., a mutation). */
    val query: String,
    val variables: Map<String, Any?>? = null,
    val operationName: String? = null
)

data class GraphQlResponse(val data: Map<String, Any?>? = null, val errors: List<GraphQlResponseError>? = null)

data class GraphQlResponseError(val message: String)

data class Login(val username: Username, val password: Password)

data class TokenSet(val accessToken: String, val refreshToken: String)

data class AccountInput(
    val username: Username,
    val password: Password,
    val emailAddress: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: Bio? = null
)

interface AccountData {
    val id: Int
    val username: Username
    val emailAddress: String
    val firstName: String?
    val lastName: String?
    val bio: Bio?
}

data class Account(
    override val id: Int,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: String? = null,
    override val lastName: String? = null,
    override val bio: Bio? = null
) : AccountData {
    /**
     * Case-insensitively searches for the [query] in the [UserRepresentation.username], [UserRepresentation.firstName],
     * [UserRepresentation.lastName], and [UserRepresentation.email].
     */
    fun matches(query: String): Boolean =
        listOfNotNull(username.value, firstName, lastName, emailAddress).any { it.contains(query, ignoreCase = true) }
}

data class MessageContext(val hasContext: Boolean, val id: Int?)

interface BareMessage {
    val sender: Account
    val text: TextMessage
    val dateTimes: MessageDateTimes
    val context: MessageContext
}

interface ContactsSubscription

data class NewContact(
    override val id: Int,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: String? = null,
    override val lastName: String? = null,
    override val bio: Bio? = null
) : AccountData, ContactsSubscription {
    companion object {
        fun build(userId: Int): NewContact =
            with(readUserById(userId)) { NewContact(id, username, emailAddress, firstName, lastName, bio) }
    }
}

data class UpdatedContact(
    override val id: Int,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: String? = null,
    override val lastName: String? = null,
    override val bio: Bio? = null
) : AccountData, ContactsSubscription {
    companion object {
        fun build(userId: Int): UpdatedContact =
            with(readUserById(userId)) { UpdatedContact(id, username, emailAddress, firstName, lastName, bio) }
    }
}

interface OnlineStatusesSubscription

data class UpdatedOnlineStatus(val userId: Int, val isOnline: Boolean) : OnlineStatusesSubscription

data class OnlineStatus(val userId: Int, val isOnline: Boolean, val lastOnline: LocalDateTime?)

data class DeletedContact(val id: Int) : ContactsSubscription

data class AccountUpdate(
    val username: Username? = null,
    val password: Password? = null,
    val emailAddress: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: Bio? = null
)

data class GroupChatInput(
    val title: GroupChatTitle,
    val description: GroupChatDescription,
    val userIdList: List<Int> = listOf()
)

private fun <T> verifyGroupChatUsers(newUsers: List<T>?, removedUsers: List<T>?) {
    if (newUsers != null && removedUsers != null) {
        val intersection = newUsers.intersect(removedUsers)
        if (intersection.isNotEmpty())
            throw IllegalArgumentException(
                "The list of new and removed users must be distinct. Users in both lists: $intersection"
            )
    }
}

interface UpdatedChatsSubscription

/**
 * @throws [IllegalArgumentException] if the [value] isn't 1-[Messages.MAX_TEXT_LENGTH] characters with at least one
 * non-whitespace.
 */
data class TextMessage(val value: String) {
    init {
        if (value.trim().isEmpty() || value.length > Messages.MAX_TEXT_LENGTH)
            throw IllegalArgumentException(
                "The text must be 1-${Messages.MAX_TEXT_LENGTH} characters, with at least one non-whitespace."
            )
    }
}

/**
 * @throws [IllegalArgumentException] if the [value] isn't 1-[GroupChats.MAX_TITLE_LENGTH] characters, of which at
 * least one isn't whitespace.
 */
data class GroupChatTitle(val value: String) {
    init {
        if (value.trim().isEmpty() || value.length > GroupChats.MAX_TITLE_LENGTH)
            throw IllegalArgumentException(
                """
                The title ("$value") must be 1-${GroupChats.MAX_TITLE_LENGTH} characters, with at least one 
                non-whitespace character.
                """.trimIndent()
            )
    }
}

/** @throws [IllegalArgumentException] if the [value] isn't at most [GroupChats.MAX_DESCRIPTION_LENGTH] characters. */
data class GroupChatDescription(val value: String) {
    init {
        if (value.length > GroupChats.MAX_DESCRIPTION_LENGTH)
            throw IllegalArgumentException(
                """The description ("$value") must be at most ${GroupChats.MAX_DESCRIPTION_LENGTH} characters"""
            )
    }
}

/** @throws [IllegalArgumentException] if the [newUsers] and [removedUsers] aren't distinct. */
data class UpdatedGroupChat(
    val chatId: Int,
    val title: GroupChatTitle? = null,
    val description: GroupChatDescription? = null,
    val newUsers: List<Account>? = null,
    val removedUsers: List<Account>? = null,
    val adminId: Int? = null
) : UpdatedChatsSubscription {
    init {
        verifyGroupChatUsers(newUsers, removedUsers)
    }
}

interface TypingStatusesSubscription

data class TypingStatus(val chatId: Int, val userId: Int, val isTyping: Boolean) : TypingStatusesSubscription

data class UpdatedAccount(
    val userId: Int,
    val username: Username,
    val emailAddress: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: Bio? = null
) : UpdatedChatsSubscription {
    companion object {
        fun build(userId: Int): UpdatedAccount =
            with(readUserById(userId)) { UpdatedAccount(userId, username, emailAddress, firstName, lastName, bio) }
    }
}

/** @throws [IllegalArgumentException] if the [newUserIdList] and [removedUserIdList] aren't distinct. */
data class GroupChatUpdate(
    val chatId: Int,
    val title: GroupChatTitle? = null,
    val description: GroupChatDescription? = null,
    val newUserIdList: List<Int>? = null,
    val removedUserIdList: List<Int>? = null,
    val newAdminId: Int? = null
) {
    init {
        verifyGroupChatUsers(newUserIdList, removedUserIdList)
    }

    fun toUpdatedGroupChat(): UpdatedGroupChat = UpdatedGroupChat(
        chatId,
        title,
        description,
        newUserIdList?.map(::readUserById),
        removedUserIdList?.map(::readUserById),
        newAdminId
    )
}

interface Chat {
    val id: Int
    val messages: MessagesConnection
}

data class PrivateChat(
    override val id: Int,
    val user: Account,
    override val messages: MessagesConnection
) : Chat

data class GroupChat(
    override val id: Int,
    val adminId: Int,
    val users: AccountsConnection,
    val title: GroupChatTitle,
    val description: GroupChatDescription? = null,
    override val messages: MessagesConnection
) : Chat

data class MessagesConnection(val edges: List<MessageEdge>, val pageInfo: PageInfo)

data class MessageEdge(val node: Message, val cursor: Cursor)

interface MessageData : BareMessage {
    val chatId: Int
    val messageId: Int
}

data class StarredMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val text: TextMessage,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext
) : MessageData {
    companion object {
        fun build(messageId: Int): StarredMessage = with(Messages.readBareMessage(messageId)) {
            StarredMessage(Messages.readChatFromMessage(messageId), messageId, sender, text, dateTimes, context)
        }
    }
}

data class Message(
    val id: Int,
    override val sender: Account,
    override val text: TextMessage,
    override val dateTimes: MessageDateTimes,
    val hasStar: Boolean,
    override val context: MessageContext
) : BareMessage {
    fun toNewMessage(): NewMessage = NewMessage(Messages.readChatFromMessage(id), id, sender, text, dateTimes, context)

    fun toUpdatedMessage(): UpdatedMessage =
        UpdatedMessage(Messages.readChatFromMessage(id), id, sender, text, dateTimes, hasStar, context)

    companion object {
        /** The [userId] the [Message] is for. */
        fun build(userId: Int, messageId: Int, message: BareMessage): Message = with(message) {
            Message(messageId, sender, text, dateTimes, Stargazers.hasStar(userId, messageId), context)
        }
    }
}

interface MessagesSubscription

data class NewMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val text: TextMessage,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext
) : MessageData, MessagesSubscription {
    companion object {
        fun build(id: Int, message: BareMessage): NewMessage =
            with(message) { NewMessage(Messages.readChatFromMessage(id), id, sender, text, dateTimes, context) }
    }
}

data class UpdatedMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val text: TextMessage,
    override val dateTimes: MessageDateTimes,
    val hasStar: Boolean,
    override val context: MessageContext
) : MessageData, MessagesSubscription {
    companion object {
        fun build(userId: Int, messageId: Int): UpdatedMessage =
            Messages.readMessage(userId, messageId).toUpdatedMessage()
    }
}

data class MessageDateTimes(val sent: LocalDateTime, val statuses: List<MessageDateTimeStatus>)

data class MessageDateTimeStatus(val user: Account, val dateTime: LocalDateTime, val status: MessageStatus)

enum class MessageStatus { DELIVERED, READ }

data class DeletedMessage(val chatId: Int, val messageId: Int) : MessagesSubscription

data class MessageDeletionPoint(val chatId: Int, val until: LocalDateTime) : MessagesSubscription

data class UserChatMessagesRemoval(val chatId: Int, val userId: Int) : MessagesSubscription

data class ExitedUser(val chatId: Int, val userId: Int) : UpdatedChatsSubscription

interface NewGroupChatsSubscription

data class GroupChatId(val id: Int) : NewGroupChatsSubscription

data class DeletionOfEveryMessage(val chatId: Int) : MessagesSubscription

object CreatedSubscription :
    MessagesSubscription,
    ContactsSubscription,
    UpdatedChatsSubscription,
    NewGroupChatsSubscription,
    TypingStatusesSubscription,
    OnlineStatusesSubscription {

    @Suppress("unused")
    val placeholder = Placeholder
}

data class ChatMessages(val chat: Chat, val messages: List<MessageEdge>)

data class AccountsConnection(val edges: List<AccountEdge>, val pageInfo: PageInfo) {
    companion object {
        /** @param[AccountEdges] needn't be listed in ascending order of their [AccountEdge.cursor]. */
        fun build(AccountEdges: List<AccountEdge>, pagination: ForwardPagination? = null): AccountsConnection {
            val (first, after) = pagination ?: ForwardPagination()
            val accounts = AccountEdges.sortedBy { it.cursor }
            val afterAccounts = if (after == null) accounts else accounts.filter { it.cursor > after }
            val firstAccounts = if (first == null) afterAccounts else afterAccounts.take(first)
            val edges = firstAccounts.map { AccountEdge(it.node, cursor = it.cursor) }
            val pageInfo = PageInfo(
                hasNextPage = firstAccounts.size < afterAccounts.size,
                hasPreviousPage = afterAccounts.size < accounts.size,
                startCursor = accounts.firstOrNull()?.cursor,
                endCursor = accounts.lastOrNull()?.cursor
            )
            return AccountsConnection(edges, pageInfo)
        }
    }
}

data class AccountEdge(val node: Account, val cursor: Cursor)

data class PageInfo(
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean,
    val startCursor: Cursor? = null,
    val endCursor: Cursor? = null
)