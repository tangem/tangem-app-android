package com.tangem.tap.features.welcome.redux

import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnResult
import com.tangem.common.doOnSuccess
import com.tangem.common.flatMap
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.utils.popTo
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.card.analytics.ParamCardCurrencyConverter
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.legacy.UserWalletsListManager.Lockable.UnlockType
import com.tangem.domain.wallets.legacy.unlockIfLockable
import com.tangem.tap.*
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.proxy.redux.DaggerGraphState
import kotlinx.coroutines.launch
import org.rekotlin.Middleware
import timber.log.Timber

internal class WelcomeMiddleware {
    val middleware: Middleware<AppState> = { _, appStateProvider ->
        { next ->
            { action ->
                val appState = appStateProvider()
                if (action is WelcomeAction && appState != null) {
                    handleAction(action)
                }
                next(action)
            }
        }
    }

    private fun handleAction(action: WelcomeAction) {
        mainScope.launch {
            when (action) {
                is WelcomeAction.ProceedWithBiometrics -> proceedWithBiometrics()
                is WelcomeAction.ProceedWithCard -> proceedWithCard()
                is WelcomeAction.ClearUserWallets -> disableUserWalletsSaving()
                else -> Unit
            }
        }
    }

    private suspend fun proceedWithBiometrics() {
        val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)
        userWalletsListManager.unlockIfLockable(type = UnlockType.ANY)
            .doOnFailure { error ->
                Timber.e(error, "Unable to unlock user wallets with biometrics")
                store.dispatchWithMain(WelcomeAction.ProceedWithBiometrics.Error(error))
            }
            .doOnSuccess { selectedUserWallet ->
                sendSignedInAnalyticsEvent(
                    userWallet = selectedUserWallet,
                    signInType = Basic.SignedIn.SignInType.Biometric,
                )

                store.dispatchNavigationAction { replaceAll(AppRoute.Wallet) }
                store.dispatchWithMain(WelcomeAction.ProceedWithBiometrics.Success)
                store.onUserWalletSelected(userWallet = selectedUserWallet)
            }
    }

    private suspend fun proceedWithCard() {
        scanCardInternal { scanResponse ->
            val userWalletBuilder = store.inject(DaggerGraphState::coldUserWalletBuilderFactory).create(scanResponse)

            val userWallet = userWalletBuilder.build() ?: return@scanCardInternal

            val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)
            userWalletsListManager.save(userWallet, canOverride = true)
                .doOnFailure { error ->
                    Timber.e(error, "Unable to save user wallet")
                    store.dispatchWithMain(WelcomeAction.ProceedWithCard.Error(error))
                }
                .doOnSuccess {
                    sendSignedInAnalyticsEvent(userWallet, signInType = Basic.SignedIn.SignInType.Card)

                    store.dispatchNavigationAction { replaceAll(AppRoute.Wallet) }
                    store.dispatchWithMain(WelcomeAction.ProceedWithCard.Success)
                    store.onUserWalletSelected(userWallet = userWallet)
                }
        }
    }

    private fun sendSignedInAnalyticsEvent(userWallet: UserWallet, signInType: Basic.SignedIn.SignInType) {
        // TODO [REDACTED_TASK_KEY] [Hot Wallet] Analytics

        if (userWallet !is UserWallet.Cold) {
            return
        }

        val scanResponse = userWallet.scanResponse
        val currency = ParamCardCurrencyConverter().convert(
            value = scanResponse.cardTypesResolver,
        )
        Analytics.addContext(scanResponse)
        if (currency != null) {
            val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)

            Analytics.send(
                event = Basic.SignedIn(
                    currency = currency,
                    batch = scanResponse.card.batchId,
                    signInType = signInType,
                    walletsCount = userWalletsListManager.walletsCount.toString(),
                    hasBackup = scanResponse.card.backupStatus?.isActive,
                ),
            )
        }
    }

    private suspend fun disableUserWalletsSaving() {
        val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)
        userWalletsListManager.clear()
            .flatMap { tangemSdkManager.clearSavedUserCodes() }
            .doOnFailure { e ->
                Timber.e(e, "Unable to clear user wallets")
            }
            .doOnResult {
                store.dispatchWithMain(WelcomeAction.CloseError)
                store.dispatchNavigationAction { popTo<AppRoute.Home>() }
            }
    }

    private suspend inline fun scanCardInternal(crossinline onCardScanned: suspend (ScanResponse) -> Unit) {
        val shouldSaveAccessCodes = store.inject(DaggerGraphState::settingsRepository).shouldSaveAccessCodes()

        store.inject(DaggerGraphState::cardSdkConfigRepository).setAccessCodeRequestPolicy(
            isBiometricsRequestPolicy = shouldSaveAccessCodes,
        )

        store.inject(DaggerGraphState::scanCardProcessor).scan(
            analyticsSource = AnalyticsParam.ScreensSources.SignIn,
            onSuccess = { scanResponse ->
                scope.launch { onCardScanned(scanResponse) }
            },
            onFailure = {
                when (it) {
                    is TangemSdkError.ExceptionError -> {
                        store.dispatchOnMain(WelcomeAction.ProceedWithCard.Success)
                    }
                    else -> {
                        store.dispatchOnMain(WelcomeAction.ProceedWithCard.Error(it))
                    }
                }
            },
            onProgressStateChange = {
                store.dispatchWithMain(WelcomeAction.ProceedWithCard.ChangeProgress(it))
            },
            onWalletNotCreated = {
                store.dispatchOnMain(WelcomeAction.ProceedWithCard.Success)
            },
        )
    }
}