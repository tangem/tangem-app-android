package com.tangem.domain.polymarket.model

/**
 * A Discovery feed category shown as a tab (BFF `Category`).
 *
 * @property id category id used to filter the events feed (`GET /events?category=<id>`)
 * @property label localized display name
 * @property icon icon named by the BFF `Category.icon` key, `null` when the BFF sends none or a key this build does not know
 */
data class PolymarketCategory(
    val id: Int,
    val label: String,
    val icon: PolymarketCategoryIcon?,
)