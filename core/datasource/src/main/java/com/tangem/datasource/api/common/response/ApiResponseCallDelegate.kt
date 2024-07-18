package com.tangem.datasource.api.common.response

import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

internal class ApiResponseCallDelegate<T : Any>(
    private val wrappedCall: Call<T>,
) : Call<ApiResponse<T>> {

    override fun enqueue(callback: Callback<ApiResponse<T>>) {
        wrappedCall.enqueue(ApiResponseCallback(callback))
    }

    override fun execute(): Response<ApiResponse<T>> = throw NotImplementedError()
    override fun clone(): Call<ApiResponse<T>> = ApiResponseCallDelegate(wrappedCall.clone())
    override fun request(): Request = wrappedCall.request()
    override fun timeout(): Timeout = wrappedCall.timeout()
    override fun isExecuted(): Boolean = wrappedCall.isExecuted
    override fun isCanceled(): Boolean = wrappedCall.isCanceled
    override fun cancel() {
        wrappedCall.cancel()
    }

    private inner class ApiResponseCallback(
        private val responseCallback: Callback<ApiResponse<T>>,
    ) : Callback<T> {

        override fun onResponse(call: Call<T>, response: Response<T>) {
            val safeResponse = response.toSafeApiResponse()

            responseCallback.onResponse(this@ApiResponseCallDelegate, Response.success(safeResponse))
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            val error = try {
                t.toApiError()
            } catch (e: ApiResponseError) {
                Timber.e(e, "error map toApiError")
                e
            } catch (e: Exception) {
                Timber.e(e, "onFailure UnknownException")
                ApiResponseError.UnknownException(e)
            }
            val safeResponse = apiError<T>(error)

            responseCallback.onResponse(this@ApiResponseCallDelegate, Response.success(safeResponse))
        }
    }
}