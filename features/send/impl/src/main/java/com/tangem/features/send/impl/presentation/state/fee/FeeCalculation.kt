package com.tangem.features.send.impl.presentation.state.fee

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