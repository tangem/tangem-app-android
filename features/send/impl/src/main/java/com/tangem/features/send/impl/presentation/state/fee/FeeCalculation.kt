package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.features.send.impl.presentation.state.SendUiState
import java.math.BigDecimal

/**
 * Calculate receiving amount when fee is subtracted from sending amount
 */
internal fun FeeSelectorState.Content.calculateReceiveAmount(uiState: SendUiState): BigDecimal {
    val amount = uiState.amountState?.amountTextField?.value ?: return BigDecimal.ZERO

    val fee = when (fees) {
        is TransactionFee.Choosable -> {
            when (selectedFee) {
                FeeType.SLOW -> fees.minimum.amount.value
                FeeType.MARKET -> fees.normal.amount.value
                FeeType.FAST -> fees.priority.amount.value
                FeeType.CUSTOM -> customValues.firstOrNull()?.value?.let { BigDecimal(it.ifEmpty { "0" }) }
            }
        }
        is TransactionFee.Single -> fees.normal.amount.value
    } ?: BigDecimal.ZERO

    return BigDecimal(amount).minus(fee)
}