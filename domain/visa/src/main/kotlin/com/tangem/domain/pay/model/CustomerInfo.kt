package com.tangem.domain.pay.model

import com.tangem.domain.models.kyc.KycStatus
import java.math.BigDecimal

sealed class MainCustomerInfoContentState {
    object Loading : MainCustomerInfoContentState()
    object OnboardingBanner : MainCustomerInfoContentState()
    data class Content(val info: MainScreenCustomerInfo) : MainCustomerInfoContentState()
    object Empty : MainCustomerInfoContentState()
}

data class MainScreenCustomerInfo(
    val info: CustomerInfo,
    val orderStatus: OrderStatus,
)

data class CustomerInfo(
    val customerId: String?,
    val productInstance: ProductInstance?,
    val kycStatus: KycStatus,
    val cardInfo: CardInfo?,
) {

    data class ProductInstance(
        val id: String,
        val cardId: String,
    )

    data class CardInfo(
        val lastFourDigits: String,
        val balance: BigDecimal,
        val currencyCode: String,
        val depositAddress: String?,
        val isPinSet: Boolean,
    )
}