package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceTypeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

class SetBalanceLoadingTransformerTest {

    private val currencyIconState: CurrencyIconState = mockk(relaxed = true)

    @Test
    fun `GIVEN any state WHEN transform THEN balance block is Loading`() {
        // GIVEN
        val transformer = SetBalanceLoadingTransformer(currencyIconState = currencyIconState)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.balanceBlockUM).isInstanceOf(TokenDetailsBalanceBlockUM.Loading::class.java)
    }

    @Test
    fun `GIVEN currency icon state WHEN transform THEN Loading block carries that icon state`() {
        // GIVEN
        val transformer = SetBalanceLoadingTransformer(currencyIconState = currencyIconState)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.balanceBlockUM.currencyIconState).isSameInstanceAs(currencyIconState)
    }

    @Test
    fun `GIVEN state with action buttons WHEN transform THEN action buttons are preserved`() {
        // GIVEN
        val buttons = persistentListOf(
            TangemButtonUM(
                text = stringReference("Test"),
                onClick = {},
                isEnabled = true,
                type = TangemButtonType.Secondary,
            ),
        )
        val state = initialState(actionButtons = buttons)
        val transformer = SetBalanceLoadingTransformer(currencyIconState = currencyIconState)

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result.balanceBlockUM.actionButtons).isEqualTo(buttons)
    }

    @Test
    fun `GIVEN any state WHEN transform THEN balance type is Single`() {
        // GIVEN
        val transformer = SetBalanceLoadingTransformer(currencyIconState = currencyIconState)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.balanceBlockUM.tokenBalanceTypeUM).isEqualTo(TokenBalanceTypeUM.Single)
    }

    @Test
    fun `GIVEN Content balance block WHEN transform THEN switches to Loading`() {
        // GIVEN
        val contentState = initialState().copy(
            balanceBlockUM = TokenDetailsBalanceBlockUM.Content(
                actionButtons = persistentListOf(),
                tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                currencyIconState = CurrencyIconState.Loading,
                displayCryptoBalanceAll = stringReference("1.0 BTC"),
                displayFiatBalanceAll = stringReference("$50,000"),
                displayCryptoBalanceAvailable = null,
                displayFiatBalanceAvailable = null,
                isBalanceFlickering = false,
            ),
        )
        val transformer = SetBalanceLoadingTransformer(currencyIconState = currencyIconState)

        // WHEN
        val result = transformer.transform(contentState)

        // THEN
        assertThat(result.balanceBlockUM).isInstanceOf(TokenDetailsBalanceBlockUM.Loading::class.java)
    }

    @Test
    fun `GIVEN any state WHEN transform THEN unrelated fields are preserved`() {
        // GIVEN
        val state = initialState()
        val transformer = SetBalanceLoadingTransformer(currencyIconState = currencyIconState)

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result.topAppBarUM).isSameInstanceAs(state.topAppBarUM)
        assertThat(result.marketPriceBlockState).isSameInstanceAs(state.marketPriceBlockState)
        assertThat(result.stakingBlocksState).isEqualTo(state.stakingBlocksState)
        assertThat(result.pullToRefreshConfig).isSameInstanceAs(state.pullToRefreshConfig)
        assertThat(result.isBalanceHidden).isEqualTo(state.isBalanceHidden)
        assertThat(result.isMarketPriceAvailable).isEqualTo(state.isMarketPriceAvailable)
    }

    private fun initialState(
        actionButtons: ImmutableList<TangemButtonUM> = persistentListOf(),
    ): TokenDetailsUM = TokenDetailsUM(
        topAppBarUM = TokenDetailsTopAppBarUM(
            titleState = TitleState.Simple(tokenName = ""),
            subtitle = stringReference(""),
            onBackClick = {},
            menuItems = persistentListOf(),
        ),
        balanceBlockUM = TokenDetailsBalanceBlockUM.Loading(
            actionButtons = actionButtons,
            tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
            currencyIconState = CurrencyIconState.Loading,
        ),
        marketPriceBlockState = mockk<MarketPriceBlockState>(relaxed = true),
        stakingBlocksState = null,
        pullToRefreshConfig = mockk<PullToRefreshConfig>(relaxed = true),
        isBalanceHidden = false,
        isMarketPriceAvailable = false,
    )
}