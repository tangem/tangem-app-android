package com.tangem.data.polymarket.error

import com.squareup.moshi.Moshi

internal object ClobSignerRefusalParser {

    private const val ERROR_FIELD = "error"
    private val REFUSAL_MARKERS = listOf("signature", "signer", "api key", "unauthorized")

    private val adapter = Moshi.Builder().build().adapter(Map::class.java)

    /**
     * @param errorBody the CLOB error body, `{"error": "<message>"}`; the CLOB sends no error code, only the message
     * @return the message when it refuses the order's signature, signer or API key, `null` otherwise
     */
    fun parse(errorBody: String?): String? {
        val message = errorBody
            ?.let { body -> runCatching { adapter.fromJson(body)?.get(ERROR_FIELD) as? String }.getOrNull() }
            ?: return null

        return message.takeIf { REFUSAL_MARKERS.any { marker -> message.contains(marker, ignoreCase = true) } }
    }
}