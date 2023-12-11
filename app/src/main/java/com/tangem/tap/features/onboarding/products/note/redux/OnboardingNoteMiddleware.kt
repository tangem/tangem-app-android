package com.tangem.tap.features.onboarding.products.note.redux

import com.tangem.common.CompletionResult
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.extensions.makePrimaryWalletManager
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.entities.ProgressState
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.postUi
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.features.onboarding.OnboardingDialog
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware

object OnboardingNoteMiddleware {
    val handler = onboardingNoteMiddleware
}

private val onboardingNoteMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            handleNoteAction(state, action, dispatch)
            next(action)
        }
    }
}

@Suppress("LongMethod", "ComplexMethod", "MagicNumber")
private fun handleNoteAction(appState: () -> AppState?, action: Action, dispatch: DispatchFunction) {
    if (action !is OnboardingNoteAction) return
    if (DemoHelper.tryHandle(appState, action)) return

    val globalState = store.state.globalState
    val onboardingManager = globalState.onboardingState.onboardingManager ?: return

    val scanResponse = onboardingManager.scanResponse
    val card = onboardingManager.scanResponse.card
    val noteState = store.state.onboardingNoteState

    when (action) {
        is OnboardingNoteAction.Init -> {
            if (!onboardingManager.isActivationStarted(card.cardId)) {
                Analytics.send(Onboarding.Started())
            }
        }
        is OnboardingNoteAction.LoadCardArtwork -> {
            scope.launch {
                val artworkUrl = onboardingManager.loadArtworkUrl()
                withMainContext { store.dispatch(OnboardingNoteAction.SetArtworkUrl(artworkUrl)) }
            }
        }
        is OnboardingNoteAction.DetermineStepOfScreen -> {
            val step = when {
                card.wallets.isEmpty() -> OnboardingNoteStep.CreateWallet
                noteState.walletBalance.balanceIsToppedUp() -> OnboardingNoteStep.Done
                else -> OnboardingNoteStep.TopUpWallet
            }
            store.dispatch(OnboardingNoteAction.SetStepOfScreen(step))
        }
        is OnboardingNoteAction.SetStepOfScreen -> {
            when (action.step) {
                OnboardingNoteStep.CreateWallet -> {
                    Analytics.send(Onboarding.CreateWallet.ScreenOpened())
                }
                OnboardingNoteStep.TopUpWallet -> {
                    Analytics.send(Onboarding.Topup.ScreenOpened())
                    store.dispatch(OnboardingNoteAction.Balance.Update)
                }
                OnboardingNoteStep.Done -> {
                    Analytics.send(Onboarding.Finished())
                    onboardingManager.activationFinished(card.cardId)
                    postUi(DELAY_SDK_DIALOG_CLOSE) { store.dispatch(OnboardingNoteAction.Confetti.Show) }
                }
                else -> Unit
            }
        }
        is OnboardingNoteAction.CreateWallet -> {
            scope.launch {
                val result = tangemSdkManager.createProductWallet(scanResponse)
                withMainContext {
                    when (result) {
                        is CompletionResult.Success -> {
                            Analytics.send(Onboarding.CreateWallet.WalletCreatedSuccessfully())
                            val updatedResponse = scanResponse.copy(card = result.data.card)
                            onboardingManager.scanResponse = updatedResponse
                            onboardingManager.activationStarted(updatedResponse.card.cardId)
                            store.state.globalState.topUpController?.registerEmptyWallet(updatedResponse)
                            store.dispatch(OnboardingNoteAction.SetStepOfScreen(OnboardingNoteStep.TopUpWallet))
                        }
                        is CompletionResult.Failure -> Unit
                    }
                }
            }
        }
        is OnboardingNoteAction.Balance.Update -> {
            val walletManager = if (noteState.walletManager != null) {
                noteState.walletManager
            } else {
                val wmFactory = globalState.tapWalletManager.walletManagerFactory
                val walletManager = wmFactory.makePrimaryWalletManager(scanResponse).guard {
                    val message = "Loading cancelled. Cause: wallet manager didn't created"
                    val customError = TapError.CustomError(message)
                    store.dispatchErrorNotification(customError)
                    return
                }
                dispatch(OnboardingNoteAction.SetWalletManager(walletManager))
                walletManager
            }

            val isLoadedBefore = noteState.walletBalance.state != ProgressState.Loading
            val balanceIsLoading = noteState.walletBalance.copy(
                currency = Currency.Blockchain(
                    walletManager.wallet.blockchain,
                    walletManager.wallet.publicKey.derivationPath?.rawPath,
                ),
                state = ProgressState.Loading,
                error = null,
                criticalError = null,
            )
            store.dispatch(OnboardingNoteAction.Balance.Set(balanceIsLoading))

            scope.launch {
                val loadedBalance = onboardingManager.updateBalance(walletManager)
                delay(if (isLoadedBefore) 0 else 300)
                loadedBalance.criticalError?.let { store.dispatchErrorNotification(it) }
                withMainContext {
                    store.dispatch(OnboardingNoteAction.Balance.Set(loadedBalance))
                    store.dispatch(OnboardingNoteAction.Balance.SetCriticalError(loadedBalance.criticalError))
                    store.dispatch(OnboardingNoteAction.Balance.SetNonCriticalError(loadedBalance.error))
                }
            }
        }
        is OnboardingNoteAction.Balance.Set -> {
            if (action.balance.balanceIsToppedUp()) {
                OnboardingHelper.sendToppedUpEvent(scanResponse)

                store.state.globalState.topUpController?.send(scanResponse, AnalyticsParam.CardBalanceState.Full)
                store.dispatch(OnboardingNoteAction.SetStepOfScreen(OnboardingNoteStep.Done))
            }
        }
        is OnboardingNoteAction.ShowAddressInfoDialog -> {
            val addressData = noteState.walletManager?.getAddressData() ?: return

            Analytics.send(Onboarding.Topup.ButtonShowWalletAddress())
            val appDialog = AppDialog.AddressInfoDialog(noteState.walletBalance.currency, addressData)
            store.dispatchDialogShow(appDialog)
        }
        is OnboardingNoteAction.TopUp -> {
            val walletManager = noteState.walletManager.guard {
                store.dispatchDebugErrorNotification("NPE: WalletManager")
                return
            }

            val topUpUrl = walletManager.getTopUpUrl() ?: return
            val blockchain = walletManager.wallet.blockchain

            val currencyType = AnalyticsParam.CurrencyType.Blockchain(blockchain)
            Analytics.send(Onboarding.Topup.ButtonBuyCrypto(currencyType))

            if (globalState.userCountryCode == RUSSIA_COUNTRY_CODE) {
                val dialogData = WalletDialog.RussianCardholdersWarningDialog.Data(topUpUrl)
                store.dispatchOnMain(WalletAction.DialogAction.RussianCardholdersWarningDialog(dialogData))
            } else {
                store.dispatchOpenUrl(topUpUrl)
            }
        }
        is OnboardingNoteAction.Done -> {
            store.dispatch(GlobalAction.Onboarding.Stop)
            OnboardingHelper.trySaveWalletAndNavigateToWalletScreen(scanResponse)
        }
        is OnboardingNoteAction.OnBackPressed -> {
            store.dispatchDialogShow(
                OnboardingDialog.InterruptOnboarding(
                    onOk = {
                        OnboardingHelper.onInterrupted()
                        store.dispatch(NavigationAction.PopBackTo())
                    },
                ),
            )
        }
        else -> Unit
    }
}
