package com.tangem.features.send.v2.subcomponents.fee.model

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType
import com.tangem.utils.extensions.isZero
import java.math.BigDecimal
import java.math.RoundingMode

private val FEE_MAX_DIFF = BigDecimal("5")

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
 * Check if custom fee is too high
 */
internal fun checkIfFeeTooHigh(feeSelectorState: FeeSelectorUM.Content): Pair<Boolean, String> {
    val defaultResult = false to ""
    val multipleFees = feeSelectorState.fees as? TransactionFee.Choosable ?: return defaultResult
    val highValue = multipleFees.priority.amount.value ?: return defaultResult
    val customAmount = feeSelectorState.customValues.firstOrNull() ?: return defaultResult
    val customValue = customAmount.value.parseToBigDecimal(customAmount.decimals)
    val diff = if (highValue > BigDecimal.ZERO) {
        customValue / highValue
    } else {
        BigDecimal.ZERO
    }
    val isFeeTooHigh = feeSelectorState.selectedType == FeeType.Custom && diff > FEE_MAX_DIFF
    return isFeeTooHigh to diff.parseBigDecimal(0, RoundingMode.HALF_UP)
}

/**
 * Check if custom fee is too low
 */
internal fun checkIfFeeTooLow(feeSelectorUM: FeeSelectorUM.Content): Boolean {
    val multipleFees = feeSelectorUM.fees as? TransactionFee.Choosable ?: return false
    val minimumValue = multipleFees.minimum.amount.value ?: return false
    val customAmount = feeSelectorUM.customValues.firstOrNull() ?: return false
    val customValue = customAmount.value.parseToBigDecimal(customAmount.decimals)

    return feeSelectorUM.selectedType == FeeType.Custom && minimumValue > customValue
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

/**
 * Checks if fee exceeds fee paid currency balance
 */
fun checkExceedBalance(feeBalance: BigDecimal?, feeAmount: BigDecimal?): Boolean {
    return feeAmount == null || feeBalance == null || feeAmount.isZero() || feeAmount > feeBalance
}