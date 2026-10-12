package com.tangem.data.yield.supply.incentives.converters

import com.tangem.domain.yield.supply.models.incentives.YieldClaimHistoryItem
import com.tangem.grow.datasource.yield.models.incentives.YieldClaimHistoryEntryDto
import com.tangem.grow.datasource.yield.models.incentives.YieldClaimHistoryResponse
import kotlinx.datetime.Instant
import java.math.BigInteger

internal object YieldClaimHistoryConverter {

    fun convert(response: YieldClaimHistoryResponse): List<YieldClaimHistoryItem> {
        return response.claims.orEmpty().map(::convertEntry)
    }

    private fun convertEntry(dto: YieldClaimHistoryEntryDto): YieldClaimHistoryItem {
        return YieldClaimHistoryItem(
            tokenAddress = dto.tokenAddress.orEmpty(),
            amount = dto.amount?.toBigIntegerOrNull() ?: BigInteger.ZERO,
            timestamp = Instant.fromEpochSeconds(dto.timestamp ?: 0L),
            blockNumber = dto.blockNumber ?: 0L,
            campaignId = dto.campaignId.orEmpty(),
            isLatest = dto.isLatest == true,
        )
    }
}