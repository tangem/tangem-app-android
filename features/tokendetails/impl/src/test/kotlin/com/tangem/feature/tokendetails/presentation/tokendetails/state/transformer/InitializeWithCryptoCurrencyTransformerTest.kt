package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceTypeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
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

    @Test
    fun `GIVEN crypto currency WHEN transform THEN top bar title is Simple with token name`() {
        // GIVEN
        val transformer = InitializeWithCryptoCurrencyTransformer(
            cryptoCurrency = cryptoCurrency,
            onBackClick = onBackClick,
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
        )

        // WHEN
        val result = transformer.transform(state)

        // THEN — only top bar title/subtitle/onBackClick and marketPriceBlockState are touched
        assertThat(result.topAppBarUM.menuItems).isEqualTo(state.topAppBarUM.menuItems)
        assertThat(result.balanceBlockUM.actionButtons).isEqualTo(state.balanceBlockUM.actionButtons)
        assertThat(result.balanceBlockUM.tokenBalanceTypeUM).isEqualTo(state.balanceBlockUM.tokenBalanceTypeUM)
        assertThat(result.earnBlockState).isEqualTo(state.earnBlockState)
        assertThat(result.pullToRefreshConfig).isSameInstanceAs(state.pullToRefreshConfig)
        assertThat(result.isBalanceHidden).isEqualTo(state.isBalanceHidden)
        assertThat(result.isMarketPriceAvailable).isEqualTo(state.isMarketPriceAvailable)
    }

    private fun initialState(): TokenDetailsUM = TokenDetailsUM(
        topAppBarUM = TokenDetailsTopAppBarUM(
            titleState = TitleState.Simple(tokenName = ""),
            subtitle = stringReference(""),
            onBackClick = {},
            menuItems = persistentListOf(),
        ),
        balanceBlockUM = TokenDetailsBalanceBlockUM.Loading(
            actionButtons = persistentListOf(),
            tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
            currencyIconState = mockk(relaxed = true),
        ),
        notifications = persistentListOf(),
        marketPriceBlockState = mockk<MarketPriceBlockState>(relaxed = true),
        earnBlockState = null,
        pullToRefreshConfig = mockk<PullToRefreshConfig>(relaxed = true),
        isBalanceHidden = false,
        isMarketPriceAvailable = false,
    )

    private companion object {
        const val TOKEN_NAME = "Tether"
        const val TOKEN_SYMBOL = "USDT"
    }
}