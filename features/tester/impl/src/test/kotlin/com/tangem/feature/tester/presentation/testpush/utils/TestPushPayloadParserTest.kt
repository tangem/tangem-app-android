package com.tangem.feature.tester.presentation.testpush.utils

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class TestPushPayloadParserTest {

    @Test
    fun `GIVEN flat payload WHEN parse THEN every entry becomes a data key`() {
        // Arrange
        val rawPayload = """
            {
              "type": "transaction_spend",
              "customer_wallet_id": "wallet-1",
              "amount": "-12.5"
            }
        """.trimIndent()

        // Act
        val actual = TestPushPayloadParser.parse(rawPayload)

        // Assert
        val expected = mapOf(
            "type" to "transaction_spend",
            "customer_wallet_id" to "wallet-1",
            "amount" to "-12.5",
        )
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN non-string values WHEN parse THEN they are stringified as FCM does`() {
        // Arrange
        val rawPayload = """
            {
              "amount": -12.5,
              "is_refund": true,
              "meta": { "mcc": "5812" },
              "tags": ["a", "b"]
            }
        """.trimIndent()

        // Act
        val actual = TestPushPayloadParser.parse(rawPayload)

        // Assert
        val expected = mapOf(
            "amount" to "-12.5",
            "is_refund" to "true",
            "meta" to """{"mcc":"5812"}""",
            "tags" to """["a","b"]""",
        )
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN data and notification blocks WHEN parse THEN notification keys get the gcm prefix`() {
        // Arrange
        val rawPayload = """
            {
              "data": { "type": "card_ready" },
              "notification": { "title": "Card is ready", "body": "Tap to open", "channel_id": "pay" }
            }
        """.trimIndent()

        // Act
        val actual = TestPushPayloadParser.parse(rawPayload)

        // Assert
        val expected = mapOf(
            "type" to "card_ready",
            "gcm.n.e" to "1",
            "gcm.n.title" to "Card is ready",
            "gcm.n.body" to "Tap to open",
            "gcm.n.channel_id" to "pay",
        )
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN FCM v1 envelope WHEN parse THEN message block is unwrapped`() {
        // Arrange
        val rawPayload = """
            {
              "message": {
                "token": "fcm-token",
                "data": { "type": "threshold1_top_up" }
              }
            }
        """.trimIndent()

        // Act
        val actual = TestPushPayloadParser.parse(rawPayload)

        // Assert
        assertThat(actual).isEqualTo(mapOf("type" to "threshold1_top_up").right())
    }

    @Test
    fun `GIVEN envelope without data and notification WHEN parse THEN its own entries are used`() {
        // Arrange
        val rawPayload = """{ "message": { "type": "collateral_deposit" } }"""

        // Act
        val actual = TestPushPayloadParser.parse(rawPayload)

        // Assert
        assertThat(actual).isEqualTo(mapOf("type" to "collateral_deposit").right())
    }

    @Test
    fun `GIVEN malformed json WHEN parse THEN returns error`() {
        // Act
        val actual = TestPushPayloadParser.parse("""{ "type": """)

        // Assert
        assertThat(actual.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN json array WHEN parse THEN returns error`() {
        // Act
        val actual = TestPushPayloadParser.parse("""["type"]""")

        // Assert
        assertThat(actual).isEqualTo("Payload must be a JSON object".left())
    }
}