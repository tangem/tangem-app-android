package com.tangem.tap.features.home.redux

import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.common.redux.navigation.FragmentShareTransition
import org.rekotlin.Action

sealed class HomeAction : Action {
    // from ui
    object ReadCard : HomeAction()
    object GoToShop : HomeAction()

    // internal
    data class ShouldScanCardOnResume(val shouldScanCard: Boolean):HomeAction()
    object Init : HomeAction()

    data class SetFragmentShareTransition(val shareTransition: FragmentShareTransition?) : HomeAction()
    data class SetTermsOfUseState(val isDisclaimerAccepted: Boolean) : HomeAction()
    data class ChangeScanCardButtonState(val state: IndeterminateProgressButton) : HomeAction()

}
