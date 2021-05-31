package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object CreatedSubscription :
    ChatOnlineStatusesSubscription,
    ChatTypingStatusesSubscription,
    ChatMessagesSubscription,
    MessagesSubscription,
    AccountsSubscription,
    GroupChatsSubscription,
    OnlineStatusesSubscription,
    TypingStatusesSubscription {

    fun getPlaceholder() = Placeholder
}
