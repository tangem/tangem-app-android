package com.tangem.datasource.api.common.config

import com.tangem.utils.ProviderSuspend

/**
 * Api environment config
 *
 * @property environment environment
 * @property baseUrl     base url
 * @property headers     headers
 *
 * @author Andrew Khokhlov on 13/08/2024
 */
data class ApiEnvironmentConfig(
    val environment: ApiEnvironment,
    val baseUrl: String,
    val headers: Map<String, ProviderSuspend<String>> = emptyMap(),
)
