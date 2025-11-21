package com.tangem.datasource.local.config.environment

import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.datasource.local.config.environment.models.ExpressModel
import com.tangem.datasource.local.config.environment.models.P2PKeys

data class EnvironmentConfig(
    val moonPayApiKey: String = "",
    val moonPayApiSecretKey: String = "",
    val mercuryoWidgetId: String = "",
    val mercuryoSecret: String = "",
    val amplitudeApiKey: String = "",
    val appsFlyerApiKey: String = "",
    val appsAppId: String = "",
    val blockchainSdkConfig: BlockchainSdkConfig = BlockchainSdkConfig(),
    val walletConnectProjectId: String = "",
    val express: ExpressModel? = null,
    val devExpress: ExpressModel? = null,
    val stakeKitApiKey: String? = null,
    val p2pApiKey: P2PKeys? = null,
    val blockAidApiKey: String? = null,
    val tangemApiKey: String? = null,
    val tangemApiKeyDev: String? = null,
    val tangemApiKeyStage: String? = null,
    val yieldModuleApiKey: String? = null,
    val yieldModuleApiKeyDev: String? = null,
)