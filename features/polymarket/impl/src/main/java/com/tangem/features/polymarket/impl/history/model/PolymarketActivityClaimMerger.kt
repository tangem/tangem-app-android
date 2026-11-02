package com.tangem.features.polymarket.impl.history.model

import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.domain.polymarket.model.PolymarketActivity.RedeemResult

internal object PolymarketActivityClaimMerger {

    fun merge(items: List<PolymarketActivity>): List<PolymarketActivity> {
        val claims = items.groupBy { it.claimKey() }
            .filterKeys { it != null }
            .mapValues { (_, rows) ->
                val winner = rows.firstOrNull { it.redeemResult == RedeemResult.WON }
                if (rows.size > 1 && winner != null) {
                    rows.first().copy(
                        amount = rows.sumOf { it.amount },
                        market = winner.market,
                        redeemResult = RedeemResult.WON,
                    )
                } else {
                    null
                }
            }
        val emitted = mutableSetOf<ClaimKey>()
        return items.mapNotNull { activity ->
            val key = activity.claimKey()
            val claim = claims[key]
            when {
                key == null || claim == null -> activity
                emitted.add(key) -> claim
                else -> null
            }
        }
    }

    private fun PolymarketActivity.claimKey(): ClaimKey? {
        if (type != PolymarketActivity.Type.REDEEM) return null
        val hash = transactionHash?.takeIf { it.isNotBlank() } ?: return null
        val conditionId = market?.conditionId?.takeIf { it.isNotBlank() } ?: return null
        return ClaimKey(hash.lowercase(), conditionId.lowercase())
    }

    private data class ClaimKey(val transactionHash: String, val conditionId: String)
}