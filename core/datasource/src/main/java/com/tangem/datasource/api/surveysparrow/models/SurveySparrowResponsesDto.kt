package com.tangem.datasource.api.surveysparrow.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SurveySparrowResponsesDto(
    @Json(name = "data") val data: List<SurveySparrowResponseDto>,
)