package com.tangem.domain.staking.model

sealed class StakingAvailability {

    data class Available(val option: StakingOption) : StakingAvailability()

    /**
     * Integration exists and APY is known, but there is no free capacity (all vaults full).
     * Existing stakes stay visible; new stakes are not offered. P2P ETH only.
     */
    data class Full(val option: StakingOption) : StakingAvailability()

    data object Unavailable : StakingAvailability()

    data object TemporaryUnavailable : StakingAvailability()

    /**
     * The P2P API returned HTTP 451 — staking is blocked in the user's region.
     * New stakes are not offered; an existing stake is shown as a read-only block.
     */
    data object RegionUnavailable : StakingAvailability()
}

/** Staking option if the integration is known (Available or Full), else null. */
val StakingAvailability.optionOrNull: StakingOption?
    get() = when (this) {
        is StakingAvailability.Available -> option
        is StakingAvailability.Full -> option
        StakingAvailability.Unavailable,
        StakingAvailability.TemporaryUnavailable,
        StakingAvailability.RegionUnavailable,
        -> null
    }