package com.tangem.lib.auth.http

import com.tangem.lib.auth.api.RequiresDpopProof
import com.tangem.lib.auth.api.RequiresSessionAuth
import com.tangem.lib.auth.api.RequiresSessionRefresh
import okhttp3.Request
import retrofit2.Invocation

internal const val HEADER_AUTHORIZATION = "Authorization"
internal const val HEADER_DPOP = "DPoP"
internal const val DPOP_SCHEME = "DPoP"

/**
 * `true` when the Retrofit method behind this request opts into outgoing DPoP proof headers —
 * either explicitly via [RequiresDpopProof] or transitively via the umbrella [RequiresSessionAuth].
 */
internal fun Request.requiresDpopProof(): Boolean =
    hasMethodAnnotation<RequiresDpopProof>() || hasMethodAnnotation<RequiresSessionAuth>()

/**
 * `true` when the Retrofit method behind this request opts into automatic session-token refresh
 * on 401/403 — either explicitly via [RequiresSessionRefresh] or transitively via [RequiresSessionAuth].
 */
internal fun Request.requiresSessionRefresh(): Boolean =
    hasMethodAnnotation<RequiresSessionRefresh>() || hasMethodAnnotation<RequiresSessionAuth>()

/** Returns a copy of this request with `Authorization: DPoP <token>` and `DPoP: <proof>` headers set. */
internal fun Request.withDpopHeaders(accessToken: String, proof: String): Request = newBuilder()
    .header(HEADER_AUTHORIZATION, "$DPOP_SCHEME $accessToken")
    .header(HEADER_DPOP, proof)
    .build()

/**
 * Target URI for the DPoP `htu` claim — full URL stripped of query and fragment per RFC 9449 §4.2.
 * Callers must pass this (not the raw `url.toString()`) to `DpopProofFactory.create` so the contract
 * is honoured at the call site rather than relying on defensive stripping inside any one factory impl.
 */
internal fun Request.htuUrl(): String = url.toString().substringBefore('#').substringBefore('?')

/**
 * Marks a request as the result of a session-auth retry already taken for this logical call.
 *
 * [DpopAuthorizationInterceptor] (for a 403) and [SessionAuthenticator] (for a 401) each own a
 * *separate* `chain.proceed()` invocation — a request retried by one of them starts a brand-new
 * `RetryAndFollowUpInterceptor` run, so OkHttp's own `response.priorResponse` tracking (and
 * [SessionAuthenticator]'s check of it) only spans retries *within* a single such invocation, not
 * across the two. Without this shared marker, a 401 answered by the Authenticator and then followed
 * by a 403 (or the reverse) would get a second, independent retry from the other mechanism — three
 * requests total instead of the one retry the contract promises. Both mechanisms stamp this marker
 * on the request they build for their retry, and both refuse to retry a request that already carries it.
 */
private object SessionAuthRetryMarker

internal fun Request.isSessionAuthRetry(): Boolean = tag(SessionAuthRetryMarker::class.java) != null

internal fun Request.markSessionAuthRetried(): Request = newBuilder()
    .tag(SessionAuthRetryMarker::class.java, SessionAuthRetryMarker)
    .build()

private inline fun <reified A : Annotation> Request.hasMethodAnnotation(): Boolean =
    tag(Invocation::class.java)?.method()?.isAnnotationPresent(A::class.java) == true