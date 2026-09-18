package com.tangem.domain.polymarket.model

import com.tangem.pagination.PaginationStatus

data class PolymarketPositionsState(
    val positions: List<PolymarketPosition> = emptyList(),
    val status: PaginationStatus<List<PolymarketPosition>> = PaginationStatus.None,
    val filter: PolymarketPositionsFilter = PolymarketPositionsFilter.ALL,
)