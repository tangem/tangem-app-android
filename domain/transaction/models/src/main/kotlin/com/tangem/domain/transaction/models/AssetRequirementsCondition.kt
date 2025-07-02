package com.tangem.domain.transaction.models

import java.math.BigDecimal

sealed class AssetRequirementsCondition {

    /**
     * The exact value of the fee for this type of condition is unknown.
     */
    data object PaidTransaction : AssetRequirementsCondition()

    /**
     * Trustlines are an explicit opt-in for an account to hold a particular asset.
     */
    data class RequiredTrustline(
        val requiredAmount: BigDecimal,
        val currencySymbol: String,
        val decimals: Int,
    ) : AssetRequirementsCondition()

    /**
     * The exact value of the fee for this type of condition is stored in `feeAmount`.
     */
    data class PaidTransactionWithFee(
        val feeAmount: BigDecimal,
        val feeCurrencySymbol: String,
        val decimals: Int,
    ) : AssetRequirementsCondition()

    /**
     * The exact value of the fee for this type of condition is stored in `feeAmount`.
     */
    data class IncompleteTransaction(
        val amount: BigDecimal,
        val currencySymbol: String,
        val currencyDecimals: Int,
        val feeAmount: BigDecimal,
        val feeCurrencySymbol: String,
        val feeCurrencyDecimals: Int,
    ) : AssetRequirementsCondition()
}