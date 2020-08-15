package com.neelkamath.omniChat.graphql.routing

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.tables.Contacts
import com.neelkamath.omniChat.graphql.operations.ACCOUNTS_SUBSCRIPTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.CREATED_SUBSCRIPTION_FRAGMENT
import io.ktor.http.cio.websocket.FrameType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExtendWith(DbExtension::class)
class SubscriptionsTest {
    @Nested
    inner class RouteSubscription {
        private fun testOperationName(shouldSupplyOperationName: Boolean) {
            val query = """
                subscription SubscribeToMessages {
                    subscribeToMessages {
                        $CREATED_SUBSCRIPTION_FRAGMENT
                    }
                }
                
                subscription SubscribeToAccounts {
                    subscribeToAccounts {
                        $CREATED_SUBSCRIPTION_FRAGMENT
                    }
                }
            """
            val operationName = "SubscribeToAccounts".takeIf { shouldSupplyOperationName }
            val token = createVerifiedUsers(1)[0].accessToken
            executeGraphQlSubscriptionViaWebSocket(
                uri = "accounts-subscription",
                request = GraphQlRequest(query, operationName = operationName),
                accessToken = token
            ) { incoming ->
                if (shouldSupplyOperationName) parseFrameData<CreatedSubscription>(incoming)
                else assertEquals(FrameType.CLOSE, incoming.receive().frameType)
            }
        }

        @Test
        fun `The specified operation should be executed when there are multiple`() {
            testOperationName(shouldSupplyOperationName = true)
        }

        @Test
        fun `An error should be returned when supplying multiple operations but not which to execute`() {
            testOperationName(shouldSupplyOperationName = false)
        }
    }

    private fun subscribeToAccounts(accessToken: String? = null, callback: SubscriptionCallback) {
        val subscribeToAccountsQuery = """
            subscription SubscribeToAccounts {
                subscribeToAccounts {
                    $ACCOUNTS_SUBSCRIPTION_FRAGMENT
                }
            }
        """
        executeGraphQlSubscriptionViaWebSocket(
            uri = "accounts-subscription",
            request = GraphQlRequest(subscribeToAccountsQuery),
            accessToken = accessToken,
            callback = callback
        )
    }

    @Nested
    inner class Subscribe {
        @Test
        fun `Recreating a subscription shouldn't cause duplicate notifications from the previous connection`() {
            val (owner, user) = createVerifiedUsers(2)
            subscribeToAccounts(owner.accessToken) {}
            subscribeToAccounts(owner.accessToken) { incoming ->
                parseFrameData<CreatedSubscription>(incoming)
                Contacts.create(owner.info.id, setOf(user.info.id))
                awaitBrokering()
                assertNotNull(incoming.poll())
                assertNull(incoming.poll())
            }
        }
    }

    @Nested
    inner class CloseWithError {
        @Test
        fun `The connection should be closed when calling an operation with an invalid token`() {
            subscribeToAccounts { incoming -> assertEquals(FrameType.CLOSE, incoming.receive().frameType) }
        }
    }
}