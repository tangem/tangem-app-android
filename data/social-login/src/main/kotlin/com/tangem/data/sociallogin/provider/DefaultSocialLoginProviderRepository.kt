package com.tangem.data.sociallogin.provider

import arrow.core.Either
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.repository.SocialLoginProviderRepository
import com.tangem.google.auth.GoogleIdTokenProvider
import javax.inject.Inject

internal class DefaultSocialLoginProviderRepository @Inject constructor(
    private val googleIdTokenProvider: GoogleIdTokenProvider,
    private val nonceProvider: SocialLoginNonceProvider,
) : SocialLoginProviderRepository {

    override suspend fun getAvailableProviders(): List<SocialLoginProvider> {
        return if (googleIdTokenProvider.isAvailable()) {
            listOf(SocialLoginProvider.Google)
        } else {
            emptyList()
        }
    }

    override suspend fun signIn(provider: SocialLoginProvider): Either<SocialLoginError, SocialProviderIdToken> {
        return when (provider) {
            SocialLoginProvider.Google -> googleIdTokenProvider
                .getIdToken(nonce = nonceProvider.getNonce())
                .map(::SocialProviderIdToken)
                .mapLeft(GoogleIdTokenErrorConverter::convert)
        }
    }

    override suspend fun signOut(provider: SocialLoginProvider) {
        when (provider) {
            SocialLoginProvider.Google -> googleIdTokenProvider.signOut()
        }
    }
}