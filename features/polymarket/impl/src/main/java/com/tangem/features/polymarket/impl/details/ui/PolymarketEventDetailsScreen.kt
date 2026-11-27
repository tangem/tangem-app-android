package com.tangem.features.polymarket.impl.details.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketDetailsMarketUM
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketDetailsOutcomeUM
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM
import com.tangem.features.polymarket.impl.common.toResolutionSourceUM
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketSubcategoryTabUM
import kotlinx.collections.immutable.persistentListOf

private const val KEY_SUBCATEGORIES = "subcategories"
private const val KEY_CLOSED_CHIP = "closed_chip"

@Composable
internal fun PolymarketEventDetailsScreen(
    state: PolymarketEventDetailsUM,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    chart: (@Composable () -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val content = state as? PolymarketEventDetailsUM.Content
    PolymarketEventDetailsScaffold(
        modifier = modifier,
        listState = listState,
        title = content?.title,
        categoryLabel = content?.categoryLabel,
        onBackClick = onBackClick,
        icon = { EventIcon(iconUrl = content?.iconUrl, size = 40.dp) },
    ) { contentPadding ->
        when (state) {
            PolymarketEventDetailsUM.Loading -> EventDetailsLoadingState(modifier = Modifier.fillMaxSize())
            is PolymarketEventDetailsUM.Content -> ContentState(
                modifier = Modifier.fillMaxSize(),
                state = state,
                listState = listState,
                contentPadding = contentPadding,
                chart = chart,
            )
            is PolymarketEventDetailsUM.Error -> EventDetailsErrorState(
                modifier = Modifier.fillMaxSize(),
                onRetryClick = state.onRetryClick,
            )
        }
    }
}

@Composable
private fun ContentState(
    state: PolymarketEventDetailsUM.Content,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    chart: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = EVENT_DETAILS_HEADER_KEY) {
            EventHeader(state = state)
        }

        if (chart != null) {
            item(key = "chart") { chart() }
        }

        if (state.subcategories.isNotEmpty()) {
            item(key = KEY_SUBCATEGORIES) {
                SubcategoryBar(subcategories = state.subcategories)
            }
        }

        marketCards(markets = state.activeMarkets)

        if (state.closedMarkets.isNotEmpty()) {
            if (state.activeMarkets.isNotEmpty()) {
                item(key = KEY_CLOSED_CHIP) {
                    ClosedMarketsChip(
                        count = state.closedMarkets.size,
                        onClick = state.onClosedMarketsClick,
                    )
                }
            }

            if (state.isClosedMarketsExpanded) {
                marketCards(markets = state.closedMarkets)
            }
        }

        summarySection(state = state)
    }
}

@Composable
internal fun EventDetailsLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        TangemLoader(
            color = TangemTheme.colors3.icon.primary,
            size = TangemLoaderSize.X32,
        )
    }
}

@Composable
internal fun EventDetailsErrorState(onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResourceSafe(R.string.common_something_went_wrong),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
        TangemButton(
            modifier = Modifier.padding(top = 16.dp),
            size = TangemButton.Size.X10,
            variant = TangemButton.Variant.Primary,
            text = resourceReference(R.string.common_retry),
            onClick = onRetryClick,
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 780)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 780,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketEventDetailsScreenPreview() {
    TangemThemePreviewRedesign {
        PolymarketEventDetailsScreen(
            state = previewContent(),
            onBackClick = {},
        )
    }
}

private fun previewContent(): PolymarketEventDetailsUM.Content {
    return PolymarketEventDetailsUM.Content(
        title = stringReference("Who will win FIFA World Cup 2026 in the USA?"),
        categoryLabel = stringReference("Sports"),
        iconUrl = null,
        totalVolume = stringReference("\$6.3M"),
        change24h = PriceChangeState.Content(valueInPercent = "2.08%", type = PriceChangeType.UP),
        subcategories = persistentListOf(
            PolymarketSubcategoryTabUM(id = "lines", label = "Game lines", isSelected = true, onClick = {}),
            PolymarketSubcategoryTabUM(id = "score", label = "Exact score", isSelected = false, onClick = {}),
            PolymarketSubcategoryTabUM(id = "halves", label = "Halves", isSelected = false, onClick = {}),
        ),
        activeMarkets = persistentListOf(
            previewMarket(id = "france", title = "France", volume = "\$300K"),
            previewMarket(id = "germany", title = "Germany", volume = "\$250K"),
            previewMarket(id = "argentina", title = "Argentina", volume = "\$6.3M"),
        ),
        closedMarkets = persistentListOf(
            previewMarket(id = "closed", title = "Qatar", volume = "\$120K", withOutcomes = false),
        ),
        isClosedMarketsExpanded = false,
        description = stringReference(
            "This market will resolve to the team that is officially declared the winner " +
                "of the 2026 FIFA World Cup final by the official governing body.",
        ),
        isDescriptionExpanded = false,
        resolutionDate = stringReference("Jul 11, 2026, 6:00 PM ET"),
        marketOpenedDate = stringReference("Jul 11, 2026, 6:00 PM ET"),
        resolutionSource = "https://www.fifa.com".toResolutionSourceUM(),
        onShareClick = {},
        onClosedMarketsClick = {},
        onReadMoreClick = {},
    )
}

private fun previewMarket(id: String, title: String, volume: String, withOutcomes: Boolean = true) =
    PolymarketDetailsMarketUM(
        id = id,
        title = stringReference(title),
        volume = stringReference(volume),
        iconUrl = null,
        outcomes = if (withOutcomes) {
            persistentListOf(
                PolymarketDetailsOutcomeUM(
                    assetId = "yes",
                    title = stringReference("Yes"),
                    price = stringReference("25¢"),
                    onClick = {},
                ),
                PolymarketDetailsOutcomeUM(
                    assetId = "no",
                    title = stringReference("No"),
                    price = stringReference("74¢"),
                    onClick = {},
                ),
            )
        } else {
            persistentListOf()
        },
    )