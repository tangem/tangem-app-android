package com.tangem.domain.express.models

import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigDecimal

/**
 * A crypto asset leg of an express operation: which asset and how much of it moved.
 *
 * @property id The asset identifier (network id + contract address).
 * @property amount Human-readable amount (already scaled by [decimals]).
 * @property decimals The asset's decimals.
 * @property cryptoCurrency The portfolio [CryptoCurrency] this asset was resolved to (matched by network id +
 * contract address across all accounts). `null` when no portfolio currency matched and no fallback could be built.
 */
data class ExpressTransactionAsset(
    val id: ExpressAsset.ID,
    val amount: BigDecimal,
    val decimals: Int,
    val cryptoCurrency: CryptoCurrency? = null,
)