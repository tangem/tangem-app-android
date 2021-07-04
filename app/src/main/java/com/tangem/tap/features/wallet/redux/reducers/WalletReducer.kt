package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Wallet
import com.tangem.commands.common.network.TangemService
import com.tangem.common.extensions.toHexString
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.tap.features.wallet.redux.*
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import org.rekotlin.Action
import java.math.BigDecimal
import java.math.RoundingMode

class WalletReducer {
    companion object {
        fun reduce(action: Action, state: AppState): WalletState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, state: AppState): WalletState {

    val multiWalletReducer = MultiWalletReducer()
    val twinsReducer = TwinsReducer()
    val onWalletLoadedReducer = OnWalletLoadedReducer()

    if (action !is WalletAction) return state.walletState

    var newState = state.walletState

    when (action) {
        is WalletAction.Warnings -> newState = handleCheckSignedHashesActions(action, newState)
        is WalletAction.TwinsAction -> newState = twinsReducer.reduce(action, newState)
        is WalletAction.MultiWallet -> newState = multiWalletReducer.reduce(action, newState)

        is WalletAction.ResetState -> newState = WalletState()
        is WalletAction.SetIfTestnetCard -> newState = newState.copy(isTestnet = action.isTestnet)
        is WalletAction.EmptyWallet -> {
            val creatingWalletAllowed = !(newState.twinCardsState != null &&
                    newState.twinCardsState?.isCreatingTwinCardsAllowed != true)

            newState = newState.copy(
                state = ProgressState.Done,
                wallets = listOf(
                    WalletData(
                        currencyData = BalanceWidgetData(BalanceStatus.EmptyCard),
                        mainButton = WalletMainButton.CreateWalletButton(creatingWalletAllowed),
                        topUpState = TopUpState(false)
                    )
                )
            )
        }
        is WalletAction.LoadData.Failure -> {
            when (action.error) {
                is TapError.NoInternetConnection -> {
                    val wallets = newState.wallets
                        .map {
                            it.copy(
                                currencyData = it.currencyData.copy(
                                    status = BalanceStatus.Unreachable
                                )
                            )
                        }
                    newState = newState.copy(
                        state = ProgressState.Error,
                        error = ErrorType.NoInternetConnection,
                        wallets = wallets
                    )
                }
                is TapError.UnknownBlockchain -> {
                    newState = newState.copy(
                        state = ProgressState.Done,
                        wallets = listOf(
                            WalletData(
                                currencyData = BalanceWidgetData(BalanceStatus.UnknownBlockchain),
                                topUpState = TopUpState(false)
                            )
                        )
                    )
                }
            }
        }

        is WalletAction.LoadData -> {
            newState = newState.copy(
                state = ProgressState.Loading,
                error = null,
            )
        }
        is WalletAction.LoadWallet -> {
            if (action.blockchain == null) {
                val wallets = newState.wallets.map { wallet ->
                    wallet.copy(
                        currencyData = wallet.currencyData.copy(
                            status = BalanceStatus.Loading,
                            currency = wallet.currencyData.currency,
                            currencySymbol = wallet.currencyData.currencySymbol,
                        ),
                        mainButton = WalletMainButton.SendButton(false),
                        topUpState = TopUpState(
                            allowed = action.allowTopUp
                                ?: wallet.topUpState.allowed
                        )
                    )
                }
                newState = newState.copy(
                    state = ProgressState.Loading,
                    wallets = wallets
                )
            } else {
                val walletManager = newState.getWalletManager(action.blockchain) ?: return newState
                val blockchain = walletManager.wallet.blockchain
                val currencies = listOf(Currency.Blockchain(blockchain)) +
                        walletManager.cardTokens.map { Currency.Token(it) }
                val newWallets = newState.wallets.filter { currencies.contains(it.currency) }
                    .map { wallet ->
                        wallet.copy(
                            currencyData = wallet.currencyData.copy(
                                status = BalanceStatus.Loading,
                                currency = wallet.currencyData.currency,
                                currencySymbol = wallet.currencyData.currencySymbol,
                            ),
                            mainButton = WalletMainButton.SendButton(false),
                            topUpState = TopUpState(
                                allowed = action.allowTopUp ?: wallet.topUpState.allowed
                            )
                        )
                    }
                val wallets = newState.replaceSomeWallets(newWallets)
                newState = newState.copy(wallets = wallets)
            }
        }
        is WalletAction.LoadWallet.Success -> newState =
            onWalletLoadedReducer.reduce(action.wallet, newState)
        is WalletAction.UpdateWallet.Success -> {
            newState = onWalletLoadedReducer.reduce(action.wallet, newState)
        }
        is WalletAction.LoadWallet.NoAccount -> {
            val walletData = newState.getWalletData(action.wallet.blockchain)?.copy(
                currencyData = BalanceWidgetData(
                    BalanceStatus.NoAccount, action.wallet.blockchain.fullName,
                    currencySymbol = action.wallet.blockchain.currency,
                    amountToCreateAccount = action.amountToCreateAccount
                )
            )
            val wallets = newState.replaceWalletInWallets(walletData)
            val progressState =
                if (wallets.any { it.currencyData.status == BalanceStatus.Loading }) {
                    ProgressState.Loading
                } else {
                    ProgressState.Done
                }
            newState = newState.copy(
                state = progressState,
                wallets = wallets
            )
        }
        is WalletAction.LoadWallet.Failure -> {
            val message = if (newState.error == ErrorType.NoInternetConnection) {
                null
            } else {
                action.errorMessage
            }
            val walletData = newState.getWalletData(action.wallet.blockchain)
            val newWalletData = walletData?.copy(
                currencyData = walletData.currencyData.copy(
                    status = BalanceStatus.Unreachable,
                    errorMessage = message
                ),
                topUpState = TopUpState(false)
            )
            val tokenWallets = action.wallet.getTokens()
                .mapNotNull { newState.getWalletData(it) }
                .map {
                    it.copy(
                        currencyData = it.currencyData.copy(
                            status = BalanceStatus.Unreachable, errorMessage = message
                        )
                    )
                }
            val wallets = newState.replaceSomeWallets(listOfNotNull(newWalletData) + tokenWallets)

            val progressState =
                if (wallets.any { it.currencyData.status == BalanceStatus.Loading }) {
                    ProgressState.Loading
                } else {
                    ProgressState.Done
                }
            newState = newState.copy(
                state = progressState, wallets = wallets
            )
        }
        is WalletAction.SetArtworkId -> {
            val cardImage = if (newState.cardImage?.artworkId == action.artworkId) {
                newState.cardImage
            } else {
                null
            }
            newState = newState.copy(cardImage = cardImage)
        }

        is WalletAction.LoadFiatRate.Success ->
            newState = setNewFiatRate(action.fiatRate, state.globalState.appCurrency, newState)
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
            val selectedWalletData = newState.getWalletData(newState.selectedWallet)
            newState = newState.copy(
                walletDialog = WalletDialog.QrDialog(
                    selectedWalletData?.walletAddresses?.selectedAddress?.shareUrl?.toQrCode(),
                    selectedWalletData?.walletAddresses?.selectedAddress?.shareUrl,
                    selectedWalletData?.currencyData?.currency
                )
            )
        }
        is WalletAction.ShowDialog.ScanFails -> {
            newState = newState.copy(walletDialog = WalletDialog.ScanFailsDialog)
        }
        is WalletAction.ShowDialog.SignedHashesMultiWalletDialog -> {
            newState = newState.copy(walletDialog = WalletDialog.SignedHashesMultiWalletDialog)
        }
        is WalletAction.HideDialog -> {
            newState = newState.copy(walletDialog = null)
        }
        is WalletAction.Send.ChooseCurrency -> {
            newState = newState.copy(
                walletDialog = WalletDialog.SelectAmountToSendDialog(action.amounts)
            )
        }
        is WalletAction.Send.Cancel -> newState = newState.copy(walletDialog = null)
        is WalletAction.TopUpAction -> return newState
        is WalletAction.ChangeSelectedAddress -> {
            val selectedWalletData = newState.getWalletData(newState.selectedWallet)

            val walletAddresses =
                newState.getWalletData(selectedWalletData?.currency)?.walletAddresses
                    ?: return newState
            val address = walletAddresses.list.firstOrNull { it.type == action.type }
                ?: return newState
            val wallets = newState.replaceWalletInWallets(
                selectedWalletData?.copy(
                    walletAddresses = WalletAddresses(
                        address,
                        walletAddresses.list
                    )
                )
            )
            newState = newState.copy(wallets = wallets)
        }
    }
    return newState
}

