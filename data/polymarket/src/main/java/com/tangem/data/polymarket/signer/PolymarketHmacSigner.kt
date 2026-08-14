package com.tangem.data.polymarket.signer

import com.tangem.utils.extensions.calculateHmacSha256
import javax.inject.Inject

/**
 * Polymarket L2 HMAC-SHA256 request signer. Keyed by the base64url-decoded `secret`; returns the url-safe
 * base64 signature used as the `POLY_SIGNATURE` header. Mirrors `py-clob-client/py_clob_client/signing/hmac.py`.
 */
internal class PolymarketHmacSigner @Inject constructor(
    private val base64: Base64UrlCodec,
) {

    fun sign(secret: String, message: String): String {
        val mac = message.toByteArray(Charsets.UTF_8).calculateHmacSha256(base64.decode(secret))
        return base64.encode(mac)
    }
}