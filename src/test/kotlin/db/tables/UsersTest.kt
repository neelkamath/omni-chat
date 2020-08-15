package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.OnlineStatusesAsset
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.onlineStatusesNotifier
import com.neelkamath.omniChat.db.safelySubscribe
import com.neelkamath.omniChat.graphql.routing.UpdatedOnlineStatus
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test

@ExtendWith(DbExtension::class)
class UsersTest {
    @Nested
    inner class SetOnlineStatus {
        @Test
        fun `Updating the user's online status to the current value shouldn't cause notifications to be sent`() {
            runBlocking {
                val (contactOwnerId, contactId) = createVerifiedUsers(2).map { it.info.id }
                val subscriber = onlineStatusesNotifier
                    .safelySubscribe(OnlineStatusesAsset(contactOwnerId)).subscribeWith(TestSubscriber())
                Users.setOnlineStatus(contactId, Users.read(contactId).isOnline)
                awaitBrokering()
                subscriber.assertNoValues()
            }
        }

        @Test
        fun `Updating the user's status should only notify users who have them in their contacts or chats`(): Unit =
            runBlocking {
                val (updaterId, contactOwnerId, chatSharerId, userId) = createVerifiedUsers(4).map { it.info.id }
                Contacts.create(contactOwnerId, setOf(updaterId))
                PrivateChats.create(chatSharerId, updaterId)
                val (updaterSubscriber, contactOwnerSubscriber, chatSharerSubscriber, userSubscriber) =
                    listOf(updaterId, contactOwnerId, chatSharerId, userId).map {
                        onlineStatusesNotifier.safelySubscribe(OnlineStatusesAsset(it)).subscribeWith(TestSubscriber())
                    }
                val status = Users.read(updaterId).isOnline.not()
                Users.setOnlineStatus(updaterId, status)
                awaitBrokering()
                listOf(updaterSubscriber, userSubscriber).forEach { it.assertNoValues() }
                listOf(contactOwnerSubscriber, chatSharerSubscriber)
                    .forEach { it.assertValue(UpdatedOnlineStatus(updaterId, status)) }
            }
    }
}