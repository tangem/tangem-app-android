package com.tangem.domain.pay.model

import java.math.BigDecimal

private const val APPROVED_KYC_STATUS = "APPROVED"

data class MainScreenCustomerInfo(
    val info: CustomerInfo,
    val orderStatus: OrderStatus,
)

data class CustomerInfo(
    val productInstance: ProductInstance?,
    val kycStatus: String?,
    val cardInfo: CardInfo?,
) {

    data class ProductInstance(
        val id: String,
        val status: String,
    )

    data class CardInfo(
        val lastFourDigits: String,
        val balance: BigDecimal,
        val currencyCode: String,
    )

    fun isKycApproved() = kycStatus == APPROVED_KYC_STATUS
}