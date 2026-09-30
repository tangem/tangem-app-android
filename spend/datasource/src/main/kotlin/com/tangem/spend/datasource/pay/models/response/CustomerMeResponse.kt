package com.tangem.spend.datasource.pay.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerMeResponse(
    @SerialName("result") val result: Result?,
) {
    @Serializable
    data class Result(
        @SerialName("id") val id: String,
        @SerialName("state") val state: String,
        @SerialName("created_at") val createdAt: String,
        @SerialName("payment_account") val paymentAccount: PaymentAccount?,
        @SerialName("kyc") val kyc: Kyc?,
        @SerialName("balance") val balance: BalanceResponse?,
        @SerialName("product_instances") val productInstances: List<ProductInstance>,
        @SerialName("cards") val cards: List<Card>,
        @SerialName("customer_tariff_plan") val customerTariffPlan: CustomerTariffPlan? = null,
        @SerialName("profile") val profile: Profile? = null,
    )

    @Serializable
    data class Profile(
        @SerialName("country") val country: String? = null,
        @SerialName("phone_mask") val phoneMask: String? = null,
        @SerialName("email") val email: String? = null,
        @SerialName("emboss_name") val embossName: String? = null,
    )

    @Serializable
    data class CustomerTariffPlan(
        @SerialName("status") val status: String?,
        @SerialName("source") val source: String?,
        @SerialName("next_billing_at") val nextBillingAt: String?,
        @SerialName("pending_transition_at") val pendingTransitionAt: String?,
        @SerialName("tariff_plan") val tariffPlan: TariffPlan?,
        @SerialName("pending_tariff_plan") val pendingTariffPlan: TariffPlan?,
    )

    @Serializable
    data class TariffPlan(
        @SerialName("id") val id: String?,
        @SerialName("type") val type: String?,
        @SerialName("name") val name: String?,
        @SerialName("program_name") val programName: String?,
        @SerialName("description_items") val descriptionItems: List<DescriptionItem>?,
        @SerialName("images") val images: List<Image>? = null,
        @SerialName("fees") val fees: List<Fee>? = null,
    )

    @Serializable
    data class Fee(
        @SerialName("type") val type: String?,
        @SerialName("amount") val amount: SerializedBigDecimal?,
        @SerialName("currency") val currency: String?,
        @SerialName("description") val description: String?,
        @SerialName("period") val period: String?,
    )

    @Serializable
    data class Image(
        @SerialName("type") val type: String?,
        @SerialName("url") val url: String?,
    )

    @Serializable
    data class DescriptionItem(
        @SerialName("type") val type: String?,
        @SerialName("order") val order: Int?,
        @SerialName("title") val title: String?,
        @SerialName("body") val body: String?,
    )

    @Serializable
    data class ProductInstance(
        @SerialName("id") val id: String,
        @SerialName("cid") val cid: String?,
        @SerialName("card_id") val cardId: String?,
        @SerialName("card_wallet_address") val cardWalletAddress: String?,
        @SerialName("status") val status: Status = Status.UNKNOWN,
        @SerialName("updated_at") val updatedAt: String,
        @SerialName("payment_account_id") val paymentAccountId: String,
        @SerialName("display_name") val displayName: String?,
        @SerialName("actual_card_limit") val actualCardLimit: CardLimit?,
        @SerialName("admin_card_limit") val adminCardLimit: CardLimit?,
        @SerialName("product_specification_data_type") val specificationDataType: SpecificationDataType =
            SpecificationDataType.UNKNOWN,
    ) {
        @Serializable
        enum class SpecificationDataType {
            @SerialName("ACCOUNT")
            ACCOUNT,

            @SerialName("CARD")
            CARD,

            UNKNOWN,
        }

        @Serializable
        enum class Status {
            @SerialName("NEW")
            NEW,

            @SerialName("READY_FOR_MANUFACTURING")
            READY_FOR_MANUFACTURING,

            @SerialName("MANUFACTURING")
            MANUFACTURING,

            @SerialName("SENT_TO_DELIVERY")
            SENT_TO_DELIVERY,

            @SerialName("DELIVERED")
            DELIVERED,

            @SerialName("ACTIVATING")
            ACTIVATING,

            @SerialName("ACTIVE")
            ACTIVE,

            @SerialName("BLOCKED")
            BLOCKED,

            @SerialName("DEACTIVATING")
            DEACTIVATING,

            @SerialName("DEACTIVATED")
            DEACTIVATED,

            @SerialName("CANCELED")
            CANCELED,

            @SerialName("UNKNOWN")
            UNKNOWN,
        }
    }

    @Serializable
    data class CardLimit(
        @SerialName("amount") val amount: SerializedBigDecimal,
        @SerialName("period_type") val periodType: String,
    )

    @Serializable
    data class PaymentAccount(
        @SerialName("id") val id: String,
        @SerialName("address") val address: String?,
        @SerialName("customer_wallet_address") val customerWalletAddress: String,
    )

    @Serializable
    data class Kyc(
        @SerialName("id") val id: String,
        @SerialName("provider") val provider: String,
        @SerialName("status") val status: String,
        @SerialName("risk") val risk: String,
        @SerialName("review_answer") val reviewAnswer: String,
        @SerialName("created_at") val createdAt: String,
    )

    @Serializable
    data class Card(
        @SerialName("id") val id: String?,
        @SerialName("token") val token: String,
        @SerialName("expiration_month") val expirationMonth: String,
        @SerialName("expiration_year") val expirationYear: String,
        @SerialName("emboss_name") val embossName: String?,
        @SerialName("card_type") val cardType: String,
        @SerialName("card_status") val cardStatus: String,
        @SerialName("card_number_end") val cardNumberEnd: String,
        @SerialName("is_pin_set") val isPinSet: Boolean?,
        @SerialName("images") val images: List<Image>?,
    )
}