package com.neelkamath.omniChat.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChat.graphql.routing.*
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import kotlin.test.*

const val FORWARD_MESSAGE_QUERY = """
    mutation ForwardMessage(${"$"}chatId: Int!, ${"$"}messageId: Int!, ${"$"}contextMessageId: Int) {
        forwardMessage(chatId: ${"$"}chatId, messageId: ${"$"}messageId, contextMessageId: ${"$"}contextMessageId)
    }
"""

private fun operateForwardMessage(
    userId: Int,
    chatId: Int,
    messageId: Int,
    contextMessageId: Int? = null
): GraphQlResponse = executeGraphQlViaEngine(
    FORWARD_MESSAGE_QUERY,
    mapOf("chatId" to chatId, "messageId" to messageId, "contextMessageId" to contextMessageId),
    userId
)

fun forwardMessage(userId: Int, chatId: Int, messageId: Int, contextMessageId: Int? = null): Placeholder {
    val data = operateForwardMessage(userId, chatId, messageId, contextMessageId).data!!["forwardMessage"] as String
    return testingObjectMapper.convertValue(data)
}

fun errForwardMessage(userId: Int, chatId: Int, messageId: Int, contextMessageId: Int? = null): String =
    operateForwardMessage(userId, chatId, messageId, contextMessageId).errors!![0].message

const val REMOVE_GROUP_CHAT_USERS_QUERY = """
    mutation RemoveGroupChatUsers(${"$"}chatId: Int!, ${"$"}userIdList: [Int!]!) {
        removeGroupChatUsers(chatId: ${"$"}chatId, userIdList: ${"$"}userIdList)
    }
"""

private fun operateRemoveGroupChatUsers(userId: Int, chatId: Int, userIdList: List<Int>): GraphQlResponse =
    executeGraphQlViaEngine(
        REMOVE_GROUP_CHAT_USERS_QUERY,
        mapOf("chatId" to chatId, "userIdList" to userIdList),
        userId
    )

fun removeGroupChatUsers(userId: Int, chatId: Int, userIdList: List<Int>): Placeholder {
    val data = operateRemoveGroupChatUsers(userId, chatId, userIdList).data!!["removeGroupChatUsers"] as String
    return testingObjectMapper.convertValue(data)
}

fun errRemoveGroupChatUsers(userId: Int, chatId: Int, userIdList: List<Int>): String =
    operateRemoveGroupChatUsers(userId, chatId, userIdList).errors!![0].message

const val SET_INVITABILITY_QUERY = """
    mutation SetInvitability(${"$"}chatId: Int!, ${"$"}isInvitable: Boolean!) {
        setInvitability(chatId: ${"$"}chatId, isInvitable: ${"$"}isInvitable)
    }
"""

private fun operateSetInvitability(userId: Int, chatId: Int, isInvitable: Boolean): GraphQlResponse =
    executeGraphQlViaEngine(SET_INVITABILITY_QUERY, mapOf("chatId" to chatId, "isInvitable" to isInvitable), userId)

fun setInvitability(userId: Int, chatId: Int, isInvitable: Boolean): Placeholder {
    val data = operateSetInvitability(userId, chatId, isInvitable).data!!["setInvitability"] as String
    return testingObjectMapper.convertValue(data)
}

fun errSetInvitability(userId: Int, chatId: Int, isInvitable: Boolean): String =
    operateSetInvitability(userId, chatId, isInvitable).errors!![0].message

const val CREATE_GROUP_CHAT_INVITE_MESSAGE_QUERY = """
    mutation CreateGroupChatInviteMessage(${"$"}chatId: Int!, ${"$"}invitedChatId: Int!, ${"$"}contextMessageId: Int) {
        createGroupChatInviteMessage(
            chatId: ${"$"}chatId
            invitedChatId: ${"$"}invitedChatId
            contextMessageId: ${"$"}contextMessageId
        )
    }
"""

private fun operateCreateGroupChatInviteMessage(
    userId: Int,
    chatId: Int,
    invitedChatId: Int,
    contextMessageId: Int? = null
): GraphQlResponse = executeGraphQlViaEngine(
    CREATE_GROUP_CHAT_INVITE_MESSAGE_QUERY,
    mapOf("chatId" to chatId, "invitedChatId" to invitedChatId, "contextMessageId" to contextMessageId),
    userId
)

fun createGroupChatInviteMessage(
    userId: Int,
    chatId: Int,
    invitedChatId: Int,
    contextMessageId: Int? = null
): Placeholder {
    val data = operateCreateGroupChatInviteMessage(userId, chatId, invitedChatId, contextMessageId)
        .data!!["createGroupChatInviteMessage"] as String
    return testingObjectMapper.convertValue(data)
}

fun errCreateGroupChatInviteMessage(
    userId: Int,
    chatId: Int,
    invitedChatId: Int,
    contextMessageId: Int? = null
): String = operateCreateGroupChatInviteMessage(userId, chatId, invitedChatId, contextMessageId).errors!![0].message

const val JOIN_GROUP_CHAT_QUERY = """
    mutation JoinGroupChat(${"$"}inviteCode: Uuid!) {
        joinGroupChat(inviteCode: ${"$"}inviteCode)
    }
"""

private fun operateJoinGroupChat(userId: Int, inviteCode: UUID): GraphQlResponse =
    executeGraphQlViaEngine(JOIN_GROUP_CHAT_QUERY, mapOf("inviteCode" to inviteCode), userId)

fun joinGroupChat(userId: Int, inviteCode: UUID): Placeholder {
    val data = operateJoinGroupChat(userId, inviteCode).data!!["joinGroupChat"] as String
    return testingObjectMapper.convertValue(data)
}

fun errJoinGroupChat(userId: Int, inviteCode: UUID): String =
    operateJoinGroupChat(userId, inviteCode).errors!![0].message

const val CREATE_POLL_MESSAGE_QUERY = """
    mutation CreatePollMessage(${"$"}chatId: Int!, ${"$"}poll: PollInput!, ${"$"}contextMessageId: Int) {
        createPollMessage(chatId: ${"$"}chatId, poll: ${"$"}poll, contextMessageId: ${"$"}contextMessageId)
    }
"""

private fun operateCreatePollMessage(
    userId: Int,
    chatId: Int,
    poll: PollInput,
    contextMessageId: Int? = null
): GraphQlResponse = executeGraphQlViaEngine(
    CREATE_POLL_MESSAGE_QUERY,
    mapOf("chatId" to chatId, "poll" to poll, "contextMessageId" to contextMessageId),
    userId
)

