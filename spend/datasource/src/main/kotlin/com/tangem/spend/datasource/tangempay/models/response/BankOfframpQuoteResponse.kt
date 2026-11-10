package com.tangem.spend.datasource.tangempay.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BankOfframpQuoteResponse(
    @SerialName("result") val result: Result?,
) {

    @Serializable
    data class Result(
        @SerialName("quote_id") val quoteId: String?,
        @SerialName("bank_account_id") val bankAccountId: String?,
        @SerialName("fee_amount") val feeAmount: SerializedBigDecimal?,
        @SerialName("source_amount") val sourceAmount: SerializedBigDecimal?,
        @SerialName("destination_currency") val destinationCurrency: String?,
        @SerialName("destination_amount") val destinationAmount: SerializedBigDecimal?,
        @SerialName("exchange_rate") val exchangeRate: SerializedBigDecimal?,
        @SerialName("expires_at") val expiresAt: String?,
        @SerialName("fee") val fee: BankOfframpOptionsResponse.Fee?,
        @SerialName("eta") val eta: BankOfframpOptionsResponse.Eta?,
    )
}