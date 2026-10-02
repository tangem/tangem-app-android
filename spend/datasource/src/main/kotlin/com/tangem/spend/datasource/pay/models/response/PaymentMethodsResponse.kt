package com.tangem.spend.datasource.pay.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of `bff-v2/v1/payment-methods`
 */
@Serializable
data class PaymentMethodsResponse(
    @SerialName("result") val result: List<PaymentMethod>?,
) {

    @Serializable
    data class PaymentMethod(
        @SerialName("id") val id: String?,
        @SerialName("name") val name: String?,
        @SerialName("country_code") val countryCode: String?,
        @SerialName("icon_url") val iconUrl: String?,
        @SerialName("fees") val fees: Fees?,
    )

    @Serializable
    data class Fees(
        @SerialName("receive") val receive: Fee?,
        @SerialName("send") val send: Fee?,
    )

    @Serializable
    data class Fee(
        @SerialName("type") val type: String?,
        @SerialName("calculation_type") val calculationType: String?,
        @SerialName("amount") val amount: SerializedBigDecimal?,
        @SerialName("currency") val currency: String?,
    )
}