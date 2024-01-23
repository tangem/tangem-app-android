package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.features.send.impl.presentation.state.SendUiState
import java.math.BigDecimal

/**
 * Calculate receiving amount when fee is subtracted from sending amount
 */
internal fun calculateReceiveAmount(state: SendUiState, feeAmount: Fee): BigDecimal {
    val amountValue = state.amountState?.amountValue ?: BigDecimal.ZERO
    val fee = feeAmount.amount.value ?: return BigDecimal.ZERO
    return amountValue.minus(fee)
}

/**
 * Returns fee amount depending on current state
 */
internal fun FeeSelectorState.Content.getFee(): Fee {
    return when (fees) {
        is TransactionFee.Choosable -> {
            when (selectedFee) {
                FeeType.SLOW -> fees.minimum
                FeeType.MARKET -> fees.normal
                FeeType.FAST -> fees.priority
                FeeType.CUSTOM -> {
                    val feeAmount =
                        customValues.firstOrNull()?.value?.let { BigDecimal(it.ifEmpty { "0" }) } ?: BigDecimal.ZERO
                    Fee.Common(
                        fees.normal.amount.copy(
                            value = feeAmount,
                        ),
                    )
                }
            }
        }
        is TransactionFee.Single -> fees.normal
    }
}