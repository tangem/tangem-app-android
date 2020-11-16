package com.tangem.tap.domain.config

import com.tangem.tangem_sdk_new.ui.animation.VoidCallback

/**
[REDACTED_AUTHOR]
 */
enum class ConfigType {
    Local, Remote
}

class Config(
        val features: MutableMap<String, Feature>,
        val configValues: MutableMap<String, ConfigurationValue>
) {
    companion object {
        fun empty(): Config = Config(mutableMapOf(), mutableMapOf())
    }
}

class ConfigManager(
        private val localLoader: ConfigLoader,
        private val remoteLoader: ConfigLoader
) {
    private var localConfig: Config = Config.empty()
    private var remoteConfig: Config = Config.empty()

    fun load(onComplete: VoidCallback? = null) {
        localLoader.loadConfig { localConfig = it }
        remoteLoader.loadConfig {
            remoteConfig = it
            onComplete?.invoke()
        }
    }

    fun featureIsActive(name: String): Boolean {
        return getFeatureWithHighestPriority(name)?.isActive() ?: false
    }

    fun getFeature(name: String, type: ConfigType? = null): Feature? {
        return when (type) {
            ConfigType.Local -> localConfig.features[name]
            ConfigType.Remote -> remoteConfig.features[name]
            else -> getFeatureWithHighestPriority(name)
        }
    }

    private fun getFeatureWithHighestPriority(name: String): Feature? {
        // if a localFeature doesn't exist then a remoteFeature won't exist to
        val localFeature = localConfig.features[name] ?: return null

        // if the localFeature has a condition, then it has higher priority than a remoteFeature
        return if (localFeature.hasCondition()) {
            localFeature
        } else {
            // if not then the remoteFeature has higher priority than the localFeature
            // if the remoteFeature doesn't exist - used the localFeature
            remoteConfig.features[name] ?: localFeature
        }
    }

    fun getConfigValue(name: String): ConfigurationValue? {
        return remoteConfig.configValues[name] ?: localConfig.configValues[name]
    }

    fun updateCondition(name: String, condition: Condition?) {
        localConfig.features[name]?.updateCondition(condition)
    }
}