package com.tangem.domain.transaction.models

import java.math.BigDecimal

sealed class AssetRequirementsCondition {

    /**
     * The exact value of the fee for this type of condition is unknown.
     */
    data object PaidTransaction : AssetRequirementsCondition()

    /**
     * The exact value of the fee for this type of condition is stored in `feeAmount`.
     */
    data class PaidTransactionWithFee(
        val feeAmount: BigDecimal,
        val feeCurrencySymbol: String,
        val decimals: Int,
    ) : AssetRequirementsCondition()
}
