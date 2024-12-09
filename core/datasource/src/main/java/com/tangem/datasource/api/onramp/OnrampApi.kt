package com.tangem.datasource.api.onramp

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.onramp.models.request.OnrampPairsRequest
import com.tangem.datasource.api.onramp.models.response.OnrampDataResponse
import com.tangem.datasource.api.onramp.models.response.model.OnrampPairDTO
import com.tangem.datasource.api.onramp.models.response.OnrampQuoteResponse
import com.tangem.datasource.api.onramp.models.response.OnrampStatusResponse
import com.tangem.datasource.api.onramp.models.response.model.OnrampCountryDTO
import com.tangem.datasource.api.onramp.models.response.model.OnrampCurrencyDTO
import com.tangem.datasource.api.onramp.models.response.model.PaymentMethodDTO
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

@Suppress("LongParameterList", "LargeClass", "TooManyFunctions")
interface OnrampApi {

    @GET("currencies")
    suspend fun getCurrencies(): ApiResponse<List<OnrampCurrencyDTO>>

    @GET("countries")
    suspend fun getCountries(): ApiResponse<List<OnrampCountryDTO>>

    @GET("country-by-ip")
    suspend fun getCountryByIp(): ApiResponse<OnrampCountryDTO>

    @GET("payment-methods")
    suspend fun getPaymentMethods(): ApiResponse<List<PaymentMethodDTO>>

    @POST("onramp-pairs")
    suspend fun getPairs(@Body body: OnrampPairsRequest): ApiResponse<List<OnrampPairDTO>>

    @GET("onramp-quote")
    suspend fun getQuote(
        @Query("fromCurrencyCode") fromCurrencyCode: String,
        @Query("fromPrecision") fromPrecision: Int,
        @Query("toContractAddress") toContractAddress: String,
        @Query("toNetwork") toNetwork: String,
        @Query("paymentMethod") paymentMethod: String,
        @Query("countryCode") countryCode: String,
        @Query("fromAmount") fromAmount: String,
        @Query("toDecimals") toDecimals: Int,
        @Query("providerId") providerId: String,
    ): ApiResponse<OnrampQuoteResponse>

    @GET("onramp-data")
    suspend fun getData(
        @Query("fromCurrencyCode") fromCurrencyCode: String,
        @Query("fromPrecision") fromPrecision: Int,
        @Query("toContractAddress") toContractAddress: String,
        @Query("toNetwork") toNetwork: String,
        @Query("paymentMethod") paymentMethod: String,
        @Query("countryCode") countryCode: String,
        @Query("fromAmount") fromAmount: String,
        @Query("toDecimals") toDecimals: Int,
        @Query("providerId") providerId: String,
        @Query("toAddress") toAddress: String,
        @Query("redirectUrl") redirectUrl: String,
        @Query("language") language: String?,
        @Query("theme") theme: String?,
        @Query("requestId") requestId: String,
    ): ApiResponse<OnrampDataResponse>

    @GET("onramp-status")
    suspend fun getStatus(@Query("txId") txId: String): ApiResponse<OnrampStatusResponse>
}