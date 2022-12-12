package com.tangem.datasource.api.oneinch.models

import com.squareup.moshi.Json

/**
 * Path view dto
 */
data class PathViewDto(
    @Json(name = "name") val name: String,
    @Json(name = "part") val part: Int,
    @Json(name = "fromTokenAddress") val fromTokenAddress: String,
    @Json(name = "toTokenAddress") val toTokenAddress: String,
)
