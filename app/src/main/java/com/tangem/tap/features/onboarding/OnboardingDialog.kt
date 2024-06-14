package com.tangem.tap.features.onboarding

import com.tangem.common.extensions.VoidCallback
import com.tangem.domain.redux.StateDialog

/**
[REDACTED_AUTHOR]
 */
sealed class OnboardingDialog : StateDialog {
    object TwinningProcessNotCompleted : OnboardingDialog()
    data class InterruptOnboarding(val onOk: VoidCallback) : OnboardingDialog()
    data class WalletActivationError(val onConfirm: () -> Unit) : OnboardingDialog()
}