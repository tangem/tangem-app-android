package com.tangem.datasource.local.config.environment.converter

import com.tangem.blockchain.common.*
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.config.environment.generated.GeneratedEnvironmentConfig
import com.tangem.datasource.local.config.environment.generated.GeneratedEnvironmentConfig.AppsFlyer
import com.tangem.datasource.local.config.environment.generated.GeneratedEnvironmentConfig.DevExpress
import com.tangem.datasource.local.config.environment.generated.GeneratedEnvironmentConfig.Express
import com.tangem.datasource.local.config.environment.generated.GeneratedEnvironmentConfig.GetBlockAccessTokens
import com.tangem.datasource.local.config.environment.generated.GeneratedEnvironmentConfig.P2pApiKey
import com.tangem.datasource.local.config.environment.generated.GeneratedEnvironmentConfig.TonCenterApiKey
import com.tangem.datasource.local.config.environment.models.ExpressModel
import com.tangem.datasource.local.config.environment.models.P2PKeys

/**
 * Converts [GeneratedEnvironmentConfig] to [EnvironmentConfig]
 *
 * This converter maps the auto-generated config (from JSON) to the domain model.
 * The generated config has nested objects that mirror the JSON structure.
 */
internal object GeneratedEnvironmentConfigConverter {

    fun convert(): EnvironmentConfig {
        return EnvironmentConfig(
            moonPayApiKey = GeneratedEnvironmentConfig.moonPayApiKey,
            moonPayApiSecretKey = GeneratedEnvironmentConfig.moonPayApiSecretKey,
            mercuryoWidgetId = GeneratedEnvironmentConfig.mercuryoWidgetId,
            mercuryoSecret = GeneratedEnvironmentConfig.mercuryoSecret,
            blockchainSdkConfig = createBlockchainSdkConfig(),
            amplitudeApiKey = GeneratedEnvironmentConfig.amplitudeApiKey,
            appsFlyerApiKey = AppsFlyer.appsFlyerDevKey,
            appsAppId = AppsFlyer.appsFlyerAppID,
            walletConnectProjectId = GeneratedEnvironmentConfig.walletConnectProjectId,
            express = createExpressModel(
                apiKey = Express.apiKey,
                signVerifierPublicKey = Express.signVerifierPublicKey,
            ),
            devExpress = createExpressModel(
                apiKey = DevExpress.apiKey,
                signVerifierPublicKey = DevExpress.signVerifierPublicKey,
            ),
            stakeKitApiKey = GeneratedEnvironmentConfig.stakeKitApiKey,
            p2pApiKey = createP2PKeys(),
            blockAidApiKey = GeneratedEnvironmentConfig.blockaidApiKey,
            tangemApiKey = GeneratedEnvironmentConfig.tangemApiKey,
            tangemApiKeyDev = GeneratedEnvironmentConfig.tangemApiKeyDev,
            tangemApiKeyStage = GeneratedEnvironmentConfig.tangemApiKeyStage,
            yieldModuleApiKey = GeneratedEnvironmentConfig.yieldModuleApiKey,
            yieldModuleApiKeyDev = GeneratedEnvironmentConfig.yieldModuleApiKeyDev,
            bffStaticToken = GeneratedEnvironmentConfig.bffStaticToken,
            bffStaticTokenDev = GeneratedEnvironmentConfig.bffStaticTokenDev,
            gaslessTxApiKeyDev = GeneratedEnvironmentConfig.gaslessTxApiKeyDev,
            gaslessTxApiKey = GeneratedEnvironmentConfig.gaslessTxApiKey,
            customerIoCdpApiKey = GeneratedEnvironmentConfig.CustomerIO.androidApiKey,
            surveySparrowToken = GeneratedEnvironmentConfig.SurveySparrow.apiKey,
        )
    }

    private fun createExpressModel(apiKey: String?, signVerifierPublicKey: String?): ExpressModel? {
        return if (!apiKey.isNullOrEmpty() && !signVerifierPublicKey.isNullOrEmpty()) {
            ExpressModel(apiKey = apiKey, signVerifierPublicKey = signVerifierPublicKey)
        } else {
            null
        }
    }

    private fun createP2PKeys(): P2PKeys? {
        val mainnet = P2pApiKey.mainnet
        val hoodi = P2pApiKey.hoodi
        return if (mainnet.isNotEmpty() && hoodi.isNotEmpty()) {
            P2PKeys(mainnet = mainnet, hoodi = hoodi)
        } else {
            null
        }
    }

