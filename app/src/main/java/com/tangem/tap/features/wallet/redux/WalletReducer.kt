package com.tangem.tap.features.wallet.redux

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Wallet
import com.tangem.common.extensions.isZero
import com.tangem.common.extensions.toHexString
import com.tangem.tap.common.extensions.toFiatString
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.common.extensions.toFormattedString
import com.tangem.tap.common.extensions.toQrCode
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.wallet.models.toPendingTransactions
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.features.wallet.ui.TokenData
import com.tangem.tap.store
import com.tangem.wallet.R
import org.rekotlin.Action

class WalletReducer {
    companion object {
        fun reduce(action: Action, state: AppState): WalletState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, state: AppState): WalletState {

    if (action !is WalletAction) return state.walletState

    var newState = state.walletState

    when (action) {
        is WalletAction.EmptyWallet -> newState = WalletState(
                state = ProgressState.Done,
                currencyData = BalanceWidgetData(BalanceStatus.EmptyCard),
                mainButton = WalletMainButton.CreateWalletButton(true)
        )
        is WalletAction.LoadData.Failure -> {
            when (action.error) {
                is TapError.NoInternetConnection -> {
                    val wallet = state.globalState.scanNoteResponse?.walletManager?.wallet
                    val addressData = if (wallet == null) {
                        null
                    } else {
                        AddressData(wallet.address, wallet.shareUrl, wallet.exploreUrl)
                    }
                    newState = WalletState(
                            state = ProgressState.Error,
                            error = ErrorType.NoInternetConnection,
                            addressData = addressData,
                            currencyData = BalanceWidgetData(
                                    status = BalanceStatus.Unreachable,
                                    currency = wallet?.blockchain?.fullName),
                            mainButton = WalletMainButton.SendButton(false)
                    )
                }
                is TapError.UnknownBlockchain -> {
                    newState = WalletState(
                            state = ProgressState.Done,
                            currencyData = BalanceWidgetData(BalanceStatus.UnknownBlockchain)
                    )
                }
            }

        }
        is WalletAction.LoadWallet -> {
            val wallet = state.globalState.scanNoteResponse?.walletManager?.wallet
            val addressData = if (wallet == null) {
                null
            } else {
                AddressData(wallet.address, wallet.shareUrl, wallet.exploreUrl)
            }
            val currentArtworkId = state.globalState.scanNoteResponse?.verifyResponse?.artworkInfo?.id
            val cardImage = if (newState.cardImage?.artworkId == currentArtworkId) {
                newState.cardImage
            } else {
                null
            }
            newState = WalletState(
                    state = ProgressState.Loading,
                    cardImage = cardImage,
                    currencyData = BalanceWidgetData(
                            BalanceStatus.Loading,
                            wallet?.blockchain?.fullName
                    ),
                    addressData = addressData,
                    mainButton = WalletMainButton.SendButton(false)

            )
        }
        is WalletAction.LoadWallet.Success -> newState = onWalletLoaded(action.wallet, newState)

        is WalletAction.LoadWallet.NoAccount -> {
            val wallet = state.globalState.scanNoteResponse?.walletManager?.wallet
            newState = newState.copy(
                    state = ProgressState.Done, wallet = wallet,
                    currencyData = BalanceWidgetData(
                            BalanceStatus.NoAccount, wallet?.blockchain?.fullName,
                            currencySymbol = wallet?.blockchain?.currency,
                            amountToCreateAccount = action.amountToCreateAccount
                    )
            )
        }
        is WalletAction.LoadWallet.Failure -> newState = newState.copy(
                state = ProgressState.Done,
                currencyData = newState.currencyData.copy(
                        status = BalanceStatus.Unreachable,
                        errorMessage = action.errorMessage
                )
        )
        is WalletAction.UpdateWallet.Success -> newState = onWalletLoaded(action.wallet, newState)
        is WalletAction.LoadFiatRate -> {
            newState.copy(currencyData = newState.currencyData.copy(
                    fiatAmount = null,
                    token = newState.currencyData.token?.copy(fiatAmount = null))
            )
        }
        is WalletAction.LoadFiatRate.Success -> {
            val rate = action.fiatRates.second ?: return newState
            val currency = action.fiatRates.first
            val fiatAmount = if (currency == newState.wallet?.blockchain?.currency) {
                newState.wallet?.amounts?.get(AmountType.Coin)?.value
                        ?.toFiatString(rate, state.globalState.appCurrency)
            } else {
                newState.currencyData.fiatAmount
            }
            val tokenFiatAmount = if (currency == newState.wallet?.token?.symbol) {
                newState.wallet?.amounts?.get(AmountType.Token)?.value
                        ?.toFiatString(rate, state.globalState.appCurrency)
            } else {
                newState.currencyData.token?.fiatAmount
            }
            newState = newState.copy(currencyData = newState.currencyData.copy(
                    fiatAmount = fiatAmount,
                    token = newState.currencyData.token?.copy(fiatAmount = tokenFiatAmount)
            ))
        }
        is WalletAction.LoadArtwork -> {
// [REDACTED_TODO_COMMENT]
            var artworkAddress = "https://verify.tangem.com/card/artwork"
            artworkAddress += "?artworkId=${action.artworkId}"
            artworkAddress += "&CID=${store.state.globalState.scanNoteResponse?.card?.cardId}"
            artworkAddress += "&publicKey=${store.state.globalState.scanNoteResponse?.card?.cardPublicKey?.toHexString()}"
            val artwork = if (action.artworkId != null) {
                Artwork(artworkId = artworkAddress)
            } else {
                Artwork(artworkResId = R.drawable.card_default)
            }
            newState = newState.copy(cardImage = artwork)
        }
        is WalletAction.ShowQrCode -> {
            newState = newState.copy(
                    walletDialog = WalletDialog.QrDialog(
                            newState.addressData?.shareUrl?.toQrCode(),
                            newState.addressData?.shareUrl,
                            newState.currencyData.currency
                    )
            )
        }
        is WalletAction.HideQrCode -> {
            newState = newState.copy(walletDialog = null)
        }
        is WalletAction.LoadPayId.Success -> newState = newState.copy(
                payIdData = PayIdData(PayIdState.Created, action.payId)
        )
        is WalletAction.LoadPayId.NotCreated -> newState = newState.copy(
                payIdData = PayIdData(PayIdState.NotCreated, null)
        )
        is WalletAction.CreatePayId, is WalletAction.CreatePayId.Failure ->
            newState = newState.copy(
                    walletDialog = WalletDialog.CreatePayIdDialog(CreatingPayIdState.EnterPayId)
            )
        is WalletAction.CreatePayId.CompleteCreatingPayId -> newState = newState.copy(
                walletDialog = WalletDialog.CreatePayIdDialog(CreatingPayIdState.Waiting)
        )
        is WalletAction.CreatePayId.Success -> newState = newState.copy(
                payIdData = PayIdData(PayIdState.Created, action.payId),
                walletDialog = null
        )
        is WalletAction.CreatePayId.Cancel -> newState = newState.copy(walletDialog = null)
        is WalletAction.Send.ChooseCurrency -> {
            newState = newState.copy(
                    walletDialog = WalletDialog.SelectAmountToSendDialog(action.amounts)
            )
        }
        is WalletAction.Send.Cancel -> newState = newState.copy(walletDialog = null)

    }
    return newState
}

private fun onWalletLoaded(wallet: Wallet, walletState: WalletState): WalletState {
    val fiatCurrencySymbol = store.state.globalState.appCurrency
    val token = wallet.amounts[AmountType.Token]
    val tokenData = if (token != null) {
        val tokenFiatRate = store.state.globalState.conversionRates.getRate(token.currencySymbol)
        val tokenFiatAmount = tokenFiatRate?.let { token.value?.toFiatString(it, fiatCurrencySymbol) }
        TokenData(
                token.value?.toFormattedString(token.decimals) ?: "",
                token.currencySymbol, tokenFiatAmount)
    } else {
        null
    }
    val amount = wallet.amounts[AmountType.Coin]?.value
    val formattedAmount = amount?.toFormattedCurrencyString(
            wallet.blockchain.decimals(),
            wallet.blockchain.currency)
    val fiatRate = store.state.globalState.conversionRates.getRate(wallet.blockchain.currency)
    val fiatAmount = fiatRate?.let { amount?.toFiatString(it, fiatCurrencySymbol) }

    val pendingTransactions = wallet.recentTransactions
            .toPendingTransactions(wallet.address)

    val sendButtonEnabled = amount?.isZero() == false && pendingTransactions.isEmpty()
    return walletState.copy(
            state = ProgressState.Done, wallet = wallet,
            currencyData = BalanceWidgetData(
                    BalanceStatus.VerifiedOnline, wallet.blockchain.fullName,
                    currencySymbol = wallet.blockchain.currency,
                    formattedAmount,
                    token = tokenData,
                    fiatAmount = fiatAmount
            ),
            pendingTransactions = pendingTransactions,
            mainButton = WalletMainButton.SendButton(sendButtonEnabled)
    )
}