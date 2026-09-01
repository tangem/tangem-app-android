package com.tangem.domain.sociallogin.models

/**
 * A decrypted social backup: the wallet secret plus the header of the file it came from.
 *
 * Unlike a header read via [SocialBackupInfo] alone, this one is authenticated — it survived the
 * AES-GCM check over the AAD built from it (CPR-03), so the wallet id and version can be trusted. The
 * restored wallet still derives its own id from the imported mnemonic; [info] only relates the backup to
 * the binding it was recovered for.
 *
 * @property info   authenticated header of the backup file
 * @property secret the recovered wallet secret
 */
data class RestoredSocialBackup(
    val info: SocialBackupInfo,
    val secret: SocialBackupSecret,
)