package com.tangem.domain.onboarding.repository

import com.tangem.domain.models.scan.ScanResponse

/**
 * Onboarding repository
 *
[REDACTED_AUTHOR]
 */
interface OnboardingRepository {

    @Throws
    suspend fun wasTwinsOnboardingShownSync(): Boolean

    suspend fun saveTwinsOnboardingShown()

    suspend fun getUnfinishedFinalizeOnboarding(): ScanResponse?

    suspend fun saveUnfinishedFinalizeOnboarding(scanResponse: ScanResponse)

    suspend fun clearUnfinishedFinalizeOnboarding()
}