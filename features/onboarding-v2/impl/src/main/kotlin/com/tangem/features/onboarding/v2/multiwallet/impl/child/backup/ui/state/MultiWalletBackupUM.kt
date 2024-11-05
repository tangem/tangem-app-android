package com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui.state

import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference

@Immutable
sealed class MultiWalletBackupUM {

    abstract val dialog: MultiWalletBackupUMDialog?

    data class Wallet1(
        val title: TextReference = stringReference(""),
        val bodyText: TextReference = stringReference(""),
        val addBackupButtonEnabled: Boolean = true,
        val addBackupButtonLoading: Boolean = false,
        val showFinalizeButton: Boolean = false,
        val oneBackupAdded: Boolean = false,
        @IntRange(0, 2) val artworkNumberOfBackupCards: Int = 0, // highlights cards on 0, 1, 2
        val onAddBackupClick: () -> Unit = {},
        val onFinalizeButtonClick: () -> Unit = {},
        val onSkipButtonClick: () -> Unit = {},
        override val dialog: MultiWalletBackupUMDialog? = null,
    ) : MultiWalletBackupUM()

    data class Wallet2(
        val isRing: Boolean,
        val backupAdded: Boolean,
        val onAddBackupClick: () -> Unit,
        val onFinalizeButtonClick: () -> Unit,
        override val dialog: MultiWalletBackupUMDialog? = null,
    ) : MultiWalletBackupUM()
}

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
