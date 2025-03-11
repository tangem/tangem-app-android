package com.tangem.tap.features.welcome.redux

import android.content.Intent
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

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListManager.Lockable.UnlockType
import com.tangem.domain.wallets.legacy.unlockIfLockable
import com.tangem.tap.backupService
import com.tangem.tap.common.analytics.converters.ParamCardCurrencyConverter
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.intentHandler.handlers.BackgroundScanIntentHandler
import com.tangem.tap.features.intentHandler.handlers.WalletConnectLinkIntentHandler
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.rekotlin.Middleware
import timber.log.Timber

internal class WelcomeMiddleware {
    val middleware: Middleware<AppState> = { _, appStateProvider ->
        { next ->
            { action ->
                val appState = appStateProvider()
                if (action is WelcomeAction && appState != null) {
                    handleAction(action, appState.welcomeState)
                }
                next(action)
            }
        }
    }

    private fun handleAction(action: WelcomeAction, state: WelcomeState) {
        state.scope?.launch {
            when (action) {
                is WelcomeAction.ProceedWithIntent -> proceedWithIntent(action.intent, scope = this)
                is WelcomeAction.ProceedWithBiometrics -> proceedWithBiometrics(
                    afterUnlockIntent = action.afterUnlockIntent ?: state.intent,
                )
                is WelcomeAction.ProceedWithCard -> proceedWithCard(afterScanIntent = state.intent)
                is WelcomeAction.ClearUserWallets -> disableUserWalletsSaving()
                else -> Unit
            }
        }
    }

    private suspend fun proceedWithIntent(initialIntent: Intent, scope: CoroutineScope) {
        Timber.d(
            """
                Proceeding with intent
                |- Intent: $initialIntent
            """.trimIndent(),
        )

        val handler = BackgroundScanIntentHandler(
            scope = scope,
            hasSavedUserWalletsProvider = { true },
        )
        val isBackgroundScanHandled = handler.handleIntent(initialIntent, isFromForeground = false)
        val hasUncompletedBackup = backupService.hasIncompletedBackup

        if (!isBackgroundScanHandled && !hasUncompletedBackup) {
            store.dispatchWithMain(WelcomeAction.ProceedWithBiometrics(initialIntent))
        }
    }

    private suspend fun proceedWithBiometrics(afterUnlockIntent: Intent?) {
        Timber.d(
            """
                Proceeding with biometry
                |- Intent: $afterUnlockIntent
            """.trimIndent(),
        )

        val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)
        userWalletsListManager.unlockIfLockable(type = UnlockType.ANY)
            .doOnFailure { error ->
                Timber.e(error, "Unable to unlock user wallets with biometrics")
                store.dispatchWithMain(WelcomeAction.ProceedWithBiometrics.Error(error))
            }
            .doOnSuccess { selectedUserWallet ->
                sendSignedInAnalyticsEvent(
                    scanResponse = selectedUserWallet.scanResponse,
                    signInType = Basic.SignedIn.SignInType.Biometric,
                )

                store.dispatchNavigationAction { replaceAll(AppRoute.Wallet) }
                store.dispatchWithMain(WelcomeAction.ProceedWithBiometrics.Success)
                store.onUserWalletSelected(userWallet = selectedUserWallet)

                afterUnlockIntent?.let {
                    WalletConnectLinkIntentHandler().handleIntent(it, false)
                }
            }
    }

    private suspend fun proceedWithCard(afterScanIntent: Intent?) {
        Timber.d(
            """
                Proceeding with card
                |- Intent: $afterScanIntent
            """.trimIndent(),
        )

        scanCardInternal { scanResponse ->
            val walletNameGenerateUseCase = store.inject(DaggerGraphState::generateWalletNameUseCase)
            val userWallet = UserWalletBuilder(scanResponse, walletNameGenerateUseCase).build()
                ?: return@scanCardInternal

            val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)
            userWalletsListManager.save(userWallet, canOverride = true)
                .doOnFailure { error ->
                    Timber.e(error, "Unable to save user wallet")
                    store.dispatchWithMain(WelcomeAction.ProceedWithCard.Error(error))
                }
                .doOnSuccess {
                    sendSignedInAnalyticsEvent(scanResponse = scanResponse, signInType = Basic.SignedIn.SignInType.Card)

                    store.dispatchNavigationAction { replaceAll(AppRoute.Wallet) }
                    store.dispatchWithMain(WelcomeAction.ProceedWithCard.Success)
                    store.onUserWalletSelected(userWallet = userWallet)

                    afterScanIntent?.let {
                        WalletConnectLinkIntentHandler().handleIntent(it, false)
                    }
                }
        }
    }

    private fun sendSignedInAnalyticsEvent(scanResponse: ScanResponse, signInType: Basic.SignedIn.SignInType) {
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