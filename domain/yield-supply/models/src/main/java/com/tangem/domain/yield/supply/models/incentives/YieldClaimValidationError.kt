package com.tangem.domain.yield.supply.models.incentives

/**
 * Reason a fresh claim payload was rejected before signing.
 *
 * Every reason is terminal for the current payload: the app refreshes the incentives and never patches the rejected
 * calldata into something it would sign.
 */
enum class YieldClaimValidationError {

    /** The backend reports nothing claimable, so there is no transaction to build. */
    NothingToClaim,

    /** Claiming now would cost more than it returns, by the backend's own verdict. */
    NotRecommended,

    /** The module, target or payload is missing, so the transaction cannot be assembled. */
    IncompleteCalldata,

    /** The payload was built for another chain or another wallet than the one in front of the user. */
    ContextMismatch,

    /** The transaction targets something other than the user's own yield module. */
    DestinationMismatch,

    /** The module address could not be derived on chain, so there is nothing independent to pin the claim against. */
    ModuleAddressUnavailable,

    /** Reward tokens and cumulative amounts do not line up, so the proof cannot be trusted. */
    MalformedProof,

    /** The payload pays out a reward this app version cannot route. */
    UnsupportedRewardToken,
}