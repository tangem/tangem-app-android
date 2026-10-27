package com.tangem.feature.swap.mcp.model

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.core.error.DataError
import com.tangem.domain.mcp.models.analytics.McpIntentAnalyticsEvent.Reason
import com.tangem.domain.swap.models.ExchangeIntentError

/** Express error codes the repository turns into typed errors, kept for analytics. */
private const val INTENT_NOT_FOUND_CODE = "2600"
private const val INTENT_OF_ANOTHER_WALLET_CODE = "2610"

/** Why the offer of an exchange intent is not shown, as analytics reports it. */
internal data class ExchangeIntentLoadFailure(val reason: Reason, val errorCode: String? = null) {

    companion object {

        fun ofReadError(error: Throwable): ExchangeIntentLoadFailure = when (error) {
            is ExchangeIntentError.NotFound -> ExchangeIntentLoadFailure(Reason.NotFound, INTENT_NOT_FOUND_CODE)
            is DataError.UserWalletError.WrongUserWallet -> {
                ExchangeIntentLoadFailure(Reason.WrongWallet, INTENT_OF_ANOTHER_WALLET_CODE)
            }
            is ExchangeIntentError.Unreachable -> ExchangeIntentLoadFailure(Reason.Unreachable)
            is ExchangeIntentError.AmbiguousAmountSide -> ExchangeIntentLoadFailure(Reason.InvalidAmount)
            is ApiResponseError.HttpException -> {
                ExchangeIntentLoadFailure(Reason.Unknown, error.code.numericCode.toString())
            }
            else -> ExchangeIntentLoadFailure(Reason.Unknown)
        }
    }
}