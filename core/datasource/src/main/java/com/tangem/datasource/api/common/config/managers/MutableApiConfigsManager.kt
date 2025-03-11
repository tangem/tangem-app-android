package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import kotlinx.coroutines.flow.Flow

/**
 * Mutable [ApiConfigsManager] for change information about the current api environment
 *
* [REDACTED_AUTHOR]
 */
interface MutableApiConfigsManager : ApiConfigsManager {

    /** Api configs with current [ApiEnvironment] */
    val configs: Flow<Map<ApiConfig, ApiEnvironment>>

    /** Change api environment [environment] by [id] */
    suspend fun changeEnvironment(id: String, environment: ApiEnvironment)
}
