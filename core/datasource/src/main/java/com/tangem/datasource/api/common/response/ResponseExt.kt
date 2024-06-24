package com.tangem.datasource.api.common.response

import kotlinx.coroutines.TimeoutCancellationException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
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

internal fun Throwable.toApiError(): ApiResponseError = when (this) {
    is ConnectException,
    is UnknownHostException,
    is SSLHandshakeException,
    -> ApiResponseError.NetworkException
    is TimeoutException,
    is TimeoutCancellationException,
    is SocketTimeoutException,
    -> ApiResponseError.TimeoutException
    else -> ApiResponseError.UnknownException(cause = this)
}