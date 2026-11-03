package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

@Serializable
sealed interface PredictionPositionsValue {

    @Serializable
    data object Unknown : PredictionPositionsValue

    @Serializable
    data object Unavailable : PredictionPositionsValue

    /**
     * @property amount the current value of every open position, zero when there are none
     * @property source [StatusSource.ONLY_CACHE] once a refresh has failed to re-read [amount]
     */
    @Serializable
    data class Known(val amount: SerializedBigDecimal, val source: StatusSource) : PredictionPositionsValue
}