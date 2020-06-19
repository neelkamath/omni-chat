package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.EmailAddressTakenException
import com.neelkamath.omniChat.graphql.UsernameTakenException
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.api.queries.requestTokenSet
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe

const val UPDATE_ACCOUNT_QUERY: String = """
    mutation UpdateAccount(${"$"}update: AccountUpdate!) {
        updateAccount(update: ${"$"}update)
    }
"""

private fun operateUpdateAccount(accessToken: String, update: AccountUpdate): GraphQlResponse =
    operateQueryOrMutation(UPDATE_ACCOUNT_QUERY, variables = mapOf("update" to update), accessToken = accessToken)

fun updateAccount(accessToken: String, update: AccountUpdate): Boolean =
    operateUpdateAccount(accessToken, update).data!!["updateAccount"] as Boolean

fun errUpdateAccount(accessToken: String, update: AccountUpdate): String =
    operateUpdateAccount(accessToken, update).errors!![0].message

class UpdateAccountTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    fun testAccountInfo(accountBeforeUpdate: AccountInfo, accountAfterUpdate: AccountUpdate) {
        isUsernameTaken(accountBeforeUpdate.username).shouldBeFalse()
        with(findUserByUsername(accountAfterUpdate.username!!)) {
            username shouldBe accountAfterUpdate.username
            emailAddress shouldBe accountAfterUpdate.emailAddress
            isEmailVerified(id).shouldBeFalse()
            firstName shouldBe accountBeforeUpdate.firstName
            lastName shouldBe accountAfterUpdate.lastName
        }
    }

    test("Only the specified fields should be updated") {
        val user = createSignedInUsers(1)[0]
        val update = AccountUpdate(username = "john_roger", emailAddress = "john.roger@example.com", lastName = "Roger")
        updateAccount(user.accessToken, update)
        testAccountInfo(user.info, update)
    }

    test("The password should be updated") {
        val user = createSignedInUsers(1)[0]
        val newPassword = "new password"
        updateAccount(user.accessToken, AccountUpdate(password = newPassword))
        val login = user.login.copy(password = newPassword)
        requestTokenSet(login) // Successfully requesting a JWT tests the password update.
    }

    test("Updating a username to one already taken shouldn't allow the account to be updated") {
        val (user1, user2) = createSignedInUsers(2)
        errUpdateAccount(user1.accessToken, AccountUpdate(username = user2.info.username)) shouldBe
                UsernameTakenException.message
    }

    test("Updating an email to one already taken shouldn't allow the account to be updated") {
        val (user1, user2) = createSignedInUsers(2)
        errUpdateAccount(user1.accessToken, AccountUpdate(emailAddress = user2.info.emailAddress)) shouldBe
                EmailAddressTakenException.message
    }
}