package com.tangem.common.ui.markets.action

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
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
            isRedesignEnabled = true,
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
            isRedesignEnabled = true,
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
            isRedesignEnabled = true,
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
            isRedesignEnabled = true,
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
            isRedesignEnabled = true,
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
}