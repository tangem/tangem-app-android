package com.tangem.tap.features.home.redux

import com.tangem.tap.common.entities.IndeterminateProgressButton
import org.rekotlin.Action

sealed class HomeAction : Action {
    // from ui
    object ReadCard : HomeAction()
    data class ScanInProgress(val scanInProgress: Boolean) : HomeAction()
    data class GoToShop(val userCountryCode: String?) : HomeAction()

    // internal
    data class ShouldScanCardOnResume(val shouldScanCard: Boolean) : HomeAction()
    object Init : HomeAction()

    data class ChangeScanCardButtonState(val state: IndeterminateProgressButton) : HomeAction()
}

