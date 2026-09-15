package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.pay.models.response.CustomerMeResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CustomerInfoConverterEmbossNameTest {

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: EmbossNameModel) {
        // Act
        val info = CustomerInfoConverter.convert(result(cards = model.cards))

        // Assert
        assertThat(info.embossName).isEqualTo(model.expected)
    }

    internal data class EmbossNameModel(
        val name: String,
        val cards: List<CustomerMeResponse.Card>,
        val expected: String?,
    ) {
        override fun toString(): String = name
    }

    private fun provideTestModels() = listOf(
        EmbossNameModel(
            name = "first card name is taken",
            cards = listOf(card(embossName = "JOHNNY SILVERHAND"), card(embossName = "V ARASAKA")),
            expected = "JOHNNY SILVERHAND",
        ),
        EmbossNameModel(
            name = "name is trimmed",
            cards = listOf(card(embossName = "  V ARASAKA  ")),
            expected = "V ARASAKA",
        ),
        EmbossNameModel(
            name = "card without a name falls through to the next one",
            cards = listOf(card(embossName = null), card(embossName = "   "), card(embossName = "V ARASAKA")),
            expected = "V ARASAKA",
        ),
        EmbossNameModel(
            name = "no cards -> no name",
            cards = emptyList(),
            expected = null,
        ),
        EmbossNameModel(
            name = "no card carries a name -> no name",
            cards = listOf(card(embossName = null), card(embossName = "  ")),
            expected = null,
        ),
        EmbossNameModel(
            name = "card without an id is skipped",
            cards = listOf(card(id = null, embossName = "JOHNNY SILVERHAND"), card(embossName = "V ARASAKA")),
            expected = "V ARASAKA",
        ),
    )

    private fun result(cards: List<CustomerMeResponse.Card>) = CustomerMeResponse.Result(
        id = "c1",
        state = "ACTIVE",
        createdAt = "2026-01-01T00:00:00Z",
        paymentAccount = null,
        kyc = null,
        depositAddress = null,
        balance = null,
        productInstances = emptyList(),
        cards = cards,
        customerTariffPlan = null,
        profile = CustomerMeResponse.Profile(embossName = "PROFILE NAME"),
    )

    private fun card(embossName: String?, id: String? = "card_1") = CustomerMeResponse.Card(
        id = id,
        token = "token",
        expirationMonth = "01",
        expirationYear = "30",
        embossName = embossName,
        cardType = "VIRTUAL",
        cardStatus = "ACTIVE",
        cardNumberEnd = "1234",
        isPinSet = true,
        images = null,
    )
}