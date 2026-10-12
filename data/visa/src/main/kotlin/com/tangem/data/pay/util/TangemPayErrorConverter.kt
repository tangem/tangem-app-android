package com.tangem.data.pay.util

import com.tangem.core.remote.json.NetworkJson
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.models.response.TangemPayErrorResponse
import com.tangem.utils.converter.Converter
import com.tangem.utils.logging.TangemLogger
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TangemPayErrorConverter @Inject constructor(
    @NetworkJson private val json: Json,
) : Converter<Throwable, VisaApiError> {

    override fun convert(value: Throwable): VisaApiError {
        return if (value is ApiResponseError.HttpException) {
            if (value.isServerError()) return VisaApiError.ServerUnavailable
            if (value.code == ApiResponseError.HttpException.Code.NOT_FOUND) return VisaApiError.NotFound
            if (value.code == ApiResponseError.HttpException.Code.UNAUTHORIZED) return VisaApiError.RefreshTokenExpired

            val error = TangemPayErrorResponse.decodeOrNull(json, value.errorBody)?.error
                ?: return VisaApiError.UnknownWithoutCode

            VisaApiError.fromBackendName(error.name) ?: VisaApiError.fromBackendError(error.code)
        } else {
            TangemLogger.e("Not HttpException. ${value.message}", value)
            VisaApiError.UnknownWithoutCode
        }
    }
}