package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json

/**
 * Shop response
 *
 * @property isOrderingAvailable ordering availability
 *
[REDACTED_AUTHOR]
 */
data class ShopResponse(
    @Json(name = "canOrder") val isOrderingAvailable: Boolean,
)