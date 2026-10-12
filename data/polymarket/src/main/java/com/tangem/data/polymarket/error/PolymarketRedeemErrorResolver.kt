package com.tangem.data.polymarket.error

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PolymarketRedeemError
import javax.inject.Inject

internal class PolymarketRedeemErrorResolver @Inject constructor(
    private val problemDetailParser: ProblemDetailParser,
) {

    fun resolve(error: ApiResponseError): PolymarketRedeemError = when (error) {
        is ApiResponseError.HttpException -> resolveHttp(error)
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> PolymarketRedeemError.Network
        is ApiResponseError.UnknownException ->
            PolymarketRedeemError.Unexpected(httpCode = null, detail = error.cause.message ?: error.message)
    }

    private fun resolveHttp(error: ApiResponseError.HttpException): PolymarketRedeemError {
        val detail = problemDetailParser.parseDetail(error.errorBody)
        return when (error.code) {
            Code.BAD_REQUEST -> PolymarketRedeemError.InvalidRequest(detail = detail)
            Code.UNAUTHORIZED -> PolymarketRedeemError.Unauthorized
            Code.NOT_FOUND -> PolymarketRedeemError.NotFound
            Code.UNPROCESSABLE_ENTITY -> PolymarketRedeemError.RelayerRejected(detail = detail)
            Code.BAD_GATEWAY -> PolymarketRedeemError.RelayerUnavailable
            else -> PolymarketRedeemError.Unexpected(httpCode = error.code.numericCode, detail = detail)
        }
    }
}