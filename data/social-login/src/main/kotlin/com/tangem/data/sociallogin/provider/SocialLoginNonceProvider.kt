package com.tangem.data.sociallogin.provider

import com.tangem.lib.auth.social.SignInNonce
import com.tangem.lib.auth.social.SocialRecoveryClient
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * Supplies the server nonce the provider id token is bound to, so a token obtained elsewhere cannot be
 * replayed against the recovery backend.
 */
internal interface SocialLoginNonceProvider {

    /** `null` when the backend cannot issue one */
    suspend fun getNonce(): SignInNonce?
}

internal class BackendSocialLoginNonceProvider @Inject constructor(
    private val client: SocialRecoveryClient,
) : SocialLoginNonceProvider {

    override suspend fun getNonce(): SignInNonce? = client.requestSignInNonce()
        .onLeft { TangemLogger.e("Failed to obtain the social sign-in nonce: ${it::class.simpleName}") }
        .getOrNull()
}