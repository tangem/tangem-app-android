package com.tangem.tap.features.home.redux

import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.common.redux.navigation.AppScreen
import org.rekotlin.Action

sealed class HomeAction : Action {

    object Init : HomeAction()
    data class ShouldScanCardOnResume(val shouldScanCard: Boolean) : HomeAction()

    // from ui
    data class ReadCard(
        val fromScreen: AppScreen = AppScreen.Home,
    ) : HomeAction()

    data class ScanInProgress(val scanInProgress: Boolean) : HomeAction()
    data class GoToShop(val userCountryCode: String?) : HomeAction()

    data class ChangeScanCardButtonState(val state: IndeterminateProgressButton) : HomeAction()
}