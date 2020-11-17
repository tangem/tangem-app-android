package com.tangem.tap.domain.config

import com.tangem.tangem_sdk_new.ui.animation.VoidCallback

/**
[REDACTED_AUTHOR]
 */
data class Config(
        val coinMarketCapKey: String = "f6622117-c043-47a0-8975-9d673ce484de",
        val moonPayApiKey: String = "pk_test_kc90oYTANy7UQdBavDKGfL4K9l6VEPE",
        val moonPayApiSecretKey: String = "sk_test_V8w4M19LbDjjYOt170s0tGuvXAgyEb1C",
        val isPayIdCreationEnabled: Boolean = true,
        val isTopUpEnabled: Boolean = false,
)

class ConfigManager(
        private val localLoader: ConfigLoader,
        private val remoteLoader: ConfigLoader
) {

    var config: Config = Config()
        private set

    private var defaultConfig = Config()

    fun load(onComplete: VoidCallback? = null) {
        localLoader.loadConfig { config ->
            config.features?.forEach { setupFeature(it.name, it.value) }
            config.configValues?.forEach { setupKey(it.name, it.value) }
        }
        remoteLoader.loadConfig { config ->
            config.features?.forEach { setupFeature(it.name, it.value) }
            onComplete?.invoke()
        }
    }

    fun turnOf(name: String) {
        when (name) {
            isPayIdCreationEnabled -> config = config.copy(isPayIdCreationEnabled = false)
            isTopUpEnabled -> config = config.copy(isTopUpEnabled = false)
        }
    }

    fun resetToDefault(name: String) {
        when (name) {
            isPayIdCreationEnabled -> config = config.copy(isPayIdCreationEnabled = defaultConfig.isPayIdCreationEnabled)
            isTopUpEnabled -> config = config.copy(isTopUpEnabled = defaultConfig.isTopUpEnabled)
        }
    }

    private fun setupFeature(name: String, value: Boolean) {
        val newValue = value ?: return

        when (name) {
            isPayIdCreationEnabled -> {
                config = config.copy(isPayIdCreationEnabled = newValue)
                defaultConfig = defaultConfig.copy(isPayIdCreationEnabled = newValue)
            }
            isTopUpEnabled -> {
                config = config.copy(isTopUpEnabled = newValue)
                defaultConfig = defaultConfig.copy(isTopUpEnabled = newValue)
            }
        }
    }

    private fun setupKey(name: String, value: String) {
        when (name) {
            coinMarketCapKey -> {
                config = config.copy(coinMarketCapKey = value)
                defaultConfig = defaultConfig.copy(coinMarketCapKey = value)
            }
            moonPayApiKey -> {
                config = config.copy(moonPayApiKey = value)
                defaultConfig = defaultConfig.copy(moonPayApiKey = value)
            }
            moonPayApiSecretKey -> {
                config = config.copy(moonPayApiSecretKey = value)
                defaultConfig = defaultConfig.copy(moonPayApiSecretKey = value)
            }
        }
    }

    companion object {
        const val isPayIdCreationEnabled = "isPayIdCreationEnabled"
        const val isTopUpEnabled = "useTopUp"
        const val coinMarketCapKey = "coinMarketCapKey"
        const val moonPayApiKey = "moonPayApiKey"
        const val moonPayApiSecretKey = "moonPayApiSecretKey"
    }
}