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
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

class BindTransferActionButtonTransformerTest {

    private val onClick: () -> Unit = mockk(relaxed = true)
    private val addFundsClick: () -> Unit = mockk(relaxed = true)
    private val swapClick: () -> Unit = mockk(relaxed = true)
    private val previousTransferClick: () -> Unit = mockk(relaxed = true)

    @Test
    fun `GIVEN buttons WHEN transform THEN onClick of transfer button is replaced`() {
        // GIVEN
        val transformer = BindTransferActionButtonTransformer(onClick = onClick)

        // WHEN
        val result = transformer.transform(stateWithButtons())
        result.balanceBlockUM.transferButton.onClick()

        // THEN
        verify(exactly = 1) { onClick.invoke() }
        verify(exactly = 0) { previousTransferClick.invoke() }
    }

    @Test
    fun `GIVEN buttons WHEN transform THEN AddFunds and Swap onClick are untouched`() {
        // GIVEN
        val transformer = BindTransferActionButtonTransformer(onClick = onClick)

        // WHEN
        val result = transformer.transform(stateWithButtons())
        result.balanceBlockUM.addFundsButton.onClick()
        result.balanceBlockUM.swapButton.onClick()

        // THEN
        verify(exactly = 0) { onClick.invoke() }
        verify(exactly = 1) { addFundsClick.invoke() }
        verify(exactly = 1) { swapClick.invoke() }
    }

    @Test
    fun `GIVEN transfer button WHEN transform THEN other fields of the button are preserved`() {
        // GIVEN
        val transformer = BindTransferActionButtonTransformer(onClick = onClick)
        val state = stateWithButtons()

        // WHEN
        val result = transformer.transform(state)

        // THEN
        val original = state.balanceBlockUM.transferButton
        val updated = result.balanceBlockUM.transferButton
        assertThat(updated.text).isEqualTo(original.text)
        assertThat(updated.tangemIconUM).isEqualTo(original.tangemIconUM)
        assertThat(updated.type).isEqualTo(original.type)
        assertThat(updated.isEnabled).isEqualTo(original.isEnabled)
    }

    @Test
    fun `GIVEN buttons WHEN transform THEN unrelated state fields are preserved`() {
        // GIVEN
        val transformer = BindTransferActionButtonTransformer(onClick = onClick)
        val state = stateWithButtons()

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result.topAppBarUM).isSameInstanceAs(state.topAppBarUM)
        assertThat(result.addFundsUM).isSameInstanceAs(state.addFundsUM)
        assertThat(result.transferUM).isSameInstanceAs(state.transferUM)
    }

    private fun stateWithButtons(): TokenDetailsUM = TokenDetailsUM(
        topAppBarUM = TokenDetailsTopAppBarUM(
            titleState = TitleState.Simple(tokenName = "Tether"),
            subtitle = stringReference("USDT"),
            onBackClick = {},
            menuItems = persistentListOf(),
        ),
        balanceBlockUM = TokenDetailsBalanceBlockUM.Loading(
            addFundsButton = button(text = "Add funds", onClick = addFundsClick),
            swapButton = button(text = "Swap", onClick = swapClick),
            transferButton = button(text = "Transfer", onClick = previousTransferClick),
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

    private fun button(text: String, onClick: () -> Unit) = TangemButtonUM(
        text = stringReference(text),
        type = TangemButtonType.Secondary,
        onClick = onClick,
    )
}