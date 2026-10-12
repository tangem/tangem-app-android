package com.tangem.domain.polymarket.model

enum class PolymarketPositionsFilter(val status: PolymarketPositionStatus?) {
    ALL(null),
    ACTIVE(PolymarketPositionStatus.ACTIVE),
    WON(PolymarketPositionStatus.WON),
    LOST(PolymarketPositionStatus.LOST),
    DISPUTED(PolymarketPositionStatus.DISPUTED),
    PENDING(PolymarketPositionStatus.PENDING),
    SPLIT(PolymarketPositionStatus.SPLIT),
}