package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object InvalidChatId :
    ChatOnlineStatusesSubscription,
    ChatTypingStatusesSubscription,
    ChatAccountsSubscription,
    GroupChatMetadataSubscription,
    CreateTextMessageResult,
    ForwardMessageResult,
    SearchChatMessagesResult,
    ReadChatResult,
    LeaveGroupChatResult,
    CreatePrivateChatResult,
    CreateActionMessageResult,
    CreatePollMessageResult,
    CreateGroupChatInviteMessageResult,
    ChatMessagesSubscription {

    fun getPlaceholder() = Placeholder
}
