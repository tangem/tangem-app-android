package com.tangem.domain.sociallogin.models

/**
 * Recovery key `K` — the random 32-byte value the social backup is encrypted with, used directly as the
 * AES-256 key, without a KDF.
 *
 * Never leaves the device in the clear: the backend receives it sealed for the KMS and returns it
 * sealed for an ephemeral device key.
 *
 * Call [wipe] when the whole flow holding the key is over, not after an individual step: creating a
 * wallet uses the same key to encrypt the backup and then to register it, so a key wiped in between
 * silently registers 32 zero bytes.
 */
class RecoveryKey(val value: ByteArray) {

    init {
        require(value.size == SIZE_BYTES) { "Recovery key must be $SIZE_BYTES bytes long, but was ${value.size}" }
    }

    fun wipe() {
        value.fill(0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecoveryKey) return false

        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = "RecoveryKey(***)"

    companion object {
        const val SIZE_BYTES = 32
    }
}