fun createPollMessage(userId: Int, chatId: Int, poll: PollInput, contextMessageId: Int? = null): Placeholder {
    val data = operateCreatePollMessage(userId, chatId, poll, contextMessageId).data!!["createPollMessage"] as String
    return testingObjectMapper.convertValue(data)
}

fun errCreatePollMessage(userId: Int, chatId: Int, poll: PollInput, contextMessageId: Int? = null): String =
    operateCreatePollMessage(userId, chatId, poll, contextMessageId).errors!![0].message

const val SET_POLL_VOTE_QUERY = """
    mutation SetPollVote(${"$"}messageId: Int!, ${"$"}option: MessageText!, ${"$"}vote: Boolean!) {
        setPollVote(messageId: ${"$"}messageId, option: ${"$"}option, vote: ${"$"}vote)
    }
"""

private fun operateSetPollVote(userId: Int, messageId: Int, option: MessageText, vote: Boolean): GraphQlResponse =
    executeGraphQlViaEngine(
        SET_POLL_VOTE_QUERY,
        mapOf("messageId" to messageId, "option" to option, "vote" to vote),
        userId
    )

fun setPollVote(userId: Int, messageId: Int, option: MessageText, vote: Boolean): Placeholder {
    val data = operateSetPollVote(userId, messageId, option, vote).data!!["setPollVote"] as String
    return testingObjectMapper.convertValue(data)
}

fun errSetPollVote(userId: Int, messageId: Int, option: MessageText, vote: Boolean): String =
    operateSetPollVote(userId, messageId, option, vote).errors!![0].message

const val SET_BROADCAST_STATUS_QUERY = """
    mutation SetBroadcastStatus(${"$"}chatId: Int!, ${"$"}isBroadcast: Boolean!) {
        setBroadcastStatus(chatId: ${"$"}chatId, isBroadcast: ${"$"}isBroadcast)
    }
"""

private fun operateSetBroadcastStatus(userId: Int, chatId: Int, isBroadcast: Boolean): GraphQlResponse =
    executeGraphQlViaEngine(SET_BROADCAST_STATUS_QUERY, mapOf("chatId" to chatId, "isBroadcast" to isBroadcast), userId)

fun setBroadcastStatus(userId: Int, chatId: Int, isBroadcast: Boolean): Placeholder {
    val data = operateSetBroadcastStatus(userId, chatId, isBroadcast).data!!["setBroadcastStatus"] as String
    return testingObjectMapper.convertValue(data)
}

const val MAKE_GROUP_CHAT_ADMINS_QUERY = """
    mutation MakeGroupChatAdmins(${"$"}chatId: Int!, ${"$"}userIdList: [Int!]!) {
        makeGroupChatAdmins(chatId: ${"$"}chatId, userIdList: ${"$"}userIdList)
    }
"""

private fun operateMakeGroupChatAdmins(userId: Int, chatId: Int, userIdList: List<Int>): GraphQlResponse =
    executeGraphQlViaEngine(MAKE_GROUP_CHAT_ADMINS_QUERY, mapOf("chatId" to chatId, "userIdList" to userIdList), userId)

fun makeGroupChatAdmins(userId: Int, chatId: Int, userIdList: List<Int>): Placeholder {
    val data = operateMakeGroupChatAdmins(userId, chatId, userIdList).data!!["makeGroupChatAdmins"] as String
    return testingObjectMapper.convertValue(data)
}

fun errMakeGroupChatAdmins(userId: Int, chatId: Int, userIdList: List<Int>): String =
    operateMakeGroupChatAdmins(userId, chatId, userIdList).errors!![0].message

const val ADD_GROUP_CHAT_USERS_QUERY = """
    mutation AddGroupChatUsers(${"$"}chatId: Int!, ${"$"}userIdList: [Int!]!) {
        addGroupChatUsers(chatId: ${"$"}chatId, userIdList: ${"$"}userIdList)
    }
"""

private fun operateAddGroupChatUsers(userId: Int, chatId: Int, userIdList: List<Int>): GraphQlResponse =
    executeGraphQlViaEngine(ADD_GROUP_CHAT_USERS_QUERY, mapOf("chatId" to chatId, "userIdList" to userIdList), userId)

fun addGroupChatUsers(userId: Int, chatId: Int, userIdList: List<Int>): Placeholder {
    val data = operateAddGroupChatUsers(userId, chatId, userIdList).data!!["addGroupChatUsers"] as String
    return testingObjectMapper.convertValue(data)
}

fun errAddGroupChatUsers(userId: Int, chatId: Int, userIdList: List<Int>): String =
    operateAddGroupChatUsers(userId, chatId, userIdList).errors!![0].message

const val UPDATE_GROUP_CHAT_DESCRIPTION_QUERY = """
    mutation UpdateGroupChatDescription(${"$"}chatId: Int!, ${"$"}description: GroupChatDescription!) {
        updateGroupChatDescription(chatId: ${"$"}chatId, description: ${"$"}description)
    }
"""

private fun operateUpdateGroupChatDescription(
    userId: Int,
    chatId: Int,
    description: GroupChatDescription
): GraphQlResponse = executeGraphQlViaEngine(
    UPDATE_GROUP_CHAT_DESCRIPTION_QUERY,
    mapOf("chatId" to chatId, "description" to description.value),
    userId
)

fun updateGroupChatDescription(userId: Int, chatId: Int, description: GroupChatDescription): Placeholder {
    val data =
        operateUpdateGroupChatDescription(userId, chatId, description).data!!["updateGroupChatDescription"] as String
    return testingObjectMapper.convertValue(data)
}

const val UPDATE_GROUP_CHAT_TITLE_QUERY = """
    mutation UpdateGroupChatTitle(${"$"}chatId: Int!, ${"$"}title: GroupChatTitle!) {
        updateGroupChatTitle(chatId: ${"$"}chatId, title: ${"$"}title)
    }
"""

private fun operateUpdateGroupChatTitle(userId: Int, chatId: Int, title: GroupChatTitle): GraphQlResponse =
    executeGraphQlViaEngine(UPDATE_GROUP_CHAT_TITLE_QUERY, mapOf("chatId" to chatId, "title" to title.value), userId)

fun updateGroupChatTitle(userId: Int, chatId: Int, title: GroupChatTitle): Placeholder {
    val data = operateUpdateGroupChatTitle(userId, chatId, title).data!!["updateGroupChatTitle"] as String
    return testingObjectMapper.convertValue(data)
}

const val DELETE_STAR_QUERY = """
    mutation DeleteStar(${"$"}messageId: Int!) {
        deleteStar(messageId: ${"$"}messageId)
    }
"""

private fun operateDeleteStar(userId: Int, messageId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_STAR_QUERY, mapOf("messageId" to messageId), userId)

