package com.tangem.domain.models.yield.supply

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Represents the status of yield supply for a cryptocurrency asset.
 *
 * This data class encapsulates the current state of yield supply, including whether it is active,
 * initialized, and allowed to spend.
 *
 * @property isActive           Indicates if the yield token is currently active.
 * @property isInitialized      Indicates if the yield token has been initialized.
 * @property isAllowedToSpend   Indicates if spending from the yield module is permitted.
 * @property effectiveProtocolBalance    Indicates the balance (excluding service fee)
 * */
@Serializable
data class YieldSupplyStatus(
    val isActive: Boolean,
    val isInitialized: Boolean,
    val isAllowedToSpend: Boolean,
    val effectiveProtocolBalance: SerializedBigDecimal?,
)