package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.redux.AddressData
import com.tangem.tap.features.wallet.redux.Artwork
import com.tangem.tap.features.wallet.redux.ErrorType
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletAddresses
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.WalletStore
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.BalanceWidgetData
import com.tangem.tap.proxy.AppStateHolder
import org.rekotlin.Action

object WalletReducer {
    fun reduce(action: Action, state: AppState, appStateHolder: AppStateHolder): WalletState =
        internalReduce(action, state, appStateHolder)
}

@Suppress("LongMethod", "ComplexMethod")
private fun internalReduce(action: Action, state: AppState, appStateHolder: AppStateHolder): WalletState {
    val multiWalletReducer = MultiWalletReducer()
    val appCurrencyReducer = AppCurrencyReducer()

    if (action !is WalletAction) return state.walletState

    var newState = state.walletState

    when (action) {
        is WalletAction.Warnings -> newState = handleCheckSignedHashesActions(action, newState)
        is WalletAction.MultiWallet -> newState = multiWalletReducer.reduce(action, newState)
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
        is WalletAction.AppCurrencyAction -> {
            newState = appCurrencyReducer.reduce(action, newState)
        }
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
                state = action.reduxWalletStores.flatMap { it.walletsData }.findProgressState(newState.state),
                walletsStores = action.reduxWalletStores,
            )
        }
        is WalletAction.TotalFiatBalanceChanged -> {
            newState = newState.copy(
                totalBalance = action.balance,
            )
        }
        is WalletAction.LoadData.Success -> {
            newState = newState.copy(
                state = ProgressState.Done,
                selectedCurrency = findSelectedCurrency(
                    walletsStores = newState.walletsStores,
                    currentSelectedCurrency = newState.selectedCurrency,
                    isMultiWalletAllowed = newState.isMultiwalletAllowed,
                ),
            )
        }
        is WalletAction.UpdateCanSaveUserWallets -> {
            newState = newState.copy(
                canSaveUserWallets = action.canSaveUserWallets,
            )
        }
        else -> Unit
    }
    appStateHolder.walletState = newState
    return newState
}

fun findSelectedCurrency(
    walletsStores: List<WalletStore>,
    currentSelectedCurrency: Currency?,
    isMultiWalletAllowed: Boolean,
): Currency? = if (isMultiWalletAllowed) {
    currentSelectedCurrency
} else {
    walletsStores.firstOrNull()
        ?.walletsData
        ?.firstOrNull()
        ?.currency
}

private fun CardDTO.findCardsCount(): Int? {
    return (this.backupStatus as? CardDTO.BackupStatus.Active)?.cardCount?.inc()
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

private fun handleCheckSignedHashesActions(action: WalletAction.Warnings, state: WalletState): WalletState {
    return when (action) {
        WalletAction.Warnings.CheckHashesCount.ConfirmHashesCount -> state.copy(hashesCountVerified = true)
        WalletAction.Warnings.CheckHashesCount.NeedToCheckHashesCountOnline -> state.copy(
            hashesCountVerified = false,
        )
        is WalletAction.Warnings.Set -> state.copy(mainWarningsList = action.warningList)
        else -> state
    }
}