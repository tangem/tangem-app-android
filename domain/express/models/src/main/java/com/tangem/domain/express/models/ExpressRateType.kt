package com.tangem.domain.express.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Rate type.
 *
 * Current implementation contains only float type, fixed will be supported later.
 */
@JsonClass(generateAdapter = false)
enum class ExpressRateType {
    @Json(name = "FLOAT")
    Float,

    @Json(name = "FIXED")
    Fixed,
}