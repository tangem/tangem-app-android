package com.tangem.tap.features.home.redux

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.tangem.common.doOnFailure
import com.tangem.common.doOnResult
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.tap.common.analytics.converters.ParamCardCurrencyConverter
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.analytics.events.Shop
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.home.redux.HomeMiddleware.NEW_BUY_WALLET_URL
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber

private const val HIDE_PROGRESS_DELAY = 400L

object HomeMiddleware {
    val handler = homeMiddleware

    const val NEW_BUY_WALLET_URL = "https://buy.tangem.com/?utm_source=tangem&utm_medium=app"
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
                readCard()
            }
        }
        is HomeAction.GoToShop -> {
            Analytics.send(Shop.ScreenOpened())
            Firebase.analytics.appInstanceId
                .addOnSuccessListener {
                    store.dispatchOpenUrl("$NEW_BUY_WALLET_URL&app_instance_id=$it")
                }
                .addOnFailureListener {
                    store.dispatchOpenUrl(NEW_BUY_WALLET_URL)
                }
            // disabled for now in task https://tangem.atlassian.net/browse/AND-4135
            // when (action.userCountryCode) {
            //     RUSSIA_COUNTRY_CODE, BELARUS_COUNTRY_CODE -> store.dispatchOpenUrl(BUY_WALLET_URL)
            //     else -> store.dispatch(NavigationAction.NavigateTo(AppScreen.Shop))
            // }
        }
    }
}

private suspend fun readCard() {
    val shouldSaveAccessCodes = store.inject(DaggerGraphState::settingsRepository).shouldSaveAccessCodes()

    store.inject(DaggerGraphState::cardSdkConfigRepository).setAccessCodeRequestPolicy(
        isBiometricsRequestPolicy = shouldSaveAccessCodes,
    )

    store.inject(DaggerGraphState::scanCardProcessor).scan(
        analyticsSource = AnalyticsParam.ScreensSources.Intro,
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
    val walletNameGenerateUseCase = store.inject(DaggerGraphState::generateWalletNameUseCase)
    val userWallet = UserWalletBuilder(scanResponse, walletNameGenerateUseCase).build().guard {
        Timber.e("User wallet not created")
        return@launch
    }

    val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)
    userWalletsListManager.save(userWallet)
        .doOnFailure { error ->
            Timber.e(error, "Unable to save user wallet")
        }
        .doOnSuccess {
            sendSignedInCardAnalyticsEvent(scanResponse)
            scope.launch { store.onUserWalletSelected(userWallet = userWallet) }
        }
        .doOnResult {
            navigateTo(AppScreen.Wallet)
        }
}

private fun sendSignedInCardAnalyticsEvent(scanResponse: ScanResponse) {
    val currency = ParamCardCurrencyConverter().convert(
        value = scanResponse.cardTypesResolver,
    )

    if (currency != null) {
        val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)

        Analytics.send(
            event = Basic.SignedIn(
                currency = currency,
                batch = scanResponse.card.batchId,
                signInType = Basic.SignedIn.SignInType.Card,
                walletsCount = userWalletsListManager.walletsCount.toString(),
                hasBackup = scanResponse.card.backupStatus?.isActive,
            ),
        )
    }
}

private suspend fun navigateTo(appScreen: AppScreen) {
    store.dispatchOnMain(NavigationAction.NavigateTo(appScreen))
    delay(HIDE_PROGRESS_DELAY)
    store.dispatch(HomeAction.ScanInProgress(scanInProgress = false))
}
