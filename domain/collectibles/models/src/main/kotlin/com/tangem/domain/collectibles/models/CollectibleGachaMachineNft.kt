package com.tangem.domain.collectibles.models

import java.math.BigDecimal

data class CollectibleGachaMachineNftsListConfig(val externalId: String)

/**
 * One page of a machine's prize pool.
 *
 * @property page 1-based index of this page, as echoed back by the backend
 */
data class CollectibleGachaMachineNftsPage(
    val nfts: List<CollectibleGachaMachineNft>,
    val page: Int,
    val hasMore: Boolean,
)

/**
 * A collectible sitting in a machine's prize pool.
 *
 * @property insuredValue appraised value in whole currency units. The wire format is cents, so the
 *   data layer divides by 100 — see `MachineNftConverter`.
 * @property rarity `null` when the backend sends no rarity, or one this version does not know.
 * @property grade grader and grade joined for display (e.g. `PSA 10`); `null` when the collectible is ungraded.
 */
data class CollectibleGachaMachineNft(
    val id: String,
    val nftAddress: String,
    val name: String,
    val description: String?,
    val rarity: CollectibleRarity?,
    val grade: Grade?,
    val imageUrl: String?,
    val insuredValue: BigDecimal?,
) {

    data class Grade(val company: String, val value: String)
}