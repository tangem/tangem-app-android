package com.tangem.features.hotwallet.walletbackup.entity

import com.tangem.core.ui.components.label.entity.LabelUM

internal data class WalletBackupUM(
    val onBackClick: () -> Unit,
    val hardwareWalletOption: LabelUM?,
    val recoveryPhraseOption: LabelUM?,
    val googleDriveOption: LabelUM?,
    val googleDriveStatus: BackupStatus,
    val onRecoveryPhraseClick: () -> Unit,
    val onGoogleDriveAction: (Boolean) -> Unit, // boolean is for show and hide dialog
    val onHardwareWalletClick: () -> Unit,
    val isGoogleDriveDialogShown: Boolean,
    val isBackedUp: Boolean,
)

internal sealed class BackupStatus {
    object Done : BackupStatus()
    object ComingSoon : BackupStatus()
    object NoBackup : BackupStatus()
}