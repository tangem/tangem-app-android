package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.tokendetails.state.AddFundsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceTypeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TransferUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ZeroBalanceActionsUM
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

class InitializeWithCryptoCurrencyTransformerTest {

    private val cryptoCurrency: CryptoCurrency = mockk(relaxed = true) {
        every { name } returns TOKEN_NAME
        every { symbol } returns TOKEN_SYMBOL
    }
    private val onBackClick: () -> Unit = mockk(relaxed = true)
    private val onRefreshSwipe: (Boolean) -> Unit = mockk(relaxed = true)

    @Test
    fun `GIVEN crypto currency WHEN transform THEN top bar title is Simple with token name`() {
        // GIVEN
        val transformer = InitializeWithCryptoCurrencyTransformer(
            cryptoCurrency = cryptoCurrency,
            onBackClick = onBackClick,
            onRefreshSwipe = onRefreshSwipe,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.topAppBarUM.titleState).isEqualTo(TitleState.Simple(tokenName = TOKEN_NAME))
    }

    @Test
    fun `GIVEN crypto currency WHEN transform THEN subtitle is token symbol`() {
        // GIVEN
        val transformer = InitializeWithCryptoCurrencyTransformer(
            cryptoCurrency = cryptoCurrency,
            onBackClick = onBackClick,
            onRefreshSwipe = onRefreshSwipe,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.topAppBarUM.subtitle).isEqualTo(stringReference(TOKEN_SYMBOL))
    }

    @Test
    fun `GIVEN onBackClick callback WHEN top bar onBackClick invoked THEN callback is dispatched`() {
        // GIVEN
        val transformer = InitializeWithCryptoCurrencyTransformer(
            cryptoCurrency = cryptoCurrency,
            onBackClick = onBackClick,
            onRefreshSwipe = onRefreshSwipe,
        )

        // WHEN
        val result = transformer.transform(initialState())
        result.topAppBarUM.onBackClick()

        // THEN
        verify(exactly = 1) { onBackClick.invoke() }
    }

    @Test
    fun `GIVEN crypto currency WHEN transform THEN market price loading carries currency symbol`() {
        // GIVEN
        val transformer = InitializeWithCryptoCurrencyTransformer(
            cryptoCurrency = cryptoCurrency,
            onBackClick = onBackClick,
            onRefreshSwipe = onRefreshSwipe,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.marketPriceBlockState)
            .isEqualTo(MarketPriceBlockState.Loading(currencySymbol = TOKEN_SYMBOL))
    }

    @Test
    fun `GIVEN any state WHEN transform THEN unrelated fields are preserved`() {
        // GIVEN
        val state = initialState()
        val transformer = InitializeWithCryptoCurrencyTransformer(
            cryptoCurrency = cryptoCurrency,
            onBackClick = onBackClick,
            onRefreshSwipe = onRefreshSwipe,
        )

        // WHEN
        val result = transformer.transform(state)

        // THEN — only top bar title/subtitle/onBackClick, marketPriceBlockState and pullToRefresh.onRefresh are touched
        assertThat(result.topAppBarUM.menuItems).isEqualTo(state.topAppBarUM.menuItems)
        assertThat(result.balanceBlockUM.addFundsButton).isEqualTo(state.balanceBlockUM.addFundsButton)
        assertThat(result.balanceBlockUM.swapButton).isEqualTo(state.balanceBlockUM.swapButton)
        assertThat(result.balanceBlockUM.transferButton).isEqualTo(state.balanceBlockUM.transferButton)
        assertThat(result.balanceBlockUM.tokenBalanceTypeUM).isEqualTo(state.balanceBlockUM.tokenBalanceTypeUM)
        assertThat(result.earnBlockState).isEqualTo(state.earnBlockState)
        assertThat(result.pullToRefreshConfig.isRefreshing).isEqualTo(state.pullToRefreshConfig.isRefreshing)
        assertThat(result.isBalanceHidden).isEqualTo(state.isBalanceHidden)
        assertThat(result.isMarketPriceAvailable).isEqualTo(state.isMarketPriceAvailable)
    }

    @Test
    fun `GIVEN onRefreshSwipe WHEN pull-to-refresh callback invoked THEN onRefreshSwipe is dispatched`() {
        // GIVEN
        val transformer = InitializeWithCryptoCurrencyTransformer(
            cryptoCurrency = cryptoCurrency,
            onBackClick = onBackClick,
            onRefreshSwipe = onRefreshSwipe,
        )

        // WHEN
        val result = transformer.transform(initialState())
        result.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState(value = true))

        // THEN
        verify(exactly = 1) { onRefreshSwipe.invoke(true) }
    }

    private fun initialState(): TokenDetailsUM = TokenDetailsUM(
        topAppBarUM = TokenDetailsTopAppBarUM(
            titleState = TitleState.Simple(tokenName = ""),
            subtitle = stringReference(""),
            onBackClick = {},
            menuItems = persistentListOf(),
        ),
        balanceBlockUM = TokenDetailsBalanceBlockUM.Loading(
            addFundsButton = placeholderButton(),
            swapButton = placeholderButton(),
            transferButton = placeholderButton(),
            tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
            currencyIconState = mockk(relaxed = true),
        ),
        notifications = persistentListOf(),
        marketPriceBlockState = mockk<MarketPriceBlockState>(relaxed = true),
        earnBlockState = null,
        pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
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

    private companion object {
        const val TOKEN_NAME = "Tether"
        const val TOKEN_SYMBOL = "USDT"
    }
}