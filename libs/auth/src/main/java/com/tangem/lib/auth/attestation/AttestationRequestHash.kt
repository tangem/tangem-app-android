package com.tangem.lib.auth.attestation

import android.util.Base64
import java.security.MessageDigest

/**
 * Builds the `requestHash` that binds a Play Integrity token to an auth request.
 *
 * Contract shared byte-for-byte with the backend, which recomputes it as
 * `sha256Base64(nonce)` — hashing the raw UTF-8 bytes of the nonce **string** (not its
 * base64url-decoded value) — and compares against `requestDetails.requestHash` in the decoded token:
 *
 * `requestHash = Base64Std( SHA-256( nonce.utf8Bytes ) )`
 *
 * The 32-byte digest is standard Base64 (with padding, no wrapping).
 */
object AttestationRequestHash {

    fun create(nonce: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(nonce.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}