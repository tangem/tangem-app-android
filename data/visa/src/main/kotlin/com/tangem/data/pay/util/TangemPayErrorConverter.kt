package com.tangem.data.pay.util

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.pay.models.response.TangemPayErrorResponse
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.converter.Converter
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
            if (value.code == ApiResponseError.HttpException.Code.NOT_FOUND) return VisaApiError.NotPaeraCustomer
            if (value.code == ApiResponseError.HttpException.Code.UNAUTHORIZED) return VisaApiError.RefreshTokenExpired

            val errorBody = value.errorBody ?: return VisaApiError.UnknownWithoutCode
            return runCatching {
                tangemPayErrorAdapter.fromJson(errorBody)?.error?.code ?: value.code.numericCode
            }.map {
                VisaApiError.fromBackendError(it)
            }.getOrElse {
                VisaApiError.UnknownWithoutCode
            }
        } else {
            VisaApiError.UnknownWithoutCode
        }
    }
}