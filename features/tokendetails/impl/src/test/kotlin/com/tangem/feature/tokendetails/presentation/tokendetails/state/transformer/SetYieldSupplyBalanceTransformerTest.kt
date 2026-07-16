package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.yield.supply.models.YieldSupplyRewardBalance
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

class SetYieldSupplyBalanceTransformerTest {

    @Test
    fun `GIVEN Content state WHEN transform THEN yield balances are set from reward balance`() {
        // GIVEN
        val state = stateWith(contentBlock())
        val transformer = SetYieldSupplyBalanceTransformer(
            YieldSupplyRewardBalance(fiatBalance = "$21,000.12", cryptoBalance = "10.500001 ETH"),
        )

        // WHEN
        val result = transformer.transform(state)

        // THEN
        val content = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(content.displayYieldSupplyFiatBalance).isEqualTo("$21,000.12")
        assertThat(content.displayYieldSupplyCryptoBalance).isEqualTo("10.500001 ETH")
    }

    @Test
    fun `GIVEN empty reward balance WHEN transform on Content THEN yield balances are nulled`() {
        // GIVEN
        val state = stateWith(
            contentBlock().copy(
                displayYieldSupplyFiatBalance = "$21,000.12",
                displayYieldSupplyCryptoBalance = "10.500001 ETH",
            ),
        )
        val transformer = SetYieldSupplyBalanceTransformer(YieldSupplyRewardBalance.empty())

        // WHEN
        val result = transformer.transform(state)

        // THEN
        val content = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(content.displayYieldSupplyFiatBalance).isNull()
        assertThat(content.displayYieldSupplyCryptoBalance).isNull()
    }

    @Test
    fun `GIVEN Loading block WHEN transform THEN state is unchanged`() {
        // GIVEN
        val state = stateWith(loadingBlock())
        val transformer = SetYieldSupplyBalanceTransformer(
            YieldSupplyRewardBalance(fiatBalance = "$1", cryptoBalance = "1 ETH"),
        )

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result).isSameInstanceAs(state)
    }

    @Test
    fun `GIVEN Error block WHEN transform THEN state is unchanged`() {
        // GIVEN
        val state = stateWith(errorBlock())
        val transformer = SetYieldSupplyBalanceTransformer(
            YieldSupplyRewardBalance(fiatBalance = "$1", cryptoBalance = "1 ETH"),
        )

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result).isSameInstanceAs(state)
    }

    private fun contentBlock(): TokenDetailsBalanceBlockUM.Content = TokenDetailsBalanceBlockUM.Content(
        addFundsButton = placeholderButton(),
        swapButton = placeholderButton(),
        transferButton = placeholderButton(),
        tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
        currencyIconState = CurrencyIconState.Loading,
        displayCryptoBalanceAll = stringReference("10.5 ETH"),
        displayFiatBalanceAll = stringReference("$21,000.00"),
        displayCryptoBalanceAvailable = null,
        displayFiatBalanceAvailable = null,
        isBalanceFlickering = false,
        isBalanceZero = false,
    )

    private fun loadingBlock(): TokenDetailsBalanceBlockUM.Loading = TokenDetailsBalanceBlockUM.Loading(
        addFundsButton = placeholderButton(),
        swapButton = placeholderButton(),
        transferButton = placeholderButton(),
        tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
        currencyIconState = CurrencyIconState.Loading,
    )

    private fun errorBlock(): TokenDetailsBalanceBlockUM.Error = TokenDetailsBalanceBlockUM.Error(
        addFundsButton = placeholderButton(),
        swapButton = placeholderButton(),
        transferButton = placeholderButton(),
        tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
        currencyIconState = CurrencyIconState.Loading,
    )

    private fun stateWith(balanceBlockUM: TokenDetailsBalanceBlockUM): TokenDetailsUM = TokenDetailsUM(
        topAppBarUM = TokenDetailsTopAppBarUM(
            titleState = TitleState.Simple(tokenName = ""),
            subtitle = stringReference(""),
            onBackClick = {},
            menuItems = persistentListOf(),
        ),
        balanceBlockUM = balanceBlockUM,
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

    private fun placeholderButton(): TangemButtonUM = TangemButtonUM(
        text = stringReference(""),
        type = TangemButtonType.Secondary,
        onClick = {},
    )
}