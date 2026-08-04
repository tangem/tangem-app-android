package com.tangem.datasource.local.visa.entity

import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.serialization.SerializedCurrency
import com.tangem.domain.models.serialization.SerializedDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayTxHistoryItemDM {

    abstract val id: String
    abstract val date: SerializedDateTime
    abstract val amount: SerializedBigDecimal
    abstract val currency: SerializedCurrency
    abstract val jsonRepresentation: String

    @Serializable
    @SerialName("spend")
    data class Spend(
        @SerialName("id") override val id: String,
        @SerialName("json_representation") override val jsonRepresentation: String,
        @SerialName("date") override val date: SerializedDateTime,
        @SerialName("amount") override val amount: SerializedBigDecimal,
        @SerialName("currency") override val currency: SerializedCurrency,
        @SerialName("authorized_amount") val authorizedAmount: SerializedBigDecimal,
        @SerialName("local_amount") val localAmount: SerializedBigDecimal?,
        @SerialName("local_currency") val localCurrency: SerializedCurrency?,
        @SerialName("enriched_merchant_name") val enrichedMerchantName: String?,
        @SerialName("merchant_name") val merchantName: String,
        @SerialName("enriched_merchant_category") val enrichedMerchantCategory: String?,
        @SerialName("merchant_category_code") val merchantCategoryCode: String?,
        @SerialName("merchant_category") val merchantCategory: String?,
        @SerialName("status") val status: Status,
        @SerialName("enriched_merchant_icon_url") val enrichedMerchantIconUrl: String?,
        @SerialName("declined_reason") val declinedReason: String?,
        @SerialName("cashback") val cashback: Cashback? = null,
    ) : TangemPayTxHistoryItemDM()

    @Serializable
    @SerialName("payment")
    data class Payment(
        @SerialName("id") override val id: String,
        @SerialName("json_representation") override val jsonRepresentation: String,
        @SerialName("date") override val date: SerializedDateTime,
        @SerialName("amount") override val amount: SerializedBigDecimal,
        @SerialName("currency") override val currency: SerializedCurrency,
        @SerialName("transaction_hash") val transactionHash: String?,
    ) : TangemPayTxHistoryItemDM()

    @Serializable
    @SerialName("fee")
    data class Fee(
        @SerialName("id") override val id: String,
        @SerialName("json_representation") override val jsonRepresentation: String,
        @SerialName("date") override val date: SerializedDateTime,
        @SerialName("amount") override val amount: SerializedBigDecimal,
        @SerialName("currency") override val currency: SerializedCurrency,
        @SerialName("description") val description: String?,
    ) : TangemPayTxHistoryItemDM()

    @Serializable
    @SerialName("collateral")
    data class Collateral(
        @SerialName("id") override val id: String,
        @SerialName("json_representation") override val jsonRepresentation: String,
        @SerialName("date") override val date: SerializedDateTime,
        @SerialName("amount") override val amount: SerializedBigDecimal,
        @SerialName("currency") override val currency: SerializedCurrency,
        @SerialName("transaction_hash") val transactionHash: String,
        @SerialName("type") val type: Type,
    ) : TangemPayTxHistoryItemDM()

    @Serializable
    enum class Type {
        @SerialName("deposit")
        Deposit,

        @SerialName("withdrawal")
        Withdrawal,
    }

    @Serializable
    enum class Status {
        @SerialName("pending")
        PENDING,

        @SerialName("reserved")
        RESERVED,

        @SerialName("completed")
        COMPLETED,

        @SerialName("declined")
        DECLINED,

        @SerialName("reversed")
        REVERSED,

        @SerialName("unknown")
        UNKNOWN,
    }

    @Serializable
    data class Cashback(
        @SerialName("status") val status: Status,
        @SerialName("amount") val amount: SerializedBigDecimal? = null,
        @SerialName("currency") val currency: SerializedCurrency? = null,
        @SerialName("cap_trimmed") val isCapTrimmed: Boolean = false,
        @SerialName("exclusion_reason") val exclusionReason: ExclusionReason? = null,
        @SerialName("promotion_ids") val promotionIds: List<String> = emptyList(),
    ) {
        @Serializable
        enum class Status {
            @SerialName("estimated")
            ESTIMATED,

            @SerialName("confirmed")
            CONFIRMED,

            @SerialName("excluded")
            EXCLUDED,

            @SerialName("awaiting_calculation")
            AWAITING_CALCULATION,

            @SerialName("unknown")
            UNKNOWN,
        }

        @Serializable
        enum class ExclusionReason {
            @SerialName("mcc_excluded")
            MCC_EXCLUDED,

            @SerialName("monthly_cap_reached")
            MONTHLY_CAP_REACHED,

            @SerialName("customer_blocklisted")
            CUSTOMER_BLOCKLISTED,

            @SerialName("merchant_country_excluded")
            MERCHANT_COUNTRY_EXCLUDED,

            @SerialName("below-min")
            BELOW_MIN,

            @SerialName("unknown")
            UNKNOWN,
        }
    }
}