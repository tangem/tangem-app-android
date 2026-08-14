package com.tangem.data.cloudbackup.crypto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Decrypted cloud-backup payload — the plaintext that sits INSIDE [CloudBackupFileData.crypto].
 *
 * This is the cross-platform "secret" contract: whatever a backup encrypts, both platforms must produce
 * and parse the identical structure, otherwise decryption succeeds but parsing fails. Kept separate from
 * the outer keystore file so the BIP39 passphrase is backed up and restored together with the phrase
 * (a phrase alone reconstructs a different wallet when a passphrase is set).
 *
 * @property mnemonic   space-joined BIP39 words
 * @property passphrase optional BIP39 passphrase (25th word); absent when the wallet has none
 */
@Serializable
internal data class CloudBackupSecret(
    @SerialName("mnemonic") val mnemonic: String,
    @SerialName("passphrase") val passphrase: String? = null,
)