package com.tangem.common.annotations

import com.tangem.datasource.api.common.config.ApiEnvironment

/**
 * Annotation to specify the API environment for a class or function.
 *
 * @property environment the API environment to be used (defaults to [ApiEnvironment.MOCK])
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiEnv(
    val environment: ApiEnvironment = ApiEnvironment.MOCK,
)