package com.tangem.features.txhistory.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo

/** A counterparty the details screen can offer to save to the address book, with the network its entry belongs to. */
internal data class ContactCandidate(val address: String, val networkId: Network.RawID)

/**
 * The counterparty of this transaction that is worth offering to save as a contact, or `null` when there is nothing to
 * offer. Drives the "Add contact" row of the details header menu.
 *
 * A candidate must be a real counterparty the user chose:
 *  - a plain transfer (Send / Receive), so a contract, validator or protocol address is never offered;
 *  - or the payout leg of an **outgoing swap**, which is a real recipient only in a send-with-swap — a plain swap pays
 *    out to the user's own address and is filtered out by the own-portfolio check below;
 *  - none of the user's own portfolios (there is nothing to name about one's own address);
 *  - not already in the address book of the wallet the transaction belongs to — which is where the contact would be

 */
internal fun TxHistoryInfo.contactCandidate(
    currency: CryptoCurrency,
    lookup: TxHistoryLookupContext,
): ContactCandidate? {
    val candidate = when (this) {
        is OnChainTx.BSDK -> txInfo.transferCandidate(networkId = currency.network.id.rawId)
        is ExpressTx.Swap -> swapPayoutCandidate()
        is OnChainTx.TangemPay, is ExpressTx.Onramp -> null
    } ?: return null

    return candidate.takeIf { lookup.isSavableAsContact(it) }
}

/**
 * The recipient (outgoing) or sender (incoming) of a plain transfer. [TxInfo.interactionAddressType] already carries
 * the correct side for a transfer, so it is read directly — the reclassification exception that applies to contract
 * calls cannot be reached from here.
 */
private fun TxInfo.transferCandidate(networkId: Network.RawID): ContactCandidate? {
    if (!isPlainTransfer()) return null
    val address = (interactionAddressType as? TxInfo.InteractionAddressType.User)?.address ?: return null

    return ContactCandidate(address = address, networkId = networkId)
}

/**
 * The payout leg of an outgoing swap, on the network it pays out on (not the viewed token's network). `null` for an
 * incoming swap — that payout is the user's own — or while the leg's currency is still unresolved, since an address

 */
private fun ExpressTx.Swap.swapPayoutCandidate(): ContactCandidate? {
    if (!isOutgoing) return null
    val payoutCurrency = tx.toAsset.cryptoCurrency ?: return null

    return ContactCandidate(address = tx.payoutAddress, networkId = payoutCurrency.network.id.rawId)
}

/** A plain value transfer: an ordinary Send / Receive, or the non-withdraw yield Send that is one in disguise. */
internal fun TxInfo.isPlainTransfer(): Boolean = type is TxInfo.TransactionType.Transfer ||
    (type as? TxInfo.TransactionType.YieldSupply.Send)?.isYieldSupplyWithdraw == false

/**
 * Whether [candidate] is a foreign address that the transaction's wallet has not saved yet. `false` without a

 * Shared with the joint operation sheet, whose recipient comes from the operation payload rather than a [TxHistoryInfo].
 */
internal fun TxHistoryLookupContext.isSavableAsContact(candidate: ContactCandidate): Boolean {
    val walletId = viewedWalletId ?: return false
    val owner = resolveOwner(address = candidate.address, networkRawId = candidate.networkId)
    if (owner !is ResolvedOwner.Foreign) return false

    return !contacts.isSavedIn(walletId = walletId, networkId = candidate.networkId, address = candidate.address)
}