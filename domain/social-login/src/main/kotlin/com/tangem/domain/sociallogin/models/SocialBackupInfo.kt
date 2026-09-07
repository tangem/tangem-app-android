package com.tangem.domain.sociallogin.models

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Plaintext header of the `social-backup.json` file — the fields stored unencrypted so the app can tell
 * whose backup the folder currently holds without the recovery key.
 *
 * The folder keeps a single file per cloud account regardless of the wallet it belongs to (section 4.3),
 * so [walletId] is what distinguishes "my backup" from a backup the same account made for another
 * wallet. It is also part of the AAD, so a file whose header was edited fails decryption (CPR-03) — but
 * a header read on its own is unauthenticated and must only be used to decide which file to touch.
 *
 * @property walletId        wallet the backup was written for

 * @property version         social backup format version the file was written with
 */
data class SocialBackupInfo(
    val walletId: UserWalletId,
    val createdAtMillis: Long,
    val version: Int,
)