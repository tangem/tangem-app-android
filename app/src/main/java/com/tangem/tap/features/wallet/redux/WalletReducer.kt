package com.tangem.tap.features.wallet.redux

import com.tangem.blockchain.common.AmountType
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
        is WalletAction.LoadWallet -> newState = WalletState(
                state = ProgressState.Loading,
                currencyData = BalanceWidgetData(
                        BalanceStatus.Loading,
                        state.globalState.walletManager?.wallet?.blockchain?.fullName
                )
        )
        is WalletAction.LoadWallet.Success -> {
            val token = action.wallet.amounts[AmountType.Token]
            val tokenData = if (token != null) {
                TokenData(
                        token.value?.toFormattedString(action.wallet.blockchain) ?: "",
                        token.currencySymbol)
            } else {
                null
            }
            val amount = action.wallet.amounts[AmountType.Coin]?.value
            newState = newState.copy(
                    state = ProgressState.Done, wallet = action.wallet,
                    currencyData = BalanceWidgetData(
                            BalanceStatus.VerifiedOnline, action.wallet.blockchain.fullName,
                            amount?.toFormattedString(action.wallet.blockchain),
                            token = tokenData,
                    )
            )
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