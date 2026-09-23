package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Represents the various states a prediction account can have, encapsulating different information based on
 * the state. Mirrors [MoneyAccountStatusValue], but carries the states of a prediction deposit wallet:
 * it is either absent, being set up, or holding a collateral balance.
 *
 * Unlike the payment and money accounts, no state of this one reports [TotalFiatBalance.Loading]: the wallet
 * total is the sum of every account's contribution, and a single loading or failed contribution short-circuits
 * it, so this account alone would stall or blank the number the user already had. A state whose amount this
 * device cannot read contributes a loaded zero instead — the total omits the collateral rather than hiding the
 * rest of the wallet, and the row on the wallet screen reports the amount as unknown next to it. Loading marks
 * its zero as [StatusSource.CACHE], so the total also reads as not final.
 *
 * @property source The source of the status information.
 */
@Serializable
sealed class PredictionAccountStatusValue {

    abstract val source: StatusSource

    /**
     * The collateral priced in the app's currency, or `null` when it cannot be priced. The wallet total and the
     * row on the wallet screen both read it, so neither can multiply differently from the other.
     */
    val fiatBalance: SerializedBigDecimal?
        get() {
            val active = this as? Active ?: return null

            return active.fiatRate?.let(active.balance::multiply)
        }

    /** The total fiat balance associated with this status. */
    val totalFiatBalance: TotalFiatBalance
        get() = when (this) {
            is Loading,
            is NotOnboarded,
            is Onboarding,
            is Onboarded,
            is Error,
            -> TotalFiatBalance.Loaded(amount = SerializedBigDecimal.ZERO, source = source)
            is Active -> {
                val amount = fiatBalance ?: return TotalFiatBalance.Failed
                TotalFiatBalance.Loaded(amount = amount, source = source)
            }
        }

    /**
     * Copies the status with a new [source].
     *
     * @param source The new source of the status information.
     */
    fun copySealed(source: StatusSource): PredictionAccountStatusValue {
        return when (this) {
            is Onboarding -> copy(source = source)
            is Onboarded -> copy(source = source)
            is Active -> copy(source = source)
            is Loading,
            is NotOnboarded,
            is Error,
            -> this
        }
    }

    /** Represents the state of a prediction account whose details are being fetched for the first time. */
    @Serializable
    data object Loading : PredictionAccountStatusValue() {
        override val source: StatusSource = StatusSource.CACHE
    }

    /** Represents a state where the user has no prediction deposit wallet yet. */
    @Serializable
    data object NotOnboarded : PredictionAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /**
     * The deposit wallet exists, but reading its collateral needs the locally derived L2 credentials, which a
     * wallet set up elsewhere does not have here. The amount is unknown rather than zero, and the state lasts
     * until the user reconnects, so contributing [TotalFiatBalance.Failed] would blank the wallet total for as
     * long as it holds.
     */
    @Serializable
    data class Onboarded(override val source: StatusSource) : PredictionAccountStatusValue()

    /**
     * Represents a state where the prediction deposit wallet is being set up and cannot hold funds yet.
     *
     * @property source The source of the status information.
     * @property stage The stage the setup has reached.
     */
    @Serializable
    data class Onboarding(override val source: StatusSource, val stage: Stage) : PredictionAccountStatusValue() {

        /** The stages a prediction deposit wallet goes through before it is ready to trade. */
        @Serializable
        enum class Stage {

            /** The deposit wallet contract is being deployed. */
            DEPLOYING,

            /** The deposit wallet is deployed, its trading approvals are not granted yet. */
            DEPLOYED,

            /** The trading approvals are being granted. */
            APPROVING,
        }
    }

    /**
     * Represents a state where the prediction deposit wallet is ready and holds a collateral balance.
     *
     * @property source The source of the status information.
     * @property balance The collateral balance of the deposit wallet, which is what the wallet total counts.
     * @property positionsValue The last value of the open positions a full refresh read, or `null` until one has.
     *                           A failed read keeps the previous value, so it can be older than [source] says.
     * @property fiatRate Exchange rate of the collateral to the app's selected fiat currency, or `null` if the
     *                    quote has never been available. When `null`, [totalFiatBalance] resolves to
     *                    [TotalFiatBalance.Failed].
     * @property depositCurrency The token a deposit is made in, which is not the token [balance] is
     *                           denominated in.
     * @property depositAddress Address a deposit is sent to for it to credit this account, or `null` until the
     *                          bridge has confirmed one.
     */
    @Serializable
    data class Active(
        override val source: StatusSource,
        val balance: SerializedBigDecimal,
        val positionsValue: SerializedBigDecimal?,
        val fiatRate: SerializedBigDecimal?,
        val depositCurrency: CryptoCurrency.Token,
        val depositAddress: String?,
    ) : PredictionAccountStatusValue() {

        /** The collateral plus the open positions, or `null` while the positions value is unknown. */
        val totalBalance: SerializedBigDecimal?
            get() = positionsValue?.let(balance::add)

        /**
         * What the funding flows act on: the token a deposit is made in, at the bridge address that credits
         * this account. The swap takes its destination from [CryptoCurrencyStatus.value]'s network address, and
         * so does the receive sheet. `null` while the deposit address is unknown, which is the only state in
         * which neither may be offered. The amount is the trading balance, which is denominated in the
         * exchange's own settlement token rather than in [depositCurrency].
         */
        val cryptoCurrencyStatus: CryptoCurrencyStatus?
            get() = depositAddress?.let { address ->
                CryptoCurrencyStatus(
                    currency = depositCurrency,
                    value = buildCollateralStatusValue(
                        amount = balance,
                        fiatRate = fiatRate,
                        depositAddress = address,
                        source = source,
                    ),
                )
            }
    }

    /** Represents an error state for the prediction account status. */
    @Serializable
    sealed class Error : PredictionAccountStatusValue() {

        /** Error state indicating that the setup of the deposit wallet did not complete. */
        @Serializable
        data object OnboardingFailed : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }

        /** Error state indicating that the state of the deposit wallet could not be obtained. */
        @Serializable
        data object Unavailable : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }
    }
}

private fun buildCollateralStatusValue(
    amount: SerializedBigDecimal,
    fiatRate: SerializedBigDecimal?,
    depositAddress: String,
    source: StatusSource,
): CryptoCurrencyStatus.Value {
    val networkAddress = NetworkAddress.Single(
        defaultAddress = NetworkAddress.Address(
            type = NetworkAddress.Address.Type.Primary,
            value = depositAddress,
        ),
    )
    val sources = CryptoCurrencyStatus.Sources(
        networkSource = source,
        quoteSource = source,
        stakingBalanceSource = source,
    )

    return if (fiatRate != null) {
        CryptoCurrencyStatus.Loaded(
            amount = amount,
            fiatAmount = amount.multiply(fiatRate),
            fiatRate = fiatRate,
            priceChange = SerializedBigDecimal.ZERO,
            networkAddress = networkAddress,
            sources = sources,
            pendingTransactions = emptySet(),
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
        )
    } else {
        CryptoCurrencyStatus.NoQuote(
            amount = amount,
            networkAddress = networkAddress,
            stakingBalance = null,
            yieldSupplyStatus = null,
            sources = sources,
            pendingTransactions = emptySet(),
            hasCurrentNetworkTransactions = false,
        )
    }
}