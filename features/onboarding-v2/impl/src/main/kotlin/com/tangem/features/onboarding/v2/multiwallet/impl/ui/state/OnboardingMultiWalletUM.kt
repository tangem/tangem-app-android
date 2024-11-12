package com.tangem.features.onboarding.v2.multiwallet.impl.ui.state

import com.tangem.core.ui.extensions.TextReference

internal data class OnboardingMultiWalletUM(
    val onCreateWalletClick: () -> Unit,
    val showSeedPhraseOption: Boolean,
    val onOtherOptionsClick: () -> Unit,
    val onBack: () -> Unit,
    val dialog: Dialog?,
) {
    data class Dialog(
        val title: TextReference,
        val description: TextReference,
        val dismissButtonText: TextReference,
        val confirmButtonText: TextReference,
        val onConfirm: () -> Unit,
        val onDismissButtonClick: () -> Unit,
        val onDismiss: () -> Unit,
    )
}