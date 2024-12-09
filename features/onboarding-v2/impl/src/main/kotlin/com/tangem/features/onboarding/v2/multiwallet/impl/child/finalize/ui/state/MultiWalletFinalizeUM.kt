package com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.ui.state

import com.tangem.features.onboarding.v2.multiwallet.impl.common.ui.OnboardingDialogUM

internal data class MultiWalletFinalizeUM(
    val step: Step = Step.Primary,
    val isRing: Boolean = false,
    val scanPrimary: Boolean = true,
    val cardNumber: String = "",
    val onScanClick: () -> Unit = {},
    val dialog: OnboardingDialogUM? = null,
) {
    enum class Step {
        Primary, BackupDevice1, BackupDevice2
    }
}