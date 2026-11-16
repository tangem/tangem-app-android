package com.tangem.domain.polymarket.model

enum class PolymarketPositionsFilter(
    val status: PolymarketPositionStatus?,
    private val tabs: Set<PolymarketPositionsTab>,
) {
    ALL(null, BOTH_TABS),
    ACTIVE(PolymarketPositionStatus.ACTIVE, ACTIVE_TAB),
    WON(PolymarketPositionStatus.WON, BOTH_TABS),
    LOST(PolymarketPositionStatus.LOST, BOTH_TABS),
    DISPUTED(PolymarketPositionStatus.DISPUTED, ACTIVE_TAB),
    PENDING(PolymarketPositionStatus.PENDING, ACTIVE_TAB),
    SPLIT(PolymarketPositionStatus.SPLIT, BOTH_TABS),
    CLOSED(PolymarketPositionStatus.CLOSED, FINISHED_TAB),
    ;

    companion object {

        fun of(tab: PolymarketPositionsTab): List<PolymarketPositionsFilter> = entries.filter { tab in it.tabs }
    }
}

private val ACTIVE_TAB = setOf(PolymarketPositionsTab.ACTIVE)
private val FINISHED_TAB = setOf(PolymarketPositionsTab.FINISHED)
private val BOTH_TABS = PolymarketPositionsTab.entries.toSet()