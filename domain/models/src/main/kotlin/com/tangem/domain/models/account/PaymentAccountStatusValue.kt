package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Represents the various states a payment account can have, encapsulating different information based on the state.
 *
 * @property source The source of the status information.
 */
@Serializable
sealed class PaymentAccountStatusValue {
    abstract val source: StatusSource

    /** The total fiat balance associated with this status. */
    val totalFiatBalance: TotalFiatBalance
        get() = when (this) {
            is Error,
            is IssuingCard,
            is Empty,
            is NotCreated,
            is UnderReview,
            -> TotalFiatBalance.Loaded(amount = SerializedBigDecimal.ZERO, source = source)
            is Loading -> TotalFiatBalance.Loading
            is Locked -> TotalFiatBalance.Loaded(amount = fiatBalance.availableBalance, source = source)
            is Loaded -> TotalFiatBalance.Loaded(amount = fiatBalance.availableBalance, source = source)
            is Deactivated -> TotalFiatBalance.Loaded(amount = fiatBalance.availableBalance, source = source)
        }

    /**
     * Copies the status with a new [source].
     *
     * @param source The new source of the status information.
     */
    fun copySealed(source: StatusSource): PaymentAccountStatusValue {
        return when (this) {
            is IssuingCard -> copy(source = source)
            is Loaded -> copy(source = source)
            is Locked -> copy(source = source)
            is UnderReview -> copy(source = source)
            is Deactivated -> copy(source = source)
            is Loading,
            is Empty,
            is NotCreated,
            is Error,
            -> this
        }
    }

    /** Represents an empty payment account status when no specific state is available. */
    @Serializable
    data object Empty : PaymentAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /** Represents the Loading state of a payment account, typically while fetching its details. */
    @Serializable
    data object Loading : PaymentAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /** Represents a state where the payment account has not been created yet. */
    @Serializable
    data object NotCreated : PaymentAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /**
     * Represents a state where the payment account is under review (KYC).
     *
     * @property source The source of the status information.
     * @property kycStatus The current KYC status.
     * @property customerId The unique identifier of the customer.
     */
    @Serializable
    data class UnderReview(
        override val source: StatusSource,
        val kycStatus: KycStatus,
        val customerId: String,
    ) : PaymentAccountStatusValue()

    /**
     * Represents a state where the card for the payment account is being issued.
     *
     * @property source The source of the status information.
     */
    @Serializable
    data class IssuingCard(override val source: StatusSource) : PaymentAccountStatusValue()

    /**
     * Represents a state where the account is deactivated.
     *
     * @property source The source of the status information.
     * @property fiatBalance The fiat balance details.
     */
    @Serializable
    data class Deactivated(
        override val source: StatusSource,
        val fiatBalance: FiatBalance,
    ) : PaymentAccountStatusValue()

    /**
     * Represents a state where the payment account is locked.
     *
     * @property source The source of the status information.
     * @property customerId The unique identifier of the customer.
     * @property cardId The unique identifier of the card.
     * @property lastFourDigits The last four digits of the card number.
     * @property currencyCode The code of the currency.
     * @property depositAddress The address for deposits, if available.
     * @property isPinSet Indicates if the PIN is set for the card.
     * @property fiatBalance The fiat balance details.
     * @property cryptoBalance The crypto balance details.
     */
    @Serializable
    data class Locked(
        override val source: StatusSource,
        val customerId: String,
        val cardId: String,
        val lastFourDigits: String,
        val currencyCode: String,
        val depositAddress: String?,
        val isPinSet: Boolean,
        val fiatBalance: FiatBalance,
        val cryptoBalance: CryptoBalance,
    ) : PaymentAccountStatusValue()

    /**
     * Represents a state where the payment account is successfully loaded with complete information.
     *
     * @property source The source of the status information.
     * @property customerId The unique identifier of the customer.
     * @property cardId The unique identifier of the card.
     * @property lastFourDigits The last four digits of the card number.
     * @property currencyCode The code of the currency.
     * @property depositAddress The address for deposits, if available.
     * @property isPinSet Indicates if the PIN is set for the card.
     * @property fiatBalance The fiat balance details.
     * @property cryptoBalance The crypto balance details.
     */
    @Serializable
    data class Loaded(
        override val source: StatusSource,
        val customerId: String,
        val cardId: String,
        val lastFourDigits: String,
        val currencyCode: String,
        val depositAddress: String?,
        val isPinSet: Boolean,
        val fiatBalance: FiatBalance,
        val cryptoBalance: CryptoBalance,
    ) : PaymentAccountStatusValue()

    /** Represents an error state for the payment account status. */
    @Serializable
    sealed class Error : PaymentAccountStatusValue() {
        /** Error state indicating the device is exposed. */
        @Serializable
        data object ExposedDevice : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }

        /** Error state indicating the account is unavailable. */
        @Serializable
        data object Unavailable : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }

        /** Error state indicating the account data is not synced. */
        @Serializable
        data object NotSynced : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }

        /**
         * Error state indicating that card issuance failed.
         *
         * @property customerId The unique identifier of the customer.
         */
        @Serializable
        data class CardIssueFailed(val customerId: String) : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }
    }

    /**
     * Represents the fiat balance of the payment account.
     *
     * @property availableBalance The amount of available balance in fiat.
     * @property currency The currency of the balance.
     */
    @Serializable
    data class FiatBalance(val availableBalance: SerializedBigDecimal, val currency: String)

    /**
     * Represents the crypto balance of the payment account.
     *
     * @property id The unique identifier of the crypto asset.
     * @property chainId The identifier of the blockchain network.
     * @property depositAddress The address for deposits.
     * @property tokenContractAddress The contract address of the token.
     * @property balance The amount of the crypto balance.
     */
    @Serializable
    data class CryptoBalance(
        val id: String,
        val chainId: Long,
        val depositAddress: String,
        val tokenContractAddress: String,
        val balance: SerializedBigDecimal,
    )
}