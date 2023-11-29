package com.tangem.datasource.api.common.response

import retrofit2.Response
import java.net.ConnectException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

internal fun <T : Any> Response<T>.toSafeApiResponse(): ApiResponse<T> {
    val body = body()

    return if (isSuccessful && body != null) {
        apiSuccess(body)
    } else {
        val code = ApiResponseError.HttpException.Code.values
            .firstOrNull { it.code == code() }
        val e = if (code == null) {
            ApiResponseError.UnknownException(IllegalArgumentException("Unknown error status code: ${code()}"))
        } else {
            ApiResponseError.HttpException(code, message(), errorBody()?.string())
        }

        apiError(e)
    }
}

internal fun Throwable.isNetworkException(): Boolean = when (this) {
    is ConnectException,
    is UnknownHostException,
    is SSLHandshakeException,
    -> true
    else -> false
}