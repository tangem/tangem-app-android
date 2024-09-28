package com.tangem.datasource.local.config.environment.converter

import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.config.environment.models.EnvironmentConfigModel
import com.tangem.utils.converter.Converter

/**
 * Converter from [EnvironmentConfigModel] to [EnvironmentConfig]
 *
[REDACTED_AUTHOR]
 */
internal object EnvironmentConfigConverter : Converter<EnvironmentConfigModel, EnvironmentConfig> {

    override fun convert(value: EnvironmentConfigModel): EnvironmentConfig {
        return EnvironmentConfig(
            moonPayApiKey = value.moonPayApiKey,
            moonPayApiSecretKey = value.moonPayApiSecretKey,
            mercuryoWidgetId = value.mercuryoWidgetId,
            mercuryoSecret = value.mercuryoSecret,
            blockchainSdkConfig = BlockchainSDKConfigConverter.convert(value = value),
            amplitudeApiKey = value.amplitudeApiKey,
            walletConnectProjectId = value.walletConnectProjectId,
            express = value.express,
            devExpress = value.devExpress,
            stakeKitApiKey = value.stakeKitApiKey,
        )
    }
}