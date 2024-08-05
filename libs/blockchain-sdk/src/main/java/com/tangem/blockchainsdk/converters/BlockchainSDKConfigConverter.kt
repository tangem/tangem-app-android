package com.tangem.blockchainsdk.converters

import com.tangem.blockchain.common.*
import com.tangem.datasource.config.models.ConfigValueModel
import com.tangem.utils.converter.Converter

/**
 * Converts [ConfigValueModel] to [BlockchainSdkConfig]
 *
[REDACTED_AUTHOR]
 */
internal object BlockchainSDKConfigConverter : Converter<ConfigValueModel, BlockchainSdkConfig> {

    override fun convert(value: ConfigValueModel): BlockchainSdkConfig {
        return BlockchainSdkConfig(
            blockchairCredentials = BlockchairCredentials(
                apiKey = value.blockchairApiKeys,
                authToken = value.blockchairAuthorizationToken,
            ),
            blockcypherTokens = value.blockcypherTokens,
            quickNodeSolanaCredentials = QuickNodeCredentials(
                apiKey = value.quiknodeApiKey,
                subdomain = value.quiknodeSubdomain,
            ),
            quickNodeBscCredentials = QuickNodeCredentials(
                apiKey = value.bscQuiknodeApiKey,
                subdomain = value.bscQuiknodeSubdomain,
            ),
            infuraProjectId = value.infuraProjectId,
            tronGridApiKey = value.tronGridApiKey,
            nowNodeCredentials = NowNodeCredentials(value.nowNodesApiKey),
            getBlockCredentials = createGetBlockCredentials(value),
            kaspaSecondaryApiUrl = value.kaspaSecondaryApiUrl,
            tonCenterCredentials = TonCenterCredentials(
                mainnetApiKey = value.tonCenterKeys.mainnet,
                testnetApiKey = value.tonCenterKeys.testnet,
            ),
            chiaFireAcademyApiKey = value.chiaFireAcademyApiKey,
            chiaTangemApiKey = value.chiaTangemApiKey,
            hederaArkhiaApiKey = value.hederaArkhiaKey,
            polygonScanApiKey = value.polygonScanApiKey,
            bittensorDwellirApiKey = value.bittensorDwellirApiKey,
            bittensorOnfinalityApiKey = value.bittensorOnfinalityKey,
            koinosProApiKey = value.koinosProApiKey,
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
                zkSyncEra = GetBlockAccessToken(jsonRpc = accessTokens.zksync?.jsonRPC),
                polygonZkEvm = GetBlockAccessToken(jsonRpc = accessTokens.polygonZkevm?.jsonRPC),
                base = GetBlockAccessToken(jsonRpc = accessTokens.base?.jsonRPC),
                blast = GetBlockAccessToken(jsonRpc = accessTokens.blast?.jsonRPC),
                filecoin = GetBlockAccessToken(jsonRpc = accessTokens.filecoin?.jsonRPC),
            )
        }
    }
}