package com.tangem.grow.datasource.onramp

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.onramp.models.request.OnrampPairsRequest
import com.tangem.grow.datasource.onramp.models.response.OnrampDataResponse
import com.tangem.grow.datasource.onramp.models.response.OnrampHistoryDeltaResponse
import com.tangem.grow.datasource.onramp.models.response.OnrampHistoryResponse
import com.tangem.grow.datasource.onramp.models.response.OnrampQuoteResponse
import com.tangem.grow.datasource.onramp.models.response.OnrampItemResponse
import com.tangem.grow.datasource.onramp.models.response.model.OnrampCountryDTO
import com.tangem.grow.datasource.onramp.models.response.model.OnrampCurrencyDTO
import com.tangem.grow.datasource.onramp.models.response.model.OnrampPairDTO
import com.tangem.grow.datasource.onramp.models.response.model.PaymentMethodDTO
import com.tangem.lib.auth.api.RequiresSessionAuth
import retrofit2.http.*

/**
 * [RequiresSessionAuth] takes effect only on Express `/v2`, where the client is built with session auth.
 */
@Suppress("LongParameterList", "LargeClass", "TooManyFunctions")
interface OnrampApi {

    @RequiresSessionAuth
    @GET("currencies")
    suspend fun getCurrencies(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String?,
    ): ApiResponse<List<OnrampCurrencyDTO>>

    @RequiresSessionAuth
    @GET("countries")
    suspend fun getCountries(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String?,
    ): ApiResponse<List<OnrampCountryDTO>>

    @RequiresSessionAuth
    @GET("country-by-ip")
    suspend fun getCountryByIp(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String?,
    ): ApiResponse<OnrampCountryDTO>

    @RequiresSessionAuth
    @GET("payment-methods")
    suspend fun getPaymentMethods(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String?,
    ): ApiResponse<List<PaymentMethodDTO>>

    @RequiresSessionAuth
    @POST("onramp-pairs")
    suspend fun getPairs(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String?,
        @Body body: OnrampPairsRequest,
    ): ApiResponse<List<OnrampPairDTO>>

    @RequiresSessionAuth
    @GET("onramp-quote")
    suspend fun getQuote(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String?,
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

    @RequiresSessionAuth
    @GET("onramp-data")
    suspend fun getData(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String?,
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

    @RequiresSessionAuth
    @GET("onramp-status")
    suspend fun getStatus(
        @Header("user-id") userWalletId: String,
        @Header("refcode") refCode: String?,
        @Query("txId") txId: String,
    ): ApiResponse<OnrampItemResponse>

    @RequiresSessionAuth
    @GET("history/onramp")
    suspend fun getHistory(
        @Header("user-id") userWalletId: String,
        @Query("afterCursor") afterCursor: String?,
        @Query("limit") limit: Int = 100,
    ): ApiResponse<OnrampHistoryResponse>

    @RequiresSessionAuth
    @GET("history/delta/onramp")
    suspend fun getHistoryDelta(
        @Header("user-id") userWalletId: String,
        @Query("beforeCursor") cursor: String?,
        @Query("limit") limit: Int = 100,
    ): ApiResponse<OnrampHistoryDeltaResponse>
}