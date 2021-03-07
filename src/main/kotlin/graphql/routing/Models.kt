package com.neelkamath.omniChat.graphql.routing

import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.MessageType
import com.neelkamath.omniChat.db.tables.*
import java.time.LocalDateTime
import java.util.*

typealias Cursor = Int

/**
 * An [IllegalArgumentException] will be thrown if it contains whitespace, isn't lowercase, or exceeds
 * [Users.MAX_NAME_LENGTH] characters.
 */
data class Username(val value: String) {
    init {
        if (value != value.toLowerCase()) throw IllegalArgumentException("The username ($value) must be lowercase.")
        if (value.contains(Regex("""\s""")))
            throw IllegalArgumentException("""The username ("$value") cannot contain whitespace.""")
        if (value.length !in 1..Users.MAX_NAME_LENGTH)
            throw IllegalArgumentException("The username ($value) must be 1-${Users.MAX_NAME_LENGTH} characters long.")
    }
}

/**
 * An [IllegalArgumentException] will be thrown if it contains whitespace, or exceeds [Users.MAX_NAME_LENGTH]
 * characters.
 */
data class Name(val value: String) {
    init {
        if (value.contains(Regex("""\s""")))
            throw IllegalArgumentException("""The name ("$value") cannot contain whitespace.""")
        if (value.length > Users.MAX_NAME_LENGTH)
            throw IllegalArgumentException("The name ($value) mustn't exceed ${Users.MAX_NAME_LENGTH} characters.")
    }
}

/** An [IllegalArgumentException] will be thrown if the [value] exceeds [Bio.MAX_LENGTH] */
data class Bio(val value: String) {
    init {
        if (value.length > MAX_LENGTH)
            throw IllegalArgumentException("The value ($value) cannot exceed $MAX_LENGTH characters.")
    }

    companion object {
        const val MAX_LENGTH = 2500
    }
}

/** An [IllegalArgumentException] will be thrown if the [value] doesn't contain non-whitespace characters. */
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
    val firstName: Name = Name(""),
    val lastName: Name = Name(""),
    val bio: Bio = Bio("")
)

interface AccountData {
    val id: Int
    val username: Username
    val emailAddress: String
    val firstName: Name
    val lastName: Name
    val bio: Bio
}

data class Account(
    override val id: Int,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: Name,
    override val lastName: Name,
    override val bio: Bio
) : AccountData {
    /**
     * Case-insensitively [query]s the [username], [firstName], [lastName], and [emailAddress].
     */
    fun matches(query: String): Boolean = listOfNotNull(username.value, firstName.value, lastName.value, emailAddress)
        .any { it.contains(query, ignoreCase = true) }
}

data class MessageContext(val hasContext: Boolean, val id: Int?)

enum class GroupChatPublicity { NOT_INVITABLE, INVITABLE, PUBLIC }

interface BareMessage {
    val messageId: Int
    val sender: Account
    val dateTimes: MessageDateTimes
    val context: MessageContext
    val isForwarded: Boolean

    fun toNewTextMessage(): NewTextMessage = NewTextMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        TextMessages.read(messageId)
    )

    fun toNewActionMessage(): NewActionMessage = NewActionMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        ActionMessages.read(messageId)
    )

    fun toNewAudioMessage(): NewAudioMessage =
        NewAudioMessage(Messages.readChatIdFromMessageId(messageId), messageId, sender, dateTimes, context, isForwarded)

    fun toNewGroupChatInviteMessage(): NewGroupChatInviteMessage {
        val chatId = Messages.readChatIdFromMessageId(messageId)
        return NewGroupChatInviteMessage(
            chatId,
            messageId,
            sender,
            dateTimes,
            context,
            isForwarded,
            GroupChats.readInviteCode(chatId)
        )
    }

    fun toNewDocMessage(): NewDocMessage =
        NewDocMessage(Messages.readChatIdFromMessageId(messageId), messageId, sender, dateTimes, context, isForwarded)

    fun toNewVideoMessage(): NewVideoMessage =
        NewVideoMessage(Messages.readChatIdFromMessageId(messageId), messageId, sender, dateTimes, context, isForwarded)

    fun toNewPicMessage(): NewPicMessage = NewPicMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        PicMessages.read(messageId).caption
    )

    fun toNewPollMessage(): NewPollMessage = NewPollMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        PollMessages.read(messageId)
    )
}

