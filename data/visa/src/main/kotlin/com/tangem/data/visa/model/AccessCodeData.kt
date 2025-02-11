package com.tangem.data.visa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AccessCodeData(
    @Json(name = "pid") val productInstanceId: String,
    @Json(name = "sub") val customerId: String,
)