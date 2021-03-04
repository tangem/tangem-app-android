package com.tangem.tap.domain.configurable.config

import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.tangem_sdk_new.ui.animation.VoidCallback
import com.tangem.tap.domain.configurable.Loader

/**
* [REDACTED_AUTHOR]
 */
data class Config(
        val coinMarketCapKey: String = "f6622117-c043-47a0-8975-9d673ce484de",
        val moonPayApiKey: String = "pk_test_kc90oYTANy7UQdBavDKGfL4K9l6VEPE",
        val moonPayApiSecretKey: String = "sk_test_V8w4M19LbDjjYOt170s0tGuvXAgyEb1C",
        val blockchainSdkConfig: BlockchainSdkConfig = BlockchainSdkConfig(),
        val isWalletPayIdEnabled: Boolean = true,
        val isSendingToPayIdEnabled: Boolean = true,
        val isTopUpEnabled: Boolean = false,
        val isCreatingTwinCardsAllowed: Boolean = false
)

class ConfigManager(
        private val localLoader: Loader<ConfigModel>,
        private val remoteLoader: Loader<ConfigModel>
) {

    var config: Config = Config()
        private set

    private var defaultConfig = Config()

    fun load(onComplete: VoidCallback? = null) {
        localLoader.load { config ->
            setupFeature(config.features)
            setupKey(config.configValues)
        }
        remoteLoader.load { config ->
            setupFeature(config.features)
            onComplete?.invoke()
        }
    }

    fun turnOff(name: String) {
        when (name) {
            isWalletPayIdEnabled -> config = config.copy(isWalletPayIdEnabled = false)
            isSendingToPayIdEnabled -> config = config.copy(isSendingToPayIdEnabled = false)
            isTopUpEnabled -> config = config.copy(isTopUpEnabled = false)
            isCreatingTwinCardsAllowed -> config = config.copy(isCreatingTwinCardsAllowed = false)
        }
    }

    fun resetToDefault(name: String) {
        when (name) {
            isWalletPayIdEnabled -> config = config.copy(isWalletPayIdEnabled = defaultConfig.isWalletPayIdEnabled)
            isSendingToPayIdEnabled -> config = config.copy(isSendingToPayIdEnabled = defaultConfig.isSendingToPayIdEnabled)
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
                isSendingToPayIdEnabled = model.isSendingToPayIdEnabled,
                isCreatingTwinCardsAllowed = model.isCreatingTwinCardsAllowed
        )
        defaultConfig = defaultConfig.copy(
                isWalletPayIdEnabled = model.isWalletPayIdEnabled,
                isTopUpEnabled = model.isTopUpEnabled,
                isSendingToPayIdEnabled = model.isSendingToPayIdEnabled,
                isCreatingTwinCardsAllowed = model.isCreatingTwinCardsAllowed
        )
    }

    private fun setupKey(configValues: ConfigValueModel?) {
        val values = configValues ?: return
        config = config.copy(
                coinMarketCapKey = values.coinMarketCapKey,
                moonPayApiKey = values.moonPayApiKey,
                moonPayApiSecretKey = values.moonPayApiSecretKey,
                blockchainSdkConfig = BlockchainSdkConfig(
                        blockchairApiKey = values.blockchairApiKey,
                        blockcypherTokens = values.blockcypherTokens,
                        infuraProjectId =  values.infuraProjectId
                )
        )
        defaultConfig = defaultConfig.copy(
                coinMarketCapKey = values.coinMarketCapKey,
                moonPayApiKey = values.moonPayApiKey,
                moonPayApiSecretKey = values.moonPayApiSecretKey,
                blockchainSdkConfig = BlockchainSdkConfig(
                        blockchairApiKey = values.blockchairApiKey,
                        blockcypherTokens = values.blockcypherTokens,
                        infuraProjectId =  values.infuraProjectId
                )
        )
    }

    companion object {
        const val isWalletPayIdEnabled = "isWalletPayIdEnabled"
        const val isSendingToPayIdEnabled = "isSendingToPayIdEnabled"
        const val isCreatingTwinCardsAllowed = "isCreatingTwinCardsAllowed"
        const val isTopUpEnabled = "isTopUpEnabled"
    }
}