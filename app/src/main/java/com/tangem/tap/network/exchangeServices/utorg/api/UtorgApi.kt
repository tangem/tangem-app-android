package com.tangem.tap.network.exchangeServices.utorg.api

import com.squareup.moshi.Json
import com.tangem.tap.network.exchangeServices.utorg.api.model.UtorgCurrencyResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
* [REDACTED_AUTHOR]
 */
interface UtorgApi {

    @POST("api/merchant/{apiVersion}/settings/currency")
    suspend fun getCurrency(
        @Path("apiVersion") apiVersion: String,
    ): UtorgCurrencyResponse

    @POST("api/merchant/{apiVersion}/settings/successUrl")
    suspend fun setSuccessUrl(
        @Path("apiVersion") apiVersion: String,
        @Body request: RequestSuccessUrl,
    )
}

data class RequestSuccessUrl(
    val url: String,
)

interface UtorgResponse<Data> {
    val success: Boolean
    val timestamp: Long
    val data: Data?
    val error: UtorgErrorResponse?

    fun isSuccess(): Boolean = success && data != null && error == null

    @Throws(NullPointerException::class)
    fun toSuccess(): UtorgSuccessResponse<Data> {
        val internalTimestamp = this.timestamp
        val internalData = this.data

        return object : UtorgSuccessResponse<Data> {
            override val timestamp: Long = internalTimestamp
            override val data: Data = internalData!!
        }
    }

    @Throws(NullPointerException::class)
    fun toError(): UtorgErrorResponse = error!!
}

interface UtorgSuccessResponse<T> {
    val timestamp: Long
    val data: T
}

data class UtorgErrorResponse(
    @Json(name = "message") val message: String?,
    @Json(name = "type") val type: UtorgErrorType,
)

enum class UtorgErrorType {
    UNAUTHORIZED,
    UNKNOWN_ERROR,
    BAD_REQUEST,
}
