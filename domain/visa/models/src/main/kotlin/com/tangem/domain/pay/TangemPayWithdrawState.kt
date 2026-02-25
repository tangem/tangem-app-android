package com.tangem.domain.pay

data class TangemPayWithdrawState(
    val orderId: String,
    val exchangeData: TangemPayWithdrawExchangeState?,
    val txHash: String? = null,
)

data class TangemPayWithdrawExchangeState(
    val txId: String,
    val fromNetwork: String,
    val fromAddress: String,
    val payInAddress: String,
    val payInExtraId: String?,
)