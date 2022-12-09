package com.tangem.tap.features.walletSelector.redux

import com.tangem.common.CompletionResult
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.flatMap
import com.tangem.common.map
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.model.builders.UserWalletBuilder
import com.tangem.tap.domain.scanCard.ScanCardProcessor
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.totalFiatBalanceCalculator
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.walletStoresManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.rekotlin.Middleware
import timber.log.Timber

internal class WalletSelectorMiddleware {
    val middleware: Middleware<AppState> = { _, appStateProvider ->
        { next ->
            { action ->
                val appState = appStateProvider()
                if (action is WalletSelectorAction && appState != null) {
                    handleAction(action, appState.walletSelectorState)
                }
                next(action)
            }
        }
    }

    private fun handleAction(action: WalletSelectorAction, state: WalletSelectorState) {
        when (action) {
            is WalletSelectorAction.UserWalletsLoaded -> {
                fetchWalletStores(action.userWallets)
            }
            is WalletSelectorAction.WalletStoresChanged -> {
                updateBalances(action.walletsStores, state)
            }
            is WalletSelectorAction.UnlockWithBiometry -> {
                unlockWalletsWithBiometry()
            }
            is WalletSelectorAction.AddWallet -> {
                addWallet()
            }
            is WalletSelectorAction.SelectWallet -> {
                selectWallet(action.walletId)
            }
            is WalletSelectorAction.RemoveWallets -> {
                removeWallets(action.walletIdsToRemove, state)
            }
            is WalletSelectorAction.RenameWallet -> {
                renameWallet(action.walletId, action.newName)
            }
            is WalletSelectorAction.ChangeAppCurrency,
            is WalletSelectorAction.AddWallet.Success,
            is WalletSelectorAction.AddWallet.Error,
            is WalletSelectorAction.SelectedWalletChanged,
            is WalletSelectorAction.UnlockWithBiometry.Error,
            is WalletSelectorAction.UnlockWithBiometry.Success,
            is WalletSelectorAction.BalanceLoaded,
            is WalletSelectorAction.IsLockedChanged,
            is WalletSelectorAction.HandleError,
            is WalletSelectorAction.CloseError,
            -> Unit
        }
    }

    private fun fetchWalletStores(userWallets: List<UserWallet>) {
        scope.launch {
            walletStoresManager.fetch(userWallets)
                .doOnFailure { error ->
                    Timber.e(error, "Unable to fetch wallet stores")
                    store.dispatchOnMain(WalletSelectorAction.HandleError(error))
                }
        }
    }

    private fun updateBalances(walletStores: Map<UserWalletId, List<WalletStoreModel>>, state: WalletSelectorState) {
        walletStores.forEach { (walletId, walletStores) ->
            scope.launch {
                val updatedWallet = state.wallets
                    .find { it.id == walletId.stringValue }
                    ?.updateWalletStoresAndCalculateFiatBalance(walletStores)

                if (updatedWallet != null) {
                    store.dispatchOnMain(WalletSelectorAction.BalanceLoaded(updatedWallet))
                }
            }
        }
    }

    private fun unlockWalletsWithBiometry() {
        scope.launch {
            userWalletsListManager.unlockWithBiometry()
                .doOnFailure { error ->
                    store.dispatchOnMain(WalletSelectorAction.UnlockWithBiometry.Error(error))
                }
                .doOnSuccess {
                    store.dispatchOnMain(WalletSelectorAction.UnlockWithBiometry.Success)
                }
        }
    }

    private fun addWallet() = scope.launch {
        scanCardInternal { scanResponse ->
            val userWallet = UserWalletBuilder(scanResponse).build()

            userWalletsListManager.save(userWallet)
                .doOnFailure { error ->
                    store.dispatchOnMain(WalletSelectorAction.AddWallet.Error(error))
                }
                .doOnSuccess {
                    val selectedWallet = userWalletsListManager.selectedUserWallet.first()
                    val isSavedWalletSelected = userWallet == selectedWallet
                    store.dispatchOnMain(WalletSelectorAction.AddWallet.Success)

                    if (isSavedWalletSelected) {
                        store.dispatchOnMain(NavigationAction.PopBackTo())
                        store.onUserWalletSelected(selectedWallet)
                    }
                }
        }
    }

