package com.tangem.tap.network.auth

import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.lib.auth.StakeKitAuthProvider

internal class DefaultStakeKitAuthProvider(
    private val environmentConfigStorage: EnvironmentConfigStorage,
) : StakeKitAuthProvider {

    override fun getApiKey(): String {
        return environmentConfigStorage.getConfigSync().stakeKitApiKey ?: error("No StakeKit api key provided")
    }
}