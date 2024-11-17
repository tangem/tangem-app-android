package com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.ui.state

import com.tangem.core.ui.extensions.TextReference

data class MultiWalletCreateWalletUM(
    val title: TextReference,
    val bodyText: TextReference,
    val showOtherOptionsButton: Boolean,
    val onCreateWalletClick: () -> Unit,
    val onOtherOptionsClick: () -> Unit,
    val dialog: Dialog?,
) {
    data class Dialog(
        val title: TextReference,
        val description: TextReference,
        val dismissButtonText: TextReference,
        val confirmButtonText: TextReference,
        val dismissWarningColor: Boolean = false,
        val onConfirm: () -> Unit,
        val onDismissButtonClick: () -> Unit,
        val onDismiss: () -> Unit,
    )
}
