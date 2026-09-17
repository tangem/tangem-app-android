package com.tangem.domain.sociallogin.models

/**
 * The plaintext encrypted into `social-backup.json`.
 *
 * The BIP39 passphrase itself is never backed up; only the fact that the wallet needs one travels

 * for forward compatibility of the format.
 *
 * @property mnemonic space-separated BIP39 words, held as a [CharArray] so it can be wiped
 */
class SocialBackupSecret(
    val mnemonic: CharArray,
    val isPassphraseRequired: Boolean,
) {

    /** Call as soon as the secret has been encrypted or imported */
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