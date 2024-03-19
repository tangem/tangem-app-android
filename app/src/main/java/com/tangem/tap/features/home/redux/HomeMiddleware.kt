package com.tangem.tap.features.home.redux

import com.tangem.common.doOnFailure
import com.tangem.common.doOnResult
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.Basic
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.analytics.events.Shop
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.home.redux.HomeMiddleware.NEW_BUY_WALLET_URL
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

private const val HIDE_PROGRESS_DELAY = 400L

object HomeMiddleware {
    val handler = homeMiddleware

    const val NEW_BUY_WALLET_URL = "https://buy.tangem.com/"
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
            action.scope.launch {
                readCard(action.analyticsEvent)
            }
        }
        is HomeAction.GoToShop -> {
            Analytics.send(Shop.ScreenOpened())
            store.dispatchOpenUrl(NEW_BUY_WALLET_URL)

            // disabled for now in task https://tangem.atlassian.net/browse/AND-4135
            // when (action.userCountryCode) {
            //     RUSSIA_COUNTRY_CODE, BELARUS_COUNTRY_CODE -> store.dispatchOpenUrl(BUY_WALLET_URL)
            //     else -> store.dispatch(NavigationAction.NavigateTo(AppScreen.Shop))
            // }
        }
    }
}

private suspend fun readCard(analyticsEvent: AnalyticsEvent?) {
    store.inject(DaggerGraphState::cardSdkConfigRepository).setAccessCodeRequestPolicy(
        isBiometricsRequestPolicy = preferencesStorage.shouldSaveAccessCodes,
    )

    store.inject(DaggerGraphState::scanCardProcessor).scan(
        analyticsEvent = analyticsEvent,
        onProgressStateChange = { showProgress ->
            if (showProgress) {
                store.dispatch(HomeAction.ScanInProgress(scanInProgress = true))
            } else {
                delay(HIDE_PROGRESS_DELAY)
                store.dispatch(HomeAction.ScanInProgress(scanInProgress = false))
            }
        },
        onFailure = {
            Timber.e(it, "Unable to scan card")
            delay(HIDE_PROGRESS_DELAY)
            store.dispatch(HomeAction.ScanInProgress(scanInProgress = false))
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
            scope.launch { store.onUserWalletSelected(userWallet) }
        }
        .doOnResult {
            store.dispatchOnMain(SignInAction.SetSignInType(Basic.SignedIn.SignInType.Card))
            navigateTo(AppScreen.Wallet)
        }
}

private suspend fun navigateTo(appScreen: AppScreen) {
    store.dispatchOnMain(NavigationAction.NavigateTo(appScreen))
    delay(HIDE_PROGRESS_DELAY)
    store.dispatch(HomeAction.ScanInProgress(scanInProgress = false))
}
