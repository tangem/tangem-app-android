package com.tangem.data.polymarket.error

import com.squareup.moshi.Moshi
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.core.remote.response.ApiResponseError
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
        return when (error.code.numericCode) {
            HTTP_BAD_REQUEST -> PredictionWithdrawError.InvalidRequest(detail = detail)
            HTTP_UNAUTHORIZED -> PredictionWithdrawError.Unauthorized
            HTTP_NOT_FOUND -> PredictionWithdrawError.NotFound
            HTTP_CONFLICT -> PredictionWithdrawError.NonceTaken(detail = detail)
            HTTP_UNPROCESSABLE_ENTITY -> PredictionWithdrawError.RelayerRejected(detail = detail)
            HTTP_BAD_GATEWAY -> PredictionWithdrawError.RelayerUnavailable
            else -> PredictionWithdrawError.Unexpected(httpCode = error.code.numericCode, detail = detail)
        }
    }

    private fun parseDetail(errorBody: String?): String? =
        errorBody?.let { body -> runCatching { problemDetailAdapter.fromJson(body)?.detail }.getOrNull() }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
        const val HTTP_UNPROCESSABLE_ENTITY = 422
        const val HTTP_BAD_GATEWAY = 502
    }
}