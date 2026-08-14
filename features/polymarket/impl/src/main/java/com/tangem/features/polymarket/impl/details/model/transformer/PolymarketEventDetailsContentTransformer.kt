package com.tangem.features.polymarket.impl.details.model.transformer

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.features.polymarket.impl.common.formatPolymarketVolume
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketDetailsMarketUM
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketDetailsOutcomeUM
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Builds the event-details screen content for [event]. The previous state is not read — the content
 * is rebuilt from the domain event alone.
 *
 * @property event event to show
 * @property onShareClick called with the event slug
 * @property onOutcomeClick called with the market id and the outcome asset id
 */
internal class PolymarketEventDetailsContentTransformer(
    private val event: PolymarketEvent,
    private val onShareClick: (String) -> Unit,
    private val onOutcomeClick: (marketId: String, assetId: String) -> Unit,
) : Transformer<PolymarketEventDetailsUM> {

    override fun transform(prevState: PolymarketEventDetailsUM): PolymarketEventDetailsUM {
        return PolymarketEventDetailsUM.Content(
            title = stringReference(event.title),
            iconUrl = event.iconUrl,
            totalVolume = event.volume?.let { stringReference(it.formatPolymarketVolume()) },
            change24h = formatChange24h(volume = event.volume, volume24h = event.volume24h),
            // The BFF does not serve subcategories yet; the UI hides the row while the list is empty.
            subcategories = persistentListOf(),
            activeMarkets = event.markets
                .sortedBy(PolymarketMarket::orderIndex)
                .filter { it.status == PolymarketStatus.ACTIVE || it.status == PolymarketStatus.UNKNOWN }
                .map(::transformMarket)
                .toImmutableList(),
            onShareClick = { onShareClick(event.slug) },
        )
    }

    private fun transformMarket(market: PolymarketMarket): PolymarketDetailsMarketUM {
        return PolymarketDetailsMarketUM(
            id = market.id,
            title = transformMarketTitle(market),
            volume = market.volume?.let { stringReference(it.formatPolymarketVolume()) },
            iconUrl = market.iconUrl ?: event.iconUrl,
            outcomes = market.outcomes
                .map { transformOutcome(market = market, outcome = it) }
                .toImmutableList(),
        )
    }

    /**
     * A grouped event competes its markets against each other, so a card is labelled by its group item title.
     * A plain event holds independent markets, each asking its own question.
     */
    private fun transformMarketTitle(market: PolymarketMarket): TextReference {
        return when (event.displayMode) {
            PolymarketDisplayMode.GROUPED_OUTCOMES -> stringReference(market.groupItemTitle ?: market.title)
            PolymarketDisplayMode.PLAIN_MARKETS -> stringReference(market.title)
        }
    }

    private fun transformOutcome(market: PolymarketMarket, outcome: PolymarketOutcome): PolymarketDetailsOutcomeUM {
        val price = outcome.probability?.formatCents()
        return PolymarketDetailsOutcomeUM(
            assetId = outcome.assetId,
            title = stringReference(if (price != null) "${outcome.title} • $price" else outcome.title),
            onClick = { onOutcomeClick(market.id, outcome.assetId) },
        )
    }

    /** An implied probability doubles as the outcome share price: 0.25 → "25¢". */
    private fun BigDecimal.formatCents(): String {
        val cents = multiply(CENTS_IN_DOLLAR).setScale(0, RoundingMode.HALF_UP)
        return "$cents$CENT_SIGN"
    }

    private fun formatChange24h(volume: BigDecimal?, volume24h: BigDecimal?): TextReference? {
        val ratio = computeChange24hRatio(volume = volume, volume24h = volume24h) ?: return null
        return stringReference(
            ratio.format { percent(minFractionDigits = 0, maxFractionDigits = 2) },
        )
    }

    companion object {
        private const val CENT_SIGN = "¢"
        private val CENTS_IN_DOLLAR = BigDecimal(100)

        /**
         * Growth ratio of the total traded volume over the last 24 hours, derived from the two
         * volumes the BFF serves: `volume24h / (volume - volume24h)`. `null` when either volume is
         * unknown or the event traded its entire volume within the window (no base to grow from).
         *
         * Note this is a volume delta — a price change would need historical probabilities, which
         * the BFF does not serve.
         */
        internal fun computeChange24hRatio(volume: BigDecimal?, volume24h: BigDecimal?): BigDecimal? {
            volume ?: return null
            volume24h ?: return null
            val base = volume - volume24h
            if (base <= BigDecimal.ZERO) return null
            return volume24h.divide(base, MathContext.DECIMAL64)
        }
    }
}