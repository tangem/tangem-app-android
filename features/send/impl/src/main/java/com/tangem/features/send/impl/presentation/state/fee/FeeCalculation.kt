package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.common.extensions.minimalAmount
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.lib.crypto.BlockchainUtils
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
): BigDecimal {
    val balance = cryptoCurrencyStatus.value.amount ?: return amountValue
    val isFeeCoverage = checkFeeCoverage(
        isSubtractAvailable = isAmountSubtractAvailable,
        balance = balance,
        amountValue = amountValue,
        feeValue = feeValue,
    )
    return calculateSubtractedAmount(
        isFeeCoverage = isFeeCoverage,
        cryptoCurrencyStatus = cryptoCurrencyStatus,
        amountValue = amountValue,
        feeValue = feeValue,
    )
}

/**
 * Checks if sending amount with fee is greater than balance
 */
internal fun checkFeeCoverage(
    isSubtractAvailable: Boolean,
    balance: BigDecimal,
    amountValue: BigDecimal,
    feeValue: BigDecimal,
): Boolean {
    if (!isSubtractAvailable) return false
    return balance < amountValue + feeValue && balance > feeValue
}

/**
 * Calculates subtracted amount
 */
internal fun calculateSubtractedAmount(
    isFeeCoverage: Boolean,
    cryptoCurrencyStatus: CryptoCurrencyStatus,
    amountValue: BigDecimal,
    feeValue: BigDecimal,
): BigDecimal {
    val balance = cryptoCurrencyStatus.value.amount ?: return amountValue
    return if (isFeeCoverage) {
        var subtractedValue = minOf(amountValue, balance.minus(feeValue))
        if (BlockchainUtils.isTezos(cryptoCurrencyStatus.currency.network.id.value)) {
            val threshold = Blockchain.Tezos.minimalAmount()
            subtractedValue = -threshold
        }
        subtractedValue
    } else {
        amountValue
    }
}

/**
 * Check if custom fee is too low
 */
internal fun checkIfFeeTooLow(state: SendUiState): Boolean {
    val feeSelectorState = state.feeState?.feeSelectorState as? FeeSelectorState.Content ?: return false
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
    val diff = customValue / highValue
    val isShow = feeSelectorState.selectedFee == FeeType.Custom && diff > FEE_MAX_DIFF
    if (isShow) onShow(diff.parseBigDecimal(ZERO_DECIMALS, RoundingMode.HALF_UP))
    return isShow
}

private val FEE_MAX_DIFF = BigDecimal("5")
private const val ZERO_DECIMALS = 0
