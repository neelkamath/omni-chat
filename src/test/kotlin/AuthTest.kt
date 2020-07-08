package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.ContactsAsset
import com.neelkamath.omniChat.db.UpdatedChatsAsset
import com.neelkamath.omniChat.db.contactsBroker
import com.neelkamath.omniChat.db.tables.Contacts
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.PrivateChats
import com.neelkamath.omniChat.db.updatedChatsBroker
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.reactivex.rxjava3.subscribers.TestSubscriber

class AuthTest : FunSpec({
    context("isValidLogin(Login)") {
        test("An incorrect login should be invalid") {
            val login = Login(Username("username"), Password("password"))
            isValidLogin(login).shouldBeFalse()
        }

        test("A correct login should be valid") {
            val login = createVerifiedUsers(1)[0].login
            isValidLogin(login).shouldBeTrue()
        }
    }

    context("isUsernameTaken(String)") {
        test(
            """
            Given an existing username, and a nonexistent username similar to the one which exists,
            when checking if the nonexistent username exists,
            then it should be said to not exist
            """
        ) {
            val username = createVerifiedUsers(1)[0].info.username
            val similarUsername = Username(username.value.dropLast(1))
            isUsernameTaken(similarUsername).shouldBeFalse()
        }

        test("An existing username should be said to exist") {
            val username = createVerifiedUsers(1)[0].info.username
            isUsernameTaken(username).shouldBeTrue()
        }
    }

    context("userIdExists(String)") {
        test("A nonexistent user ID should not be said to exist") { userIdExists("user ID").shouldBeFalse() }

        test("An existing user ID should be said to exist") {
            val id = createVerifiedUsers(1)[0].info.id
            userIdExists(id).shouldBeTrue()
        }
    }

    context("emailAddressExists(String)") {
        test("A nonexistent email address should not be said to exist") {
            emailAddressExists("address").shouldBeFalse()
        }

        test("An existing email address should be said to exist") {
            val address = createVerifiedUsers(1)[0].info.emailAddress
            emailAddressExists(address).shouldBeTrue()
        }
    }

    context("readUserByUsername(Username)") {
        test("Finding a user by their username should yield that user") {
            val username = createVerifiedUsers(1)[0].info.username
            readUserByUsername(username).username shouldBe username
        }
    }

    context("searchUsers(String)") {
        /** Creates users, and returns their IDs. */
        fun createUsers(): List<String> = listOf(
            NewAccount(Username("tony"), Password("p"), emailAddress = "tony@example.com", firstName = "Tony"),
            NewAccount(Username("johndoe"), Password("p"), emailAddress = "john@example.com", firstName = "John"),
            NewAccount(Username("john.rogers"), Password("p"), emailAddress = "rogers@example.com"),
            NewAccount(Username("anonymous"), Password("p"), emailAddress = "anon@example.com", firstName = "John")
        ).map {
            createUser(it)
            readUserByUsername(it.username).id
        }

        test("Users should be searched case-insensitively") {
            val infoList = createUsers()
            val search = { query: String, userIdList: List<String> ->
                searchUsers(query).map { it.id } shouldBe userIdList
            }
            search("tOnY", listOf(infoList[0]))
            search("doe", listOf(infoList[1]))
            search("john", listOf(infoList[1], infoList[2], infoList[3]))
        }

        test("Searching users shouldn't include duplicate results") {
            val userIdList = listOf(
                NewAccount(Username("tony_stark"), Password("p"), emailAddress = "e"),
                NewAccount(Username("username"), Password("p"), "tony@example.com", firstName = "Tony")
            ).map {
                createUser(it)
                readUserByUsername(it.username).id
            }
            searchUsers("tony").map { it.id } shouldBe userIdList
        }
    }

    context("updateUser(String, UpdatedAccount)") {
        test("Updating an account should trigger a notification for the contact owner, but not the contact") {
            val (ownerId, contactId) = createVerifiedUsers(2).map { it.info.id }
            Contacts.create(ownerId, setOf(contactId))
            val (ownerSubscriber, contactSubscriber) = listOf(ownerId, contactId)
                .map { contactsBroker.subscribe(ContactsAsset(it)).subscribeWith(TestSubscriber()) }
            updateUser(contactId, AccountUpdate())
            ownerSubscriber.assertValue(UpdatedContact.fromUserId(contactId))
            contactSubscriber.assertNoValues()
        }

        test(
            """
            Given subscribers to updated chats,
            when a user updates their account,
            then only the users who share their group chat should be notified of the updated account, except the updater
            """
        ) {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            listOf(user1Id, user2Id).forEach { GroupChats.create(adminId, buildNewGroupChat(it)) }
            val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id)
                .map { updatedChatsBroker.subscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
            updateUser(user1Id, AccountUpdate(firstName = "new name"))
            adminSubscriber.assertValue(UpdatedAccount.fromUserId(user1Id))
            listOf(user1Subscriber, user2Subscriber).forEach { it.assertNoValues() }
        }

        test(
            """
            Given two users subscribed to the private chat,
            when one user updates their account,
            then only the other user should be notified
            """
        ) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            PrivateChats.create(user1Id, user2Id)
            val (user1Subscriber, user2Subscriber) = listOf(user1Id, user2Id)
                .map { updatedChatsBroker.subscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
            updateUser(user2Id, AccountUpdate())
            user1Subscriber.assertValue(UpdatedAccount.fromUserId(user2Id))
            user2Subscriber.assertNoValues()
        }

        test("Updating an account should update only the specified fields") {
            val user = createVerifiedUsers(1)[0]
            val update = AccountUpdate(Username("updated username"), firstName = "updated first name")
            updateUser(user.info.id, update)
            with(readUserById(user.info.id)) {
                username shouldBe update.username
                emailAddress shouldBe user.info.emailAddress
                firstName shouldBe update.firstName
                lastName shouldBe user.info.lastName
            }
        }

        fun assertEmailAddressUpdate(changeAddress: Boolean) {
            val (userId, _, emailAddress) = createVerifiedUsers(1)[0].info
            val address = if (changeAddress) "updated address" else emailAddress
            updateUser(userId, AccountUpdate(emailAddress = address))
            isEmailVerified(userId) shouldNotBe changeAddress
        }

        test(
            """
            Given an account with a verified email address,
            when its email address is changed,
            then its email address should become unverified
            """
        ) { assertEmailAddressUpdate(changeAddress = true) }

        test(
            """
            Given an account with a verified email address,
            when its email address is updated to the same address,
            then its email address shouldn't become unverified
            """
        ) { assertEmailAddressUpdate(changeAddress = false) }
    }
})
