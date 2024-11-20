package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.extensions.isZero
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import java.math.BigDecimal
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

/**
 * Check if custom fee is too low
 */
internal fun checkIfFeeTooLow(feeSelectorState: FeeSelectorState.Content): Boolean {
    val multipleFees = feeSelectorState.fees as? TransactionFee.Choosable ?: return false
    val minimumValue = multipleFees.minimum.amount.value ?: return false
    val customAmount = feeSelectorState.customValues.firstOrNull() ?: return false
    val customValue = customAmount.value.parseToBigDecimal(customAmount.decimals)

    return feeSelectorState.selectedFee == FeeType.Custom && minimumValue > customValue
}

/**
 * Check if custom fee is too high
 */
internal fun checkIfFeeTooHigh(feeSelectorState: FeeSelectorState.Content, onShow: (String) -> Unit): Boolean {
    val multipleFees = feeSelectorState.fees as? TransactionFee.Choosable ?: return false
    val highValue = multipleFees.priority.amount.value ?: return false
    val customAmount = feeSelectorState.customValues.firstOrNull() ?: return false
    val customValue = customAmount.value.parseToBigDecimal(customAmount.decimals)
    val diff = if (highValue > BigDecimal.ZERO) {
        customValue / highValue
    } else {
        BigDecimal.ZERO
    }
    val isShow = feeSelectorState.selectedFee == FeeType.Custom && diff > FEE_MAX_DIFF
    if (isShow) onShow(diff.parseBigDecimal(ZERO_DECIMALS, RoundingMode.HALF_UP))
    return isShow
}

/**
 * Checks if fee exceeds fee paid currency balance
 */
fun checkExceedBalance(feeBalance: BigDecimal?, feeAmount: BigDecimal?): Boolean {
    return feeAmount == null || feeBalance == null || feeAmount.isZero() || feeAmount > feeBalance
}

private val FEE_MAX_DIFF = BigDecimal("5")
private const val ZERO_DECIMALS = 0