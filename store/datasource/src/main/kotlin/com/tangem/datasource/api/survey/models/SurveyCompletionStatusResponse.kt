package com.tangem.datasource.api.survey.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SurveyCompletionStatusResponse(
    @Json(name = "completed") val isCompleted: Boolean,
)