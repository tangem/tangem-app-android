package com.tangem.tap.features.home.redux

import android.content.res.Resources
import com.tangem.common.doOnFailure
import com.tangem.common.doOnResult
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.guard
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.analytics.converters.ParamCardCurrencyConverter
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.extensions.eraseContext
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber
import java.util.Locale

internal const val HIDE_PROGRESS_DELAY = 400L

object HomeMiddleware {
    val handler = homeMiddleware

    private val SYSTEM_LANGUAGE =
        runCatching { Resources.getSystem().configuration.locales[0].language }.getOrElse { "" }
    private val APP_LANGUAGE = Locale.getDefault().language
    private val UTM_MARKS = "utm_source=tangem-app" +
        "&utm_medium=app" +
        "&utm_campaign=prospect-$SYSTEM_LANGUAGE" +
        "&utm_content=devicelang-$APP_LANGUAGE"

    val NEW_BUY_WALLET_URL = "https://buy.tangem.com/?$UTM_MARKS"
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

            store.dispatch(GlobalAction.RestoreAppCurrency)
        }
        is HomeAction.ReadCard -> {
            action.scope.launch {
                readCard()
            }
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
    val userWalletBuilder = store.inject(DaggerGraphState::coldUserWalletBuilderFactory).create(scanResponse)

    val userWallet = userWalletBuilder.build().guard {
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
            store.onUserWalletSelected(userWallet = userWallet)
        }
        .doOnResult {
            navigateTo(AppRoute.Wallet)
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

private suspend fun navigateTo(route: AppRoute) {
    store.dispatchNavigationAction { push(route) }
    delay(HIDE_PROGRESS_DELAY)
    store.dispatch(HomeAction.ScanInProgress(scanInProgress = false))
}