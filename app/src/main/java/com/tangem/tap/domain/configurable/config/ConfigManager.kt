package com.tangem.tap.domain.configurable.config

import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.BlockchairCredentials
import com.tangem.blockchain.common.QuickNodeCredentials
import com.tangem.tap.common.shop.shopify.ShopifyShop
import com.tangem.tap.common.zendesk.ZendeskConfig
import com.tangem.tap.domain.configurable.Loader
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayConfig

/**
 * Created by Anton Zhilenkov on 12/11/2020.
 */
data class Config(
    val coinMarketCapKey: String = "f6622117-c043-47a0-8975-9d673ce484de",
    val moonPayApiKey: String = "pk_test_kc90oYTANy7UQdBavDKGfL4K9l6VEPE",
    val moonPayApiSecretKey: String = "sk_test_V8w4M19LbDjjYOt170s0tGuvXAgyEb1C",
    val mercuryoWidgetId: String = "",
    val mercuryoSecret: String = "",
    val appsFlyerDevKey: String = "",
    val amplitudeApiKey: String = "",
    val blockchainSdkConfig: BlockchainSdkConfig = BlockchainSdkConfig(),
    val isSendingToPayIdEnabled: Boolean = true,
    val isTopUpEnabled: Boolean = false,
    @Deprecated("Not relevant since version 3.23")
    val isCreatingTwinCardsAllowed: Boolean = false,
    val shopify: ShopifyShop? = null,
    val zendesk: ZendeskConfig? = null,
    val saltPayConfig: SaltPayConfig? = null,
)

class ConfigManager {

    var config: Config = Config()
        private set

    private var defaultConfig = Config()

    fun load(configLoader: Loader<ConfigModel>, onComplete: ((config: Config) -> Unit)? = null) {
        configLoader.load { configModel ->
            setupFeature(configModel.features)
            setupKey(configModel.configValues)
            onComplete?.invoke(config)
        }
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
            isCreatingTwinCardsAllowed = model.isCreatingTwinCardsAllowed,
        )
        defaultConfig = defaultConfig.copy(
            isTopUpEnabled = model.isTopUpEnabled,
            isSendingToPayIdEnabled = model.isSendingToPayIdEnabled,
            isCreatingTwinCardsAllowed = model.isCreatingTwinCardsAllowed,
        )
    }

    @Suppress("LongMethod")
    private fun setupKey(configValues: ConfigValueModel?) {
        val values = configValues ?: return
        config = config.copy(
            coinMarketCapKey = values.coinMarketCapKey,
            moonPayApiKey = values.moonPayApiKey,
            moonPayApiSecretKey = values.moonPayApiSecretKey,
            mercuryoWidgetId = values.mercuryoWidgetId,
            mercuryoSecret = values.mercuryoSecret,
            blockchainSdkConfig = BlockchainSdkConfig(
                blockchairCredentials = BlockchairCredentials(
                    apiKey = values.blockchairApiKeys,
                    authToken = values.blockchairAuthorizationToken,
                ),
                blockcypherTokens = values.blockcypherTokens,
                quickNodeCredentials = QuickNodeCredentials(
                    apiKey = values.quiknodeApiKey,
                    subdomain = values.quiknodeSubdomain,
                ),
                bscQuickNodeCredentials = QuickNodeCredentials(
                    apiKey = values.bscQuiknodeApiKey,
                    subdomain = values.bscQuiknodeSubdomain,
                ),
                infuraProjectId = values.infuraProjectId,
                tronGridApiKey = values.tronGridApiKey,
                saltPayAuthToken = values.saltPay.credentials.token,
            ),
            appsFlyerDevKey = values.appsFlyer.appsFlyerDevKey,
            amplitudeApiKey = values.amplitudeApiKey,
            shopify = values.shopifyShop,
            zendesk = values.zendesk,
            saltPayConfig = values.saltPay,
        )
        defaultConfig = defaultConfig.copy(
            coinMarketCapKey = values.coinMarketCapKey,
            moonPayApiKey = values.moonPayApiKey,
            moonPayApiSecretKey = values.moonPayApiSecretKey,
            mercuryoWidgetId = values.mercuryoWidgetId,
            mercuryoSecret = values.mercuryoSecret,
            blockchainSdkConfig = BlockchainSdkConfig(
                blockchairCredentials = BlockchairCredentials(
                    apiKey = values.blockchairApiKeys,
                    authToken = values.blockchairAuthorizationToken,
                ),
                blockcypherTokens = values.blockcypherTokens,
                quickNodeCredentials = QuickNodeCredentials(
                    apiKey = values.quiknodeApiKey,
                    subdomain = values.quiknodeSubdomain,
                ),
                bscQuickNodeCredentials = QuickNodeCredentials(
                    apiKey = values.bscQuiknodeApiKey,
                    subdomain = values.bscQuiknodeSubdomain,
                ),
                infuraProjectId = values.infuraProjectId,
                saltPayAuthToken = values.saltPay.credentials.token,
            ),
            appsFlyerDevKey = values.appsFlyer.appsFlyerDevKey,
            amplitudeApiKey = values.amplitudeApiKey,
            shopify = values.shopifyShop,
            zendesk = values.zendesk,
            saltPayConfig = values.saltPay,
        )
    }

    companion object {
        const val isSendingToPayIdEnabled = "isSendingToPayIdEnabled"
        const val isCreatingTwinCardsAllowed = "isCreatingTwinCardsAllowed"
        const val isTopUpEnabled = "isTopUpEnabled"
    }
}
