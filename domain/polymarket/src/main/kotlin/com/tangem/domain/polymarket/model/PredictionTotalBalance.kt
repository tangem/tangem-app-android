package com.tangem.domain.polymarket.model

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.getResultStatusSource
import java.math.BigDecimal

/**
 * @property amount the collateral plus the open positions
 * @property source the less fresh of the two sources [amount] is made of
 */
data class PredictionTotalBalance(val amount: BigDecimal, val source: StatusSource) {

    companion object {

        /** @return `null` while the positions value has never been read */
        fun of(
            status: PredictionAccountStatusValue.Active,
            positions: PredictionPositionsValue,
        ): PredictionTotalBalance? {
            if (positions !is PredictionPositionsValue.Known) return null

            return PredictionTotalBalance(
                amount = status.balance + positions.amount,
                source = listOf(status.source, positions.source).getResultStatusSource(),
            )
        }
    }
}