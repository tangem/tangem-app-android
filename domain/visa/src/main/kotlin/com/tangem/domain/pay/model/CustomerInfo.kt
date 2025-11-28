package com.tangem.domain.pay.model

import com.tangem.domain.visa.model.TangemPayCardFrozenState
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
        val cardFrozenState: TangemPayCardFrozenState,
    )

    data class CardInfo(
        val lastFourDigits: String,
        val balance: BigDecimal,
        val currencyCode: String,
        val customerWalletAddress: String,
        val depositAddress: String?,
    )
}