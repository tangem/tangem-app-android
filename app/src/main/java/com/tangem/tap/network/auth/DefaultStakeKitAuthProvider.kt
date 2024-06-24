package com.tangem.tap.network.auth

import com.tangem.datasource.config.ConfigManager
import com.tangem.lib.auth.StakeKitAuthProvider

internal class DefaultStakeKitAuthProvider(
    private val configManager: ConfigManager,
) : StakeKitAuthProvider {

    override fun getApiKey(): String {
        return configManager.config.stakeKitApiKey ?: error("No StakeKit api key provided")
    }
}