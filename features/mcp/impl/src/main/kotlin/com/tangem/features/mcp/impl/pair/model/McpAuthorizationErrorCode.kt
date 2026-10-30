package com.tangem.features.mcp.impl.pair.model

import com.tangem.core.remote.response.ApiResponseError

/** Error codes the authorization analytics reports, in the format of the intent ones. */
internal object McpAuthorizationErrorCode {

    /** The service answered the approval without an error, but did not grant the access. */
    const val REFUSED = "refused"
    const val UNKNOWN = "unknown"

    private const val NETWORK = "network"
    private const val TIMEOUT = "timeout"

    fun of(error: Throwable): String = when (error) {
        is ApiResponseError.HttpException -> error.code.numericCode.toString()
        is ApiResponseError.NetworkException -> NETWORK
        is ApiResponseError.TimeoutException -> TIMEOUT
        else -> UNKNOWN
    }
}