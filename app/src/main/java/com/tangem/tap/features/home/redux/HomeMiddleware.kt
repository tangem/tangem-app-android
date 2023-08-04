package com.tangem.tap.features.home.redux

import com.tangem.common.doOnFailure
import com.tangem.common.doOnResult
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.analytics.events.Shop
import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.extensions.eraseContext
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.home.redux.HomeMiddleware.NEW_BUY_WALLET_URL
import com.tangem.tap.features.send.redux.states.ButtonState
import com.tangem.tap.features.signin.redux.SignInAction
import com.tangem.tap.preferencesStorage
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.userWalletsListManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber

object HomeMiddleware {
    val handler = homeMiddleware

    const val BUY_WALLET_URL = "https://tangem.com/ru/resellers/"
    const val NEW_BUY_WALLET_URL = "https://buy.tangem.com/ru/"
}

private val homeMiddleware: Middleware<AppState> = { _, _ ->
    { next ->
        { action ->
            handleHomeAction(action)
            next(action)
        }
    }
}

private fun handleHomeAction(action: Action) {
    when (action) {
        is HomeAction.OnCreate -> {
            Analytics.eraseContext()
            Analytics.send(IntroductionProcess.ScreenOpened())
        }
        is HomeAction.Init -> {
            store.dispatch(GlobalAction.RestoreAppCurrency)
            store.dispatch(GlobalAction.ExchangeManager.Init)
            store.dispatch(GlobalAction.FetchUserCountry)
        }
        is HomeAction.ReadCard -> {
            readCard(action.analyticsEvent)
        }
        is HomeAction.GoToShop -> {
            Analytics.send(Shop.ScreenOpened())
            store.dispatchOpenUrl(NEW_BUY_WALLET_URL)

            // disabled for now in task [REDACTED_JIRA]
            // when (action.userCountryCode) {
            //     RUSSIA_COUNTRY_CODE, BELARUS_COUNTRY_CODE -> store.dispatchOpenUrl(BUY_WALLET_URL)
            //     else -> store.dispatch(NavigationAction.NavigateTo(AppScreen.Shop))
            // }
        }
    }
}

private fun readCard(analyticsEvent: AnalyticsEvent?) = scope.launch {
    delay(timeMillis = 200)
    store.state.daggerGraphState.get(DaggerGraphState::cardSdkConfigRepository).setAccessCodeRequestPolicy(
        isBiometricsRequestPolicy = preferencesStorage.shouldSaveAccessCodes,
    )

    store.state.daggerGraphState.get(DaggerGraphState::scanCardProcessor).scan(
        analyticsEvent = analyticsEvent,
        onProgressStateChange = { showProgress ->
            if (showProgress) {
                changeButtonState(ButtonState.PROGRESS)
            } else {
                changeButtonState(ButtonState.ENABLED)
            }
        },
        onScanStateChange = { scanInProgress ->
            store.dispatch(HomeAction.ScanInProgress(scanInProgress))
        },
        onFailure = {
            Timber.e(it, "Unable to scan card")
            changeButtonState(ButtonState.ENABLED)
        },
        onSuccess = { scanResponse ->
            proceedWithScanResponse(scanResponse)
        },
    )
}

private fun proceedWithScanResponse(scanResponse: ScanResponse) = scope.launch {
    val userWallet = UserWalletBuilder(scanResponse).build().guard {
        Timber.e("User wallet not created")
        return@launch
    }

    userWalletsListManager.save(userWallet)
        .doOnFailure { error ->
            Timber.e(error, "Unable to save user wallet")
        }
        .doOnSuccess {
            scope.launch { store.onUserWalletSelected(userWallet = userWallet) }
        }
        .doOnResult {
            store.dispatchOnMain(SignInAction.SetSignInType(Basic.SignedIn.SignInType.Card))
            navigateTo(AppScreen.Wallet)
        }
}

private suspend fun navigateTo(appScreen: AppScreen) {
    store.dispatchOnMain(NavigationAction.NavigateTo(appScreen))
    delay(timeMillis = 200)
    changeButtonState(ButtonState.ENABLED)
}

private fun changeButtonState(state: ButtonState) {
    store.dispatchOnMain(HomeAction.ChangeScanCardButtonState(IndeterminateProgressButton(state)))
}