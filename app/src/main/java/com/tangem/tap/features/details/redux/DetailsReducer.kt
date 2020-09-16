package com.tangem.tap.features.details.redux

import com.tangem.commands.Card
import com.tangem.commands.Settings
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

class DetailsReducer {
    companion object {
        fun reduce(action: Action, state: AppState): DetailsState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, state: AppState): DetailsState {

    if (action !is DetailsAction) return state.detailsState

    var detailsState = state.detailsState
    when (action) {
        is DetailsAction.PrepareScreen -> {
            detailsState = DetailsState(
                    card = action.card, wallet = action.wallet,
                    cardInfo = action.card.toCardInfo(),
                    appCurrencyState = AppCurrencyState(
                            action.fiatCurrencyName
                    )
            )
        }
        is DetailsAction.EraseWallet -> {
            detailsState = handleEraseWallet(action, detailsState)
        }
        is DetailsAction.AppCurrencyAction -> {
            detailsState = handleAppCurrencyAction(action, detailsState)
        }
    }
    return detailsState
}

private fun handleEraseWallet(action: DetailsAction.EraseWallet, state: DetailsState): DetailsState {
    return when (action) {
        DetailsAction.EraseWallet.Check -> {
            val notAllowedByCard = state.card?.settingsMask?.contains(Settings.ProhibitPurgeWallet) == true
            val notEmpty = state.wallet?.transactions?.isNullOrEmpty() != true ||
                    state.wallet.amounts.toList().unzip().second.map { it.value?.isZero() }.contains(false)
            val eraseWalletState = when {
                notAllowedByCard -> EraseWalletState.NotAllowedByCard
                notEmpty -> EraseWalletState.NotEmpty
                else -> EraseWalletState.Allowed
            }
            state.copy(eraseWalletState = eraseWalletState)
        }
        DetailsAction.EraseWallet.Proceed -> {
            if (state.eraseWalletState == EraseWalletState.Allowed) {
                state.copy(confirmScreenState = ConfirmScreenState.EraseWallet)
            } else {
                state
            }
        }
        DetailsAction.EraseWallet.Cancel -> state.copy(eraseWalletState = null)
        DetailsAction.EraseWallet.Failure -> state.copy(eraseWalletState = null)
        DetailsAction.EraseWallet.Success -> state.copy(eraseWalletState = null)
        else -> state
    }
}

private fun handleAppCurrencyAction(
        action: DetailsAction.AppCurrencyAction, state: DetailsState
): DetailsState {
    return when (action) {
        is DetailsAction.AppCurrencyAction.SetCurrencies -> {
            state.copy(appCurrencyState = state.appCurrencyState.copy(fiatCurrencies = action.currencies))
        }
        DetailsAction.AppCurrencyAction.ChooseAppCurrency -> {
            state.copy(appCurrencyState = state.appCurrencyState.copy(showAppCurrencyDialog = true))
        }
        DetailsAction.AppCurrencyAction.Cancel -> {
            state.copy(appCurrencyState = state.appCurrencyState.copy(showAppCurrencyDialog = false))
        }
        is DetailsAction.AppCurrencyAction.SelectAppCurrency -> {
            state.copy(
                    appCurrencyState = state.appCurrencyState.copy(
                            fiatCurrencyName = action.fiatCurrencyName, showAppCurrencyDialog = false
                    )
            )
        }
        else -> state
    }
}

private fun Card.toCardInfo(): CardInfo? {
    val cardId = this.cardId.chunked(4).joinToString(separator = " ")
    val issuer = this.cardData?.issuerName ?: return null
    val signedHashes = this.walletSignedHashes ?: return null
    return CardInfo(cardId, issuer, signedHashes)
}