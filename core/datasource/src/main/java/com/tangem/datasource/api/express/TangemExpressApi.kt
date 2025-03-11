package com.tangem.datasource.api.express

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.express.models.request.AssetsRequestBody
import com.tangem.datasource.api.express.models.request.ExchangeSentRequestBody
import com.tangem.datasource.api.express.models.request.PairsRequestBody
import com.tangem.datasource.api.express.models.response.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Interface of Tangem Express API (new swap mechanism)
 */
@Suppress("LongParameterList")
interface TangemExpressApi {

    @POST("assets")
    suspend fun getAssets(@Body body: AssetsRequestBody): ApiResponse<List<Asset>>

    @POST("pairs")
    suspend fun getPairs(@Body body: PairsRequestBody): ApiResponse<List<SwapPair>>

    @GET("providers")
    suspend fun getProviders(): ApiResponse<List<ExchangeProvider>>

    @GET("exchange-quote")
    suspend fun getExchangeQuote(
        @Query("fromContractAddress") fromContractAddress: String,
        @Query("fromNetwork") fromNetwork: String,
        @Query("toContractAddress") toContractAddress: String,
        @Query("toNetwork") toNetwork: String,
        @Query("fromAmount") fromAmount: String,
        @Query("fromDecimals") fromDecimals: Int,
        @Query("toDecimals") toDecimals: Int,
        @Query("providerId") providerId: String,
        @Query("rateType") rateType: String,
    ): ApiResponse<ExchangeQuoteResponse>

    @GET("exchange-data")
    suspend fun getExchangeData(
        @Query("fromContractAddress") fromContractAddress: String,
        @Query("fromNetwork") fromNetwork: String,
        @Query("toContractAddress") toContractAddress: String,
        @Query("fromAddress") fromAddress: String,
        @Query("toNetwork") toNetwork: String,
        @Query("fromAmount") fromAmount: String,
        @Query("fromDecimals") fromDecimals: Int,
        @Query("toDecimals") toDecimals: Int,
        @Query("providerId") providerId: String,
        @Query("rateType") rateType: String,
        @Query("toAddress") toAddress: String,
        @Query("requestId") requestId: String,
        @Query("refundAddress") refundAddress: String?, // for cex only
        @Query("refundExtraId") refundExtraId: String?, // for cex only
    ): ApiResponse<ExchangeDataResponse>

    @GET("exchange-status")
    suspend fun getExchangeStatus(@Query("txId") txId: String): ApiResponse<ExchangeStatusResponse>

    @POST("exchange-sent")
    suspend fun exchangeSent(@Body body: ExchangeSentRequestBody): ApiResponse<ExchangeSentResponseBody>
}