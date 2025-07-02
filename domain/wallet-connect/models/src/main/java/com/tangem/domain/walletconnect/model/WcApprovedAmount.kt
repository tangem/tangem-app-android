package com.tangem.domain.walletconnect.model

import com.tangem.domain.tokens.model.Amount

data class WcApprovedAmount(
    val amount: Amount?,
    val chainId: Int?,
    val logoUrl: String?,
)