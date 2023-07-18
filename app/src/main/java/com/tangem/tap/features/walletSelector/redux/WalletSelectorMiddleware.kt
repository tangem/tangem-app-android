package com.tangem.tap.features.walletSelector.redux

import com.tangem.common.*
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.domain.userwallets.UserWalletIdBuilder
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.*
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.analytics.events.MyWallets
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.userWalletList.unlockIfLockable
import com.tangem.tap.proxy.redux.DaggerGraphState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.rekotlin.Middleware
import timber.log.Timber

// Refactoring is coming
@Suppress("LargeClass")
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
                unlockWallets()
            }
            is WalletSelectorAction.AddWallet -> {
                addWallet()
            }
            is WalletSelectorAction.SelectWallet -> {
                selectWallet(action.userWalletId)
            }
            is WalletSelectorAction.RemoveWallets -> {
                deleteWallets(action.userWalletsIds, state)
            }
            is WalletSelectorAction.RenameWallet -> {
                renameWallet(action.userWalletId, action.newName)
            }
            is WalletSelectorAction.ChangeAppCurrency -> {
                refreshUserWalletsAmounts()
            }
            is WalletSelectorAction.ClearUserWallets -> {
                clearUserWalletsAndCloseError()
            }
            is WalletSelectorAction.AddWallet.Success,
            is WalletSelectorAction.AddWallet.Error,
            is WalletSelectorAction.SelectedWalletChanged,
            is WalletSelectorAction.UnlockWithBiometry.Error,
            is WalletSelectorAction.UnlockWithBiometry.Success,
            is WalletSelectorAction.BalancesLoaded,
            is WalletSelectorAction.IsLockedChanged,
            is WalletSelectorAction.CloseError,
            -> Unit
        }
    }

    private fun fetchWalletStores(userWallets: List<UserWallet>) {
        scope.launch {
            walletStoresManager.fetch(userWallets)
                .doOnFailure { error ->
                    Timber.e(error, "Unable to fetch wallet stores")
                }
        }
    }

    private fun updateBalances(
        updatedWalletStores: Map<UserWalletId, List<WalletStoreModel>>,
        state: WalletSelectorState,
    ) {
        if (updatedWalletStores.isNotEmpty()) {
            scope.launch(Dispatchers.Default) {
                val updatedWallets = state.wallets.calculateBalanceAndUpdateWalletStores(updatedWalletStores)
                if (updatedWallets != state.wallets) {
                    store.dispatchOnMain(WalletSelectorAction.BalancesLoaded(updatedWallets))
                }
            }
        }
    }

    private fun unlockWallets() {
        Analytics.send(MyWallets.Button.UnlockWithBiometrics())

        scope.launch {
            userWalletsListManager.unlockIfLockable()
                .doOnFailure { error ->
                    Timber.e(error, "Unable to unlock all user wallets")
                    store.dispatchOnMain(WalletSelectorAction.UnlockWithBiometry.Error(error))
                }
                .doOnSuccess {
                    store.dispatchOnMain(WalletSelectorAction.UnlockWithBiometry.Success)
                }
        }
    }

    private fun addWallet() = scope.launch {
        Analytics.send(MyWallets.Button.ScanNewCard())

        val cardSdkConfigRepository = store.state.daggerGraphState.get(DaggerGraphState::cardSdkConfigRepository)

        val prevUseBiometricsForAccessCode = cardSdkConfigRepository.isBiometricsRequestPolicy()

        // Update access code policy for access code saving when a card was scanned
        cardSdkConfigRepository.setAccessCodeRequestPolicy(
            isBiometricsRequestPolicy = preferencesStorage.shouldSaveAccessCodes,
        )

        store.state.daggerGraphState.get(DaggerGraphState::scanCardProcessor).scan(
            analyticsEvent = Basic.CardWasScanned(AnalyticsParam.ScannedFrom.MyWallets),
            onWalletNotCreated = {
                // No need to rollback policy, continue with the policy set before the card scan
                store.dispatchOnMain(WalletSelectorAction.AddWallet.Success)
                store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Wallet))
            },
            disclaimerWillShow = {
                store.dispatchOnMain(NavigationAction.PopBackTo())
            },
            onSuccess = { scanResponse ->
                saveUserWalletAndPopBackToWalletScreen(scanResponse)
                    .doOnFailure { error ->
                        // Rollback policy if card saving was failed
                        cardSdkConfigRepository.setAccessCodeRequestPolicy(prevUseBiometricsForAccessCode)
                        Timber.e(error, "Unable to save user wallet")
                        store.dispatchOnMain(WalletSelectorAction.AddWallet.Error(error))
                    }
            },
            onFailure = { error ->
                // Rollback policy if card scanning was failed
                cardSdkConfigRepository.setAccessCodeRequestPolicy(prevUseBiometricsForAccessCode)
                Timber.e(error, "Unable to scan card")
                store.dispatchOnMain(WalletSelectorAction.AddWallet.Error(error))
            },
        )
    }

    private suspend fun saveUserWalletAndPopBackToWalletScreen(scanResponse: ScanResponse): CompletionResult<Unit> {
        val userWallet = UserWalletBuilder(scanResponse).build()
            ?: return CompletionResult.Failure(TangemSdkError.WalletIsNotCreated())

        return userWalletsListManager.save(userWallet)
            .doOnSuccess {
                store.dispatchOnMain(WalletSelectorAction.AddWallet.Success)
                store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Wallet))
                store.onUserWalletSelected(userWallet)
            }
    }

    private fun selectWallet(userWalletId: UserWalletId) {
        scope.launch {
            userWalletsListManager.get(userWalletId)
                .flatMap { userWallet ->
                    if (userWallet.isLocked) {
                        unlockUserWalletWithScannedCard(userWallet)
                    } else {
                        userWalletsListManager.select(userWalletId)
                    }
                }
                .doOnFailure { error ->
                    Timber.e(
                        error,
                        """
                            Unable to select user wallet
                            |- Wallet ID to select: $userWalletId
                        """.trimIndent(),
                    )
                }
                .doOnSuccess {
                    val selectedUserWallet = userWalletsListManager.selectedUserWalletSync
                    if (selectedUserWallet != null) {
                        store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Wallet))
                        store.onUserWalletSelected(
                            userWallet = selectedUserWallet,
                            sendAnalyticsEvent = true,
                        )
                    }
                }
        }
    }

    private suspend fun unlockUserWalletWithScannedCard(userWallet: UserWallet): CompletionResult<Unit> {
        Analytics.send(MyWallets.Button.WalletUnlockTapped())
        tangemSdkManager.changeDisplayedCardIdNumbersCount(userWallet.scanResponse)
        return store.state.daggerGraphState.get(DaggerGraphState::scanCardProcessor).scan()
            .map { scanResponse ->
                val scannedUserWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()
                if (scannedUserWalletId == userWallet.walletId) {
                    userWallet.updateCardWallets(scanResponse)
                } else {
// [REDACTED_TODO_COMMENT]
                    Timber.e(
                        """
                            Unable to unlock and select user wallet
                            |- Excepted ID: ${userWallet.walletId}
                            |- Received ID: $scannedUserWalletId
                        """.trimIndent(),
                    )
                    error("Wrong card")
                }
            }
            .flatMap { updatedUserWallet ->
                userWalletsListManager.save(updatedUserWallet, canOverride = true)
            }
            .doOnFailure {
                tangemSdkManager.changeDisplayedCardIdNumbersCount(
                    scanResponse = userWalletsListManager.selectedUserWalletSync?.scanResponse,
                )
            }
    }

    private fun UserWallet.updateCardWallets(scanResponse: ScanResponse): UserWallet {
        return this.copy(
            scanResponse = this.scanResponse.copy(
                card = this.scanResponse.card.copy(
                    wallets = scanResponse.card.wallets,
                ),
            ),
        )
    }

    private fun deleteWallets(userWalletsIds: List<UserWalletId>, state: WalletSelectorState) {
        Analytics.send(MyWallets.Button.DeleteWalletTapped())

        scope.launch {
            when (userWalletsIds.size) {
                state.wallets.size -> clearUserWalletsAndPopBackToHomeScreen()
                else -> deleteUserWallets(
                    userWalletsIds = userWalletsIds,
                    currentSelectedWalletId = state.selectedWalletId,
                )
            }
                .doOnFailure { error ->
                    Timber.e(
                        error,
                        """
                            Unable to delete user wallets
                            |- Wallets IDs to delete: $userWalletsIds
                            |- Current wallets IDs: ${state.wallets.map { it.id }}
                        """.trimIndent(),
                    )
                }
        }
    }

    private fun renameWallet(userWalletId: UserWalletId, newName: String) {
        scope.launch {
            userWalletsListManager.update(userWalletId) { it.copy(name = newName) }
                .doOnFailure { error ->
                    Timber.e(
                        error,
                        """
                            Unable to rename user wallet
                            |- Wallet ID: $userWalletId
                            |- New name: $newName
                        """.trimIndent(),
                    )
                }
        }
    }

    private fun refreshUserWalletsAmounts() {
        scope.launch {
            walletStoresManager.updateAmounts(
                userWallets = userWalletsListManager.userWallets.firstOrNull().orEmpty(),
            )
                .doOnFailure { error ->
                    Timber.e(error, "Unable to refresh user wallets amounts")
                }
        }
    }

    private fun clearUserWalletsAndCloseError() = scope.launch {
        clearUserWallets()
            .doOnSuccess {
                store.dispatchWithMain(WalletSelectorAction.CloseError)
                popBackToHome()
            }
            .doOnFailure { e ->
                Timber.e(e, "Unable to clear user wallets")
            }
    }

    private suspend fun clearUserWalletsAndPopBackToHomeScreen(): CompletionResult<Unit> {
        return clearUserWallets()
            .doOnSuccess { popBackToHome() }
    }

    private suspend fun clearUserWallets(): CompletionResult<Unit> {
        return userWalletsListManager.clear()
            .flatMap { walletStoresManager.clear() }
            .flatMap { tangemSdkManager.clearSavedUserCodes() }
    }

    private suspend fun popBackToHome() {
        // !!! Workaround !!!
        store.dispatchWithMain(NavigationAction.PopBackTo(AppScreen.Wallet))
        delay(timeMillis = 280)
        store.dispatchWithMain(NavigationAction.PopBackTo(AppScreen.Home))
    }

    private suspend fun deleteUserWallets(
        userWalletsIds: List<UserWalletId>,
        currentSelectedWalletId: UserWalletId?,
    ): CompletionResult<Unit> {
        return userWalletsListManager.delete(userWalletsIds)
            .flatMap { walletStoresManager.delete(userWalletsIds) }
            .flatMap { deleteAccessCodes(userWalletsIds) }
            .doOnSuccess {
                val selectedUserWallet = userWalletsListManager.selectedUserWalletSync

                when {
                    selectedUserWallet == null -> {
                        store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Welcome))
                    }
                    currentSelectedWalletId != selectedUserWallet.walletId -> {
                        store.onUserWalletSelected(
                            userWallet = selectedUserWallet,
                            sendAnalyticsEvent = true,
                        )
                    }
                }
            }
    }

    private suspend fun deleteAccessCodes(userWalletsIds: List<UserWalletId>): CompletionResult<Unit> {
        val cardsIds = userWalletsListManager.userWallets.firstOrNull()
            ?.asSequence()
            ?.filter { it.walletId in userWalletsIds }
            ?.flatMap { it.cardsInWallet }
            ?.toSet()

        return if (cardsIds.isNullOrEmpty()) {
            CompletionResult.Success(Unit)
        } else {
            tangemSdkManager.deleteSavedUserCodes(cardsIds.toSet())
        }
    }

    private suspend fun List<UserWalletModel>.calculateBalanceAndUpdateWalletStores(
        walletStores: Map<UserWalletId, List<WalletStoreModel>>,
    ): List<UserWalletModel> {
        return this
            .associateWith { walletStores[it.id] }
            .map { (wallet, walletStores) ->
                wallet.calculateBalanceAndUpdateWalletStores(walletStores)
            }
    }

    private suspend fun UserWalletModel.calculateBalanceAndUpdateWalletStores(
        walletStores: List<WalletStoreModel>?,
    ): UserWalletModel {
        return this.copy(
            type = when (type) {
                is UserWalletModel.Type.MultiCurrency -> type.copy(
                    tokensCount = walletStores?.flatMap { it.walletsData }?.size ?: 0,
                )
                is UserWalletModel.Type.SingleCurrency -> type
            },
            fiatBalance = totalFiatBalanceCalculator.calculate(
                walletStores = walletStores.orEmpty(),
                initial = TotalFiatBalance.Loading,
            ),
        )
    }
}
