package com.tangem.domain.walletconnect.model.pay

data class WcPayRequiredAction(
    val chainId: String,
    val method: String,
    val params: String,
)