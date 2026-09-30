package com.tangem.domain.sociallogin.usecase

import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.repository.SocialLoginProviderRepository
import javax.inject.Inject

/**
 * Forgets the picked provider account so the next sign-in asks for one again — the "switch account"
 * action offered when the chosen identity has no wallet or the flow has to be retried.
 */
class SignOutFromSocialProviderUseCase @Inject constructor(
    private val socialLoginProviderRepository: SocialLoginProviderRepository,
) {

    suspend operator fun invoke(provider: SocialLoginProvider) {
        socialLoginProviderRepository.signOut(provider)
    }
}