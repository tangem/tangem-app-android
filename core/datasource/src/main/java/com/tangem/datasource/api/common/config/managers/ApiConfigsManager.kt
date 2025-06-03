package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironmentConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * Api configs manager
 *
[REDACTED_AUTHOR]
 */
interface ApiConfigsManager {

    /** Flag that determines whether the manager is initialized */
    val isInitialized: StateFlow<Boolean>

    /** Initialize resources */
    fun initialize()

    /** Get environment config by [id] */
    fun getEnvironmentConfig(id: ApiConfig.ID): ApiEnvironmentConfig
}