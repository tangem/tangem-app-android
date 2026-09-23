package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.repository.SocialLoginProviderRepository
import javax.inject.Inject

/** Signs in with a social provider and returns the id token the wallet binding is resolved by. */
class SignInWithSocialProviderUseCase @Inject constructor(
    private val socialLoginProviderRepository: SocialLoginProviderRepository,
) {

    suspend operator fun invoke(provider: SocialLoginProvider): Either<SocialLoginError, SocialProviderIdToken> {
        return socialLoginProviderRepository.signIn(provider)
    }
}