package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object InvalidPoll : CreatePollMessageResult {
    fun getPlaceholder() = Placeholder
}
