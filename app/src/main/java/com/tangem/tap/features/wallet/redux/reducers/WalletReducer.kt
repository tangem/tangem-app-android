package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.common.extensions.mapNotNullValues
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.TwinCardNumber
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.extensions.toFiatRateString
import com.tangem.tap.common.extensions.toFiatString
import com.tangem.tap.common.extensions.toFiatValue
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.common.extensions.toFormattedFiatValue
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.extensions.getArtworkUrl
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.WalletRent
import com.tangem.tap.features.wallet.redux.AddressData
import com.tangem.tap.features.wallet.redux.Artwork
import com.tangem.tap.features.wallet.redux.ErrorType
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletAddresses
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.redux.WalletMainButton
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.WalletStore
import com.tangem.tap.features.wallet.redux.replaceSomeWalletsData
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.proxy.AppStateHolder
import org.rekotlin.Action
import timber.log.Timber
import java.math.BigDecimal

object WalletReducer {
    fun reduce(action: Action, state: AppState, appStateHolder: AppStateHolder): WalletState =
        internalReduce(action, state, appStateHolder)
}

@Suppress("LongMethod", "ComplexMethod")
private fun internalReduce(action: Action, state: AppState, appStateHolder: AppStateHolder): WalletState {
    val multiWalletReducer = MultiWalletReducer()
    val onWalletLoadedReducer = OnWalletLoadedReducer()
    val appCurrencyReducer = AppCurrencyReducer()

    if (action !is WalletAction) return state.walletState

    var newState = state.walletState

    when (action) {
        is WalletAction.Warnings -> newState = handleCheckSignedHashesActions(action, newState)
        is WalletAction.MultiWallet -> newState = multiWalletReducer.reduce(action, newState)

        is WalletAction.ResetState -> {
            newState = WalletState(
                cardId = action.newCard.cardId,
                walletCardsCount = action.newCard.findCardsCount(),
            )
        }
        is WalletAction.SetIfTestnetCard -> newState = newState.copy(isTestnet = action.isTestnet)
        is WalletAction.EmptyWallet -> {
            newState = newState.copy(
                state = ProgressState.Done,
                walletsStores = listOf(
                    WalletStore(
                        walletManager = null,
                        blockchainNetwork = BlockchainNetwork(
                            Blockchain.Unknown,
                            null,
                            emptyList(),
                        ),
                        walletsData = listOf(
                            WalletData(
                                currencyData = BalanceWidgetData(BalanceStatus.EmptyCard),
                                mainButton = WalletMainButton.CreateWalletButton(true),
                                currency = Currency.Blockchain(Blockchain.Unknown, null),
                            ),
                        ),
                    ),
                ),
            )
        }
        is WalletAction.LoadData.Failure -> {
            when (action.error) {
                is TapError.NoInternetConnection -> {
                    val wallets = newState.walletsStores
                        .map { store ->
                            store.copy(
                                walletsData = store.walletsData.map {
                                    it.copy(
                                        currencyData = it.currencyData.copy(
                                            status = BalanceStatus.Unreachable,
                                        ),
                                    )
                                },
                            )
                        }

                    newState = newState.copy(
                        state = ProgressState.Error,
                        error = ErrorType.NoInternetConnection,
                        walletsStores = wallets,
                    )
                }
                is TapError.UnknownBlockchain -> {
                    newState = newState.copy(
                        state = ProgressState.Done,
                        walletsStores = listOf(
                            WalletStore(
                                walletManager = null,
                                blockchainNetwork = BlockchainNetwork(
                                    Blockchain.Unknown,
                                    null,
                                    emptyList(),
                                ),
                                walletsData = listOf(
                                    WalletData(
                                        currencyData = BalanceWidgetData(BalanceStatus.UnknownBlockchain),
                                        currency = Currency.Blockchain(Blockchain.Unknown, null),
                                    ),
                                ),
                            ),
                        ),
                    )
                }
                else -> {
                    newState = newState.copy(
                        state = ProgressState.Error,
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
        is WalletAction.LoadData.Refresh -> {
            newState = newState.copy(
                state = ProgressState.Refreshing,
                error = null,
            )
        }
        is WalletAction.LoadWallet -> {
            val balanceStatus = if (newState.state == ProgressState.Refreshing) {
                BalanceStatus.Refreshing
            } else {
                BalanceStatus.Loading
            }
            if (action.blockchain == null) {
                val wallets = newState.walletsStores.map { walletStore ->
                    walletStore.copy(
                        walletsData = walletStore.walletsData.map { walletData ->
                            walletData.copy(
                                currencyData = walletData.currencyData.copy(
                                    status =
                                    if (walletStore.walletManager != null) balanceStatus else BalanceStatus.Unreachable,
                                    currency = walletData.currencyData.currency,
                                    currencySymbol = walletData.currencyData.currencySymbol,
                                ),
                                mainButton = WalletMainButton.SendButton(false),
                            )
                        },
                    )
                }
                newState = newState.copy(
                    state = ProgressState.Loading,
                    walletsStores = wallets,
                )
            } else {
                val walletManager = newState.getWalletManager(action.blockchain) ?: return newState
                val currencies = listOf(Currency.fromBlockchainNetwork(action.blockchain)) +
                    walletManager.cardTokens.map {
                        Currency.fromBlockchainNetwork(action.blockchain, it)
                    }
                val newWalletsData = newState.walletsDataFromStores.filter { currencies.contains(it.currency) }
                    .map { wallet ->
                        wallet.copy(
                            currencyData = wallet.currencyData.copy(
                                status = balanceStatus,
                                currency = wallet.currencyData.currency,
                                currencySymbol = wallet.currencyData.currencySymbol,
                            ),
                            mainButton = WalletMainButton.SendButton(false),
                        )
                    }
                val walletsData = newState.walletsDataFromStores.replaceSomeWalletsData(newWalletsData)
                val walletStore = newState.getWalletStore(action.blockchain)?.updateWallets(walletsData)
                newState = newState.updateWalletStore(walletStore)
            }
        }
        is WalletAction.LoadWallet.Success -> newState = onWalletLoadedReducer.reduce(
            wallet = action.wallet,
            blockchainNetwork = action.blockchain,
            walletState = newState,
        )
        is WalletAction.LoadWallet.NoAccount -> {
            val amount = BigDecimal.ZERO
            val fiatAmount = BigDecimal.ZERO
            val walletData = newState.getWalletData(action.blockchain)?.let { walletData ->
                val walletBlockchain = walletData.currency.blockchain
                walletData.copy(
                    currencyData = BalanceWidgetData(
                        status = BalanceStatus.NoAccount,
                        currency = action.wallet.blockchain.fullName,
                        currencySymbol = action.wallet.blockchain.currency,
                        amount = amount,
                        amountFormatted = amount.toFormattedCurrencyString(
                            decimals = walletBlockchain.decimals(),
                            currency = walletBlockchain.currency,
                        ),
                        fiatAmount = fiatAmount,
                        fiatAmountFormatted = fiatAmount.toFormattedFiatValue(
                            fiatCurrencyName = state.globalState.appCurrency.symbol,
                        ),
                        amountToCreateAccount = action.amountToCreateAccount,
                    ),
                )
            }
            val updatedWalletStore = newState.getWalletStore(action.blockchain)
                ?.updateWallets(listOfNotNull(walletData))

            newState = newState.updateWalletStore(updatedWalletStore)
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
                    errorMessage = message,
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
                            status = BalanceStatus.Unreachable,
                            errorMessage = message,
                        ),
                    )
                }
            val updatedWallets = walletStore!!.updateWallets(listOfNotNull(newWalletData) + tokenWallets).walletsData

            newState = newState.updateWalletsData(updatedWallets)

            val progressState =
                if (newState.walletsDataFromStores.any { it.currencyData.status == BalanceStatus.Loading }) {
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
        is WalletAction.TradeCryptoAction -> return newState
        is WalletAction.ChangeSelectedAddress -> {
            val walletAddresses = newState.getWalletData(newState.selectedCurrency)?.walletAddresses
                ?: return newState
            val address = walletAddresses.list.firstOrNull { it.type == action.type }
                ?: return newState

            newState = newState.updateWalletData(
                newState.selectedWalletData?.copy(
                    walletAddresses = WalletAddresses(
                        selectedAddress = address,
                        list = walletAddresses.list,
                    ),
                ),
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
        is WalletAction.UserTokens.Loading -> newState = newState.copy(loadingUserTokens = true)
        is WalletAction.UserTokens.Loaded -> newState = newState.copy(loadingUserTokens = false)
        is WalletAction.UserWalletChanged -> with(action.userWallet) {
            val card = scanResponse.card
            newState = WalletState(
                cardId = card.cardId,
                isMultiwalletAllowed = isMultiCurrency,
                cardImage = Artwork(
                    artworkId = artworkUrl,
                ),
                isTestnet = card.isTestCard,
                state = ProgressState.Loading,
                showBackupWarning = isMultiCurrency &&
                    card.settings.isBackupAllowed &&
                    card.backupStatus == CardDTO.BackupStatus.NoBackup,
                walletCardsCount = card.findCardsCount(),
                walletsStores = newState.walletsStores,
                totalBalance = if (isMultiCurrency) {
                    newState.totalBalance
                } else {
                    null
                },
            )
        }
        is WalletAction.WalletStoresChanged.UpdateWalletStores -> {
            newState = newState.copy(
                walletsStores = action.reduxWalletStores,
            )
        }
        is WalletAction.TotalFiatBalanceChanged -> {
            newState = newState.copy(
                totalBalance = action.balance,
            )
        }
        is WalletAction.LoadData.Success -> {
            val selectedCurrency = if (newState.isMultiwalletAllowed) {
                newState.selectedCurrency
            } else {
                newState.walletsStores.firstOrNull()
                    ?.walletsData
                    ?.firstOrNull()
                    ?.currency
            }

            newState = newState.copy(
                state = ProgressState.Done,
                selectedCurrency = selectedCurrency,
            )
        }
        else -> Unit
    }
    appStateHolder.walletState = newState
    return newState
}

private fun CardDTO.findCardsCount(): Int? {
    return (this.backupStatus as? CardDTO.BackupStatus.Active)?.cardCount?.inc()
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
            getExploreUrl(it.value),
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
    state: WalletState,
): WalletState {
    return when (action) {
        WalletAction.Warnings.CheckHashesCount.ConfirmHashesCount -> state.copy(hashesCountVerified = true)
        WalletAction.Warnings.CheckHashesCount.NeedToCheckHashesCountOnline -> state.copy(
            hashesCountVerified = false,
        )
        is WalletAction.Warnings.Set -> state.copy(mainWarningsList = action.warningList)
        else -> state
    }
}

private fun setNewFiatRate(
    fiatRates: Map<Currency, BigDecimal?>,
    appCurrency: FiatCurrency,
    state: WalletState,
): WalletState {
    val rateFormatter: (BigDecimal) -> String = { rate: BigDecimal ->
        rate.toFiatRateString(
            fiatCurrencyName = appCurrency.symbol,
        )
    }

    return if (state.isMultiwalletAllowed) {
        setMultiWalletFiatRate(
            fiatRates = fiatRates.mapNotNullValues { it.value },
            rateFormatter = rateFormatter,
            appCurrency = appCurrency,
            state = state,
        )
    } else {
        setSingleWalletFiatRates(
            fiatRates = fiatRates.mapNotNullValues { it.value },
            rateFormatter = rateFormatter,
            appCurrency = appCurrency,
            state = state,
        )
    }
}

private fun setMultiWalletFiatRate(
    fiatRates: Map<Currency, BigDecimal>,
    rateFormatter: (BigDecimal) -> String,
    appCurrency: FiatCurrency,
    state: WalletState,
): WalletState {
    val newWalletsData = fiatRates.mapNotNull { (currency, rate) ->
        val walletStore = state.getWalletStore(currency) ?: return@mapNotNull null
        val wallet = walletStore.walletManager?.wallet
        val walletData = state.getWalletData(currency) ?: return@mapNotNull null
        val currencyData = walletData.currencyData

        var fiatAmount = when (currency) {
            is Currency.Blockchain ->
                wallet?.amounts?.get(AmountType.Coin)?.value?.toFiatValue(rate)
            is Currency.Token ->
                wallet?.getTokenAmount(currency.token)?.value?.toFiatValue(rate)
        }
        if (currencyData.status == BalanceStatus.NoAccount && fiatAmount == null) {
            fiatAmount = BigDecimal.ZERO.setScale(2)
        }
        val fiatAmountFormatted = fiatAmount?.toFormattedFiatValue(appCurrency.symbol)

        walletData.copy(
            currencyData = currencyData.copy(
                fiatAmountFormatted = fiatAmountFormatted,
                fiatAmount = fiatAmount,
            ),
            fiatRate = rate,
            fiatRateString = rateFormatter(rate),
        )
    }

    return state
        .updateWalletsData(newWalletsData)
}

private fun setSingleWalletFiatRates(
    fiatRates: Map<Currency, BigDecimal>,
    appCurrency: FiatCurrency,
    rateFormatter: (BigDecimal) -> String,
    state: WalletState,
): WalletState {
    val blockchainFiatRate = fiatRates.entries.firstOrNull { it.key.isBlockchain() }
    val tokenFiatRate = fiatRates.entries.firstOrNull { it.key.isToken() }
    Timber.e("Token Fiat Rate is ${tokenFiatRate ?: "NULL"}")
    val updatedState = updateStateWithFiatRate(blockchainFiatRate?.toPair(), appCurrency, rateFormatter, state)
    return updateStateWithFiatRate(tokenFiatRate?.toPair(), appCurrency, rateFormatter, updatedState)
}

private fun updateStateWithFiatRate(
    fiatRate: Pair<Currency, BigDecimal>?,
    appCurrency: FiatCurrency,
    rateFormatter: (BigDecimal) -> String,
    state: WalletState,
): WalletState {
    return if (fiatRate != null) {
        val currency = fiatRate.first
        val rate = fiatRate.second

        setSingleWalletFiatRate(
            rate = rate,
            rateFormatted = rateFormatter(rate),
            currency = currency,
            appCurrency = appCurrency,
            state = state,
        )
    } else {
        state
    }
}

private fun setSingleWalletFiatRate(
    rate: BigDecimal,
    rateFormatted: String,
    currency: Currency,
    appCurrency: FiatCurrency,
    state: WalletState,
): WalletState {
    val wallet = state.primaryWalletManager?.wallet ?: return state
    val token = wallet.getFirstToken()
    Timber.e("Working with currency: ${currency.currencyName}")

    if (currency == state.primaryWallet?.currency) {
        val fiatAmount = wallet.amounts[AmountType.Coin]?.value
            ?.toFiatString(rate, appCurrency.code)
        val walletData = state.primaryWallet.copy(
            currencyData = state.primaryWallet.currencyData.copy(fiatAmountFormatted = fiatAmount),
            fiatRate = rate,
            fiatRateString = rateFormatted,
        )
        return state.updateWalletData(walletData)
    } else if (currency is Currency.Token && currency.token == token) {
        Timber.e("Working with token fiat rate")

        val tokenFiatAmount = wallet.getTokenAmount(token)
            ?.value
        val tokenAmountFormatted = tokenFiatAmount?.toFiatString(rate, appCurrency.code)

        val tokenData = state.primaryWallet?.currencyData?.token?.copy(
            fiatAmountFormatted = tokenAmountFormatted,
            fiatAmount = tokenFiatAmount,
            fiatRate = rate,
            fiatRateString = rateFormatted,
        )
        //     ?: TokenData(
        //     fiatAmountFormatted = tokenAmountFormatted,
        //     fiatAmount = tokenFiatAmount,
        //     fiatRate = rate,
        //     fiatRateString = rateFormatted,
        //     amount = "",
        //     tokenSymbol = currency.currencySymbol
        // )

        Timber.e("Token Data is ${tokenData ?: "NULL"}")

        val walletData = state.primaryWallet?.copy(
            currencyData = state.primaryWallet.currencyData.copy(
                token = tokenData,
            ),
        )
        Timber.e("Wallet Data is ${walletData ?: "NULL"}")
        return state.updateWalletData(walletData)
    }
    return state
}
