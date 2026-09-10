package com.tangem.features.hotwallet.walletbackup.entity

import com.tangem.core.ui.components.label.entity.LabelUM

internal data class WalletBackupUM(
    val onBackClick: () -> Unit,
    val hardwareWalletOption: LabelUM?,
    val recoveryPhraseOption: LabelUM?,
    val googleDriveOption: LabelUM?,
    val googleDriveStatus: BackupStatus,
    val onRecoveryPhraseClick: () -> Unit,
    val onGoogleDriveClick: () -> Unit,
    val onHardwareWalletClick: () -> Unit,
    val isBackedUp: Boolean,
    /** Hidden for a social-login wallet, whose seed is already in the cloud under its own key (FR-10) */
    val isGoogleDriveOptionShown: Boolean = true,
) {

    val isGoogleDriveEnabled: Boolean get() = googleDriveStatus != BackupStatus.Loading
}

internal sealed class BackupStatus {
    object Loading : BackupStatus()
    object Done : BackupStatus()
    object ComingSoon : BackupStatus()
    object NoBackup : BackupStatus()
    object NetworkError : BackupStatus()

    data class ActionRequired(val reason: Reason) : BackupStatus() {
        enum class Reason { NoAccess, FileNotFound }
    }
}