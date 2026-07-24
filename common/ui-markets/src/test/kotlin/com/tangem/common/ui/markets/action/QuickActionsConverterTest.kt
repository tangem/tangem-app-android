package com.tangem.common.ui.markets.action

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class QuickActionsConverterTest {

    @Test
    fun `GIVEN actions in source order WHEN context is AddFunds THEN buy exchange receive in order with no stake`() {
        // Arrange
        val actions = listOf(
            TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None),
            TokenActionsState.ActionState.Stake(ScenarioUnavailabilityReason.None, option = null),
            TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.None, shouldShowBadge = false),
            TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None),
        )

        // Act
        val result = QuickActionsConverter.toQuickActions(
            actions = actions,
            context = TokenActionsContext.AddFunds,
        )

        // Assert
        assertThat(result).containsExactly(
            QuickActionUM.V2.Buy,
            QuickActionUM.V2.Exchange(shouldShowBadge = false),
            QuickActionUM.V2.Receive,
        ).inOrder()
        assertThat(result).doesNotContain(QuickActionUM.V2.Stake)
    }

    @Test
    fun `GIVEN receive and stake WHEN context is Markets THEN preserves source order and keeps stake`() {
        // Arrange
        val actions = listOf(
            TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None),
            TokenActionsState.ActionState.Stake(ScenarioUnavailabilityReason.None, option = null),
        )

        // Act
        val result = QuickActionsConverter.toQuickActions(
            actions = actions,
            context = TokenActionsContext.Markets,
        )

        // Assert
        assertThat(result).containsExactly(
            QuickActionUM.V2.Receive,
            QuickActionUM.V2.Stake,
        ).inOrder()
    }

    @Test
    fun `GIVEN buy with unavailability reason mixed with available actions WHEN toQuickActions THEN unavailable action is excluded`() {
        // Arrange
        val actions = listOf(
            TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.BuyUnavailable(cryptoCurrencyName = "BTC")),
            TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None),
            TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.None, shouldShowBadge = false),
        )

        // Act
        val result = QuickActionsConverter.toQuickActions(
            actions = actions,
            context = TokenActionsContext.Markets,
        )

        // Assert
        assertThat(result).containsExactly(
            QuickActionUM.V2.Receive,
            QuickActionUM.V2.Exchange(shouldShowBadge = false),
        ).inOrder()
        assertThat(result).doesNotContain(QuickActionUM.V2.Buy)
    }

    @Test
    fun `GIVEN send swap sell available WHEN context is Transfer THEN send exchange swapAndSend sell in order`() {
        // Arrange
        val actions = listOf(
            TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.None),
            TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.None, shouldShowBadge = false),
            TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.None),
        )

        // Act
        val result = QuickActionsConverter.toQuickActions(
            actions = actions,
            context = TokenActionsContext.Transfer,
        )

        // Assert
        assertThat(result).containsExactly(
            QuickActionUM.V2.Send,
            QuickActionUM.V2.Exchange(shouldShowBadge = false),
            QuickActionUM.V2.SwapAndSend,
            QuickActionUM.V2.Sell,
        ).inOrder()
    }

    @Test
    fun `GIVEN send and sell but no swap WHEN context is Transfer THEN only send and sell with no swapAndSend`() {
        // Arrange
        val actions = listOf(
            TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.None),
            TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.None),
        )

        // Act
        val result = QuickActionsConverter.toQuickActions(
            actions = actions,
            context = TokenActionsContext.Transfer,
        )

        // Assert
        assertThat(result).containsExactly(
            QuickActionUM.V2.Send,
            QuickActionUM.V2.Sell,
        ).inOrder()
        assertThat(result).doesNotContain(QuickActionUM.V2.SwapAndSend)
        assertThat(result).doesNotContain(QuickActionUM.V2.Exchange(shouldShowBadge = false))
    }

    @Test
    fun `GIVEN buy and swap unavailable WHEN context is AddFunds THEN they are still shown (disabled) not hidden`() {
        // Arrange
        val actions = listOf(
            TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.BuyUnavailable(cryptoCurrencyName = "BTC")),
            TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.SingleWallet, shouldShowBadge = false),
            TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None),
        )

        // Act
        val result = QuickActionsConverter.toQuickActions(
            actions = actions,
            context = TokenActionsContext.AddFunds,
        )

        // Assert
        assertThat(result).containsExactly(
            QuickActionUM.V2.Buy,
            QuickActionUM.V2.Exchange(shouldShowBadge = false),
            QuickActionUM.V2.Receive,
        ).inOrder()
    }

    @Test
    fun `GIVEN swap and sell unavailable WHEN context is Transfer THEN swap and sell shown disabled and no swapAndSend`() {
        // Arrange
        val actions = listOf(
            TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.None),
            TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.SingleWallet, shouldShowBadge = false),
            TokenActionsState.ActionState.Sell(
                ScenarioUnavailabilityReason.NotSupportedBySellService(cryptoCurrencyName = "BTC"),
            ),
        )

        // Act
        val result = QuickActionsConverter.toQuickActions(
            actions = actions,
            context = TokenActionsContext.Transfer,
        )

        // Assert
        assertThat(result).containsExactly(
            QuickActionUM.V2.Send,
            QuickActionUM.V2.Exchange(shouldShowBadge = false),
            QuickActionUM.V2.Sell,
        ).inOrder()
        assertThat(result).doesNotContain(QuickActionUM.V2.SwapAndSend)
    }

    @Test
    fun `GIVEN send and sell unavailable WHEN context is Transfer THEN no actions are marked disabled`() {
        // Arrange
        val actions = listOf(
            TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.EmptyBalance(SEND_SCENARIO)),
            TokenActionsState.ActionState.Sell(
                ScenarioUnavailabilityReason.NotSupportedBySellService(cryptoCurrencyName = "BTC"),
            ),
        )

        // Act
        val result = QuickActionsConverter.quickActions(
            cryptoData = cryptoData(actions),
            tokenActionsHandler = mockk(relaxed = true),
            context = TokenActionsContext.Transfer,
        )

        // Assert
        assertThat(result.actions).containsExactly(QuickActionUM.V2.Send, QuickActionUM.V2.Sell).inOrder()
        assertThat(result.disabledActions).isEmpty()
    }

    @Test
    fun `GIVEN buy unavailable WHEN context is AddFunds THEN buy is marked disabled`() {
        // Arrange
        val actions = listOf(
            TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.BuyUnavailable(cryptoCurrencyName = "BTC")),
            TokenActionsState.ActionState.Receive(ScenarioUnavailabilityReason.None),
        )

        // Act
        val result = QuickActionsConverter.quickActions(
            cryptoData = cryptoData(actions),
            tokenActionsHandler = mockk(relaxed = true),
            context = TokenActionsContext.AddFunds,
        )

        // Assert
        assertThat(result.disabledActions).containsExactly(QuickActionUM.V2.Buy)
    }

    @Test
    fun `GIVEN unavailable action in list WHEN unavailabilityReason THEN returns its reason`() {
        // Arrange
        val reason = ScenarioUnavailabilityReason.NotSupportedBySellService(cryptoCurrencyName = "BTC")
        val actions = listOf(
            TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.None),
            TokenActionsState.ActionState.Sell(reason),
        )

        // Act
        val result = QuickActionsConverter.unavailabilityReason(TokenActionsBSContentUM.Action.Sell, actions)

        // Assert
        assertThat(result).isEqualTo(reason)
    }

    @Test
    fun `GIVEN available action WHEN unavailabilityReason THEN returns None`() {
        // Arrange
        val actions = listOf(TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.None))

        // Act
        val result = QuickActionsConverter.unavailabilityReason(TokenActionsBSContentUM.Action.Send, actions)

        // Assert
        assertThat(result).isEqualTo(ScenarioUnavailabilityReason.None)
    }

    @Test
    fun `GIVEN action absent from list WHEN unavailabilityReason THEN returns None`() {
        // Act
        val result = QuickActionsConverter.unavailabilityReason(TokenActionsBSContentUM.Action.Sell, actions = emptyList())

        // Assert
        assertThat(result).isEqualTo(ScenarioUnavailabilityReason.None)
    }

    private fun cryptoData(actions: List<TokenActionsState.ActionState>) = CryptoCurrencyData(
        userWallet = mockk(relaxed = true),
        status = mockk(relaxed = true),
        actions = actions,
        isAccountMode = false,
        account = mockk(relaxed = true),
    )

    private companion object {
        val SEND_SCENARIO = ScenarioUnavailabilityReason.WithdrawalScenario.SEND
    }
}