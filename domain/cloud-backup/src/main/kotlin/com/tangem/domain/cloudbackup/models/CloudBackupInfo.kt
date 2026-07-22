package com.tangem.domain.cloudbackup.models

/**
 * Info about a single wallet backup file stored in the cloud
 *
 * @property fileId          cloud file identifier (e.g. Google Drive file id)
 * @property walletName      name of the backed up wallet

 * @property walletId        id of the backed up wallet (matches `UserWalletId.stringValue`);
 *                           `null` for legacy files uploaded before the id was persisted
 */
data class CloudBackupInfo(
    val fileId: String,
    val walletName: String,
    val createdAtMillis: Long,
    val walletId: String?,
)