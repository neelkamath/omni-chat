package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.InvalidNewAdminIdException
import com.neelkamath.omniChat.graphql.UnauthorizedException
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

fun buildUpdateGroupChatQuery(): String = """
    mutation UpdateGroupChat(${"$"}update: GroupChatUpdate!) {
        updateGroupChat(update: ${"$"}update)
    }
"""

private fun operateUpdateGroupChat(accessToken: String, update: GroupChatUpdate): GraphQlResponse =
    operateQueryOrMutation(
        buildUpdateGroupChatQuery(),
        variables = mapOf("update" to update),
        accessToken = accessToken
    )

fun updateGroupChat(accessToken: String, update: GroupChatUpdate): Boolean =
    operateUpdateGroupChat(accessToken, update).data!!["updateGroupChat"] as Boolean

class UpdateGroupChatTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("Only the supplied fields should be updated") {
        val (admin, user1, user2) = createSignedInUsers(3)
        val initialUserIdList = setOf(user1.info.id)
        val chat = NewGroupChat("Title", "description", initialUserIdList)
        val chatId = createGroupChat(admin.accessToken, chat)
        val update = GroupChatUpdate(
            chatId,
            "New Title",
            newUserIdList = setOf(user2.info.id),
            removedUserIdList = setOf(user1.info.id)
        )
        updateGroupChat(admin.accessToken, update)
        val userIdList = initialUserIdList + admin.info.id + update.newUserIdList - update.removedUserIdList
        val users = userIdList.map(::findUserById).toSet()
        val connection = Messages.buildGroupChatConnection(chatId)
        val groupChat = GroupChat(chatId, admin.info.id, users, update.title!!, chat.description, connection)
        GroupChats.read(admin.info.id) shouldBe listOf(groupChat)
    }

    test("The chat's new admin should be set") {
        val (firstAdmin, secondAdmin) = createSignedInUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(secondAdmin.info.id))
        val chatId = createGroupChat(firstAdmin.accessToken, chat)
        val update = GroupChatUpdate(chatId, newAdminId = secondAdmin.info.id)
        updateGroupChat(firstAdmin.accessToken, update)
        GroupChats.read(chatId).adminId shouldBe secondAdmin.info.id
    }

    test("Transferring admin status to a user not in the chat should throw an exception") {
        val (admin, notInvitedUser) = createSignedInUsers(2)
        val chatId = createGroupChat(admin.accessToken, NewGroupChat("Title"))
        val update = GroupChatUpdate(chatId, newAdminId = notInvitedUser.info.id)
        val response = operateUpdateGroupChat(admin.accessToken, update)
        response.errors!![0].message shouldBe InvalidNewAdminIdException.message
    }

    test("Updating a nonexistent chat should throw an exception") {
        val token = createSignedInUsers(1)[0].accessToken
        val response = operateUpdateGroupChat(token, GroupChatUpdate(chatId = 1))
        response.errors!![0].message shouldBe InvalidChatIdException.message
    }

    test("Updating a chat the user isn't the admin of should throw an exception") {
        val (admin, user) = createSignedInUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(admin.accessToken, chat)
        val response = operateUpdateGroupChat(user.accessToken, GroupChatUpdate(chatId))
        response.errors!![0].message shouldBe UnauthorizedException.message
    }
}