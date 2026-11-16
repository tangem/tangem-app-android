package com.tangem.tap.domain.walletregistration

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ColdWalletRegistrationSupportTest {

    private val walletCard = MockScanResponseFactory.create(
        cardConfig = GenericCardConfig(maxWalletCount = 2),
        derivedKeys = emptyMap(),
    ).card

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN card type WHEN isColdRegistrationSupported THEN matches the registration rule`(model: SupportModel) {
        // Act
        val actual = model.card.isColdRegistrationSupported

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        SupportModel(card = walletCard, expected = true),
        SupportModel(card = walletCard.copy(cardId = TWIN_CARD_ID), expected = false),
    )

    data class SupportModel(val card: CardDTO, val expected: Boolean)

    private companion object {
        const val TWIN_CARD_ID = "CB61000000000001"
    }
}