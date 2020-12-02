package com.tangem.tap.domain.config

import com.tangem.tangem_sdk_new.ui.animation.VoidCallback

/**
[REDACTED_AUTHOR]
 */
data class Config(
        val coinMarketCapKey: String = "f6622117-c043-47a0-8975-9d673ce484de",
        val moonPayApiKey: String = "pk_test_kc90oYTANy7UQdBavDKGfL4K9l6VEPE",
        val moonPayApiSecretKey: String = "sk_test_V8w4M19LbDjjYOt170s0tGuvXAgyEb1C",
        val isWalletPayIdEnabled: Boolean = true,
        val isTopUpEnabled: Boolean = false,
        val isCreatingTwinCardsAllowed: Boolean = false
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

    fun turnOff(name: String) {
        when (name) {
            isWalletPayIdEnabled -> config = config.copy(isWalletPayIdEnabled = false)
            isTopUpEnabled -> config = config.copy(isTopUpEnabled = false)
            isCreatingTwinCardsAllowed -> config = config.copy(isCreatingTwinCardsAllowed = false)
        }
    }

    fun resetToDefault(name: String) {
        when (name) {
            isWalletPayIdEnabled -> config = config.copy(isWalletPayIdEnabled = defaultConfig.isWalletPayIdEnabled)
            isTopUpEnabled -> config = config.copy(isTopUpEnabled = defaultConfig.isTopUpEnabled)
            isCreatingTwinCardsAllowed -> config =
                    config.copy(isCreatingTwinCardsAllowed = defaultConfig.isCreatingTwinCardsAllowed)
        }
    }

    private fun setupFeature(name: String, value: Boolean) {
        val newValue = value ?: return

        when (name) {
            isWalletPayIdEnabled -> {
                config = config.copy(isWalletPayIdEnabled = newValue)
                defaultConfig = defaultConfig.copy(isWalletPayIdEnabled = newValue)
            }
            isTopUpEnabled -> {
                config = config.copy(isTopUpEnabled = newValue)
                defaultConfig = defaultConfig.copy(isTopUpEnabled = newValue)
            }
            isCreatingTwinCardsAllowed -> {
                config = config.copy(isCreatingTwinCardsAllowed = newValue)
                defaultConfig = defaultConfig.copy(isCreatingTwinCardsAllowed = newValue)
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
        const val isWalletPayIdEnabled = "isWalletPayIdEnabled"
        const val isCreatingTwinCardsAllowed = "isCreatingTwinCardsAllowed"
        const val isTopUpEnabled = "isTopUpEnabled"
        const val coinMarketCapKey = "coinMarketCapKey"
        const val moonPayApiKey = "moonPayApiKey"
        const val moonPayApiSecretKey = "moonPayApiSecretKey"
    }
}