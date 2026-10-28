package com.tangem.features.send.mcp.model

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.mcp.models.analytics.McpIntentAnalyticsEvent.Reason
import com.tangem.domain.transaction.error.SendIntentError

private const val NETWORK_ERROR_CODE = "network"
private const val TIMEOUT_ERROR_CODE = "timeout"
private const val UNKNOWN_ERROR_CODE = "unknown"

/** Why the offer of a send intent is not shown, as analytics reports it. */
internal data class SendIntentLoadFailure(val reason: Reason, val errorCode: String? = null) {

    companion object {

        fun ofReadError(error: Throwable): SendIntentLoadFailure = when (error) {
            is SendIntentError.NotFound -> SendIntentLoadFailure(Reason.NotFound, error.code)
            is SendIntentError.AlreadyCompleted -> SendIntentLoadFailure(Reason.AlreadyProcessed, error.code)
            is SendIntentError.Unreachable -> SendIntentLoadFailure(Reason.Unreachable)
            is SendIntentError.MalformedAmount -> SendIntentLoadFailure(Reason.InvalidAmount)
            is ApiResponseError.HttpException -> SendIntentLoadFailure(Reason.Unknown, error.httpCode())
            else -> SendIntentLoadFailure(Reason.Unknown)
        }
    }
}

/** The error code analytics reports for a transaction hash the Transaction Service did not take. */
internal fun sendIntentReportErrorCode(error: Throwable): String = when (error) {
    is SendIntentError.NotFound -> error.code
    is SendIntentError.AlreadyCompleted -> error.code
    is ApiResponseError.HttpException -> error.httpCode()
    is ApiResponseError.NetworkException -> NETWORK_ERROR_CODE
    is ApiResponseError.TimeoutException -> TIMEOUT_ERROR_CODE
    else -> UNKNOWN_ERROR_CODE
}

private fun ApiResponseError.HttpException.httpCode(): String = code.numericCode.toString()