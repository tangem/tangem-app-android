package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import timber.log.Timber

internal class SetCryptoCurrencyActionsTransformer(
    private val tokenActionsState: TokenActionsState,
    private val userWallet: UserWallet,
    private val clickIntents: WalletClickIntentsV2,
) : WalletStateTransformer(userWallet.walletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.SingleCurrency.Content -> {
                prevState.copy(buttons = tokenActionsState.toManageButtons())
            }
            is WalletState.SingleCurrency.Locked -> {
                Timber.w("Impossible to load primary currency status for locked wallet")
                prevState
            }
            is WalletState.MultiCurrency -> {
                Timber.w("Impossible to load crypto currency actions for multi-currency wallet")
                prevState
            }
            is WalletState.Visa -> {
                Timber.w("Impossible to load crypto currency actions for VISA wallet")
                prevState
            }
        }
    }

    private fun TokenActionsState.toManageButtons(): PersistentList<WalletManageButton> {
        return states
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
                            onClick = { clickIntents.onSendClick(cryptoCurrencyStatus) },
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
        return if (userWallet.scanResponse.cardTypesResolver.isStart2Coin()) {
            filterNot { it is TokenActionsState.ActionState.Buy || it is TokenActionsState.ActionState.Sell }
        } else {
            this
        }
    }
}
