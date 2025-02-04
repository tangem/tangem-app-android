package com.tangem.lib.visa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.joda.time.DateTime
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class VisaTxHistoryResponse(
    @Json(name = "card_wallet_address")
    val cardWalletAddress: String,
    @Json(name = "transactions")
    val transactions: List<Transaction>,
) {

    @JsonClass(generateAdapter = true)
    data class Transaction(
        @Json(name = "auth_code")
        val authCode: String?,
        @Json(name = "billing_amount")
        val billingAmount: BigDecimal,
        @Json(name = "billing_currency_code")
        val billingCurrencyCode: Int,
        @Json(name = "blockchain_amount")
        val blockchainAmount: BigDecimal,
        @Json(name = "blockchain_coin_name")
        val blockchainCoinName: String,
        @Json(name = "blockchain_fee")
        val blockchainFee: BigDecimal,
        // @Json(name = "local_dt")
        // val localDate: DateTime?,
        @Json(name = "merchant_category_code")
        val merchantCategoryCode: String?,
        @Json(name = "merchant_city")
        val merchantCity: String?,
        @Json(name = "merchant_country_code")
        val merchantCountryCode: String?,
        @Json(name = "merchant_name")
        val merchantName: String?,
        @Json(name = "requests")
        val requests: List<Request>,
        @Json(name = "rrn")
        val rrn: String?,
        @Json(name = "transaction_amount")
        val transactionAmount: BigDecimal,
        @Json(name = "transaction_currency_code")
        val transactionCurrencyCode: Int,
        @Json(name = "transaction_dt")
        val transactionDt: DateTime,
        @Json(name = "transaction_id")
        val transactionId: Long,
        @Json(name = "transaction_status")
        val transactionStatus: String,
        @Json(name = "transaction_type")
        val transactionType: String,
    ) {

        @JsonClass(generateAdapter = true)
        data class Request(
            @Json(name = "billing_amount")
            val billingAmount: BigDecimal,
            @Json(name = "billing_currency_code")
            val billingCurrencyCode: Int,
            @Json(name = "blockchain_amount")
            val blockchainAmount: BigDecimal,
            @Json(name = "blockchain_fee")
            val blockchainFee: BigDecimal,
            @Json(name = "error_code")
            val errorCode: Int,
            @Json(name = "request_dt")
            val requestDt: DateTime,
            @Json(name = "request_status")
            val requestStatus: String,
            @Json(name = "request_type")
            val requestType: String,
            @Json(name = "transaction_amount")
            val transactionAmount: BigDecimal,
            @Json(name = "transaction_currency_code")
            val transactionCurrencyCode: Int,
            @Json(name = "transaction_request_id")
            val transactionRequestId: Long,
            @Json(name = "tx_hash")
            val txHash: String?,
            @Json(name = "tx_status")
            val txStatus: String?,
        )
    }
}