package com.tangem.common.annotations

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment

/**
 * Single API environment configuration.
 *
 * @property apiConfigId the ID of the API configuration
 * @property environment the API environment to be used (defaults to [ApiEnvironment.MOCK])
 */
annotation class ApiEnvConfig(
    val apiConfigId: ApiConfig.ID,
    val environment: ApiEnvironment = ApiEnvironment.MOCK,
)

/**
 * Annotation to specify the API environment configurations for a class or function.
 *
 * @property value array of API environment configurations
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiEnv(
    vararg val value: ApiEnvConfig,
)