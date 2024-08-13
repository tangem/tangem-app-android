package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironmentConfig

/**
 * Api configs manager
 *
 * @author Andrew Khokhlov on 07/08/2024
 */
interface ApiConfigsManager {

    /** Initialize resources */
    suspend fun initialize() {}

    /** Get environment config by [id] */
    fun getEnvironmentConfig(id: ApiConfig.ID): ApiEnvironmentConfig
}
