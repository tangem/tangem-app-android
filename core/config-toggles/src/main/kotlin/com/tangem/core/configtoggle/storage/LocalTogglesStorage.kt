package com.tangem.core.configtoggle.storage

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.datasource.local.preferences.utils.storeObjectMap

/**
 * Local storage for toggles
 *
 * @property appPreferencesStore app preferences store
 * @property preferencesKey      preferences key
 *
[REDACTED_AUTHOR]
 */
internal class LocalTogglesStorage(
    private val appPreferencesStore: AppPreferencesStore,
    private val preferencesKey: Preferences.Key<String>,
) {

    suspend fun getSyncOrEmpty(): Map<String, Boolean> {
        return appPreferencesStore.getObjectMapSync<Boolean>(key = preferencesKey)
    }

    suspend fun store(value: Map<String, Boolean>) {
        appPreferencesStore.storeObjectMap(key = preferencesKey, value = value)
    }

    companion object {
        val FEATURE_TOGGLES_KEY by lazy { stringPreferencesKey(name = "featureToggles") }
        val EXCLUDED_BLOCKCHAINS_KEY by lazy { stringPreferencesKey(name = "excludedBlockchainsV2") }
    }
}