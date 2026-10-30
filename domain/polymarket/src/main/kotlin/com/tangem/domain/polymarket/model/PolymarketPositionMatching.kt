package com.tangem.domain.polymarket.model

/**
 * The market a position trades on: the one with the position's condition id, else the one whose outcome carries
 * the position's token. Condition ids are compared ignoring case, and an empty id matches nothing.
 */
fun PolymarketEvent.findMarket(position: PolymarketPosition.Open): PolymarketMarket? =
    position.id.conditionId.takeIf { it.isNotEmpty() }
        ?.let { conditionId -> markets.firstOrNull { it.conditionId.equals(conditionId, ignoreCase = true) } }
        ?: markets.firstOrNull { it.findOutcome(position.id.assetId) != null }

/**
 * The outcome a position holds: the one trading under the position's token today or before the market moved to
 * V2, else the one at the index the BFF serves with the position. An exact token wins over the index because the
 * index is positional and says nothing about which token it names.
 */
fun PolymarketMarket.findOutcome(position: PolymarketPosition.Open): PolymarketOutcome? =
    findOutcome(position.id.assetId) ?: position.outcomeIndex?.let(outcomes::getOrNull)

/**
 * The outcome trading under [assetId] today, or the one that did before its market moved to V2. An empty id
 * matches nothing.
 */
fun PolymarketMarket.findOutcome(assetId: String): PolymarketOutcome? {
    if (assetId.isEmpty()) return null
    return outcomes.firstOrNull { it.assetId == assetId } ?: outcomes.firstOrNull { it.legacyAssetId == assetId }
}