fun createAddressList(wallet: Wallet?, walletAddresses: WalletAddresses? = null): WalletAddresses? {
    if (wallet == null) return null

    val listOfAddressData = mutableListOf<AddressData>()
    // put a defaultAddress at the first place
    wallet.addresses.forEach {
        val addressData = AddressData(
            it.value,
            it.type,
            wallet.getShareUri(it.value),
            wallet.getExploreUrl(it.value)
        )
        if (it.type == wallet.blockchain.defaultAddressType()) {
            listOfAddressData.add(0, addressData)
        } else {
            listOfAddressData.add(addressData)
        }
    }

    // restore a selected wallet address
    var indexOfSelectedWallet = 0
    walletAddresses?.let {
        val index =
            listOfAddressData.indexOfFirst { it.address == walletAddresses.selectedAddress.address }
        if (index != -1) indexOfSelectedWallet = index
    }
    return WalletAddresses(listOfAddressData[indexOfSelectedWallet], listOfAddressData)
}

private fun handleCheckSignedHashesActions(
    action: WalletAction.Warnings,
    state: WalletState
): WalletState {
    return when (action) {
        WalletAction.Warnings.CheckHashesCount.ConfirmHashesCount -> state.copy(hashesCountVerified = true)
        WalletAction.Warnings.CheckHashesCount.NeedToCheckHashesCountOnline -> state.copy(
            hashesCountVerified = false
        )
        is WalletAction.Warnings.Set -> state.copy(mainWarningsList = action.warningList)
        else -> state
    }
}


