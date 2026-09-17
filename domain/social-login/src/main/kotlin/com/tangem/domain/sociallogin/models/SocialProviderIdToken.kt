package com.tangem.domain.sociallogin.models

/**
 * OIDC ID token issued by the social provider after a successful sign-in.
 *
 * The app never parses it: the identity (`issuer` + `sub`) the binding is keyed by is derived on the
 * backend, which also validates signature, audience, expiry and nonce. A short-lived credential
 * that must not reach logs or analytics, hence the masked [toString].
 */
data class SocialProviderIdToken(
    val value: String,
    /**
     * The server nonce this token was minted with. Travels with the token because the recovery call
     * has to present the very same value the `nonce` claim carries, and the backend consumes it there.
     */
    val nonce: String,
) {

    override fun toString(): String = "SocialProviderIdToken(***)"
}