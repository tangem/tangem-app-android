package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CustomerMeResponse(
    @Json(name = "result") val result: Result?,
    @Json(name = "error") val error: String?,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "id") val id: String,
        @Json(name = "state") val state: String,
        @Json(name = "createdAt") val createdAt: String,
        @Json(name = "product_instance") val productInstance: ProductInstance,
        @Json(name = "payment_account") val paymentAccount: PaymentAccount,
        @Json(name = "kyc") val kyc: Kyc,
    )

    @JsonClass(generateAdapter = true)
    data class ProductInstance(
        @Json(name = "id") val id: String,
        @Json(name = "cid") val cid: String,
        @Json(name = "card_id") val cardId: String,
        @Json(name = "card_wallet_address") val cardWalletAddress: String,
        @Json(name = "status") val status: String,
        @Json(name = "updated_at") val updatedAt: String,
        @Json(name = "payment_account_id") val paymentAccountId: String,
    )

    @JsonClass(generateAdapter = true)
    data class PaymentAccount(
        @Json(name = "id") val id: String,
        @Json(name = "address") val address: String,
        @Json(name = "customer_wallet_address") val customerWalletAddress: String,
    )

    @JsonClass(generateAdapter = true)
    data class Kyc(
        @Json(name = "id") val id: String,
        @Json(name = "provider") val provider: String,
        @Json(name = "status") val status: String,
        @Json(name = "risk") val risk: String,
        @Json(name = "review_answer") val reviewAnswer: String,
        @Json(name = "created_at") val createdAt: String,
    )
}