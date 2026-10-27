package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BankOfframpRouteResponse(
    @SerialName("result") val result: Result?,
) {

    @Serializable
    data class Result(
        @SerialName("deposit_address") val depositAddress: String?,
        @SerialName("fee") val fee: BankOfframpOptionsResponse.Fee?,
        @SerialName("eta") val eta: BankOfframpOptionsResponse.Eta?,
        @SerialName("bank_account_id") val bankAccountId: String?,
    )
}