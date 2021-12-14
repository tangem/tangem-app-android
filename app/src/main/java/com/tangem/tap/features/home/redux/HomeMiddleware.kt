package com.tangem.tap.features.home.redux

import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.extensions.onCardScanned
import com.tangem.tap.common.extensions.withMainContext
import com.tangem.tap.common.post
import com.tangem.tap.common.postUi
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.FragmentShareTransition
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsStep
import com.tangem.tap.features.send.redux.states.ButtonState
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.launch
import org.rekotlin.Middleware

class HomeMiddleware {
    companion object {
        val handler = homeMiddleware

        const val CARD_SHOP_URI = "http://cards.tangem.com/"
    }
}

private val homeMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            when (action) {
                is HomeAction.Init -> {
                    store.dispatch(GlobalAction.RestoreAppCurrency)
                    store.dispatch(GlobalAction.GetMoonPayStatus)
                    store.dispatch(HomeAction.SetTermsOfUseState(preferencesStorage.wasDisclaimerAccepted()))
                }
                is HomeAction.ShouldScanCardOnResume -> {
                    if (action.shouldScanCard) {
                        store.dispatch(HomeAction.ShouldScanCardOnResume(false))
                        postUi(700) { store.dispatch(HomeAction.ReadCard) }
                    }
                }
                is HomeAction.ReadCard -> handleReadCard()
                is HomeAction.GoToShop -> store.dispatchOpenUrl(HomeMiddleware.CARD_SHOP_URI)
            }
            next(action)
        }
    }
}

private fun handleReadCard() {
    if (!preferencesStorage.wasDisclaimerAccepted()) {
        store.dispatch(NavigationAction.NavigateTo(AppScreen.Disclaimer))
    } else {
        changeButtonState(ButtonState.PROGRESS)
        store.dispatch(GlobalAction.ScanCard(false, { scanResponse ->
            store.state.globalState.tapWalletManager.updateConfigManager(scanResponse)
            store.dispatch(TwinCardsAction.IfTwinsPrepareState(scanResponse))

            if (OnboardingHelper.isOnboardingCase(scanResponse)) {
                val navigateTo = OnboardingHelper.whereToNavigate(scanResponse)
                store.dispatch(GlobalAction.Onboarding.Start(scanResponse))
                navigateTo(navigateTo, store.state.homeState.shareTransition)
            } else {
                scope.launch {
                    store.onCardScanned(scanResponse)
                    withMainContext {
                        if (scanResponse.twinsIsTwinned() && !preferencesStorage.wasTwinsOnboardingShown()) {
                            store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.WelcomeOnly))
                            navigateTo(AppScreen.OnboardingTwins, store.state.homeState.shareTransition)
                        } else {
                            navigateTo(AppScreen.Wallet, null)
                        }
                    }
                }
            }
        }, {
            changeButtonState(ButtonState.ENABLED)
        }))
    }
}

private fun changeButtonState(state: ButtonState) {
    val btnState = IndeterminateProgressButton(state)
    store.dispatch(HomeAction.ChangeScanCardButtonState(btnState))
}

private fun navigateTo(screen: AppScreen, transition: FragmentShareTransition?) {
    post(DELAY_SDK_DIALOG_CLOSE) {
        changeButtonState(ButtonState.ENABLED)
        store.dispatch(NavigationAction.NavigateTo(screen, transition))
    }
}