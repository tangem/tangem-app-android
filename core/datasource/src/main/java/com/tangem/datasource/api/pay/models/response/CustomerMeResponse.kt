package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

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
        @Json(name = "product_instances") val productInstances: List<ProductInstance>,
        @Json(name = "cards") val cards: List<Card>,
        @Json(name = "customer_tariff_plan") val customerTariffPlan: CustomerTariffPlan? = null,
    )

    @JsonClass(generateAdapter = true)
    data class CustomerTariffPlan(
        @Json(name = "status") val status: String?,
        @Json(name = "next_billing_at") val nextBillingAt: String?,
        @Json(name = "pending_transition_at") val pendingTransitionAt: String?,
        @Json(name = "tariff_plan") val tariffPlan: TariffPlan?,
        @Json(name = "pending_tariff_plan") val pendingTariffPlan: TariffPlan?,
    )

    @JsonClass(generateAdapter = true)
    data class TariffPlan(
        @Json(name = "id") val id: String?,
        @Json(name = "type") val type: String?,
        @Json(name = "name") val name: String?,
        @Json(name = "description_items") val descriptionItems: List<DescriptionItem>?,
    )

    @JsonClass(generateAdapter = true)
    data class DescriptionItem(
        @Json(name = "type") val type: String?,
        @Json(name = "order") val order: Int?,
        @Json(name = "title") val title: String?,
        @Json(name = "body") val body: String?,
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
        @Json(name = "display_name") val displayName: String?,
        @Json(name = "actual_card_limit") val actualCardLimit: CardLimit?,
        @Json(name = "admin_card_limit") val adminCardLimit: CardLimit?,
        @Json(name = "product_specification_data_type") val specificationDataType: SpecificationDataType,
    ) {
        @JsonClass(generateAdapter = false)
        enum class SpecificationDataType {
            @Json(name = "ACCOUNT")
            ACCOUNT,

            @Json(name = "CARD")
            CARD,
        }

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
    data class CardLimit(
        @Json(name = "amount") val amount: BigDecimal,
        @Json(name = "period_type") val periodType: String,
    )

    @JsonClass(generateAdapter = true)
    data class PaymentAccount(
        @Json(name = "id") val id: String,
        @Json(name = "address") val address: String?,
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
        // Present in the multi-card `cards[]` array to join a card to its product instance;
        // absent in the legacy single-card `card` object, where the card joins the single product instance.
        @Json(name = "card_id") val cardId: String?,
        @Json(name = "token") val token: String,
        @Json(name = "expiration_month") val expirationMonth: String,
        @Json(name = "expiration_year") val expirationYear: String,
        @Json(name = "emboss_name") val embossName: String,
        @Json(name = "card_type") val cardType: String,
        @Json(name = "card_status") val cardStatus: String,
        @Json(name = "card_number_end") val cardNumberEnd: String,
        @Json(name = "is_pin_set") val isPinSet: Boolean?,
    )
}