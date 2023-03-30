package com.tangem.core.featuretoggle.manager

import androidx.annotation.VisibleForTesting
import com.squareup.moshi.JsonAdapter
import com.tangem.core.featuretoggle.storage.FeatureTogglesStorage
import com.tangem.core.featuretoggle.utils.associateToggles
import com.tangem.core.featuretoggle.version.VersionProvider
import com.tangem.datasource.local.AppPreferenceStorage
import kotlin.properties.Delegates

/**
 * Feature toggles manager implementation in DEV build
 *
 * @property localFeatureTogglesStorage local feature toggles storage
 * @property appPreferenceStorage       application local storage
 * @property jsonAdapter                adapter for parsing json
 * @property versionProvider            application version provider
 */
internal class DevFeatureTogglesManager(
    private val localFeatureTogglesStorage: FeatureTogglesStorage,
    private val appPreferenceStorage: AppPreferenceStorage,
    private val jsonAdapter: JsonAdapter<Map<String, Boolean>>,
    private val versionProvider: VersionProvider,
) : MutableFeatureTogglesManager {

    private var featureTogglesMap: MutableMap<String, Boolean> by Delegates.notNull()

    override suspend fun init() {
        localFeatureTogglesStorage.init()

        val savedFeatureToggles = if (appPreferenceStorage.featureToggles.isNotEmpty()) {
            jsonAdapter.fromJson(appPreferenceStorage.featureToggles).orEmpty()
        } else {
            emptyMap()
        }

        featureTogglesMap = localFeatureTogglesStorage.featureToggles
            .associateToggles(currentVersion = versionProvider.get().orEmpty())
            .mapValues { resultToggle ->
                savedFeatureToggles[resultToggle.key] ?: resultToggle.value
            }
            .toMutableMap()
    }

    override fun isFeatureEnabled(name: String): Boolean = featureTogglesMap[name] ?: false

    override fun getFeatureToggles(): Map<String, Boolean> = featureTogglesMap

    override fun changeToggle(name: String, isEnabled: Boolean) {
        featureTogglesMap[name] ?: return
        featureTogglesMap[name] = isEnabled
        appPreferenceStorage.featureToggles = jsonAdapter.toJson(featureTogglesMap)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setFeatureToggles(map: MutableMap<String, Boolean>) {
        featureTogglesMap = map
    }
}
