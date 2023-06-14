package com.tangem.tap.domain.walletconnect2.domain.models

import com.tangem.tap.features.details.redux.walletconnect.BinanceMessageData

data class BnbData(
    val data: BinanceMessageData,
    val topic: String,
    val requestId: Long,
    val dAppName: String,
)