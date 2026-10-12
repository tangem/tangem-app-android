package com.tangem.domain.yield.supply.models

/** What the chain reports for a broadcast yield transaction, once it has been mined. */
enum class YieldSupplyTransactionOutcome {

    Confirmed,

    Reverted,
}