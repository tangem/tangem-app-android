package com.tangem.core.featuretoggle.manager

import androidx.annotation.VisibleForTesting
import com.tangem.core.featuretoggle.storage.FeatureTogglesStorage
import com.tangem.core.featuretoggle.utils.associateToggles
import com.tangem.core.featuretoggle.version.VersionProvider
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import kotlin.properties.Delegates

/**
 * Feature toggles manager implementation in DEV build
 *
 * @property localFeatureTogglesStorage local feature toggles storage
 * @property appPreferencesStore        application local store
 * @property versionProvider            application version provider
 */
internal class DevFeatureTogglesManager(
    private val localFeatureTogglesStorage: FeatureTogglesStorage,
    private val appPreferencesStore: AppPreferencesStore,
    private val versionProvider: VersionProvider,
) : MutableFeatureTogglesManager {

    private var featureTogglesMap: MutableMap<String, Boolean> by Delegates.notNull()
    private var localFeatureTogglesMap: Map<String, Boolean> by Delegates.notNull()

    override suspend fun init() {
        localFeatureTogglesStorage.init()

        val savedFeatureToggles = appPreferencesStore.getObjectSyncOrNull<Map<String, Boolean>>(
            key = PreferencesKeys.FEATURE_TOGGLES_KEY,
        ) ?: emptyMap()

        val localFeatureToggles = localFeatureTogglesStorage.featureToggles
            .associateToggles(currentVersion = versionProvider.get().orEmpty())

        localFeatureTogglesMap = localFeatureToggles

        featureTogglesMap = localFeatureToggles
            .mapValues { resultToggle ->
                savedFeatureToggles[resultToggle.key] ?: resultToggle.value
            }
            .toMutableMap()
    }

    override fun isFeatureEnabled(name: String): Boolean = featureTogglesMap[name] ?: false

    override fun isMatchLocalConfig(): Boolean = featureTogglesMap == localFeatureTogglesMap

    override fun getFeatureToggles(): Map<String, Boolean> = featureTogglesMap

    override suspend fun changeToggle(name: String, isEnabled: Boolean) {
        featureTogglesMap[name] ?: return
        featureTogglesMap[name] = isEnabled
        appPreferencesStore.storeFeatureToggles(value = featureTogglesMap)
    }

    override suspend fun recoverLocalConfig() {
        featureTogglesMap = localFeatureTogglesMap.toMutableMap()
        appPreferencesStore.storeFeatureToggles(value = localFeatureTogglesMap)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setFeatureToggles(map: MutableMap<String, Boolean>) {
        featureTogglesMap = map
    }

    private suspend fun AppPreferencesStore.storeFeatureToggles(value: Map<String, Boolean>) {
        storeObject(PreferencesKeys.FEATURE_TOGGLES_KEY, value)
    }
}
