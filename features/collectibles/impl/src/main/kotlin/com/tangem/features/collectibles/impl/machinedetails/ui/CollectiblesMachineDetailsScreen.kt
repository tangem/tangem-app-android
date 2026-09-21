package com.tangem.features.collectibles.impl.machinedetails.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.collectibles.impl.R
import com.tangem.features.collectibles.impl.common.ui.CollectiblesLoadMoreEffect
import com.tangem.features.collectibles.impl.common.ui.CollectiblesStatus
import com.tangem.features.collectibles.impl.machinedetails.ui.components.*
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun CollectiblesMachineDetailsScreen(
    state: CollectiblesMachineDetailsUM,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var footerHeight by remember { mutableIntStateOf(0) }

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                title = (state as? CollectiblesMachineDetailsUM.Content)?.title ?: TextReference.EMPTY,
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onBack = state.onBackClick,
            )
        },
        overlay = {
            if (state is CollectiblesMachineDetailsUM.Content) {
                CollectiblesPackDetailsFooter(
                    state = state.footer,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { size -> footerHeight = size.height },
                )
            }
        },
    ) { contentPadding ->
        val bottomInset = with(LocalDensity.current) { footerHeight.toDp() } + 12.dp

        when (state) {
            is CollectiblesMachineDetailsUM.Loading -> CollectiblesPackDetailsSkeleton(
                contentPadding = PaddingValues(top = contentPadding.calculateTopPadding()),
            )
            is CollectiblesMachineDetailsUM.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CollectiblesStatus(message = state.message, onReloadClick = state.onReloadClick)
            }
            is CollectiblesMachineDetailsUM.Content -> MachineDetailsInfoList(
                state = state,
                onLoadMore = onLoadMore,
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    bottom = bottomInset,
                ),
            )
        }
    }
}

@Composable
private fun MachineDetailsInfoList(
    state: CollectiblesMachineDetailsUM.Content,
    onLoadMore: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    CollectiblesLoadMoreEffect(
        listState = listState,
        isEnabled = state.prizePool is CollectiblesMachineDetailsUM.PrizePoolUM.Content,
        onLoadMore = onLoadMore,
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
    ) {
        item(key = PackDetailsKeys.HERO) {
            CollectiblesPackHero(imageUrl = state.imageUrl)
        }
        item(key = PackDetailsKeys.TIERS) {
            Column {
                SpacerH(32.dp)
                CollectiblesSectionHeading(
                    title = resourceReference(R.string.collectibles_machine_value_and_odds_section),
                )
                CardGrid(items = state.tiers) { tier -> CollectiblesTierCard(state = tier) }
            }
        }
        item(key = PackDetailsKeys.DETAILS) {
            Column {
                SpacerH(32.dp)
                CollectiblesSectionHeading(title = resourceReference(R.string.collectibles_machine_details_section))
                CardGrid(items = state.details) { detail -> CollectiblesDetailCard(state = detail) }
                PoweredBy()
            }
        }
        item(key = PackDetailsKeys.PRIZE_POOL_HEADING) {
            SpacerH(32.dp)
            CollectiblesSectionHeading(title = resourceReference(R.string.collectibles_machine_prize_pool_section))
        }

        prizePool(state = state.prizePool)
    }
}

private fun LazyListScope.prizePool(state: CollectiblesMachineDetailsUM.PrizePoolUM) {
    when (state) {
        is CollectiblesMachineDetailsUM.PrizePoolUM.Loading -> item(key = PackDetailsKeys.PRIZE_POOL_STATUS) {
            CollectiblesPrizeGridSkeleton()
        }
        is CollectiblesMachineDetailsUM.PrizePoolUM.Error -> item(key = PackDetailsKeys.PRIZE_POOL_STATUS) {
            CollectiblesStatus(
                message = state.message,
                onReloadClick = state.onReloadClick,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
        is CollectiblesMachineDetailsUM.PrizePoolUM.Empty -> item(key = PackDetailsKeys.PRIZE_POOL_STATUS) {
            CollectiblesStatus(
                message = state.message,
                onReloadClick = null,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
        is CollectiblesMachineDetailsUM.PrizePoolUM.Content -> {
            items(items = state.rows, key = { row -> row.id }, contentType = { row -> row::class.java }) { row ->
                PrizeRow(row = row)
            }

            if (state.isLoadingNextPage) {
                item(key = PackDetailsKeys.PRIZE_POOL_NEXT_PAGE) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TangemLoader()
                    }
                }
            }
        }
    }
}

@Composable
private fun PrizeRow(row: CollectiblesMachineDetailsUM.PrizeRowUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        row.prizes.forEach { prize ->
            CollectiblesPrizeCard(state = prize, modifier = Modifier.weight(1f))
        }
        repeat(PRIZES_PER_ROW - row.prizes.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/** The 2-up card grid the odds and the detail blocks share. */
@Composable
private fun <T> CardGrid(items: ImmutableList<T>, modifier: Modifier = Modifier, card: @Composable (T) -> Unit) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.chunked(CARDS_PER_ROW).forEach { chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chunk.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) { card(item) }
                }
                repeat(CARDS_PER_ROW - chunk.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PoweredBy(modifier: Modifier = Modifier) {
    Text(
        text = resourceReference(
            id = R.string.collectibles_machine_powered_by,
            formatArgs = wrappedList(
                styledStringReference(
                    value = "Collector Crypt",
                    spanStyleReference = { SpanStyle(color = TangemTheme.colors3.text.primary) },
                ),
            ),
        ).resolveAnnotatedReference(),
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
    )
}

private const val CARDS_PER_ROW = 2
private const val PRIZES_PER_ROW = 2

private object PackDetailsKeys {
    const val HERO = "hero"
    const val TIERS = "tiers"
    const val DETAILS = "details"
    const val PRIZE_POOL_HEADING = "prizePoolHeading"
    const val PRIZE_POOL_STATUS = "prizePoolStatus"
    const val PRIZE_POOL_NEXT_PAGE = "prizePoolNextPage"
}