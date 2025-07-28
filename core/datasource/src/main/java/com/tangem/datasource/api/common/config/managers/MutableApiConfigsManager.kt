package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.ApiEnvironmentConfig
import kotlinx.coroutines.flow.Flow

/**
 * Mutable [ApiConfigsManager] for change information about the current api environment
 *
[REDACTED_AUTHOR]
 */
abstract class MutableApiConfigsManager : ApiConfigsManager {

    /** Api configs with current [ApiEnvironment] */
    abstract val configs: Flow<Map<ApiConfig, ApiEnvironment>>

    /**
     * A set of listeners registered to observe changes in API environment configurations.
     * These listeners are notified whenever an environment change occurs.
     */
    protected val registerListeners: Set<ApiConfigEnvChangeListener>
    field = mutableSetOf<ApiConfigEnvChangeListener>()

    /** Change api environment [environment] by [id] */
    abstract suspend fun changeEnvironment(id: String, environment: ApiEnvironment)

    /** Change api environment [environment] for all configs */
    abstract suspend fun changeEnvironment(environment: ApiEnvironment)

    /** Adds a [listener] to observe changes in API environment configurations */
    open fun addListener(listener: ApiConfigEnvChangeListener) {
        registerListeners += listener
    }

    /**
     * Listener for observing changes in API environment configurations
     *
     * @property id the identifier of the API configuration this listener is associated with
     */
    abstract class ApiConfigEnvChangeListener(val id: ApiConfig.ID) {

        /**
         * Called when the environment configuration changes
         *
         * @param environmentConfig the updated [ApiEnvironmentConfig] for the associated API configuration
         */
        abstract fun onChange(environmentConfig: ApiEnvironmentConfig)
    }
}