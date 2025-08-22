package com.tangem.features.hotwallet.walletbackup.entity

internal data class WalletBackupUM(
    val onBackClick: () -> Unit,
    val recoveryPhraseStatus: BackupStatus,
    val googleDriveStatus: BackupStatus,
    val onRecoveryPhraseClick: () -> Unit,
    val onGoogleDriveClick: () -> Unit,
)

internal sealed class BackupStatus {
    object Done : BackupStatus()
    object ComingSoon : BackupStatus()
    object NoBackup : BackupStatus()
}