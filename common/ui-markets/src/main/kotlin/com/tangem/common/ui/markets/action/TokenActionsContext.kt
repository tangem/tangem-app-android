package com.tangem.common.ui.markets.action

import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.markets.action.TokenActionsBSContentUM.Action

/**
 * Drives how the shared token-actions bottom sheet behaves per entry point.
 *
 * @property allowedActionsInOrder explicit allow-list + display order; `null` means "all actions
 *   in source order" (legacy Markets behaviour).
 * @property swapPosition the [AppRoute.Swap.CurrencyPosition] used when the Swap/Exchange action is
 *   launched from this context.
 */
enum class TokenActionsContext(
    val allowedActionsInOrder: List<Action>?,
    val swapPosition: AppRoute.Swap.CurrencyPosition,
) {
    Markets(
        allowedActionsInOrder = null,
        swapPosition = AppRoute.Swap.CurrencyPosition.ANY,
    ),
    AddFunds(
        allowedActionsInOrder = listOf(Action.Buy, Action.Exchange, Action.Receive),
        swapPosition = AppRoute.Swap.CurrencyPosition.TO,
    ),
    Transfer(
        // Action.SendWithSwap has no TokenActionsState.ActionState counterpart and is never produced from the
        // domain action list; it is injected as a synthetic row by the converter when Swap is available.
        allowedActionsInOrder = listOf(Action.Send, Action.Exchange, Action.SendWithSwap, Action.Sell),
        swapPosition = AppRoute.Swap.CurrencyPosition.FROM,
    ),
}