interface AccountsSubscription

data class NewContact(
    override val id: Int,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: Name,
    override val lastName: Name,
    override val bio: Bio
) : AccountData, AccountsSubscription {
    companion object {
        fun build(userId: Int): NewContact =
            with(Users.read(userId)) { NewContact(id, username, emailAddress, firstName, lastName, bio) }
    }
}

interface OnlineStatusesSubscription

data class UpdatedOnlineStatus(val userId: Int, val isOnline: Boolean) : OnlineStatusesSubscription

data class OnlineStatus(val userId: Int, val isOnline: Boolean, val lastOnline: LocalDateTime?)

data class DeletedContact(val id: Int) : AccountsSubscription

data class AccountUpdate(
    val username: Username? = null,
    val password: Password? = null,
    val emailAddress: String? = null,
    val firstName: Name? = null,
    val lastName: Name? = null,
    val bio: Bio? = null,
)

/**
 * An [IllegalArgumentException] will be thrown if the [adminIdList] is empty, or the [adminIdList] isn't a subset of
 * the [userIdList].
 */
data class GroupChatInput(
    val title: GroupChatTitle,
    val description: GroupChatDescription,
    val userIdList: List<Int>,
    val adminIdList: List<Int>,
    val isBroadcast: Boolean,
    val publicity: GroupChatPublicity,
) {
    init {
        if (adminIdList.isEmpty()) throw IllegalArgumentException("There must be at least one admin.")
        if (!userIdList.containsAll(adminIdList))
            throw IllegalArgumentException(
                "The admin ID list ($adminIdList) must be a subset of the user ID list ($userIdList).",
            )
    }
}

/**
 * An [IllegalArgumentException] will be thrown if the [value] isn't 1-[MessageText.MAX_LENGTH] characters with at least
 * one non-whitespace.
 */
data class MessageText(val value: String) {
    init {
        if (value.trim().isEmpty() || value.length > MAX_LENGTH)
            throw IllegalArgumentException(
                "The text must be 1-$MAX_LENGTH characters, with at least one non-whitespace."
            )
    }

    companion object {
        const val MAX_LENGTH = 10_000
    }
}

/**
 * An [IllegalArgumentException] will be thrown if the [value] isn't 1-[GroupChatTitle.MAX_LENGTH] characters, of which
 * at least one isn't whitespace.
 */
data class GroupChatTitle(val value: String) {
    init {
        if (value.trim().isEmpty() || value.length > MAX_LENGTH)
            throw IllegalArgumentException(
                """
                The title ("$value") must be 1-$MAX_LENGTH characters, with at least one 
                non-whitespace character.
                """.trimIndent()
            )
    }

    companion object {
        const val MAX_LENGTH = 70
    }
}

/**
 * An [IllegalArgumentException] will be thrown if the [value] isn't at most [GroupChatDescription.MAX_LENGTH]
 * characters.
 */
data class GroupChatDescription(val value: String) {
    init {
        if (value.length > MAX_LENGTH)
            throw IllegalArgumentException(
                """The description ("$value") must be at most $MAX_LENGTH characters"""
            )
    }

    companion object {
        const val MAX_LENGTH = 1000
    }
}

/** A blocked user. */
data class BlockedAccount(
    override val id: Int,
    override val username: Username,
    override val emailAddress: String,
    override val firstName: Name,
    override val lastName: Name,
    override val bio: Bio,
) : AccountsSubscription, AccountData {
    companion object {
        fun build(userId: Int): BlockedAccount =
            with(Users.read(userId)) { BlockedAccount(id, username, emailAddress, firstName, lastName, bio) }
    }
}

/** An unblocked user. */
data class UnblockedAccount(val id: Int) : AccountsSubscription

