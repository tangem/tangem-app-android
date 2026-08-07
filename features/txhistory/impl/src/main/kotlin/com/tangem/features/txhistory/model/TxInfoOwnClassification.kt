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