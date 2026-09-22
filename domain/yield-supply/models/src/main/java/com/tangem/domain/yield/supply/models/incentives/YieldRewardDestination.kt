package com.tangem.domain.yield.supply.models.incentives

/** On-chain destination a reward is routed to once claimed. */
enum class YieldRewardDestination {
    Owner,
    Aave,
    Module,

    /** The backend reported a destination this app version does not know — treat the reward as not claimable. */
    Unsupported,
}