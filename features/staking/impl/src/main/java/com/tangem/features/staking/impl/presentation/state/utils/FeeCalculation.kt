package com.tangem.features.staking.impl.presentation.state.utils

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.lib.crypto.BlockchainUtils.isTron
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

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
    val isTron = isTron(cryptoCurrencyStatus.currency.network.rawId)
    val feeValueRounded = if (isTron) {
        feeValue.round(MathContext(0, RoundingMode.UP))
    } else {
        feeValue
    }
    val isFeeCoverage = checkFeeCoverage(
        isSubtractAvailable = isAmountSubtractAvailable,
        balance = balance,
        amountValue = amountValue,
        feeValue = feeValueRounded,
        reduceAmountBy = reduceAmountBy,
    )
    return if (isFeeCoverage) {
        val reducedAmount = balance.minus(reduceAmountBy).minus(feeValueRounded)
        if (isTron(cryptoCurrencyStatus.currency.network.rawId)) {
            reducedAmount.setScale(0, RoundingMode.DOWN)
        } else {
            reducedAmount
        }
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