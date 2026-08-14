package com.tangem.datasource.api.common.config.managers

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironmentConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * Api configs manager
 *
[REDACTED_AUTHOR]
 */
interface ApiConfigsManager {

    /** Flag that determines whether the manager is initialized */
    val initializedState: StateFlow<Boolean>

    /** Initialize resources */
    fun initialize()

    /** Get environment config by [id] */
    fun getEnvironmentConfig(id: ApiConfig.ID): ApiEnvironmentConfig
}