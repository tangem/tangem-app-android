package com.tangem.tap.features.onboarding

import com.tangem.domain.redux.StateDialog

/**
[REDACTED_AUTHOR]
 */
sealed class OnboardingDialog : StateDialog {
    data class WalletActivationError(val onConfirm: () -> Unit) : OnboardingDialog()
}