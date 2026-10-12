package com.tangem.spend.datasource.pay.models.response

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
        @Json(name = "status") val status: Status = Status.UNKNOWN,
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
            @Json(name = "source_product_instance_id") val sourceProductInstanceId: String? = null,
            @Json(name = "payment_account_id") val paymentAccountId: String?,
            @Json(name = "target_tariff_plan_id") val targetTariffPlanId: String?,
            @Json(name = "transaction_hash") val transactionHash: String?,
            @Json(name = "bank_account_id") val bankAccountId: String? = null,
            @Json(name = "withdraw_data") val withdrawData: WithdrawData? = null,
        ) {

            /**
             * What a bank off-ramp order asks the card to sign, sent once the order reaches `AWAITING_SIGNATURE`.
             *
             * Only [hash] is consumed by the app; the rest is optional because this response is also the element
             * of the orders list, where one unexpected bank order must not break parsing of every other order.
             */
            @JsonClass(generateAdapter = true)
            data class WithdrawData(
                @Json(name = "hash") val hash: String,
                @Json(name = "salt") val salt: String? = null,
                @Json(name = "sender_address") val senderAddress: String? = null,
                /**
                 * Kept as the text the server sent: re-encoding it through Kotlin types would turn
                 * `chainId: 137` into `137.0` and the EIP-712 digest would stop matching [hash].
                 */
                @Json(name = "structured_data") @RawJson val structuredData: String? = null,
            )
        }

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

            /** Not a wire value: every status this app version does not know is mapped here by the fallback adapter. */
            UNKNOWN,
        }
    }
}