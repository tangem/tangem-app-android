package com.tangem.store.datasource.surveysparrow.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SurveySparrowQuestionsDto(
    @Json(name = "data")
    val questions: List<SurveySparrowQuestionDto>,
)