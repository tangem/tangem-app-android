package com.tangem.tap.features.saveWallet.redux

import com.tangem.common.CompletionResult
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.flatMap
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.builders.UserWalletBuilder
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userWalletsListManager
import kotlinx.coroutines.launch
import org.rekotlin.Middleware

internal class SaveWalletMiddleware {
    val middleware: Middleware<AppState> = { _, stateProvider ->
        { next ->
            { action ->
                val state = stateProvider()
                if (action is SaveWalletAction && state != null) {
                    handleAction(action, state.saveWalletState)
                }
                next(action)
            }
        }
    }

    private fun handleAction(action: SaveWalletAction, state: SaveWalletState) {
        when (action) {
            is SaveWalletAction.Save -> saveWalletIfBiometricsEnrolled(state)
            is SaveWalletAction.Save.Success -> popBack()
            is SaveWalletAction.EnrollBiometrics.Enroll -> enrollBiometrics()
            is SaveWalletAction.SaveWalletWasShown -> saveWalletWasShown()
            is SaveWalletAction.ProvideAdditionalInfo,
            is SaveWalletAction.Dismiss,
            is SaveWalletAction.CloseError,
            is SaveWalletAction.Save.Error,
            is SaveWalletAction.EnrollBiometrics,
            is SaveWalletAction.EnrollBiometrics.Cancel,
            -> Unit
        }
    }

    private fun enrollBiometrics() {
        store.dispatchOnMain(NavigationAction.OpenBiometricsSettings)
    }

    private fun saveWalletIfBiometricsEnrolled(state: SaveWalletState) {
        if (tangemSdkManager.canEnrollBiometrics) {
            store.dispatchOnMain(SaveWalletAction.EnrollBiometrics)
        } else {
            saveWallet(state)
        }
    }

    private fun saveWallet(state: SaveWalletState) {
        val scanResponse = state.additionalInfo?.scanResponse
            ?: store.state.globalState.scanResponse
            ?: return

        scope.launch {
            val userWallet = UserWalletBuilder(scanResponse)
                .setBackupCardsIds(backupCardsIds = state.additionalInfo?.backupCardsIds)
                .build()

            userWalletsListManager.save(userWallet)
                .flatMap {
                    trySaveAccessCode(
                        userWallet = userWallet,
                        additionalInfo = state.additionalInfo,
                    )
                }
                .doOnFailure { error ->
                    store.dispatchOnMain(SaveWalletAction.Save.Error(error))
                }
                .doOnSuccess {
                    preferencesStorage.shouldSaveUserWallets = true
                    preferencesStorage.shouldSaveAccessCodes = true
                    store.dispatchOnMain(SaveWalletAction.Save.Success)
                    store.onUserWalletSelected(userWallet)
                }
        }
    }

    private fun popBack() {
        store.dispatchOnMain(NavigationAction.PopBackTo())
    }

    private fun saveWalletWasShown() {
        preferencesStorage.shouldShowSaveWallet = false
    }

    private suspend fun trySaveAccessCode(
        userWallet: UserWallet,
        additionalInfo: SaveWalletState.WalletAdditionalInfo?,
    ): CompletionResult<Unit> {
        return when {
            additionalInfo?.accessCode != null -> {
                tangemSdkManager.saveAccessCode(
                    accessCode = additionalInfo.accessCode,
                    cardsIds = userWallet.cardsInWallet,
                )
            }
            else -> {
                CompletionResult.Success(Unit)
            }
        }
    }
}