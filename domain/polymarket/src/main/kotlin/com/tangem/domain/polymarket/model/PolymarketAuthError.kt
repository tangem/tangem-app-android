package com.tangem.domain.polymarket.model

sealed interface PolymarketAuthError : PolymarketError {

    /** The L1 signature was rejected (HTTP 401). */
    data object InvalidSignature : PolymarketAuthError

    /** No API key exists yet for this address (HTTP 404) — the caller should create one. */
    data object KeyNotFound : PolymarketAuthError

    /** Throttled by Cloudflare (HTTP 429). */
    data object RateLimited : PolymarketAuthError

    /** Transport failure (no connection / timeout). */
    data object Network : PolymarketAuthError

    /** Anything else. */
    data class Unknown(val httpCode: Int?, val detail: String?) : PolymarketAuthError
}