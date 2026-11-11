package com.tangem.domain.tangempay

data class TangemPayWithdrawState(
    val orderId: String,
    val exchangeData: TangemPayWithdrawExchangeState?,
    val txHash: String? = null,
    val createdAtMillis: Long,
)

data class TangemPayWithdrawExchangeState(
    val txId: String,
    val fromNetwork: String,
    val fromAddress: String,
    val payInAddress: String,
    val payInExtraId: String?,
)