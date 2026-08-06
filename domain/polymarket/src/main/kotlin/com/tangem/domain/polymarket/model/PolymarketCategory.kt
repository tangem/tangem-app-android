package com.tangem.domain.polymarket.model

/**
 * A Discovery feed category shown as a tab (BFF `Category`).
 *
 * @property id category id used to filter the events feed (`GET /events?category=<id>`)
 * @property label localized display name
 * @property iconUrl optional category icon
 */
data class PolymarketCategory(
    val id: Int,
    val label: String,
    val iconUrl: String?,
)