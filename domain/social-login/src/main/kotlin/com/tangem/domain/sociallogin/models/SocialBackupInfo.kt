package com.tangem.domain.sociallogin.models

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Plaintext header of `social-backup.json`, readable without the recovery key.
 *
 * The folder keeps a single file per cloud account regardless of the wallet, so
 * [walletId] is what tells "my backup" from one the same account made for another wallet. Read on its
 * own the header is unauthenticated and must only be used to decide which file to touch.
 */
data class SocialBackupInfo(
    val walletId: UserWalletId,
    val createdAtMillis: Long,
    val version: Int,
)