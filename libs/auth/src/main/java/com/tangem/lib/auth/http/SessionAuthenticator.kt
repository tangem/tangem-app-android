package com.tangem.lib.auth.http

import arrow.core.getOrElse
import com.tangem.lib.auth.api.RequiresSessionRefresh
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.lib.auth.dpop.DpopProofFactory
import com.tangem.lib.auth.session.SessionTokenRefresher
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp [Authenticator] that reacts to a 401 by rotating session tokens via [SessionTokenRefresher]
 * and retrying the original request with a fresh DPoP proof.
 *
 * OkHttp's `RetryAndFollowUpInterceptor` only ever calls `authenticator.authenticate()` for a 401
 * response (RFC 7235) — never for a 403 — so the 403 branch below is only reachable when this method
 * is invoked directly (e.g. in tests); in production a 403 is retried by [DpopAuthorizationInterceptor]
 * instead, which does see that response.
 *
 * Returns `null` (giving up) when:
 *  - the response code is not 401/403;
 *  - the Retrofit method is **not** annotated with [RequiresSessionRefresh] (or the umbrella
 *    [RequiresSessionAuth]) — keeps public endpoints and refresh-flow endpoints themselves
 *    (annotated with `@RequiresDpopProof` only) from triggering token rotation on incidental 401s;
 *  - the request was already retried once, either within this same `chain.proceed()` run
 *    (`response.priorResponse != null`) or by [DpopAuthorizationInterceptor]'s separate 403 retry
 *    (`response.request` carries [markSessionAuthRetried] — see that marker's KDoc for why OkHttp's
 *    own `priorResponse` tracking alone cannot see across the two mechanisms);
 *  - the refresher fails (revoked session, network error, etc.).
 *
 * This guarantees at most one retry per logical call — OkHttp will not loop on persistent 401s, and
 * neither will a 401-then-403 (or 403-then-401) sequence hand out a second retry between the two
 * mechanisms.
 */
class SessionAuthenticator(
    private val refresher: SessionTokenRefresher,
    private val proofFactory: DpopProofFactory,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != Code.UNAUTHORIZED.numericCode && response.code != Code.FORBIDDEN.numericCode) return null
        if (response.priorResponse != null) return null
        if (response.request.isSessionAuthRetry()) return null
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

        return request.withDpopHeaders(refreshed.accessToken, proof).markSessionAuthRetried()
    }
}