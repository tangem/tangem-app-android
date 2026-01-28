package com.tangem.domain.pay.model

import com.tangem.domain.visa.model.TangemPayCardFrozenState
import java.math.BigDecimal

sealed class MainCustomerInfoContentState {
    object Loading : MainCustomerInfoContentState()
    object OnboardingBanner : MainCustomerInfoContentState()
    data class Content(val info: MainScreenCustomerInfo) : MainCustomerInfoContentState()
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

    enum class KycStatus {
        /** Initial state */
        INIT,

        /** Performing the check */
        PENDING,

        /** SumSub approved */
        APPROVED,

        /** The check failed, documents rejected */
        REJECTED,
    }

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
        val isPinSet: Boolean,
    )
}