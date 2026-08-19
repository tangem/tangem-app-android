package com.tangem.lib.auth.attestation

import android.util.Base64
import java.security.MessageDigest

/**
 * Builds the `requestHash` that binds a Play Integrity token to an auth request.
 *
 * Contract shared byte-for-byte with the backend (which recomputes and compares against
 * `requestDetails.requestHash` in the decoded token):
 *
 * `requestHash = Base64Std( SHA-256( devicePublicKeyBytes ‖ nonceBytes ) )`
 *
 * - `devicePublicKeyBytes` — the raw SPKI/DER public key bytes (same bytes the request carries
 *   base64-encoded in `payload.devicePublicKey`), NOT the base64 string.
 * - `nonceBytes` — the decrypted nonce base64url-decoded to raw bytes.
 * - concatenation is `devicePublicKey` then `nonce`, no separator.
 * - the 32-byte digest is standard Base64 (with padding, no wrapping).
 */
object AttestationRequestHash {

    fun create(devicePublicKey: ByteArray, nonce: String): String {
        val nonceBytes = Base64.decode(nonce, Base64.URL_SAFE or Base64.NO_WRAP)
        val digest = MessageDigest.getInstance("SHA-256").digest(devicePublicKey + nonceBytes)
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}