/** An [IllegalArgumentException] will be thrown if the [newUsers] and [removedUsers] aren't distinct. */
data class UpdatedGroupChat(
    val chatId: Int,
    val title: GroupChatTitle? = null,
    val description: GroupChatDescription? = null,
    val newUsers: List<Account>? = null,
    val removedUsers: List<Account>? = null,
    val adminIdList: List<Int>? = null,
    val isBroadcast: Boolean? = null,
    val publicity: GroupChatPublicity? = null
) : GroupChatsSubscription {
    init {
        if (newUsers != null && removedUsers != null) {
            val intersection = newUsers.intersect(removedUsers)
            if (intersection.isNotEmpty())
                throw IllegalArgumentException(
                    "The list of new and removed users must be distinct. Users in both lists: $intersection."
                )
        }
    }
}

interface TypingStatusesSubscription

data class TypingStatus(val chatId: Int, val userId: Int, val isTyping: Boolean) : TypingStatusesSubscription

data class UpdatedAccount(
    val userId: Int,
    val username: Username,
    val emailAddress: String,
    val firstName: Name,
    val lastName: Name,
    val bio: Bio
) : AccountsSubscription {
    companion object {
        fun build(userId: Int): UpdatedAccount =
            with(Users.read(userId)) { UpdatedAccount(userId, username, emailAddress, firstName, lastName, bio) }
    }
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

interface BareGroupChat {
    val title: GroupChatTitle
    val description: GroupChatDescription
    val adminIdList: List<Int>
    val users: AccountsConnection
    val isBroadcast: Boolean
    val publicity: GroupChatPublicity
}

data class GroupChatInfo(
    override val adminIdList: List<Int>,
    override val users: AccountsConnection,
    override val title: GroupChatTitle,
    override val description: GroupChatDescription,
    override val isBroadcast: Boolean,
    override val publicity: GroupChatPublicity
) : BareGroupChat

data class GroupChat(
    override val id: Int,
    override val adminIdList: List<Int>,
    override val users: AccountsConnection,
    override val title: GroupChatTitle,
    override val description: GroupChatDescription,
    override val messages: MessagesConnection,
    override val isBroadcast: Boolean,
    override val publicity: GroupChatPublicity,
    val inviteCode: UUID?
) : Chat, BareGroupChat

data class MessagesConnection(val edges: List<MessageEdge>, val pageInfo: PageInfo)

data class MessageEdge(val node: Message, val cursor: Cursor)

interface BareChatMessage : BareMessage {
    val chatId: Int
    override val messageId: Int
    override val sender: Account
    override val dateTimes: MessageDateTimes
    override val context: MessageContext
    override val isForwarded: Boolean
}

interface Message : BareMessage {
    override val messageId: Int
    override val sender: Account
    override val dateTimes: MessageDateTimes
    override val context: MessageContext
    override val isForwarded: Boolean
    val hasStar: Boolean

    fun toUpdatedTextMessage(): UpdatedTextMessage = UpdatedTextMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        hasStar,
        TextMessages.read(messageId)
    )

    fun toUpdatedActionMessage(): UpdatedActionMessage = UpdatedActionMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        hasStar,
        ActionMessages.read(messageId)
    )

    fun toUpdatedPicMessage(): UpdatedPicMessage = UpdatedPicMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        hasStar,
        PicMessages.read(messageId).caption
    )

    fun toUpdatedAudioMessage(): UpdatedAudioMessage = UpdatedAudioMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        hasStar
    )

    fun toUpdatedGroupChatInviteMessage(): UpdatedGroupChatInviteMessage {
        val chatId = Messages.readChatIdFromMessageId(messageId)
        return UpdatedGroupChatInviteMessage(
            chatId,
            messageId,
            sender,
            dateTimes,
            context,
            isForwarded,
            hasStar,
            GroupChats.readInviteCode(chatId)
        )
    }

    fun toUpdatedDocMessage(): UpdatedDocMessage = UpdatedDocMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        hasStar
    )

    fun toUpdatedVideoMessage(): UpdatedVideoMessage = UpdatedVideoMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        hasStar
    )

    fun toUpdatedPollMessage(): UpdatedPollMessage = UpdatedPollMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        hasStar,
        PollMessages.read(messageId)
    )

    fun toStarredTextMessage(): StarredTextMessage = StarredTextMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        TextMessages.read(messageId)
    )

    fun toStarredActionMessage(): StarredActionMessage = StarredActionMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        ActionMessages.read(messageId)
    )

    fun toStarredPicMessage(): StarredPicMessage = StarredPicMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        PicMessages.read(messageId).caption
    )

    fun toStarredAudioMessage(): StarredAudioMessage = StarredAudioMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded
    )

    fun toStarredGroupChatInviteMessage(): StarredGroupChatInviteMessage {
        val chatId = Messages.readChatIdFromMessageId(messageId)
        return StarredGroupChatInviteMessage(
            chatId,
            messageId,
            sender,
            dateTimes,
            context,
            isForwarded,
            GroupChats.readInviteCode(chatId)
        )
    }

    fun toStarredDocMessage(): StarredDocMessage = StarredDocMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded
    )

    fun toStarredVideoMessage(): StarredVideoMessage = StarredVideoMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded
    )

    fun toStarredPollMessage(): StarredPollMessage = StarredPollMessage(
        Messages.readChatIdFromMessageId(messageId),
        messageId,
        sender,
        dateTimes,
        context,
        isForwarded,
        PollMessages.read(messageId)
    )
}

