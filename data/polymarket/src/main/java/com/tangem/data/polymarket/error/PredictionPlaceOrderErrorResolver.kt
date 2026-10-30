package com.tangem.data.polymarket.error

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import javax.inject.Inject

internal class PredictionPlaceOrderErrorResolver @Inject constructor() {

    /** @param identity the order's identity; only an order of a session key is refused for its key */
    fun resolve(error: ApiResponseError, identity: PolymarketClobIdentity): PredictionPlaceOrderError = when (error) {
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> PredictionPlaceOrderError.Ambiguous(detail = error.message)
        is ApiResponseError.HttpException -> error.toPlaceOrderError(identity = identity)
        is ApiResponseError.UnknownException ->
            PredictionPlaceOrderError.Ambiguous(detail = error.cause.message ?: error.message)
    }

    private fun ApiResponseError.HttpException.toPlaceOrderError(
        identity: PolymarketClobIdentity,
    ): PredictionPlaceOrderError {
        val sessionKeyRefusal = ClobSignerRefusalParser.parse(errorBody)
            ?.takeIf { identity is PolymarketClobIdentity.Session && code.numericCode in CLIENT_ERROR_CODES }

        return when {
            code == Code.UNAUTHORIZED -> PredictionPlaceOrderError.Unauthorized(detail = errorBody)
            sessionKeyRefusal != null -> PredictionPlaceOrderError.SessionKeyRefused(
                httpCode = code.numericCode,
                reason = sessionKeyRefusal,
            )
            isNoMatchForFillAndKill() -> PredictionPlaceOrderError.NoLiquidity(detail = errorBody)
            code == Code.BAD_REQUEST -> PredictionPlaceOrderError.Rejected(detail = errorBody)
            else -> PredictionPlaceOrderError.Ambiguous(detail = errorBody)
        }
    }

    private fun ApiResponseError.HttpException.isNoMatchForFillAndKill(): Boolean =
        code == Code.BAD_REQUEST && errorBody?.contains(other = NO_MATCH_MARKER, ignoreCase = true) == true

    private companion object {
        const val NO_MATCH_MARKER = "no orders found to match"
        val CLIENT_ERROR_CODES = 400..499
    }
}