package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
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
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

class UpdateZeroBalanceActionsTransformerTest {

    private val clickIntents: TokenDetailsClickIntents = mockk(relaxed = true)

    @Test
    fun `GIVEN actions without Buy Swap or Receive WHEN transform THEN state is unchanged`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.None)),
        )
        val state = initialState()

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result).isSameInstanceAs(state)
        assertThat(result.zeroBalanceActionsUM).isInstanceOf(ZeroBalanceActionsUM.Loading::class.java)
    }

    @Test
    fun `GIVEN all three actions available WHEN transform THEN Content carries all rows enabled`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None),
                TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.None, false),
                TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.zeroBalanceActionsUM as ZeroBalanceActionsUM.Content
        assertThat(content.buy?.isEnabled).isTrue()
        assertThat(content.swap?.isEnabled).isTrue()
        assertThat(content.receive?.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN Swap with unavailability reason WHEN transform THEN Swap row stays visible but disabled`() {
        // GIVEN — when Swap carries any non-None unavailability reason the row must stay visible
        // (layout keeps three slots) but render disabled; click is gated at the row level
        // via isEnabled = false. A None reason still produces an enabled row (see test above).
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None),
                TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.UsedOutdatedData, false),
                TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.zeroBalanceActionsUM as ZeroBalanceActionsUM.Content
        assertThat(content.swap).isNotNull()
        assertThat(content.swap?.isEnabled).isFalse()
        assertThat(content.buy?.isEnabled).isTrue()
        assertThat(content.receive?.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN missing actions WHEN transform THEN absent rows are null`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None)),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.zeroBalanceActionsUM as ZeroBalanceActionsUM.Content
        assertThat(content.buy).isNotNull()
        assertThat(content.swap).isNull()
        assertThat(content.receive).isNull()
    }

    @Test
    fun `GIVEN Buy row WHEN onClick invoked THEN buy click is dispatched with reason`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None)),
        )

        // WHEN
        val content = transformer.transform(initialState()).zeroBalanceActionsUM as ZeroBalanceActionsUM.Content
        content.buy!!.onClick()

        // THEN
        verify(exactly = 1) { clickIntents.onBuyClick(ScenarioUnavailabilityReason.None) }
    }

    @Test
    fun `GIVEN disabled Swap WHEN onClick invoked THEN swap click receives the unavailability reason`() {
        // GIVEN — Row.onClick is always wired; UI gating (isEnabled=false) decides whether it fires.
        // This test guards the wiring: when the row IS invoked, the reason is forwarded so callers
        // could decide to show the unavailability dialog if they ever drop the UI gating.
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.UsedOutdatedData, false),
            ),
        )

        // WHEN
        val content = transformer.transform(initialState()).zeroBalanceActionsUM as ZeroBalanceActionsUM.Content
        content.swap!!.onClick()

        // THEN
        verify(exactly = 1) { clickIntents.onSwapToClick(ScenarioUnavailabilityReason.UsedOutdatedData) }
    }

    @Test
    fun `GIVEN Receive row WHEN long-clicked THEN copy address is dispatched`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None)),
        )

        // WHEN
        val content = transformer.transform(initialState()).zeroBalanceActionsUM as ZeroBalanceActionsUM.Content
        content.receive!!.onLongClick!!()

        // THEN
        verify(exactly = 1) { clickIntents.onCopyAddress() }
    }

    @Test
    fun `GIVEN actions WHEN transform THEN unrelated state fields are preserved`() {
        // GIVEN
        val state = initialState()
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None)),
        )

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result.addFundsUM).isSameInstanceAs(state.addFundsUM)
        assertThat(result.transferUM).isSameInstanceAs(state.transferUM)
        assertThat(result.balanceBlockUM).isSameInstanceAs(state.balanceBlockUM)
        assertThat(result.topAppBarUM).isSameInstanceAs(state.topAppBarUM)
    }

    @Test
    fun `GIVEN action with loading marker reason WHEN transform THEN that row is marked isLoading`() {
        // GIVEN — Swap commonly arrives with DataLoading while networkSource is still CACHE.
        // The Swap row stays in Content but with isLoading=true so the UI keeps the spinner.
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None),
                TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.DataLoading, false),
                TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.zeroBalanceActionsUM as ZeroBalanceActionsUM.Content
        assertThat(content.swap?.isLoading).isTrue()
        assertThat(content.swap?.isEnabled).isFalse()
        assertThat(content.buy?.isLoading).isFalse()
        assertThat(content.receive?.isLoading).isFalse()
    }

    @Test
    fun `GIVEN Receive with None reason AND networkSource is CACHE WHEN transform THEN only Receive is marked isLoading`() {
        // GIVEN — networkSource=CACHE is the "still loading" signal for Receive (which has no
        // Loading reason of its own). Buy/Swap rely on their own reasons and must not be flipped.
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None),
                TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.None, false),
                TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None),
            ),
            networkSource = StatusSource.CACHE,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.zeroBalanceActionsUM as ZeroBalanceActionsUM.Content
        assertThat(content.receive?.isLoading).isTrue()
        assertThat(content.receive?.isEnabled).isTrue()
        assertThat(content.buy?.isLoading).isFalse()
        assertThat(content.swap?.isLoading).isFalse()
    }

    @Test
    fun `GIVEN Receive with None reason AND networkSource is ONLY_CACHE WHEN transform THEN Receive is not loading`() {
        // GIVEN — ONLY_CACHE is the terminal "refresh failed" state; Receive drops the spinner.
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None),
            ),
            networkSource = StatusSource.ONLY_CACHE,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.zeroBalanceActionsUM as ZeroBalanceActionsUM.Content
        assertThat(content.receive?.isLoading).isFalse()
        assertThat(content.receive?.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN row WHEN not clicked THEN no callbacks fire`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None)),
        )

        // WHEN
        transformer.transform(initialState())

        // THEN
        verify(exactly = 0) { clickIntents.onBuyClick(any()) }
    }

    private fun createTransformer(
        actions: List<TokenActionsState.ActionState>,
        networkSource: StatusSource = StatusSource.ACTUAL,
    ) = UpdateZeroBalanceActionsTransformer(
        actions = actions,
        networkSource = networkSource,
        clickIntents = clickIntents,
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