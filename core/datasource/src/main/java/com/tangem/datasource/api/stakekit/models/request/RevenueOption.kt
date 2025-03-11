package com.tangem.datasource.api.stakekit.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class RevenueOption(val value: String) {
    @Json(name = "SUPPORTS_FEE") SUPPORTS_FEE("supportsFee"),

    @Json(name = "SUPPORTS_REV_SHARE") SUPPORTS_REV_SHARE("supportsRevShare"),
    ;

    override fun toString(): String {
        return value
    }
}