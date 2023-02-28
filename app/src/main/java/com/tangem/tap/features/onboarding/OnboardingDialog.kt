package com.tangem.tap.features.onboarding

import com.tangem.common.extensions.VoidCallback
import com.tangem.tap.common.redux.StateDialog

/**
* [REDACTED_AUTHOR]
 */
sealed class OnboardingDialog : StateDialog {
    object TwinningProcessNotCompleted : OnboardingDialog()
    data class InterruptOnboarding(val onOk: VoidCallback) : OnboardingDialog()
}
