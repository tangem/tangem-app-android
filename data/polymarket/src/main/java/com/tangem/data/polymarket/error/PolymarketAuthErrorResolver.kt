package com.tangem.data.polymarket.error

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PolymarketAuthError
import javax.inject.Inject

internal class PolymarketAuthErrorResolver @Inject constructor() {

    fun resolve(error: ApiResponseError): PolymarketAuthError = when (error) {
        is ApiResponseError.HttpException -> resolveHttp(error)
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> PolymarketAuthError.Network
        is ApiResponseError.UnknownException ->
            PolymarketAuthError.Unknown(httpCode = null, detail = error.cause.message ?: error.message)
    }

    private fun resolveHttp(error: ApiResponseError.HttpException): PolymarketAuthError = when (error.code) {
        Code.UNAUTHORIZED -> PolymarketAuthError.InvalidSignature
        Code.NOT_FOUND -> PolymarketAuthError.KeyNotFound
        Code.TOO_MANY_REQUESTS -> PolymarketAuthError.RateLimited
        else -> PolymarketAuthError.Unknown(httpCode = error.code.numericCode, detail = error.errorBody)
    }
}