package com.tangem.core.configtoggle.feature.impl

import androidx.annotation.VisibleForTesting
import com.tangem.core.configtoggle.feature.MutableFeatureTogglesManager
import com.tangem.core.configtoggle.storage.TogglesStorage
import com.tangem.core.configtoggle.utils.associateToggles
import com.tangem.core.configtoggle.version.VersionProvider
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import kotlin.properties.Delegates

/**
 * Feature toggles manager implementation in DEV build
 *
 * @property localTogglesStorage local feature toggles storage
 * @property appPreferencesStore        application local store
 * @property versionProvider            application version provider
 */
internal class DevFeatureTogglesManager(
    private val localTogglesStorage: TogglesStorage,
    private val appPreferencesStore: AppPreferencesStore,
    private val versionProvider: VersionProvider,
) : MutableFeatureTogglesManager {

    private var featureTogglesMap: MutableMap<String, Boolean> by Delegates.notNull()
    private var localFeatureTogglesMap: Map<String, Boolean> by Delegates.notNull()

    override suspend fun init() {
        localTogglesStorage.populate(FeatureTogglesConstants.LOCAL_CONFIG_PATH)

        val savedFeatureToggles = appPreferencesStore.getObjectSyncOrNull<Map<String, Boolean>>(
            key = PreferencesKeys.FEATURE_TOGGLES_KEY,
        ) ?: emptyMap<String, Boolean>()

        val localFeatureToggles = localTogglesStorage.toggles
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