package com.tangem.tap.features.home.redux

import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.onboarding.OnboardingService
import com.tangem.tap.features.onboarding.redux.OnboardingAction
import com.tangem.tap.features.send.redux.states.ButtonState
import com.tangem.tap.preferencesStorage
import com.tangem.tap.store
import org.rekotlin.Middleware

class HomeMiddleware {
    companion object {
        val handler = homeMiddleware

        const val CARD_SHOP_URI =
                "https://shop.tangem.com/?afmc=1i&utm_campaign=1i&utm_source=leaddyno&utm_medium=affiliate"
    }
}

private val homeMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            when (action) {
                is HomeAction.Init -> {
                    store.dispatch(GlobalAction.RestoreAppCurrency)
                    store.dispatch(GlobalAction.GetMoonPayUserStatus)
                    store.dispatch(HomeAction.SetTermsOfUseState(preferencesStorage.wasDisclaimerAccepted()))
                }
                is HomeAction.ReadCard -> {
                    if (!preferencesStorage.wasDisclaimerAccepted()) {
                        store.dispatch(NavigationAction.NavigateTo(AppScreen.Disclaimer))
                    } else {
                        launchOnboardingService()
                    }
                }
                is HomeAction.GoToShop -> store.dispatch(HomeAction.SetOpenUrl(HomeMiddleware.CARD_SHOP_URI))
            }
            next(action)
        }
    }
}

private fun launchOnboardingService() {
    fun changeButtonState(state: ButtonState) {
        val btnState = IndeterminateProgressButton(state)
        store.dispatch(HomeAction.ChangeScanCardButtonState(btnState))
    }
    changeButtonState(ButtonState.PROGRESS)

    val walletManager = store.state.globalState.tapWalletManager
    val service = OnboardingService(
            walletManager,
            preferencesStorage.usedCardsPrefStorage
    )

    service.onReadyToProceed = {
        store.dispatch(OnboardingAction.SetData(it))
        changeButtonState(ButtonState.ENABLED)
        val sharedTransition = store.state.homeState.shareTransition
        store.dispatch(NavigationAction.NavigateTo(it.onboardingScreen, fragmentShareTransition = sharedTransition))
    }

    service.onFailedToProceedToOnboardingCase = {
        changeButtonState(ButtonState.ENABLED)
        walletManager.onCardScanned(it)
        store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet))
    }

    service.onError = {
        changeButtonState(ButtonState.ENABLED)
        store.dispatch(GlobalAction.DebugShowError(TapError.CustomError(it)))
    }

    store.dispatch(GlobalAction.ReadCard(service::onSuccess, {
        changeButtonState(ButtonState.ENABLED)
        store.dispatch(GlobalAction.DebugShowError(TapError.ScanCardError))
    }))
}