package com.neelkamath.omniChat.graphql.engine

import com.neelkamath.omniChat.graphql.operations.*
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring

/** Wires the GraphQL queries, mutations, and subscriptions to the [builder]. */
fun wireGraphQlOperations(builder: RuntimeWiring.Builder): RuntimeWiring.Builder =
    builder.type("Query", ::wireQuery).type("Mutation", ::wireMutation).type("Subscription", ::wireSubscription)

private fun wireQuery(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder
    .dataFetcher("readTypingUsers", ::readTypingUsers)
    .dataFetcher("readOnlineStatus", ::readOnlineStatus)
    .dataFetcher("readAccount", ::readAccount)
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
    .dataFetcher("searchBlockedUsers", ::searchBlockedUsers)
    .dataFetcher("readBlockedUsers", ::readBlockedUsers)
    .dataFetcher("readGroupChat", ::readGroupChat)
    .dataFetcher("searchPublicChats", ::searchPublicChats)

private fun wireMutation(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder
    .dataFetcher("deleteAccount", ::deleteAccount)
    .dataFetcher("createAccount", ::createAccount)
    .dataFetcher("verifyEmailAddress", ::verifyEmailAddress)
    .dataFetcher("blockUser", ::blockUser)
    .dataFetcher("unblockUser", ::unblockUser)
    .dataFetcher("setOnline", ::setOnline)
    .dataFetcher("updateAccount", ::updateAccount)
    .dataFetcher("deleteProfilePic", ::deleteProfilePic)
    .dataFetcher("deleteGroupChatPic", ::deleteGroupChatPic)
    .dataFetcher("emailEmailAddressVerification", ::emailEmailAddressVerification)
    .dataFetcher("updateGroupChatTitle", ::updateGroupChatTitle)
    .dataFetcher("resetPassword", ::resetPassword)
    .dataFetcher("updateGroupChatDescription", ::updateGroupChatDescription)
    .dataFetcher("addGroupChatUsers", ::addGroupChatUsers)
    .dataFetcher("removeGroupChatUsers", ::removeGroupChatUsers)
    .dataFetcher("leaveGroupChat", ::leaveGroupChat)
    .dataFetcher("makeGroupChatAdmins", ::makeGroupChatAdmins)
    .dataFetcher("createPollMessage", ::createPollMessage)
    .dataFetcher("setPollVote", ::setPollVote)
    .dataFetcher("joinGroupChat", ::joinGroupChat)
    .dataFetcher("joinPublicChat", ::joinPublicChat)
    .dataFetcher("createGroupChatInviteMessage", ::createGroupChatInviteMessage)
    .dataFetcher("setInvitability", ::setInvitability)
    .dataFetcher("forwardMessage", ::forwardMessage)
    .dataFetcher("createActionMessage", ::createActionMessage)
    .dataFetcher("triggerAction", ::triggerAction)
    .dataFetcher("emailPasswordResetCode", ::emailPasswordResetCode)
    .dataFetcher("createStatus", ::createStatus)
    .dataFetcher("unstar", ::unstar)
    .dataFetcher("createGroupChat", ::createGroupChat)
    .dataFetcher("setTyping", ::setTyping)
    .dataFetcher("deletePrivateChat", ::deletePrivateChat)
    .dataFetcher("createPrivateChat", ::createPrivateChat)
    .dataFetcher("createTextMessage", ::createTextMessage)
    .dataFetcher("setBroadcast", ::setBroadcast)
    .dataFetcher("star", ::star)
    .dataFetcher("deleteContacts", ::deleteContacts)
    .dataFetcher("createContact", ::createContact)
    .dataFetcher("deleteMessage", ::deleteMessage)

private fun wireSubscription(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder
    .dataFetcher("subscribeToMessages", ::subscribeToMessages)
    .dataFetcher("subscribeToAccounts", ::subscribeToAccounts)
    .dataFetcher("subscribeToGroupChats", ::subscribeToGroupChats)
    .dataFetcher("subscribeToTypingStatuses", ::subscribeToTypingStatuses)
    .dataFetcher("subscribeToOnlineStatuses", ::subscribeToOnlineStatuses)
