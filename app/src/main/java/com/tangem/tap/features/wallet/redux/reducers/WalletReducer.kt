package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressType
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.userwallets.Artwork
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.redux.ErrorType
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.userWalletsListManager
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
                    newState = newState.copy(
                        state = ProgressState.Error,
                        error = ErrorType.NoInternetConnection,
                    )
                }
                is TapError.UnknownBlockchain -> {
                    newState = newState.copy(
                        state = ProgressState.Error,
                        error = ErrorType.UnknownBlockchain,
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
        is WalletAction.AppCurrencyAction -> {
            newState = appCurrencyReducer.reduce(action, newState)
        }
        is WalletAction.UserWalletChanged -> with(action.userWallet) {
            val card = scanResponse.card
            newState = WalletState(
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
        is WalletAction.WalletStoresChanged -> {
            newState = newState.copy(
                walletsStores = action.walletStores,
                selectedCurrency = findSelectedCurrency(
                    walletsStores = action.walletStores,
                    currentSelectedCurrency = newState.selectedCurrency,
                    isMultiWalletAllowed = newState.isMultiwalletAllowed,
                ),
            )
        }
        is WalletAction.TotalFiatBalanceChanged -> {
            newState = newState.copy(
                totalBalance = action.balance,
            )
        }
        is WalletAction.LoadData.Success -> {
            newState = newState.copy(state = ProgressState.Done)
        }
        is WalletAction.UpdateCanSaveUserWallets -> {
            newState = newState.copy(canSaveUserWallets = action.canSaveUserWallets)
        }
        is WalletAction.SetArtworkUrl -> {
            val selectedUserWallet = userWalletsListManager.selectedUserWalletSync?.walletId

            if (selectedUserWallet == action.userWalletId) {
                newState = newState.copy(
                    cardImage = Artwork(artworkId = action.url),
                )
            }
        }
        else -> Unit
    }
    appStateHolder.walletState = newState
    return newState
}

fun findSelectedCurrency(
    walletsStores: List<WalletStoreModel>,
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

fun Wallet.createAddressesData(): List<WalletDataModel.AddressData> {
    val listOfAddressData = mutableListOf<WalletDataModel.AddressData>()
    // put a defaultAddress at the first place
    addresses.forEach {
        val addressData = WalletDataModel.AddressData(
            it.value,
            it.type,
            getShareUri(it.value),
            getExploreUrl(it.value),
        )
        if (it.type == AddressType.Default) {
            listOfAddressData.add(0, addressData)
        } else {
            listOfAddressData.add(addressData)
        }
    }
    return listOfAddressData
}

private fun handleCheckSignedHashesActions(action: WalletAction.Warnings, state: WalletState): WalletState {
    return when (action) {
        is WalletAction.Warnings.Set -> state.copy(mainWarningsList = action.warningList)
        else -> state
    }
}