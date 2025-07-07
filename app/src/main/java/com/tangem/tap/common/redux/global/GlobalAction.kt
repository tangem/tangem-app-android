package com.tangem.tap.common.redux.global

import com.tangem.common.CompletionResult
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.StateDialog
import org.rekotlin.Action

sealed class GlobalAction : Action {

    // dialogs
    data class ShowDialog(val stateDialog: StateDialog) : GlobalAction()
    object HideDialog : GlobalAction()

    object ScanFailsCounter {
        data class ChooseBehavior(
            val result: CompletionResult<ScanResponse>,
            val analyticsSource: AnalyticsParam.ScreensSources,
        ) : GlobalAction()

        object Reset : GlobalAction()
        object Increment : GlobalAction()
    }

    data class SaveScanResponse(val scanResponse: ScanResponse) : GlobalAction()

    data class ChangeAppCurrency(val appCurrency: AppCurrency) : GlobalAction()
    object RestoreAppCurrency : GlobalAction() {
        data class Success(val appCurrency: AppCurrency) : GlobalAction()
    }

    data class IsSignWithRing(val isSignWithRing: Boolean) : GlobalAction()
}