package com.tangem.domain.pay.model

import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardLimit
import java.math.BigDecimal
import java.util.Locale

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
    val productInstances: List<ProductInstance>,
    val cards: List<CardInfo>,
    val kycStatus: KycStatus,
    val state: State,
    val fiatBalance: PaymentAccountStatusValue.FiatBalance?,
    val cryptoBalance: PaymentAccountStatusValue.CryptoBalance?,
    val availableForWithdrawal: BigDecimal,
    val tariffPlan: TangemPayCustomerTariffPlan?,
) {

    /** Transitional single-card accessor — returns the first product instance, or null if none. */
    val productInstance: ProductInstance? get() = productInstances.firstOrNull()

    /** Transitional single-card accessor — returns the first card, or null if none. */
    val cardInfo: CardInfo? get() = cards.firstOrNull()

    enum class State {
        NEW,
        ACTIVE,
        BLOCKED,
        FORMER,
        IN_PROGRESS,
        UNDEFINED,
        ;

        companion object {
            fun fromString(value: String) = when (value.uppercase(Locale.US)) {
                "NEW" -> NEW
                "ACTIVE" -> ACTIVE
                "BLOCKED" -> BLOCKED
                "FORMER" -> FORMER
                "IN_PROGRESS" -> IN_PROGRESS
                else -> UNDEFINED
            }
        }
    }

    data class ProductInstance(
        val id: String,
        val cardId: String,
        val frozenState: TangemPayCardFrozenState,
        val displayName: CardDisplayName?,
        val actualCardLimit: TangemPayCardLimit?,
        val adminCardLimit: TangemPayCardLimit?,
        val status: Status,
        val specificationDataType: SpecificationDataType,
    ) {
        enum class Status {
            NEW,
            READY_FOR_MANUFACTURING,
            MANUFACTURING,
            SENT_TO_DELIVERY,
            DELIVERED,
            ACTIVATING,
            ACTIVE,
            BLOCKED,
            DEACTIVATING,
            DEACTIVATED,
            CANCELED,
            UNKNOWN,
        }

        /** `ACCOUNT` marks a Virtual Account instance (vs. a `CARD`); used by VA MVP0 (TWI-1638). */
        enum class SpecificationDataType {
            ACCOUNT,
            CARD,
        }
    }

    data class CardInfo(
        /** Card identifier — matches [ProductInstance.cardId] to join a card to its product instance. */
        val cardId: String,
        val cardStatus: TangemPayCard.Status,
        val lastFourDigits: String,
        val isPinSet: Boolean,
    )
}