package com.tangem.features.earn.lending.impl.provider.aave

import arrow.core.Either
import arrow.core.right
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.yield.supply.models.incentives.YieldAprHistory
import com.tangem.domain.yield.supply.usecase.GetYieldAprHistoryUseCase
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnApyPoint
import com.tangem.features.earn.product.api.product.EarnPeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import java.math.BigDecimal
import java.math.MathContext
import javax.inject.Inject

internal class AaveApyHistoryLoader @Inject constructor(
    private val markets: AaveMarkets,
    private val getAprHistory: GetYieldAprHistoryUseCase,
) {

    suspend fun load(asset: LendingAsset, period: EarnPeriod): Either<Throwable, EarnApyHistory> {
        val market = markets.current(asset) ?: return EarnApyHistory(points = emptyList(), average = null).right()

        return getAprHistory(
            chainId = market.chainId,
            tokenAddress = asset.reserveToken.contractAddress,
            days = days(period),
        )
            .mapLeft { error -> IllegalStateException("Aave APY history failed: $error") }
            .map { history -> history.toEarnHistory() }
    }

    private fun days(period: EarnPeriod): Int = when (period.id) {
        AaveApyPeriods.Month.id -> MONTH_DAYS
        else -> MAX_DAYS
    }

    /** The same series as the market rate: with a running Merkl campaign it includes the rewards */
    private fun YieldAprHistory.toEarnHistory(): EarnApyHistory {
        val earnPoints = points.map { point ->
            EarnApyPoint(
                at = point.date.atStartOfDayIn(TimeZone.UTC),
                rate = point.totalApyEst.percentToApy(),
            )
        }
        val average = earnPoints.takeIf { it.isNotEmpty() }
            ?.map { it.rate.fraction }
            ?.reduce(BigDecimal::add)
            ?.divide(earnPoints.size.toBigDecimal(), MathContext.DECIMAL64)

        return EarnApyHistory(points = earnPoints, average = average)
    }

    private companion object {
        const val MONTH_DAYS = 30

        /** The backend keeps 90 days of history, so a longer period cannot be drawn */
        const val MAX_DAYS = 90
    }
}

internal object AaveApyPeriods {
    val Month = EarnPeriod(id = "1M", label = stringReference("1M"))
    val ThreeMonths = EarnPeriod(id = "3M", label = stringReference("3M"))
    val All = listOf(Month, ThreeMonths)
}