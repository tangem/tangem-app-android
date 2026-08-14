package com.tangem.domain.cloudbackup.models

/**
 * The wallet secret carried by a cloud backup — the plaintext that is encrypted into the backup file
 * and recovered from it. Used both as the input to a backup upload and the output of a restore.
 *
 * @property mnemonic   space-joined BIP39 words
 * @property passphrase optional BIP39 passphrase (25th word); `null` when the wallet has none
 */
data class CloudBackupSecretData(
    val mnemonic: String,
    val passphrase: String?,
)