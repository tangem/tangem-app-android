package com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui.state.MultiWalletBackupUMDialog

internal fun resetBackupCardDialog(onReset: () -> Unit, onDismiss: () -> Unit) = MultiWalletBackupUMDialog(
    title = resourceReference(R.string.common_attention),
    bodyText = resourceReference(R.string.onboarding_linking_error_card_with_wallets),
    confirmText = resourceReference(R.string.common_cancel),
    cancelText = resourceReference(R.string.card_settings_action_sheet_reset),
    warningCancelColor = true,
    onDismiss = onDismiss,
    onConfirm = onDismiss,
    onCancel = { onReset(); onDismiss() },
)

internal fun backupCardAttestationFailedDialog(onDismiss: () -> Unit) = MultiWalletBackupUMDialog(
    title = resourceReference(R.string.common_error),
    bodyText = resourceReference(R.string.issuer_signature_loading_failed),
    confirmText = resourceReference(R.string.common_ok),
    cancelText = null,
    warningCancelColor = true,
    onDismiss = onDismiss,
    onConfirm = onDismiss,
    onCancel = null,
)