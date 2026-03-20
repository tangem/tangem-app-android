package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import com.tangem.utils.logging.TangemLogger

internal class SetCryptoCurrencyActionsTransformer(
    private val tokenActionsState: TokenActionsState,
    private val userWallet: UserWallet,
    private val accountId: AccountId,
    private val clickIntents: WalletClickIntents,
) : WalletStateTransformer(userWallet.walletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.SingleCurrency.Content -> {
                prevState.copy(buttons = tokenActionsState.toManageButtons())
            }
            is WalletState.SingleCurrency.Locked -> {
                TangemLogger.w("Impossible to load primary currency status for locked wallet")
                prevState
            }
            is WalletState.MultiCurrency -> {
                TangemLogger.w("Impossible to load crypto currency actions for multi-currency wallet")
                prevState
            }
        }
    }

    override fun transform(walletUM: WalletUM): WalletUM {
        return walletUM // todo redesign main
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
                                    accountId = accountId,
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
                                clickIntents.onReceiveClick(
                                    accountId,
                                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                                )
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
                                    accountId = accountId,
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