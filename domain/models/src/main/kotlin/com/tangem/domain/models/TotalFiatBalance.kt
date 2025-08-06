package com.tangem.domain.models

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Represents the possible states of the fiat balance, including loading, failure, or a loaded amount
 */
@Serializable
sealed interface TotalFiatBalance {

    /**
     * Represents the loading state of the fiat balance.
     * This state indicates that the fiat balance is currently being retrieved or calculated.
     */
    @Serializable
    data object Loading : TotalFiatBalance

    /**
     * Represents the failure state of the fiat balance.
     * This state indicates that an attempt to retrieve or calculate the fiat balance has failed.
     */
    @Serializable
    data object Failed : TotalFiatBalance

    /**
     * Represents the successfully loaded state of the fiat balance.
     *
     * @property amount                 the loaded fiat balance amount
     * @property isAllAmountsSummarized indicates whether the amount includes a summary of all underlying amounts
     */
    @Serializable
    data class Loaded(
        val amount: SerializedBigDecimal,
        val isAllAmountsSummarized: Boolean,
        val source: StatusSource,
    ) : TotalFiatBalance
}