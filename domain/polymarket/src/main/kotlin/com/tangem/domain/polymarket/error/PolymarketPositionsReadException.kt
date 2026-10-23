package com.tangem.domain.polymarket.error

import com.tangem.domain.polymarket.model.PolymarketPositionsError

/** Carries a failed positions page read through the pagination layer, whose contract is a throwable. */
class PolymarketPositionsReadException(val error: PolymarketPositionsError) : Exception(error.toString())