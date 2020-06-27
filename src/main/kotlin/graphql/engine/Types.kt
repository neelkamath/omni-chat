package com.neelkamath.omniChat.graphql.engine

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.operations.GroupChatDto
import com.neelkamath.omniChat.graphql.operations.PrivateChatDto
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring

fun wireGraphQlTypes(builder: RuntimeWiring.Builder): RuntimeWiring.Builder = builder
    .type("Chat", ::wireChat)
    .type("MessageUpdate", ::wireMessageUpdate)
    .type("ContactUpdate", ::wireContactUpdate)
    .type("AccountData", ::wireAccountData)

private fun wireChat(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder.typeResolver {
    val type = when (val obj = it.getObject<Any>()) {
        is PrivateChat, is PrivateChatDto -> "PrivateChat"
        is GroupChat, is GroupChatDto -> "GroupChat"
        else -> throw Error("$obj was neither a PrivateChat nor a GroupChat.")
    }
    it.schema.getObjectType(type)
}

private fun wireMessageUpdate(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder =
    builder.typeResolver {
        val type = when (val obj = it.getObject<Any>()) {
            is CreatedSubscription -> "CreatedSubscription"
            is Message -> "Message"
            is DeletedMessage -> "DeletedMessage"
            is MessageDeletionPoint -> "MessageDeletionPoint"
            is UserChatMessagesRemoval -> "UserChatMessagesRemoval"
            is DeletionOfEveryMessage -> "DeletionOfEveryMessage"
            else -> throw Error(
                """
                $obj wasn't a CreatedSubscription, Message, DeletedMessage, MessageDeletionPoint, 
                UserChatMessagesRemoval, or DeletionOfEveryMessage.
                """.trimIndent()
            )
        }
        it.schema.getObjectType(type)
    }

private fun wireContactUpdate(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder =
    builder.typeResolver {
        val type = when (val obj = it.getObject<Any>()) {
            is CreatedSubscription -> "CreatedSubscription"
            is NewContact -> "NewContact"
            is UpdatedContact -> "UpdatedContact"
            is DeletedContact -> "DeletedContact"
            else -> throw Error("$obj wasn't a CreatedSubscription, NewContact, UpdatedContact, or DeletedContact.")
        }
        it.schema.getObjectType(type)
    }

private fun wireAccountData(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder.typeResolver {
    val type = when (val obj = it.getObject<Any>()) {
        is Account -> "Account"
        is UpdatedContact -> "UpdatedContact"
        is DeletedContact -> "DeletedContact"
        is NewContact -> "NewContact"
        else -> throw Error("$obj wasn't an Account, UpdatedContact, DeletedContact, or NewContact.")
    }
    it.schema.getObjectType(type)
}