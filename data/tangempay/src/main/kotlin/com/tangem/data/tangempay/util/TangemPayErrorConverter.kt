package com.tangem.data.tangempay.util

import com.tangem.core.remote.json.NetworkJson
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.spend.datasource.tangempay.models.response.TangemPayErrorResponse
import com.tangem.utils.converter.Converter
import com.tangem.utils.logging.TangemLogger
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TangemPayErrorConverter @Inject constructor(
    @NetworkJson private val json: Json,
) : Converter<Throwable, TangemPayApiError> {

    override fun convert(value: Throwable): TangemPayApiError {
        return if (value is ApiResponseError.HttpException) {
            if (value.isServerError()) return TangemPayApiError.ServerUnavailable
            if (value.code == ApiResponseError.HttpException.Code.NOT_FOUND) return TangemPayApiError.NotFound
            if (value.code == ApiResponseError.HttpException.Code.UNAUTHORIZED) {
                return TangemPayApiError.RefreshTokenExpired
            }

            val error = TangemPayErrorResponse.decodeOrNull(json, value.errorBody)?.error
                ?: return TangemPayApiError.UnknownWithoutCode

            TangemPayApiError.fromBackendName(error.name) ?: TangemPayApiError.fromBackendError(error.code)
        } else {
            TangemLogger.e("Not HttpException. ${value.message}", value)
            TangemPayApiError.UnknownWithoutCode
        }
    }
}