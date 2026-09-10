package com.tangem.data.sociallogin.provider

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.data.sociallogin.recovery.toDomainError
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.repository.SocialLoginProviderRepository
import com.tangem.google.auth.GoogleIdTokenProvider
import com.tangem.google.drive.AppDataDrive
import com.tangem.google.drive.GoogleDriveTokenProvider
import javax.inject.Inject

internal class DefaultSocialLoginProviderRepository @Inject constructor(
    private val googleIdTokenProvider: GoogleIdTokenProvider,
    @AppDataDrive private val appDataTokenProvider: GoogleDriveTokenProvider,
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
        // not best-effort: the recovery call has to present this exact value, so a token minted without
        // it could only be rejected, and the sign-in prompt would have been shown for nothing
        val nonce = nonceProvider.getNonce().getOrElse { error ->
            return Either.Left(error.toDomainError())
        }

        return when (provider) {
            SocialLoginProvider.Google -> googleIdTokenProvider
                .getIdToken(nonce = nonce.forProvider)
                // The Drive authorization that follows would otherwise ask again for the account just
                // chosen here, or reuse a token cached for a previous attempt's account.
                .onRight { idToken -> appDataTokenProvider.switchAccount(idToken.accountName) }
                .map { idToken -> SocialProviderIdToken(value = idToken.value, nonce = nonce.forRequest) }
                .mapLeft(GoogleIdTokenErrorConverter::convert)
        }
    }

    override suspend fun signOut(provider: SocialLoginProvider) {
        when (provider) {
            SocialLoginProvider.Google -> {
                googleIdTokenProvider.signOut()
                // Unpins the account, so a retry under another one stops writing into the previous
                // account's folder. Not signOut: the grant is still good, re-consenting would be noise.
                appDataTokenProvider.invalidate()
            }
        }
    }
}