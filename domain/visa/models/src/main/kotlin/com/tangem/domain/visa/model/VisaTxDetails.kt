package com.tangem.domain.visa.model

import org.joda.time.DateTime
import java.math.BigDecimal
import java.util.Currency

data class VisaTxDetails(
    val id: String,
    val type: String,
    val status: String,
    val blockchainAmount: BigDecimal,
    val transactionAmount: BigDecimal,
    val transactionCurrencyCode: Int,
    val merchantName: String?,
    val merchantCity: String?,
    val merchantCountryCode: String?,
    val merchantCategoryCode: String?,
    val fiatCurrency: Currency,
    val requests: List<Request>,
) {

    data class Request(
        val id: String,
        val billingAmount: BigDecimal,
        val billingCurrencyCode: Int,
        val blockchainAmount: BigDecimal,
        val errorCode: Int,
        val requestDate: DateTime,
        val requestStatus: String,
        val requestType: String,
        val transactionAmount: BigDecimal,
        val txCurrencyCode: Int,
        val txHash: String?,
        val txStatus: String?,
        val exploreUrl: String?,
        val fiatCurrency: Currency,
    )
}