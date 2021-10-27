package com.tangem.tap.features.onboarding.products.twins.redux

import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.guard
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.postUi
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.extensions.makePrimaryWalletManager
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.domain.twins.TwinCardsManager
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware

class TwinCardsMiddleware {
    companion object {
        val handler = twinsWalletMiddleware
    }
}

private val twinsWalletMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            handle(action, dispatch)
            next(action)
        }
    }
}

private fun handle(action: Action, dispatch: DispatchFunction) {
    val action = action as? TwinCardsAction ?: return

    val globalState = store.state.globalState
    val onboardingManager = globalState.onboardingManager
    val twinCardsState = store.state.twinCardsState

    fun getScanResponse(): ScanResponse {
        return when (twinCardsState.mode) {
            CreateTwinWalletMode.CreateWallet -> onboardingManager?.scanResponse
            CreateTwinWalletMode.RecreateWallet -> globalState.scanResponse
        } ?: throw NullPointerException("ScanResponse can't be NULL")
    }

    fun updateScanResponse(response: ScanResponse) {
        when (twinCardsState.mode) {
            CreateTwinWalletMode.CreateWallet -> onboardingManager?.scanResponse = response
            CreateTwinWalletMode.RecreateWallet -> store.dispatch(GlobalAction.SaveScanNoteResponse(response))
        }
    }

    fun startCardActivation(cardId: String) {
        if (twinCardsState.mode == CreateTwinWalletMode.CreateWallet) {
            onboardingManager?.activationStarted(cardId)
        }
    }

    fun finishCardActivation() {
        if (twinCardsState.mode == CreateTwinWalletMode.CreateWallet) {
            onboardingManager?.activationFinished(getScanResponse().card.cardId)
            twinCardsState.pairCardId?.let { onboardingManager?.activationFinished(it) }
        }
    }

    when (action) {
        is TwinCardsAction.Init -> {
            if (twinCardsState.currentStep == TwinCardsStep.WelcomeOnly) return

            when (twinCardsState.mode) {
                CreateTwinWalletMode.CreateWallet -> {
                    if (preferencesStorage.wasTwinsOnboardingShown()) {
                        val step = when {
                            !getScanResponse().twinsIsTwinned() -> TwinCardsStep.CreateFirstWallet
                            twinCardsState.walletBalance.balanceIsToppedUp() -> TwinCardsStep.Done
                            else -> TwinCardsStep.TopUpWallet
                        }
                        store.dispatch((TwinCardsAction.SetStepOfScreen(step)))
                    } else {
                        store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.Welcome))
                    }
                }
                CreateTwinWalletMode.RecreateWallet -> {
                    store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.Warning))
                }
            }
        }
        is TwinCardsAction.Wallet.HandleOnBackPressed -> {
            val shouldReturnCardBack = twinCardsState.mode == CreateTwinWalletMode.CreateWallet
                    && twinCardsState.currentStep != TwinCardsStep.TopUpWallet
                    && twinCardsState.currentStep != TwinCardsStep.Done

            if (twinCardsState.showAlert) {
                val onInterruptPrompt = {
                    store.dispatch(TwinCardsAction.CardsManager.Release)
                    action.shouldResetTwinCardsWidget(shouldReturnCardBack) {
                        store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
                    }
                }
                val stateDialog = TwinCardsAction.Wallet.ShowInterruptDialog(onInterruptPrompt)
                store.dispatchDialogShow(stateDialog)
            } else {
                store.dispatch(TwinCardsAction.CardsManager.Release)
                action.shouldResetTwinCardsWidget(shouldReturnCardBack) {
                    store.dispatch(NavigationAction.PopBackTo())
                }
            }
        }
        is TwinCardsAction.SetStepOfScreen -> {
            when (action.step) {
                TwinCardsStep.WelcomeOnly -> preferencesStorage.saveTwinsOnboardingShown()
                TwinCardsStep.Welcome -> preferencesStorage.saveTwinsOnboardingShown()
                TwinCardsStep.TopUpWallet -> store.dispatch(TwinCardsAction.Balance.Update)
                TwinCardsStep.Done -> {
                    finishCardActivation()
                    postUi(500) { store.dispatch(TwinCardsAction.Confetti.Show) }
                }
            }
        }
        is TwinCardsAction.Wallet.LaunchFirstStep -> {
            val manager = TwinCardsManager(getScanResponse().card, action.reader)
            store.dispatch(TwinCardsAction.CardsManager.Set(manager))

            scope.launch {
                when (val result = manager.createFirstWallet(action.initialMessage)) {
                    is CompletionResult.Success -> {
                        startCardActivation(result.data.cardId)
                        delay(DELAY_SDK_DIALOG_CLOSE)
                        withMainContext {
                            store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.CreateSecondWallet))
                        }
                    }
                    is CompletionResult.Failure -> {
                    }
                }
            }
        }
        is TwinCardsAction.Wallet.LaunchSecondStep -> {
            val manager = twinCardsState.twinCardsManager ?: return

            scope.launch {
                val result = manager.createSecondWallet(
                    action.initialMessage,
                    action.preparingMessage,
                    action.creatingWalletMessage
                )
                when (result) {
                    is CompletionResult.Success -> {
                        startCardActivation(result.data.cardId)
                        delay(DELAY_SDK_DIALOG_CLOSE)
                        withMainContext {
                            store.dispatch(TwinCardsAction.SetPairCardId(result.data.cardId))
                            store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.CreateThirdWallet))
                        }
                    }
                    is CompletionResult.Failure -> {
                    }
                }
            }
        }
        is TwinCardsAction.Wallet.LaunchThirdStep -> {
            val manager = twinCardsState.twinCardsManager ?: return

            scope.launch {
                when (val result = manager.complete(action.message)) {
                    is Result.Success -> {
                        updateScanResponse(result.data)
                        delay(DELAY_SDK_DIALOG_CLOSE)
                        withMainContext { store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.TopUpWallet)) }
                    }
                    is Result.Failure -> {
                    }
                }
            }
        }
        is TwinCardsAction.Balance.Update -> {
            val walletManager = if (twinCardsState.walletManager != null) {
                twinCardsState.walletManager
            } else {
                val wmFactory = globalState.tapWalletManager.walletManagerFactory
                val walletManager = wmFactory.makePrimaryWalletManager(getScanResponse()).guard {
                    val message = "Loading cancelled. Cause: wallet manager didn't created"
                    val customError = TapError.CustomError(message)
                    store.dispatchErrorNotification(customError)
                    return
                }
                dispatch(TwinCardsAction.SetWalletManager(walletManager))
                walletManager
            }
            val isLoadedBefore = twinCardsState.walletBalance.state != ProgressState.Loading
            val balanceIsLoading = twinCardsState.walletBalance.copy(
                currency = Currency.Blockchain(walletManager.wallet.blockchain),
                state = ProgressState.Loading,
                error = null,
                criticalError = null
            )
            store.dispatch(TwinCardsAction.Balance.Set(balanceIsLoading))

            scope.launch {
                val loadedBalance = onboardingManager?.updateBalance(walletManager) ?: return@launch
                loadedBalance.criticalError?.let { store.dispatchErrorNotification(it) }
                delay(if (isLoadedBefore) 0 else 300)
                withMainContext { store.dispatch(TwinCardsAction.Balance.Set(loadedBalance)) }
            }

        }
        is TwinCardsAction.Balance.Set -> {
            if (action.balance.balanceIsToppedUp()) {
                scope.launch {
                    withMainContext { store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.Done)) }
                }
            }
        }
        is TwinCardsAction.ShowAddressInfoDialog -> {
            val addressData = twinCardsState.walletManager?.getAddressData() ?: return

            val addressWasCopied = globalState.resources.strings.addressWasCopied
            val appDialog = AppDialog.AddressInfoDialog(
                addressData,
                onCopyAddress = { store.dispatchToastNotification(addressWasCopied) },
                onExploreAddress = { store.dispatchOpenUrl(addressData.exploreUrl) }
            )
            store.dispatchDialogShow(appDialog)
        }
        is TwinCardsAction.TopUp -> {
            val topUpUrl = twinCardsState.walletManager?.getToUpUrl() ?: return
            store.dispatchOpenUrl(topUpUrl)
        }
        TwinCardsAction.Done -> {
            scope.launch {
                val scanResponse = getScanResponse()
                store.onCardScanned(scanResponse)
                withMainContext {
                    when (twinCardsState.mode) {
                        CreateTwinWalletMode.CreateWallet -> {
                            store.dispatch(GlobalAction.Onboarding.Stop)
                            store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet))
                        }
                        CreateTwinWalletMode.RecreateWallet -> {
                            store.dispatch(NavigationAction.PopBackTo())
                        }
                    }
                }
            }
        }
    }
}
