package com.tangem.tap.features.home.redux

import kotlinx.coroutines.CoroutineScope
import org.rekotlin.Action

sealed class HomeAction : Action {

    data object OnCreate : HomeAction()

    /**
     * Action for scanning card
     *
     * @property scope lifecycle scope. It will be canceled when lifecycle-aware component is destroyed
     */
    data class ReadCard(val scope: CoroutineScope) : HomeAction()

    data class ScanInProgress(val scanInProgress: Boolean) : HomeAction()
    data object GoToShop : HomeAction()

    data class UpdateCountryCode(val userCountryCode: String) : HomeAction()
}