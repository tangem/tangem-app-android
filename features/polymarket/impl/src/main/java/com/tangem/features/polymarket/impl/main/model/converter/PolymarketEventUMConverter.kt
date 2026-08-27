package com.tangem.features.polymarket.impl.main.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.compact
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventRowUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketOutcomeUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

/**
 * Converts a [PolymarketEvent] into the card shown in the Discovery feed.
 *
 * @property onEventClick called with the event id
 * @property onOutcomeClick called with the event id, the market id and the outcome asset id
 */
internal class PolymarketEventUMConverter(
    private val onEventClick: (String) -> Unit,
    private val onOutcomeClick: (eventId: String, marketId: String, assetId: String) -> Unit,
) : Converter<PolymarketEvent, PolymarketEventUM> {

    override fun convert(value: PolymarketEvent): PolymarketEventUM {
        return PolymarketEventUM(
            id = value.id,
            title = stringReference(value.title),
            iconUrl = value.iconUrl,
            volume = value.volume?.let { stringReference(it.formatVolume()) },
            rows = value.markets
                .map { convertRow(event = value, market = it) }
                .toImmutableList(),
            hiddenMarketsCount = (value.totalMarketsCount - value.markets.size).coerceAtLeast(minimumValue = 0),
            onClick = { onEventClick(value.id) },
        )
    }

    private fun convertRow(event: PolymarketEvent, market: PolymarketMarket): PolymarketEventRowUM {
        return PolymarketEventRowUM(
            marketId = market.id,
            title = convertRowTitle(event = event, market = market),
            probability = market.outcomes.firstOrNull()?.probability?.let { stringReference(it.formatProbability()) },
            outcomes = market.outcomes
                .map { convertOutcome(event = event, market = market, outcome = it) }
                .toImmutableList(),
        )
    }

    /**
     * A grouped event competes its markets against each other, so a market is labelled by its group item title.
     *
     * A plain event holds independent markets: a single one is already asked by the card title, so its row needs no
     * label of its own, while several markets must each name their own question.
     */
    private fun convertRowTitle(event: PolymarketEvent, market: PolymarketMarket): TextReference {
        return when (event.displayMode) {
            PolymarketDisplayMode.GROUPED_OUTCOMES -> stringReference(market.groupItemTitle ?: market.title)
            PolymarketDisplayMode.PLAIN_MARKETS -> if (event.markets.size > 1) {
                stringReference(market.title)
            } else {
                resourceReference(R.string.prediction_event_probability)
            }
        }
    }

    private fun convertOutcome(
        event: PolymarketEvent,
        market: PolymarketMarket,
        outcome: PolymarketOutcome,
    ): PolymarketOutcomeUM {
        return PolymarketOutcomeUM(
            assetId = outcome.assetId,
            title = stringReference(outcome.title),
            onClick = { onOutcomeClick(event.id, market.id, outcome.assetId) },
        )
    }

    /** Polymarket denominates volumes in USD regardless of the currency selected in the app. */
    private fun BigDecimal.formatVolume(): String = format {
        fiat(fiatCurrencyCode = USD_CODE, fiatCurrencySymbol = USD_SYMBOL).compact()
    }

    private fun BigDecimal.formatProbability(): String = format {
        percent(minFractionDigits = 0, maxFractionDigits = 0)
    }

    private companion object {
        const val USD_CODE = "USD"
        const val USD_SYMBOL = "$"
    }
}