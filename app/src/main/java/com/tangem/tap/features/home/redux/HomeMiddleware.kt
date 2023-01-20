package com.tangem.tap.features.home.redux

import com.tangem.common.doOnFailure
import com.tangem.common.doOnResult
import com.tangem.common.doOnSuccess
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.analytics.events.Shop
import com.tangem.tap.common.analytics.paramsInterceptor.BatchIdParamsInterceptor
import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.extensions.onCardScanned
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.postUiDelayBg
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.model.builders.UserWalletBuilder
import com.tangem.tap.domain.scanCard.ScanCardProcessor
import com.tangem.tap.features.home.BELARUS_COUNTRY_CODE
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.features.home.redux.HomeMiddleware.Companion.BUY_WALLET_URL
import com.tangem.tap.features.send.redux.states.ButtonState
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userWalletsListManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber

class HomeMiddleware {
    companion object {
        val handler = homeMiddleware

        const val BUY_WALLET_URL = "https://tangem.com/ru/resellers/"
    }
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
        is HomeAction.Init -> {
            store.dispatch(GlobalAction.RestoreAppCurrency)
            store.dispatch(GlobalAction.ExchangeManager.Init)
            store.dispatch(GlobalAction.FetchUserCountry)
        }
        is HomeAction.ShouldScanCardOnResume -> {
            if (action.shouldScanCard) {
                store.dispatch(HomeAction.ShouldScanCardOnResume(false))
                postUiDelayBg(700) { store.dispatch(HomeAction.ReadCard(AppScreen.Wallet)) }
            }
        }
        is HomeAction.ReadCard -> readCard(action.fromScreen)
        is HomeAction.GoToShop -> {
            Analytics.send(Shop.ScreenOpened())
            when (action.userCountryCode) {
                RUSSIA_COUNTRY_CODE, BELARUS_COUNTRY_CODE -> store.dispatchOpenUrl(BUY_WALLET_URL)
                else -> store.dispatch(NavigationAction.NavigateTo(AppScreen.Shop))
            }
        }
    }
}

private fun readCard(fromScreen: AppScreen) = scope.launch {
    delay(timeMillis = 200)
    tangemSdkManager.setAccessCodeRequestPolicy(
        useBiometricsForAccessCode = preferencesStorage.shouldSaveAccessCodes,
    )
    ScanCardProcessor.scan(
        onProgressStateChange = { showProgress ->
            if (showProgress) {
                changeButtonState(ButtonState.PROGRESS)
            }
            // else { //todo hide this because
            //     changeButtonState(ButtonState.ENABLED)
            // }
        },
        onScanStateChange = { scanInProgress ->
            store.dispatch(HomeAction.ScanInProgress(scanInProgress))
        },
        onFailure = {
            changeButtonState(ButtonState.ENABLED)
        },
        onSuccess = { scanResponse ->
            when (fromScreen) {
                AppScreen.Wallet -> Analytics.send(MainScreen.CardWasScanned())
                else -> Analytics.send(IntroductionProcess.CardWasScanned())
            }
            scope.launch {
                if (preferencesStorage.shouldSaveUserWallets) {
                    val userWallet = UserWalletBuilder(scanResponse).build() ?: return@launch
                    userWalletsListManager.save(userWallet)
                        .doOnFailure { error ->
                            Timber.e(error, "Unable to save user wallet")
                            store.onCardScanned(scanResponse)
                        }
                        .doOnSuccess {
                            scope.launch { store.onUserWalletSelected(userWallet) }
                        }
                        .doOnResult {
                            changeButtonState(ButtonState.ENABLED)
                            store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
                        }
                } else {
                    store.onCardScanned(scanResponse)
                    changeButtonState(ButtonState.ENABLED)
                    store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
                }
            }
        },
    )
}

private fun changeButtonState(state: ButtonState) {
    store.dispatchOnMain(HomeAction.ChangeScanCardButtonState(IndeterminateProgressButton(state)))
}
