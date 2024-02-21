package com.tangem.data.onboarding

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
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
) : OnboardingRepository {

    override fun wasTwinsOnboardingShown(): Flow<Boolean> {
        return appPreferencesStore.get(key = PreferencesKeys.WAS_TWINS_ONBOARDING_SHOWN, default = false)
    }

    override suspend fun wasTwinsOnboardingShownSync(): Boolean {
        return appPreferencesStore.getSyncOrDefault(key = PreferencesKeys.WAS_TWINS_ONBOARDING_SHOWN, default = false)
    }

    override suspend fun saveTwinsOnboardingShown() {
        appPreferencesStore.store(key = PreferencesKeys.WAS_TWINS_ONBOARDING_SHOWN, value = true)
    }
}