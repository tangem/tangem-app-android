package com.tangem.features.send.send.confirm.model

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.transaction.error.GetFeeError
import java.math.BigDecimal

@Immutable
data class ConfirmData(
    val enteredAmount: BigDecimal?,
    val reduceAmountBy: BigDecimal,
    val isIgnoreReduce: Boolean,
    val enteredDestination: String?,
    val enteredMemo: String?,
    val fee: Fee?,
    val feeError: GetFeeError?,
)