    private fun selectWallet(id: String) {
        scope.launch {
            userWalletsListManager.selectWallet(UserWalletId(id))
                .doOnFailure { error ->
                    store.dispatchOnMain(WalletSelectorAction.HandleError(error))
                }
                .doOnSuccess { selectedWallet ->
                    store.dispatchOnMain(NavigationAction.PopBackTo())
                    store.onUserWalletSelected(selectedWallet)
                }
        }
    }

    private fun removeWallets(walletIdsToRemove: List<String>, state: WalletSelectorState) {
        scope.launch {
            when (walletIdsToRemove.size) {
                state.wallets.size -> clearUserWallets()
                else -> removeUserWallets(walletIdsToRemove, state)
            }
                .doOnFailure { error ->
                    store.dispatchOnMain(WalletSelectorAction.HandleError(error))
                }
        }
    }

    private fun renameWallet(walletId: String, newName: String) {
        scope.launch {
            userWalletsListManager.get(walletId = UserWalletId(walletId))
                .map { it.copy(name = newName) }
                .flatMap { userWalletsListManager.update(it) }
                .doOnFailure { error ->
                    store.dispatchOnMain(WalletSelectorAction.HandleError(error))
                }
        }
    }

    private suspend inline fun scanCardInternal(
        crossinline onCardScanned: suspend (ScanResponse) -> Unit,
    ) {
        ScanCardProcessor.scan(
            useBiometricsForAccessCode = preferencesStorage.shouldSaveAccessCodes,
            onSuccess = {
                onCardScanned(it)
            },
            onFailure = { error ->
                store.dispatchOnMain(WalletSelectorAction.AddWallet.Error(error))
            },
            onWalletNotCreated = {
                store.dispatchOnMain(WalletSelectorAction.AddWallet.Success)
                store.dispatchOnMain(NavigationAction.PopBackTo())
            },
        )
    }

    private suspend fun clearUserWallets(): CompletionResult<Unit> {
        return userWalletsListManager.clear()
            .flatMap { walletStoresManager.clear() }
            .doOnSuccess {
                store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Home))
            }
    }

    private suspend fun removeUserWallets(
        walletIdsToRemove: List<String>,
        state: WalletSelectorState,
    ): CompletionResult<Unit> {
        val prevSelectedWalletId = state.selectedWalletId
        return userWalletsListManager.delete(walletIdsToRemove.map { UserWalletId(it) })
            .flatMap { walletStoresManager.delete(walletIdsToRemove) }
            .doOnSuccess {
                val selectedWallet = userWalletsListManager.selectedUserWalletSync ?: return@doOnSuccess
                val isSelectedWalletRemoved = prevSelectedWalletId != selectedWallet.walletId.stringValue

                if (isSelectedWalletRemoved) {
                    store.onUserWalletSelected(selectedWallet)
                }
            }
    }

    private suspend fun UserWalletModel.updateWalletStoresAndCalculateFiatBalance(
        walletStores: List<WalletStoreModel>,
    ): UserWalletModel {
        return this.copy(
            type = when (type) {
                is UserWalletModel.Type.MultiCurrency -> type.copy(
                    tokensCount = walletStores.flatMap { it.walletsData }.size,
                )

                is UserWalletModel.Type.SingleCurrency -> type.copy(
                    blockchainName = walletStores
                        .firstOrNull()
                        ?.blockchainNetwork
                        ?.blockchain
                        ?.fullName,
                )
            },
            fiatBalance = totalFiatBalanceCalculator.calculate(
                prevAmount = fiatBalance.amount,
                walletStores = walletStores,
            ),
        )
    }
}
