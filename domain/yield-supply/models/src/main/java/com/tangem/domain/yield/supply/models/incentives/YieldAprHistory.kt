package com.tangem.domain.yield.supply.models.incentives

import kotlinx.datetime.LocalDate
import java.math.BigDecimal

/** Daily incentivised APY history of a supplied token, ascending by date. */
data class YieldAprHistory(
    val chainId: Int,
    val tokenAddress: String,
    val points: List<Point>,
) {

    /** @property isCarriedForward the point repeats the previous day because the backend had a data gap */
    data class Point(
        val date: LocalDate,
        val baseApy: BigDecimal,
        val rewardsApr: BigDecimal,
        val totalApyEst: BigDecimal,
        val isCarriedForward: Boolean,
    )
}