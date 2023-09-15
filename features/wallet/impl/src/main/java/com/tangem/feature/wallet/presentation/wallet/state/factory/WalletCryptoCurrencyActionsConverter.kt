package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.common.Provider
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal class WalletCryptoCurrencyActionsConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val clickIntents: WalletClickIntents,
) : Converter<TokenActionsState, WalletState> {

    override fun convert(value: TokenActionsState): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletSingleCurrencyState.Content -> state.copy(buttons = value.mapToManageButtons())
            is WalletSingleCurrencyState.Locked,
            is WalletMultiCurrencyState,
            is WalletState.Initial,
            -> state
        }
    }

    private fun TokenActionsState.mapToManageButtons(): PersistentList<WalletManageButton> {
        return this.states
            .mapNotNull { action ->
                when (action) {
                    is TokenActionsState.ActionState.Buy -> {
                        WalletManageButton.Buy(
                            enabled = action.enabled,
                            onClick = { clickIntents.onBuyClick(cryptoCurrencyStatus) },
                        )
                    }
                    is TokenActionsState.ActionState.Receive -> {
                        WalletManageButton.Receive(
                            onClick = { clickIntents.onReceiveClick(cryptoCurrencyStatus) },
                        )
                    }
                    is TokenActionsState.ActionState.Sell -> {
                        WalletManageButton.Sell(
                            enabled = action.enabled,
                            onClick = { clickIntents.onSellClick(cryptoCurrencyStatus) },
                        )
                    }
                    is TokenActionsState.ActionState.Send -> {
                        WalletManageButton.Send(
                            enabled = action.enabled,
                            onClick = { clickIntents.onSingleCurrencySendClick(cryptoCurrencyStatus) },
                        )
                    }
                    is TokenActionsState.ActionState.Swap -> null
                }
            }
            .toPersistentList()
    }
}
