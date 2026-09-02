package com.tangem.domain.sociallogin.models

/**
 * The wallet secret carried by a social backup — the plaintext that is encrypted into
 * `social-backup.json` and recovered from it. Used both as the input to a backup write and the output
 * of a restore.
 *
 * The BIP39 passphrase itself is never backed up (CPR-04); only the fact that the wallet needs one

 * carried for forward compatibility of the file format (section 4.2).
 *
 * @property mnemonic             space-separated BIP39 words, held as a [CharArray] so it can be wiped

 */
class SocialBackupSecret(
    val mnemonic: CharArray,
    val isPassphraseRequired: Boolean,
) {

    /** Blanks the mnemonic — call it as soon as the secret has been encrypted or imported. */
    fun wipe() {
        mnemonic.fill(' ')
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SocialBackupSecret) return false

        return mnemonic.contentEquals(other.mnemonic) && isPassphraseRequired == other.isPassphraseRequired
    }

    override fun hashCode(): Int = 31 * mnemonic.contentHashCode() + isPassphraseRequired.hashCode()

    override fun toString(): String = "SocialBackupSecret(mnemonic=***, isPassphraseRequired=$isPassphraseRequired)"
}