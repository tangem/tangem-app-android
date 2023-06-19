package com.tangem.datasource.api.promotion

import com.tangem.datasource.api.promotion.models.*
import retrofit2.http.*

/**
[REDACTED_AUTHOR]
 * Api details
 * https://www.notion.so/tangem/Promotion-Program-API-0907159c3fdb4975aac761be632f44da
 */
interface PromotionApi {

    
    @Headers("Cache-Control: max-age=$CACHE_CONTROL_SECONDS")
    @GET("promotion")
    suspend fun getPromotionInfo(@Query("programName") name: String): PromotionInfoResponse

    
    @POST("promotion/code/validate")
    suspend fun codeValidate(@Body request: CodeValidateRequestBody): CodeValidateResponse

    
    @POST("promotion/code/award")
    suspend fun codeAward(@Body request: CodeAwardRequestBody): CodeAwardResponse

    
    @POST("promotion/validate")
    suspend fun validate(@Body request: ValidateRequestBody): ValidateResponse

    
    @GET("promotion/award")
    suspend fun award(@Body request: AwardRequestBody): AwardResponse

    companion object {
        const val ONE_INCH_TIMEOUT_MS = 5000L
        const val CACHE_CONTROL_SECONDS = 3600
    }
}