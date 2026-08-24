package com.tangem.domain.markets

/**
 * Coin category — a curated group of coins, optionally split into sectors
 *
 * @property id           category id
 * @property code         stable machine-readable key, e.g. `stocks`, `commodities`
 * @property displayName  name to show when no localized name is available
 * @property tokensCount  total number of coins in the category
 * @property isRestricted category is region-restricted: displayable, but its coins must not be actionable
 * @property sectors      sub-groups of the category; empty when the category has none
 *
[REDACTED_AUTHOR]
 */
data class CoinCategory(
    val id: String,
    val code: String,
    val displayName: String,
    val displayOrder: Int,
    val tokensCount: Int,
    val isRestricted: Boolean,
    val sectors: List<Sector>,
    val isVisible: Boolean,
) {

    /**
     * Sector of a [CoinCategory]
     *
     * @property id          sector id, e.g. `technology`
     * @property displayName name to show when no localized name is available
     * @property tokensCount number of coins in the sector
     */
    data class Sector(
        val id: String,
        val displayName: String,
        val tokensCount: Int,
    )
}