data class TextMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val message: MessageText
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): TextMessage = with(message) {
            TextMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
                TextMessages.read(messageId)
            )
        }
    }
}

data class ActionMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val message: ActionableMessage
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): ActionMessage = with(message) {
            ActionMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
                ActionMessages.read(messageId)
            )
        }
    }
}

data class PicMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val caption: MessageText?
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): PicMessage = with(message) {
            PicMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
                PicMessages.read(messageId).caption
            )
        }
    }
}

data class AudioMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): AudioMessage = with(message) {
            AudioMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId)
            )
        }
    }
}

data class GroupChatInviteMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val inviteCode: UUID
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): GroupChatInviteMessage = with(message) {
            GroupChatInviteMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
                GroupChats.readInviteCode(Messages.readChatIdFromMessageId(messageId))
            )
        }
    }
}

data class DocMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): DocMessage = with(message) {
            DocMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId)
            )
        }
    }
}

data class VideoMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): VideoMessage = with(message) {
            VideoMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId)
            )
        }
    }
}

data class PollMessage(
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val poll: Poll
) : BareMessage, Message {
    companion object {
        /** Builds the message as seen by the [userId]. */
        fun build(message: BareMessage, userId: Int? = null): PollMessage = with(message) {
            PollMessage(
                messageId,
                sender,
                dateTimes,
                context,
                isForwarded,
                if (userId == null) false else Stargazers.hasStar(userId, messageId),
                PollMessages.read(messageId)
            )
        }
    }
}

interface StarredMessage : BareChatMessage, BareMessage {
    override val chatId: Int
    override val messageId: Int
    override val sender: Account
    override val dateTimes: MessageDateTimes
    override val context: MessageContext
    override val isForwarded: Boolean

    companion object {
        /** Returns a concrete class for the [messageId] as seen by the [userId]. */
        fun build(userId: Int, messageId: Int): StarredMessage =
            when (val message = Messages.readMessage(userId, messageId)) {
                is TextMessage -> message.toStarredTextMessage()
                is ActionMessage -> message.toStarredActionMessage()
                is PicMessage -> message.toStarredPicMessage()
                is AudioMessage -> message.toStarredAudioMessage()
                is GroupChatInviteMessage -> message.toStarredGroupChatInviteMessage()
                is DocMessage -> message.toStarredDocMessage()
                is VideoMessage -> message.toStarredVideoMessage()
                is PollMessage -> message.toStarredPollMessage()
                else -> throw IllegalArgumentException("$message didn't match a concrete type.")
            }
    }
}

data class StarredTextMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val message: MessageText
) : StarredMessage, BareChatMessage, BareMessage

data class StarredActionMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val message: ActionableMessage
) : StarredMessage, BareChatMessage, BareMessage

data class StarredPicMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val caption: MessageText?
) : StarredMessage, BareChatMessage, BareMessage

data class StarredPollMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val poll: Poll
) : StarredMessage, BareChatMessage, BareMessage

