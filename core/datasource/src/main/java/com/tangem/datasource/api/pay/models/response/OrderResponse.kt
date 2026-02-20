package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OrderResponse(
    @Json(name = "result") val result: Result?,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "id") val id: String,
        @Json(name = "customer_id") val customerId: String?,
        @Json(name = "type") val type: String?,
        @Json(name = "status") val status: Status,
        @Json(name = "step") val step: String?,
        @Json(name = "data") val data: Data,
        @Json(name = "step_change_code") val stepChangeCode: Int?,
        @Json(name = "created_at") val createdAt: String?,
        @Json(name = "updated_at") val updatedAt: String?,
    ) {
        @JsonClass(generateAdapter = true)
        data class Data(
            @Json(name = "type") val type: String?,
            @Json(name = "specification_name") val specificationName: String?,
            @Json(name = "customer_wallet_address") val customerWalletAddress: String?,
            @Json(name = "emboss_name") val embossName: String?,
            @Json(name = "product_instance_id") val productInstanceId: String?,
            @Json(name = "payment_account_id") val paymentAccountId: String?,
            @Json(name = "transaction_hash") val transactionHash: String?,
        )

        @JsonClass(generateAdapter = false)
        enum class Status {
            @Json(name = "NEW")
            NEW,

            @Json(name = "PROCESSING")
            PROCESSING,

            @Json(name = "COMPLETED")
            COMPLETED,

            @Json(name = "CANCELED")
            CANCELED,
        }
    }
}