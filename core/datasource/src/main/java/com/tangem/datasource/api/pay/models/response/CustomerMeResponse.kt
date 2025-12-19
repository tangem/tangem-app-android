package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CustomerMeResponse(
    @Json(name = "result") val result: Result?,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "id") val id: String,
        @Json(name = "state") val state: String,
        @Json(name = "created_at") val createdAt: String,
        @Json(name = "product_instance") val productInstance: ProductInstance?,
        @Json(name = "payment_account") val paymentAccount: PaymentAccount?,
        @Json(name = "kyc") val kyc: Kyc?,
        @Json(name = "deposit_address") val depositAddress: String?,
        @Json(name = "card") val card: Card?,
        @Json(name = "balance") val balance: BalanceResponse?,
    )

    @JsonClass(generateAdapter = true)
    data class ProductInstance(
        @Json(name = "id") val id: String,
        @Json(name = "cid") val cid: String?,
        @Json(name = "card_id") val cardId: String,
        @Json(name = "card_wallet_address") val cardWalletAddress: String?,
        @Json(name = "status") val status: Status,
        @Json(name = "updated_at") val updatedAt: String,
        @Json(name = "payment_account_id") val paymentAccountId: String,
    ) {
        @JsonClass(generateAdapter = false)
        enum class Status {
            @Json(name = "NEW")
            NEW,

            @Json(name = "READY_FOR_MANUFACTURING")
            READY_FOR_MANUFACTURING,

            @Json(name = "MANUFACTURING")
            MANUFACTURING,

            @Json(name = "SENT_TO_DELIVERY")
            SENT_TO_DELIVERY,

            @Json(name = "DELIVERED")
            DELIVERED,

            @Json(name = "ACTIVATING")
            ACTIVATING,

            @Json(name = "ACTIVE")
            ACTIVE,

            @Json(name = "BLOCKED")
            BLOCKED,

            @Json(name = "DEACTIVATING")
            DEACTIVATING,

            @Json(name = "DEACTIVATED")
            DEACTIVATED,

            @Json(name = "CANCELED")
            CANCELED,

            @Json(name = "UNKNOWN")
            UNKNOWN,
        }
    }

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

    @JsonClass(generateAdapter = true)
    data class Card(
        @Json(name = "token") val token: String,
        @Json(name = "expiration_month") val expirationMonth: String,
        @Json(name = "expiration_year") val expirationYear: String,
        @Json(name = "emboss_name") val embossName: String,
        @Json(name = "card_type") val cardType: String,
        @Json(name = "card_status") val cardStatus: String,
        @Json(name = "card_number_end") val cardNumberEnd: String,
    )
}