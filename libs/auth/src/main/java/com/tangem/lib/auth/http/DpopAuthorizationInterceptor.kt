package com.tangem.lib.auth.http

import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.lib.auth.api.RequiresDpopProof
import com.tangem.lib.auth.api.RequiresSessionAuth
import com.tangem.lib.auth.dpop.DpopProofFactory
import com.tangem.lib.auth.session.SessionTokenRefresher
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.utils.logging.TangemLogger
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Adds [RFC 9449](https://www.rfc-editor.org/rfc/rfc9449) DPoP headers to requests whose
 * Retrofit method is marked with [RequiresDpopProof] or the umbrella [RequiresSessionAuth]:
 *  - `Authorization: DPoP <access-token>` — present if [SessionTokensStore] holds an access token.
 *  - `DPoP: <proof-jwt>` — freshly generated for every annotated request; `ath` claim is set if
 *    the access token is present.
 *
 * Methods **without** the annotation pass through unchanged — keeps public endpoints
 * (e.g. `/auth/nonce/auth`, `/auth/authenticate`) free of unnecessary proof generation.
 *
 * On unrecoverable proof-generation failures (e.g. device key unavailable) the request is passed
 * through unmodified — the upstream HTTP layer will surface the resulting 401/403 and the
 * `SessionAuthenticator` (if installed) will attempt recovery.
 *
 * A 403 is handled right here instead: OkHttp's `Authenticator` mechanism (`RetryAndFollowUpInterceptor`)
 * only ever calls `authenticator.authenticate()` for a 401 response, so `SessionAuthenticator` is simply
 * never reached for a 403 — this interceptor is the only place in the chain that still sees that response
 * and can retry it.
 *
 * This retry and [SessionAuthenticator]'s 401 retry share one budget via [markSessionAuthRetried] /
 * [isSessionAuthRetry] — see that marker's KDoc for why OkHttp's own retry tracking isn't enough to
 * keep a 401-then-403 (or 403-then-401) sequence down to a single retry.
 */
class DpopAuthorizationInterceptor(
    private val store: SessionTokensStore,
    private val proofFactory: DpopProofFactory,
    // Lazy to break the interceptor → refresher → AuthApi → OkHttp cycle (the client already
    // resolves this interceptor through a Provider, so this only ever runs at request time).
    private val refresher: Lazy<SessionTokenRefresher>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        if (!original.requiresDpopProof()) return chain.proceed(original)

        val isSessionRefreshRequired = original.requiresSessionRefresh()
        val response = chain.proceed(signRequest(original, isSessionRefreshRequired))

        val canRetry = isSessionRefreshRequired &&
            response.code == Code.FORBIDDEN.numericCode &&
            !response.request.isSessionAuthRetry()
        if (!canRetry) return response

        return retryAfterForbidden(chain, original, response) ?: response
    }

    private fun signRequest(original: Request, requiresSessionRefresh: Boolean): Request {
        val accessToken = runBlocking { resolveAccessToken(requiresSessionRefresh) }
        if (accessToken == null) {
            // Annotated endpoint reached without a session — let the upstream HTTP layer surface
            // the resulting 401 so `SessionAuthenticator` can drive recovery.
            TangemLogger.e("Skipping DPoP headers: no access token in store")
            return original
        }

        val proof = runBlocking {
            proofFactory.create(original.method, original.htuUrl(), accessToken)
        }.getOrNull()

        if (proof == null) {
            TangemLogger.e("DPoP proof generation failed; sending request without DPoP headers")
            return original
        }

        return original.withDpopHeaders(accessToken, proof)
    }

    // Mirrors SessionAuthenticator's 401 retry (force a real rotation via refresh(), not ensureSession() —
    // the token that just got a 403 is still "valid" by TTL, so ensureSession() would hand back the same one).
    private fun retryAfterForbidden(chain: Interceptor.Chain, original: Request, forbidden: Response): Response? {
        val refreshed = runBlocking { refresher.get().refresh() }.getOrNull()
        if (refreshed == null) {
            TangemLogger.e("Session refresh after 403 failed; surfacing original response")
            return null
        }

        val proof = runBlocking {
            proofFactory.create(original.method, original.htuUrl(), refreshed.accessToken)
        }.getOrNull()
        if (proof == null) {
            TangemLogger.e("DPoP proof generation failed after 403 refresh; surfacing original response")
            return null
        }

        forbidden.close()
        return chain.proceed(original.withDpopHeaders(refreshed.accessToken, proof).markSessionAuthRetried())
    }

    private suspend fun resolveAccessToken(requiresSessionRefresh: Boolean): String? = if (requiresSessionRefresh) {
        // Session-protected endpoint: ensure the access token is valid before sending. ensureSession
        // returns the current token if still valid, otherwise refreshes — joining any in-flight refresh
        // via single-flight. This stops a request that started while /token/refresh was in flight from
        // going out with the stale token (a guaranteed 401 that would trigger a second token rotation).
        refresher.get().ensureSession().getOrNull()?.accessToken
    } else {
        // Refresh-flow endpoints (@RequiresDpopProof only, e.g. /refresh) must read the stored token
        // directly — calling ensureSession here would re-enter the refresh single-flight and deadlock.
        store.get().getOrNull()?.accessToken
    }
}