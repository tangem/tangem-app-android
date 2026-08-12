package com.tangem.domain.cloudbackup.models


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