package com.tangem.features.send.feeselector.component.token.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SponsoredFeeTokenItemTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN coin row WHEN asSponsoredFee THEN row is keyed apart from the coin row`(model: SponsoredFeeModel) {
        // Act
        val actual = model.coinRow.asSponsoredFee(symbol = "SOL")

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class SponsoredFeeModel(val coinRow: TokenItemState, val expected: TokenItemState)

    private fun provideTestModels() = listOf(
        SponsoredFeeModel(
            coinRow = TokenItemState.Content(
                id = COIN_ID,
                iconState = CurrencyIconState.Loading,
                titleState = COIN_TITLE,
                subtitleState = TokenItemState.SubtitleState.Loading,
                fiatAmountState = null,
                subtitle2State = null,
                onItemClick = null,
                onItemLongClick = null,
            ),
            expected = TokenItemState.Content(
                id = SPONSORED_ID,
                iconState = CurrencyIconState.Loading,
                titleState = SPONSORED_TITLE,
                subtitleState = TokenItemState.SubtitleState.Loading,
                fiatAmountState = null,
                subtitle2State = null,
                onItemClick = null,
                onItemLongClick = null,
            ),
        ),
        SponsoredFeeModel(
            coinRow = TokenItemState.Unreachable(
                id = COIN_ID,
                iconState = CurrencyIconState.Loading,
                titleState = COIN_TITLE,
                onItemClick = null,
                onItemLongClick = null,
            ),
            expected = TokenItemState.Unreachable(
                id = SPONSORED_ID,
                iconState = CurrencyIconState.Loading,
                titleState = SPONSORED_TITLE,
                onItemClick = null,
                onItemLongClick = null,
            ),
        ),
        SponsoredFeeModel(
            coinRow = TokenItemState.NoAddress(
                id = COIN_ID,
                iconState = CurrencyIconState.Loading,
                titleState = COIN_TITLE,
                onItemLongClick = null,
            ),
            expected = TokenItemState.NoAddress(
                id = SPONSORED_ID,
                iconState = CurrencyIconState.Loading,
                titleState = SPONSORED_TITLE,
                onItemLongClick = null,
            ),
        ),
        SponsoredFeeModel(
            coinRow = TokenItemState.Loading(id = COIN_ID),
            expected = TokenItemState.Loading(id = SPONSORED_ID),
        ),
    )

    private companion object {
        const val COIN_ID = "solana"
        const val SPONSORED_ID = "solana_sponsored"
        val COIN_TITLE = TokenItemState.TitleState.Content(text = stringReference("Solana"))
        val SPONSORED_TITLE = TokenItemState.TitleState.Content(text = stringReference("SmartGas SOL"))
    }
}