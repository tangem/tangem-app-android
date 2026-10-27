package com.tangem.domain.onboarding

import com.tangem.domain.onboarding.repository.OnboardingRepository
import com.tangem.utils.coroutines.runSuspendCatching

/**
 * Use case for checking if twins onboarding was shown
 *
 * @property onboardingRepository onboarding repository
 *
[REDACTED_AUTHOR]
 */
class WasTwinsOnboardingShownUseCase(
    private val onboardingRepository: OnboardingRepository,
) {

    /** Get twins onboarding state synchronously or default value if exception will be thrown */
    suspend fun invokeSync(): Boolean {
        return runSuspendCatching { onboardingRepository.wasTwinsOnboardingShownSync() }
            .getOrDefault(defaultValue = false)
    }
}