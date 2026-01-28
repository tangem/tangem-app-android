package com.tangem.domain.yield.supply.models

/**
 * Represents the availability of yield supply for a given asset.
 */
sealed class YieldSupplyAvailability {
    data class Available(val apy: String) : YieldSupplyAvailability()

    data object Unavailable : YieldSupplyAvailability()
}