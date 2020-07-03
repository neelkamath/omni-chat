package com.neelkamath.omniChat.graphql.operations.mutations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.Placeholder
import com.neelkamath.omniChat.db.tables.GroupChatDescription
import com.neelkamath.omniChat.db.tables.GroupChatTitle
import com.neelkamath.omniChat.db.tables.GroupChatUsers
import com.neelkamath.omniChat.graphql.AdminCannotLeaveException
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.objectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val LEAVE_GROUP_CHAT_QUERY = """
    mutation LeaveGroupChat(${"$"}chatId: Int!) {
        leaveGroupChat(chatId: ${"$"}chatId)
    }
"""

private fun operateLeaveGroupChat(accessToken: String, chatId: Int): GraphQlResponse = operateGraphQlQueryOrMutation(
    LEAVE_GROUP_CHAT_QUERY,
    variables = mapOf("chatId" to chatId),
    accessToken = accessToken
)

fun leaveGroupChat(accessToken: String, chatId: Int): Placeholder {
    val data = operateLeaveGroupChat(accessToken, chatId).data!!["leaveGroupChat"] as String
    return objectMapper.convertValue(data)
}

fun errLeaveGroupChat(accessToken: String, chatId: Int): String =
    operateLeaveGroupChat(accessToken, chatId).errors!![0].message

class LeaveGroupChatTest : FunSpec({
    test("A non-admin should leave the chat") {
        val (admin, user) = createSignedInUsers(2)
        val chat = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription(""), listOf(user.info.id))
        val chatId = createGroupChat(admin.accessToken, chat)
        leaveGroupChat(user.accessToken, chatId)
        GroupChatUsers.readUserIdList(chatId) shouldBe listOf(admin.info.id)
    }

    test("The admin should leave the chat if they're the only user") {
        val token = createSignedInUsers(1)[0].accessToken
        val chat = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription(""))
        val chatId = createGroupChat(token, chat)
        leaveGroupChat(token, chatId)
    }

    test("The admin shouldn't be allowed to leave if there are other users in the chat") {
        val (admin, user) = createSignedInUsers(2)
        val chat = NewGroupChat(GroupChatTitle("T"), GroupChatDescription(""), listOf(user.info.id))
        val chatId = createGroupChat(admin.accessToken, chat)
        errLeaveGroupChat(admin.accessToken, chatId) shouldBe AdminCannotLeaveException.message
    }

    test("Leaving a group chat the user is not in should throw an exception") {
        val token = createSignedInUsers(1)[0].accessToken
        errLeaveGroupChat(token, chatId = 1) shouldBe InvalidChatIdException.message
    }
})