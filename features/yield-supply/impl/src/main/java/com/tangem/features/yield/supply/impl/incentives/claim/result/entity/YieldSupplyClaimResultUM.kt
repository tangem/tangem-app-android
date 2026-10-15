package com.tangem.features.yield.supply.impl.incentives.claim.result.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class YieldSupplyClaimResultUM(
    val status: Status,
    val total: TextReference,
    val serviceFee: TextReference?,
    val networkFee: TextReference?,
) {

    @Immutable
    internal sealed interface Status {

        data object Submitted : Status

        /** @property timestamp epoch milliseconds */
        data class Confirmed(val timestamp: Long) : Status

        data object Failed : Status
    }
}