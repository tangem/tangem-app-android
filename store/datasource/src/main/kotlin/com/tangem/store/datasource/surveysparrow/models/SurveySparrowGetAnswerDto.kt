package com.tangem.store.datasource.surveysparrow.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Any? is required because the API returns question_id as Long for survey questions but as String
// ("startTime", "submittedTime", etc.) for metadata answers, and answer as Int for ratings but
// as String for other answer types.
@JsonClass(generateAdapter = true)
data class SurveySparrowGetAnswerDto(
    @Json(name = "question_id") val questionId: Any?,
    @Json(name = "answer") val answer: Any?,
)