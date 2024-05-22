package com.tangem.datasource.api.stakekit.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Validator(
    @Json(name = "address")
    val address: String,
    @Json(name = "status")
    val status: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "image")
    val image: String?,
    @Json(name = "website")
    val website: String?,
    @Json(name = "apr")
    val apr: Double?,
    @Json(name = "commission")
    val commission: Double?,
    @Json(name = "stakedBalance")
    val stakedBalance: String?,
    @Json(name = "votingPower")
    val votingPower: Double?,
    @Json(name = "preferred")
    val preferred: Boolean,
)
