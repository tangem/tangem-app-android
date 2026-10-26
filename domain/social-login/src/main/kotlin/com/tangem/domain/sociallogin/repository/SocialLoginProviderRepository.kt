package com.tangem.domain.sociallogin.repository

import arrow.core.Either
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider

/** Sign-in with the social identity providers the wallet binding is keyed by */
interface SocialLoginProviderRepository {

    /**
     * Providers that can actually be used on this device. Google is absent when Google services are
     * missing, so the caller hides the button instead of starting a flow that cannot finish.
     */
    suspend fun getAvailableProviders(): List<SocialLoginProvider>

    /**
     * Signs in with [provider] and returns the id token to hand to the backend. The server nonce the
     * token is bound to against replay is obtained by the implementation, not by the caller.
     */
    suspend fun signIn(provider: SocialLoginProvider): Either<SocialLoginError, SocialProviderIdToken>

    /**
     * Forgets the account picked for [provider] so the next [signIn] shows the account chooser again.
     * Backs the "switch account" recovery action of the error states.
     */
    suspend fun signOut(provider: SocialLoginProvider)
}