package com.tangem.domain.cloudbackup.models

/**
 * Info about a single wallet backup file stored in the cloud
 *
 * @property fileId          cloud file identifier (e.g. Google Drive file id)
 * @property backupName      cloud file name without the backup extension. Equals [walletName] unless
 *                           another wallet already occupies that name, in which case it carries the
 *                           deduplicating " (n)" suffix — e.g. `Wallet (1)`. Doubles as the username the
 *                           backup password is stored under in the password manager
 * @property walletName      name of the backed up wallet

 * @property walletId        id of the backed up wallet (matches `UserWalletId.stringValue`);
 *                           `null` for legacy files uploaded before the id was persisted
 */
data class CloudBackupInfo(
    val fileId: String,
    val backupName: String,
    val walletName: String,
    val createdAtMillis: Long,
    val walletId: String?,
)