package com.tangem.domain.express.models

import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType

/**
 * An express onramp operation, independent of how it is presented in the transaction history.
 *
 * Minimal set for now; extend as more of the express deal is needed.
 *
 * @property txId The express operation id.
 * @property status The current onramp status.

 * @property provider The provider behind the deal; `null` if not resolved.
 * @property payoutHash On-chain hash of the payout (received) leg, if known.
 * @property fromFiat The fiat paid.
 * @property toAsset The crypto asset received.
 */
data class OnrampTransaction(
    val txId: String,
    val status: ExpressOnrampStatus,
    val createdAtMillis: Long,
    val provider: ExpressProvider?,
    val payoutHash: String?,
    /** The [Amount.type] is [AmountType.FiatType] . */
    val fromFiat: Amount,
    val toAsset: ExpressTransactionAsset,
)