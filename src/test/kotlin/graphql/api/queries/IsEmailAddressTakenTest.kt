package com.neelkamath.omniChat.graphql.api.queries

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

const val IS_EMAIL_ADDRESS_TAKEN_QUERY: String = """
    query IsEmailAddressTaken(${"$"}emailAddress: String!) {
        isEmailAddressTaken(emailAddress: ${"$"}emailAddress)
    }
"""

private fun operateIsEmailAddressTaken(emailAddress: String): GraphQlResponse =
    operateQueryOrMutation(IS_EMAIL_ADDRESS_TAKEN_QUERY, variables = mapOf("emailAddress" to emailAddress))

fun isEmailTaken(emailAddress: String): Boolean =
    operateIsEmailAddressTaken(emailAddress).data!!["isEmailAddressTaken"] as Boolean

class IsEmailTakenTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("The email shouldn't be taken") { isEmailTaken("username@example.com").shouldBeFalse() }

    test("The email should be taken") {
        val address = createSignedInUsers(1)[0].info.emailAddress
        isEmailTaken(address).shouldBeTrue()
    }
}