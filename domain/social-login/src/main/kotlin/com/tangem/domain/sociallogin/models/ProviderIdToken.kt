package com.tangem.domain.sociallogin.models

/**
 * OIDC ID token issued by the social provider after a successful sign-in.
 *
 * The app never parses it: the provider identity (`issuer` + `sub`) that the wallet binding is keyed by
 * is derived on the backend, which also validates signature, audience, expiry and nonce (FR-05). The
 * token is a short-lived credential and must not reach logs, analytics or crash reports (NFR-04),
 * hence the masked [toString].
 */
@JvmInline
value class ProviderIdToken(val value: String) {

    override fun toString(): String = "ProviderIdToken(***)"
}