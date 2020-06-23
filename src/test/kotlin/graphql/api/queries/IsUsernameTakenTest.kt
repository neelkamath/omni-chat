package com.neelkamath.omniChat.graphql.api.queries

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.graphql.UsernameNotLowercaseException
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

const val IS_USERNAME_TAKEN_QUERY: String = """
    query IsUsernameTaken(${"$"}username: String!) {
        isUsernameTaken(username: ${"$"}username)
    }
"""

private fun operateIsUsernameTaken(username: String): GraphQlResponse =
    operateQueryOrMutation(IS_USERNAME_TAKEN_QUERY, variables = mapOf("username" to username))

fun isUsernameTaken(username: String): Boolean = operateIsUsernameTaken(username).data!!["isUsernameTaken"] as Boolean

fun errIsUsernameTake(username: String): String = operateIsUsernameTaken(username).errors!![0].message

class IsUsernameTakenTest : FunSpec({
    test("The username shouldn't be taken") { isUsernameTaken("username").shouldBeFalse() }

    test("The username should be taken") {
        val username = createSignedInUsers(1)[0].info.username
        isUsernameTaken(username).shouldBeTrue()
    }

    test("Checking if a non-lowercase username has been taken should return an error") {
        errIsUsernameTake("Username") shouldBe UsernameNotLowercaseException.message
    }
})