package com.tangem.datasource.api.stakekit.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class AddressArgumentDTO(
    @Json(name = "required")
    val required: Boolean,
    @Json(name = "network")
    val network: String? = null,
    @Json(name = "minimum")
    val minimum: BigDecimal? = null,
    @Json(name = "maximum")
    val maximum: BigDecimal? = null,
)