package com.tangem.grow.datasource.collectibles.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for GET /api/v1/geoblock
 *
 * The backend also returns `countryCode` and `ipAddress`; the app decides by [isBlocked] alone and does not keep them.
 */
@Serializable
data class GeoblockResponse(
    @SerialName("blocked")
    val isBlocked: Boolean,
)