package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardBalanceResponse(
    @SerialName("result") val result: BalanceResponse?,
)