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
        blockAidApiKey = BLOCK_AID_API_KEY,
        tangemApiKey = TANGEM_API_KEY,
        tangemApiKeyDev = TANGEM_API_KEY_DEV,
        tangemApiKeyStage = TANGEM_API_KEY_STAGE,
        yieldModuleApiKey = YIELD_MODULE_KEY,
        yieldModuleApiKeyDev = YIELD_MODULE_KEY_DEV,
    )

    override suspend fun initialize() = environmentConfig
    override fun getConfig() = flowOf(environmentConfig)
    override fun getConfigSync() = environmentConfig

    companion object {
        const val EXPRESS_API_KEY = "express_api_key"
        const val EXPRESS_DEV_API_KEY = "express_dev_api_key"
        const val BLOCK_AID_API_KEY = "block_aid_api_key"
        const val TANGEM_API_KEY = "tangem_api_key"
        const val TANGEM_API_KEY_DEV = "tangem_api_key_dev"
        const val TANGEM_API_KEY_STAGE = "tangem_api_key_stage"
        const val YIELD_MODULE_KEY = "yield_module_api_key"
        const val YIELD_MODULE_KEY_DEV = "yield_module_api_key_dev"
    }
}