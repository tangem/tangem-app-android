package com.tangem.grow.datasource.stakekit.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
@Suppress("BooleanPropertyNaming")
data class AddressArgumentDTO(
    @Json(name = "required")
    val required: Boolean,
    @Json(name = "network")
    val network: String? = null,
    @Json(name = "minimum")
    val minimum: SerializedBigDecimal? = null,
    @Json(name = "maximum")
    val maximum: SerializedBigDecimal? = null,
)