package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import timber.log.Timber

internal class SetCryptoCurrencyActionsTransformer(
    private val tokenActionsState: TokenActionsState,
    private val userWallet: UserWallet,
    private val clickIntents: WalletClickIntents,
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
                            enabled = true,
                            dimContent = action.unavailabilityReason != ScenarioUnavailabilityReason.None,
                            onClick = {
                                clickIntents.onBuyClick(
                                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                                    unavailabilityReason = action.unavailabilityReason,
                                )
                            },
                        )
                    }
                    is TokenActionsState.ActionState.Receive -> {
                        WalletManageButton.Receive(
                            enabled = true,
                            dimContent = action.unavailabilityReason != ScenarioUnavailabilityReason.None,
                            onClick = {
                                clickIntents.onReceiveClick(cryptoCurrencyStatus = cryptoCurrencyStatus)
                            },
                            onLongClick = {
                                clickIntents.onCopyAddressLongClick(cryptoCurrencyStatus = cryptoCurrencyStatus)
                            },
                        )
                    }
                    is TokenActionsState.ActionState.Sell -> {
                        WalletManageButton.Sell(
                            enabled = true,
                            dimContent = action.unavailabilityReason != ScenarioUnavailabilityReason.None,
                            onClick = {
                                clickIntents.onSellClick(
                                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                                    unavailabilityReason = action.unavailabilityReason,
                                )
                            },
                        )
                    }
                    is TokenActionsState.ActionState.Send -> {
                        WalletManageButton.Send(
                            enabled = true,
                            dimContent = action.unavailabilityReason != ScenarioUnavailabilityReason.None,
                            onClick = {
                                clickIntents.onSendClick(
                                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                                    unavailabilityReason = action.unavailabilityReason,
                                )
                            },
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
        return if (userWallet is UserWallet.Cold && userWallet.scanResponse.cardTypesResolver.isStart2Coin()) {
            filterNot { it is TokenActionsState.ActionState.Buy || it is TokenActionsState.ActionState.Sell }
        } else {
            this
        }
    }
}