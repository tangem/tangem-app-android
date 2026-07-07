package com.tangem.lib.auth.http

import arrow.core.getOrElse
import com.tangem.datasource.api.auth.RequiresSessionRefresh
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException.Code
import com.tangem.lib.auth.dpop.DpopProofFactory
import com.tangem.lib.auth.session.SessionTokenRefresher
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp [Authenticator] that reacts to 401/403 by rotating session tokens via
 * [SessionTokenRefresher] and retrying the original request with a fresh DPoP proof.
 *
 * Returns `null` (giving up) when:
 *  - the response code is not 401/403;
 *  - the Retrofit method is **not** annotated with [RequiresSessionRefresh] (or the umbrella
 *    [RequiresSessionAuth]) — keeps public endpoints and refresh-flow endpoints themselves
 *    (annotated with `@RequiresDpopProof` only) from triggering token rotation on incidental 401s;
 *  - the request was already retried once (`response.priorResponse != null`);
 *  - the refresher fails (revoked session, network error, etc.).
 *
 * This guarantees at most one retry per call site — OkHttp will not loop on persistent 401s.
 */
class SessionAuthenticator(
    private val refresher: SessionTokenRefresher,
    private val proofFactory: DpopProofFactory,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != Code.UNAUTHORIZED.numericCode && response.code != Code.FORBIDDEN.numericCode) return null
        if (response.priorResponse != null) return null
        if (!response.request.requiresSessionRefresh()) return null

        val refreshed = runBlocking { refresher.refresh() }.getOrElse { error ->
            TangemLogger.e("Session refresh failed ($error); surfacing original ${response.code}")
            return null
        }

        val request = response.request
        val proof = runBlocking {
            proofFactory.create(request.method, request.htuUrl(), refreshed.accessToken)
        }.getOrElse {
            TangemLogger.e("DPoP proof generation failed after refresh; cannot retry request")
            return null
        }

        return request.withDpopHeaders(refreshed.accessToken, proof)
    }
}