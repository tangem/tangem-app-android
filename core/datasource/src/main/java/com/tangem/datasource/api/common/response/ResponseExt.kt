package com.tangem.datasource.api.common.response

import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.datasource.api.common.response.analytics.ApiErrorEvent
import kotlinx.coroutines.TimeoutCancellationException
import retrofit2.Response
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLHandshakeException

internal fun <T : Any> Response<T>.toSafeApiResponse(analyticsErrorHandler: AnalyticsErrorHandler): ApiResponse<T> {
    val headers = headers().toMultimap()
    val body = body()

    return if (isSuccessful && body != null) {
        apiSuccess(data = body, headers = headers)
    } else {
        val code = ApiResponseError.HttpException.Code.entries
            .firstOrNull { it.numericCode == code() }
        val e = try {
            if (code == null) {
                ApiResponseError.UnknownException(IllegalArgumentException("Unknown error status code: ${code()}"))
            } else {
                val errorBody = errorBody()?.string().orEmpty() // !!!Beware!!! string() closes stream after invocation
                sendHttpError(code, analyticsErrorHandler, errorBody)

                ApiResponseError.HttpException(code, message(), errorBody)
            }
        } catch (e: Exception) {
            Timber.e(e, "UnknownException occured")
            ApiResponseError.UnknownException(e)
        }

        apiError(e, headers)
    }
}

private fun <T : Any> Response<T>.sendHttpError(
    code: ApiResponseError.HttpException.Code,
    analyticsErrorHandler: AnalyticsErrorHandler,
    errorBody: String,
) {
    val fullRequestUrl = raw().request.url.toUrl()
    val shortUrl = fullRequestUrl.authority + fullRequestUrl.path
    analyticsErrorHandler.sendErrorEvent(
        ApiErrorEvent(
            endpoint = shortUrl,
            code = code.numericCode,
            message = errorBody,
        ),
    )
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