package com.tangem.tap.features.onboarding

import com.tangem.common.extensions.VoidCallback
import com.tangem.core.navigation.StateDialog

/**
 * Created by Anton Zhilenkov on 01.02.2023.
 */
sealed class OnboardingDialog : StateDialog {
    object TwinningProcessNotCompleted : OnboardingDialog()
    data class InterruptOnboarding(val onOk: VoidCallback) : OnboardingDialog()
}
