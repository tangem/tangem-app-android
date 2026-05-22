package com.tangem.datasource.api.surveysparrow

import com.tangem.datasource.api.surveysparrow.models.CreateSurveySparrowResponseBody
import com.tangem.datasource.api.surveysparrow.models.SurveySparrowResponsesDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SurveySparrowApi {

    @GET("v3/responses")
    suspend fun getResponses(
        @Query("survey_id") surveyId: Long,
        @Query("variables") variables: String,
        @Query("limit") limit: Int = 1,
    ): SurveySparrowResponsesDto

    @POST("v3/responses")
    suspend fun createResponse(@Body body: CreateSurveySparrowResponseBody)
}