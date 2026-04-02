package com.tangem.domain.pay.model

import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.visa.model.TangemPayCardFrozenState
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
    enum class State {
        NEW,
        ACTIVE,
        BLOCKED,
        IN_PROGRESS,
        UNDEFINED,
        ;

        companion object {
            fun fromString(value: String) = when (value.uppercase()) {
                "NEW" -> NEW
                "ACTIVE" -> ACTIVE
                "BLOCKED" -> BLOCKED
                "IN_PROGRESS" -> IN_PROGRESS
                else -> UNDEFINED
            }
        }
    }

    data class ProductInstance(
        val id: String,
        val cardId: String,
        val frozenState: TangemPayCardFrozenState,
    )

    data class CardInfo(
        val lastFourDigits: String,
        val balance: BigDecimal,
        val currencyCode: String,
        val depositAddress: String?,
        val isPinSet: Boolean,
        val fiatBalance: PaymentAccountStatusValue.FiatBalance,
        val cryptoBalance: PaymentAccountStatusValue.CryptoBalance,
    )
}