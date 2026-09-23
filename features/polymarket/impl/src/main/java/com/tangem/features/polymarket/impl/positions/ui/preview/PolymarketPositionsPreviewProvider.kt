package com.tangem.features.polymarket.impl.positions.ui.preview

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketPositionsFilter
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionUM
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.Content
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.NextPage
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.Tab
import kotlinx.collections.immutable.toImmutableList

private const val LONG_LIST_SIZE = 20

internal data class PolymarketPositionsPreviewCase(
    val name: String,
    val selectedTab: Tab = Tab.Active,
    val active: Content = Content.Empty,
    val finished: Content = Content.Empty,
    val selectedFilter: PolymarketPositionsFilter = PolymarketPositionsFilter.ALL,
)

internal class PolymarketPositionsPreviewProvider :
    CollectionPreviewParameterProvider<PolymarketPositionsPreviewCase>(screenCases()) {

    override fun getDisplayName(index: Int): String = values.elementAt(index).name
}

private fun screenCases(): List<PolymarketPositionsPreviewCase> {
    val active = Content.Data(positions = activePositions())
    val finished = Content.Data(positions = finishedPositions())
    return listOf(
        PolymarketPositionsPreviewCase("Active", active = active, finished = finished),
        PolymarketPositionsPreviewCase(
            name = "Finished",
            selectedTab = Tab.Finished,
            active = active,
            finished = finished,
        ),
        PolymarketPositionsPreviewCase("Initial loading", active = Content.Loading),
        PolymarketPositionsPreviewCase("Empty Active"),
        PolymarketPositionsPreviewCase("Empty Finished", selectedTab = Tab.Finished),
        PolymarketPositionsPreviewCase("Initial error", active = Content.Error(onRetry = {})),
        PolymarketPositionsPreviewCase(
            "Loading next page",
            active = active.copy(nextPage = NextPage.Loading),
        ),
        PolymarketPositionsPreviewCase(
            "Next page error",
            active = active.copy(nextPage = NextPage.Error(onRetry = {})),
        ),
        PolymarketPositionsPreviewCase(
            "Long lists - reset scroll on tab change",
            active = active.copy(
                positions = List(LONG_LIST_SIZE) {
                    active.positions[it % active.positions.size].copy(id = "active_$it")
                }.toImmutableList(),
                nextPage = NextPage.Ready(onLoadMore = {}),
            ),
            finished = finished.copy(
                positions = List(LONG_LIST_SIZE) {
                    finished.positions[it % finished.positions.size].copy(id = "finished_$it")
                }.toImmutableList(),
            ),
        ),
    ) + filteredCases(active)
}

private fun filteredCases(active: Content.Data): List<PolymarketPositionsPreviewCase> {
    val won = active.copy(
        positions = active.positions.take(1).map { position ->
            position.copy(
                result = TangemBadgeUM(
                    text = resourceReference(R.string.prediction_positions_won),
                    status = TangemBadge.Status.Success,
                ),
            )
        }.toImmutableList(),
    )
    return listOf(
        PolymarketPositionsPreviewCase("Won filter", active = won, selectedFilter = PolymarketPositionsFilter.WON),
        PolymarketPositionsPreviewCase("No matches", selectedFilter = PolymarketPositionsFilter.LOST),
        PolymarketPositionsPreviewCase(
            "Looking for matches",
            active = Content.Loading,
            selectedFilter = PolymarketPositionsFilter.PENDING,
        ),
        PolymarketPositionsPreviewCase(
            "Filtered error",
            active = Content.Error(onRetry = {}),
            selectedFilter = PolymarketPositionsFilter.DISPUTED,
        ),
        PolymarketPositionsPreviewCase(
            "Partial matches loading",
            active = won.copy(nextPage = NextPage.Loading),
            selectedFilter = PolymarketPositionsFilter.WON,
        ),
        PolymarketPositionsPreviewCase(
            "Partial matches error",
            active = won.copy(nextPage = NextPage.Error(onRetry = {})),
            selectedFilter = PolymarketPositionsFilter.WON,
        ),
    )
}

private fun activePositions() = listOf("France", "Argentina", "Uzbekistan").mapIndexed { index, team ->
    PolymarketPositionUM(
        id = "active_$index",
        title = stringReference("Will $team win the 2026 FIFA World Cup?"),
        iconUrl = null,
        outcomeLabel = stringReference(if (index == 1) "No" else "Yes"),
        outcomeVariant = if (index == 1) TangemMessageBubble.Variant.Error else TangemMessageBubble.Variant.Info,
        priceChange = TangemPriceChange.State(
            value = stringReference("$12.40 (1.10%)"),
            direction = TangemPriceChange.Direction.Up,
        ),
        currentValue = stringReference("$1,245.00"),
        result = null,
    )
}.toImmutableList()

private fun finishedPositions() = finishedPositionCards().mapIndexed { index, case ->
    case.state.copy(id = "finished_$index")
}.toImmutableList()