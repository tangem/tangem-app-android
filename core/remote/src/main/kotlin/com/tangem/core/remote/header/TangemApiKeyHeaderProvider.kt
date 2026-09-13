package com.tangem.core.remote.header

import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.utils.Provider

/**
 * Supplies the auth-backed `api-key` [RequestHeader] for a given API environment.
 *
 * The implementation lives in the module that owns the api-key source, so [ApiConfig][com.tangem.core.remote.config.ApiConfig]s
 * declared in other modules can attach the header by injecting this contract instead of depending on
 * that source directly.
 */
fun interface TangemApiKeyHeaderProvider {

    /** Resolves the environment lazily, so the header follows runtime environment switching. */
    fun forEnvironment(environment: Provider<ApiEnvironment>): RequestHeader

    /** Convenience for a fixed environment. */
    fun forEnvironment(environment: ApiEnvironment): RequestHeader = forEnvironment(Provider { environment })
}