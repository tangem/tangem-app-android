package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.features.polymarket.impl.common.polymarketOutcomeAccent
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal
import java.time.Instant

/**
 * Fills the header the user reads before paying.
 *
 * Seeds the price with the outcome's probability. Price polling updates it with the best ask while the amount
 * is empty; a placeable quote updates it with the average execution price after an amount is entered.
 * A market that reports no probability keeps the initial caption empty rather than printing it as free.
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
            outcomeAccent = polymarketOutcomeAccent(
                outcomeIndex = market.outcomes.indexOf(outcome).takeIf { it >= 0 },
                outcome = outcome.title,
            ),
            outcomePriceCents = outcome.probability?.toCents(),
            resolutionDateMillis = market.endDate?.toEpochMillisOrNull(),
        ),
        isMarketAcceptingOrders = market.isAcceptingOrders,
    ).recomputeGate()

    private fun BigDecimal.toCents(): Int = movePointRight(2).toInt()

    private fun String.toEpochMillisOrNull(): Long? = runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()
}