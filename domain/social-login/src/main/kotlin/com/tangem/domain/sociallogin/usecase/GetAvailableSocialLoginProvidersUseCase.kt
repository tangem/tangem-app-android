package com.tangem.domain.sociallogin.usecase

import com.tangem.domain.sociallogin.SocialLoginFeatureToggles
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.repository.SocialLoginProviderRepository
import javax.inject.Inject

/**
 * Providers the onboarding may offer: none while the feature is off, otherwise the ones usable on this
 * device (FR-13).
 */
class GetAvailableSocialLoginProvidersUseCase @Inject constructor(
    private val socialLoginProviderRepository: SocialLoginProviderRepository,
    private val socialLoginFeatureToggles: SocialLoginFeatureToggles,
) {

    suspend operator fun invoke(): List<SocialLoginProvider> {
        return if (socialLoginFeatureToggles.isSocialLoginEnabled) {
            socialLoginProviderRepository.getAvailableProviders()
        } else {
            emptyList()
        }
    }
}