package com.tangem.core.configtoggle.feature.impl

import androidx.annotation.VisibleForTesting
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.MutableFeatureTogglesManager
import com.tangem.core.configtoggle.storage.FeatureTogglesLocalStorage
import com.tangem.core.configtoggle.utils.defineTogglesAvailability
import com.tangem.core.configtoggle.version.VersionProvider
import kotlinx.coroutines.runBlocking
import java.util.Locale
import kotlin.properties.Delegates

/**
 * Feature toggles manager implementation in DEV build
 *
 * @property versionProvider            application version provider
 * @property featureTogglesLocalStorage local storage for feature toggles
 */
internal class DevFeatureTogglesManager(
    private val versionProvider: VersionProvider,
    private val featureTogglesLocalStorage: FeatureTogglesLocalStorage,
) : MutableFeatureTogglesManager {

    private var fileFeatureTogglesMap: Map<String, Boolean> = getFileFeatureToggles()
    private var featureTogglesMap: MutableMap<String, Boolean> by Delegates.notNull()

    init {
        val savedFeatureToggles = runBlocking { featureTogglesLocalStorage.getSyncOrEmpty() }

        featureTogglesMap = fileFeatureTogglesMap
            .mapValues { resultToggle ->
                savedFeatureToggles[resultToggle.key] ?: resultToggle.value
            }
            .toMutableMap()
    }

    override fun isFeatureEnabled(name: String): Boolean = featureTogglesMap[name] == true

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
        return buildString {
            append("DevFeatureTogglesManager:\n")
            append("|------------------------------------------|-----------|\n")
            append(String.format(Locale.getDefault(), "| %-40s | %-9s |\n", "name", "isEnabled"))
            append("|------------------------------------------|-----------|\n")
            featureTogglesMap.entries.forEachIndexed { index, (name, isEnabled) ->
                append(String.format(Locale.getDefault(), "| %-40s | %-9s |\n", name, isEnabled))
            }
            append("|------------------------------------------|-----------|")
        }
    }

    private fun getFileFeatureToggles(): Map<String, Boolean> {
        val appVersion = versionProvider.get()

        return FeatureToggles.values.defineTogglesAvailability(appVersion = appVersion)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setFeatureToggles(map: MutableMap<String, Boolean>) {
        featureTogglesMap = map
    }
}