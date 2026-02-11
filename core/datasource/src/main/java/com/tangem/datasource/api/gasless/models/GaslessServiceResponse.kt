package com.tangem.datasource.api.gasless.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GaslessServiceResponse<T>(
    @Json(name = "result") val result: T,
    @Json(name = "success") val isSuccess: Boolean,
    @Json(name = "timestamp") val timestamp: String,
)