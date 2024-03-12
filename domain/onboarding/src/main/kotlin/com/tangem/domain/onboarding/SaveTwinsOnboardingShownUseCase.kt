package com.tangem.domain.onboarding

import com.tangem.domain.onboarding.repository.OnboardingRepository

/**
 * Saves that twins onboarding was shown
 *
 * @property onboardingRepository onboarding repository
 *
[REDACTED_AUTHOR]
 */
class SaveTwinsOnboardingShownUseCase(
    private val onboardingRepository: OnboardingRepository,
) {

    suspend operator fun invoke() {
        onboardingRepository.saveTwinsOnboardingShown()
    }
}