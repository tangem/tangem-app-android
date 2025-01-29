package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Shop response
 *
 * @property isOrderingAvailable ordering availability
 *
[REDACTED_AUTHOR]
 */
@JsonClass(generateAdapter = true)
data class ShopResponse(
    @Json(name = "canOrder") val isOrderingAvailable: Boolean,
)