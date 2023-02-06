package com.tangem.tap.features.disclaimer.redux

import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.features.disclaimer.Disclaimer
import com.tangem.tap.features.wallet.redux.ProgressState
import org.rekotlin.Action

sealed class DisclaimerAction : Action {

    data class SetDisclaimer(val disclaimer: Disclaimer) : DisclaimerAction()

    data class Show(
        val fromScreen: AppScreen,
        val callback: DisclaimerCallback? = null,
    ) : DisclaimerAction()

    object AcceptDisclaimer : DisclaimerAction()
    object OnBackPressed : DisclaimerAction()

    data class OnProgressStateChanged(val state: ProgressState?) : DisclaimerAction()
}
