package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.config.environment.models.ExpressModel
import kotlinx.coroutines.flow.flowOf

/**
 * Mock [EnvironmentConfigStorage] implementation for [ProdApiConfigsManagerTest]
 *
[REDACTED_AUTHOR]
 */
internal class MockEnvironmentConfigStorage : EnvironmentConfigStorage {

    private val environmentConfig = EnvironmentConfig(
        express = ExpressModel(apiKey = EXPRESS_API_KEY, signVerifierPublicKey = "vocibus"),
        devExpress = ExpressModel(apiKey = EXPRESS_DEV_API_KEY, signVerifierPublicKey = "pellentesque"),
    )

    override suspend fun initialize() = environmentConfig
    override fun getConfig() = flowOf(environmentConfig)
    override fun getConfigSync() = environmentConfig

    companion object {
        const val EXPRESS_API_KEY = "express_api_key"
        const val EXPRESS_DEV_API_KEY = "express_dev_api_key"
    }
}