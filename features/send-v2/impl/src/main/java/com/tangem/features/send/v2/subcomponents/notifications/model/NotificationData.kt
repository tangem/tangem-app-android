package com.tangem.features.send.v2.subcomponents.notifications.model

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.transaction.error.GetFeeError
import java.math.BigDecimal

data class NotificationData(
    val destinationAddress: String,
    val memo: String?,
    val amountValue: BigDecimal,
    val reduceAmountBy: BigDecimal,
    val isIgnoreReduce: Boolean,
    val fee: Fee?,
    val feeError: GetFeeError?,
)