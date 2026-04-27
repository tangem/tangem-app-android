package com.tangem.domain.walletconnect.model.pay

data class WcPaymentInfo(
    val status: WcPaymentStatus,
    val amount: WcPayAmount,
    val expiresAt: Long,
    val merchant: WcPayMerchantInfo,
)