package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

/**
 * Fills the header the user reads before paying.
 *
 * The price shown here is the outcome's probability, which is a caption and nothing more: it is a live best ask
 * without slippage that silently falls back to a stale price when the order book is unreachable. Everything the
 * order actually executes at comes from the quote. A market that reports no probability keeps the caption
 * empty, the way the feed and the details screen do, rather than printing it as free.
 */
internal class SetMarketTransformer(
    private val market: PolymarketMarket,
    private val outcome: PolymarketOutcome,
) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM = prevState.copy(
        market = MarketHeaderUM(
            title = market.title,
            imageUrl = market.imageUrl ?: market.iconUrl,
            outcomeTitle = outcome.title,
            outcomePriceCents = outcome.probability?.toCents(),
        ),
    ).recomputeGate()

    private fun BigDecimal.toCents(): Int = movePointRight(2).toInt()
}