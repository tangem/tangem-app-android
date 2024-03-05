package com.tangem.tap.features.onboarding.products.otherCards.redux

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.postUi
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.OnboardingDialog
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.mainScope
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware

object OnboardingOtherCardsMiddleware {
    val handler = onboardingOtherCardsMiddleware
}

private val onboardingOtherCardsMiddleware: Middleware<AppState> = { dispatch, _ ->
    { next ->
        { action ->
            handleOtherCardsAction(action)
            next(action)
        }
    }
}

@Suppress("LongMethod", "ComplexMethod", "MagicNumber")
private fun handleOtherCardsAction(action: Action) {
    if (action !is OnboardingOtherCardsAction) return
    val globalState = store.state.globalState
    val onboardingManager = globalState.onboardingState.onboardingManager ?: return

    val card = onboardingManager.scanResponse.card

    when (action) {
        is OnboardingOtherCardsAction.Init -> {
            scope.launch {
                if (!onboardingManager.isActivationStarted(card.cardId)) {
                    Analytics.send(Onboarding.Started())
                }
            }
        }
        is OnboardingOtherCardsAction.LoadCardArtwork -> {
            scope.launch {
                val artworkUrl = onboardingManager.loadArtworkUrl()
                withMainContext { store.dispatch(OnboardingOtherCardsAction.SetArtworkUrl(artworkUrl)) }
            }
        }
        is OnboardingOtherCardsAction.DetermineStepOfScreen -> {
            val step = when {
                card.wallets.isEmpty() -> OnboardingOtherCardsStep.CreateWallet
                else -> OnboardingOtherCardsStep.Done
            }
            store.dispatch(OnboardingOtherCardsAction.SetStepOfScreen(step))
        }
        is OnboardingOtherCardsAction.SetStepOfScreen -> {
            when (action.step) {
                OnboardingOtherCardsStep.CreateWallet -> {
                    Analytics.send(Onboarding.CreateWallet.ScreenOpened())
                }
                OnboardingOtherCardsStep.Done -> {
                    Analytics.send(Onboarding.Finished())
                    mainScope.launch {
                        onboardingManager.finishActivation(card.cardId)
                        postUi(200) { store.dispatch(OnboardingOtherCardsAction.Confetti.Show) }
                    }
                }
                else -> Unit
            }
        }
        is OnboardingOtherCardsAction.CreateWallet -> {
            scope.launch {
                val result = tangemSdkManager.createProductWallet(
                    scanResponse = onboardingManager.scanResponse,
                    shouldReset = globalState.onboardingState.shouldResetOnCreate,
                )
                withMainContext {
                    when (result) {
                        is CompletionResult.Success -> {
                            Analytics.send(Onboarding.CreateWallet.WalletCreatedSuccessfully())
                            val updatedResponse = onboardingManager.scanResponse.copy(
                                card = result.data.card,
                            )
                            val updatedCard = updatedResponse.card
                            onboardingManager.scanResponse = updatedResponse
                            onboardingManager.startActivation(updatedCard.cardId)

                            delay(DELAY_SDK_DIALOG_CLOSE)
                            store.dispatch(OnboardingOtherCardsAction.SetStepOfScreen(OnboardingOtherCardsStep.Done))
                        }
                        is CompletionResult.Failure -> {
                            if (result.error is TangemSdkError.WalletAlreadyCreated) {
                                handleActivationError()
                            }
                        }
                    }
                }
            }
        }
        OnboardingOtherCardsAction.Done -> {
            store.dispatch(GlobalAction.Onboarding.Stop)
            OnboardingHelper.trySaveWalletAndNavigateToWalletScreen(onboardingManager.scanResponse)
        }
        OnboardingOtherCardsAction.OnBackPressed -> {
            OnboardingHelper.onInterrupted()
        }
        else -> Unit
    }
}

private fun handleActivationError() {
    store.dispatchDialogShow(
        OnboardingDialog.WalletActivationError(
            onConfirm = {
                store.dispatch(GlobalAction.Onboarding.ShouldResetCardOnCreate(true))
            },
        ),
    )
}