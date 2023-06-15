package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json

/**
 * Shop response
 *
 * @property isOrderingAvailable ordering availability
 *
 * @author Andrew Khokhlov on 15/06/2023
 */
data class ShopResponse(
    @Json(name = "canOrder") val isOrderingAvailable: Boolean,
)
