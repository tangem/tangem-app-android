package com.tangem.features.txhistory.model

import com.tangem.domain.models.network.TxInfo

/**
 * Whether the transaction could be an interaction with a staking target, and so is worth resolving one for.
 *
 * Covers both a recognized staking type and an **unrecognized contract call**: whether a staking call lands on a
 * `Staking` type depends on its 4-byte selector being registered in `contract_methods.json`, and not all are — a P2P
 * vault deposit/unstake is (`pooledStake` / `pooledUnstake`), but e.g. its exit-queue withdrawal is not, so it arrives
 * as Operation / UnknownOperation. Since a target is only ever resolved by the address actually being a known
 * validator/vault, admitting unrecognized calls costs nothing and stops an unregistered selector from silently hiding
 * the row. Plain transfers, swaps, approvals and yield-supply (which has its own protocol row) are excluded.
 */
internal fun TxInfo.mayCarryStakingTarget(): Boolean = when (type) {
    is TxInfo.TransactionType.Staking,
    is TxInfo.TransactionType.Operation,
    is TxInfo.TransactionType.UnknownOperation,
    -> true
    else -> false
}