package com.tangem.datasource.api.onramp

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.onramp.models.request.OnrampPairsRequest
import com.tangem.datasource.api.onramp.models.response.OnrampDataResponse
import com.tangem.datasource.api.onramp.models.response.OnrampQuoteResponse
import com.tangem.datasource.api.onramp.models.response.OnrampStatusResponse
import com.tangem.datasource.api.onramp.models.response.model.OnrampCountryDTO
import com.tangem.datasource.api.onramp.models.response.model.OnrampCurrencyDTO
import com.tangem.datasource.api.onramp.models.response.model.OnrampPairDTO
import com.tangem.datasource.api.onramp.models.response.model.PaymentMethodDTO
import retrofit2.http.*

@Suppress("LongParameterList", "LargeClass", "TooManyFunctions")
interface OnrampApi {

    @GET("currencies")
    suspend fun getCurrencies(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String,
    ): ApiResponse<List<OnrampCurrencyDTO>>

    @GET("countries")
    suspend fun getCountries(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String,
    ): ApiResponse<List<OnrampCountryDTO>>

    @GET("country-by-ip")
    suspend fun getCountryByIp(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String,
    ): ApiResponse<OnrampCountryDTO>

    @GET("payment-methods")
    suspend fun getPaymentMethods(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String,
    ): ApiResponse<List<PaymentMethodDTO>>

    @POST("onramp-pairs")
    suspend fun getPairs(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String,
        @Body body: OnrampPairsRequest,
    ): ApiResponse<List<OnrampPairDTO>>

    @GET("onramp-quote")
    suspend fun getQuote(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String,
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
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String,
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
    suspend fun getStatus(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String,
        @Query("txId") txId: String,
    ): ApiResponse<OnrampStatusResponse>
}