package com.tangem.tap.network.auth

import com.tangem.datasource.config.ConfigManager
import com.tangem.lib.auth.AuthBearerProvider

internal class DefaultOneInchProvider(
    private val configManager: ConfigManager,
) : AuthBearerProvider {

    override fun getApiKey(): String {
        return configManager.config.oneInchApiKey
    }
}