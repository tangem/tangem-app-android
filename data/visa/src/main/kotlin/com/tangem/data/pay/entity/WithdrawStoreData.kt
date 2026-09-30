package com.tangem.data.pay.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WithdrawStoreData(
    @SerialName("orderId") val orderId: String,
    @SerialName("exchangeData") val exchangeData: ExchangeStoreData?,
    @SerialName("txHash") val txHash: String?,
)

@Serializable
data class ExchangeStoreData(
    @SerialName("txId") val txId: String,
    @SerialName("fromNetwork") val fromNetwork: String,
    @SerialName("fromAddress") val fromAddress: String,
    @SerialName("payInAddress") val payInAddress: String,
    @SerialName("payInExtraId") val payInExtraId: String?,
)