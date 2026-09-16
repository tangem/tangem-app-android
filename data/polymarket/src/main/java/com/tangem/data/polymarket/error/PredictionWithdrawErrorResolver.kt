package com.tangem.data.polymarket.error

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PredictionWithdrawError
import javax.inject.Inject

internal class PredictionWithdrawErrorResolver @Inject constructor(
    private val problemDetailParser: ProblemDetailParser,
) {

    fun resolve(error: ApiResponseError): PredictionWithdrawError = when (error) {
        is ApiResponseError.HttpException -> resolveHttp(error)
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> PredictionWithdrawError.Network
        is ApiResponseError.UnknownException ->
            PredictionWithdrawError.Unexpected(httpCode = null, detail = error.cause.message ?: error.message)
    }

    private fun resolveHttp(error: ApiResponseError.HttpException): PredictionWithdrawError {
        val detail = problemDetailParser.parseDetail(error.errorBody)
        return when (error.code) {
            Code.BAD_REQUEST -> PredictionWithdrawError.InvalidRequest(detail = detail)
            Code.UNAUTHORIZED -> PredictionWithdrawError.Unauthorized
            Code.NOT_FOUND -> PredictionWithdrawError.NotFound
            Code.CONFLICT -> PredictionWithdrawError.NonceTaken(detail = detail)
            Code.UNPROCESSABLE_ENTITY -> PredictionWithdrawError.RelayerRejected(detail = detail)
            Code.BAD_GATEWAY -> PredictionWithdrawError.RelayerUnavailable
            else -> PredictionWithdrawError.Unexpected(httpCode = error.code.numericCode, detail = detail)
        }
    }
}