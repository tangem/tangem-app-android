package com.tangem.features.polymarket.impl.main.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.features.polymarket.impl.common.buyPrice
import com.tangem.features.polymarket.impl.common.probability
import com.tangem.features.polymarket.impl.common.isLiveForQuotes
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeButtonTitle
import com.tangem.features.polymarket.impl.common.formatPolymarketPrice
import com.tangem.features.polymarket.impl.common.formatPolymarketVolume
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
    private val isPhaseTwoEnabled: Boolean,
) : Converter<PolymarketEvent, PolymarketEventUM> {

    private val sportsConverter = PolymarketSportsUMConverter(isPhaseTwoEnabled, onOutcomeClick)

    override fun convert(value: PolymarketEvent): PolymarketEventUM = convert(value, sports = null)

    fun convert(
        value: PolymarketEvent,
        sports: PolymarketSportsState.Game?,
        quotes: Map<String, PolymarketLiveQuote> = emptyMap(),
    ): PolymarketEventUM {
        val game = sports?.takeIf { isPhaseTwoEnabled && it.gameId == value.gameId }
        val liveQuotes = quotes.takeIf { isPhaseTwoEnabled && value.isLiveForQuotes(game) }.orEmpty()
        val sportsRow = sportsConverter.outcomes(value, game, liveQuotes)
        return PolymarketEventUM(
            id = value.id,
            title = stringReference(value.title),
            iconUrl = value.iconUrl,
            volume = stringReference(value.volume.formatPolymarketVolume()),
            sports = game?.takeIf { sportsRow != null }?.let { sportsConverter.convert(it) },
            rows = sportsRow?.let(::listOf)?.toImmutableList() ?: value.markets
                .map { convertRow(event = value, market = it, quotes = liveQuotes) }
                .toImmutableList(),
            hiddenMarketsCount = (value.totalMarketsCount - value.markets.size).coerceAtLeast(minimumValue = 0),
            onClick = { onEventClick(value.id) },
        )
    }

    private fun convertRow(
        event: PolymarketEvent,
        market: PolymarketMarket,
        quotes: Map<String, PolymarketLiveQuote>,
    ): PolymarketEventRowUM {
        val shouldShowPrices = market.groupItemTitle.isNullOrBlank() ||
            market.groupItemTitle == MATCH_WINNER_GROUP_ITEM_TITLE
        val titles = PolymarketOutcomeButtonTitle.convert(
            market.outcomes.map { it.title },
            event.teams,
            isPhaseTwoEnabled,
        )
        return PolymarketEventRowUM(
            marketId = market.id,
            title = if (shouldShowPrices) {
                market.title.takeIf { event.totalMarketsCount > 1 }?.let(::stringReference)
            } else {
                convertRowTitle(event = event, market = market)
            },
            probability = if (shouldShowPrices) {
                null
            } else {
                stringReference(market.outcomes.firstOrNull()?.let { outcome ->
                    quotes[outcome.assetId].probability(outcome.probability)
                }.formatProbability())
            },
            outcomes = market.outcomes
                .mapIndexed { index, outcome ->
                    val price = if (shouldShowPrices) {
                        stringReference(quotes[outcome.assetId].buyPrice(outcome.probability).formatPolymarketPrice())
                    } else {
                        null
                    }
                    convertOutcome(
                        event = event,
                        market = market,
                        outcome = outcome,
                        title = titles[index],
                        price = price,
                    )
                }
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
        title: String,
        price: TextReference?,
    ): PolymarketOutcomeUM {
        return PolymarketOutcomeUM(
            assetId = outcome.assetId,
            title = stringReference(title),
            price = price,
            onClick = { onOutcomeClick(event.id, market.id, outcome.assetId) },
        )
    }

    private fun BigDecimal?.formatProbability(): String = format {
        percent(minFractionDigits = 0, maxFractionDigits = 0)
    }

    private companion object {
        const val MATCH_WINNER_GROUP_ITEM_TITLE = "Match Winner"
    }
}