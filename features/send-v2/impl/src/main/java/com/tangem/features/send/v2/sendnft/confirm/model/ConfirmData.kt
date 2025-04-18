package com.tangem.features.send.v2.sendnft.confirm.model

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.transaction.error.GetFeeError

data class ConfirmData(
    val enteredDestination: String?,
    val enteredMemo: String?,
    val fee: Fee?,
    val feeError: GetFeeError?,
)