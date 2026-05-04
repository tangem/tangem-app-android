package com.tangem.tap.common.redux.global

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.scan.ScanResponse
import org.rekotlin.Action

sealed class GlobalAction : Action {

    data class SaveScanResponse(val scanResponse: ScanResponse) : GlobalAction()

    data class ChangeAppCurrency(val appCurrency: AppCurrency) : GlobalAction()
    object RestoreAppCurrency : GlobalAction() {
        data class Success(val appCurrency: AppCurrency) : GlobalAction()
    }

    data class IsSignWithRing(val isSignWithRing: Boolean) : GlobalAction()
}