package com.tangem.data.yield.supply.incentives.converters

import com.tangem.domain.yield.supply.models.incentives.YieldAprHistory
import com.tangem.grow.datasource.yield.models.incentives.YieldAprHistoryPointDto
import com.tangem.grow.datasource.yield.models.incentives.YieldAprHistoryResponse
import com.tangem.utils.extensions.orZero
import kotlinx.datetime.LocalDate

internal object YieldAprHistoryConverter {

    fun convert(response: YieldAprHistoryResponse): YieldAprHistory {
        return YieldAprHistory(
            chainId = response.chainId ?: -1,
            tokenAddress = response.tokenAddress.orEmpty(),
            points = response.points.orEmpty().map(::convertPoint),
        )
    }

    private fun convertPoint(dto: YieldAprHistoryPointDto): YieldAprHistory.Point {
        return YieldAprHistory.Point(
            date = LocalDate.parse(dto.date.orEmpty()),
            baseApy = dto.baseApy.orZero(),
            rewardsApr = dto.rewardsApr.orZero(),
            totalApyEst = dto.totalApyEst.orZero(),
            isCarriedForward = dto.isCarriedForward == true,
        )
    }
}