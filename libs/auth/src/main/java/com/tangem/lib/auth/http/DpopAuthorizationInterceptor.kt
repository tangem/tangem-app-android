package com.tangem.lib.auth.http

import com.tangem.lib.auth.api.RequiresDpopProof
import com.tangem.lib.auth.api.RequiresSessionAuth
import com.tangem.lib.auth.dpop.DpopProofFactory
import com.tangem.lib.auth.session.SessionTokenRefresher
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.utils.logging.TangemLogger
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
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

        val accessToken = runBlocking { resolveAccessToken(original.requiresSessionRefresh()) }
        if (accessToken == null) {
            // Annotated endpoint reached without a session — let the upstream HTTP layer surface
            // the resulting 401 so `SessionAuthenticator` can drive recovery.
            TangemLogger.e("Skipping DPoP headers: no access token in store")
            return chain.proceed(original)
        }

        val proof = runBlocking {
            proofFactory.create(original.method, original.htuUrl(), accessToken)
        }.getOrNull()

        if (proof == null) {
            TangemLogger.e("DPoP proof generation failed; sending request without DPoP headers")
            return chain.proceed(original)
        }

        return chain.proceed(original.withDpopHeaders(accessToken, proof))
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