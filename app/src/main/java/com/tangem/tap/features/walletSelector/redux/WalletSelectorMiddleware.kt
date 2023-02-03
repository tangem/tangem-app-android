package com.tangem.tap.features.walletSelector.redux

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.flatMap
import com.tangem.common.map
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.common.analytics.events.MyWallets
import com.tangem.tap.common.analytics.paramsInterceptor.BatchIdParamsInterceptor
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.model.builders.UserWalletBuilder
import com.tangem.tap.domain.model.builders.UserWalletIdBuilder
import com.tangem.tap.domain.scanCard.ScanCardProcessor
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayActivationError
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.totalFiatBalanceCalculator
import com.tangem.tap.userTokensRepository
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.walletStoresManager
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
                unlockWalletsWithBiometry()
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
                val updatedWallets = state.wallets.updateWalletStoresAndCalculateFiatBalance(updatedWalletStores)
                if (updatedWallets != state.wallets) {
                    store.dispatchOnMain(WalletSelectorAction.BalancesLoaded(updatedWallets))
                }
            }
        }
    }

    private fun unlockWalletsWithBiometry() {
        Analytics.send(MyWallets.Button.UnlockWithBiometrics())

        scope.launch {
            userWalletsListManager.unlockWithBiometry()
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

        val prevUseBiometricsForAccessCode = tangemSdkManager.useBiometricsForAccessCode()

        // Update access code policy for access code saving when a card was scanned
        tangemSdkManager.setAccessCodeRequestPolicy(
            useBiometricsForAccessCode = preferencesStorage.shouldSaveAccessCodes,
        )

        ScanCardProcessor.scan(
            analyticsEvent = MyWallets.CardWasScanned(),
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
                        tangemSdkManager.setAccessCodeRequestPolicy(prevUseBiometricsForAccessCode)
                        Timber.e(error, "Unable to save user wallet")
                        store.dispatchOnMain(WalletSelectorAction.AddWallet.Error(error))
                    }
            },
            onFailure = { error ->
                // Rollback policy if card scanning was failed
                tangemSdkManager.setAccessCodeRequestPolicy(prevUseBiometricsForAccessCode)
                when {
                    error is TangemSdkError.ExceptionError && error.cause is SaltPayActivationError -> {
                        store.dispatchOnMain(WalletSelectorAction.AddWallet.Success)
                        store.dispatchOnMain(NavigationAction.PopBackTo())
                    }
                    else -> {
                        Timber.e(error, "Unable to scan card")
                        store.dispatchOnMain(WalletSelectorAction.AddWallet.Error(error))
                    }
                }
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
                        userWalletsListManager.selectWallet(userWalletId)
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
                        val batchId = selectedUserWallet.scanResponse.card.batchId
                        Analytics.addParamsInterceptor(BatchIdParamsInterceptor(batchId))

                        store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Wallet))
                        store.onUserWalletSelected(selectedUserWallet)
                    }
                }
        }
    }

    private suspend fun unlockUserWalletWithScannedCard(userWallet: UserWallet): CompletionResult<Unit> {
        Analytics.send(MyWallets.Button.WalletUnlockTapped())
        tangemSdkManager.changeDisplayedCardIdNumbersCount(userWallet.scanResponse)
        return tangemSdkManager.scanProduct(userTokensRepository)
            .map { scanResponse ->
                val scannedUserWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()
                if (scannedUserWalletId == userWallet.walletId) {
                    userWallet.updateCardWallets(scanResponse)
                } else {
                    // TODO: Display error
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
                state.wallets.size -> clearUserWallets()
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

    private suspend fun clearUserWallets(): CompletionResult<Unit> {
        return userWalletsListManager.clear()
            .flatMap { walletStoresManager.clear() }
            .flatMap { tangemSdkManager.clearSavedUserCodes() }
            .doOnSuccess {
                // !!! Workaround !!!
                store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Wallet))
                delay(timeMillis = 280)
                store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Home))
            }
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
                        store.onUserWalletSelected(selectedUserWallet)
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

    private suspend fun List<UserWalletModel>.updateWalletStoresAndCalculateFiatBalance(
        walletStores: Map<UserWalletId, List<WalletStoreModel>>,
    ): List<UserWalletModel> {
        return this
            .associateWith { walletStores[it.id] }
            .map { (wallet, walletStores) ->
                wallet.updateWalletStoresAndCalculateFiatBalance(walletStores)
            }
    }

    private suspend fun UserWalletModel.updateWalletStoresAndCalculateFiatBalance(
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
