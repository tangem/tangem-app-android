package com.tangem.domain.walletconnect.model.pay

data class WcPaymentOption(
    val id: String,
    val amount: WcPayAmount,
    val account: String,
    val estimatedTxs: Int?,
    val collectData: WcPayCollectData?,
)