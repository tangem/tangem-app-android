package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState

/**
 * Check if sending amount with fee is greater than balance
 */
internal fun checkFeeCoverage(state: SendUiState, cryptoCurrencyStatus: CryptoCurrencyStatus): Boolean {
    val balance = cryptoCurrencyStatus.value.amount ?: return false
    val fee = state.feeState?.fee?.amount?.value ?: return false
    val amount = state.amountState?.amountTextField?.cryptoAmount?.value ?: return false
    return balance <= amount + fee
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