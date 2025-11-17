package com.tangem.domain.pay.model

import java.math.BigDecimal

data class MainScreenCustomerInfo(
    val info: CustomerInfo,
    val orderStatus: OrderStatus,
)

data class CustomerInfo(
    val productInstance: ProductInstance?,
    val isKycApproved: Boolean,
    val cardInfo: CardInfo?,
) {

    data class ProductInstance(
        val id: String,
        val cardId: String,
        val status: Status,
    ) {
        enum class Status {
            ACTIVE,
            INACTIVE,
        }
    }

    data class CardInfo(
        val lastFourDigits: String,
        val balance: BigDecimal,
        val currencyCode: String,
        val customerWalletAddress: String,
        val depositAddress: String?,
    )
}