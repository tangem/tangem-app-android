package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.common.extensions.mapNotNullValues
import com.tangem.domain.common.TwinCardNumber
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.extensions.toFiatRateString
import com.tangem.tap.common.extensions.toFiatString
import com.tangem.tap.common.extensions.toFiatValue
import com.tangem.tap.common.extensions.toFormattedFiatValue
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.extensions.getArtworkUrl
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.domain.tokens.BlockchainNetwork
import com.tangem.tap.features.wallet.redux.AddressData
import com.tangem.tap.features.wallet.redux.Artwork
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.ErrorType
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.TradeCryptoState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletAddresses
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.redux.WalletDialog
import com.tangem.tap.features.wallet.redux.WalletMainButton
import com.tangem.tap.features.wallet.redux.WalletRent
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.WalletStore
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.store
import java.math.BigDecimal
import java.math.RoundingMode
import org.rekotlin.Action

class WalletReducer {
    companion object {
        fun reduce(action: Action, state: AppState): WalletState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, state: AppState): WalletState {

    val multiWalletReducer = MultiWalletReducer()
    val onWalletLoadedReducer = OnWalletLoadedReducer()
    val appCurrencyReducer = AppCurrencyReducer()

    if (action !is WalletAction) return state.walletState

    val exchangeManager = store.state.globalState.currencyExchangeManager
    var newState = state.walletState

    when (action) {
        is WalletAction.Warnings -> newState = handleCheckSignedHashesActions(action, newState)
        is WalletAction.MultiWallet -> newState = multiWalletReducer.reduce(action, newState)

        is WalletAction.ResetState -> newState = WalletState()
        is WalletAction.SetIfTestnetCard -> newState = newState.copy(isTestnet = action.isTestnet)
        is WalletAction.EmptyWallet -> {
            newState = newState.copy(
                state = ProgressState.Done,
                wallets = listOf(
                    WalletStore(
                        walletManager = null,
                        blockchainNetwork = BlockchainNetwork(
                            Blockchain.Unknown,
                            null,
                            emptyList()
                        ),
                        walletsData = listOf(
                            WalletData(
                                currencyData = BalanceWidgetData(BalanceStatus.EmptyCard),
                                mainButton = WalletMainButton.CreateWalletButton(true),
                                currency = Currency.Blockchain(Blockchain.Unknown, null)
                            )
                        )
                    )
                )
            )
        }
        is WalletAction.LoadData.Failure -> {
            when (action.error) {
                is TapError.NoInternetConnection -> {
                    val wallets = newState.wallets
                        .map { store ->
                            store.copy(
                                walletsData = store.walletsData.map {
                                    it.copy(
                                        currencyData = it.currencyData.copy(
                                            status = BalanceStatus.Unreachable
                                        )
                                    )
                                }

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
                            WalletStore(
                                walletManager = null,
                                blockchainNetwork = BlockchainNetwork(
                                    Blockchain.Unknown,
                                    null,
                                    emptyList()
                                ),
                                walletsData = listOf(
                                    WalletData(
                                        currencyData = BalanceWidgetData(BalanceStatus.UnknownBlockchain),
                                        currency = Currency.Blockchain(Blockchain.Unknown, null)
                                    )
                                )
                            )
                        )
                    )
                }
                else -> { /* no-op */
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
                val wallets = newState.wallets.map {
                    it.copy(
                        walletsData = it.walletsData.map { walletData ->
                            walletData.copy(
                                currencyData = walletData.currencyData.copy(
                                    status = BalanceStatus.Loading,
                                    currency = walletData.currencyData.currency,
                                    currencySymbol = walletData.currencyData.currencySymbol,
                                ),
                                mainButton = WalletMainButton.SendButton(false),
                                tradeCryptoState = TradeCryptoState.from(
                                    exchangeManager,
                                    walletData
                                )
                            )
                        }
                    )
                }
                newState = newState.copy(
                    state = ProgressState.Loading,
                    wallets = wallets,
                )
            } else {
                val walletManager = newState.getWalletManager(action.blockchain) ?: return newState
                val currencies = listOf(Currency.fromBlockchainNetwork(action.blockchain)) +
                    walletManager.cardTokens.map {
                        Currency.fromBlockchainNetwork(action.blockchain, it)
                    }
                val newWallets = newState.walletsData.filter { currencies.contains(it.currency) }
                    .map { wallet ->
                        wallet.copy(
                            currencyData = wallet.currencyData.copy(
                                status = BalanceStatus.Loading,
                                currency = wallet.currencyData.currency,
                                currencySymbol = wallet.currencyData.currencySymbol,
                            ),
                            mainButton = WalletMainButton.SendButton(false),
                            tradeCryptoState = TradeCryptoState.from(exchangeManager, wallet)
                        )
                    }
                val wallets = newState.updateTradeCryptoState(
                    exchangeManager,
                    newState.replaceSomeWallets(newWallets)
                )
                val walletStore = newState.getWalletStore(action.blockchain)?.updateWallets(wallets)
                newState = newState.updateWalletStore(walletStore)
            }
        }
        is WalletAction.LoadWallet.Success -> newState = onWalletLoadedReducer.reduce(
            wallet = action.wallet,
            blockchainNetwork = action.blockchain,
            walletState = newState
        )
        is WalletAction.LoadWallet.NoAccount -> {
            val walletData = newState.getWalletData(action.blockchain)?.copy(
                currencyData = BalanceWidgetData(
                    BalanceStatus.NoAccount, action.wallet.blockchain.fullName,
                    currencySymbol = action.wallet.blockchain.currency,
                    amountToCreateAccount = action.amountToCreateAccount
                )
            )
            var updatedWalletStore = newState.getWalletStore(action.blockchain)
                ?.updateWallets(listOfNotNull(walletData))

            val progressState =
                if (updatedWalletStore?.walletsData?.any { it.currencyData.status == BalanceStatus.Loading } == true) {
                    ProgressState.Loading
                } else {
                    ProgressState.Done
                }
            updatedWalletStore =
                updatedWalletStore?.updateWallets(
                    newState.updateTradeCryptoState(exchangeManager, updatedWalletStore.walletsData)
                )

            newState = newState.updateWalletStore(updatedWalletStore)
                .copy(
                    state = progressState
                )
        }

        is WalletAction.LoadWallet.Failure -> {
            val message = if (newState.error == ErrorType.NoInternetConnection) {
                null
            } else {
                action.errorMessage
            }
            val walletStore = newState.getWalletStore(action.wallet)
            val walletData = walletStore?.walletsData?.first { it.currency is Currency.Blockchain }
            val newWalletData = walletData?.copy(
                currencyData = walletData.currencyData.copy(
                    status = BalanceStatus.Unreachable,
                    errorMessage = message
                ),
            )
            val tokenWallets = action.wallet.getTokens()
                .mapNotNull { token ->
                    walletStore?.blockchainNetwork?.let {
                        newState.getWalletData(Currency.fromBlockchainNetwork(it, token))
                    }
                }
                .map {
                    it.copy(
                        currencyData = it.currencyData.copy(
                            status = BalanceStatus.Unreachable, errorMessage = message
                        )
                    )
                }
            val updatedWallets =
                newState.updateTradeCryptoState(
                    exchangeManager,
                    walletStore!!.updateWallets(listOfNotNull(newWalletData) + tokenWallets).walletsData
                )

            newState = newState.updateWalletsData(updatedWallets)

            val progressState =
                if (newState.walletsData.any { it.currencyData.status == BalanceStatus.Loading }) {
                    ProgressState.Loading
                } else {
                    ProgressState.Done
                }

            newState = newState.copy(
                state = progressState,
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
            newState = setNewFiatRate(action.fiatRates, state.globalState.appCurrency, newState)
        is WalletAction.LoadArtwork -> {
            val artworkUrl = action.card.getArtworkUrl(action.artworkId)
                ?: when (state.twinCardsState.cardNumber) {
                    TwinCardNumber.First -> Artwork.TWIN_CARD_1
                    TwinCardNumber.Second -> Artwork.TWIN_CARD_2
                    else -> Artwork.DEFAULT_IMG_URL
                }
            newState = newState.copy(cardImage = Artwork(artworkId = artworkUrl))
        }
        is WalletAction.ShowDialog.SignedHashesMultiWalletDialog -> {
            newState = newState.copy(walletDialog = WalletDialog.SignedHashesMultiWalletDialog)
        }
        is WalletAction.ShowDialog.ChooseTradeActionDialog -> {
            newState = newState.copy(walletDialog = WalletDialog.ChooseTradeActionDialog)
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
        is WalletAction.TradeCryptoAction -> return newState
        is WalletAction.ChangeSelectedAddress -> {
            val selectedWalletData = newState.getWalletData(newState.selectedCurrency)

            val walletAddresses =
                newState.getWalletData(selectedWalletData?.currency)?.walletAddresses
                    ?: return newState
            val address = walletAddresses.list.firstOrNull { it.type == action.type }
                ?: return newState
            newState = newState.updateWalletData(
                selectedWalletData?.copy(
                    walletAddresses = WalletAddresses(
                        address,
                        walletAddresses.list
                    )
                )
            )
        }
        is WalletAction.SetWalletRent -> {
            val walletStore = newState.getWalletStore(action.wallet) ?: return newState
            val walletRent = WalletRent(action.minRent, action.rentExempt)
            val walletsData = walletStore.walletsData.map { it.copy(walletRent = walletRent) }
            newState = newState.updateWalletsData(walletsData)
        }
        is WalletAction.RemoveWalletRent -> {
            val walletStore = newState.getWalletStore(action.wallet) ?: return newState
            val walletsData = walletStore.walletsData.map { it.copy(walletRent = null) }
            newState = newState.updateWalletsData(walletsData)
        }
        is WalletAction.AppCurrencyAction -> {
            newState = appCurrencyReducer.reduce(action, newState)
        }
        else -> { /* no-op */
        }
    }
    return newState
}

fun createAddressList(wallet: Wallet?, walletAddresses: WalletAddresses? = null): WalletAddresses? {
    if (wallet == null) return null

    val listOfAddressData = wallet.createAddressesData()
    // restore a selected wallet address
    var indexOfSelectedWallet = 0
    walletAddresses?.let {
        val index =
            listOfAddressData.indexOfFirst { it.address == walletAddresses.selectedAddress.address }
        if (index != -1) indexOfSelectedWallet = index
    }
    return WalletAddresses(listOfAddressData[indexOfSelectedWallet], listOfAddressData)
}

fun Wallet.createAddressesData(): List<AddressData> {
    val listOfAddressData = mutableListOf<AddressData>()
    // put a defaultAddress at the first place
    addresses.forEach {
        val addressData = AddressData(
            it.value,
            it.type,
            getShareUri(it.value),
            getExploreUrl(it.value)
        )
        if (it.type == blockchain.defaultAddressType()) {
            listOfAddressData.add(0, addressData)
        } else {
            listOfAddressData.add(addressData)
        }
    }
    return listOfAddressData
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
    fiatRates: Map<Currency, BigDecimal?>,
    appCurrency: FiatCurrency,
    state: WalletState
): WalletState {
    val rateFormatter: (BigDecimal) -> String = { rate: BigDecimal ->
        rate.toFiatRateString(
            fiatCurrencyName = appCurrency.symbol
        )
    }

    return if (state.isMultiwalletAllowed) {
        setMultiWalletFiatRate(
            fiatRates = fiatRates.mapNotNullValues { it.value },
            rateFormatter = rateFormatter,
            appCurrency = appCurrency,
            state = state
        )
    } else {
        val fiatRate = fiatRates.entries.firstOrNull()
        val currency = fiatRate?.key ?: return state
        val rate = fiatRate.value ?: return state

        setSingleWalletFiatRate(
            rate = rate,
            rateFormatted = rateFormatter(rate),
            currency = currency,
            appCurrency = appCurrency,
            state = state
        )
    }
}

private fun setMultiWalletFiatRate(
    fiatRates: Map<Currency, BigDecimal>,
    rateFormatter: (BigDecimal) -> String,
    appCurrency: FiatCurrency,
    state: WalletState
): WalletState {
    val newWalletsData = fiatRates.mapNotNull { (currency, rate) ->
        val walletStore = state.getWalletStore(currency) ?: return state
        val wallet = walletStore.walletManager?.wallet
        val walletData = state.getWalletData(currency) ?: return state

        val fiatAmount = when (currency) {
            is Currency.Blockchain ->
                wallet?.amounts?.get(AmountType.Coin)?.value?.toFiatValue(rate)
            is Currency.Token ->
                wallet?.getTokenAmount(currency.token)?.value?.toFiatValue(rate)
        }
        val fiatAmountFormatted = fiatAmount?.toFormattedFiatValue(appCurrency.symbol)
        state.getWalletData(currency)?.copy(
            currencyData = walletData.currencyData.copy(
                fiatAmountFormatted = fiatAmountFormatted,
                fiatAmount = fiatAmount
            ),
            fiatRate = rate,
            fiatRateString = rateFormatter(rate)
        )
    }

    return state
        .updateWalletsData(newWalletsData)
        .updateTotalBalance(
            totalBalance = obtainTotalBalance(newWalletsData, appCurrency)
        )
}

private fun setSingleWalletFiatRate(
    rate: BigDecimal,
    rateFormatted: String,
    currency: Currency,
    appCurrency: FiatCurrency,
    state: WalletState
): WalletState {
    val wallet = state.primaryWalletManager?.wallet ?: return state
    val token = wallet.getFirstToken()

    if (currency == state.primaryWallet?.currency) {
        val fiatAmount = wallet.amounts[AmountType.Coin]?.value
            ?.toFiatString(rate, appCurrency.code)
        val walletData = state.primaryWallet.copy(
            currencyData = state.primaryWallet.currencyData.copy(fiatAmountFormatted = fiatAmount),
            fiatRate = rate,
            fiatRateString = rateFormatted
        )
        return state.updateWalletData(walletData)
    } else if (currency is Currency.Token && currency.token == token) {
        val tokenFiatAmount = wallet.getTokenAmount(token)
            ?.value
            ?.toFiatString(rate, appCurrency.code)
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
        return state.updateWalletsData(wallets)
    }
    return state
}