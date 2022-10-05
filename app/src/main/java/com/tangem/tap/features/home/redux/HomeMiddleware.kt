package com.tangem.tap.features.home.redux

import com.tangem.domain.common.TapWorkarounds.isSaltPayVisa
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.tap.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.AnalyticsParam
import com.tangem.tap.common.analytics.GetCardSourceParams
import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.extensions.onCardScanned
import com.tangem.tap.common.post
import com.tangem.tap.common.postUi
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.FragmentShareTransition
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.home.BELARUS_COUNTRY_CODE
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.features.home.redux.HomeMiddleware.Companion.BUY_WALLET_URL
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

        const val BUY_WALLET_URL = "https://tangem.com/ru/resellers/"
    }
}

private val homeMiddleware: Middleware<AppState> = { _, _ ->
    { next ->
        { action ->
            when (action) {
                is HomeAction.Init -> {
                    store.dispatch(GlobalAction.RestoreAppCurrency)
                    store.dispatch(GlobalAction.ExchangeManager.Init)
                    store.dispatch(HomeAction.SetTermsOfUseState(preferencesStorage.wasDisclaimerAccepted()))
                    store.dispatch(GlobalAction.FetchUserCountry)
                }
                is HomeAction.ShouldScanCardOnResume -> {
                    if (action.shouldScanCard) {
                        store.dispatch(HomeAction.ShouldScanCardOnResume(false))
                        postUi(700) { store.dispatch(HomeAction.ReadCard) }
                    }
                }
                is HomeAction.ReadCard -> {
                    handleReadCard()
//                    store.dispatch(NavigationAction.NavigateTo(AppScreen.AddCustomTokens))
                }
                is HomeAction.GoToShop -> {
                    when (action.userCountryCode) {
                        RUSSIA_COUNTRY_CODE, BELARUS_COUNTRY_CODE ->
                            store.dispatchOpenUrl(BUY_WALLET_URL)
                        else -> store.dispatch(NavigationAction.NavigateTo(AppScreen.Shop))
                    }
                    store.state.globalState.analyticsHandlers?.triggerEvent(
                        event = AnalyticsEvent.GET_CARD,
                        params = mapOf(AnalyticsParam.SOURCE.param to GetCardSourceParams.WELCOME.param),
                    )
                }
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
        store.dispatch(
            GlobalAction.ScanCard(
                onSuccess = { scanResponse ->
                    store.state.globalState.tapWalletManager.updateConfigManager(scanResponse)
                    store.dispatch(TwinCardsAction.IfTwinsPrepareState(scanResponse))
// [REDACTED_TODO_COMMENT]
                    if (scanResponse.card.isSaltPayVisa) {
                        scope.launch {
                            store.onCardScanned(scanResponse)
                            withMainContext {
                                navigateTo(AppScreen.Wallet, null)
                            }
                        }
                        return@ScanCard
                    }

                    if (OnboardingHelper.isOnboardingCase(scanResponse)) {
                        val navigateTo = OnboardingHelper.whereToNavigate(scanResponse)
                        store.dispatch(GlobalAction.Onboarding.Start(scanResponse))
                        navigateTo(navigateTo)
                    } else {
                        scope.launch {
                            withMainContext {
                                if (scanResponse.twinsIsTwinned() && !preferencesStorage.wasTwinsOnboardingShown()) {
                                    store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.WelcomeOnly))
                                    navigateTo(AppScreen.OnboardingTwins)
                                } else {
                                    navigateTo(AppScreen.Wallet, null)
                                }
                            }
                            store.onCardScanned(scanResponse)
                        }
                    }
                },
                onFailure = {
                    changeButtonState(ButtonState.ENABLED)
                },
            ),
        )
    }
}

private fun changeButtonState(state: ButtonState) {
    val btnState = IndeterminateProgressButton(state)
    store.dispatch(HomeAction.ChangeScanCardButtonState(btnState))
}

private fun navigateTo(screen: AppScreen, transition: FragmentShareTransition? = null) {
    post(DELAY_SDK_DIALOG_CLOSE) {
        changeButtonState(ButtonState.ENABLED)
        store.dispatch(NavigationAction.NavigateTo(screen, transition))
    }
}
