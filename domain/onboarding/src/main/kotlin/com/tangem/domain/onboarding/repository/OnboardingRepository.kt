package com.tangem.domain.onboarding.repository

import com.tangem.domain.models.scan.ScanResponse
import kotlinx.coroutines.flow.Flow

/**
 * Onboarding repository
 *
 * @author Andrew Khokhlov on 18/02/2024
 */
interface OnboardingRepository {

    fun wasTwinsOnboardingShown(): Flow<Boolean>

    @Throws
    suspend fun wasTwinsOnboardingShownSync(): Boolean

    suspend fun saveTwinsOnboardingShown()

    suspend fun getUnfinishedFinalizeOnboarding(): ScanResponse?

    suspend fun saveUnfinishedFinalizeOnboarding(scanResponse: ScanResponse)

    suspend fun clearUnfinishedFinalizeOnboarding()
}
