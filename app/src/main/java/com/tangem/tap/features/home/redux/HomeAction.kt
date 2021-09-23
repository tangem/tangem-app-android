package com.tangem.tap.features.home.redux

import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.common.redux.navigation.FragmentShareTransition
import org.rekotlin.Action

sealed class HomeAction : Action {
    object Init : HomeAction()
    data class SetFragmentShareTransition(val shareTransition: FragmentShareTransition?) : HomeAction()

    object ReadCard : HomeAction()
    object GoToShop : HomeAction()
    class SetOpenUrl(val url: String?) : HomeAction()
    class SetTermsOfUseState(val isDisclaimerAccepted: Boolean) : HomeAction()

    data class ChangeScanCardButtonState(val state: IndeterminateProgressButton) : HomeAction()

    object ShowDialog : HomeAction() {
        object ScanFails : HomeAction()
    }
    object HideDialog : HomeAction()
}
