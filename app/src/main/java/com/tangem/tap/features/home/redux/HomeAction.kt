package com.tangem.tap.features.home.redux

import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.features.home.RegionProvider
import org.rekotlin.Action

sealed class HomeAction : Action {
    // from ui
    object ReadCard : HomeAction()
    data class GoToShop(val regionProvider: RegionProvider) : HomeAction()

    // internal
    data class ShouldScanCardOnResume(val shouldScanCard: Boolean) : HomeAction()
    object Init : HomeAction()

    data class SetTermsOfUseState(val isDisclaimerAccepted: Boolean) : HomeAction()
    data class ChangeScanCardButtonState(val state: IndeterminateProgressButton) : HomeAction()

}
