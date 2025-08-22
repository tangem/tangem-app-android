package com.tangem.features.hotwallet.walletbackup.entity

import com.tangem.core.ui.components.label.entity.LabelUM

internal data class WalletBackupUM(
    val onBackClick: () -> Unit,
    val recoveryPhraseStatus: LabelUM?,
    val googleDriveStatus: LabelUM?,
    val onRecoveryPhraseClick: () -> Unit,
    val onGoogleDriveClick: () -> Unit,
    val backedUp: Boolean,
)

internal sealed class BackupStatus {
    object Done : BackupStatus()
    object ComingSoon : BackupStatus()
    object NoBackup : BackupStatus()
}