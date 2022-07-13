package com.tangem.tap.domain.configurable.config

import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.tap.common.shop.shopify.ShopifyShop
import com.tangem.tap.common.zendesk.ZendeskConfig
import com.tangem.tap.domain.configurable.Loader

/**
* [REDACTED_AUTHOR]
 */
data class Config(
    val coinMarketCapKey: String = "f6622117-c043-47a0-8975-9d673ce484de",
    val moonPayApiKey: String = "pk_test_kc90oYTANy7UQdBavDKGfL4K9l6VEPE",
    val moonPayApiSecretKey: String = "sk_test_V8w4M19LbDjjYOt170s0tGuvXAgyEb1C",
    val mercuryoWidgetId: String = "",
    val mercuryoSecret: String = "",
    val appsFlyerDevKey: String = "",
    val blockchainSdkConfig: BlockchainSdkConfig = BlockchainSdkConfig(),
    val isSendingToPayIdEnabled: Boolean = true,
    val isTopUpEnabled: Boolean = false,
    @Deprecated("Not relevant since version 3.23")
    val isCreatingTwinCardsAllowed: Boolean = false,
    val shopify: ShopifyShop? = null,
    val zendesk: ZendeskConfig? = null,
)

class ConfigManager(
    private val localLoader: Loader<ConfigModel>,
    private val remoteLoader: Loader<ConfigModel>
) {

    var config: Config = Config()
        private set

    private var defaultConfig = Config()

    fun load(onComplete: ((config: Config) -> Unit)? = null) {
        localLoader.load { configModel ->
            setupFeature(configModel.features)
            setupKey(configModel.configValues)
            onComplete?.invoke(config)
        }
        // Uncomment to enable remote config
//        remoteLoader.load { config ->
//            setupFeature(config.features)
//        }
    }

    fun turnOff(name: String) {
        when (name) {
            isSendingToPayIdEnabled -> config = config.copy(isSendingToPayIdEnabled = false)
            isTopUpEnabled -> config = config.copy(isTopUpEnabled = false)
            isCreatingTwinCardsAllowed -> config = config.copy(isCreatingTwinCardsAllowed = false)
        }
    }

    fun resetToDefault(name: String) {
        when (name) {
            isSendingToPayIdEnabled -> config =
                config.copy(isSendingToPayIdEnabled = defaultConfig.isSendingToPayIdEnabled)
            isTopUpEnabled -> config = config.copy(isTopUpEnabled = defaultConfig.isTopUpEnabled)
            isCreatingTwinCardsAllowed -> config =
                config.copy(isCreatingTwinCardsAllowed = defaultConfig.isCreatingTwinCardsAllowed)
        }
    }

    private fun setupFeature(featureModel: FeatureModel?) {
        val model = featureModel ?: return

        config = config.copy(
            isTopUpEnabled = model.isTopUpEnabled,
            isSendingToPayIdEnabled = model.isSendingToPayIdEnabled,
            isCreatingTwinCardsAllowed = model.isCreatingTwinCardsAllowed
        )
        defaultConfig = defaultConfig.copy(
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
            mercuryoWidgetId = values.mercuryoWidgetId,
            mercuryoSecret = values.mercuryoSecret,
            blockchainSdkConfig = BlockchainSdkConfig(
                blockchairApiKey = values.blockchairApiKey,
                blockchairAuthorizationToken = values.blockchairAuthorizationToken,
                blockcypherTokens = values.blockcypherTokens,
                infuraProjectId = values.infuraProjectId
            ),
            appsFlyerDevKey = values.appsFlyerDevKey,
            shopify = values.shopifyShop,
            zendesk = values.zendesk,
        )
        defaultConfig = defaultConfig.copy(
            coinMarketCapKey = values.coinMarketCapKey,
            moonPayApiKey = values.moonPayApiKey,
            moonPayApiSecretKey = values.moonPayApiSecretKey,
            mercuryoWidgetId = values.mercuryoWidgetId,
            mercuryoSecret = values.mercuryoSecret,
            blockchainSdkConfig = BlockchainSdkConfig(
                blockchairApiKey = values.blockchairApiKey,
                blockchairAuthorizationToken = values.blockchairAuthorizationToken,
                blockcypherTokens = values.blockcypherTokens,
                infuraProjectId = values.infuraProjectId
            ),
            appsFlyerDevKey = values.appsFlyerDevKey,
            shopify = values.shopifyShop,
            zendesk = values.zendesk,
        )
    }

    companion object {
        const val isSendingToPayIdEnabled = "isSendingToPayIdEnabled"
        const val isCreatingTwinCardsAllowed = "isCreatingTwinCardsAllowed"
        const val isTopUpEnabled = "isTopUpEnabled"
    }
}
