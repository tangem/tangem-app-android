package com.tangem.datasource.api.surveysparrow.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SurveySparrowAnswerDto(
    @Json(name = "question_id") val questionId: Long,
    @Json(name = "answer") val answer: String? = null,
)