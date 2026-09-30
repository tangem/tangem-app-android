package com.tangem.spend.datasource.pay.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.serialization.SerializedIsoDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class TangemPayTxHistoryResponse(
    @SerialName("result") val result: Result,
) {

    @Serializable
    data class Result(
        @SerialName("transactions") val transactions: List<Transaction>,
    )

    @Serializable
    sealed interface Transaction {

        val id: String

        @Serializable
        @SerialName("spend")
        data class Spend(
            @SerialName("id") override val id: String,
            @SerialName("spend") val spend: SpendData?,
        ) : Transaction

        @Serializable
        @SerialName("refund")
        data class Refund(
            @SerialName("id") override val id: String,
            @SerialName("refund") val refund: RefundData?,
        ) : Transaction

        @Serializable
        @SerialName("collateral")
        data class Collateral(
            @SerialName("id") override val id: String,
            @SerialName("collateral") val collateral: CollateralData?,
        ) : Transaction

        @Serializable
        @SerialName("payment")
        data class Payment(
            @SerialName("id") override val id: String,
            @SerialName("payment") val payment: PaymentData?,
        ) : Transaction

        @Serializable
        @SerialName("fee")
        data class Fee(
            @SerialName("id") override val id: String,
            @SerialName("fee") val fee: FeeData?,
        ) : Transaction

        @Serializable
        data class Unknown(
            @SerialName("id") override val id: String,
            @SerialName("wire_type") val type: String? = null,
            @SerialName("payload") val payload: JsonObject? = null,
        ) : Transaction
    }

    @Serializable
    @Suppress("BooleanPropertyNaming")
    data class SpendData(
        @SerialName("amount") val amount: SerializedBigDecimal,
        @SerialName("currency") val currency: String,
        @SerialName("local_amount") val localAmount: SerializedBigDecimal? = null,
        @SerialName("local_currency") val localCurrency: String? = null,
        @SerialName("authorized_amount") val authorizedAmount: SerializedBigDecimal? = null,
        @SerialName("authorization_method") val authorizationMethod: String? = null,
        @SerialName("memo") val memo: String? = null,
        @SerialName("receipt") val receipt: Boolean,
        @SerialName("merchant_name") val merchantName: String,
        @SerialName("merchant_category") val merchantCategory: String? = null,
        @SerialName("merchant_category_code") val merchantCategoryCode: String? = null,
        @SerialName("merchant_id") val merchantId: String? = null,
        @SerialName("enriched_merchant_icon") val enrichedMerchantIcon: String? = null,
        @SerialName("enriched_merchant_name") val enrichedMerchantName: String? = null,
        @SerialName("enriched_merchant_category") val enrichedMerchantCategory: String? = null,
        @SerialName("card_id") val cardId: String? = null,
        @SerialName("card_type") val cardType: String? = null,
        @SerialName("card_display_name") val cardDisplayName: String? = null,
        @SerialName("card_number_end") val cardNumberEnd: String? = null,
        @SerialName("status") val status: String,
        @SerialName("declined_reason") val declinedReason: String? = null,
        @SerialName("authorized_at") val authorizedAt: SerializedIsoDateTime,
        @SerialName("posted_at") val postedAt: SerializedIsoDateTime?,
        @SerialName("cashback") val cashback: SerializedBigDecimal? = null,
        @SerialName("cashback_status") val cashbackStatus: String? = null,
        @SerialName("cashback_currency_code") val cashbackCurrencyCode: String? = null,
    )

    @Serializable
    data class RefundData(
        @SerialName("amount") val amount: SerializedBigDecimal,
        @SerialName("currency") val currency: String,
        @SerialName("local_amount") val localAmount: SerializedBigDecimal? = null,
        @SerialName("local_currency") val localCurrency: String? = null,
        @SerialName("source_transaction_id") val sourceTransactionId: String? = null,
        @SerialName("merchant_name") val merchantName: String,
        @SerialName("merchant_category") val merchantCategory: String? = null,
        @SerialName("merchant_category_code") val merchantCategoryCode: String? = null,
        @SerialName("merchant_id") val merchantId: String? = null,
        @SerialName("enriched_merchant_icon") val enrichedMerchantIcon: String? = null,
        @SerialName("enriched_merchant_name") val enrichedMerchantName: String? = null,
        @SerialName("enriched_merchant_category") val enrichedMerchantCategory: String? = null,
        @SerialName("card_id") val cardId: String? = null,
        @SerialName("card_type") val cardType: String? = null,
        @SerialName("card_display_name") val cardDisplayName: String? = null,
        @SerialName("card_number_end") val cardNumberEnd: String? = null,
        @SerialName("status") val status: String,
        @SerialName("authorized_at") val authorizedAt: SerializedIsoDateTime,
        @SerialName("posted_at") val postedAt: SerializedIsoDateTime?,
        @SerialName("cashback") val cashback: SerializedBigDecimal? = null,
        @SerialName("cashback_status") val cashbackStatus: String? = null,
        @SerialName("cashback_currency_code") val cashbackCurrencyCode: String? = null,
    )

    @Serializable
    data class CollateralData(
        @SerialName("amount") val amount: SerializedBigDecimal,
        @SerialName("currency") val currency: String,
        @SerialName("memo") val memo: String? = null,
        @SerialName("chain_id") val chainId: Long,
        @SerialName("wallet_address") val walletAddress: String,
        @SerialName("transaction_hash") val transactionHash: String,
        @SerialName("posted_at") val postedAt: SerializedIsoDateTime?,
    )

    @Serializable
    data class PaymentData(
        @SerialName("amount") val amount: SerializedBigDecimal,
        @SerialName("currency") val currency: String,
        @SerialName("memo") val memo: String? = null,
        @SerialName("chain_id") val chainId: Long? = null,
        @SerialName("wallet_address") val walletAddress: String? = null,
        @SerialName("transaction_hash") val transactionHash: String? = null,
        @SerialName("status") val status: String,
        @SerialName("posted_at") val postedAt: SerializedIsoDateTime,
    )

    @Serializable
    data class FeeData(
        @SerialName("amount") val amount: SerializedBigDecimal,
        @SerialName("currency") val currency: String,
        @SerialName("description") val description: String? = null,
        @SerialName("posted_at") val postedAt: SerializedIsoDateTime,
    )
}