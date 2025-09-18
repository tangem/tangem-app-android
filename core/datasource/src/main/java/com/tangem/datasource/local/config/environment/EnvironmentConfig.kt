package com.tangem.datasource.local.config.environment

import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.datasource.local.config.environment.models.ExpressModel

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
    val blockAidApiKey: String? = null,
)