package com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui.state

data class MultiWalletBackupUM(
    val finalizeButtonEnabled: Boolean,
    val onAddBackupClick: () -> Unit,
    val onFinalizeButtonClick: () -> Unit,
)