fun deleteStar(userId: Int, messageId: Int): Placeholder {
    val data = operateDeleteStar(userId, messageId).data!!["deleteStar"] as String
    return testingObjectMapper.convertValue(data)
}

const val STAR_QUERY = """
    mutation Star(${"$"}messageId: Int!) {
        star(messageId: ${"$"}messageId)
    }
"""

private fun operateStar(userId: Int, messageId: Int): GraphQlResponse =
    executeGraphQlViaEngine(STAR_QUERY, mapOf("messageId" to messageId), userId)

fun star(userId: Int, messageId: Int): Placeholder {
    val data = operateStar(userId, messageId).data!!["star"] as String
    return testingObjectMapper.convertValue(data)
}

fun errStar(userId: Int, messageId: Int): String = operateStar(userId, messageId).errors!![0].message

const val SET_ONLINE_STATUS_QUERY = """
    mutation SetOnlineStatus(${"$"}isOnline: Boolean!) {
        setOnlineStatus(isOnline: ${"$"}isOnline)
    }
"""

private fun operateSetOnlineStatus(userId: Int, isOnline: Boolean): GraphQlResponse =
    executeGraphQlViaEngine(SET_ONLINE_STATUS_QUERY, mapOf("isOnline" to isOnline), userId)

fun setOnlineStatus(userId: Int, isOnline: Boolean): Placeholder {
    val data = operateSetOnlineStatus(userId, isOnline).data!!["setOnlineStatus"] as String
    return testingObjectMapper.convertValue(data)
}

const val SET_TYPING_QUERY = """
    mutation SetTyping(${"$"}chatId: Int!, ${"$"}isTyping: Boolean!) {
        setTyping(chatId: ${"$"}chatId, isTyping: ${"$"}isTyping)
    }
"""

private fun operateSetTyping(userId: Int, chatId: Int, isTyping: Boolean): GraphQlResponse =
    executeGraphQlViaEngine(SET_TYPING_QUERY, mapOf("chatId" to chatId, "isTyping" to isTyping), userId)

fun setTyping(userId: Int, chatId: Int, isTyping: Boolean): Placeholder {
    val data = operateSetTyping(userId, chatId, isTyping).data!!["setTyping"] as String
    return testingObjectMapper.convertValue(data)
}

fun errSetTyping(userId: Int, chatId: Int, isTyping: Boolean): String =
    operateSetTyping(userId, chatId, isTyping).errors!![0].message

const val DELETE_GROUP_CHAT_PIC_QUERY = """
    mutation DeleteGroupChatPic(${"$"}chatId: Int!) {
        deleteGroupChatPic(chatId: ${"$"}chatId)
    }
"""

private fun operateDeleteGroupChatPic(userId: Int, chatId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_GROUP_CHAT_PIC_QUERY, mapOf("chatId" to chatId), userId)

fun deleteGroupChatPic(userId: Int, chatId: Int): Placeholder {
    val data = operateDeleteGroupChatPic(userId, chatId).data!!["deleteGroupChatPic"] as String
    return testingObjectMapper.convertValue(data)
}

const val DELETE_PROFILE_PIC_QUERY = """
    mutation DeleteProfilePic {
        deleteProfilePic
    }
"""

private fun operateDeleteProfilePic(userId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_PROFILE_PIC_QUERY, userId = userId)

fun deleteProfilePic(userId: Int): Placeholder {
    val data = operateDeleteProfilePic(userId).data!!["deleteProfilePic"] as String
    return testingObjectMapper.convertValue(data)
}

const val CREATE_ACCOUNTS_QUERY = """
    mutation CreateAccount(${"$"}account: AccountInput!) {
        createAccount(account: ${"$"}account)
    }
"""

private fun operateCreateAccount(account: AccountInput): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_ACCOUNTS_QUERY, mapOf("account" to account))

fun createAccount(account: AccountInput): Placeholder {
    val data = operateCreateAccount(account).data!!["createAccount"] as String
    return testingObjectMapper.convertValue(data)
}

fun errCreateAccount(account: AccountInput): String = operateCreateAccount(account).errors!![0].message

const val CREATE_CONTACTS_QUERY = """
    mutation CreateContacts(${"$"}userIdList: [Int!]!) {
        createContacts(userIdList: ${"$"}userIdList)
    }
"""

private fun operateCreateContacts(userId: Int, userIdList: List<Int>): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_CONTACTS_QUERY, mapOf("userIdList" to userIdList), userId)

fun createContacts(userId: Int, userIdList: List<Int>): Placeholder {
    val data = operateCreateContacts(userId, userIdList).data!!["createContacts"] as String
    return testingObjectMapper.convertValue(data)
}

fun errCreateContacts(userId: Int, userIdList: List<Int>): String =
    operateCreateContacts(userId, userIdList).errors!![0].message

const val CREATE_GROUP_CHAT_QUERY = """
    mutation CreateGroupChat(${"$"}chat: GroupChatInput!) {
        createGroupChat(chat: ${"$"}chat)
    }
"""

private fun operateCreateGroupChat(userId: Int, chat: GroupChatInput): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_GROUP_CHAT_QUERY, mapOf("chat" to chat), userId)

fun errCreateGroupChat(userId: Int, chat: GroupChatInput): String =
    operateCreateGroupChat(userId, chat).errors!![0].message

const val CREATE_MESSAGE_QUERY = """
    mutation CreateTextMessage(${"$"}chatId: Int!, ${"$"}text: MessageText!, ${"$"}contextMessageId: Int) {
        createTextMessage(chatId: ${"$"}chatId, text: ${"$"}text, contextMessageId: ${"$"}contextMessageId)
    }
"""

private fun operateCreateTextMessage(
    userId: Int,
    chatId: Int,
    text: MessageText,
    contextMessageId: Int? = null
): GraphQlResponse = executeGraphQlViaEngine(
    CREATE_MESSAGE_QUERY,
    mapOf("chatId" to chatId, "text" to text, "contextMessageId" to contextMessageId),
    userId
)

fun createTextMessage(userId: Int, chatId: Int, text: MessageText, contextMessageId: Int? = null): Placeholder {
    val data = operateCreateTextMessage(userId, chatId, text, contextMessageId).data!!["createTextMessage"] as String
    return testingObjectMapper.convertValue(data)
}

fun errCreateTextMessage(userId: Int, chatId: Int, text: MessageText, contextMessageId: Int? = null): String =
    operateCreateTextMessage(userId, chatId, text, contextMessageId).errors!![0].message

const val CREATE_PRIVATE_CHAT_QUERY = """
    mutation CreatePrivateChat(${"$"}userId: Int!) {
        createPrivateChat(userId: ${"$"}userId)
    }
"""

