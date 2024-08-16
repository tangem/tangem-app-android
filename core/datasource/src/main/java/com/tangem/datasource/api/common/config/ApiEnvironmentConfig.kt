package com.tangem.datasource.api.common.config

import com.tangem.utils.Provider

/**
 * Api environment config
 *
 * @property environment environment
 * @property baseUrl     base url
 * @property headers     headers
 *
* [REDACTED_AUTHOR]
 */
data class ApiEnvironmentConfig(
    val environment: ApiEnvironment,
    val baseUrl: String,
    val headers: Map<String, Provider<String>> = emptyMap(),
)
