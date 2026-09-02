package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Represents the states a collectibles account can have: the gacha vault either does not exist on chain yet, or
 * its state could not be read.
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
            is Error,
            -> TotalFiatBalance.Loaded(amount = SerializedBigDecimal.ZERO, source = source)
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