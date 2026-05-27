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
    ): QuickActions {
        return QuickActions(
            actions = toQuickActions(cryptoData.actions, isRedesignEnabled),
            onQuickActionClick = { quickActionUM ->
                when (quickActionUM) {
                    QuickActionUM.V1.Buy -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Buy,
                        cryptoCurrencyData = cryptoData,
                    )
                    is QuickActionUM.V1.Exchange -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Exchange,
                        cryptoCurrencyData = cryptoData,
                    )
                    QuickActionUM.V1.Receive -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Receive,
                        cryptoCurrencyData = cryptoData,
                    )
                    QuickActionUM.V1.Stake -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Stake,
                        cryptoCurrencyData = cryptoData,
                    )
                    is QuickActionUM.V1.YieldMode -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.YieldMode,
                        cryptoCurrencyData = cryptoData,
                    )
                    QuickActionUM.V2.Buy -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Buy,
                        cryptoCurrencyData = cryptoData,
                    )
                    is QuickActionUM.V2.Exchange -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Exchange,
                        cryptoCurrencyData = cryptoData,
                    )
                    QuickActionUM.V2.Receive -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Receive,
                        cryptoCurrencyData = cryptoData,
                    )
                    QuickActionUM.V2.Stake -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Stake,
                        cryptoCurrencyData = cryptoData,
                    )
                    is QuickActionUM.V2.YieldMode -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.YieldMode,
                        cryptoCurrencyData = cryptoData,
                    )
                }
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

    fun toQuickActions(actions: List<TokenActionsState.ActionState>, isRedesignEnabled: Boolean) =
        if (isRedesignEnabled) {
            redesignedQuickActions(actions)
        } else {
            legacyQuickActions(actions)
        }

    private fun redesignedQuickActions(actions: List<TokenActionsState.ActionState>): ImmutableList<QuickActionUM> {
        return buildList {
            actions.forEach { action ->
                if (action.unavailabilityReason == ScenarioUnavailabilityReason.None) {
                    when (action) {
                        is TokenActionsState.ActionState.Buy -> QuickActionUM.V2.Buy
                        is TokenActionsState.ActionState.Swap -> QuickActionUM.V2.Exchange(action.shouldShowBadge)
                        is TokenActionsState.ActionState.Receive -> QuickActionUM.V2.Receive
                        is TokenActionsState.ActionState.Stake -> QuickActionUM.V2.Stake
                        is TokenActionsState.ActionState.YieldMode -> QuickActionUM.V2.YieldMode(action.apy)
                        else -> null
                    }?.let(::add)
                }
            }
        }.toImmutableList()
    }

    private fun legacyQuickActions(actions: List<TokenActionsState.ActionState>): ImmutableList<QuickActionUM> {
        return buildList {
            actions.forEach { action ->
                if (action.unavailabilityReason == ScenarioUnavailabilityReason.None) {
                    when (action) {
                        is TokenActionsState.ActionState.Buy -> QuickActionUM.V1.Buy
                        is TokenActionsState.ActionState.Swap -> QuickActionUM.V1.Exchange(action.shouldShowBadge)
                        is TokenActionsState.ActionState.Receive -> QuickActionUM.V1.Receive
                        is TokenActionsState.ActionState.Stake -> QuickActionUM.V1.Stake
                        is TokenActionsState.ActionState.YieldMode -> QuickActionUM.V1.YieldMode(action.apy)
                        else -> null
                    }?.let(::add)
                }
            }
        }.toImmutableList()
    }
}