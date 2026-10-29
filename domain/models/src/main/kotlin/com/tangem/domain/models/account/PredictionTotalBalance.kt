package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.getResultStatusSource
import java.math.BigDecimal

/**
 * @property amount the collateral plus the open positions
 * @property source the less fresh of the two sources [amount] is made of
 */
data class PredictionTotalBalance(val amount: BigDecimal, val source: StatusSource) {

    companion object {

        /** @return `null` while the positions value is not known */
        fun of(
            status: PredictionAccountStatusValue.Active,
            positions: PredictionPositionsValue,
        ): PredictionTotalBalance? {
            return (positions as? PredictionPositionsValue.Known)?.let { of(status = status, positions = it) }
        }

        fun of(
            status: PredictionAccountStatusValue.Active,
            positions: PredictionPositionsValue.Known,
        ): PredictionTotalBalance {
            return PredictionTotalBalance(
                amount = status.balance + positions.amount,
                source = listOf(status.source, positions.source).getResultStatusSource(),
            )
        }
    }
}