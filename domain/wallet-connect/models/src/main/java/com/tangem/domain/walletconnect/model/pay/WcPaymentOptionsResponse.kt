package com.tangem.domain.walletconnect.model.pay

data class WcPaymentOptionsResponse(
    val paymentId: String,
    val info: WcPaymentInfo?,
    val options: List<WcPaymentOption>,
    val collectDataAction: WcPayCollectData?,
)