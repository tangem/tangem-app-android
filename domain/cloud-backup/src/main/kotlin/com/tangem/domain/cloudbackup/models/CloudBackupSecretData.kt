package com.tangem.domain.cloudbackup.models

/**
 * The wallet secret carried by a cloud backup — the plaintext that is encrypted into the backup file
 * and recovered from it. Used both as the input to a backup upload and the output of a restore.
 *
 * The BIP39 passphrase itself is never backed up and never leaves the device; only the fact
 * that the wallet needs one travels with the backup, so the restore flow knows to ask for it.
 *
 * @property mnemonic             space-separated BIP39 words, held as a [CharArray] so it can be wiped

 */
class CloudBackupSecretData(
    val mnemonic: CharArray,
    val isPassphraseRequired: Boolean,
) {

    /** Blanks the mnemonic — call it as soon as the secret has been encrypted or imported. */
    fun wipe() {
        mnemonic.fill(' ')
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CloudBackupSecretData) return false

        return mnemonic.contentEquals(other.mnemonic) && isPassphraseRequired == other.isPassphraseRequired
    }

    override fun hashCode(): Int = 31 * mnemonic.contentHashCode() + isPassphraseRequired.hashCode()

    override fun toString(): String = "CloudBackupSecretData(mnemonic=***, isPassphraseRequired=$isPassphraseRequired)"
}