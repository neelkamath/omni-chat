package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.graphql.UnregisteredEmailAddressException
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

fun buildResetPasswordQuery(): String = """
    mutation ResetPassword(${"$"}emailAddress: String!) {
        resetPassword(emailAddress: ${"$"}emailAddress)
    }
"""

private fun operateResetPassword(emailAddress: String): GraphQlResponse =
    operateQueryOrMutation(buildResetPasswordQuery(), variables = mapOf("emailAddress" to emailAddress))

fun resetPassword(emailAddress: String): Boolean = operateResetPassword(emailAddress).data!!["resetPassword"] as Boolean

fun errResetPassword(emailAddress: String): String = operateResetPassword(emailAddress).errors!![0].message

class ResetPasswordTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("A password reset request should be sent") {
        val address = createSignedInUsers(1)[0].info.emailAddress
        resetPassword(address).shouldBeTrue()
    }

    test("Requesting a password reset for an unregistered address should throw an exception") {
        errResetPassword("username@example.com") shouldBe UnregisteredEmailAddressException.message
    }
}