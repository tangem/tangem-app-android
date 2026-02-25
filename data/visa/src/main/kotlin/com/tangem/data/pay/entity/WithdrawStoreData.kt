package com.tangem.data.pay.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class WithdrawStoreData(
    @Json(name = "orderId") val orderId: String,
    @Json(name = "exchangeData") val exchangeData: ExchangeStoreData?,
    @Json(name = "txHash") val txHash: String?,
)

@JsonClass(generateAdapter = false)
data class ExchangeStoreData(
    @Json(name = "txId") val txId: String,
    @Json(name = "fromNetwork") val fromNetwork: String,
    @Json(name = "fromAddress") val fromAddress: String,
    @Json(name = "payInAddress") val payInAddress: String,
    @Json(name = "payInExtraId") val payInExtraId: String?,
)