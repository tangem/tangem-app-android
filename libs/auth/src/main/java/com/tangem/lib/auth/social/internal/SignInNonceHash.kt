package com.tangem.lib.auth.social.internal

import android.util.Base64
import java.security.MessageDigest

/**
 * What the provider mints the id token's `nonce` claim from.
 *
 * Both Apple and Google prescribe hashing the nonce before handing it to the provider, and the backend
 * rebuilds the same digest to check the claim. Hashes the UTF-8 bytes of the nonce **string** — not its
 * base64url-decoded value — and encodes the digest as standard Base64 with padding, matching iOS
 * (`Data(SHA256.hash(data: Data(nonce.utf8))).base64EncodedString()`).
 */
internal fun String.sha256Base64(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(digest, Base64.NO_WRAP)
}