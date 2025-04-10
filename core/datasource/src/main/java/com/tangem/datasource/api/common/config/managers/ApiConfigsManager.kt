package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironmentConfig

/**
 * Api configs manager
 *
[REDACTED_AUTHOR]
 */
interface ApiConfigsManager {

    /** Initialize resources */
    fun initialize() {}

    /** Get environment config by [id] */
    fun getEnvironmentConfig(id: ApiConfig.ID): ApiEnvironmentConfig
}