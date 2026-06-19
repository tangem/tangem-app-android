package com.tangem.common.ui.markets.action

import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

object QuickActionsConverter {

    fun quickActions(
        cryptoData: CryptoCurrencyData,
        tokenActionsHandler: TokenActionsHandler,
        isRedesignEnabled: Boolean,
        context: TokenActionsContext = TokenActionsContext.Markets,
    ): QuickActions {
        return QuickActions(
            actions = toQuickActions(cryptoData.actions, isRedesignEnabled, context),
            onQuickActionClick = { quickActionUM ->
                tokenActionsHandler.handle(
                    action = quickActionUM.toHandledAction(),
                    cryptoCurrencyData = cryptoData,
                    context = context,
                )
            },
            onQuickActionLongClick = { actionUM ->
                if (actionUM == QuickActionUM.V1.Receive || actionUM == QuickActionUM.V2.Receive) {
                    tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.CopyAddress,
                        cryptoCurrencyData = cryptoData,
                    )
                }
            },
        )
    }

    private fun QuickActionUM.toHandledAction(): TokenActionsBSContentUM.Action = when (this) {
        QuickActionUM.V1.Buy, QuickActionUM.V2.Buy -> TokenActionsBSContentUM.Action.Buy
        is QuickActionUM.V1.Exchange, is QuickActionUM.V2.Exchange -> TokenActionsBSContentUM.Action.Exchange
        QuickActionUM.V1.Receive, QuickActionUM.V2.Receive -> TokenActionsBSContentUM.Action.Receive
        QuickActionUM.V1.Stake, QuickActionUM.V2.Stake -> TokenActionsBSContentUM.Action.Stake
        is QuickActionUM.V1.YieldMode, is QuickActionUM.V2.YieldMode -> TokenActionsBSContentUM.Action.YieldMode
        QuickActionUM.V1.Send, QuickActionUM.V2.Send -> TokenActionsBSContentUM.Action.Send
        QuickActionUM.V1.Sell, QuickActionUM.V2.Sell -> TokenActionsBSContentUM.Action.Sell
        QuickActionUM.V1.SwapAndSend, QuickActionUM.V2.SwapAndSend -> TokenActionsBSContentUM.Action.SendWithSwap
    }

    /**
     * Returns available actions filtered to [context]'s allow-list and ordered by it.
     * Omitting [context] (default [TokenActionsContext.Markets]) yields all available actions in source order;
     * a context with a non-null [TokenActionsContext.allowedActionsInOrder] filters to and orders by that list.
     */
    fun toQuickActions(
        actions: List<TokenActionsState.ActionState>,
        isRedesignEnabled: Boolean,
        context: TokenActionsContext = TokenActionsContext.Markets,
    ): ImmutableList<QuickActionUM> {
        val available = actions.filter { it.unavailabilityReason == ScenarioUnavailabilityReason.None }
        val allowed = context.allowedActionsInOrder
            ?: return available.mapNotNull { it.toQuickActionUM(isRedesignEnabled) }.toImmutableList()

        val byBsAction = available.associateBy { it.toBsAction() }
        val hasExchange = byBsAction.containsKey(TokenActionsBSContentUM.Action.Exchange)
        return allowed.mapNotNull { action ->
            when (action) {
                TokenActionsBSContentUM.Action.SendWithSwap ->
                    if (hasExchange) swapAndSendUM(isRedesignEnabled) else null
                else -> byBsAction[action]?.toQuickActionUM(isRedesignEnabled)
            }
        }.toImmutableList()
    }

    private fun swapAndSendUM(isRedesignEnabled: Boolean): QuickActionUM =
        if (isRedesignEnabled) QuickActionUM.V2.SwapAndSend else QuickActionUM.V1.SwapAndSend

    private fun TokenActionsState.ActionState.toQuickActionUM(isRedesignEnabled: Boolean): QuickActionUM? =
        if (isRedesignEnabled) toV2() else toV1()

    private fun TokenActionsState.ActionState.toV2(): QuickActionUM? = when (this) {
        is TokenActionsState.ActionState.Buy -> QuickActionUM.V2.Buy
        is TokenActionsState.ActionState.Swap -> QuickActionUM.V2.Exchange(shouldShowBadge)
        is TokenActionsState.ActionState.Receive -> QuickActionUM.V2.Receive
        is TokenActionsState.ActionState.Send -> QuickActionUM.V2.Send
        is TokenActionsState.ActionState.Sell -> QuickActionUM.V2.Sell
        is TokenActionsState.ActionState.Stake -> QuickActionUM.V2.Stake
        is TokenActionsState.ActionState.YieldMode -> QuickActionUM.V2.YieldMode(apy)
        else -> null
    }

    private fun TokenActionsState.ActionState.toV1(): QuickActionUM? = when (this) {
        is TokenActionsState.ActionState.Buy -> QuickActionUM.V1.Buy
        is TokenActionsState.ActionState.Swap -> QuickActionUM.V1.Exchange(shouldShowBadge)
        is TokenActionsState.ActionState.Receive -> QuickActionUM.V1.Receive
        is TokenActionsState.ActionState.Send -> QuickActionUM.V1.Send
        is TokenActionsState.ActionState.Sell -> QuickActionUM.V1.Sell
        is TokenActionsState.ActionState.Stake -> QuickActionUM.V1.Stake
        is TokenActionsState.ActionState.YieldMode -> QuickActionUM.V1.YieldMode(apy)
        else -> null
    }

    private fun TokenActionsState.ActionState.toBsAction(): TokenActionsBSContentUM.Action? = when (this) {
        is TokenActionsState.ActionState.Buy -> TokenActionsBSContentUM.Action.Buy
        is TokenActionsState.ActionState.Swap -> TokenActionsBSContentUM.Action.Exchange
        is TokenActionsState.ActionState.Receive -> TokenActionsBSContentUM.Action.Receive
        is TokenActionsState.ActionState.Send -> TokenActionsBSContentUM.Action.Send
        is TokenActionsState.ActionState.Sell -> TokenActionsBSContentUM.Action.Sell
        is TokenActionsState.ActionState.Stake -> TokenActionsBSContentUM.Action.Stake
        is TokenActionsState.ActionState.YieldMode -> TokenActionsBSContentUM.Action.YieldMode
        else -> null
    }
}