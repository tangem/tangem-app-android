package com.tangem.tap.network.auth

import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.lib.auth.StakeKitAuthProvider

internal class DefaultStakeKitAuthProvider(
    private val environmentConfig: EnvironmentConfig,
) : StakeKitAuthProvider {

    override fun getApiKey(): String {
        return environmentConfig.stakeKitApiKey ?: error("No StakeKit api key provided")
    }
}