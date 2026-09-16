package com.tangem.data.onboarding

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.datasource.local.preferences.utils.*
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.onboarding.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow

/**
 * Default implementation of [OnboardingRepository]
 *
 * @property appPreferencesStore app preferences store
 *
[REDACTED_AUTHOR]
 */
internal class DefaultOnboardingRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val storePreferencesKeys: StorePreferencesKeys,
) : OnboardingRepository {

    override fun wasTwinsOnboardingShown(): Flow<Boolean> {
        return appPreferencesStore.get(key = storePreferencesKeys.wasTwinsOnboardingShownKey, default = false)
    }

    override suspend fun wasTwinsOnboardingShownSync(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = storePreferencesKeys.wasTwinsOnboardingShownKey,
            default = false,
        )
    }

    override suspend fun saveTwinsOnboardingShown() {
        appPreferencesStore.store(key = storePreferencesKeys.wasTwinsOnboardingShownKey, value = true)
    }

    override suspend fun getUnfinishedFinalizeOnboarding(): ScanResponse? {
        return appPreferencesStore
            .getObjectSyncOrNull<ScanResponse>(storePreferencesKeys.onboardingFinalizeScanResponseKey)
    }

    override suspend fun saveUnfinishedFinalizeOnboarding(scanResponse: ScanResponse) {
        appPreferencesStore
            .storeObject<ScanResponse>(
                storePreferencesKeys.onboardingFinalizeScanResponseKey,
                scanResponse,
            )
    }

    override suspend fun clearUnfinishedFinalizeOnboarding() {
        appPreferencesStore.editData { mutablePreferences ->
            mutablePreferences.remove(storePreferencesKeys.onboardingFinalizeScanResponseKey)
        }
    }
}