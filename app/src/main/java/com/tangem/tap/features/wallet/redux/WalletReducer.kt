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
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.domain.twins.TwinCardNumber
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
        is WalletAction.EmptyWallet -> {
            val creatingWalletAllowed = !(newState.twinCardsState != null &&
                    newState.twinCardsState?.isCreatingTwinCardsAllowed != true)

            newState = newState.copy(
                    state = ProgressState.Done,
                    currencyData = BalanceWidgetData(BalanceStatus.EmptyCard),
                    mainButton = WalletMainButton.CreateWalletButton(creatingWalletAllowed),
                    topUpState = TopUpState(false)
            )
        }
        is WalletAction.LoadData.Failure -> {
            when (action.error) {
                is TapError.NoInternetConnection -> {
                    val wallet = state.globalState.scanNoteResponse?.walletManager?.wallet
                    newState = newState.copy(
                            state = ProgressState.Error,
                            error = ErrorType.NoInternetConnection,
                            walletAddresses = createAddressList(wallet, newState.walletAddresses),
                            currencyData = BalanceWidgetData(
                                    status = BalanceStatus.Unreachable,
                                    currency = wallet?.blockchain?.fullName,
                                    token = wallet?.getFirstToken()?.symbol?.let {
                                        TokenData("", tokenSymbol = it)
                                    }),
                            mainButton = WalletMainButton.SendButton(false),
                            topUpState = TopUpState(false)
                    )
                }
                is TapError.UnknownBlockchain -> {
                    newState = newState.copy(
                            state = ProgressState.Done,
                            currencyData = BalanceWidgetData(BalanceStatus.UnknownBlockchain),
                            topUpState = TopUpState(false)
                    )
                }
            }

        }
        is WalletAction.LoadWallet -> {
            val wallet = action.wallet
            val cardImage = if (newState.cardImage?.artworkId == action.artworkId) {
                newState.cardImage
            } else {
                null
            }
            newState = newState.copy(
                    state = ProgressState.Loading,
                    cardImage = cardImage,
                    currencyData = BalanceWidgetData(
                            BalanceStatus.Loading,
                            wallet.blockchain.fullName,
                            currencySymbol = wallet.blockchain.currency,
                            token = wallet.getFirstToken()?.symbol?.let {
                                TokenData("", tokenSymbol = it)
                            }
                    ),
                    walletAddresses = createAddressList(wallet, newState.walletAddresses),
                    mainButton = WalletMainButton.SendButton(false),
                    topUpState = TopUpState(allowed = action.allowTopUp)
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
                ),
                topUpState = TopUpState(false)
        )
        is WalletAction.UpdateWallet -> {
            if (store.state.walletState.state == ProgressState.Done) {
                newState = newState.copy(updatingWallet = true)
            }
        }
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
            val token = newState.wallet?.getFirstToken()
            val tokenFiatAmount = if (currency == token?.symbol) {
                newState.wallet?.getTokenAmount(token)?.value?.toFiatString(rate, state.globalState.appCurrency)
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
            } else if (newState.twinCardsState?.cardNumber != null) {
                when (newState.twinCardsState?.cardNumber) {
                    TwinCardNumber.First -> Artwork.TWIN_CARD_1
                    TwinCardNumber.Second -> Artwork.TWIN_CARD_2
                    null -> Artwork.DEFAULT_IMG_URL
                }
            } else {
                Artwork.DEFAULT_IMG_URL
            }
            newState = newState.copy(cardImage = Artwork(artworkId = artworkUrl))
        }
        is WalletAction.ShowDialog.QrCode -> {
            newState = newState.copy(
                    walletDialog = WalletDialog.QrDialog(
                            newState.walletAddresses?.selectedAddress?.shareUrl?.toQrCode(),
                            newState.walletAddresses?.selectedAddress?.shareUrl,
                            newState.currencyData.currency
                    )
            )
        }
        is WalletAction.ShowDialog.ScanFails -> {
            newState = newState.copy(walletDialog = WalletDialog.ScanFailsDialog)
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
        is WalletAction.Warnings.SetWarnings -> newState = newState.copy(mainWarningsList = action.warningList)
        is WalletAction.NeedToCheckHashesCountOnline ->
            newState = newState.copy(hashesCountVerified = false)
        is WalletAction.ConfirmHashesCount ->
            newState = newState.copy(hashesCountVerified = true)
        is WalletAction.TopUpAction -> {
            newState = newState.copy(topUpState = handleTopUpActions(action, newState.topUpState))
        }
        is WalletAction.ChangeSelectedAddress -> {
            val walletAddresses = newState.walletAddresses ?: return newState
            val address = walletAddresses.list.firstOrNull { it.type == action.type }
                    ?: return newState

            newState = newState.copy(walletAddresses = WalletAddresses(address, walletAddresses.list))
        }
        is WalletAction.TwinsAction.SetTwinCard -> {
            newState = newState.copy(
                    twinCardsState = TwinCardsState(
                            secondCardId = action.secondCardId,
                            cardNumber = action.number,
                            showTwinOnboarding = newState.twinCardsState?.showTwinOnboarding
                                    ?: false,
                            isCreatingTwinCardsAllowed = action.isCreatingTwinCardsAllowed
                    )
            )
        }
        is WalletAction.TwinsAction.ShowOnboarding -> {
            newState = newState.copy(
                    twinCardsState = newState.twinCardsState?.copy(showTwinOnboarding = true)
                            ?: TwinCardsState(null, null,
                                    showTwinOnboarding = true,
                                    isCreatingTwinCardsAllowed = false)
            )
        }
        is WalletAction.TwinsAction.SetOnboardingShown -> {
            newState = newState.copy(
                    twinCardsState = newState.twinCardsState?.copy(showTwinOnboarding = false)
            )
        }
        is WalletAction.ScanCardFinished -> {
            newState = if (action.scanError == null) {
                newState.copy(scanCardFailsCounter = 0)
            } else {
                newState.copy(scanCardFailsCounter = newState.scanCardFailsCounter + 1)
            }
        }
    }
    return newState
}

