package com.tangem.data.pay.util

import com.squareup.moshi.Moshi
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.pay.models.response.VisaErrorResponseJsonAdapter
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.converter.Converter

class TangemPayErrorConverter(moshi: Moshi) : Converter<Throwable, UniversalError> {

    private val visaErrorAdapter = VisaErrorResponseJsonAdapter(moshi)

    override fun convert(value: Throwable): UniversalError {
        return if (value is ApiResponseError.HttpException) {
            val errorBody = value.errorBody ?: return VisaApiError.UnknownWithoutCode
            return runCatching {
                visaErrorAdapter.fromJson(errorBody)?.error?.code ?: value.code.numericCode
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