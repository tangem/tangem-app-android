package com.tangem.tap.common.redux.global

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.StateDialog
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware

object GlobalMiddleware {
    val handler = globalMiddlewareHandler
}

private val globalMiddlewareHandler: Middleware<AppState> = { _, _ ->
    { nextDispatch ->
        { action ->
            handleAction(action)
            nextDispatch(action)
        }
    }
}

@Suppress("LongMethod", "ComplexMethod")
private fun handleAction(action: Action) {
    when (action) {
        is GlobalAction.ScanFailsCounter.ChooseBehavior -> {
            when (action.result) {
                is CompletionResult.Success -> store.dispatch(GlobalAction.ScanFailsCounter.Reset)
                is CompletionResult.Failure -> {
                    handleFailureChooseBehaviour(action.result, action.analyticsSource)
                }
            }
        }
        is GlobalAction.RestoreAppCurrency -> restoreAppCurrency()
    }
}

private fun handleFailureChooseBehaviour(
    result: CompletionResult.Failure<ScanResponse>,
    analyticsSource: AnalyticsParam.ScreensSources,
) {
    if (result.error is TangemSdkError.UserCancelled) {
        store.dispatch(GlobalAction.ScanFailsCounter.Increment)
        if (store.state.globalState.scanCardFailsCounter >= 2) {
            val scanFailsSource = when (analyticsSource) {
                is AnalyticsParam.ScreensSources.SignIn -> StateDialog.ScanFailsSource.SIGN_IN
                is AnalyticsParam.ScreensSources.Settings -> StateDialog.ScanFailsSource.SETTINGS
                is AnalyticsParam.ScreensSources.Intro -> StateDialog.ScanFailsSource.INTRO
                else -> StateDialog.ScanFailsSource.MAIN
            }
            store.dispatchDialogShow(StateDialog.ScanFailsDialog(scanFailsSource))
        }
    } else {
        store.dispatch(GlobalAction.ScanFailsCounter.Reset)
    }
}

private fun restoreAppCurrency() {
    scope.launch {
        val currency = store.inject(DaggerGraphState::appCurrencyRepository)
            .getSelectedAppCurrency()
            .firstOrNull()
            ?: AppCurrency.Default

        store.dispatchWithMain(GlobalAction.RestoreAppCurrency.Success(currency))
    }
}