package com.tangem.domain.collectibles.models

import java.math.BigDecimal

/**
 * A gacha machine (pack) offered by Collector Crypt.
 *
 * @property externalId stable public identifier (e.g. `pokemon_50`); the address used to fetch a single machine
 * @property contains   number of cards a single pack yields
 */
data class CollectibleGachaMachine(
    val id: String,
    val externalId: String,
    val name: String,
    val shortName: String,
    val price: BigDecimal,
    val fee: BigDecimal,
    val feeAmount: BigDecimal,
    val contains: Int?,
    val instantBuybackPercent: Int,
    val lowThreshold: Int,
    val odds: Odds,
    val tierRanges: TierRanges,
    val stock: Stock,
    val expectedValue: BigDecimal,
    val imageUrl: String?,
    val categoryId: String,
) {

    /** Drop chance per rarity tier, in percent. */
    data class Odds(
        val common: Int,
        val uncommon: Int,
        val rare: Int,
        val epic: Int?,
    )

    /** Remaining card count per rarity tier. */
    data class Stock(
        val common: Int,
        val uncommon: Int,
        val rare: Int,
        val epic: Int?,
    )

    /** Card value range per rarity tier. */
    data class TierRanges(
        val common: TierRange,
        val uncommon: TierRange,
        val rare: TierRange,
        val epic: TierRange?,
    )

    data class TierRange(val start: Int, val end: Int)
}