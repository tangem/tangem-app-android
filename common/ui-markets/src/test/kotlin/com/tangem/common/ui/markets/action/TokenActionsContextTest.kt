package com.tangem.common.ui.markets.action

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.markets.action.TokenActionsBSContentUM.Action
import org.junit.jupiter.api.Test

internal class TokenActionsContextTest {

    @Test
    fun `GIVEN AddFunds context WHEN allowedActionsInOrder THEN Buy Swap Receive`() {
        // Act
        val actual = TokenActionsContext.AddFunds.allowedActionsInOrder

        // Assert
        assertThat(actual).containsExactly(Action.Buy, Action.Exchange, Action.Receive).inOrder()
    }

    @Test
    fun `GIVEN AddFunds context WHEN swapPosition THEN TO`() {
        assertThat(TokenActionsContext.AddFunds.swapPosition)
            .isEqualTo(AppRoute.Swap.CurrencyPosition.TO)
    }

    @Test
    fun `GIVEN Transfer context WHEN swapPosition THEN FROM`() {
        assertThat(TokenActionsContext.Transfer.swapPosition)
            .isEqualTo(AppRoute.Swap.CurrencyPosition.FROM)
    }

    @Test
    fun `GIVEN Markets context WHEN allowedActionsInOrder THEN null meaning all`() {
        assertThat(TokenActionsContext.Markets.allowedActionsInOrder).isNull()
    }
}