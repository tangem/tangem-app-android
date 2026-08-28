package com.tangem.domain.collectibles.models

/**
 * One page of machines.
 *
 * @property cursor opaque keyset token for the next page, `null` when there is nothing to continue from
 */
data class CollectibleGachaMachinesPage(
    val collectibleGachaMachines: List<CollectibleGachaMachine>,
    val cursor: String?,
    val hasNext: Boolean,
)

/**
 * One loaded batch of machines.
 *
 * @property requestCursor the cursor this batch was requested with, so the same page can be re-fetched
 */
data class CollectibleGachaMachinesBatch(
    val collectibleGachaMachines: List<CollectibleGachaMachine>,
    val requestCursor: String?,
)

data class CollectibleGachaMachinesListConfig(
    val categoryId: String? = null,
    val search: String? = null,
)