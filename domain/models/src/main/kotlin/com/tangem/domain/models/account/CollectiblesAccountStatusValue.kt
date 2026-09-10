package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Represents the states a collectibles account can have: the gacha vault does not exist on chain yet, exists and
 * answers to this device, exists and answers to another one, or could not be read.
 *
 * Like [PredictionAccountStatusValue], no state reports [TotalFiatBalance.Loading]: the wallet total is the sum of
 * every account's contribution and a single loading one short-circuits it, so a collectibles account that is still
 * fetching would stall the number the user already had. Loading contributes a loaded zero marked as
 * [StatusSource.CACHE] instead.
 *
 * @property source the source of the status information
 */
@Serializable
sealed class CollectiblesAccountStatusValue {

    abstract val source: StatusSource

    /** The total fiat balance associated with this status. */
    val totalFiatBalance: TotalFiatBalance
        get() = when (this) {
            is Loading,
            is NotOnboarded,
            is Onboarded,
            is UpdateRequired,
            is Error,
            -> TotalFiatBalance.Loaded(amount = SerializedBigDecimal.ZERO, source = source)
        }

    /**
     * Copies the status with a new [source].
     *
     * @param source the new source of the status information
     */
    fun copySealed(source: StatusSource): CollectiblesAccountStatusValue {
        return when (this) {
            is Onboarded -> copy(source = source)
            is UpdateRequired -> copy(source = source)
            is Loading,
            is NotOnboarded,
            is Error,
            -> this
        }
    }

    /** The vault state is being read for the first time. */
    @Serializable
    data object Loading : CollectiblesAccountStatusValue() {
        override val source: StatusSource = StatusSource.CACHE
    }

    /** The wallet has no gacha vault on chain yet. */
    @Serializable
    data object NotOnboarded : CollectiblesAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /** The vault exists and its `hot_delegate` is the key this device holds, so it can sign spins. */
    @Serializable
    data class Onboarded(override val source: StatusSource) : CollectiblesAccountStatusValue()

    /**
     * The vault exists but its `hot_delegate` is some other key — the app was reinstalled, or the delegate was
     * rotated on another device. Spins fail with error 2001 until `update_vault` points the vault back at this
     * device, which needs a signature from the vault's owner.
     */
    @Serializable
    data class UpdateRequired(override val source: StatusSource) : CollectiblesAccountStatusValue()

    /** Represents an error state for the collectibles account status. */
    @Serializable
    sealed class Error : CollectiblesAccountStatusValue() {

        /** The state of the vault could not be obtained. */
        @Serializable
        data object Unavailable : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }
    }
}

/**
 * Whether the wallet has a collectibles account at all — the vault exists on chain, whichever device it currently
 * answers to. `null` is the absence of any record, which is not an account.
 */
val CollectiblesAccountStatusValue?.isAccountCreated: Boolean
    get() = this is CollectiblesAccountStatusValue.Onboarded || this is CollectiblesAccountStatusValue.UpdateRequired