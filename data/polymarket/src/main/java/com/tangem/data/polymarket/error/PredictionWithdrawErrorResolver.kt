package com.tangem.data.polymarket.error

import com.squareup.moshi.Moshi
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PredictionWithdrawError
import com.tangem.grow.datasource.polymarket.models.ProblemDetailResponse
import javax.inject.Inject

internal class PredictionWithdrawErrorResolver @Inject constructor(
    @NetworkMoshi moshi: Moshi,
) {

    private val problemDetailAdapter = moshi.adapter(ProblemDetailResponse::class.java)

    fun resolve(error: ApiResponseError): PredictionWithdrawError = when (error) {
        is ApiResponseError.HttpException -> resolveHttp(error)
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> PredictionWithdrawError.Network
        is ApiResponseError.UnknownException ->
            PredictionWithdrawError.Unexpected(httpCode = null, detail = error.cause.message ?: error.message)
    }

    private fun resolveHttp(error: ApiResponseError.HttpException): PredictionWithdrawError {
        val detail = parseDetail(error.errorBody)
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

    private fun parseDetail(errorBody: String?): String? =
        errorBody?.let { body -> runCatching { problemDetailAdapter.fromJson(body)?.detail }.getOrNull() }
}