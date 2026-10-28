package com.tangem.feature.swap.mcp.model

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.mcp.models.analytics.McpIntentAnalyticsEvent.Reason
import com.tangem.domain.swap.models.ExchangeIntentError

/** Why the offer of an exchange intent is not shown, as analytics reports it. */
internal data class ExchangeIntentLoadFailure(val reason: Reason, val errorCode: String? = null) {

    companion object {

        fun ofReadError(error: Throwable): ExchangeIntentLoadFailure = when (error) {
            is ExchangeIntentError.NotFound -> ExchangeIntentLoadFailure(Reason.NotFound, error.code.toString())
            is ExchangeIntentError.OtherWallet -> ExchangeIntentLoadFailure(Reason.WrongWallet, error.code.toString())
            is ExchangeIntentError.Unreachable -> ExchangeIntentLoadFailure(Reason.Unreachable)
            is ExchangeIntentError.AmbiguousAmountSide -> ExchangeIntentLoadFailure(Reason.InvalidAmount)
            is ApiResponseError.HttpException -> {
                ExchangeIntentLoadFailure(Reason.Unknown, error.code.numericCode.toString())
            }
            else -> ExchangeIntentLoadFailure(Reason.Unknown)
        }
    }
}