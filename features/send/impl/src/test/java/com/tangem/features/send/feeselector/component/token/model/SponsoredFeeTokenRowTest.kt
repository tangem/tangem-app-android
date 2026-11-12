package com.tangem.features.send.feeselector.component.token.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.stringReference
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SponsoredFeeTokenRowTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN coin row WHEN asSponsoredFee THEN row is keyed and titled apart from the coin row`(
        model: SponsoredFeeModel,
    ) {
        // Act
        val actual = model.coinRow.asSponsoredFee(symbol = "SOL")

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class SponsoredFeeModel(val coinRow: TangemTokenRow.State, val expected: TangemTokenRow.State)

    private fun provideTestModels() = listOf(
        SponsoredFeeModel(
            coinRow = TangemTokenRow.State.Content(id = COIN_ID, icon = ICON, title = COIN_TITLE),
            expected = TangemTokenRow.State.Content(id = SPONSORED_ID, icon = ICON, title = SPONSORED_TITLE),
        ),
        SponsoredFeeModel(
            coinRow = TangemTokenRow.State.Unreachable(id = COIN_ID, icon = ICON, title = COIN_TITLE, badge = BADGE),
            expected = TangemTokenRow.State.Unreachable(
                id = SPONSORED_ID,
                icon = ICON,
                title = SPONSORED_TITLE,
                badge = BADGE,
            ),
        ),
        SponsoredFeeModel(
            coinRow = TangemTokenRow.State.NoAddress(id = COIN_ID, icon = ICON, title = COIN_TITLE, message = BADGE),
            expected = TangemTokenRow.State.NoAddress(
                id = SPONSORED_ID,
                icon = ICON,
                title = SPONSORED_TITLE,
                message = BADGE,
            ),
        ),
        SponsoredFeeModel(
            coinRow = TangemTokenRow.State.Shimmer(id = COIN_ID),
            expected = TangemTokenRow.State.Shimmer(id = SPONSORED_ID),
        ),
    )

    private companion object {
        const val COIN_ID = "solana"
        const val SPONSORED_ID = "solana_sponsored"
        val ICON = TangemTokenIcon.UiState.Shimmer
        val COIN_TITLE = stringReference("Solana")
        val SPONSORED_TITLE = stringReference("SmartGas SOL")
        val BADGE = stringReference("Unreachable")
    }
}