package com.tangem.lib.auth.attestation

import android.util.Base64
import java.security.MessageDigest

/**
 * Builds the `requestHash` that binds a Play Integrity token to an auth request.
 *
 * Contract shared byte-for-byte with the backend (which recomputes and compares against
 * `requestDetails.requestHash` in the decoded token):
 *
 * `requestHash = Base64Std( SHA-256( nonceBytes ) )`
 *
 * - `nonceBytes` — the decrypted nonce base64url-decoded to raw bytes.
 * - the 32-byte digest is standard Base64 (with padding, no wrapping).
 */
object AttestationRequestHash {

    fun create(nonce: String): String {
        val nonceBytes = Base64.decode(nonce, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val digest = MessageDigest.getInstance("SHA-256").digest(nonceBytes)
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}