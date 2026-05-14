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

class UpdateAddFundsTransformerTest {

    private val clickIntents: TokenDetailsClickIntents = mockk(relaxed = true)
    private val onActionDispatched: () -> Unit = mockk(relaxed = true)

    @Test
    fun `GIVEN actions without Buy, Swap nor Receive WHEN transform THEN state is unchanged`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.None)),
        )
        val state = initialState()

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result).isSameInstanceAs(state)
        assertThat(result.addFundsUM).isInstanceOf(AddFundsUM.Loading::class.java)
    }

    @Test
    fun `GIVEN both Buy and Receive available WHEN transform THEN Content carries both rows`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None),
                TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.addFundsUM as AddFundsUM.Content
        assertThat(content.buy).isNotNull()
        assertThat(content.receive).isNotNull()
    }

    @Test
    fun `GIVEN Buy disabled AND Receive available WHEN transform THEN Buy row stays visible but disabled`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.BuyUnavailable("USDT")),
                TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.addFundsUM as AddFundsUM.Content
        assertThat(content.buy).isNotNull()
        assertThat(content.buy?.isEnabled).isFalse()
        assertThat(content.receive?.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN only Buy available WHEN transform THEN Receive row is null`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None)),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.addFundsUM as AddFundsUM.Content
        assertThat(content.buy).isNotNull()
        assertThat(content.receive).isNull()
    }

    @Test
    fun `GIVEN Buy row WHEN onClick invoked THEN dispatcher fires before buy click`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None)),
        )

        // WHEN
        val content = transformer.transform(initialState()).addFundsUM as AddFundsUM.Content
        content.buy!!.onClick()

        // THEN
        verifyOrder {
            onActionDispatched.invoke()
            clickIntents.onBuyClick(ScenarioUnavailabilityReason.None)
        }
    }

    @Test
    fun `GIVEN Receive row WHEN long-clicked THEN dispatcher fires before copy address`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None)),
        )

        // WHEN
        val content = transformer.transform(initialState()).addFundsUM as AddFundsUM.Content
        content.receive!!.onLongClick!!()

        // THEN
        verifyOrder {
            onActionDispatched.invoke()
            clickIntents.onCopyAddress()
        }
    }

    @Test
    fun `GIVEN Receive row WHEN onClick invoked THEN dispatcher fires before receive click`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None)),
        )

        // WHEN
        val content = transformer.transform(initialState()).addFundsUM as AddFundsUM.Content
        content.receive!!.onClick()

        // THEN
        verifyOrder {
            onActionDispatched.invoke()
            clickIntents.onReceiveClick(ScenarioUnavailabilityReason.None)
        }
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
        assertThat(result.transferUM).isSameInstanceAs(state.transferUM)
        assertThat(result.balanceBlockUM).isSameInstanceAs(state.balanceBlockUM)
        assertThat(result.topAppBarUM).isSameInstanceAs(state.topAppBarUM)
    }

    @Test
    fun `GIVEN both Buy and Receive disabled WHEN transform THEN Content shows both rows disabled`() {
        // GIVEN
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.BuyUnavailable("USDT")),
                TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.UnassociatedAsset),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.addFundsUM as AddFundsUM.Content
        assertThat(content.buy?.isEnabled).isFalse()
        assertThat(content.receive?.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN disabled Buy WHEN onClick invoked THEN buy click receives the unavailability reason`() {
        // GIVEN — Row.onClick is always wired; UI gating decides whether it fires. This test
        // guards the wiring: when the row IS invoked, the reason is forwarded.
        val reason = ScenarioUnavailabilityReason.BuyUnavailable("USDT")
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Buy(reason)),
        )

        // WHEN
        val content = transformer.transform(initialState()).addFundsUM as AddFundsUM.Content
        content.buy!!.onClick()

        // THEN
        verifyOrder {
            onActionDispatched.invoke()
            clickIntents.onBuyClick(reason)
        }
    }

    @Test
    fun `GIVEN action with loading marker reason WHEN transform THEN that row is marked isLoading`() {
        // GIVEN — DataLoading/ExpressLoading signal the underlying data is still being fetched.
        // The row stays in Content but with isLoading=true so the UI keeps the spinner.
        val transformer = createTransformer(
            actions = listOf(
                TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.ExpressLoading("USDT")),
                TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.addFundsUM as AddFundsUM.Content
        assertThat(content.buy?.isLoading).isTrue()
        assertThat(content.buy?.isEnabled).isFalse()
        assertThat(content.receive?.isLoading).isFalse()
        assertThat(content.receive?.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN Swap row WHEN onClick invoked THEN swap-to click receives the reason`() {
        // GIVEN — AddFunds context implies "swap something INTO this token", direction = TO.
        val transformer = createTransformer(
            actions = listOf(TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.None, false)),
        )

        // WHEN
        val content = transformer.transform(initialState()).addFundsUM as AddFundsUM.Content
        content.swap!!.onClick()

        // THEN
        verifyOrder {
            onActionDispatched.invoke()
            clickIntents.onSwapToClick(ScenarioUnavailabilityReason.None)
        }
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
        verify(exactly = 0) { onActionDispatched.invoke() }
        verify(exactly = 0) { clickIntents.onBuyClick(any()) }
    }

    private fun createTransformer(actions: List<TokenActionsState.ActionState>) = UpdateAddFundsTransformer(
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