private fun operateCreatePrivateChat(userId: Int, otherUserId: Int): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_PRIVATE_CHAT_QUERY, mapOf("userId" to otherUserId), userId)

fun createPrivateChat(userId: Int, otherUserId: Int): Int =
    operateCreatePrivateChat(userId, otherUserId).data!!["createPrivateChat"] as Int

fun errCreatePrivateChat(userId: Int, otherUserId: Int): String =
    operateCreatePrivateChat(userId, otherUserId).errors!![0].message

const val CREATE_STATUS_QUERY = """
    mutation CreateStatus(${"$"}messageId: Int!, ${"$"}status: MessageStatus!) {
        createStatus(messageId: ${"$"}messageId, status: ${"$"}status)
    }
"""

private fun operateCreateStatus(userId: Int, messageId: Int, status: MessageStatus): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_STATUS_QUERY, mapOf("messageId" to messageId, "status" to status), userId)

fun createStatus(userId: Int, messageId: Int, status: MessageStatus): Placeholder {
    val data = operateCreateStatus(userId, messageId, status).data!!["createStatus"] as String
    return testingObjectMapper.convertValue(data)
}

fun errCreateStatus(userId: Int, messageId: Int, status: MessageStatus): String =
    operateCreateStatus(userId, messageId, status).errors!![0].message

const val DELETE_ACCOUNT_QUERY = """
    mutation DeleteAccount {
        deleteAccount
    }
"""

private fun operateDeleteAccount(userId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_ACCOUNT_QUERY, userId = userId)

fun deleteAccount(userId: Int): Placeholder {
    val data = operateDeleteAccount(userId).data!!["deleteAccount"] as String
    return testingObjectMapper.convertValue(data)
}

fun errDeleteAccount(userId: Int): String = operateDeleteAccount(userId).errors!![0].message

const val DELETE_CONTACTS_QUERY = """
    mutation DeleteContacts(${"$"}userIdList: [Int!]!) {
        deleteContacts(userIdList: ${"$"}userIdList)
    }
"""

private fun operateDeleteContacts(userId: Int, userIdList: List<Int>): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_CONTACTS_QUERY, mapOf("userIdList" to userIdList), userId)

fun deleteContacts(userId: Int, userIdList: List<Int>): Placeholder {
    val data = operateDeleteContacts(userId, userIdList).data!!["deleteContacts"] as String
    return testingObjectMapper.convertValue(data)
}

const val DELETE_MESSAGE_QUERY = """
    mutation DeleteMessage(${"$"}id: Int!) {
        deleteMessage(id: ${"$"}id)
    }
"""

private fun operateDeleteMessage(userId: Int, messageId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_MESSAGE_QUERY, mapOf("id" to messageId), userId)

fun deleteMessage(userId: Int, messageId: Int): Placeholder {
    val data = operateDeleteMessage(userId, messageId).data!!["deleteMessage"] as String
    return testingObjectMapper.convertValue(data)
}

fun errDeleteMessage(userId: Int, messageId: Int): String =
    operateDeleteMessage(userId, messageId).errors!![0].message

const val DELETE_PRIVATE_CHAT_QUERY = """
    mutation DeletePrivateChat(${"$"}chatId: Int!) {
        deletePrivateChat(chatId: ${"$"}chatId)
    }
"""

private fun operateDeletePrivateChat(userId: Int, chatId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_PRIVATE_CHAT_QUERY, mapOf("chatId" to chatId), userId)

fun deletePrivateChat(userId: Int, chatId: Int): Placeholder {
    val data = operateDeletePrivateChat(userId, chatId).data!!["deletePrivateChat"] as String
    return testingObjectMapper.convertValue(data)
}

fun errDeletePrivateChat(userId: Int, chatId: Int): String =
    operateDeletePrivateChat(userId, chatId).errors!![0].message

const val RESET_PASSWORD_QUERY = """
    mutation ResetPassword(${"$"}emailAddress: String!) {
        resetPassword(emailAddress: ${"$"}emailAddress)
    }
"""

private fun operateResetPassword(emailAddress: String): GraphQlResponse =
    executeGraphQlViaEngine(RESET_PASSWORD_QUERY, mapOf("emailAddress" to emailAddress))

fun resetPassword(emailAddress: String): Placeholder {
    val data = operateResetPassword(emailAddress).data!!["resetPassword"] as String
    return testingObjectMapper.convertValue(data)
}

fun errResetPassword(emailAddress: String): String = operateResetPassword(emailAddress).errors!![0].message

const val SEND_EMAIL_ADDRESS_VERIFICATION_QUERY = """
    mutation SendEmailAddressVerification(${"$"}emailAddress: String!) {
        sendEmailAddressVerification(emailAddress: ${"$"}emailAddress)
    }
"""

private fun operateSendEmailAddressVerification(emailAddress: String): GraphQlResponse =
    executeGraphQlViaEngine(SEND_EMAIL_ADDRESS_VERIFICATION_QUERY, mapOf("emailAddress" to emailAddress))

fun sendEmailAddressVerification(emailAddress: String): Placeholder {
    val data = operateSendEmailAddressVerification(emailAddress).data!!["sendEmailAddressVerification"] as String
    return testingObjectMapper.convertValue(data)
}

fun errSendEmailVerification(emailAddress: String): String =
    operateSendEmailAddressVerification(emailAddress).errors!![0].message

const val UPDATE_ACCOUNT_QUERY = """
    mutation UpdateAccount(${"$"}update: AccountUpdate!) {
        updateAccount(update: ${"$"}update)
    }
"""

private fun operateUpdateAccount(userId: Int, update: AccountUpdate): GraphQlResponse =
    executeGraphQlViaEngine(UPDATE_ACCOUNT_QUERY, mapOf("update" to update), userId)

fun updateAccount(userId: Int, update: AccountUpdate): Placeholder {
    val data = operateUpdateAccount(userId, update).data!!["updateAccount"] as String
    return testingObjectMapper.convertValue(data)
}

fun errUpdateAccount(userId: Int, update: AccountUpdate): String =
    operateUpdateAccount(userId, update).errors!![0].message