data class StarredAudioMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean
) : StarredMessage, BareChatMessage, BareMessage

data class StarredGroupChatInviteMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val inviteCode: UUID
) : StarredMessage, BareChatMessage, BareMessage

data class StarredDocMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean
) : StarredMessage, BareChatMessage, BareMessage

data class StarredVideoMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean
) : StarredMessage, BareChatMessage, BareMessage

interface MessagesSubscription

interface NewMessage : BareChatMessage, BareMessage {
    override val chatId: Int
    override val messageId: Int
    override val sender: Account
    override val dateTimes: MessageDateTimes
    override val context: MessageContext
    override val isForwarded: Boolean

    companion object {
        /** Returns a concrete class for the [messageId]. */
        fun build(messageId: Int): NewMessage {
            val (type, message) = Messages.readTypedMessage(messageId)
            return when (type) {
                MessageType.TEXT -> message.toNewTextMessage()
                MessageType.ACTION -> message.toNewActionMessage()
                MessageType.PIC -> message.toNewPicMessage()
                MessageType.AUDIO -> message.toNewAudioMessage()
                MessageType.GROUP_CHAT_INVITE -> message.toNewGroupChatInviteMessage()
                MessageType.DOC -> message.toNewDocMessage()
                MessageType.VIDEO -> message.toNewVideoMessage()
                MessageType.POLL -> message.toNewPollMessage()
            }
        }
    }
}

data class NewTextMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val message: MessageText,
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

data class NewActionMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val message: ActionableMessage,
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

data class NewPicMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val caption: MessageText?,
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

data class NewPollMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val poll: Poll
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

data class NewAudioMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

data class NewGroupChatInviteMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    val inviteCode: UUID
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

data class NewDocMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

data class NewVideoMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean
) : NewMessage, BareChatMessage, BareMessage, MessagesSubscription

interface UpdatedMessage : BareChatMessage, BareMessage {
    override val chatId: Int
    override val messageId: Int
    override val sender: Account
    override val dateTimes: MessageDateTimes
    override val context: MessageContext
    override val isForwarded: Boolean
    val hasStar: Boolean

    companion object {
        /** Returns a concrete class for the [messageId] as seen by the [userId]. */
        fun build(userId: Int, messageId: Int): UpdatedMessage =
            when (val message = Messages.readMessage(userId, messageId)) {
                is TextMessage -> message.toUpdatedTextMessage()
                is ActionMessage -> message.toUpdatedActionMessage()
                is PicMessage -> message.toUpdatedPicMessage()
                is AudioMessage -> message.toUpdatedAudioMessage()
                is GroupChatInviteMessage -> message.toUpdatedGroupChatInviteMessage()
                is DocMessage -> message.toUpdatedDocMessage()
                is VideoMessage -> message.toUpdatedVideoMessage()
                is PollMessage -> message.toUpdatedPollMessage()
                else -> throw IllegalArgumentException("$message didn't match a concrete class.")
            }
    }
}

data class UpdatedTextMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val message: MessageText
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class UpdatedActionMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val message: ActionableMessage
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class UpdatedPicMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val caption: MessageText?
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class UpdatedPollMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val poll: Poll
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class UpdatedAudioMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class UpdatedGroupChatInviteMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean,
    val inviteCode: UUID
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class UpdatedDocMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class UpdatedVideoMessage(
    override val chatId: Int,
    override val messageId: Int,
    override val sender: Account,
    override val dateTimes: MessageDateTimes,
    override val context: MessageContext,
    override val isForwarded: Boolean,
    override val hasStar: Boolean
) : UpdatedMessage, BareChatMessage, BareMessage, MessagesSubscription

data class MessageDateTimes(val sent: LocalDateTime, val statuses: List<MessageDateTimeStatus>)

data class MessageDateTimeStatus(val user: Account, val dateTime: LocalDateTime, val status: MessageStatus)

enum class MessageStatus { DELIVERED, READ }

data class DeletedMessage(val chatId: Int, val messageId: Int) : MessagesSubscription

data class MessageDeletionPoint(val chatId: Int, val until: LocalDateTime) : MessagesSubscription

