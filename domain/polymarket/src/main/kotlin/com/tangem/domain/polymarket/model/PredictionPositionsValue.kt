package com.tangem.domain.polymarket.model

import com.tangem.domain.models.StatusSource
import java.math.BigDecimal

sealed interface PredictionPositionsValue {

    data object Unknown : PredictionPositionsValue

    /**
     * @property amount the current value of every open position, zero when there are none
     * @property source [StatusSource.ONLY_CACHE] once a refresh has failed to re-read [amount]
     */
    data class Known(val amount: BigDecimal, val source: StatusSource) : PredictionPositionsValue
}