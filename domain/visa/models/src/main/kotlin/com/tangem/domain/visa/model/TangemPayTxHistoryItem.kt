package com.tangem.domain.visa.model

import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.serialization.SerializedCurrency
import com.tangem.domain.models.serialization.SerializedDateTime
import kotlinx.serialization.Serializable

@Serializable
sealed class TangemPayTxHistoryItem {
    abstract val id: String
    abstract val date: SerializedDateTime
    abstract val amount: SerializedBigDecimal
    abstract val currency: SerializedCurrency
    abstract val jsonRepresentation: String

    @Serializable
    data class Spend(
        override val id: String,
        override val jsonRepresentation: String,
        override val date: SerializedDateTime,
        override val amount: SerializedBigDecimal,
        override val currency: SerializedCurrency,
        val authorizedAmount: SerializedBigDecimal,
        val localAmount: SerializedBigDecimal?,
        val localCurrency: SerializedCurrency?,
        val enrichedMerchantName: String?,
        val merchantName: String,
        val enrichedMerchantCategory: String?,
        val merchantCategoryCode: String?,
        val merchantCategory: String?,
        val status: Status,
        val enrichedMerchantIconUrl: String?,
        val declinedReason: String?,
        val cardName: String? = null,
        val cardNumberLast4: String? = null,
        val cashback: Cashback? = null,
    ) : TangemPayTxHistoryItem()

    @Serializable
    data class Payment(
        override val id: String,
        override val jsonRepresentation: String,
        override val date: SerializedDateTime,
        override val amount: SerializedBigDecimal,
        override val currency: SerializedCurrency,
        val transactionHash: String?,
    ) : TangemPayTxHistoryItem()

    @Serializable
    data class Fee(
        override val id: String,
        override val jsonRepresentation: String,
        override val date: SerializedDateTime,
        override val amount: SerializedBigDecimal,
        override val currency: SerializedCurrency,
        val description: String?,
    ) : TangemPayTxHistoryItem()

    @Serializable
    data class Collateral(
        override val id: String,
        override val jsonRepresentation: String,
        override val date: SerializedDateTime,
        override val amount: SerializedBigDecimal,
        override val currency: SerializedCurrency,
        val transactionHash: String,
        val type: Type,
    ) : TangemPayTxHistoryItem()

    enum class Type {
        Deposit, Withdrawal
    }

    enum class Status {
        PENDING,
        RESERVED,
        COMPLETED,
        DECLINED,
        REVERSED,
        UNKNOWN,
    }

    /**
     * Per-transaction cashback earned on a [Spend]. Present only when the cashback program applies to
     * the transaction; `null` when it does not (customer on the cashback ignore list, non-spend
     * transaction). [amount] is in USD and negative for refunds. [amount] and [currency] are `null`
     * while [status] is [Status.AWAITING_CALCULATION] (cashback not yet calculated by the backend).
     */
    @Serializable
    data class Cashback(
        val status: Status,
        val amount: SerializedBigDecimal?,
        val currency: SerializedCurrency?,
        val isCapTrimmed: Boolean,
        val exclusionReason: ExclusionReason?,
        val promotionIds: List<String>,
    ) {
        enum class Status {
            ESTIMATED,
            CONFIRMED,
            EXCLUDED,
            AWAITING_CALCULATION,
            UNKNOWN,
        }

        enum class ExclusionReason {
            MCC_EXCLUDED,
            MONTHLY_CAP_REACHED,
            CUSTOMER_BLOCKLISTED,
            MERCHANT_COUNTRY_EXCLUDED,
            BELOW_MIN,
            UNKNOWN,
        }
    }
}