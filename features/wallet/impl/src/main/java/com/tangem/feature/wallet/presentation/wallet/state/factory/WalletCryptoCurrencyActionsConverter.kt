package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal class WalletCryptoCurrencyActionsConverter(
    private val currentWalletProvider: Provider<UserWallet>,
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
            .filterIfS2C()
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
                            enabled = action.enabled,
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
                    else -> {
                        null
                    }
                }
            }
            .toPersistentList()
    }

    private fun List<TokenActionsState.ActionState>.filterIfS2C(): List<TokenActionsState.ActionState> {
        return if (currentWalletProvider().scanResponse.cardTypesResolver.isStart2Coin()) {
            filterNot { it is TokenActionsState.ActionState.Buy || it is TokenActionsState.ActionState.Sell }
        } else {
            this
        }
    }
}