    private fun createBlockchainSdkConfig(): BlockchainSdkConfig {
        return BlockchainSdkConfig(
            blockchairCredentials = BlockchairCredentials(
                apiKey = GeneratedEnvironmentConfig.blockchairApiKeys,
                authToken = GeneratedEnvironmentConfig.blockchairAuthorizationToken,
            ),
            blockcypherTokens = GeneratedEnvironmentConfig.blockcypherTokens.toSet(),
            quickNodeSolanaCredentials = QuickNodeCredentials(
                apiKey = GeneratedEnvironmentConfig.quiknodeApiKey,
                subdomain = GeneratedEnvironmentConfig.quiknodeSubdomain,
            ),
            quickNodeBscCredentials = QuickNodeCredentials(
                apiKey = GeneratedEnvironmentConfig.bscQuiknodeApiKey,
                subdomain = GeneratedEnvironmentConfig.bscQuiknodeSubdomain,
            ),
            quickNodePlasmaCredentials = QuickNodeCredentials(
                apiKey = GeneratedEnvironmentConfig.quiknodePlasmaApiKey,
                subdomain = GeneratedEnvironmentConfig.quiknodePlasmaSubdomain,
            ),
            quickNodeMonadCredentials = QuickNodeCredentials(
                apiKey = GeneratedEnvironmentConfig.quiknodeMonadApiKey,
                subdomain = GeneratedEnvironmentConfig.quiknodeMonadSubdomain,
            ),
            infuraProjectId = GeneratedEnvironmentConfig.infuraProjectId,
            tronGridApiKey = GeneratedEnvironmentConfig.tronGridApiKey,
            nowNodeCredentials = NowNodeCredentials(apiKey = GeneratedEnvironmentConfig.nowNodesApiKey),
            getBlockCredentials = createGetBlockCredentials(),
            kaspaSecondaryApiUrl = GeneratedEnvironmentConfig.kaspaSecondaryApiUrl,
            tonCenterCredentials = TonCenterCredentials(
                mainnetApiKey = TonCenterApiKey.mainnet,
                testnetApiKey = TonCenterApiKey.testnet,
            ),
            chiaFireAcademyApiKey = GeneratedEnvironmentConfig.chiaFireAcademyApiKey,
            chiaTangemApiKey = GeneratedEnvironmentConfig.chiaTangemApiKey,
            hederaArkhiaApiKey = GeneratedEnvironmentConfig.hederaArkhiaKey,
            polygonScanApiKey = GeneratedEnvironmentConfig.polygonScanApiKey,
            bittensorDwellirApiKey = GeneratedEnvironmentConfig.bittensorDwellirKey,
            bittensorOnfinalityApiKey = GeneratedEnvironmentConfig.bittensorOnfinalityKey,
            dwellirApiKey = GeneratedEnvironmentConfig.dwellirApiKey,
            koinosProApiKey = GeneratedEnvironmentConfig.koinosProApiKey,
            alephiumApiKey = GeneratedEnvironmentConfig.alephiumTangemApiKey,
            moralisApiKey = GeneratedEnvironmentConfig.moralisApiKey,
            etherscanApiKey = GeneratedEnvironmentConfig.etherscanApiKey,
            blinkApiKey = GeneratedEnvironmentConfig.blinkApiKey,
            tatumApiKey = GeneratedEnvironmentConfig.tatumApiKey,
        )
    }

    private fun createGetBlockCredentials(): GetBlockCredentials {
        return GetBlockCredentials(
            xrp = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Xrp.jsonRpc),
            cardano = GetBlockAccessToken(rosetta = GetBlockAccessTokens.Cardano.rosetta),
            avalanche = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Avalanche.jsonRpc),
            eth = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Ethereum.jsonRpc),
            etc = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.EthereumClassic.jsonRpc),
            fantom = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Fantom.jsonRpc),
            rsk = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Rsk.jsonRpc),
            bsc = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Bsc.jsonRpc),
            polygon = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Polygon.jsonRpc),
            gnosis = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Xdai.jsonRpc),
            cronos = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Cronos.jsonRpc),
            solana = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Solana.jsonRpc),
            ton = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Ton.jsonRpc),
            tron = GetBlockAccessToken(rest = GetBlockAccessTokens.Tron.rest),
            cosmos = GetBlockAccessToken(rest = GetBlockAccessTokens.CosmosHub.rest),
            near = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Near.jsonRpc),
            aptos = GetBlockAccessToken(rest = GetBlockAccessTokens.Aptos.rest),
            dogecoin = GetBlockAccessToken(
                jsonRpc = GetBlockAccessTokens.Dogecoin.jsonRpc,
                blockBookRest = GetBlockAccessTokens.Dogecoin.blockBookRest,
            ),
            litecoin = GetBlockAccessToken(
                jsonRpc = GetBlockAccessTokens.Litecoin.jsonRpc,
                blockBookRest = GetBlockAccessTokens.Litecoin.blockBookRest,
            ),
            dash = GetBlockAccessToken(
                jsonRpc = GetBlockAccessTokens.Dash.jsonRpc,
                blockBookRest = GetBlockAccessTokens.Dash.blockBookRest,
            ),
            bitcoin = GetBlockAccessToken(
                jsonRpc = GetBlockAccessTokens.Bitcoin.jsonRpc,
                blockBookRest = GetBlockAccessTokens.Bitcoin.blockBookRest,
            ),
            algorand = GetBlockAccessToken(rest = GetBlockAccessTokens.Algorand.rest),
            zkSyncEra = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Zksync.jsonRpc),
            polygonZkEvm = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.PolygonZkevm.jsonRpc),
            base = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Base.jsonRpc),
            blast = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Blast.jsonRpc),
            filecoin = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Filecoin.jsonRpc),
            arbitrum = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.ArbitrumOne.jsonRpc),
            bitcoinCash = GetBlockAccessToken(
                jsonRpc = GetBlockAccessTokens.BitcoinCash.jsonRpc,
                blockBookRest = GetBlockAccessTokens.BitcoinCash.blockBookRest,
            ),
            kusama = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Kusama.jsonRpc),
            moonbeam = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Moonbeam.jsonRpc),
            optimism = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Optimism.jsonRpc),
            polkadot = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Polkadot.jsonRpc),
            shibarium = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Shibarium.jsonRpc),
            sui = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Sui.jsonRpc),
            telos = GetBlockAccessToken(jsonRpc = GetBlockAccessTokens.Telos.jsonRpc),
            tezos = GetBlockAccessToken(rest = GetBlockAccessTokens.Tezos.rest),
            monad = GetBlockAccessToken(rest = GetBlockAccessTokens.Monad.rest),
            stellar = GetBlockAccessToken(rest = GetBlockAccessTokens.Stellar.rest),
        )
    }
}