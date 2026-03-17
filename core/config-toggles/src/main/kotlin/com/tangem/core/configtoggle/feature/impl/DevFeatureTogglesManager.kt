package com.tangem.core.configtoggle.feature.impl

import androidx.annotation.VisibleForTesting
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.MutableFeatureTogglesManager
import com.tangem.core.configtoggle.feature.provider.FeatureTogglesProvider
import com.tangem.core.configtoggle.storage.LocalTogglesStorage
import com.tangem.core.configtoggle.utils.defineTogglesAvailability
import com.tangem.core.configtoggle.utils.toTableString
import com.tangem.core.configtoggle.version.VersionProvider
import kotlinx.coroutines.runBlocking
import kotlin.properties.Delegates

/**
 * Feature toggles manager implementation in dev or mocked build
 *
 * @property versionProvider            application version provider
 * @property featureTogglesProvider     provider for feature toggle entries
 * @property featureTogglesLocalStorage local storage for feature toggles
 */
internal class DevFeatureTogglesManager(
    private val versionProvider: VersionProvider,
    private val featureTogglesProvider: FeatureTogglesProvider,
    private val featureTogglesLocalStorage: LocalTogglesStorage,
) : MutableFeatureTogglesManager {

    private val fileFeatureTogglesMap: Map<String, Boolean> = getFileFeatureToggles()

    @Suppress("DoubleMutabilityForCollection")
    private var featureTogglesMap: MutableMap<String, Boolean> by Delegates.notNull()

    init {
        val savedFeatureToggles = runBlocking { featureTogglesLocalStorage.getSyncOrEmpty() }

        featureTogglesMap = fileFeatureTogglesMap
            .mapValues { resultToggle ->
                savedFeatureToggles[resultToggle.key] ?: resultToggle.value
            }
            .toMutableMap()
    }

    override fun isFeatureEnabled(toggle: FeatureToggles): Boolean = featureTogglesMap[toggle.rawName] == true

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun isFeatureEnabledByName(name: String): Boolean = featureTogglesMap[name] == true

    override fun getFeatureToggles(): Map<String, Boolean> = featureTogglesMap

    override fun isMatchLocalConfig(): Boolean = featureTogglesMap == fileFeatureTogglesMap

    override suspend fun changeToggle(name: String, isEnabled: Boolean) {
        featureTogglesMap[name] ?: return
        featureTogglesMap[name] = isEnabled
        featureTogglesLocalStorage.store(value = featureTogglesMap)
    }

    override suspend fun recoverLocalConfig() {
        featureTogglesMap = fileFeatureTogglesMap.toMutableMap()
        featureTogglesLocalStorage.store(value = fileFeatureTogglesMap)
    }

    override fun toString(): String {
        return featureTogglesMap.toTableString(tableName = this@DevFeatureTogglesManager::class.java.simpleName)
    }

    private fun getFileFeatureToggles(): Map<String, Boolean> {
        val appVersion = versionProvider.get()

        return featureTogglesProvider.getToggles()
            .defineTogglesAvailability(appVersion = appVersion)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setFeatureToggles(map: MutableMap<String, Boolean>) {
        featureTogglesMap = map
    }
}