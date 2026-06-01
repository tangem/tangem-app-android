package com.tangem.lib.auth.http

import com.tangem.datasource.api.auth.RequiresSessionAuth
import com.tangem.lib.auth.dpop.DpopProofFactory
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import retrofit2.Invocation

/**
 * Adds [RFC 9449](https://www.rfc-editor.org/rfc/rfc9449) DPoP headers to requests whose
 * Retrofit method is marked with [RequiresSessionAuth]:
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
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        if (!original.requiresSessionAuth()) return chain.proceed(original)

        val accessToken = runBlocking { store.get().getOrNull()?.accessToken }
        if (accessToken == null) {
            // Annotated endpoint reached without a session — let the upstream HTTP layer surface
            // the resulting 401 so `SessionAuthenticator` can drive recovery.
            TangemLogger.e("Skipping DPoP headers: no access token in store")
            return chain.proceed(original)
        }

        val proof = runBlocking {
            proofFactory.create(original.method, original.url.toString(), accessToken)
        }.getOrNull()

        if (proof == null) {
            TangemLogger.e("DPoP proof generation failed; sending request without DPoP headers")
            return chain.proceed(original)
        }

        return chain.proceed(
            original.newBuilder()
                .header(HEADER_AUTHORIZATION, "$DPOP_SCHEME $accessToken")
                .header(HEADER_DPOP, proof)
                .build(),
        )
    }

    private fun Request.requiresSessionAuth(): Boolean =
        tag(Invocation::class.java)?.method()?.isAnnotationPresent(RequiresSessionAuth::class.java) == true

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HEADER_DPOP = "DPoP"
        const val DPOP_SCHEME = "DPoP"
    }
}