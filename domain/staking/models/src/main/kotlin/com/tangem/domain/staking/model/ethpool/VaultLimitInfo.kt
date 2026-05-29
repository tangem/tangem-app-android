package com.tangem.domain.staking.model.ethpool

import java.math.BigDecimal

/**
 * Per-vault capacity limits from Tangem API /v1/coins/settings.
 *
 * @property limit max stakeable amount in ETH (pre-computed as MAX_Threshold - TVL).
 *   Vaults absent from the API response or with null limit are not stored.
 * @property coefficient threshold multiplier (e.g. 1.25×); optional server-side field,
 *   reserved for future use, not used in client-side calculations
 */
data class VaultLimitInfo(
    val limit: BigDecimal,
    val coefficient: BigDecimal?,
)