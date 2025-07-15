package com.tangem.datasource.api.common.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Api environment
 *
[REDACTED_AUTHOR]
 */
@JsonClass(generateAdapter = false)
enum class ApiEnvironment {
    @Json(name = "DEV")
    DEV,

    @Json(name = "DEV_2")
    DEV_2,

    @Json(name = "STAGE")
    STAGE,

    @Json(name = "MOCK")
    MOCK,

    @Json(name = "PROD")
    PROD,
}