@ExtendWith(DbExtension::class)
class MutationsTest {
    @Nested
    inner class ForwardMessage {
        @Test
        fun `The message should be forwarded with a context`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chat1Id, chat2Id) = listOf(1, 2).map { GroupChats.create(listOf(adminId)) }
            val messageId = Messages.message(adminId, chat1Id)
            val contextMessageId = Messages.message(adminId, chat2Id)
            forwardMessage(adminId, chat2Id, messageId, contextMessageId)
            with(Messages.readGroupChat(chat2Id).last().node) {
                assertEquals(contextMessageId, context.id)
                assertTrue(isForwarded)
            }
        }

        @Test
        fun `A non-admin shouldn't be allowed to forward a message to a broadcast chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id), isBroadcast = true)
            val messageId = Messages.message(admin.info.id, chatId)
            val response = executeGraphQlViaHttp(
                FORWARD_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "messageId" to messageId),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `Messaging in a chat the user isn't in should fail`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val (chat1Id, chat2Id) = listOf(admin1Id, admin2Id).map { GroupChats.create(listOf(it)) }
            val messageId = Messages.message(admin1Id, chat1Id)
            assertEquals(InvalidChatIdException.message, errForwardMessage(admin1Id, chat2Id, messageId))
        }

        @Test
        fun `Forwarding a nonexistent message should fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            assertEquals(InvalidMessageIdException.message, errForwardMessage(adminId, chatId, messageId = 1))
        }

        @Test
        fun `Using an invalid context message should fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            val response = errForwardMessage(adminId, chatId, messageId, contextMessageId = 1)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `Forwarding a message the user can't see should fail`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val (chat1Id, chat2Id) = listOf(admin1Id, admin2Id).map { GroupChats.create(listOf(it)) }
            val messageId = Messages.message(admin1Id, chat1Id)
            assertEquals(InvalidMessageIdException.message, errForwardMessage(admin2Id, chat2Id, messageId))
        }
    }

    @Nested
    inner class SetInvitability {
        @Test
        fun `The chat's invitability should be updated`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            setInvitability(adminId, chatId, isInvitable = true)
            assertEquals(GroupChatPublicity.INVITABLE, GroupChats.readChat(chatId).publicity)
        }

        @Test
        fun `Updating a public chat should fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            assertEquals(InvalidChatIdException.message, errSetInvitability(adminId, chatId, isInvitable = true))
        }

        @Test
        fun `An error should be returned when a non-admin updates the invitability`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                SET_INVITABILITY_QUERY,
                mapOf("chatId" to chatId, "isInvitable" to true),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class CreateGroupChatInviteMessage {
        @Test
        fun `A message should be created with a context`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chatId, invitedChatId) = listOf(1, 2)
                .map { GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.INVITABLE) }
            val contextMessageId = Messages.message(adminId, chatId, MessageText("t"))
            createGroupChatInviteMessage(adminId, chatId, invitedChatId, contextMessageId)
            assertEquals(1, GroupChatInviteMessages.count())
        }

        @Test
        fun `Messaging in a broadcast chat should fail`() {
            val (admin1, admin2) = createVerifiedUsers(2)
            val chatId = GroupChats.create(
                adminIdList = listOf(admin1.info.id),
                userIdList = listOf(admin2.info.id),
                isBroadcast = true
            )
            val invitedChatId = GroupChats.create(listOf(admin2.info.id), publicity = GroupChatPublicity.INVITABLE)
            val response = executeGraphQlViaHttp(
                CREATE_GROUP_CHAT_INVITE_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "invitedChatId" to invitedChatId),
                admin2.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `Creating a message in a chat the user isn't in should fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val invitedChatId = GroupChats.create(listOf(adminId))
            val response = errCreateGroupChatInviteMessage(adminId, chatId = 1, invitedChatId = invitedChatId)
            assertEquals(InvalidChatIdException.message, response)
        }

        @Test
        fun `Inviting users to a private chat should fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminIdList = listOf(user1Id))
            val invitedChatId = PrivateChats.create(user1Id, user2Id)
            val response = errCreateGroupChatInviteMessage(user1Id, chatId, invitedChatId)
            assertEquals(InvalidInvitedChatException.message, response)
        }

        @Test
        fun `Inviting users to a group chat with invites turned off should fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chatId, invitedChatId) = listOf(1, 2).map { GroupChats.create(listOf(adminId)) }
            val response = errCreateGroupChatInviteMessage(adminId, chatId, invitedChatId)
            assertEquals(InvalidInvitedChatException.message, response)
        }

        @Test
        fun `Using an invalid content message should fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chatId, invitedChatId) = listOf(1, 2)
                .map { GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.INVITABLE) }
            val response = errCreateGroupChatInviteMessage(adminId, chatId, invitedChatId, contextMessageId = 1)
            assertEquals(InvalidMessageIdException.message, response)
        }
    }

    @Nested
    inner class JoinGroupChat {
        @Test
        fun `An invite code should be used to join the chat, even if the chat has already been joined`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            repeat(2) { joinGroupChat(userId, GroupChats.readInviteCode(chatId)) }
            assertEquals(setOf(adminId, userId), GroupChatUsers.readUserIdList(chatId).toSet())
        }

        @Test
        fun `Using an invalid invite code should fail`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidInviteCodeException.message, errJoinGroupChat(userId, inviteCode = UUID.randomUUID()))
        }
    }

    @Nested
    inner class CreatePollMessage {
        @Test
        fun `A poll message should be created with a context`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val contextMessageId = Messages.message(adminId, chatId)
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            createPollMessage(adminId, chatId, poll, contextMessageId)
            val message = Messages.readGroupChat(chatId, userId = adminId).last().node
            assertEquals(contextMessageId, message.context.id)
            val options = poll.options.map { PollOption(it, votes = listOf()) }
            assertEquals(Poll(poll.title, options), PollMessages.read(message.messageId))
        }

        @Test
        fun `Messaging a poll in a chat the user isn't in should fail`() {
            val userId = createVerifiedUsers(1)[0].info.id
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            assertEquals(InvalidChatIdException.message, errCreatePollMessage(userId, chatId = 1, poll = poll))
        }

        @Test
        fun `Creating a poll in response to a nonexistent message should fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            val response = errCreatePollMessage(adminId, chatId, poll, contextMessageId = 1)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `Using an invalid poll should fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val poll = mapOf("title" to "Title", "options" to listOf("option"))
            val response = executeGraphQlViaEngine(
                CREATE_POLL_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "poll" to poll, "contextMessageId" to null),
                adminId
            ).errors!![0].message
            assertEquals(InvalidPollException.message, response)
        }
    }

    @Nested
    inner class SetPollVote {
        @Test
        fun `The user's vote should be updated`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val option = MessageText("option 1")
            val poll = PollInput(MessageText("Title"), listOf(option, MessageText("option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            setPollVote(adminId, messageId, option, vote = true)
            assertEquals(listOf(adminId), PollMessages.read(messageId).options.first { it.option == option }.votes)
        }

        @Test
        fun `Voting on a nonexistent poll should fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val response = errSetPollVote(adminId, messageId = 1, option = MessageText("option"), vote = true)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `Voting for a nonexistent option should fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            val response = errSetPollVote(adminId, messageId, MessageText("nonexistent option"), vote = true)
            assertEquals(NonexistentOptionException.message, response)
        }
    }

    @Nested
    inner class SetBroadcastStatus {
        @Test
        fun `Only an admin should be allowed to set the broadcast status`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                SET_BROADCAST_STATUS_QUERY,
                mapOf("chatId" to chatId, "isBroadcast" to true),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `The broadcast status should be updated`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val isBroadcast = true
            setBroadcastStatus(adminId, chatId, isBroadcast)
            assertEquals(isBroadcast, GroupChats.readChat(chatId, userId = adminId).isBroadcast)
        }
    }

    @Nested
    inner class MakeGroupChatAdmins {
        @Test
        fun `The users should be made admins`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            makeGroupChatAdmins(adminId, chatId, listOf(userId))
            assertEquals(listOf(adminId, userId), GroupChatUsers.readAdminIdList(chatId))
        }

        @Test
        fun `Making a user who isn't in the chat an admin should fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            assertEquals(InvalidUserIdException.message, errMakeGroupChatAdmins(adminId, chatId, listOf(userId)))
        }

        @Test
        fun `A non-admin shouldn't be allowed to make users admins`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                MAKE_GROUP_CHAT_ADMINS_QUERY,
                mapOf("chatId" to chatId, "userIdList" to listOf<Int>()),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class AddGroupChatUsers {
        @Test
        fun `Users should be added to the chat while ignoring duplicates and existing users`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            addGroupChatUsers(adminId, chatId, listOf(adminId, userId, userId))
            assertEquals(listOf(adminId, userId), GroupChatUsers.readUserIdList(chatId))
        }

        @Test
        fun `An exception should be thrown when adding a nonexistent user`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val invalidUserId = -1
            assertEquals(InvalidUserIdException.message, errAddGroupChatUsers(adminId, chatId, listOf(invalidUserId)))
        }

        @Test
        fun `A non-admin shouldn't be allowed to update the chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                ADD_GROUP_CHAT_USERS_QUERY,
                mapOf("chatId" to chatId, "userIdList" to listOf<Int>()),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class RemoveGroupChatUsers {
        @Test
        fun `The admin should be allowed to remove themselves along with non-admins if they aren't the last admin`() {
            val (admin1Id, admin2Id, userId) = createVerifiedUsers(3).map { it.info.id }
            val chatId = GroupChats.create(listOf(admin1Id, admin2Id), listOf(userId))
            removeGroupChatUsers(admin1Id, chatId, listOf(admin1Id, userId))
            assertEquals(listOf(admin2Id), GroupChats.readChat(chatId).users.edges.map { it.node.id })
        }

        @Test
        fun `Removing the last admin should be allowed if there won't be any remaining users`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            removeGroupChatUsers(adminId, chatId, listOf(adminId, userId))
            assertEquals(0, GroupChats.count())
        }

        @Test
        fun `Removing the last admin shouldn't be allowed if there are other users`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            assertEquals(InvalidUserIdException.message, errRemoveGroupChatUsers(adminId, chatId, listOf(adminId)))
        }

        @Test
        fun `Removing a nonexistent user should fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            assertEquals(InvalidUserIdException.message, errRemoveGroupChatUsers(adminId, chatId, listOf(-1)))
        }
    }

    @Nested
    inner class UpdateGroupChatDescription {
        @Test
        fun `The admin should update the description`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val description = GroupChatDescription("New description.")
            updateGroupChatDescription(adminId, chatId, description)
            assertEquals(description, GroupChats.readChat(chatId, userId = adminId).description)
        }

        @Test
        fun `A non-admin shouldn't be allowed to update the chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                UPDATE_GROUP_CHAT_DESCRIPTION_QUERY,
                mapOf("chatId" to chatId, "description" to "d"),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class UpdateGroupChatTitle {
        @Test
        fun `The admin should update the title`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val title = GroupChatTitle("New Title")
            updateGroupChatTitle(adminId, chatId, title)
            assertEquals(title, GroupChats.readChat(chatId, userId = adminId).title)
        }

        @Test
        fun `A non-admin shouldn't be allowed to update the chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                UPDATE_GROUP_CHAT_TITLE_QUERY,
                mapOf("chatId" to chatId, "title" to "T"),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class DeleteStar {
        @Test
        fun `A message should be starred`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
            deleteStar(adminId, messageId)
            assertFalse(Stargazers.hasStar(adminId, messageId))
        }
    }

    @Nested
    inner class Star {
        @Test
        fun `A message should be starred`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            star(adminId, messageId)
            assertEquals(listOf(messageId), Stargazers.read(adminId))
        }

        @Test
        fun `Starring a message from a chat the user isn't in should fail`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(admin1Id))
            val messageId = Messages.message(admin1Id, chatId)
            assertEquals(InvalidMessageIdException.message, errStar(admin2Id, messageId))
        }
    }

    @Nested
    inner class SetOnlineStatus {
        fun assertOnlineStatus(isOnline: Boolean) {
            val userId = createVerifiedUsers(1)[0].info.id
            setOnlineStatus(userId, isOnline)
            assertEquals(isOnline, Users.read(userId).isOnline)
        }

        @Test
        fun `The user's online status should be set to "true"`() {
            assertOnlineStatus(true)
        }

        @Test
        fun `The user's online status should be set to "false"`() {
            assertOnlineStatus(false)
        }
    }

    @Nested
    inner class SetTyping {
        fun assertTypingStatus(isTyping: Boolean) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            setTyping(adminId, chatId, isTyping)
            assertEquals(isTyping, TypingStatuses.read(chatId, adminId))
        }

        @Test
        fun `The user's typing status should be set to "true"`() {
            assertTypingStatus(isTyping = false)
        }

        @Test
        fun `Setting the typing status in a chat the user isn't in should fail`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidChatIdException.message, errSetTyping(userId, chatId = 1, isTyping = true))
        }
    }

    @Nested
    inner class DeleteGroupChatPic {
        @Test
        fun `Deleting the pic should remove it`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            GroupChats.updatePic(chatId, Pic(ByteArray(1), Pic.Type.PNG))
            deleteGroupChatPic(adminId, chatId)
            assertNull(GroupChats.readPic(chatId))
        }

        @Test
        fun `An exception should be thrown when a non-admin updates the pic`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                DELETE_GROUP_CHAT_PIC_QUERY,
                mapOf("chatId" to chatId),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class DeleteProfilePic {
        @Test
        fun `The user's profile pic should be deleted`() {
            val userId = createVerifiedUsers(1)[0].info.id
            val pic = Pic(ByteArray(1), Pic.Type.PNG)
            Users.updatePic(userId, pic)
            deleteProfilePic(userId)
            assertNull(Users.read(userId).pic)
        }
    }

    @Nested
    inner class CreateAccount {
        @Test
        fun `Creating an account should save it to the auth system, and the DB`() {
            val account = AccountInput(Username("username"), Password("password"), "username@example.com")
            createAccount(account)
            with(readUserByUsername(account.username)) {
                assertEquals(account.username, username)
                assertEquals(account.emailAddress, emailAddress)
            }
            assertEquals(1, Users.count())
        }

        @Test
        fun `An account with a taken username shouldn't be created`() {
            val account = AccountInput(Username("username"), Password("password"), "username@example.com")
            createAccount(account)
            assertEquals(UsernameTakenException.message, errCreateAccount(account))
        }

        @Test
        fun `An account with a taken email shouldn't be created`() {
            val address = "username@example.com"
            val account = AccountInput(Username("username1"), Password("p"), address)
            createAccount(account)
            val duplicateAccount = AccountInput(Username("username2"), Password("p"), address)
            assertEquals(EmailAddressTakenException.message, errCreateAccount(duplicateAccount))
        }

        @Test
        fun `An account with a disallowed email address domain shouldn't be created`() {
            val response = errCreateAccount(AccountInput(Username("u"), Password("p"), "bob@outlook.com"))
            assertEquals(InvalidDomainException.message, response)
        }
    }

    @Nested
    inner class CreateContacts {
        @Test
        fun `Trying to save the user's own contact should be ignored`() {
            val (ownerId, userId) = createVerifiedUsers(2).map { it.info.id }
            createContacts(ownerId, listOf(ownerId, userId))
            assertEquals(listOf(userId), Contacts.readIdList(ownerId))
        }

        @Test
        fun `If one of the contacts to be saved is invalid, then none of them should be saved`() {
            val (ownerId, userId) = createVerifiedUsers(2).map { it.info.id }
            val contacts = listOf(userId, -1)
            assertEquals(InvalidContactException.message, errCreateContacts(ownerId, contacts))
            assertTrue(Contacts.readIdList(ownerId).isEmpty())
        }
    }

    @Nested
    inner class CreateGroupChat {
        @Test
        fun `A group chat should be created automatically including the creator as a user and admin`() {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val chat = mapOf(
                "title" to "Title",
                "description" to "description",
                "userIdList" to listOf(user1Id, user2Id),
                "adminIdList" to listOf<Int>(),
                "isBroadcast" to false,
                "publicity" to GroupChatPublicity.NOT_INVITABLE
            )
            val chatId = executeGraphQlViaEngine(CREATE_GROUP_CHAT_QUERY, mapOf("chat" to chat), adminId)
                .data!!["createGroupChat"] as Int
            val chats = GroupChats.readUserChats(adminId)
            assertEquals(1, chats.size)
            with(chats[0]) {
                assertEquals(chatId, id)
                assertEquals(setOf(adminId, user1Id, user2Id), users.edges.map { it.node.id }.toSet())
                assertEquals(listOf(adminId), adminIdList)
            }
        }

        @Test
        fun `A group chat shouldn't be created when supplied with an invalid user ID`() {
            val userId = createVerifiedUsers(1)[0].info.id
            val invalidUserId = -1
            val chat = GroupChatInput(
                GroupChatTitle("T"),
                GroupChatDescription(""),
                userIdList = listOf(userId, invalidUserId),
                adminIdList = listOf(userId),
                isBroadcast = false,
                publicity = GroupChatPublicity.NOT_INVITABLE
            )
            assertEquals(InvalidUserIdException.message, errCreateGroupChat(userId, chat))
        }

        @Test
        fun `A group chat shouldn't be created if the admin ID list isn't a subset of the user ID list`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chat = mapOf(
                "title" to "Title",
                "description" to "description",
                "userIdList" to listOf<Int>(),
                "adminIdList" to listOf(user2Id),
                "isBroadcast" to false,
                "publicity" to GroupChatPublicity.NOT_INVITABLE
            )
            val response = executeGraphQlViaEngine(
                CREATE_GROUP_CHAT_QUERY,
                mapOf("chat" to chat),
                user1Id
            ).errors!![0].message
            assertEquals(InvalidAdminIdException.message, response)
        }
    }

    @Nested
    inner class CreateTextMessage {
        @Test
        fun `The user should be able to create a message in a private chat they just deleted`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            createTextMessage(user1Id, chatId, MessageText("t"))
        }

        @Test
        fun `Messaging in a chat the user isn't in should throw an exception`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(admin1Id))
            GroupChats.create(listOf(admin2Id))
            assertEquals(InvalidChatIdException.message, errCreateTextMessage(admin2Id, chatId, MessageText("t")))
        }

        @Test
        fun `The message should be created sans context`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            createTextMessage(adminId, chatId, MessageText("t"))
            val contexts = Messages.readGroupChat(chatId, userId = adminId).map { it.node.context }
            assertEquals(listOf(MessageContext(hasContext = false, id = null)), contexts)
        }

        @Test
        fun `The message should be created with a context`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            createTextMessage(adminId, chatId, MessageText("t"), contextMessageId = messageId)
            val context = Messages.readGroupChat(chatId, userId = adminId)[1].node.context
            assertEquals(MessageContext(hasContext = true, id = messageId), context)
        }

        @Test
        fun `Using a nonexistent message context should fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val response = errCreateTextMessage(adminId, chatId, MessageText("t"), contextMessageId = 1)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `A non-admin shouldn't be allowed to message in a broadcast chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id), isBroadcast = true)
            val response = executeGraphQlViaHttp(
                CREATE_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "text" to "Hi"),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class CreatePrivateChat {
        @Test
        fun `A chat should be created`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = createPrivateChat(user1Id, user2Id)
            assertEquals(listOf(chatId), PrivateChats.readIdList(user1Id))
        }

        @Test
        fun `Attempting to create a chat the user is in should return an error`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            createPrivateChat(user1Id, user2Id)
            assertEquals(ChatExistsException.message, errCreatePrivateChat(user1Id, user2Id))
        }

        @Test
        fun `Recreating a chat the user deleted should cause the existing chat's ID to be returned`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = createPrivateChat(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            assertEquals(chatId, createPrivateChat(user1Id, user2Id))
        }

        @Test
        fun `A chat shouldn't be created with a nonexistent user`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidUserIdException.message, errCreatePrivateChat(userId, otherUserId = -1))
        }

        @Test
        fun `A chat shouldn't be created with the user themselves`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidUserIdException.message, errCreatePrivateChat(userId, userId))
        }
    }

    /** A private chat between two users where [user2Id] sent the [messageId]. */
    data class UtilizedPrivateChat(val messageId: Int, val user1Id: Int, val user2Id: Int)

    @Nested
    inner class CreateStatus {
        private fun createUtilizedPrivateChat(): UtilizedPrivateChat {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId)
            return UtilizedPrivateChat(messageId, user1Id, user2Id)
        }

        @Test
        fun `A status should be created`() {
            val (messageId, user1Id) = createUtilizedPrivateChat()
            createStatus(user1Id, messageId, MessageStatus.DELIVERED)
            val statuses = MessageStatuses.read(messageId)
            assertEquals(1, statuses.size)
            assertEquals(MessageStatus.DELIVERED, statuses[0].status)
        }

        @Test
        fun `Creating a duplicate status should fail`() {
            val (messageId, user1Id) = createUtilizedPrivateChat()
            createStatus(user1Id, messageId, MessageStatus.DELIVERED)
            assertEquals(DuplicateStatusException.message, errCreateStatus(user1Id, messageId, MessageStatus.DELIVERED))
        }

        @Test
        fun `Creating a status on the user's own message should fail`() {
            val (messageId, _, user2Id) = createUtilizedPrivateChat()
            val response = errCreateStatus(user2Id, messageId, MessageStatus.DELIVERED)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `Creating a status on a message from a chat the user isn't in should fail`() {
            val (messageId) = createUtilizedPrivateChat()
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidMessageIdException.message, errCreateStatus(userId, messageId, MessageStatus.DELIVERED))
        }

        @Test
        fun `Creating a status on a nonexistent message should fail`() {
            val userId = createVerifiedUsers(1)[0].info.id
            val response = errCreateStatus(userId, messageId = 1, status = MessageStatus.DELIVERED)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `Creating a status in a private chat the user deleted should fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            val response = errCreateStatus(user1Id, messageId, MessageStatus.DELIVERED)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `Creating a status on a message which was sent before the user deleted the private chat should fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user2Id)
            val response = errCreateStatus(user2Id, messageId, MessageStatus.DELIVERED)
            assertEquals(InvalidMessageIdException.message, response)
        }
    }

    @Nested
    inner class DeleteAccount {
        @Test
        fun `An account should be deleted from the auth system`() {
            val userId = createVerifiedUsers(1)[0].info.id
            deleteAccount(userId)
            assertFalse(Users.exists(userId))
        }

        @Test
        fun `An account shouldn't be deleted if the user is the last admin of a group chat with other users`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(listOf(adminId), listOf(userId))
            assertEquals(CannotDeleteAccountException.message, errDeleteAccount(adminId))
        }
    }

    @Nested
    inner class DeleteContacts {
        @Test
        fun `Contacts should be deleted, ignoring invalid ones`() {
            val (ownerId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val userIdList = listOf(user1Id, user2Id)
            Contacts.create(ownerId, userIdList.toSet())
            deleteContacts(ownerId, userIdList + -1)
            assertTrue(Contacts.readIdList(ownerId).isEmpty())
        }
    }

    @Nested
    inner class DeleteMessage {
        @Test
        fun `The user's message should be deleted`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            deleteMessage(adminId, messageId)
            assertTrue(Messages.readGroupChat(chatId, userId = adminId).isEmpty())
        }

        @Test
        fun `Deleting a nonexistent message should return an error`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidMessageIdException.message, errDeleteMessage(userId, messageId = 0))
        }

        @Test
        fun `Deleting a message from a chat the user isn't in should throw an exception`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertEquals(InvalidMessageIdException.message, errDeleteMessage(userId, messageId))
        }

        @Test
        fun `Deleting another user's message should return an error`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId)
            assertEquals(InvalidMessageIdException.message, errDeleteMessage(user1Id, messageId))
        }

        @Test
        fun `Deleting a message sent before the private chat was deleted by the user should fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            assertEquals(InvalidMessageIdException.message, errDeleteMessage(user1Id, messageId))
        }
    }

    @Nested
    inner class DeletePrivateChat {
        @Test
        fun `A chat should be deleted`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            deletePrivateChat(user1Id, chatId)
            assertTrue(PrivateChatDeletions.isDeleted(user1Id, chatId))
        }

        @Test
        fun `Deleting an invalid chat ID should throw an exception`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidChatIdException.message, errDeletePrivateChat(userId, chatId = 1))
        }
    }

    @Nested
    inner class ResetPassword {
        @Test
        fun `A password reset request should be sent`() {
            val address = createVerifiedUsers(1)[0].info.emailAddress
            resetPassword(address)
        }

        @Test
        fun `Requesting a password reset for an unregistered address should throw an exception`() {
            assertEquals(UnregisteredEmailAddressException.message, errResetPassword("username@example.com"))
        }
    }

    @Nested
    inner class SendEmailAddressVerification {
        @Test
        fun `A verification email should be sent`() {
            val address = "username@example.com"
            val account = AccountInput(Username("username"), Password("password"), address)
            createUser(account)
            sendEmailAddressVerification(address)
        }

        @Test
        fun `Sending a verification email to an unregistered address should throw an exception`() {
            assertEquals(UnregisteredEmailAddressException.message, errSendEmailVerification("username@example.com"))
        }
    }

    @Nested
    inner class UpdateAccount {
        fun testAccount(accountBeforeUpdate: Account, accountAfterUpdate: AccountUpdate) {
            assertFalse(isUsernameTaken(accountBeforeUpdate.username))
            with(readUserByUsername(accountAfterUpdate.username!!)) {
                assertEquals(accountAfterUpdate.username, username)
                assertEquals(accountAfterUpdate.emailAddress, emailAddress)
                assertFalse(isEmailVerified(id))
                assertEquals(accountBeforeUpdate.firstName, firstName)
                assertEquals(accountAfterUpdate.lastName, lastName)
                assertEquals(accountBeforeUpdate.bio, bio)
            }
        }

        @Test
        fun `Only the specified fields should be updated`() {
            val user = createVerifiedUsers(1)[0].info
            val update =
                AccountUpdate(Username("john_roger"), emailAddress = "john.roger@example.com", lastName = "Roger")
            updateAccount(user.id, update)
            testAccount(user, update)
        }

        @Test
        fun `Updating a username to one already taken shouldn't allow the account to be updated`() {
            val (user1, user2) = createVerifiedUsers(2).map { it.info }
            val response = errUpdateAccount(user1.id, AccountUpdate(username = user2.username))
            assertEquals(UsernameTakenException.message, response)
        }

        @Test
        fun `Updating an email to one already taken shouldn't allow the account to be updated`() {
            val (user1, user2) = createVerifiedUsers(2).map { it.info }
            val response = errUpdateAccount(user1.id, AccountUpdate(emailAddress = user2.emailAddress))
            assertEquals(EmailAddressTakenException.message, response)
        }
    }
}
