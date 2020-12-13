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
            setupFeature(config.features)
            setupKey(config.configValues)
        }
        remoteLoader.loadConfig { config ->
            setupFeature(config.features)
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

    private fun setupFeature(featureModel: FeatureModel?) {
        val model = featureModel ?: return

        config = config.copy(
                isWalletPayIdEnabled = model.isWalletPayIdEnabled,
                isTopUpEnabled = model.isTopUpEnabled,
                isCreatingTwinCardsAllowed = model.isCreatingTwinCardsAllowed
        )
        defaultConfig = defaultConfig.copy(
                isWalletPayIdEnabled = model.isWalletPayIdEnabled,
                isTopUpEnabled = model.isTopUpEnabled,
                isCreatingTwinCardsAllowed = model.isCreatingTwinCardsAllowed
        )
    }

    private fun setupKey(configValues: ConfigValueModel?) {
        val values = configValues ?: return
        config = config.copy(
                coinMarketCapKey = values.coinMarketCapKey,
                moonPayApiKey = values.moonPayApiKey,
                moonPayApiSecretKey = values.moonPayApiSecretKey
        )
        defaultConfig = defaultConfig.copy(
                coinMarketCapKey = values.coinMarketCapKey,
                moonPayApiKey = values.moonPayApiKey,
                moonPayApiSecretKey = values.moonPayApiSecretKey
        )
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