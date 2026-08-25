package com.tangem.core.remote.header

import com.tangem.core.remote.config.ApiEnvironment

/**
 * Supplies the auth-backed `api-key` [RequestHeader] for a given API environment.
 *
 * The implementation lives in the module that owns the api-key source, so [ApiConfig][com.tangem.core.remote.config.ApiConfig]s
 * declared in other modules can attach the header by injecting this contract instead of depending on
 * that source directly.
 */
fun interface TangemApiKeyHeaderProvider {

    fun forEnvironment(environment: ApiEnvironment): RequestHeader
}