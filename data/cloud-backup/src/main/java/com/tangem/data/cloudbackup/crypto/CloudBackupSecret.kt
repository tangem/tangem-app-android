package com.tangem.data.cloudbackup.crypto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Decrypted cloud-backup payload — the plaintext that sits INSIDE [CloudBackupFileData.crypto].
 *
 * This is the cross-platform "secret" contract (UC-07 §4.1): both platforms must produce and parse
 * exactly this JSON, otherwise decryption succeeds but parsing fails.
 *
 * The BIP39 passphrase itself is deliberately not backed up — it never leaves the device (CPR-07). Only
 * [passphraseRequired] travels with the backup, so the restore flow knows to ask for it; it is mandatory,
 * hence no default value.
 *
 * @property mnemonic           space-joined BIP39 words
 * @property passphraseRequired [PASSPHRASE_REQUIRED] for a wallet with a passphrase, [PASSPHRASE_NOT_REQUIRED] without
 */
@Serializable
internal data class CloudBackupSecret(
    @SerialName("mnemonic") val mnemonic: String,
    @SerialName("passphraseRequired") val passphraseRequired: Int,
) {

    /** `null` when [passphraseRequired] carries a value outside of the contract */
    val isPassphraseRequired: Boolean?
        get() = when (passphraseRequired) {
            PASSPHRASE_REQUIRED -> true
            PASSPHRASE_NOT_REQUIRED -> false
            else -> null
        }

    companion object {

        const val PASSPHRASE_REQUIRED = 1
        const val PASSPHRASE_NOT_REQUIRED = 0

        fun of(mnemonic: String, isPassphraseRequired: Boolean) = CloudBackupSecret(
            mnemonic = mnemonic,
            passphraseRequired = if (isPassphraseRequired) PASSPHRASE_REQUIRED else PASSPHRASE_NOT_REQUIRED,
        )
    }
}