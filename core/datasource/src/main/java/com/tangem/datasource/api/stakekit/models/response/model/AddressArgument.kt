package com.tangem.datasource.api.stakekit.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AddressArgument(
    @Json(name = "required")
    val required: Boolean,
    @Json(name = "network")
    val network: String? = null,
    @Json(name = "minimum")
    val minimum: Int? = null,
    @Json(name = "maximum")
    val maximum: Int? = null,
)