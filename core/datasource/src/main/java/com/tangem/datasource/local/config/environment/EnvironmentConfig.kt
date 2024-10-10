package com.tangem.datasource.local.config.environment

import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.datasource.local.config.environment.models.ExpressModel

data class EnvironmentConfig(
    val moonPayApiKey: String = "pk_test_kc90oYTANy7UQdBavDKGfL4K9l6VEPE",
    val moonPayApiSecretKey: String = "sk_test_V8w4M19LbDjjYOt170s0tGuvXAgyEb1C",
    val mercuryoWidgetId: String = "",
    val mercuryoSecret: String = "",
    val amplitudeApiKey: String = "",
    val blockchainSdkConfig: BlockchainSdkConfig = BlockchainSdkConfig(),
    @Deprecated("Not relevant since version 3.23")
    val walletConnectProjectId: String = "",
    val express: ExpressModel? = null,
    val devExpress: ExpressModel? = null,
    val stakeKitApiKey: String? = null,
)
