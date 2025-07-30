package com.tangem.domain.models.staking

import com.tangem.domain.models.StatusSource
import kotlinx.serialization.Serializable

/**
 * Represents a yield balance in the staking system
 */
@Serializable
sealed interface YieldBalance {

    /** The unique identifier of the staking operation */
    val stakingId: StakingID

    /** The source of the status information */
    val source: StatusSource

    /**
     * Represents a yield balance with actual data
     *
     * @property stakingId the unique identifier of the staking operation
     * @property source    the source of the status information
     * @property balance   the balance details of the yield
     */
    @Serializable
    data class Data(
        override val stakingId: StakingID,
        override val source: StatusSource,
        val balance: YieldBalanceItem,
    ) : YieldBalance

    /**
     * Represents an empty yield balance
     *
     * @property stakingId the unique identifier of the staking operation
     * @property source    the source of the status information
     */
    @Serializable
    data class Empty(
        override val stakingId: StakingID,
        override val source: StatusSource,
    ) : YieldBalance

    /**
     * Represents an error state for the yield balance
     *
     * @property stakingId the unique identifier of the staking operation
     */
    @Serializable
    data class Error(override val stakingId: StakingID) : YieldBalance {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /**
     * Creates a copy of the current yield balance with a new status source
     *
     * @param source the new source of the status information
     */
    fun copySealed(source: StatusSource): YieldBalance {
        return when (this) {
            is Data -> copy(source = source)
            is Empty -> copy(source = source)
            is Error,
            -> this
        }
    }
}