package com.tangem.tap.domain.walletconnect2.domain.models.binance

import com.tangem.tap.domain.walletconnect2.domain.WcRequestData

@Suppress("LongParameterList")
open class WcBinanceOrder<T>(
    val accountNumber: String,
    val chainId: String,
    val data: String?,
    val memo: String?,
    val sequence: String,
    val source: String,
    val msgs: List<T>,
) : WcRequestData

data class WcBinanceTxConfirmParam(
    val ok: Boolean,
    val errorMsg: String?,
) : WcRequestData
