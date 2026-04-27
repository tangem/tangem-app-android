package com.tangem.domain.walletconnect.model.pay

data class WcPayConfirmResult(
    val status: WcPaymentStatus,
    val isFinal: Boolean,
    val pollInMs: Long?,
    val txId: String?,
    val optionAmount: WcPayAmount?,
)