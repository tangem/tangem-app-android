package com.tangem.features.yield.supply.impl.incentives.claim.result.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/**
 * Outcome of a claim the user has already signed.
 *
 * The fees are the ones captured at signing rather than re-read afterwards: the sheet that estimated them is gone
 * by the time the transaction settles, and a second estimate would quote a different block.
 */
@Immutable
internal data class YieldSupplyClaimResultUM(
    val status: Status,
    val total: TextReference,
    val serviceFee: TextReference?,
    val networkFee: TextReference?,
) {

    @Immutable
    internal sealed interface Status {

        /** Submitted and not yet mined. */
        data object Submitted : Status

        /**
         * Mined successfully. Merkl still has to process the claim, so the rewards banner lags behind this by a
         * few minutes — which is what the screen says.
         *
         * @property timestamp when the confirmation was observed, in epoch milliseconds
         */
        data class Confirmed(val timestamp: Long) : Status
    }
}