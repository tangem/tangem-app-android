package com.tangem.spend.datasource.pay.models.response

import com.tangem.domain.models.serialization.SerializedRawJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    @SerialName("result") val result: Result?,
) {
    @Serializable
    data class Result(
        @SerialName("id") val id: String,
        @SerialName("customer_id") val customerId: String?,
        @SerialName("type") val type: String?,
        /**
         * A status this build does not know falls back to [Status.UNKNOWN] since app version 6.4. Older versions
         * fail to parse the whole response on it.
         */
        @SerialName("status") val status: Status = Status.UNKNOWN,
        @SerialName("step") val step: String?,
        @SerialName("data") val data: Data,
        @SerialName("step_change_code") val stepChangeCode: Int?,
        @SerialName("created_at") val createdAt: String?,
        @SerialName("updated_at") val updatedAt: String?,
    ) {
        @Serializable
        data class Data(
            @SerialName("type") val type: String?,
            @SerialName("specification_name") val specificationName: String?,
            @SerialName("customer_wallet_address") val customerWalletAddress: String?,
            @SerialName("emboss_name") val embossName: String?,
            @SerialName("product_instance_id") val productInstanceId: String?,
            @SerialName("source_product_instance_id") val sourceProductInstanceId: String? = null,
            @SerialName("payment_account_id") val paymentAccountId: String?,
            @SerialName("target_tariff_plan_id") val targetTariffPlanId: String?,
            @SerialName("transaction_hash") val transactionHash: String?,
            @SerialName("bank_account_id") val bankAccountId: String? = null,
            @SerialName("withdraw_data") val withdrawData: WithdrawData? = null,
        ) {

            /**
             * What a bank off-ramp order asks the card to sign, sent once the order reaches `AWAITING_SIGNATURE`.
             *
             * Only [hash] is consumed by the app; the rest is optional because this response is also the element
             * of the orders list, where one unexpected bank order must not break parsing of every other order.
             */
            @Serializable
            data class WithdrawData(
                @SerialName("hash") val hash: String,
                @SerialName("salt") val salt: String? = null,
                @SerialName("sender_address") val senderAddress: String? = null,
                /**
                 * Kept as the JSON the server sent, number literals included: decoding it into Kotlin types would
                 * turn `chainId: 137` into a double and the EIP-712 digest would stop matching [hash].
                 */
                @SerialName("structured_data") val structuredData: SerializedRawJson? = null,
            )
        }

        @Serializable
        enum class Status {
            @SerialName("NEW")
            NEW,

            @SerialName("PROCESSING")
            PROCESSING,

            @SerialName("COMPLETED")
            COMPLETED,

            @SerialName("CANCELED")
            CANCELED,

            /**
             * Not a wire value: every status this app version does not know is coerced to this default by
             * [com.tangem.core.remote.json.NetworkJsonFactory].
             */
            UNKNOWN,
        }
    }
}