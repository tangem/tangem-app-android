package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.joda.time.DateTime
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class TangemPayTxHistoryResponse(
    @Json(name = "error") val error: String?,
    @Json(name = "result") val result: Result,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "transactions") val transactions: List<Transaction>,
    )

    @JsonClass(generateAdapter = true)
    data class Transaction(
        @Json(name = "id") val id: String, // UUID (used as cursor for pagination)
        @Json(name = "type") val type: String, // "SPEND", "COLLATERAL", "PAYMENT", "FEE"
        @Json(name = "spend") val spend: Spend? = null,
        @Json(name = "collateral") val collateral: Collateral? = null,
        @Json(name = "payment") val payment: Payment? = null,
        @Json(name = "fee") val fee: Fee? = null,
    )

    @JsonClass(generateAdapter = true)
    @Suppress("BooleanPropertyNaming")
    data class Spend(
        @Json(name = "amount") val amount: BigDecimal,
        @Json(name = "currency") val currency: String,
        @Json(name = "local_amount") val localAmount: BigDecimal? = null,
        @Json(name = "local_currency") val localCurrency: String? = null,
        @Json(name = "authorized_amount") val authorizedAmount: BigDecimal? = null,
        @Json(name = "authorization_method") val authorizationMethod: String? = null,
        @Json(name = "memo") val memo: String? = null,
        @Json(name = "receipt") val receipt: Boolean,
        @Json(name = "merchant_name") val merchantName: String,
        @Json(name = "merchant_category") val merchantCategory: String? = null,
        @Json(name = "merchant_category_code") val merchantCategoryCode: String? = null,
        @Json(name = "merchant_id") val merchantId: String? = null,
        @Json(name = "enriched_merchant_icon") val enrichedMerchantIcon: String? = null,
        @Json(name = "enriched_merchant_name") val enrichedMerchantName: String? = null,
        @Json(name = "enriched_merchant_category") val enrichedMerchantCategory: String? = null,
        @Json(name = "card_id") val cardId: String? = null,
        @Json(name = "card_type") val cardType: String? = null,
        @Json(name = "status") val status: String,
        @Json(name = "declined_reason") val declinedReason: String? = null,
        @Json(name = "authorized_at") val authorizedAt: DateTime,
        @Json(name = "posted_at") val postedAt: DateTime?,
    )

    @JsonClass(generateAdapter = true)
    data class Collateral(
        @Json(name = "amount") val amount: BigDecimal,
        @Json(name = "currency") val currency: String,
        @Json(name = "memo") val memo: String? = null,
        @Json(name = "chain_id") val chainId: Long,
        @Json(name = "wallet_address") val walletAddress: String,
        @Json(name = "transaction_hash") val transactionHash: String,
        @Json(name = "posted_at") val postedAt: DateTime?,
    )

    @JsonClass(generateAdapter = true)
    data class Payment(
        @Json(name = "amount") val amount: BigDecimal,
        @Json(name = "currency") val currency: String,
        @Json(name = "memo") val memo: String? = null,
        @Json(name = "chain_id") val chainId: Long? = null,
        @Json(name = "wallet_address") val walletAddress: String? = null,
        @Json(name = "transaction_hash") val transactionHash: String? = null,
        @Json(name = "status") val status: String,
        @Json(name = "posted_at") val postedAt: DateTime,
    )

    @JsonClass(generateAdapter = true)
    data class Fee(
        @Json(name = "amount") val amount: BigDecimal,
        @Json(name = "currency") val currency: String,
        @Json(name = "description") val description: String? = null,
        @Json(name = "posted_at") val postedAt: DateTime,
    )
}