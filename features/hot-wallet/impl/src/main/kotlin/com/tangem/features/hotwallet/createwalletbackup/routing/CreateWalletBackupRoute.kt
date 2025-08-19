package com.tangem.features.hotwallet.createwalletbackup.routing

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface CreateWalletBackupRoute {

    @Serializable
    data object RecoveryPhraseStart : CreateWalletBackupRoute

    @Serializable
    data object RecoveryPhrase : CreateWalletBackupRoute

    @Serializable
    data object ConfirmBackup : CreateWalletBackupRoute

    @Serializable
    data object BackupCompleted : CreateWalletBackupRoute
}