package com.tangem.domain.onboarding

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.onboarding.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

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

    /** Get flow with twins onboarding state or error [Throwable] */
    operator fun invoke(): Flow<Either<Throwable, Boolean>> {
        return onboardingRepository.wasTwinsOnboardingShown()
            .map(Boolean::right)
            .catch { it.left() }
    }

    /** Get twins onboarding state synchronously or default value if exception will be thrown */
    suspend fun invokeSync(): Boolean {
        return runCatching { onboardingRepository.wasTwinsOnboardingShownSync() }.getOrDefault(defaultValue = false)
    }
}