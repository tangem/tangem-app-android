package com.tangem.features.staking.impl.presentation.state.utils

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import java.math.BigDecimal

/**
 * Check and calculates subtracted amount
 */
internal fun checkAndCalculateSubtractedAmount(
    isAmountSubtractAvailable: Boolean,
    cryptoCurrencyStatus: CryptoCurrencyStatus,
    amountValue: BigDecimal,
    feeValue: BigDecimal,
    reduceAmountBy: BigDecimal,
): BigDecimal {
    val balance = cryptoCurrencyStatus.value.amount ?: return amountValue
    val isFeeCoverage = checkFeeCoverage(
        isSubtractAvailable = isAmountSubtractAvailable,
        balance = balance,
        amountValue = amountValue,
        feeValue = feeValue,
        reduceAmountBy = reduceAmountBy,
    )
    return if (isFeeCoverage) {
        balance.minus(reduceAmountBy).minus(feeValue)
    } else {
        amountValue
    }
}

/**
 * Checks if sending amount with fee is greater than balance
 */
internal fun checkFeeCoverage(
    isSubtractAvailable: Boolean,
    balance: BigDecimal,
    amountValue: BigDecimal,
    feeValue: BigDecimal,
    reduceAmountBy: BigDecimal?,
): Boolean {
    if (!isSubtractAvailable) return false
    val reducedBy = balance - (reduceAmountBy ?: BigDecimal.ZERO)
    return reducedBy < amountValue + feeValue && reducedBy > feeValue && reducedBy >= amountValue
}