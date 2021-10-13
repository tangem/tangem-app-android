package com.tangem.tap.features.home.redux

import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.common.extensions.dispatchErrorNotification
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.extensions.withMainContext
import com.tangem.tap.common.post
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.domain.TapWalletManager
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.features.onboarding.service.OnboardingHelper
import com.tangem.tap.features.send.redux.states.ButtonState
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        fun changeButtonState(state: ButtonState) {
            val btnState = IndeterminateProgressButton(state)
            store.dispatch(HomeAction.ChangeScanCardButtonState(btnState))
        }

        fun proceedToWalletScreen(tapWalletManager: TapWalletManager, scanResponse: ScanResponse) {
            scope.launch {
                tapWalletManager.onCardScanned(scanResponse)
                delay(DELAY_SDK_DIALOG_CLOSE)
                withMainContext { changeButtonState(ButtonState.ENABLED) }
                store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet))
            }
        }
        changeButtonState(ButtonState.PROGRESS)

        store.dispatch(GlobalAction.ReadCard({ scanResponse ->
            val tapWalletManager = store.state.globalState.tapWalletManager
            if (!OnboardingHelper.isOnboardingCase(scanResponse)) {
                proceedToWalletScreen(tapWalletManager, scanResponse)
                return@ReadCard
            }

            val service = OnboardingHelper.createService(
                    AppScreen.Home,
                    scanResponse,
                    tapWalletManager.walletManagerFactory
            )
            if (service == null) {
                proceedToWalletScreen(tapWalletManager, scanResponse)
                return@ReadCard
            }

            service.onInitializationProgress = {
                when (it) {
                    ProgressState.Loading -> changeButtonState(ButtonState.PROGRESS)
                    ProgressState.Done -> changeButtonState(ButtonState.ENABLED)
                    ProgressState.Error -> changeButtonState(ButtonState.ENABLED)
                }
            }
            service.onInitialized = {
                store.dispatch(GlobalAction.Onboarding.Activate(service))
                store.dispatch(NavigationAction.NavigateTo(it, store.state.homeState.shareTransition))
            }
            service.onError = {
                store.dispatchErrorNotification(it)
            }
            post(DELAY_SDK_DIALOG_CLOSE) { service.initializeOnboarding() }
        }, {
            changeButtonState(ButtonState.ENABLED)
        }))
    }
}