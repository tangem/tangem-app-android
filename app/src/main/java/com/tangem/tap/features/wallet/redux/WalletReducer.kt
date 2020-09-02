package com.tangem.tap.features.wallet.redux

import com.tangem.blockchain.common.AmountType
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.toFiatString
import com.tangem.tap.common.extensions.toFormattedString
import com.tangem.tap.common.extensions.toQrCode
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.features.wallet.ui.TokenData
import org.rekotlin.Action

fun walletReducer(action: Action, state: AppState): WalletState {

    if (action !is WalletAction) return state.walletState

    var newState = state.walletState

    when (action) {
        is WalletAction.EmptyWallet -> newState = WalletState(
                state = ProgressState.Done,
                currencyData = BalanceWidgetData(BalanceStatus.EmptyCard),
                mainButton = WalletMainButton.CreateWalletButton(true)
        )
        is WalletAction.LoadWallet -> newState = WalletState(
                state = ProgressState.Loading,
                currencyData = BalanceWidgetData(
                        BalanceStatus.Loading,
                        state.globalState.walletManager?.wallet?.blockchain?.fullName
                ),
                mainButton = WalletMainButton.SendButton(false)

        )
        is WalletAction.LoadWallet.Success -> {
            val token = action.wallet.amounts[AmountType.Token]
            val tokenData = if (token != null) {
                val tokenFiatRate = state.globalState.fiatRates.getRateForCryptoCurrency(token.currencySymbol)
                val tokenFiatAmount = tokenFiatRate?.let { token.value?.toFiatString(it) }
                TokenData(
                        token.value?.toFormattedString(action.wallet.blockchain) ?: "",
                        token.currencySymbol, tokenFiatAmount)
            } else {
                null
            }
            val amount = action.wallet.amounts[AmountType.Coin]?.value
            val fiatRate = state.globalState.fiatRates.getRateForCryptoCurrency(action.wallet.blockchain.currency)
            val fiatAmount = fiatRate?.let { amount?.toFiatString(it) }

            val sendButtonEnabled = amount?.isZero() == false || token?.value?.isZero() == false
            newState = newState.copy(
                    state = ProgressState.Done, wallet = action.wallet,
                    currencyData = BalanceWidgetData(
                            BalanceStatus.VerifiedOnline, action.wallet.blockchain.fullName,
                            amount?.toFormattedString(action.wallet.blockchain),
                            token = tokenData,
                            fiatAmount = fiatAmount
                    ),
                    mainButton = WalletMainButton.SendButton(sendButtonEnabled)
            )
        }
        is WalletAction.LoadFiatRate.Success -> {
            val rate = action.fiatRates.second
            val currency = action.fiatRates.first
            val fiatAmount = if (currency == newState.wallet?.blockchain?.currency) {
                newState.wallet?.amounts?.get(AmountType.Coin)?.value?.toFiatString(rate)
            } else {
                null
            }
            val tokenFiatAmount = if (currency == newState.wallet?.token?.symbol) {
                newState.wallet?.amounts?.get(AmountType.Token)?.value?.toFiatString(rate)
            } else {
                null
            }
            newState = newState.copy(currencyData = newState.currencyData.copy(
                    fiatAmount = fiatAmount,
                    token = newState.currencyData.token?.copy(fiatAmount = tokenFiatAmount)
            ))
        }
        is WalletAction.LoadWallet.Failure -> newState = newState.copy(
                state = ProgressState.Done,
                currencyData = newState.currencyData.copy(status = BalanceStatus.Unreachable)
        )
        is WalletAction.ShowQrCode -> {
            newState = newState.copy(qrCode = newState.wallet?.shareUrl?.toQrCode())
        }
        is WalletAction.HideQrCode -> {
            newState = newState.copy(qrCode = null)
        }
        is WalletAction.LoadPayId.Success -> newState = newState.copy(
                payIdData = PayIdData(PayIdState.Created, action.payId)
        )
        is WalletAction.LoadPayId.NotCreated -> newState = newState.copy(
                payIdData = PayIdData(PayIdState.NotCreated, null)
        )
        is WalletAction.CreatePayId, is WalletAction.CreatePayId.Failure ->
            newState = newState.copy(creatingPayIdState = CreatingPayIdState.EnterPayId)
        is WalletAction.CreatePayId.CompleteCreatingPayId -> newState = newState.copy(
                creatingPayIdState = CreatingPayIdState.Waiting
        )
        is WalletAction.CreatePayId.Success -> newState = newState.copy(
                payIdData = PayIdData(PayIdState.Created, action.payId),
                creatingPayIdState = null
        )
        is WalletAction.CreatePayId.Cancel -> newState = newState.copy(
                creatingPayIdState = null
        )
    }
    return newState
}