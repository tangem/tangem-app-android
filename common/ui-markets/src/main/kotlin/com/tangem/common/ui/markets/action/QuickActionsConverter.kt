package com.tangem.common.ui.markets.action

import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import kotlinx.collections.immutable.toImmutableList

object QuickActionsConverter {

    fun quickActions(cryptoData: CryptoCurrencyData, tokenActionsHandler: TokenActionsHandler): QuickActions {
        return QuickActions(
            actions = toQuickActions(cryptoData.actions),
            onQuickActionClick = { quickActionUM ->
                when (quickActionUM) {
                    QuickActionUM.Buy -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Buy,
                        cryptoCurrencyData = cryptoData,
                    )
                    is QuickActionUM.Exchange -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Exchange,
                        cryptoCurrencyData = cryptoData,
                    )
                    QuickActionUM.Receive -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Receive,
                        cryptoCurrencyData = cryptoData,
                    )
                    QuickActionUM.Stake -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.Stake,
                        cryptoCurrencyData = cryptoData,
                    )
                    is QuickActionUM.YieldMode -> tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.YieldMode,
                        cryptoCurrencyData = cryptoData,
                    )
                }
            },
            onQuickActionLongClick = { actionUM ->
                if (actionUM == QuickActionUM.Receive) {
                    tokenActionsHandler.handle(
                        action = TokenActionsBSContentUM.Action.CopyAddress,
                        cryptoCurrencyData = cryptoData,
                    )
                }
            },
        )
    }

    fun toQuickActions(actions: List<TokenActionsState.ActionState>) = buildList {
        actions.forEach { action ->
            if (action.unavailabilityReason == ScenarioUnavailabilityReason.None) {
                when (action) {
                    is TokenActionsState.ActionState.Buy -> QuickActionUM.Buy
                    is TokenActionsState.ActionState.Swap -> QuickActionUM.Exchange(action.shouldShowBadge)
                    is TokenActionsState.ActionState.Receive -> QuickActionUM.Receive
                    is TokenActionsState.ActionState.Stake -> QuickActionUM.Stake
                    is TokenActionsState.ActionState.YieldMode -> QuickActionUM.YieldMode(action.apy)
                    else -> null
                }?.let(::add)
            }
        }
    }.toImmutableList()
}