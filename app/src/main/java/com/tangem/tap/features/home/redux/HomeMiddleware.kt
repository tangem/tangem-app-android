package com.tangem.tap.features.home.redux

import com.tangem.common.core.TangemError
import com.tangem.common.services.Result
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.extensions.withIOContext
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.tap.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.AnalyticsParam
import com.tangem.tap.common.analytics.GetCardSourceParams
import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.extensions.onCardScanned
import com.tangem.tap.common.postUiDelayBg
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.FragmentShareTransition
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.home.BELARUS_COUNTRY_CODE
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.features.home.redux.HomeMiddleware.Companion.BUY_WALLET_URL
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.features.onboarding.OnboardingSaltPayHelper
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsStep
import com.tangem.tap.features.onboarding.products.wallet.saltPay.dialog.SaltPayDialog
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayRegistrationError
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayAction
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayState
import com.tangem.tap.features.send.redux.states.ButtonState
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware

class HomeMiddleware {
    companion object {
        val handler = homeMiddleware

        const val BUY_WALLET_URL = "https://tangem.com/ru/resellers/"
    }
}

private val homeMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            handleHomeAction(state, action, dispatch)
            next(action)
        }
    }
}

private fun handleHomeAction(appState: () -> AppState?, action: Action, dispatch: DispatchFunction) {
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
                postUiDelayBg(700) { store.dispatch(HomeAction.ReadCard) }
            }
        }
        is HomeAction.ReadCard -> {
            if (!preferencesStorage.wasDisclaimerAccepted()) {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.Disclaimer))
            } else {
                changeButtonState(ButtonState.PROGRESS)
                val scanCardAction = GlobalAction.ScanCard(
                    onSuccess = ::onScanSuccess,
                    onFailure = ::onScanFailure,
                )
                store.dispatch(HomeAction.ScanInProgress(true))
                postUiDelayBg(300) { store.dispatch(scanCardAction) }
            }
        }
        is HomeAction.GoToShop -> {
            when (action.userCountryCode) {
                RUSSIA_COUNTRY_CODE, BELARUS_COUNTRY_CODE -> store.dispatchOpenUrl(BUY_WALLET_URL)
                else -> store.dispatch(NavigationAction.NavigateTo(AppScreen.Shop))
            }
            store.state.globalState.analyticsHandlers?.triggerEvent(
                event = AnalyticsEvent.GET_CARD,
                params = mapOf(AnalyticsParam.SOURCE.param to GetCardSourceParams.WELCOME.param),
            )
        }
    }
}

private fun onScanSuccess(scanResponse: ScanResponse) {
    val globalState = store.state.globalState
    val tapWalletManager = globalState.tapWalletManager
    tapWalletManager.updateConfigManager(scanResponse)

    store.dispatch(HomeAction.ScanInProgress(false))
    store.dispatch(TwinCardsAction.IfTwinsPrepareState(scanResponse))

    if (scanResponse.isSaltPay()) {
        scope.launch {
            val result = OnboardingSaltPayHelper.isOnboardingCase(scanResponse)
            delay(500)
            withMainContext {
                when (result) {
                    is Result.Success -> {
                        val isOnboardingCase = result.data
                        if (isOnboardingCase) {
                            store.dispatch(GlobalAction.Onboarding.Start(scanResponse, canSkipBackup = false))
                            val (manager, config) = OnboardingSaltPayState.initDependency(scanResponse)
                            store.dispatch(OnboardingSaltPayAction.Init.SetDependencies(manager, config))
                            store.dispatch(OnboardingSaltPayAction.Update)
                            // store.dispatch(OnboardingSaltPayAction.Init.DiscardBackupSteps)
                            navigateTo(AppScreen.OnboardingWallet)
                        } else {
                            navigateTo(AppScreen.Wallet)
                            withIOContext { store.onCardScanned(scanResponse) }
                        }
                    }
                    is Result.Failure -> {
                        changeButtonState(ButtonState.ENABLED)
                        when (val error = result.error) {
                            is SaltPayRegistrationError -> {
                                val dialog = when (error) {
                                    is SaltPayRegistrationError.NoGas -> SaltPayDialog.NoFundsForActivation
                                    else -> SaltPayDialog.RegistrationError(error)
                                }
                                store.dispatchDialogShow(dialog)
                            }
                            else -> {
                                store.dispatchDebugErrorNotification(error.localizedMessage ?: "SaltPay: Unknown error")
                            }
                        }
                    }
                }
            }
        }
    } else {
        if (OnboardingHelper.isOnboardingCase(scanResponse)) {
            store.dispatch(GlobalAction.Onboarding.Start(scanResponse, canSkipBackup = true))
            val appScreen = OnboardingHelper.whereToNavigate(scanResponse)
            navigateTo(appScreen)
        } else {
            if (scanResponse.twinsIsTwinned() && !preferencesStorage.wasTwinsOnboardingShown()) {
                store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.WelcomeOnly))
                navigateTo(AppScreen.OnboardingTwins)
            } else {
                navigateTo(AppScreen.Wallet, null)
            }
            scope.launch { store.onCardScanned(scanResponse) }
        }
    }
}

private fun onScanFailure(error: TangemError) {
    store.dispatch(HomeAction.ScanInProgress(false))
    changeButtonState(ButtonState.ENABLED)
}

private fun changeButtonState(state: ButtonState) {
    store.dispatch(HomeAction.ChangeScanCardButtonState(IndeterminateProgressButton(state)))
}

private fun navigateTo(screen: AppScreen, transition: FragmentShareTransition? = null) {
    postUiDelayBg(DELAY_SDK_DIALOG_CLOSE) {
        changeButtonState(ButtonState.ENABLED)
        store.dispatch(NavigationAction.NavigateTo(screen, transition))
    }
}