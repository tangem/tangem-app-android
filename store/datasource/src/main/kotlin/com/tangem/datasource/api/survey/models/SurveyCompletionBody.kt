package com.tangem.datasource.api.survey.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SurveyCompletionBody(
    @Json(name = "surveyId") val surveyId: String,
    @Json(name = "answers") val answers: List<SurveyAnswerDto>,
)

@JsonClass(generateAdapter = true)
data class SurveyAnswerDto(
    @Json(name = "question") val question: String,
    @Json(name = "answer") val answer: String,
)