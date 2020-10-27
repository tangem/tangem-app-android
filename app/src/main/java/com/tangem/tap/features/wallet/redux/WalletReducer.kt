package com.tangem.tap.features.wallet.redux

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Wallet
import com.tangem.commands.common.network.TangemService
import com.tangem.common.extensions.isZero
import com.tangem.common.extensions.toHexString
import com.tangem.tap.common.extensions.toFiatString
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.common.extensions.toFormattedString
import com.tangem.tap.common.extensions.toQrCode
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.wallet.models.removeUnknownTransactions
import com.tangem.tap.features.wallet.models.toPendingTransactions
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.features.wallet.ui.TokenData
import com.tangem.tap.store
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
        is WalletAction.ResetState -> newState = WalletState()
        is WalletAction.EmptyWallet -> newState = newState.copy(
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
                    newState = newState.copy(
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
                    newState = newState.copy(
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
            newState = newState.copy(
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
        is WalletAction.UpdateWallet -> newState = newState.copy(updatingWallet = true)
        is WalletAction.UpdateWallet.ScheduleUpdatingWallet ->
            newState = newState.copy(updatingWallet = true)
        is WalletAction.UpdateWallet.Success -> {
            newState = onWalletLoaded(action.wallet, newState)
            newState = newState.copy(updatingWallet = newState.pendingTransactions.isNotEmpty())
        }
        is WalletAction.UpdateWallet.Failure -> newState = newState.copy(updatingWallet = false)
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
            val cardId = action.card.cardId
            val cardPublicKey = action.card.cardPublicKey?.toHexString()
            val artworkUrl = if (cardPublicKey != null && action.artworkId != null) {
                TangemService.getUrlForArtwork(cardId, cardPublicKey, action.artworkId)
            } else if (action.card.cardId.startsWith(Artwork.SERGIO_CARD_ID)) {
                Artwork.SERGIO_CARD_URL
            } else if (action.card.cardId.startsWith(Artwork.MARTA_CARD_ID)) {
                Artwork.MARTA_CARD_URL
            } else {
                Artwork.DEFAULT_IMG_URL
            }
            newState = newState.copy(cardImage = Artwork(artworkId = artworkUrl))
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
        is WalletAction.HideDialog -> {
            newState = newState.copy(walletDialog = null)
        }
        is WalletAction.LoadPayId.Success -> newState = newState.copy(
                payIdData = PayIdData(PayIdState.Created, action.payId)
        )
        is WalletAction.LoadPayId.NotCreated -> newState = newState.copy(
                payIdData = PayIdData(PayIdState.NotCreated, null)
        )
        is WalletAction.DisablePayId -> newState = newState.copy(
                payIdData = PayIdData(PayIdState.Disabled, null)
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
        is WalletAction.ShowWarning ->
            newState = newState.copy(walletDialog = WalletDialog.WarningDialog(action.warningType))
        is WalletAction.NeedToCheckHashesCountOnline ->
            newState = newState.copy(hashesCountVerified = false)
        is WalletAction.ConfirmHashesCount ->
            newState = newState.copy(hashesCountVerified = true)
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
    val balanceStatus = if (pendingTransactions.isNotEmpty()) {
        BalanceStatus.TransactionInProgress
    } else {
        BalanceStatus.VerifiedOnline
    }
    return walletState.copy(
            state = ProgressState.Done, wallet = wallet,
            currencyData = BalanceWidgetData(
                    balanceStatus, wallet.blockchain.fullName,
                    currencySymbol = wallet.blockchain.currency,
                    formattedAmount,
                    token = tokenData,
                    fiatAmount = fiatAmount
            ),
            pendingTransactions = pendingTransactions.removeUnknownTransactions(),
            mainButton = WalletMainButton.SendButton(sendButtonEnabled)
    )
}