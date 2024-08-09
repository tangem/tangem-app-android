package com.tangem.datasource.config

import com.tangem.blockchain.common.*
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.config.ConfigManager.Companion.IS_CREATING_TWIN_CARDS_ALLOWED
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

    override suspend fun load(configLoader: Loader<ConfigModel>, onComplete: ((config: Config) -> Unit)?) {
        configLoader.load { configModel ->
            setupFeature(configModel.features)
            setupConfigValues(configModel.configValues)
            onComplete?.invoke(config)
        }
    }

    override fun turnOff(name: String) {
        when (name) {
            IS_TOP_UP_ENABLED -> config = config.copy(isTopUpEnabled = false)
            IS_CREATING_TWIN_CARDS_ALLOWED -> config = config.copy(isCreatingTwinCardsAllowed = false)
        }
    }

    override fun resetToDefault(name: String) {
        when (name) {
            IS_TOP_UP_ENABLED -> {
                config = config.copy(isTopUpEnabled = defaultConfig.isTopUpEnabled)
            }
            IS_CREATING_TWIN_CARDS_ALLOWED -> {
                config = config.copy(isCreatingTwinCardsAllowed = defaultConfig.isCreatingTwinCardsAllowed)
            }
            else -> Unit
        }
    }

    private fun setupFeature(featureModel: FeatureModel?) {
        val model = featureModel ?: return

        config = config.copy(
            isTopUpEnabled = model.isTopUpEnabled,
            isCreatingTwinCardsAllowed = model.isCreatingTwinCardsAllowed,
        )

        defaultConfig = defaultConfig.copy(
            isTopUpEnabled = model.isTopUpEnabled,
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
                nowNodeCredentials = NowNodeCredentials(configValues.nowNodesApiKey),
                getBlockCredentials = createGetBlockCredentials(configValues),
                kaspaSecondaryApiUrl = configValues.kaspaSecondaryApiUrl,
                tonCenterCredentials = TonCenterCredentials(
                    mainnetApiKey = configValues.tonCenterKeys.mainnet,
                    testnetApiKey = configValues.tonCenterKeys.testnet,
                ),
                chiaFireAcademyApiKey = configValues.chiaFireAcademyApiKey,
                chiaTangemApiKey = configValues.chiaTangemApiKey,
                hederaArkhiaApiKey = configValues.hederaArkhiaKey,
                polygonScanApiKey = configValues.polygonScanApiKey,
                bittensorDwellirApiKey = configValues.bittensorDwellirApiKey,
                bittensorOnfinalityApiKey = configValues.bittensorOnfinalityKey,
                koinosProApiKey = configValues.koinosProApiKey,
            ),
            amplitudeApiKey = configValues.amplitudeApiKey,
            sprinklr = configValues.sprinklr,
            walletConnectProjectId = configValues.walletConnectProjectId,
            tangemComAuthorization = configValues.tangemComAuthorization,
            express = if (BuildConfig.ENVIRONMENT == "dev") configValues.devExpress else configValues.express,
            stakeKitApiKey = configValues.stakeKitApiKey,
        )
    }

    private fun createGetBlockCredentials(configValues: ConfigValueModel): GetBlockCredentials? {
        return configValues.getBlockAccessTokens?.let { accessTokens ->
            GetBlockCredentials(
                xrp = GetBlockAccessToken(jsonRpc = accessTokens.xrp?.jsonRPC),
                cardano = GetBlockAccessToken(rosetta = accessTokens.cardano?.rosetta),
                avalanche = GetBlockAccessToken(jsonRpc = accessTokens.avalanche?.jsonRPC),
                eth = GetBlockAccessToken(jsonRpc = accessTokens.eth?.jsonRPC),
                etc = GetBlockAccessToken(jsonRpc = accessTokens.etc?.jsonRPC),
                fantom = GetBlockAccessToken(jsonRpc = accessTokens.fantom?.jsonRPC),
                rsk = GetBlockAccessToken(jsonRpc = accessTokens.rsk?.jsonRPC),
                bsc = GetBlockAccessToken(jsonRpc = accessTokens.bsc?.jsonRPC),
                polygon = GetBlockAccessToken(jsonRpc = accessTokens.polygon?.jsonRPC),
                gnosis = GetBlockAccessToken(jsonRpc = accessTokens.gnosis?.jsonRPC),
                cronos = GetBlockAccessToken(jsonRpc = accessTokens.cronos?.jsonRPC),
                solana = GetBlockAccessToken(jsonRpc = accessTokens.solana?.jsonRPC),
                ton = GetBlockAccessToken(jsonRpc = accessTokens.ton?.jsonRPC),
                tron = GetBlockAccessToken(rest = accessTokens.tron?.rest),
                cosmos = GetBlockAccessToken(rest = accessTokens.cosmos?.rest),
                near = GetBlockAccessToken(jsonRpc = accessTokens.near?.jsonRPC),
                aptos = GetBlockAccessToken(rest = accessTokens.aptos?.rest),
                dogecoin = GetBlockAccessToken(
                    jsonRpc = accessTokens.dogecoin?.jsonRPC,
                    blockBookRest = accessTokens.dogecoin?.blockBookRest,
                ),
                litecoin = GetBlockAccessToken(
                    jsonRpc = accessTokens.litecoin?.jsonRPC,
                    blockBookRest = accessTokens.litecoin?.blockBookRest,
                ),
                dash = GetBlockAccessToken(
                    jsonRpc = accessTokens.dash?.jsonRPC,
                    blockBookRest = accessTokens.dash?.blockBookRest,
                ),
                bitcoin = GetBlockAccessToken(
                    jsonRpc = accessTokens.bitcoin?.jsonRPC,
                    blockBookRest = accessTokens.bitcoin?.blockBookRest,
                ),
                algorand = GetBlockAccessToken(rest = accessTokens.algorand?.rest),
                zkSyncEra = GetBlockAccessToken(rest = accessTokens.zksync?.jsonRPC),
                polygonZkEvm = GetBlockAccessToken(rest = accessTokens.polygonZkevm?.jsonRPC),
                base = GetBlockAccessToken(rest = accessTokens.base?.jsonRPC),
                blast = GetBlockAccessToken(jsonRpc = accessTokens.blast?.jsonRPC),
                filecoin = GetBlockAccessToken(jsonRpc = accessTokens.filecoin?.jsonRPC),
            )
        }
    }
}