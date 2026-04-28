package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.PaymentAccountStatusValue.Loaded
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable
import java.math.BigDecimal

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
            is Loaded -> TotalFiatBalance.Loaded(amount = fiatBalance.availableBalance, source = source)
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
            is UnderReview -> copy(source = source)
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
     * Represents a state where the payment account is successfully loaded with complete information.
     *
     * @property source The source of the status information.
     * @property customerId The unique identifier of the customer.
     * @property currencyCode The code of the currency.
     * @property depositAddress The address for deposits, if available.
     * @property fiatBalance The fiat balance details.
     * @property cryptoBalance The crypto balance details.
     * @property cards The list of user's cards.
     */
    @Serializable
    data class Loaded(
        override val source: StatusSource,
        val customerId: String,
        val currencyCode: String,
        val depositAddress: String?,
        val fiatBalance: FiatBalance,
        val cryptoBalance: CryptoBalance,
        val cryptoCurrency: CryptoCurrency.Token,
        val cards: List<TangemPayCard>,
    ) : PaymentAccountStatusValue() {
        val cryptoCurrencyStatus: CryptoCurrencyStatus = CryptoCurrencyStatus(
            currency = cryptoCurrency,
            value = CryptoCurrencyStatus.Loaded(
                amount = cryptoBalance.balance,
                fiatAmount = fiatBalance.availableBalance,
                fiatRate = BigDecimal.ONE,
                priceChange = BigDecimal.ZERO,
                networkAddress = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        type = NetworkAddress.Address.Type.Primary,
                        value = cryptoBalance.depositAddress,
                    ),
                ),
                sources = CryptoCurrencyStatus.Sources(),
                pendingTransactions = emptySet(),
                stakingBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
            ),
        )
    }

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

fun Loaded.hasCardWithId(cardId: String): Boolean = cards.any { it.id == cardId }

fun Loaded.findCardWithId(cardId: String): TangemPayCard? = cards.firstOrNull { it.id == cardId }

fun Loaded.requireCardWithId(cardId: String): TangemPayCard = requireNotNull(findCardWithId(cardId))