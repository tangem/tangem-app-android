package com.tangem.datasource.api.stakekit.models.response.model.transaction

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class StakingTransactionStatusDTO {
    @Json(name = "NOT_FOUND")
    NOT_FOUND,

    @Json(name = "CREATED")
    CREATED,

    @Json(name = "BLOCKED")
    BLOCKED,

    @Json(name = "WAITING_FOR_SIGNATURE")
    WAITING_FOR_SIGNATURE,

    @Json(name = "SIGNED")
    SIGNED,

    @Json(name = "BROADCASTED")
    BROADCASTED,

    @Json(name = "PENDING")
    PENDING,

    @Json(name = "CONFIRMED")
    CONFIRMED,

    @Json(name = "FAILED")
    FAILED,

    @Json(name = "SKIPPED")
    SKIPPED,

    UNKNOWN,
}