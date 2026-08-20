package com.tangem.domain.pay.model

import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
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
    val paymentAccount: PaymentAccount?,
    val productInstances: List<ProductInstance>,
    val cards: List<CardInfo>,
    val kycStatus: KycStatus,
    val state: State,
    val fiatBalance: PaymentAccountStatusValue.FiatBalance?,
    val cryptoBalance: PaymentAccountStatusValue.CryptoBalance?,
    val availableForWithdrawal: BigDecimal,
    val tariffPlan: TangemPayCustomerTariffPlan?,
    val networks: List<NetworkInfo> = emptyList(),
    val country: String? = null,
    val phoneMask: String? = null,
    val email: String? = null,
) {

    /** Transitional single-card accessor — returns the first product instance, or null if none. */
    val productInstance: ProductInstance? get() = productInstances.firstOrNull()

    /** Transitional single-card accessor — returns the first card, or null if none. */
    val cardInfo: CardInfo? get() = cards.firstOrNull()

    /** Card-level product instances only (excludes the VA ACCOUNT instance). */
    val cardProductInstances: List<ProductInstance>
        get() = productInstances.filter { it.specificationDataType == ProductInstance.SpecificationDataType.CARD }

    /**
     * Card-level product instances the customer is enrolled with — the backend-side proof that the payment
     * account exists, independent of balances and of the `cards[]` payload.
     */
    val activeCardProductInstances: List<ProductInstance>
        get() = cardProductInstances.filter {
            it.status == ProductInstance.Status.ACTIVE || it.status == ProductInstance.Status.BLOCKED
        }

    /** Whether the response carried both balance dimensions. */
    val hasBalances: Boolean get() = fiatBalance != null && cryptoBalance != null

    /**
     * Backend-side proof that the payment account exists: an enrolled card instance, a payment account, or
     * balances (only an existing account has them). Deliberately independent of the `cards[]` payload, which
     * the backend can omit for an operational account.
     */
    val isEnrolled: Boolean
        get() = activeCardProductInstances.isNotEmpty() ||
            paymentAccount != null ||
            hasBalances

    /**
     * Payment account attached to the customer, as delivered by `customer/me`.payment_account.
     *
     * @property id payment account identifier.
     * @property address payment account address on chain, or `null` when not provisioned yet.
     * @property customerWalletAddress address of the wallet that owns the account.
     */
    data class PaymentAccount(
        val id: String,
        val address: String?,
        val customerWalletAddress: String,
    )

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
        val images: List<TangemPayTariffPlan.Image>,
    )

    /**
     * Raw multichain network as delivered by `customer/me`.networks[] — the transport used by the data
     * layer to build the domain [com.tangem.domain.models.account.PaymentNetworkStatus].
     */
    data class NetworkInfo(
        val name: String,
        val chainId: Long,
        val isTestnet: Boolean,
        val status: Status,
        val depositAddress: String?,
        val tokens: List<Token>,
    ) {
        enum class Status {
            ENABLED,
            NOT_ISSUED,
            DISABLED,
            ;

            companion object {
                /** Maps a backend status string; unknown/absent -> [DISABLED] (info-only, never receive). */
                fun fromWire(raw: String?): Status = when (raw?.uppercase(Locale.US)) {
                    "ENABLED" -> ENABLED
                    "NOT_ISSUED" -> NOT_ISSUED
                    "DISABLED" -> DISABLED
                    else -> DISABLED
                }
            }
        }

        data class Token(
            val symbol: String,
            val contractAddress: String,
            val availableForWithdrawal: BigDecimal?,
        )
    }
}