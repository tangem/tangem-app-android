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

class ToggleBalanceTypeTransformerTest {

    private val transformer = ToggleBalanceTypeTransformer()

    // region Toggle logic

    @Test
    fun `GIVEN Multiple with ALL WHEN transform THEN type switches to AVAILABLE`() {
        // GIVEN
        val state = stateWithContent(type = TokenBalanceTypeUM.Type.ALL)

        // WHEN
        val result = transformer.transform(state)

        // THEN
        val multiple = (result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content)
            .tokenBalanceTypeUM as TokenBalanceTypeUM.Multiple
        assertThat(multiple.type).isEqualTo(TokenBalanceTypeUM.Type.AVAILABLE)
    }

    @Test
    fun `GIVEN Multiple with AVAILABLE WHEN transform THEN type switches to ALL`() {
        // GIVEN
        val state = stateWithContent(type = TokenBalanceTypeUM.Type.AVAILABLE)

        // WHEN
        val result = transformer.transform(state)

        // THEN
        val multiple = (result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content)
            .tokenBalanceTypeUM as TokenBalanceTypeUM.Multiple
        assertThat(multiple.type).isEqualTo(TokenBalanceTypeUM.Type.ALL)
    }

    @Test
    fun `GIVEN Multiple with ALL WHEN transform twice THEN type returns to ALL`() {
        // GIVEN
        val state = stateWithContent(type = TokenBalanceTypeUM.Type.ALL)

        // WHEN
        val result = transformer.transform(transformer.transform(state))

        // THEN
        val multiple = (result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content)
            .tokenBalanceTypeUM as TokenBalanceTypeUM.Multiple
        assertThat(multiple.type).isEqualTo(TokenBalanceTypeUM.Type.ALL)
    }

    // endregion

    // region No-op cases

    @Test
    fun `GIVEN Loading balance block WHEN transform THEN state is unchanged`() {
        // GIVEN
        val state = initialState()

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result).isSameInstanceAs(state)
    }

    @Test
    fun `GIVEN Error balance block WHEN transform THEN state is unchanged`() {
        // GIVEN
        val state = initialState().copy(
            balanceBlockUM = TokenDetailsBalanceBlockUM.Error(
                addFundsButton = placeholderButton(),
                swapButton = placeholderButton(),
                transferButton = placeholderButton(),
                tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                currencyIconState = CurrencyIconState.Loading,
            ),
        )

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result).isSameInstanceAs(state)
    }

    @Test
    fun `GIVEN Content with Single balance type WHEN transform THEN state is unchanged`() {
        // GIVEN
        val state = initialState().copy(
            balanceBlockUM = TokenDetailsBalanceBlockUM.Content(
                addFundsButton = placeholderButton(),
                swapButton = placeholderButton(),
                transferButton = placeholderButton(),
                tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                currencyIconState = CurrencyIconState.Loading,
                displayCryptoBalanceAll = stringReference("1.0 ETH"),
                displayFiatBalanceAll = stringReference("$2,000"),
                displayCryptoBalanceAvailable = null,
                displayFiatBalanceAvailable = null,
                isBalanceFlickering = false,
                isBalanceZero = false,
            ),
        )

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result).isSameInstanceAs(state)
    }

    // endregion

    // region Unrelated fields preserved

    @Test
    fun `GIVEN any togglable state WHEN transform THEN unrelated fields are preserved`() {
        // GIVEN
        val state = stateWithContent(type = TokenBalanceTypeUM.Type.ALL)

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

    @Test
    fun `GIVEN togglable state WHEN transform THEN balance content fields besides type are preserved`() {
        // GIVEN
        val state = stateWithContent(type = TokenBalanceTypeUM.Type.ALL)
        val originalContent = state.balanceBlockUM as TokenDetailsBalanceBlockUM.Content

        // WHEN
        val result = transformer.transform(state)

        // THEN
        val resultContent = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(resultContent.addFundsButton).isEqualTo(originalContent.addFundsButton)
        assertThat(resultContent.swapButton).isEqualTo(originalContent.swapButton)
        assertThat(resultContent.transferButton).isEqualTo(originalContent.transferButton)
        assertThat(resultContent.currencyIconState).isEqualTo(originalContent.currencyIconState)
        assertThat(resultContent.displayCryptoBalanceAll).isEqualTo(originalContent.displayCryptoBalanceAll)
        assertThat(resultContent.displayFiatBalanceAll).isEqualTo(originalContent.displayFiatBalanceAll)
        assertThat(resultContent.isBalanceFlickering).isEqualTo(originalContent.isBalanceFlickering)
    }

    // endregion

    private fun stateWithContent(type: TokenBalanceTypeUM.Type): TokenDetailsUM {
        return initialState().copy(
            balanceBlockUM = TokenDetailsBalanceBlockUM.Content(
                addFundsButton = placeholderButton(),
                swapButton = placeholderButton(),
                transferButton = placeholderButton(),
                tokenBalanceTypeUM = TokenBalanceTypeUM.Multiple(
                    type = type,
                    availableTypes = persistentListOf(
                        TokenBalanceTypeUM.Type.ALL,
                        TokenBalanceTypeUM.Type.AVAILABLE,
                    ),
                    onSelect = {},
                ),
                currencyIconState = CurrencyIconState.Loading,
                displayCryptoBalanceAll = stringReference("10.5 ETH"),
                displayFiatBalanceAll = stringReference("$21,000"),
                displayCryptoBalanceAvailable = stringReference("9.0 ETH"),
                displayFiatBalanceAvailable = stringReference("$18,000"),
                isBalanceFlickering = false,
                isBalanceZero = false,
            ),
        )
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

    private fun placeholderButton(): TangemButtonUM = TangemButtonUM(
        text = stringReference(""),
        type = TangemButtonType.Secondary,
        onClick = {},
    )
}