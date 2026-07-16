package com.tangem.common.ui.markets.action

import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

object QuickActionsConverter {

    fun quickActions(
        cryptoData: CryptoCurrencyData,
        tokenActionsHandler: TokenActionsHandler,
        isRedesignEnabled: Boolean,
        context: TokenActionsContext = TokenActionsContext.Markets,
    ): QuickActions {
        val states = toQuickActionStates(cryptoData.actions, isRedesignEnabled, context)
        return QuickActions(
            actions = states.map { it.action }.toImmutableList(),
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
            disabledActions = if (context.shouldShowUnavailableActionsAsEnabled) {
                persistentSetOf()
            } else {
                states.filterNot { it.isEnabled }.map { it.action }.toImmutableSet()
            },
        )
    }

    /**
     * Unavailability reason for [action] as produced by the domain [actions] list, or
     * [ScenarioUnavailabilityReason.None] when the action is available or has no domain counterpart.
     */
    fun unavailabilityReason(
        action: TokenActionsBSContentUM.Action,
        actions: List<TokenActionsState.ActionState>,
    ): ScenarioUnavailabilityReason = actions.firstOrNull { it.toBsAction() == action }?.unavailabilityReason
        ?: ScenarioUnavailabilityReason.None

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
     * Returns actions filtered and ordered for [context].
     * Omitting [context] (default [TokenActionsContext.Markets]) yields only available actions in source order.
     * A context with a non-null [TokenActionsContext.allowedActionsInOrder] returns that list's actions in order,
     * including unavailable ones (they are meant to be shown disabled by the caller).
     */
    fun toQuickActions(
        actions: List<TokenActionsState.ActionState>,
        isRedesignEnabled: Boolean,
        context: TokenActionsContext = TokenActionsContext.Markets,
    ): ImmutableList<QuickActionUM> =
        toQuickActionStates(actions, isRedesignEnabled, context).map { it.action }.toImmutableList()

    private fun toQuickActionStates(
        actions: List<TokenActionsState.ActionState>,
        isRedesignEnabled: Boolean,
        context: TokenActionsContext,
    ): List<QuickActionState> {
        val allowed = context.allowedActionsInOrder
            ?: return actions
                .filter { it.unavailabilityReason == ScenarioUnavailabilityReason.None }
                .mapNotNull { action ->
                    action.toQuickActionUM(isRedesignEnabled)?.let { QuickActionState(it, isEnabled = true) }
                }

        val byBsAction = actions.associateBy { it.toBsAction() }
        val isExchangeAvailable = byBsAction[TokenActionsBSContentUM.Action.Exchange]
            ?.unavailabilityReason == ScenarioUnavailabilityReason.None
        return allowed.mapNotNull { action ->
            when (action) {
                TokenActionsBSContentUM.Action.SendWithSwap ->
                    if (isExchangeAvailable) {
                        QuickActionState(swapAndSendUM(isRedesignEnabled), isEnabled = true)
                    } else {
                        null
                    }
                else -> {
                    val state = byBsAction[action] ?: return@mapNotNull null
                    val um = state.toQuickActionUM(isRedesignEnabled) ?: return@mapNotNull null
                    QuickActionState(um, isEnabled = state.unavailabilityReason == ScenarioUnavailabilityReason.None)
                }
            }
        }
    }

    private data class QuickActionState(val action: QuickActionUM, val isEnabled: Boolean)

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