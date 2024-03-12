package com.tangem.domain.onboarding.repository

import kotlinx.coroutines.flow.Flow

/**
 * Onboarding repository
 *
[REDACTED_AUTHOR]
 */
interface OnboardingRepository {

    fun wasTwinsOnboardingShown(): Flow<Boolean>

    @Throws
    suspend fun wasTwinsOnboardingShownSync(): Boolean

    suspend fun saveTwinsOnboardingShown()
}