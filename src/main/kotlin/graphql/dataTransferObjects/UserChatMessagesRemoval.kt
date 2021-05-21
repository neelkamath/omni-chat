package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class UserChatMessagesRemoval(private val chatId: Int, private val userId: Int) : MessagesSubscription {
    fun getChatId(): Int = chatId

    fun getUserId(): Int = userId
}
