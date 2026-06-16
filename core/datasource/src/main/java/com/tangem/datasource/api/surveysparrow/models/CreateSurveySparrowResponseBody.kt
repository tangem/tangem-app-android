package com.tangem.datasource.api.surveysparrow.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateSurveySparrowResponseBody(
    @Json(name = "survey_id") val surveyId: Long,
    @Json(name = "answers") val answers: List<SurveySparrowAnswerDto>,
    @Json(name = "variables") val variables: Map<String, String>,
)