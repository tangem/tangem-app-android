package com.tangem.domain.sociallogin.models

/**
 * A decrypted social backup: the wallet secret plus the header of the file it came from.
 *
 * Only the `walletId` and `version` of [info] are authenticated — the AAD is built from those two, so

 * the file ceasing to open; treat it as a display value, never as a basis for choosing between files.
 */
data class RestoredSocialBackup(
    val info: SocialBackupInfo,
    val secret: SocialBackupSecret,
)