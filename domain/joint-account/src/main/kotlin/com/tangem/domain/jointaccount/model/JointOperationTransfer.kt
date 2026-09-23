package com.tangem.domain.jointaccount.model

import com.tangem.domain.models.currency.CryptoCurrency

data class JointOperationTransfer(
    val currency: CryptoCurrency,
    val intent: JointTransferIntent,
)