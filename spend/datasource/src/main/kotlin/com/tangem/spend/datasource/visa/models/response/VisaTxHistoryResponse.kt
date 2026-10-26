package com.tangem.spend.datasource.visa.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.serialization.SerializedIsoDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VisaTxHistoryResponse(
    @SerialName("card_wallet_address")
    val cardWalletAddress: String,
    @SerialName("transactions")
    val transactions: List<Transaction>,
) {

    @Serializable
    data class Transaction(
        @SerialName("auth_code")
        val authCode: String?,
        @SerialName("billing_amount")
        val billingAmount: SerializedBigDecimal,
        @SerialName("billing_currency_code")
        val billingCurrencyCode: Int,
        @SerialName("blockchain_amount")
        val blockchainAmount: SerializedBigDecimal,
        @SerialName("blockchain_coin_name")
        val blockchainCoinName: String,
        @SerialName("blockchain_fee")
        val blockchainFee: SerializedBigDecimal,
        // @SerialName("local_dt")
        // val localDate: SerializedIsoDateTime?,
        @SerialName("merchant_category_code")
        val merchantCategoryCode: String?,
        @SerialName("merchant_city")
        val merchantCity: String?,
        @SerialName("merchant_country_code")
        val merchantCountryCode: String?,
        @SerialName("merchant_name")
        val merchantName: String?,
        @SerialName("requests")
        val requests: List<Request>,
        @SerialName("rrn")
        val rrn: String?,
        @SerialName("transaction_amount")
        val transactionAmount: SerializedBigDecimal,
        @SerialName("transaction_currency_code")
        val transactionCurrencyCode: Int,
        @SerialName("transaction_dt")
        val transactionDt: SerializedIsoDateTime,
        @SerialName("transaction_id")
        val transactionId: Long,
        @SerialName("transaction_status")
        val transactionStatus: String,
        @SerialName("transaction_type")
        val transactionType: String,
    ) {

        @Serializable
        data class Request(
            @SerialName("billing_amount")
            val billingAmount: SerializedBigDecimal,
            @SerialName("billing_currency_code")
            val billingCurrencyCode: Int,
            @SerialName("blockchain_amount")
            val blockchainAmount: SerializedBigDecimal,
            @SerialName("blockchain_fee")
            val blockchainFee: SerializedBigDecimal,
            @SerialName("error_code")
            val errorCode: Int,
            @SerialName("request_dt")
            val requestDt: SerializedIsoDateTime,
            @SerialName("request_status")
            val requestStatus: String,
            @SerialName("request_type")
            val requestType: String,
            @SerialName("transaction_amount")
            val transactionAmount: SerializedBigDecimal,
            @SerialName("transaction_currency_code")
            val transactionCurrencyCode: Int,
            @SerialName("transaction_request_id")
            val transactionRequestId: Long,
            @SerialName("tx_hash")
            val txHash: String?,
            @SerialName("tx_status")
            val txStatus: String?,
        )
    }
}