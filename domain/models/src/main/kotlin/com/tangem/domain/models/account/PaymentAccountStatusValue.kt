package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.kyc.KycStatus
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
            is Error.CardIssueFailed -> if (balance != null && fiatRate != null) {
                totalFiatBalanceOf(balance = balance, fiatRate = fiatRate, source = source)
            } else {
                TotalFiatBalance.Loaded(amount = SerializedBigDecimal.ZERO, source = source)
            }
            is Error,
            is IssuingCard,
            is AwaitingPlanSelection,
            is Inactive,
            is Empty,
            is NotCreated,
            is UnderReview,
            -> TotalFiatBalance.Loaded(amount = SerializedBigDecimal.ZERO, source = source)
            is Loading -> TotalFiatBalance.Loading
            is Loaded -> totalFiatBalanceOf(balance = balance, fiatRate = fiatRate, source = source)
            is Deactivated -> totalFiatBalanceOf(balance = balance, fiatRate = fiatRate, source = source)
        }

    /**
     * Copies the status with a new [source].
     *
     * @param source The new source of the status information.
     */
    fun copySealed(source: StatusSource, error: Error? = null): PaymentAccountStatusValue {
        return when (this) {
            is Error.CardIssueFailed -> copy(source = source)
            is IssuingCard -> copy(source = source)
            is Loaded -> copy(source = source, error = error ?: this.error)
            is UnderReview -> copy(source = source)
            is Deactivated -> copy(source = source, error = error ?: this.error)
            is Loading,
            is AwaitingPlanSelection,
            is Inactive,
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
     * Represents a state where KYC is approved but no tariff plan has been selected yet
     *
     * @property source The source of the status information.
     * @property tariffPlan Current tariff plan
     */
    @Serializable
    data class AwaitingPlanSelection(
        override val source: StatusSource,
        val tariffPlan: TangemPayCustomerTariffPlan,
    ) : PaymentAccountStatusValue()

    /**
     * Represents a state where a tariff plan has been selected and the card is being issued
     *
     * @property source The source of the status information.
     * @property fiatBalance The fiat balance of state.
     * @property tariffPlan Current tariff plan
     */
    @Serializable
    data class Inactive(
        override val source: StatusSource,
        val fiatBalance: FiatBalance,
        val tariffPlan: TangemPayTariffPlanState,
    ) : PaymentAccountStatusValue()

    /**
     * Represents a state where the account is deactivated.
     *
     * @property source The source of the status information.
     * @property customerId The unique identifier of the customer.
     * @property balance The balance details, or `null` when the account exists but its balance is
     *                   unavailable — `customer/me` delivered no fiat balance and no cached one was found.
     * @property cryptoCurrency The crypto currency held by the deactivated account.
     * @property networks The blockchain networks attached to the account, each tagged by its issuance
     *                    status ([PaymentNetworkStatus]).
     * @property fiatRate Exchange rate of [cryptoCurrency] to the account's fiat currency,
     *                    or `null` if the quote is not yet available. When `null`,
     *                    [totalFiatBalance] resolves to [TotalFiatBalance.Failed].
     * @property error Transient error overlaid on top of cached data when a refresh fails
     *                 (see [copySealed]), or `null` when the status is up to date. Not persisted.
     */
    @Serializable
    data class Deactivated(
        override val source: StatusSource,
        val customerId: String,
        val balance: Balance?,
        val cryptoCurrency: CryptoCurrency.Token,
        val networks: List<PaymentNetworkStatus>,
        val fiatRate: SerializedBigDecimal?,
        val error: Error?,
    ) : PaymentAccountStatusValue() {

        val cryptoCurrencyStatuses: List<CryptoCurrencyStatus>
            get() = networks.filterIsInstance<PaymentNetworkStatus.Available>()
                .flatMap { it.cryptoCurrencyStatuses }

        val cryptoCurrencyStatus: CryptoCurrencyStatus?
            get() = cryptoCurrencyStatuses.singleCurrencyStatus(cryptoCurrency)

        val availableForWithdrawal: SerializedBigDecimal
            get() = cryptoCurrencyStatuses.maxAvailableForWithdrawal()
    }

    /**
     * Represents a state where the payment account is successfully loaded with complete information.
     *
     * @property source The source of the status information.
     * @property customerId The unique identifier of the customer.
     * @property paymentAccountAddress On-chain address of the payment account itself, or `null` when the
     *                                 backend has not provisioned one yet.
     * @property balance The balance details, or `null` when the account exists but its balance is
     *                   unavailable — `customer/me` delivered no fiat balance and no cached one was found.
     *                   The fiat currency code is available via [Balance.fiatBalance].
     * @property cryptoCurrency The crypto currency held by the account.
     * @property networks The blockchain networks attached to the account, each tagged by its issuance
     *                    status ([PaymentNetworkStatus]).
     * @property cards The list of user's cards.
     * @property fiatRate Exchange rate of [cryptoCurrency] to the account's fiat currency,
     *                    or `null` if the quote is not yet available. When `null`,
     *                    [totalFiatBalance] resolves to [TotalFiatBalance.Failed].
     * @property error Transient error overlaid on top of cached data when a refresh fails
     *                 (see [copySealed]), or `null` when the status is up to date. Not persisted.
     * @property virtualAccount Virtual Account (Visa on-ramp) availability — VA MVP0 (TWI-1638), or `null`
     *                          when not applicable (feature toggle off / wallet not eligible).
     *                          Transient: not persisted in the local cache.
     * @property tariffPlan Current tariff plan with subscription data (Tiers).
     *                      Transient: not persisted in the local cache.
     */
    @Serializable
    data class Loaded(
        override val source: StatusSource,
        val customerId: String,
        val paymentAccountAddress: String?,
        val balance: Balance?,
        val cryptoCurrency: CryptoCurrency.Token,
        val networks: List<PaymentNetworkStatus>,
        val cards: List<TangemPayCard>,
        val fiatRate: SerializedBigDecimal?,
        val error: Error?,
        val virtualAccount: VirtualAccountOnramp?,
        val tariffPlan: TangemPayTariffPlanState?,
    ) : PaymentAccountStatusValue() {

        val cryptoCurrencyStatuses: List<CryptoCurrencyStatus>
            get() = networks.filterIsInstance<PaymentNetworkStatus.Available>()
                .flatMap { it.cryptoCurrencyStatuses }

        val cryptoCurrencyStatus: CryptoCurrencyStatus?
            get() = cryptoCurrencyStatuses.singleCurrencyStatus(cryptoCurrency)

        val availableForWithdrawal: SerializedBigDecimal
            get() = cryptoCurrencyStatuses.maxAvailableForWithdrawal()
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

        /** Error state indicating that card issuance failed. */
        @Serializable
        data class CardIssueFailed(
            val customerId: String,
            override val source: StatusSource = StatusSource.ACTUAL,
            val tariffPlan: TangemPayTariffPlanState? = null,
            val balance: Balance? = null,
            val networks: List<PaymentNetworkStatus> = emptyList(),
            val fiatRate: SerializedBigDecimal? = null,
        ) : Error()
    }

    /**
     * Aggregates all balance data of a payment account, as returned by the `customer/me` endpoint.
     *
     * @property fiatBalance The fiat balance details.
     */
    @Serializable
    data class Balance(val fiatBalance: FiatBalance)

    /**
     * Represents the fiat balance of the payment account.
     *
     * @property availableBalance The amount of available balance in fiat.
     * @property currency The currency of the balance.
     */
    @Serializable
    data class FiatBalance(val availableBalance: SerializedBigDecimal, val currency: String)

    private fun totalFiatBalanceOf(
        balance: Balance?,
        fiatRate: SerializedBigDecimal?,
        source: StatusSource,
    ): TotalFiatBalance {
        if (balance == null || fiatRate == null) return TotalFiatBalance.Failed
        return TotalFiatBalance.Loaded(
            amount = balance.fiatBalance.availableBalance.multiply(fiatRate),
            source = source,
        )
    }
}

private fun List<CryptoCurrencyStatus>.singleCurrencyStatus(currency: CryptoCurrency.Token): CryptoCurrencyStatus? =
    firstOrNull { it.currency.network.id == currency.network.id } ?: firstOrNull()

private fun List<CryptoCurrencyStatus>.maxAvailableForWithdrawal(): SerializedBigDecimal =
    mapNotNull { it.value.amount }.maxOrNull() ?: BigDecimal.ZERO

val PaymentAccountStatusValue.tariffPlan: TangemPayCustomerTariffPlan?
    get() = when (val value = this) {
        is PaymentAccountStatusValue.Error.CardIssueFailed -> value.tariffPlan?.tariff
        is PaymentAccountStatusValue.Error,
        is PaymentAccountStatusValue.IssuingCard,
        is PaymentAccountStatusValue.Empty,
        is PaymentAccountStatusValue.NotCreated,
        is PaymentAccountStatusValue.UnderReview,
        is PaymentAccountStatusValue.Loading,
        is PaymentAccountStatusValue.Deactivated,
        -> null
        is PaymentAccountStatusValue.Inactive -> value.tariffPlan.tariff
        is PaymentAccountStatusValue.AwaitingPlanSelection -> value.tariffPlan
        is PaymentAccountStatusValue.Loaded -> value.tariffPlan?.tariff
    }

fun PaymentAccountStatusValue.hasAccountData(): Boolean = this is PaymentAccountStatusValue.Loaded ||
    this is PaymentAccountStatusValue.Deactivated ||
    this is PaymentAccountStatusValue.Error.CardIssueFailed && balance != null

/** Balances of an account-bearing status, or `null` when the status has none (or they are unavailable). */
val PaymentAccountStatusValue.balanceOrNull: PaymentAccountStatusValue.Balance?
    get() = when (this) {
        is PaymentAccountStatusValue.Loaded -> balance
        is PaymentAccountStatusValue.Deactivated -> balance
        is PaymentAccountStatusValue.Error.CardIssueFailed -> balance
        else -> null
    }

fun PaymentAccountStatusValue.Loaded.hasCardWithId(cardId: String): Boolean = cards.any { it.id == cardId }

fun PaymentAccountStatusValue.Loaded.findCardWithId(cardId: String): TangemPayCard? {
    return cards.firstOrNull { it.id == cardId }
}

fun PaymentAccountStatusValue.Loaded.requireCardWithId(cardId: String): TangemPayCard {
    return requireNotNull(findCardWithId(cardId))
}