package com.tangem.core.remote.config

import com.tangem.utils.ProviderSuspend

/**
 * Api environment config
 *
 * @property environment environment
 * @property baseUrl     base url; must end with '/' (Retrofit requirement)
 * @property headers     headers
 *
[REDACTED_AUTHOR]
 */
data class ApiEnvironmentConfig(
    val environment: ApiEnvironment,
    val baseUrl: String,
    val headers: Map<String, ProviderSuspend<String>> = emptyMap(),
) {

    init {
        // Retrofit's baseUrl(...) rejects a URL without a trailing '/'. Fail fast here, for every
        // ApiConfig at construction, instead of later at Retrofit build time.
        require(baseUrl.endsWith('/')) { "ApiEnvironmentConfig.baseUrl must end with '/', but was: $baseUrl" }
    }
}