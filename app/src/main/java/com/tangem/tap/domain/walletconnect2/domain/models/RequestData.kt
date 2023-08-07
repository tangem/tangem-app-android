package com.tangem.tap.domain.walletconnect2.domain.models

data class RequestData(
    val topic: String,
    val requestId: Long,
    val method: String,
    val blockchain: String,
)
