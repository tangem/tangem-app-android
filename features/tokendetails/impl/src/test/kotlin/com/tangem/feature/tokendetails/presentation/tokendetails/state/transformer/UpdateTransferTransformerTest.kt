package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.AddFundsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TransferUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ZeroBalanceActionsUM
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

class UpdateTransferTransformerTest {

    private val clickIntents: TokenDetailsClickIntents = mockk(relaxed = true)
    private val onActionDispatched: () -> Unit = mockk(relaxed = true)

    @Test
    fun `GIVEN actions without Send nor Sell WHEN transform THEN state is unchanged`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None)),
        )
        val state = initialState()

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result).isSameInstanceAs(state)
        assertThat(result.transferUM).isInstanceOf(TransferUM.Loading::class.java)
    }

    @Test
    fun `GIVEN both Send and Sell available WHEN transform THEN Content carries both rows`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.None),
                TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.None),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.transferUM as TransferUM.Content
        assertThat(content.send).isNotNull()
        assertThat(content.sell).isNotNull()
    }

    @Test
    fun `GIVEN Send disabled AND Sell available WHEN transform THEN Send row stays visible but disabled`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.UsedOutdatedData),
                TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.None),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.transferUM as TransferUM.Content
        assertThat(content.send?.isEnabled).isFalse()
        assertThat(content.sell?.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN Sell disabled AND Send available WHEN transform THEN Sell row stays visible but disabled`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.None),
                TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.NotSupportedBySellService("USDT")),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.transferUM as TransferUM.Content
        assertThat(content.send?.isEnabled).isTrue()
        assertThat(content.sell?.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN Send row WHEN onClick invoked THEN dispatcher fires before send click`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.None)),
        )

        // WHEN
        val content = transformer.transform(initialState()).transferUM as TransferUM.Content
        content.send!!.onClick()

        // THEN
        verifyOrder {
            onActionDispatched.invoke()
            clickIntents.onSendClick(ScenarioUnavailabilityReason.None)
        }
    }

    @Test
    fun `GIVEN Sell row WHEN onClick invoked THEN dispatcher fires before sell click`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.None)),
        )

        // WHEN
        val content = transformer.transform(initialState()).transferUM as TransferUM.Content
        content.sell!!.onClick()

        // THEN
        verifyOrder {
            onActionDispatched.invoke()
            clickIntents.onSellClick(ScenarioUnavailabilityReason.None)
        }
    }

    @Test
    fun `GIVEN actions WHEN transform THEN unrelated state fields are preserved`() {
        // GIVEN
        val state = initialState()
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.None)),
        )

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result.addFundsUM).isSameInstanceAs(state.addFundsUM)
        assertThat(result.balanceBlockUM).isSameInstanceAs(state.balanceBlockUM)
        assertThat(result.topAppBarUM).isSameInstanceAs(state.topAppBarUM)
    }

    @Test
    fun `GIVEN both Send and Sell disabled WHEN transform THEN Content shows both rows disabled`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.UsedOutdatedData),
                TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.Unreachable),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.transferUM as TransferUM.Content
        assertThat(content.send?.isEnabled).isFalse()
        assertThat(content.sell?.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN disabled Send WHEN onClick invoked THEN send click receives the unavailability reason`() {
        // GIVEN
        val reason = ScenarioUnavailabilityReason.UsedOutdatedData
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Send(reason)),
        )

        // WHEN
        val content = transformer.transform(initialState()).transferUM as TransferUM.Content
        content.send!!.onClick()

        // THEN
        verifyOrder {
            onActionDispatched.invoke()
            clickIntents.onSendClick(reason)
        }
    }

    @Test
    fun `GIVEN action with loading marker reason WHEN transform THEN that row is marked isLoading`() {
        // GIVEN — see UpdateAddFundsTransformerTest for rationale. Send/Sell don't normally
        // receive these markers in production, but the row UM honours them uniformly anyway.
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.DataLoading),
                TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.None),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.transferUM as TransferUM.Content
        assertThat(content.send?.isLoading).isTrue()
        assertThat(content.send?.isEnabled).isFalse()
        assertThat(content.sell?.isLoading).isFalse()
        assertThat(content.sell?.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN Swap row WHEN onClick invoked THEN swap-from click receives the reason`() {
        // GIVEN — Transfer context implies "swap THIS token to another", direction = FROM.
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.None, false)),
        )

        // WHEN
        val content = transformer.transform(initialState()).transferUM as TransferUM.Content
        content.swap!!.onClick()

        // THEN
        verifyOrder {
            onActionDispatched.invoke()
            clickIntents.onSwapFromClick(ScenarioUnavailabilityReason.None)
        }
    }

    @Test
    fun `GIVEN row WHEN not clicked THEN no callbacks fire`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.None)),
        )

        // WHEN
        transformer.transform(initialState())

        // THEN
        verify(exactly = 0) { onActionDispatched.invoke() }
        verify(exactly = 0) { clickIntents.onSendClick(any()) }
    }

    private fun createTransformer(actions: List<TokenActionsState.ActionState>) = UpdateTransferTransformer(
        actions = actions,
        clickIntents = clickIntents,
        onActionDispatched = onActionDispatched,
    )

    private fun initialState(): TokenDetailsUM = TokenDetailsUM(
        topAppBarUM = TokenDetailsTopAppBarUM(
            titleState = TitleState.Simple(tokenName = "Tether"),
            subtitle = stringReference("USDT"),
            onBackClick = {},
            menuItems = persistentListOf(),
        ),
        balanceBlockUM = mockk<TokenDetailsBalanceBlockUM>(relaxed = true),
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
}