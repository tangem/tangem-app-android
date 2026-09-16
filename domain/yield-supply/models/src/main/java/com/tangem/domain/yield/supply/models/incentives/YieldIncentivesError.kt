package com.tangem.domain.yield.supply.models.incentives

/** Failure of an incentives request, classified by HTTP status only — never by the backend's error text. */
enum class YieldIncentivesError {

    /** Incentives are not configured for this chain, or the feature is off. Yield Mode stays as it is. */
    FeatureAbsent,

    /** The request failed or the payload could not be read. The incentive surfaces stay hidden. */
    Unavailable,
}