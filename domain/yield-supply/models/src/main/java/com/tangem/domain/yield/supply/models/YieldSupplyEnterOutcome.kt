package com.tangem.domain.yield.supply.models

/**
 * What became of a broadcast yield "enter" transaction, once it settled on chain.
 *
 * Broadcasting is not the same as depositing: `enterProtocolByOwner` deposits whatever the owner address holds
 * at *mining* time, so it reverts with `ZeroAmount()` if anything else moved that balance first — most often
 * the backend relayer running the same operation.
 */
enum class YieldSupplyEnterOutcome {

    /** The transaction succeeded and the funds are in the protocol. */
    Deposited,

    /**
     * The transaction reverted, but the funds reached the protocol anyway because the relayer deposited them
     * first. The end state is the one the user asked for, so this is not a failure to report.
     */
    DepositedByRelayer,

    /** The transaction reverted and the funds are still sitting on the owner address. */
    Failed,
}