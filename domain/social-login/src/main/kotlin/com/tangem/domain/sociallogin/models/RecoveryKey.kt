package com.tangem.domain.sociallogin.models

/**
 * Recovery key `K` — the random 32-byte value the social backup is encrypted with, used directly as the
 * AES-256 key (no KDF, unlike the password-based cloud backup of UC 07).
 *
 * Generated on the device with a CSPRNG (CPR-01) and never sent anywhere in the clear: the backend
 * receives it encrypted with the KMS public key and returns it encrypted with the device public key
 * (FR-04, FR-08), so plaintext `K` exists only inside the app. It must not reach logs, analytics or
 * crash reports (NFR-04).
 *
 * Call [wipe] when the whole flow that holds the key is over, not after an individual step: creating a
 * wallet uses the same key twice — to encrypt the backup and then to register it on the backend — so a
 * key wiped in between silently registers 32 zero bytes.
 */
class RecoveryKey(val value: ByteArray) {

    init {
        require(value.size == SIZE_BYTES) { "Recovery key must be $SIZE_BYTES bytes long, but was ${value.size}" }
    }

    /** Zeroes the key material — see the class doc for when it is safe to call. */
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