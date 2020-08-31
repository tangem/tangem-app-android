package com.tangem.tap.features.wallet.redux

import com.tangem.blockchain.common.AmountType
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
                state = ProgressState.Loading, walletManager = action.walletManager,
                currencyData = BalanceWidgetData(
                        BalanceStatus.Loading, action.walletManager.wallet.blockchain.fullName
                )
        )
        is WalletAction.LoadWallet.Success -> {
            val token = action.wallet.amounts[AmountType.Token]
            val tokenData = if (token != null) {
                TokenData(token.value.toString(), token.currencySymbol)
            } else {
                null
            }
            newState = newState.copy(
                    state = ProgressState.Done, wallet = action.wallet,
                    currencyData = BalanceWidgetData(
                            BalanceStatus.VerifiedOnline, action.wallet.blockchain.fullName,
                            action.wallet.amounts[AmountType.Coin]?.value?.toString(),
                            token = tokenData
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

    }
    return newState
}