package com.tangem.tap.features.walletSelector.redux

import com.tangem.common.CompletionResult
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.flatMap
import com.tangem.common.map
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.MyWallets
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.model.builders.UserWalletBuilder
import com.tangem.tap.domain.scanCard.ScanCardProcessor
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.totalFiatBalanceCalculator
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.walletStoresManager
import kotlinx.coroutines.launch
import org.rekotlin.Middleware
import timber.log.Timber
import java.math.BigDecimal

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
            is WalletSelectorAction.UnlockWalletWithCard -> {
                unlockWalletWithCard(action.walletId)
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
        Analytics.send(MyWallets.Button.UnlockWithBiometrics)

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
        Analytics.send(MyWallets.Button.ScanNewCard)

        scanCardInternal { scanResponse ->
            val userWallet = UserWalletBuilder(scanResponse).build()

            userWalletsListManager.save(userWallet)
                .doOnFailure { error ->
                    store.dispatchOnMain(WalletSelectorAction.AddWallet.Error(error))
                }
                .doOnSuccess {
                    Analytics.send(MyWallets.CardWasScanned)

                    userWalletsListManager.selectWallet(userWallet.walletId)
                    store.dispatchOnMain(WalletSelectorAction.AddWallet.Success)
                    updateAccessCodeRequestPolicy(userWallet)
                    store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Wallet))
                    store.onUserWalletSelected(userWallet)
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
                    updateAccessCodeRequestPolicy(selectedWallet)
                    store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Wallet))
                    store.onUserWalletSelected(selectedWallet)
                }
        }
    }

    private fun unlockWalletWithCard(id: String) {
        scope.launch {
            userWalletsListManager.get(UserWalletId(id))
                .flatMap { userWallet ->
                    updateUserWalletWithScannedCard(userWallet)
                }
                .flatMap { updatedUserWallet ->
                    unlockUserWallet(updatedUserWallet)
                }
                .doOnFailure { error ->
                    store.dispatchOnMain(WalletSelectorAction.HandleError(error))
                }
        }
    }

    private suspend fun updateUserWalletWithScannedCard(userWallet: UserWallet): CompletionResult<UserWallet> {
        return tangemSdkManager.scanCard(userWallet.cardId)
            .map { scannedCard ->
                userWallet.copy(
                    scanResponse = userWallet.scanResponse.copy(
                        card = scannedCard,
                    ),
                )
            }
    }

    private suspend fun unlockUserWallet(userWallet: UserWallet): CompletionResult<Unit> {
        return userWalletsListManager.unlockWithCard(userWallet)
            .doOnSuccess {
                store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Wallet))
                store.onUserWalletSelected(userWallet)
            }
    }

    private fun removeWallets(walletIdsToRemove: List<String>, state: WalletSelectorState) {
        Analytics.send(MyWallets.Button.DeleteWalletTapped)

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
        Analytics.send(MyWallets.Button.EditWalletTapped)

        scope.launch {
            userWalletsListManager.get(walletId = UserWalletId(walletId))
                .map { it.copy(name = newName) }
                .flatMap { userWalletsListManager.save(it, canOverride = true) }
                .doOnFailure { error ->
                    store.dispatchOnMain(WalletSelectorAction.HandleError(error))
                }
        }
    }

    private suspend inline fun scanCardInternal(
        crossinline onCardScanned: suspend (ScanResponse) -> Unit,
    ) {
        ScanCardProcessor.scan(
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
                    updateAccessCodeRequestPolicy(selectedWallet)
                    store.onUserWalletSelected(selectedWallet)
                }
            }
    }

    private fun updateAccessCodeRequestPolicy(userWallet: UserWallet) {
        tangemSdkManager.setAccessCodeRequestPolicy(
            useBiometricsForAccessCode = preferencesStorage.shouldSaveAccessCodes &&
                userWallet.hasAccessCode,
        )
    }

    private suspend fun UserWalletModel.updateWalletStoresAndCalculateFiatBalance(
        walletStores: List<WalletStoreModel>,
    ): UserWalletModel {
        return this.copy(
            type = when (type) {
                is UserWalletModel.Type.MultiCurrency -> type.copy(
                    tokensCount = walletStores.flatMap { it.walletsData }.size,
                )
                is UserWalletModel.Type.SingleCurrency -> type
            },
            fiatBalance = totalFiatBalanceCalculator.calculate(
                prevAmount = fiatBalance.amount,
                walletStores = walletStores,
                initial = TotalFiatBalance.Loaded(BigDecimal.ZERO),
            ),
        )
    }
}