fun createAddressList(wallet: Wallet?, walletAddresses: WalletAddresses? = null): WalletAddresses? {
    if (wallet == null) return null

    val listOfAddressData = mutableListOf<AddressData>()
    // put a defaultAddress at the first place
    wallet.addresses.forEach {
        val addressData = AddressData(it.value, it.type, wallet.getShareUri(it.value), wallet.getExploreUrl(it.value))
        if (it.type == wallet.blockchain.defaultAddressType()) {
            listOfAddressData.add(0, addressData)
        } else {
            listOfAddressData.add(addressData)
        }
    }

    // restore a selected wallet address
    var indexOfSelectedWallet = 0
    walletAddresses?.let {
        val index = listOfAddressData.indexOfFirst { it.address == walletAddresses.selectedAddress.address }
        if (index != -1) indexOfSelectedWallet = index
    }
    return WalletAddresses(listOfAddressData[indexOfSelectedWallet], listOfAddressData)
}

private fun handleTopUpActions(action: WalletAction.TopUpAction, state: TopUpState): TopUpState {
    return when (action) {
        is WalletAction.TopUpAction.TopUp -> state
    }
}

private fun onWalletLoaded(
        wallet: Wallet, walletState: WalletState, topUpAllowed: Boolean? = null,
): WalletState {
    val fiatCurrencySymbol = store.state.globalState.appCurrency
    val token = wallet.getFirstToken()
    val tokenData = if (token != null) {
        val tokenAmount = wallet.getTokenAmount(token)
        if (tokenAmount != null) {
            val tokenFiatRate = store.state.globalState.conversionRates.getRate(tokenAmount.currencySymbol)
            val tokenFiatAmount = tokenFiatRate?.let { tokenAmount.value?.toFiatString(it, fiatCurrencySymbol) }
            TokenData(tokenAmount.value?.toFormattedString(tokenAmount.decimals) ?: "",
                    tokenAmount.currencySymbol, tokenFiatAmount)
        } else {
            null
        }
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