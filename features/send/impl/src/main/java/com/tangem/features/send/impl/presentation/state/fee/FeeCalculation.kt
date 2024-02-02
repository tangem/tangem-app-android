package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.features.send.impl.presentation.state.SendUiState
import java.math.BigDecimal

/**
 * Calculate receiving amount when fee is subtracted from sending amount
 */
internal fun calculateReceiveAmount(state: SendUiState, feeAmount: Fee): BigDecimal {
    val amountValue = state.amountState?.amountTextField?.cryptoAmount?.value ?: BigDecimal.ZERO
    val fee = feeAmount.amount.value ?: return BigDecimal.ZERO
    return amountValue.minus(fee)
}