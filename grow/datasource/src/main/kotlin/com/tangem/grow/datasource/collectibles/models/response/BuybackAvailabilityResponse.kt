package com.tangem.grow.datasource.collectibles.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for GET /api/v1/buyback/available
 *
 * @property amount the buyback price in USD cents
 * @property expiresAt ISO-8601 instant the offer ends at
 */
@Serializable
data class BuybackAvailabilityResponse(
    @SerialName("buybackAvailable")
    val isAvailable: Boolean,
    @SerialName("buybackAmount")
    val amount: Long? = null,
    @SerialName("buybackExpiresAt")
    val expiresAt: String? = null,
)