private fun setNewFiatRate(
    fiatRate: Pair<Currency, BigDecimal?>,
    appCurrency: FiatCurrencyName, state: WalletState
): WalletState {
    val rate = fiatRate.second ?: return state
    val rateFormatted = rate.toFormattedCurrencyString(2, appCurrency, RoundingMode.HALF_UP)
    val currency = fiatRate.first

    return if (!state.isMultiwalletAllowed) {
        setSingeWalletFiatRate(rate, rateFormatted, currency, appCurrency, state)
    } else {
        setMultiWalletFiatRate(rate, rateFormatted, currency, appCurrency, state)
    }
}

private fun setMultiWalletFiatRate(
    rate: BigDecimal, rateFormatted: String, currency: Currency,
    appCurrency: FiatCurrencyName, state: WalletState
): WalletState {
    val walletData = state.getWalletData(currency) ?: return state
    val wallet = state.getWalletManager(currency)?.wallet
    val fiatAmount = when (currency) {
        is Currency.Blockchain ->
            wallet?.amounts?.get(AmountType.Coin)?.value?.toFiatValue(rate)
        is Currency.Token ->
            wallet?.getTokenAmount(currency.token)?.value?.toFiatValue(rate)
    }
    val fiatAmountFormatted = fiatAmount?.toFormattedFiatValue(appCurrency)
    val newWalletData = state.getWalletData(currency)?.copy(
        currencyData = walletData.currencyData.copy(
            fiatAmountFormatted = fiatAmountFormatted,
            fiatAmount = fiatAmount
        ),
        fiatRate = rate, fiatRateString = rateFormatted
    )
    return state.copy(wallets = state.replaceWalletInWallets(newWalletData))
}

private fun setSingeWalletFiatRate(
    rate: BigDecimal, rateFormatted: String, currency: Currency,
    appCurrency: FiatCurrencyName, state: WalletState
): WalletState {
    val wallet = state.walletManagers[0].wallet
    val token = wallet.getFirstToken()

    if (currency == state.primaryWallet?.currency) {
        val fiatAmount = wallet.amounts[AmountType.Coin]?.value
            ?.toFiatString(rate, appCurrency)
        val walletData = state.primaryWallet.copy(
            currencyData = state.primaryWallet.currencyData.copy(fiatAmountFormatted = fiatAmount),
            fiatRate = rate,
            fiatRateString = rateFormatted
        )
        return state.copy(wallets = listOf(walletData))
    } else if (currency is Currency.Token && currency.token == token) {
        val tokenFiatAmount = wallet.getTokenAmount(token)?.value?.toFiatString(rate, appCurrency)
        val tokenData = state.primaryWallet?.currencyData?.token?.copy(
            fiatAmount = tokenFiatAmount,
            fiatRate = rate,
            fiatRateString = rateFormatted
        )

        val walletData = state.primaryWallet?.copy(
            currencyData = state.primaryWallet.currencyData.copy(
                token = tokenData
            )
        )
        val wallets = walletData?.let { listOf(walletData) } ?: emptyList()
        return state.copy(wallets = wallets)
    }
    return state
}
