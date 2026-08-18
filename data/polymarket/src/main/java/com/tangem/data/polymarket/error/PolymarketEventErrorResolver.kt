package com.tangem.data.polymarket.error

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PolymarketEventError
import javax.inject.Inject

/**
 * Maps a single-event BFF failure onto [PolymarketEventError].
 *
 * `404` is the one status worth telling apart: an event the BFF no longer serves is a dead end, while every
 * other failure is worth retrying.
 */
internal class PolymarketEventErrorResolver @Inject constructor() {

    fun resolve(error: ApiResponseError): PolymarketEventError = when (error) {
        is ApiResponseError.HttpException -> resolveHttp(error)
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> PolymarketEventError.Network
        is ApiResponseError.UnknownException ->
            PolymarketEventError.Unknown(httpCode = null, detail = error.cause.message ?: error.message)
    }

    private fun resolveHttp(error: ApiResponseError.HttpException): PolymarketEventError = when (error.code) {
        Code.NOT_FOUND -> PolymarketEventError.NotFound
        else -> PolymarketEventError.Unknown(httpCode = error.code.numericCode, detail = error.errorBody)
    }
}