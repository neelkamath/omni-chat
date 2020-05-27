package com.neelkamath.omniChat

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

/** Project-wide Jackson config. */
val objectMapper: ObjectMapper = jacksonObjectMapper()
    .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .findAndRegisterModules()

data class GraphQlRequest(
    /** GraphQL document (e.g., a mutation). */
    val query: String,
    val variables: Map<String, Any>? = null,
    val operationName: String? = null
)

data class GraphQlResponse(val data: Map<String, Any>? = null, val errors: List<GraphQlResponseError>? = null)

data class GraphQlResponseError(val message: String)

data class Login(val username: String, val password: String)

data class TokenSet(val accessToken: String, val refreshToken: String)

data class NewAccount(
    /** An [IllegalArgumentException] will be thrown if it's not lowercase. */
    val username: String,
    val password: String,
    val emailAddress: String,
    val firstName: String? = null,
    val lastName: String? = null
) {
    init {
        if (username != username.toLowerCase())
            throw IllegalArgumentException("The username ($username) must be lowercase.")
    }
}

data class AccountInfo(
    val id: String,
    val username: String,
    val emailAddress: String,
    val firstName: String? = null,
    val lastName: String? = null
)

data class AccountUpdate(
    val username: String? = null,
    val password: String? = null,
    val emailAddress: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)

data class NewGroupChat(val title: String, val description: String? = null, val userIdList: Set<String> = setOf())

data class GroupChatUpdate(
    val chatId: Int,
    val title: String? = null,
    val description: String? = null,
    val newUserIdList: Set<String> = setOf(),
    val removedUserIdList: Set<String> = setOf(),
    val newAdminId: String? = null
)

sealed class Chat

data class PrivateChat(
    val id: Int,
    /** The user being chatted with. */
    val user: AccountInfo,
    val messages: List<Message>
) : Chat()

data class GroupChat(
    val id: Int,
    val adminId: String,
    val users: Set<AccountInfo>,
    val title: String,
    val description: String? = null,
    val messages: List<Message>
) : Chat()

/** Represents created and deleted messages, and deleted chats. */
sealed class MessageUpdates

data class Message(
    val id: Int,
    val sender: AccountInfo,
    val text: String,
    val dateTimes: MessageDateTimes
) : MessageUpdates()

data class MessageDateTimes(val sent: LocalDateTime, val statuses: List<MessageDateTimeStatus> = listOf())

/** The [dateTime] and [status] the [user] has on a message. */
data class MessageDateTimeStatus(val user: AccountInfo, val dateTime: LocalDateTime, val status: MessageStatus)

enum class MessageStatus { DELIVERED, READ }

data class DeletedMessage(val id: Int) : MessageUpdates()

/** Every message [until] the [LocalDateTime] has been deleted. */
data class MessageDeletionPoint(val until: LocalDateTime) : MessageUpdates()

/**
 * Every message the [userId] sent in the chat has been deleted. This happens when a group chat's member deletes their
 * account.
 */
data class UserChatMessagesRemoval(val userId: String) : MessageUpdates()

/**
 * Every message in the chat has been deleted.]
 *
 * This happens in private chats when the user deletes the chat, or the other user deletes their account. This happens
 * in group chats when the last user leaves the chat.
 */
data class DeletionOfEveryMessage(
    /**
     * GraphQL types require at least one field. Hence, we simply state that every message has been deleted. An
     * [IllegalArgumentException] will be thrown if this is `false`.
     */
    val isDeleted: Boolean = true
) : MessageUpdates() {
    init {
        if (!isDeleted) throw IllegalArgumentException("isDeleted must be true")
    }
}

/**
 * Lets clients know that the GraphQL subscription [isCreated]. It's to be sent only once, and will be the first event
 * sent.
 *
 * Subscriptions are handled using WebSockets. It takes a small amount of time for the WebSocket connection to be
 * created. After the connection has been created, it takes a small amount of time for the subscription to be created.
 * Although these delays may be imperceptible to humans, it's possible that an event, such as a newly created chat
 * message, was sent during one of these delays. For example, if the client was opening a user's chat, they might be
 * tempted to first query the previous messages, and then subscribe to new messages. However, this might cause a message
 * another user sent in the chat to be lost during one of the aforementioned delays. Therefore, the client should first
 * subscribe (i.e., await the WebSocket connection to be created), await the [CreatedSubscription] event, and then query
 * for older data if required.
 */
data class CreatedSubscription(
    /**
     * GraphQL types require at least one field. Hence, we simply state that the subscription has been created. An
     * [IllegalArgumentException] will be thrown if this is `false`.
     */
    val isCreated: Boolean = true
) {
    init {
        if (!isCreated) throw IllegalArgumentException("isCreated must be true")
    }
}

/** The [chat] the [messages] belong to. */
data class ChatMessages(val chat: Chat, val messages: List<Message>)