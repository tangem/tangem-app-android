package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.getResultStatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
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
                val rate = fiatRate ?: return TotalFiatBalance.Failed
                val total = totalWithPositions()

                TotalFiatBalance.Loaded(amount = total.amount.multiply(rate), source = total.source)
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
     * The deposit wallet exists, but reading its collateral needs the locally derived L2 credentials, which a wallet
     * set up elsewhere does not have here, and which are dropped once the exchange rejects them. The amount is unknown
     * rather than zero, and the state lasts until the user reconnects, so contributing [TotalFiatBalance.Failed] would
     * blank the wallet total for as long as it holds.
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
     * @property balance The collateral balance of the deposit wallet, without the open positions.
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
        val fiatRate: SerializedBigDecimal?,
        val depositCurrency: CryptoCurrency.Token,
        val depositAddress: String?,
        val positions: PredictionPositionsValue = PredictionPositionsValue.Unknown,
    ) : PredictionAccountStatusValue() {

        /**
         * What the funding flows act on: the token a deposit is made in, at the bridge address that credits
         * this account. The swap takes its destination from [CryptoCurrencyStatus.value]'s network address, and
         * so does the receive sheet. `null` while the deposit address is unknown, which is the only state in
         * which neither may be offered. The amount is the trading balance, which is denominated in the
         * exchange's own settlement token rather than in [depositCurrency].
         */
        val cryptoCurrencyStatus: CryptoCurrencyStatus?
            get() = depositAddress?.let { address -> depositStatus(currency = depositCurrency, address = address) }

        /**
         * @param currency a token the bridge accepts besides [depositCurrency]
         * @param address the bridge address of [currency]'s chain family
         */
        fun depositStatus(currency: CryptoCurrency, address: String): CryptoCurrencyStatus = CryptoCurrencyStatus(
            currency = currency,
            value = depositCryptoCurrencyStatusValue(
                amount = balance,
                fiatRate = fiatRate,
                depositAddress = address,
                source = source,
            ),
        )

        fun totalWithPositions(): PredictionTotalBalance = when (positions) {
            is PredictionPositionsValue.Known -> PredictionTotalBalance.of(status = this, positions = positions)
            is PredictionPositionsValue.Unknown -> collateralOnly(positionsSource = StatusSource.CACHE)
            is PredictionPositionsValue.Unavailable -> collateralOnly(positionsSource = StatusSource.ONLY_CACHE)
        }

        private fun collateralOnly(positionsSource: StatusSource) = PredictionTotalBalance(
            amount = balance,
            source = listOf(source, positionsSource).getResultStatusSource(),
        )
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