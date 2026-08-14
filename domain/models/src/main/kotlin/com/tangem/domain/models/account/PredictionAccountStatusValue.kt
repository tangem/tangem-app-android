package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Represents the various states a prediction account can have, encapsulating different information based on
 * the state. Mirrors [VirtualAccountStatusValue], but carries the states of a prediction deposit wallet:
 * it is either absent, being set up, or holding a collateral balance.
 *
 * Unlike the payment and virtual accounts, no state of this one reports [TotalFiatBalance.Loading]: the wallet
 * total is the sum of every account's contribution, and a single loading contribution short-circuits it, so a
 * prediction account that is still fetching would stall the number the user already had. Loading contributes a
 * loaded zero marked as [StatusSource.CACHE] instead — the total stays visible and stays flagged as not final.
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
            is Error,
            -> TotalFiatBalance.Loaded(amount = SerializedBigDecimal.ZERO, source = source)
            is Active -> {
                val rate = fiatRate ?: return TotalFiatBalance.Failed
                TotalFiatBalance.Loaded(amount = balance.multiply(rate), source = source)
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
     * @property balance The collateral balance of the deposit wallet.
     * @property fiatRate Exchange rate of the collateral to the app's selected fiat currency, or `null` if the
     *                    quote has never been available. When `null`, [totalFiatBalance] resolves to
     *                    [TotalFiatBalance.Failed].
     * @property isTradingAllowed Whether the user may trade, as opposed to only viewing and withdrawing.
     */
    @Serializable
    data class Active(
        override val source: StatusSource,
        val balance: SerializedBigDecimal,
        val fiatRate: SerializedBigDecimal?,
        val isTradingAllowed: Boolean,
    ) : PredictionAccountStatusValue()

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