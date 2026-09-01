package com.tangem.features.txhistory.model

import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.TxInfo

/**
 * Direction-correct counterparty of a transaction: the recipient for an outgoing tx, the sender for an incoming one.
 *
 * Deliberately does not go through [TxInfo.interactionAddressType]: for an unrecognized contract call (Operation /
 * UnknownOperation) that field is always the destination (see `SdkTransactionHistoryItemConverter`), so on an incoming
 * tx it points at the viewed wallet itself. Picking the side that is not the viewed wallet lets the own-portfolio
 * lookup check the real other party. `null` for multi-address sides, where a single counterparty cannot be pinned.
 */
internal fun TxInfo.counterpartyAddress(): String? = if (isOutgoing) {
    (destinationType as? TxInfo.DestinationType.Single)?.addressType?.address
} else {
    (sourceType as? TxInfo.SourceType.Single)?.address
}

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

/**
 * Reclassifies an unrecognized contract call (Operation / UnknownOperation) as a plain [TxInfo.TransactionType.Transfer]
 * when its direction-correct [counterpartyAddress] resolves to one of the user's own accounts/wallets — such a tx is
 * really a transfer between the user's own portfolios and should read as Send / Receive / Transfer, not as a raw
 * operation. [TxInfo.interactionAddressType] is normalized to that counterparty so the downstream transfer rendering
 * (title, counterparty card, subtitle) uses the correct side. Any other tx is returned unchanged.
 */
internal fun TxInfo.reclassifyOwnOperationAsTransfer(
    lookup: TxHistoryLookupContext,
    networkRawId: Network.RawID,
): TxInfo {
    val isUnrecognizedCall = type is TxInfo.TransactionType.Operation ||
        type is TxInfo.TransactionType.UnknownOperation
    if (!isUnrecognizedCall) return this

    val counterparty = counterpartyAddress() ?: return this
    val owner = lookup.resolveOwner(address = counterparty, networkRawId = networkRawId)
    val isOwn = owner is ResolvedOwner.OwnAccount ||
        owner is ResolvedOwner.OwnPaymentAccount ||
        owner is ResolvedOwner.OwnWallet
    return if (isOwn) {
        copy(
            type = TxInfo.TransactionType.Transfer,
            interactionAddressType = TxInfo.InteractionAddressType.User(counterparty),
        )
    } else {
        this
    }
}