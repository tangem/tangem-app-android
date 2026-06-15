package com.tangem.lib.auth.http

import com.tangem.datasource.api.auth.RequiresDpopProof
import com.tangem.datasource.api.auth.RequiresSessionAuth
import com.tangem.datasource.api.auth.RequiresSessionRefresh
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

private inline fun <reified A : Annotation> Request.hasMethodAnnotation(): Boolean =
    tag(Invocation::class.java)?.method()?.isAnnotationPresent(A::class.java) == true