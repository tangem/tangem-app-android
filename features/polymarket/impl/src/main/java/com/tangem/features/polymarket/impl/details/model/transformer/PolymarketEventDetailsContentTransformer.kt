package com.tangem.features.polymarket.impl.details.model.transformer

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.features.polymarket.impl.common.PolymarketEventFormatting.formatEasternDate
import com.tangem.features.polymarket.impl.common.PolymarketEventFormatting.transformChange24h
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeButtonTitle
import com.tangem.features.polymarket.impl.common.formatPolymarketPrice
import com.tangem.features.polymarket.impl.common.formatPolymarketVolume
import com.tangem.features.polymarket.impl.common.toResolutionSourceUM
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketDetailsMarketUM
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketDetailsOutcomeUM
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Builds the event-details screen content for [event]. The previous state is not read — the content
 * is rebuilt from the domain event and the fold flags alone.
 *
 * @property event event to show
 * @property isClosedMarketsExpanded whether the closed-markets fold is open
 * @property isDescriptionExpanded whether the description is shown in full
 * @property onShareClick called with the event slug
 * @property onOutcomeClick called with the market id and the outcome asset id
 * @property onClosedMarketsClick toggles the closed-markets section
 * @property onReadMoreClick expands the description
 */
@Suppress("LongParameterList")
internal class PolymarketEventDetailsContentTransformer(
    private val event: PolymarketEvent,
    private val isClosedMarketsExpanded: Boolean,
    private val isDescriptionExpanded: Boolean,
    private val onShareClick: (String) -> Unit,
    private val onOutcomeClick: (marketId: String, assetId: String) -> Unit,
    private val onClosedMarketsClick: () -> Unit,
    private val onReadMoreClick: () -> Unit,
    private val onResolutionSourceClick: (String) -> Unit = {},
    private val categoryLabel: String? = null,
    private val isTeamAbbreviationEnabled: Boolean = false,
) : Transformer<PolymarketEventDetailsUM> {

    override fun transform(prevState: PolymarketEventDetailsUM): PolymarketEventDetailsUM {
        val markets = event.markets.sortedBy(PolymarketMarket::orderIndex)
        val activeMarkets = markets
            .filter { it.status == PolymarketStatus.ACTIVE || it.status == PolymarketStatus.UNKNOWN }
        return PolymarketEventDetailsUM.Content(
            title = stringReference(event.title),
            categoryLabel = categoryLabel?.takeIf(String::isNotBlank)?.let(::stringReference),
            iconUrl = event.iconUrl,
            totalVolume = stringReference(event.volume.formatPolymarketVolume()),
            change24h = transformChange24h(volume = event.volume, volume24h = event.volume24h),
            // The BFF does not serve subcategories yet; the UI hides the row while the list is empty.
            subcategories = persistentListOf(),
            activeMarkets = activeMarkets
                .map { transformMarket(market = it, isTradable = true) }
                .toImmutableList(),
            closedMarkets = markets
                .filter { it.status == PolymarketStatus.CLOSED }
                .map { transformMarket(market = it, isTradable = false) }
                .toImmutableList(),
            isClosedMarketsExpanded = isClosedMarketsExpanded || activeMarkets.isEmpty(),
            description = event.description.takeIf(String::isNotBlank)?.let(::stringReference),
            isDescriptionExpanded = isDescriptionExpanded,
            resolutionDate = stringReference(formatEasternDate(raw = event.endDate)),
            marketOpenedDate = stringReference(formatEasternDate(raw = event.startDate)),
            resolutionSource = event.resolutionSource.toResolutionSourceUM(),
            onResolutionSourceClick = onResolutionSourceClick,
            onShareClick = { onShareClick(event.slug) },
            onClosedMarketsClick = onClosedMarketsClick,
            onReadMoreClick = onReadMoreClick,
        )
    }

    private fun transformMarket(market: PolymarketMarket, isTradable: Boolean): PolymarketDetailsMarketUM {
        val titles = PolymarketOutcomeButtonTitle.convert(
            market.outcomes.map { it.title },
            event.teams,
            isTeamAbbreviationEnabled,
        )
        return PolymarketDetailsMarketUM(
            id = market.id,
            title = transformMarketTitle(market),
            volume = stringReference(market.volume.formatPolymarketVolume()),
            iconUrl = market.iconUrl ?: event.iconUrl,
            outcomes = if (isTradable) {
                market.outcomes
                    .mapIndexed { index, outcome ->
                        transformOutcome(market = market, outcome = outcome, title = titles[index])
                    }
                    .toImmutableList()
            } else {
                persistentListOf()
            },
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

    private fun transformOutcome(
        market: PolymarketMarket,
        outcome: PolymarketOutcome,
        title: String,
    ): PolymarketDetailsOutcomeUM {
        return PolymarketDetailsOutcomeUM(
            assetId = outcome.assetId,
            title = stringReference(title),
            price = stringReference(outcome.probability.formatPolymarketPrice()),
            onClick = { onOutcomeClick(market.id, outcome.assetId) },
        )
    }
}