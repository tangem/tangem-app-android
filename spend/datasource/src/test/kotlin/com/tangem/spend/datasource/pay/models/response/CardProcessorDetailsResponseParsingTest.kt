package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import org.junit.jupiter.api.Test

internal class CardProcessorDetailsResponseParsingTest {

    private val json = NetworkJsonFactory.create()

    @Test
    fun `GIVEN processor details WHEN parsed THEN card id and secret are read`() {
        // Arrange
        val raw = """{"result":{"card_id":"$CARD_ID","card_secret":"$CARD_SECRET"},"error":null}"""

        // Act
        val actual = json.decodeFromString(CardProcessorDetailsResponse.serializer(), raw)

        // Assert
        assertThat(actual).isEqualTo(
            CardProcessorDetailsResponse(
                result = CardProcessorDetailsResponse.Result(cardId = CARD_ID, cardSecret = CARD_SECRET),
            ),
        )
    }

    @Test
    fun `GIVEN empty result WHEN parsed THEN result is null`() {
        // Arrange
        val raw = """{"result":null}"""

        // Act
        val actual = json.decodeFromString(CardProcessorDetailsResponse.serializer(), raw)

        // Assert
        assertThat(actual).isEqualTo(CardProcessorDetailsResponse(result = null))
    }

    @Test
    fun `GIVEN processor details WHEN printed THEN the secret is masked`() {
        // Arrange
        val result = CardProcessorDetailsResponse.Result(cardId = CARD_ID, cardSecret = CARD_SECRET)

        // Act
        val printed = result.toString()

        // Assert
        assertThat(printed).doesNotContain(CARD_SECRET)
        assertThat(printed).contains(CARD_ID)
    }

    private companion object {
        const val CARD_ID = "processor-card-1"
        const val CARD_SECRET = "top-secret"
    }
}