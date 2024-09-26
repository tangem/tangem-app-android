package com.tangem.datasource.config

import com.tangem.datasource.config.models.Config
import kotlinx.coroutines.flow.Flow

/**
 * Config manager
 *
[REDACTED_AUTHOR]
 */
interface ConfigManager {

    /** Initialize and return [Config] */
    suspend fun initialize(): Config

    /** Get [Config] as [Flow] */
    fun getConfig(): Flow<Config>

    /** Get [Config] synchronously */
    fun getConfigSync(): Config
}