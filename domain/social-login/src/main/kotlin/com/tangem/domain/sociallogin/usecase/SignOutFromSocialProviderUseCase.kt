package com.tangem.domain.sociallogin.usecase

import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.repository.SocialLoginProviderRepository
import javax.inject.Inject


class SignOutFromSocialProviderUseCase @Inject constructor(
    private val socialLoginProviderRepository: SocialLoginProviderRepository,
) {

    suspend operator fun invoke(provider: SocialLoginProvider) {
        socialLoginProviderRepository.signOut(provider)
    }
}