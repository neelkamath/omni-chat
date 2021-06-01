package com.neelkamath.omniChatBackend.db

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedAccount
import com.neelkamath.omniChatBackend.graphql.routing.AccountUpdate
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatPublicity
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Makes up for the message broker's latency.
 *
 * There's a delay between messages being [Notifier.publish]ed and [Notifier.notify]d. This causes messages
 * [Notifier.publish]ed before a subscription to be received, and messages [Notifier.publish]ed after a subscription to
 * be missed.
 */
suspend fun awaitBrokering(): Unit = delay(Duration.ofMillis(250))

@ExtendWith(DbExtension::class)
class BrokerTest {
    @Nested
    inner class NegotiateUserUpdate {
        @Test
        fun `Unauthenticated subscribers must receive the update`(): Unit = runBlocking {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val subscriber = chatAccountsNotifier.subscribe(ChatId(chatId)).subscribeWith(TestSubscriber())
            val emailAddress = "new@example.com"
            Users.update(adminId, AccountUpdate(emailAddress = emailAddress))
            awaitBrokering()
            val actual = subscriber.values().map { (it as UpdatedAccount).getEmailAddress() }
            assertEquals(listOf(emailAddress), actual)
        }

        @Test
        fun `Updating an account must only notify non-deleted chat sharers, contact owners, and the updater for non-public chats`(): Unit =
            runBlocking {
                val (userId, contactOwnerId, deletedPrivateChatSharer, privateChatSharer, groupChatSharer) =
                    createVerifiedUsers(5).map { it.userId }
                Contacts.create(contactOwnerId, userId)
                val chatId = PrivateChats.create(userId, deletedPrivateChatSharer)
                PrivateChatDeletions.create(chatId, userId)
                PrivateChats.create(userId, privateChatSharer)
                GroupChats.create(adminIdList = listOf(userId), userIdList = listOf(groupChatSharer))
                awaitBrokering()
                val (
                    userSubscriber,
                    contactOwnerSubscriber,
                    deletedPrivateChatSharerSubscriber,
                    privateChatSharerSubscriber,
                    groupChatSharerSubscriber,
                ) = listOf(userId, contactOwnerId, deletedPrivateChatSharer, privateChatSharer, groupChatSharer)
                    .map { accountsNotifier.subscribe(UserId(it)).subscribeWith(TestSubscriber()) }
                negotiateUserUpdate(userId, isProfilePic = false)
                awaitBrokering()
                deletedPrivateChatSharerSubscriber.assertNoValues()
                listOf(
                    userSubscriber,
                    contactOwnerSubscriber,
                    privateChatSharerSubscriber,
                    groupChatSharerSubscriber,
                ).forEach { subscriber ->
                    val actual = subscriber.values().map { (it as UpdatedAccount).id }
                    assertEquals(listOf(userId), actual)
                }
            }
    }
}

@ExtendWith(DbExtension::class)
class NotifierTest {
    @Nested
    inner class Notify {
        @Test
        fun `Clients who have subscribed with a matching asset must be notified`() {
            runBlocking {
                val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.userId }
                val notifier = Notifier<String, UserId>(Topic.MESSAGES)
                val (subscriber1, subscriber2, subscriber3, subscriber4) = listOf(user1Id, user1Id, user2Id, user3Id)
                    .map { notifier.subscribe(UserId(it)).subscribeWith(TestSubscriber()) }
                val update = "update"
                val user1Notification = Notification(update, UserId(user1Id))
                val user2Notification = Notification(update, UserId(user2Id))
                notifier.notify(listOf(user1Notification, user2Notification))
                awaitBrokering()
                listOf(subscriber1, subscriber2, subscriber3).forEach { it.assertValue(update) }
                subscriber4.assertNoValues()
            }
        }
    }
}
