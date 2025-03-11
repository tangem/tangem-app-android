package com.tangem.tap.domain.walletconnect2.domain.models.binance

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.tap.domain.walletconnect2.domain.WcRequestData

@JsonClass(generateAdapter = true)
data class WcBinanceTxConfirmParam(
    @Json(name = "ok")
    val ok: Boolean,
    @Json(name = "errorMsg")
    val errorMsg: String?,
) : WcRequestData