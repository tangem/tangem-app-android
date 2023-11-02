package com.tangem.feature.swap

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.feature.swap.models.request.AssetsRequestBody
import com.tangem.feature.swap.models.request.PairsRequestBody
import com.tangem.feature.swap.models.response.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.math.BigDecimal

/**
 * Interface of Tangem Express API (new swap mechanism)
 */
internal interface ExpressApi {

    // TODO move first three params to retrofit interceptor
    @POST("assets")
    suspend fun getAssets(
        @Header("api-key") apiKey: String,
        @Header("user-id") userId: String,
        @Header("session-id") sessionId: String,
        @Body body: AssetsRequestBody,
    ): ApiResponse<List<Asset>>

    @POST("pairs")
    suspend fun getPairs(
        @Body body: PairsRequestBody,
    ): ApiResponse<List<SwapPair>>

    @GET("providers")
    suspend fun getProviders(): ApiResponse<List<ExchangeProvider>>

    @GET("exchange-quote")
    suspend fun getExchangeQuote(
        @Query("fromContractAddress") fromContractAddress: String,
        @Query("fromNetwork") fromNetwork: String,
        @Query("toContractAddress") toContractAddress: String,
        @Query("toNetwork") toNetwork: String,
        @Query("fromAmount") fromAmount: BigDecimal,
        @Query("providerId") providerId: Int,
        @Query("rateType") rateType: RateType,
    ): ApiResponse<ExchangeQuoteResponse>

    @GET("exchange-data")
    suspend fun getExchangeData(
        @Query("fromContractAddress") fromContractAddress: String,
        @Query("fromNetwork") fromNetwork: String,
        @Query("toContractAddress") toContractAddress: String,
        @Query("toNetwork") toNetwork: String,
        @Query("fromAmount") fromAmount: BigDecimal,
        @Query("providerId") providerId: Int,
        @Query("rateType") rateType: RateType,
        @Query("toAddress") toAddress: String,
    ): ApiResponse<ExchangeDataResponse>

    @GET("exchange-results")
    suspend fun getExchangeResults(
        @Query("txId") txId: String,
    ): ApiResponse<ExchangeResultsResponse>

}