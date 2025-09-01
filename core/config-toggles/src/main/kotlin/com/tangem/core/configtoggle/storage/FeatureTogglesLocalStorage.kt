package com.tangem.core.configtoggle.storage

import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.datasource.local.preferences.utils.storeObjectMap

/**
 * Local storage for feature toggles
 *
 * @property appPreferencesStore app preferences store
 *
[REDACTED_AUTHOR]
 */
internal class FeatureTogglesLocalStorage(
    private val appPreferencesStore: AppPreferencesStore,
) {

    suspend fun getSyncOrEmpty(): Map<String, Boolean> {
        return appPreferencesStore.getObjectMapSync<Boolean>(key = FEATURE_TOGGLES_KEY)
    }

    suspend fun store(value: Map<String, Boolean>) {
        appPreferencesStore.storeObjectMap(key = FEATURE_TOGGLES_KEY, value = value)
    }

    private companion object {

        val FEATURE_TOGGLES_KEY by lazy { stringPreferencesKey(name = "featureToggles") }
    }
}