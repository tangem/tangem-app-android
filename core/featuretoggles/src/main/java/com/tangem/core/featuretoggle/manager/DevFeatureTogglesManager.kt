package com.tangem.core.featuretoggle.manager

import com.squareup.moshi.JsonAdapter
import com.tangem.core.featuretoggle.comparator.VersionContract
import com.tangem.core.featuretoggle.storage.FeatureTogglesStorage
import com.tangem.core.featuretoggle.utils.associate
import com.tangem.datasource.local.AppPreferenceStorage
import kotlin.properties.Delegates

/**
 * Manager implementation for getting information about the availability of feature toggles
 * in DEV build
 *
 * @property localFeatureTogglesStorage local feature toggles storage
 * @property versionContract            contract that returns the availability of feature toggle version
 * @property appPreferenceStorage       application local storage
 * @property jsonAdapter                adapter for parsing json
 */
internal class DevFeatureTogglesManager(
    private val localFeatureTogglesStorage: FeatureTogglesStorage,
    private val versionContract: VersionContract,
    private val appPreferenceStorage: AppPreferenceStorage,
    private val jsonAdapter: JsonAdapter<Map<String, Boolean>>,
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
            .associate(versionContract)
            .mapValues { resultToggle ->
                savedFeatureToggles[resultToggle.key] ?: resultToggle.value
            }
            .toMutableMap()
    }

    override fun isFeatureEnabled(toggle: IFeatureToggle): Boolean = featureTogglesMap.any { it.key == toggle.name }

    override fun getFeatureToggles(): Map<String, Boolean> = featureTogglesMap

    override fun changeToggle(name: String, isEnabled: Boolean) {
        featureTogglesMap[name] ?: return
        featureTogglesMap[name] = isEnabled
        appPreferenceStorage.featureToggles = jsonAdapter.toJson(featureTogglesMap)
    }
}
