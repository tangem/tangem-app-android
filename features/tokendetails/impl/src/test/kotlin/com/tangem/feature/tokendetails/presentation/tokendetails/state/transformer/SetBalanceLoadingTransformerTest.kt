package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.feature.tokendetails.presentation.tokendetails.state.AddFundsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceTypeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TransferUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ZeroBalanceActionsUM
import io.mockk.mockk
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
        val addFunds = button(text = "Add funds")
        val swap = button(text = "Swap")
        val transfer = button(text = "Transfer")
        val state = initialState(
            addFundsButton = addFunds,
            swapButton = swap,
            transferButton = transfer,
        )
        val transformer = SetBalanceLoadingTransformer(currencyIconState = currencyIconState)

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result.balanceBlockUM.addFundsButton).isSameInstanceAs(addFunds)
        assertThat(result.balanceBlockUM.swapButton).isSameInstanceAs(swap)
        assertThat(result.balanceBlockUM.transferButton).isSameInstanceAs(transfer)
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
                addFundsButton = button(text = "Add funds"),
                swapButton = button(text = "Swap"),
                transferButton = button(text = "Transfer"),
                tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                currencyIconState = CurrencyIconState.Loading,
                displayCryptoBalanceAll = stringReference("1.0 BTC"),
                displayFiatBalanceAll = stringReference("$50,000"),
                displayCryptoBalanceAvailable = null,
                displayFiatBalanceAvailable = null,
                isBalanceFlickering = false,
                isBalanceZero = false,
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
        assertThat(result.earnBlockState).isEqualTo(state.earnBlockState)
        assertThat(result.pullToRefreshConfig).isSameInstanceAs(state.pullToRefreshConfig)
        assertThat(result.isBalanceHidden).isEqualTo(state.isBalanceHidden)
        assertThat(result.isMarketPriceAvailable).isEqualTo(state.isMarketPriceAvailable)
    }

    private fun initialState(
        addFundsButton: TangemButtonUM = button(text = "Add funds"),
        swapButton: TangemButtonUM = button(text = "Swap"),
        transferButton: TangemButtonUM = button(text = "Transfer"),
    ): TokenDetailsUM = TokenDetailsUM(
        topAppBarUM = TokenDetailsTopAppBarUM(
            titleState = TitleState.Simple(tokenName = ""),
            subtitle = stringReference(""),
            onBackClick = {},
            menuItems = persistentListOf(),
        ),
        balanceBlockUM = TokenDetailsBalanceBlockUM.Loading(
            addFundsButton = addFundsButton,
            swapButton = swapButton,
            transferButton = transferButton,
            tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
            currencyIconState = CurrencyIconState.Loading,
        ),
        notifications = persistentListOf(),
        marketPriceBlockState = mockk<MarketPriceBlockState>(relaxed = true),
        earnBlockState = null,
        pullToRefreshConfig = mockk<PullToRefreshConfig>(relaxed = true),
        isBalanceHidden = false,
        isMarketPriceAvailable = false,
        addFundsUM = AddFundsUM.Loading,
        transferUM = TransferUM.Loading,
        zeroBalanceActionsUM = ZeroBalanceActionsUM.Loading,
    )

    private fun button(text: String): TangemButtonUM = TangemButtonUM(
        text = stringReference(text),
        type = TangemButtonType.Secondary,
        onClick = {},
    )
}