package com.tangem.domain.onboarding

import com.tangem.domain.onboarding.repository.OnboardingRepository

/**
 * Saves that twins onboarding was shown
 *
 * @property onboardingRepository onboarding repository
 *
 * @author Andrew Khokhlov on 18/02/2024
 */
class SaveTwinsOnboardingShownUseCase(
    private val onboardingRepository: OnboardingRepository,
) {

    suspend operator fun invoke() {
        onboardingRepository.saveTwinsOnboardingShown()
    }
}
