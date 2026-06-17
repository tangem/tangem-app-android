package com.tangem.domain.express.models

import java.math.BigDecimal

/**
 * A crypto asset leg of an express operation: which asset and how much of it moved.
 *
 * @property id The asset identifier (network id + contract address).
 * @property amount Human-readable amount (already scaled by [decimals]).
 * @property decimals The asset's decimals.
 */
data class ExpressTransactionAsset(
    val id: ExpressAsset.ID,
    val amount: BigDecimal,
    val decimals: Int,
)