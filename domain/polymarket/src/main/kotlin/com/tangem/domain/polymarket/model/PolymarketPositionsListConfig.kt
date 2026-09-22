package com.tangem.domain.polymarket.model

/** Immutable identity of a paged list; no request may borrow the currently selected wallet implicitly. */
data class PolymarketPositionsListConfig(val addresses: PolymarketAddresses, val tab: PolymarketPositionsTab)

enum class PolymarketPositionsTab { ACTIVE, FINISHED }