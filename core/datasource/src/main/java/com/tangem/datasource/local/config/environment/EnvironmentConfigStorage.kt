package com.tangem.datasource.local.config.environment

import kotlinx.coroutines.flow.Flow

/**
 * Storage for [EnvironmentConfig]
 *
[REDACTED_AUTHOR]
 */
interface EnvironmentConfigStorage {

    /** Initialize and return [EnvironmentConfig] */
    suspend fun initialize(): EnvironmentConfig

    /** Get [EnvironmentConfig] as [Flow] */
    fun getConfig(): Flow<EnvironmentConfig>

    /** Get [EnvironmentConfig] synchronously */
    fun getConfigSync(): EnvironmentConfig
}