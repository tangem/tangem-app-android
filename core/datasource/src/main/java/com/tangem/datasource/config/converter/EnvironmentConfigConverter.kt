package com.tangem.datasource.config.converter

import com.tangem.datasource.config.models.Config
import com.tangem.datasource.config.models.ConfigValueModel
import com.tangem.utils.converter.Converter

/**
 * Converter from [ConfigValueModel] to [Config]
 *
[REDACTED_AUTHOR]
 */
internal object EnvironmentConfigConverter : Converter<ConfigValueModel, Config> {

    override fun convert(value: ConfigValueModel): Config {
        return Config(
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