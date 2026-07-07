package com.tangem.lib.auth.dpop

import arrow.core.Option

/**
 * Builds [RFC 9449](https://www.rfc-editor.org/rfc/rfc9449) DPoP proofs (JWS) for outgoing
 * HTTP requests. Each proof is bound to a single request — `htm` / `htu` / `ath` claims must
 * not be reused, and `jti` is a fresh UUID per invocation.
 */
interface DpopProofFactory {

    /**
     * Builds a DPoP-proof for the given request.
     *
     * @param httpMethod uppercase HTTP method (e.g. `"POST"`).
     * @param httpUri target URI **without** query and fragment (RFC 9449 §4.2).
     * @param accessToken access token bound to this proof; when present, the SHA-256 hash
     *   is included as `ath` claim (RFC 9449 §4.3). Pass `null` for unauthenticated
     *   requests (initial registration, `/authenticate`) or `/refresh` where the access
     *   token has already expired (RFC 9449 §5).
     * @return compact-serialised JWS suitable for the `DPoP:` header, or [arrow.core.None]
     *   when the device key is unavailable or signing fails.
     */
    suspend fun create(httpMethod: String, httpUri: String, accessToken: String?): Option<String>
}