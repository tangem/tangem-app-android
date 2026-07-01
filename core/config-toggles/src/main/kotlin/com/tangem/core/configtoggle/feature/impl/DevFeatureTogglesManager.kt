package com.tangem.core.configtoggle.feature.impl

import androidx.annotation.VisibleForTesting
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureToggleInfo
import com.tangem.core.configtoggle.feature.MutableFeatureTogglesManager
import com.tangem.core.configtoggle.feature.provider.FeatureTogglesProvider
import com.tangem.core.configtoggle.storage.LocalTogglesStorage
import com.tangem.core.configtoggle.utils.defineTogglesAvailability
import com.tangem.core.configtoggle.utils.toTableString
import com.tangem.core.configtoggle.version.VersionProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

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

    private val fileFeatureToggles: List<FeatureToggleInfo> = buildFileFeatureToggles()

    private val currentToggles: MutableStateFlow<List<FeatureToggleInfo>> = MutableStateFlow(buildInitialToggles())

    override fun isFeatureEnabled(toggle: FeatureToggles): Boolean =
        currentToggles.value.any { it.name == toggle.rawName && it.isEnabled }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun isFeatureEnabledByName(name: String): Boolean = currentToggles.value.any { it.name == name && it.isEnabled }

    override fun getFeatureToggles(): List<FeatureToggleInfo> = currentToggles.value

    override fun isMatchLocalConfig(): Boolean =
        currentToggles.value.associateBy { it.name } == fileFeatureToggles.associateBy { it.name }

    override suspend fun changeToggle(name: String, isEnabled: Boolean) {
        if (currentToggles.value.none { it.name == name }) return
        currentToggles.update { toggles ->
            toggles.map { toggle ->
                if (toggle.name == name) toggle.copy(isEnabled = isEnabled) else toggle
            }
        }
        featureTogglesLocalStorage.store(value = currentToggles.value.toAvailabilityMap())
    }

    override suspend fun recoverLocalConfig() {
        currentToggles.value = fileFeatureToggles
        featureTogglesLocalStorage.store(value = currentToggles.value.toAvailabilityMap())
    }

    override fun toString(): String {
        return currentToggles.value.toAvailabilityMap()
            .toTableString(tableName = this@DevFeatureTogglesManager::class.java.simpleName)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setFeatureToggles(map: MutableMap<String, Boolean>) {
        currentToggles.value = map.map { (name, isEnabled) ->
            val version = fileFeatureToggles.firstOrNull { it.name == name }?.version.orEmpty()
            FeatureToggleInfo(name = name, version = version, isEnabled = isEnabled)
        }
    }

    private fun buildInitialToggles(): List<FeatureToggleInfo> {
        val savedFeatureToggles = runBlocking { featureTogglesLocalStorage.getSyncOrEmpty() }
        return fileFeatureToggles.map { it.copy(isEnabled = savedFeatureToggles[it.name] ?: it.isEnabled) }
    }

    private fun buildFileFeatureToggles(): List<FeatureToggleInfo> {
        val rawToggles = featureTogglesProvider.getToggles()
        val availability = rawToggles.defineTogglesAvailability(appVersion = versionProvider.get())
        return rawToggles.map { (name, version) ->
            FeatureToggleInfo(name = name, version = version, isEnabled = availability.getValue(name))
        }
    }

    private fun List<FeatureToggleInfo>.toAvailabilityMap(): Map<String, Boolean> =
        associate { it.name to it.isEnabled }
}