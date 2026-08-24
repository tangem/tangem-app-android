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
class RestoredCloudBackup(
    val walletName: String,
    val secret: CloudBackupSecretData,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RestoredCloudBackup) return false

        return walletName == other.walletName && secret == other.secret
    }

    override fun hashCode(): Int = 31 * walletName.hashCode() + secret.hashCode()

    override fun toString(): String = "RestoredCloudBackup(walletName=$walletName, secret=$secret)"
}