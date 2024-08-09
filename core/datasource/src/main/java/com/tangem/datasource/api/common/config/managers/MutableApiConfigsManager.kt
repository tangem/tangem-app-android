package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import kotlinx.coroutines.flow.Flow

/**
 * Mutable [ApiConfigsManager] for change information about the current api environment
 *
[REDACTED_AUTHOR]
 */
interface MutableApiConfigsManager : ApiConfigsManager {

    /** Configs */
    val configs: Flow<List<ApiConfig>>

    /** Change api environment [environment] by [id] */
    suspend fun changeEnvironment(id: String, environment: ApiEnvironment)
}