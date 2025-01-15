package com.tangem.datasource.api.common.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Api environment
 *
 * @author Andrew Khokhlov on 07/08/2024
 */
@JsonClass(generateAdapter = false)
enum class ApiEnvironment {
    @Json(name = "DEV")
    DEV,

    @Json(name = "STAGE")
    STAGE,

    @Json(name = "PROD")
    PROD,
}
