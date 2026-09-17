package com.tangem.domain.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketClosedPositionStatus
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketPositionStatus
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.utils.converter.Converter

object PolymarketPositionStatusConverter : Converter<PolymarketPosition, PolymarketPositionStatus> {

    override fun convert(value: PolymarketPosition): PolymarketPositionStatus = when (value) {
        is PolymarketPosition.Open -> openStatus(value)
        is PolymarketPosition.Closed -> when (value.status) {
            PolymarketClosedPositionStatus.CLOSED -> PolymarketPositionStatus.CLOSED
            PolymarketClosedPositionStatus.WON -> PolymarketPositionStatus.WON
            PolymarketClosedPositionStatus.LOST -> PolymarketPositionStatus.LOST
            PolymarketClosedPositionStatus.SPLIT -> PolymarketPositionStatus.SPLIT
            PolymarketClosedPositionStatus.UNKNOWN -> PolymarketPositionStatus.UNKNOWN
        }
    }

    private fun openStatus(value: PolymarketPosition.Open): PolymarketPositionStatus = when (value.result) {
        PolymarketPositionResult.WON -> PolymarketPositionStatus.WON
        PolymarketPositionResult.LOST -> PolymarketPositionStatus.LOST
        PolymarketPositionResult.SPLIT -> PolymarketPositionStatus.SPLIT
        PolymarketPositionResult.UNKNOWN -> PolymarketPositionStatus.UNKNOWN
        null -> when (value.resolutionStatus) {
            PolymarketResolutionStatus.OPEN -> PolymarketPositionStatus.ACTIVE
            PolymarketResolutionStatus.PENDING_RESOLUTION,
            PolymarketResolutionStatus.PROPOSED,
            -> PolymarketPositionStatus.PENDING
            PolymarketResolutionStatus.DISPUTED -> PolymarketPositionStatus.DISPUTED
            PolymarketResolutionStatus.RESOLVED,
            PolymarketResolutionStatus.UNKNOWN,
            -> PolymarketPositionStatus.UNKNOWN
        }
    }
}