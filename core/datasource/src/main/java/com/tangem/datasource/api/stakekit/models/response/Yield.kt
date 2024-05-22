package com.tangem.datasource.api.stakekit.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Yield(
    @Json(name = "id")
    val id: String,
    @Json(name = "token")
    val token: Token,
    @Json(name = "tokens")
    val tokens: List<Token>,
    @Json(name = "args")
    val args: YieldArgs,
    @Json(name = "status")
    val status: Status,
    @Json(name = "apy")
    val apy: Double,
    @Json(name = "rewardRate")
    val rewardRate: Double,
    @Json(name = "rewardType")
    val rewardType: String,
    @Json(name = "metadata")
    val metadata: Metadata,
    @Json(name = "validators")
    val validators: List<Validator>,
    @Json(name = "isAvailable")
    val isAvailable: Boolean,
)