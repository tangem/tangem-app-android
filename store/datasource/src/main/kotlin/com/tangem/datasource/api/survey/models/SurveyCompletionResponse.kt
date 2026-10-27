package com.tangem.datasource.api.survey.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SurveyCompletionResponse(
    @Json(name = "status") val status: String,
)