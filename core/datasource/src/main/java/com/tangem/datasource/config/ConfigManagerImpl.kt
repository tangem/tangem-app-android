package com.tangem.datasource.config

import com.tangem.blockchain.common.*
import com.tangem.datasource.config.models.Config
import com.tangem.datasource.config.models.ConfigModel
import com.tangem.datasource.config.models.ConfigValueModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ConfigManagerImpl @Inject constructor() : ConfigManager {

    override var config: Config = Config()
        private set

    override suspend fun load(configLoader: Loader<ConfigModel>, onComplete: ((config: Config) -> Unit)?) {
        configLoader.load { configModel ->
            setupConfigValues(configModel.configValues)
            onComplete?.invoke(config)
        }
    }

    private fun setupConfigValues(configValues: ConfigValueModel?) {
        val values = configValues ?: return

        config = createConfig(config, values)
    }

    private fun createConfig(config: Config, configValues: ConfigValueModel): Config {
        return config.copy(
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
            walletConnectProjectId = configValues.walletConnectProjectId,
            express = configValues.express,
            devExpress = configValues.devExpress,
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