package com.tangem.data.polymarket.signer

import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import javax.inject.Inject

/**
 * Builds the five `POLY_*` L2 auth headers for an authenticated CLOB request. The HMAC signs
 * `timestamp + method + requestPath [+ body]`; `passphrase` is sent in the clear (not part of the HMAC).
 *
 * [requestPath] is signed verbatim, so it must be the path the CLOB expects to verify — with a leading
 * slash and, for the endpoints checked so far, without the query string. [method] and [timestamp] default
 * to a `GET` sent now; a caller only overrides them for another verb or to pin a signature in a test.
 */
internal class PolymarketL2HeaderBuilder @Inject constructor(
    private val signer: PolymarketHmacSigner,
) {

    @Suppress("LongParameterList")
    fun build(
        ownerAddress: String,
        credentials: PolymarketApiCredentials,
        requestPath: String,
        method: String = HTTP_METHOD_GET,
        timestamp: String = currentTimestampSeconds(),
        body: String? = null,
    ): Map<String, String> {
        val message = timestamp + method + requestPath + body.orEmpty()
        val signature = signer.sign(secret = credentials.secret, message = message)
        return mapOf(
            HEADER_ADDRESS to ownerAddress,
            HEADER_SIGNATURE to signature,
            HEADER_TIMESTAMP to timestamp,
            HEADER_API_KEY to credentials.apiKey,
            HEADER_PASSPHRASE to credentials.passphrase,
        )
    }

    /** The CLOB checks this against its own clock, so it is read per request and expressed in seconds. */
    private fun currentTimestampSeconds(): String = (System.currentTimeMillis() / MILLIS_IN_SECOND).toString()

    private companion object {
        const val HEADER_ADDRESS = "POLY_ADDRESS"
        const val HEADER_SIGNATURE = "POLY_SIGNATURE"
        const val HEADER_TIMESTAMP = "POLY_TIMESTAMP"
        const val HEADER_API_KEY = "POLY_API_KEY"
        const val HEADER_PASSPHRASE = "POLY_PASSPHRASE"

        const val HTTP_METHOD_GET = "GET"
        const val MILLIS_IN_SECOND = 1_000L
    }
}