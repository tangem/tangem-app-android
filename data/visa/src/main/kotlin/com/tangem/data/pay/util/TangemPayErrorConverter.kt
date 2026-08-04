package com.tangem.data.pay.util

import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.spend.datasource.pay.models.response.TangemPayErrorResponse
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.converter.Converter
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TangemPayErrorConverter @Inject constructor(
    @NetworkMoshi moshi: Moshi,
) : Converter<Throwable, VisaApiError> {

    private val tangemPayErrorAdapter by lazy { moshi.adapter(TangemPayErrorResponse::class.java) }

    override fun convert(value: Throwable): VisaApiError {
        return if (value is ApiResponseError.HttpException) {
            if (value.isServerError()) return VisaApiError.ServerUnavailable
            if (value.code == ApiResponseError.HttpException.Code.NOT_FOUND) return VisaApiError.NotFound
            if (value.code == ApiResponseError.HttpException.Code.UNAUTHORIZED) return VisaApiError.RefreshTokenExpired

            val errorBody = value.errorBody ?: return VisaApiError.UnknownWithoutCode
            runCatching {
                tangemPayErrorAdapter.fromJson(errorBody)?.error?.code ?: value.code.numericCode
            }.map {
                VisaApiError.fromBackendError(it)
            }.getOrElse {
                VisaApiError.UnknownWithoutCode
            }
        } else {
            TangemLogger.e("Not HttpException. ${value.message}", value)
            VisaApiError.UnknownWithoutCode
        }
    }
}