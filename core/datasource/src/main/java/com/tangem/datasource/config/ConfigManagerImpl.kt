package com.tangem.datasource.config

import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.BlockchairCredentials
import com.tangem.blockchain.common.BlockscoutCredentials
import com.tangem.blockchain.common.GetBlockCredentials
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.common.QuickNodeCredentials
import com.tangem.blockchain.common.TonCenterCredentials
import com.tangem.datasource.config.ConfigManager.Companion.IS_CREATING_TWIN_CARDS_ALLOWED
import com.tangem.datasource.config.ConfigManager.Companion.IS_SENDING_TO_PAY_ID_ENABLED
import com.tangem.datasource.config.ConfigManager.Companion.IS_TOP_UP_ENABLED
import com.tangem.datasource.config.models.Config
import com.tangem.datasource.config.models.ConfigModel
import com.tangem.datasource.config.models.ConfigValueModel
import com.tangem.datasource.config.models.FeatureModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ConfigManagerImpl @Inject constructor() : ConfigManager {

    override var config: Config = Config()
        private set

    private var defaultConfig = Config()

    override fun load(configLoader: Loader<ConfigModel>, onComplete: ((config: Config) -> Unit)?) {
        configLoader.load { configModel ->
            setupFeature(configModel.features)
            setupConfigValues(configModel.configValues)
            onComplete?.invoke(config)
        }
    }

    override fun turnOff(name: String) {
        when (name) {
            IS_SENDING_TO_PAY_ID_ENABLED -> config = config.copy(isSendingToPayIdEnabled = false)
            IS_TOP_UP_ENABLED -> config = config.copy(isTopUpEnabled = false)
            IS_CREATING_TWIN_CARDS_ALLOWED -> config = config.copy(isCreatingTwinCardsAllowed = false)
        }
    }

    override fun resetToDefault(name: String) {
        when (name) {
            IS_SENDING_TO_PAY_ID_ENABLED ->
                config =
                    config.copy(isSendingToPayIdEnabled = defaultConfig.isSendingToPayIdEnabled)
            IS_TOP_UP_ENABLED -> config = config.copy(isTopUpEnabled = defaultConfig.isTopUpEnabled)
            IS_CREATING_TWIN_CARDS_ALLOWED ->
                config =
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

    private fun setupConfigValues(configValues: ConfigValueModel?) {
        val values = configValues ?: return

        config = createConfig(config, values)
        defaultConfig = config.copy()
    }

    private fun createConfig(config: Config, configValues: ConfigValueModel): Config {
        return config.copy(
            coinMarketCapKey = configValues.coinMarketCapKey,
            moonPayApiKey = configValues.moonPayApiKey,
            moonPayApiSecretKey = configValues.moonPayApiSecretKey,
            mercuryoWidgetId = configValues.mercuryoWidgetId,
            mercuryoSecret = configValues.mercuryoSecret,
            blockchainSdkConfig = BlockchainSdkConfig(
                blockchairCredentials = BlockchairCredentials(
                    apiKey = configValues.blockchairApiKeys,
                    authToken = configValues.blockchairAuthorizationToken,
                ),
                blockcypherTokens = configValues.blockcypherTokens,
                blockscoutCredentials = BlockscoutCredentials(
                    userName = configValues.saltPay.blockscoutCredentials.user,
                    password = configValues.saltPay.blockscoutCredentials.password,
                ),
                quickNodeSolanaCredentials = QuickNodeCredentials(
                    apiKey = configValues.quiknodeApiKey,
                    subdomain = configValues.quiknodeSubdomain,
                ),
                quickNodeBscCredentials = QuickNodeCredentials(
                    apiKey = configValues.bscQuiknodeApiKey,
                    subdomain = configValues.bscQuiknodeSubdomain,
                ),
                infuraProjectId = configValues.infuraProjectId,
                tronGridApiKey = configValues.tronGridApiKey,
                saltPayAuthToken = configValues.saltPay.credentials.basicAuthToken,
                nowNodeCredentials = NowNodeCredentials(configValues.nowNodesApiKey),
                getBlockCredentials = GetBlockCredentials(configValues.getBlockApiKey),
                kaspaSecondaryApiUrl = configValues.kaspaSecondaryApiUrl,
                tonCenterCredentials = TonCenterCredentials(
                    mainnetApiKey = configValues.tonCenterKeys.mainnet,
                    testnetApiKey = configValues.tonCenterKeys.testnet,
                ),
            ),
            appsFlyerDevKey = configValues.appsFlyer.appsFlyerDevKey,
            amplitudeApiKey = configValues.amplitudeApiKey,
            shopify = configValues.shopifyShop,
            zendesk = configValues.zendesk,
            saltPayConfig = configValues.saltPay,
            swapReferrerAccount = configValues.swapReferrerAccount,
        )
    }
}
