package com.tangem.domain.tokens.model

enum class ScenarioUnavailabilityReason {
    NONE,

    // send-specific
    PENDING_TRANSACTION,
    EMPTY_BALANCE,
    INSUFFICIENT_FUNDS_FOR_FEE, // if token

    // buy-specific
    BUY_UNAVAILABLE,

    //swap-specific
    NOT_EXCHANGEABLE,

    //sell-specific
    SELL_UNAVAILABLE,

    GENERAL_ERROR,
}