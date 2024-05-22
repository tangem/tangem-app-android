package com.tangem.datasource.api.stakekit.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YieldArgs(
    @Json(name = "enter")
    val enter: Enter,
    @Json(name = "exit")
    val exit: Enter?,
)