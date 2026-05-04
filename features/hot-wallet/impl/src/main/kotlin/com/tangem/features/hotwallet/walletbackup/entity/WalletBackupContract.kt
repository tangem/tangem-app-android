package com.tangem.features.hotwallet.walletbackup.entity

import com.tangem.core.ui.components.label.entity.LabelUM

internal data class WalletBackupUM(
    val hardwareWalletOption: LabelUM?,
    val recoveryPhraseOption: LabelUM?,
    val googleDriveOption: LabelUM?,
    val googleDriveStatus: BackupStatus,
    val isGoogleDriveDialogShown: Boolean,
    val isBackedUp: Boolean,
)

internal sealed class BackupStatus {
    object Done : BackupStatus()
    object ComingSoon : BackupStatus()
    object NoBackup : BackupStatus()
}

internal sealed interface Action {
    data object RecoveryPhrase : Action
    data object HardwareWallet : Action
    data class GoogleDriveBackup(val isDialogShown: Boolean) : Action
    data object OnBack : Action
}