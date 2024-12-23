package com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference

@Immutable
data class MultiWalletBackupUM(
    val title: TextReference = stringReference(""),
    val bodyText: TextReference = stringReference(""),
    val addBackupButtonEnabled: Boolean = true,
    val addBackupButtonLoading: Boolean = false,
    val finalizeButtonEnabled: Boolean = false,
    val onAddBackupClick: () -> Unit = {},
    val onFinalizeButtonClick: () -> Unit = {},
    val dialog: MultiWalletBackupUMDialog? = null,
)

data class MultiWalletBackupUMDialog(
    val title: TextReference,
    val bodyText: TextReference,
    val confirmText: TextReference,
    val cancelText: TextReference?,
    val warningCancelColor: Boolean = false,
    val onConfirm: () -> Unit,
    val onCancel: (() -> Unit)?,
    val onDismiss: () -> Unit,
)