package com.tangem.domain.cloudbackup.models

/**
 * A decrypted cloud backup — the wallet secret plus the name of the backed up wallet.
 *
 * The name comes from the backup file itself and not from the cloud metadata: the file name and the
 * cloud file properties are length-limited, so a long wallet name is stored there truncated.
 *
 * @property walletName name of the backed up wallet, as it was at the moment of the backup
 * @property secret     the recovered wallet secret
 */
data class RestoredCloudBackup(
    val walletName: String,
    val secret: CloudBackupSecretData,
)