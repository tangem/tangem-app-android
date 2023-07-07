package com.tangem.datasource.api.promotion

import com.tangem.datasource.api.promotion.models.*
import retrofit2.http.*

/**
 *
 * Promotion API
 * @see <a href = "https://www.notion.so/tangem/Promotion-Program-API-0907159c3fdb4975aac761be632f44da">Documentation<a/>
* [REDACTED_AUTHOR]
 */
interface PromotionApi {

    @Headers("Cache-Control: max-age=3600")
    @GET("promotion")
    suspend fun getPromotionInfo(@Query("programName") name: String): PromotionInfoResponse

    @POST("promotion/code/validate")
    suspend fun validateCode(@Body request: CodeValidateRequestBody): CodeValidateResponse

    @POST("promotion/code/award")
    suspend fun requestAwardByCode(@Body request: CodeAwardRequestBody): CodeAwardResponse

    @POST("promotion/validate")
    suspend fun validate(@Body request: ValidateRequestBody): ValidateResponse

    @POST("promotion/award")
    suspend fun requestAward(@Body request: AwardRequestBody): AwardResponse
}
