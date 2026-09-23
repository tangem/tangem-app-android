package com.tangem.data.polymarket.pagination

import com.tangem.domain.polymarket.model.PolymarketPositionsError

internal class PolymarketPositionsReadException(val error: PolymarketPositionsError) : Exception(error.toString())