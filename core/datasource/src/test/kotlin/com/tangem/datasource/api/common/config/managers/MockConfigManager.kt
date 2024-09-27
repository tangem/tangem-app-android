package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.config.models.Config
import com.tangem.datasource.config.models.ExpressModel
import kotlinx.coroutines.flow.flowOf

/**
 * Mock [ConfigManager] implementation for [ProdApiConfigsManagerTest]
 *
[REDACTED_AUTHOR]
 */
internal class MockConfigManager : ConfigManager {

    private val config = Config(
        express = ExpressModel(apiKey = EXPRESS_API_KEY, signVerifierPublicKey = "vocibus"),
        devExpress = ExpressModel(apiKey = EXPRESS_DEV_API_KEY, signVerifierPublicKey = "pellentesque"),
    )

    override suspend fun initialize() = config
    override fun getConfig() = flowOf(config)
    override fun getConfigSync() = config

    companion object {
        const val EXPRESS_API_KEY = "express_api_key"
        const val EXPRESS_DEV_API_KEY = "express_dev_api_key"
    }
}