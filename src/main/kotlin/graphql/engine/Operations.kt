package com.neelkamath.omniChat.graphql.engine

import com.neelkamath.omniChat.graphql.operations.*
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring

/** Wires the GraphQL queries, mutations, and subscriptions to the [builder]. */
fun wireGraphQlOperations(builder: RuntimeWiring.Builder): RuntimeWiring.Builder =
    builder.type("Query", ::wireQuery).type("Mutation", ::wireMutation).type("Subscription", ::wireSubscription)

private fun wireQuery(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder
    .dataFetcher("canDeleteAccount", ::canDeleteAccount)
    .dataFetcher("readOnlineStatuses", ::readOnlineStatuses)
    .dataFetcher("readAccount", ::readAccount)
    .dataFetcher("isUsernameTaken", ::isUsernameTaken)
    .dataFetcher("isEmailAddressTaken", ::isEmailAddressTaken)
    .dataFetcher("readChat", ::readChat)
    .dataFetcher("readChats", ::readChats)
    .dataFetcher("searchChats", ::searchChats)
    .dataFetcher("readStars", ::readStars)
    .dataFetcher("readContacts", ::readContacts)
    .dataFetcher("searchContacts", ::searchContacts)
    .dataFetcher("searchMessages", ::searchMessages)
    .dataFetcher("requestTokenSet", ::requestTokenSet)
    .dataFetcher("refreshTokenSet", ::refreshTokenSet)
    .dataFetcher("searchChatMessages", ::searchChatMessages)
    .dataFetcher("searchUsers", ::searchUsers)

private fun wireMutation(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder
    .dataFetcher("deleteAccount", ::deleteAccount)
    .dataFetcher("createAccount", ::createAccount)
    .dataFetcher("setOnlineStatus", ::setOnlineStatus)
    .dataFetcher("updateAccount", ::updateAccount)
    .dataFetcher("deleteProfilePic", ::deleteProfilePic)
    .dataFetcher("deleteGroupChatPic", ::deleteGroupChatPic)
    .dataFetcher("sendEmailAddressVerification", ::sendEmailAddressVerification)
    .dataFetcher("updateGroupChatTitle", ::updateGroupChatTitle)
    .dataFetcher("updateGroupChatDescription", ::updateGroupChatDescription)
    .dataFetcher("addGroupChatUsers", ::addGroupChatUsers)
    .dataFetcher("removeGroupChatUsers", ::removeGroupChatUsers)
    .dataFetcher("makeGroupChatAdmins", ::makeGroupChatAdmins)
    .dataFetcher("createPollMessage", ::createPollMessage)
    .dataFetcher("setPollVote", ::setPollVote)
    .dataFetcher("resetPassword", ::resetPassword)
    .dataFetcher("createStatus", ::createStatus)
    .dataFetcher("deleteStar", ::deleteStar)
    .dataFetcher("createGroupChat", ::createGroupChat)
    .dataFetcher("setTyping", ::setTyping)
    .dataFetcher("deletePrivateChat", ::deletePrivateChat)
    .dataFetcher("createPrivateChat", ::createPrivateChat)
    .dataFetcher("createTextMessage", ::createTextMessage)
    .dataFetcher("setBroadcastStatus", ::setBroadcastStatus)
    .dataFetcher("star", ::star)
    .dataFetcher("deleteContacts", ::deleteContacts)
    .dataFetcher("createContacts", ::createContacts)
    .dataFetcher("deleteMessage", ::deleteMessage)

private fun wireSubscription(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder
    .dataFetcher("subscribeToMessages", ::subscribeToMessages)
    .dataFetcher("subscribeToContacts", ::subscribeToContacts)
    .dataFetcher("subscribeToUpdatedChats", ::subscribeToUpdatedChats)
    .dataFetcher("subscribeToNewGroupChats", ::subscribeToNewGroupChats)
    .dataFetcher("subscribeToTypingStatuses", ::subscribeToTypingStatuses)
    .dataFetcher("subscribeToOnlineStatuses", ::subscribeToOnlineStatuses)