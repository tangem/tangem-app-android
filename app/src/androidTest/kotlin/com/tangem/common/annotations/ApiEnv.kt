package com.tangem.common.annotations

import com.tangem.core.remote.config.ApiEnvironment

/**
 * Single API environment configuration.
 *
 * @property apiConfigId the API configuration id key; pass a raw-key constant from the config
 *                       (e.g. `TangemTech.KEY`). A value class can't be an annotation
 *                       argument, so the string key is used instead of the type-safe id here
 * @property environment the API environment to be used (defaults to [ApiEnvironment.MOCK])
 */
annotation class ApiEnvConfig(
    val apiConfigId: String,
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