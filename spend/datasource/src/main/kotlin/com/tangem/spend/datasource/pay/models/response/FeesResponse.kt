package com.tangem.spend.datasource.pay.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeesResponse(
    @SerialName("result") val result: List<Fee>?,
) {
    @Serializable
    data class Fee(
        @SerialName("type") val type: String?,
        @SerialName("group") val group: String?,
        @SerialName("name") val name: String?,
        @SerialName("payment_method_id") val paymentMethodId: String?,
        @SerialName("calculation_type") val calculationType: String?,
        @SerialName("amount") val amount: SerializedBigDecimal?,
        @SerialName("currency") val currency: String?,
        @SerialName("description") val description: String?,
    )
}