data class UserChatMessagesRemoval(val chatId: Int, val userId: Int) : MessagesSubscription

data class ExitedUser(val userId: Int, val chatId: Int) : GroupChatsSubscription

interface GroupChatsSubscription

data class GroupChatId(val id: Int) : GroupChatsSubscription

data class DeletionOfEveryMessage(val chatId: Int) : MessagesSubscription

object CreatedSubscription :
    MessagesSubscription,
    AccountsSubscription,
    GroupChatsSubscription,
    TypingStatusesSubscription,
    OnlineStatusesSubscription {

    @Suppress("unused")
    val placeholder = Placeholder
}

data class ChatMessages(val chat: Chat, val messages: List<MessageEdge>)

data class AccountsConnection(val edges: List<AccountEdge>, val pageInfo: PageInfo) {
    companion object {
        /**
         * An [AccountsConnection] is used when the dataset used is too large to load in one go. Certain datasets are
         * considered large in the context of networking but not in a server program. For example, a list of thousands
         * of elements has a negligible performance impact on a server but is too large to transfer as JSON to a client.
         * In such cases, you can easily build an [AccountsConnection] by passing the entire dataset to this function.
         * Of course, if the dataset would be considered large by a server program as well, then this function shouldn't
         * be used since the [accountEdges] passed must contain the entire dataset.
         *
         * The [accountEdges] will be sorted in ascending order of their [AccountEdge.cursor] for you.
         */
        fun build(accountEdges: List<AccountEdge>, pagination: ForwardPagination? = null): AccountsConnection {
            val (first, after) = pagination ?: ForwardPagination()
            val accounts = accountEdges.sortedBy { it.cursor }
            val afterAccounts = if (after == null) accounts else accounts.filter { it.cursor > after }
            val firstAccounts = if (first == null) afterAccounts else afterAccounts.take(first)
            val edges = firstAccounts.map { AccountEdge(it.node, it.cursor) }
            val pageInfo = PageInfo(
                hasNextPage = firstAccounts.size < afterAccounts.size,
                hasPreviousPage = afterAccounts.size < accounts.size,
                startCursor = accounts.firstOrNull()?.cursor,
                endCursor = accounts.lastOrNull()?.cursor,
            )
            return AccountsConnection(edges, pageInfo)
        }
    }
}

data class AccountEdge(val node: Account, val cursor: Cursor) {
    companion object {
        fun build(userId: Int, cursor: Cursor): AccountEdge = AccountEdge(Users.read(userId).toAccount(), cursor)
    }
}

data class PageInfo(
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean,
    val startCursor: Cursor? = null,
    val endCursor: Cursor? = null
)

/** An [IllegalArgumentException] will be thrown if there aren't at least two [options], each of which are unique. */
private fun <T> validateOptions(options: List<T>) {
    if (options.size < 2) throw IllegalArgumentException("There must be at least two options: $options.")
    if (options.toSet().size != options.size) throw IllegalArgumentException("Options must be unique: $options.")
}

/** An [IllegalArgumentException] will be thrown if there aren't at least two [options], each of which are unique. */
data class PollInput(val title: MessageText, val options: List<MessageText>) {
    init {
        validateOptions(options)
    }
}

data class PollOption(val option: MessageText, val votes: List<Int>)

/** An [IllegalArgumentException] will be thrown if there aren't at least two [options], each of which are unique. */
data class Poll(val title: MessageText, val options: List<PollOption>) {
    init {
        validateOptions(options)
    }
}

/** An [IllegalArgumentException] is thrown if there isn't at least one [actions], or the [actions] aren't unique. */
data class ActionableMessage(val text: MessageText, val actions: List<MessageText>) {
    init {
        validateOptions(actions)
    }

    fun toActionMessageInput(): ActionMessageInput = ActionMessageInput(text, actions)
}

/** An [IllegalArgumentException] is thrown if there isn't at least one [actions], or the [actions] aren't unique. */
data class ActionMessageInput(val text: MessageText, val actions: List<MessageText>) {
    init {
        validateOptions(actions)
    }
}

data class TriggeredAction(val messageId: Int, val action: MessageText, val triggeredBy: Account) : MessagesSubscription
