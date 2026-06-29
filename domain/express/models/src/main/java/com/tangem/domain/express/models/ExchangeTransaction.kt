package com.tangem.domain.express.models

/**
 * An express exchange (swap) operation, independent of how it is presented in the transaction history.
 *
 * Minimal set for now; extend as more of the express deal is needed.
 *
 * @property txId The express operation id.
 * @property status The current exchange status.

 * @property provider The provider behind the deal; `null` if not resolved.
 * @property payinHash On-chain hash of the pay-in (from-side) leg, if known.
 * @property payoutHash On-chain hash of the payout (to-side) leg, if known.
 * @property fromAddress Address the `from` assets were taken from (the user's own source address); `null` when unknown
 *  (very old app versions did not send it).
 * @property payoutAddress Address that received the `to` assets — the user's own address for a regular swap, an external
 *  one for a send-and-swap; `null` when unknown.
 * @property fromAsset The asset sent.
 * @property toAsset The asset received.
 * @property externalTxUrl The provider's page for this deal (tracking / refund / KYC); `null` when the provider
 *  supplies none (CEX only).
 */
data class ExchangeTransaction(
    val txId: String,
    val status: ExpressExchangeStatus,
    val createdAtMillis: Long,
    val provider: ExpressProvider?,
    val payinHash: String?,
    val payoutHash: String?,
    val fromAddress: String?,
    val payoutAddress: String?,
    val fromAsset: ExpressTransactionAsset,
    val toAsset: ExpressTransactionAsset,
    val externalTxUrl: String? = null,
)