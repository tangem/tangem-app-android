package com.tangem.data.sociallogin.provider

/**
 * Supplies the server nonce the provider id token is bound to, so a token obtained elsewhere cannot be
 * replayed against the recovery backend (SEC-03).
 */
internal interface SocialLoginNonceProvider {

    /** `null` when the backend cannot issue one */
    suspend fun getNonce(): String?
}

/**
 * Issues no nonce, leaving every id token unbound and SEC-03 unmet: a locally generated value would
 * give no replay protection, so the nonce can only come from the backend. Superseded once the recovery
 * API exposes an endpoint that issues one.
 */
internal class NoServerNonceProvider : SocialLoginNonceProvider {

    override suspend fun getNonce(): String? = null
}