package com.tangem.features.earn.impl.info.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.earn.impl.info.ui.state.EarnInfoTab
import com.tangem.features.earn.impl.info.ui.state.EarnInfoUM
import com.tangem.features.earn.impl.picker.ui.PAGE_MARGIN
import kotlinx.collections.immutable.ImmutableList

/** The list is the haze source: the glass buttons of the overlays and the calculator's sheet blur it. */
@Composable
internal fun EarnInfoContent(state: EarnInfoUM?, chart: (@Composable () -> Unit)?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        if (state != null) {
            EarnInfoPage(state = state, chart = chart)
        }
    }
}

/**
 * The chips float over the list, outside its haze source, so their glass pill has something to blur; a sticky header
 * would stick behind the navigation (Foundation's StickToTopPlacement places it at -beforeContentPadding).
 */
@Composable
private fun EarnInfoPage(state: EarnInfoUM, chart: (@Composable () -> Unit)?) {
    val listState = rememberLazyListState()
    var navigationHeightPx by remember { mutableIntStateOf(0) }
    var chipsHeightPx by remember { mutableIntStateOf(0) }
    var footerHeightPx by remember { mutableIntStateOf(0) }
    val hasChart = chart != null
    val items = remember(state.staleNotice, state.rewards, state.tabs, hasChart) {
        buildPageItems(state = state, hasChart = hasChart)
    }
    val chipsIndex = remember(items) { items.indexOf(PageItem.Tabs) }
    val chips = rememberSectionChips(
        tabs = state.tabs,
        sectionIndices = remember(items) { items.sectionIndices() },
        listState = listState,
        chipsHeightPx = { chipsHeightPx },
    )
    val isChipsPinned by remember(chipsIndex) { derivedStateOf { listState.hasPinnedChips(chipsIndex) } }
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem()
                .background(TangemTheme.colors3.bg.primary),
            contentPadding = with(density) {
                PaddingValues(top = navigationHeightPx.toDp(), bottom = footerHeightPx.toDp())
            },
        ) {
            pageContent(
                items = items,
                state = state,
                chart = chart,
                chipsSpace = { ChipsSpace(heightPx = chipsHeightPx) },
            )
        }
        PinnedFade(isPinned = isChipsPinned, heightPx = navigationHeightPx + chipsHeightPx)
        FloatingChips(
            chips = chips,
            listState = listState,
            chipsIndex = chipsIndex,
            navigationHeightPx = { navigationHeightPx },
            onHeightChange = { chipsHeightPx = it },
        )
        TangemTopNavigation(
            title = state.title,
            subtitle = state.subtitle,
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            fadeEnabled = !isChipsPinned,
            onBack = state.onClose,
            onClose = state.onClose,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { navigationHeightPx = it.height }
                .semantics {
                    // The overlays follow the list in the tree: without this TalkBack reads them after the page
                    isTraversalGroup = true
                    @Suppress("MagicNumber")
                    traversalIndex = -2f
                },
        )
        Footer(
            secondary = state.secondary,
            primary = state.primary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { footerHeightPx = it.height },
        )
    }
}

private fun LazyListScope.pageContent(
    items: List<PageItem>,
    state: EarnInfoUM,
    chart: (@Composable () -> Unit)?,
    chipsSpace: @Composable () -> Unit,
) {
    items.forEach { pageItem ->
        when (pageItem) {
            PageItem.Balance -> item(key = pageItem.key) { BalanceBlock(state = state) }
            is PageItem.StaleNotice -> item(key = pageItem.key) { StaleNotice(description = pageItem.description) }
            is PageItem.Rewards -> item(key = pageItem.key) { Rewards(item = pageItem) }
            PageItem.Chart -> item(key = pageItem.key) { ChartSlot(chart = chart) }
            PageItem.Tabs -> item(key = pageItem.key) { chipsSpace() }
            is PageItem.Section -> item(key = pageItem.key) { SectionItem(tab = pageItem.tab, state = state) }
        }
    }
}

/** A haze source above the list's: the glass buttons and the pill sample the fade instead of punching holes in it. */
@Composable
private fun PinnedFade(isPinned: Boolean, heightPx: Int) {
    val fadeOutAlpha by animateFloatAsState(
        targetValue = if (isPinned) 1f else 0f,
        animationSpec = if (isPinned) snap() else spring(),
        label = "EarnInfo.pinnedFadeAlpha",
    )
    val isBlurred by remember(isPinned) { derivedStateOf { isPinned || fadeOutAlpha > 0f } }
    val height = with(LocalDensity.current) { heightPx.toDp() } + 2.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .hazeSourceTangem(zIndex = 1f),
    ) {
        TangemFade(
            position = TangemFade.Position.Top,
            variant = TangemFade.Variant.Hard,
            blur = isBlurred,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (isPinned) 1f else fadeOutAlpha },
        )
    }
}

@Composable
private fun FloatingChips(
    chips: ImmutableList<TangemTabItemUM>,
    listState: LazyListState,
    chipsIndex: Int,
    navigationHeightPx: () -> Int,
    onHeightChange: (Int) -> Unit,
) {
    Chips(
        chips = chips,
        modifier = Modifier
            .offset { IntOffset(x = 0, y = listState.floatingChipsTop(chipsIndex, navigationHeightPx())) }
            .onSizeChanged { onHeightChange(it.height) }
            .scrollable(state = listState, orientation = Orientation.Vertical, reverseDirection = true)
            .semantics {
                isTraversalGroup = true
                traversalIndex = -1f
            },
    )
}

@Composable
private fun ChipsSpace(heightPx: Int) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(with(LocalDensity.current) { heightPx.toDp() }),
    )
}

@Composable
private fun BalanceBlock(state: EarnInfoUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PAGE_MARGIN, end = PAGE_MARGIN, top = 48.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = state.balanceFiat.resolveReference(),
            style = TangemTheme.typography3.display.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = state.balanceCrypto.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
        state.earned?.let { earned ->
            TangemBadge(text = earned, status = TangemBadge.Status.Info, size = TangemBadge.Size.X6)
        }
    }
}

@Composable
private fun StaleNotice(description: TextReference) {
    TangemMessageBanner(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        title = resourceReference(R.string.warning_outdated_data_title),
        description = description,
        variant = TangemMessageBanner.Variant.Warning,
    )
}

@Composable
private fun Rewards(item: PageItem.Rewards) {
    val rewards = item.rewards
    TangemMessageBanner(
        title = rewards.title,
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = if (item.isUnderStaleNotice) 0.dp else 12.dp,
            bottom = if (item.isAboveChart) 36.dp else 24.dp,
        ),
        variant = TangemMessageBanner.Variant.Info,
        description = rewards.description,
        slotEnd = {
            TangemButton(
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X9,
                text = rewards.claimLabel,
                onClick = rewards.onClaimClick,
            )
        },
    )
}

@Composable
private fun ChartSlot(chart: (@Composable () -> Unit)?) {
    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        chart?.invoke()
    }
}

@Composable
private fun Chips(chips: ImmutableList<TangemTabItemUM>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        TangemTabNavigation(tabs = chips, contentPadding = PaddingValues(horizontal = PAGE_MARGIN))
    }
}

@Composable
private fun SectionItem(tab: EarnInfoTab, state: EarnInfoUM) {
    val modifier = Modifier.padding(vertical = 12.dp)

    when (tab) {
        EarnInfoTab.MyFunds -> MyFundsSection(state = state.myFunds, modifier = modifier)
        EarnInfoTab.Benefits -> BenefitsSection(benefits = state.benefits, modifier = modifier)
        EarnInfoTab.Details -> DetailsSection(details = state.details, modifier = modifier)
    }
}

/** The DS button ignores [TangemButtonUM.type], so the style follows the position. */
@Composable
private fun Footer(secondary: ImmutableList<TangemButtonUM>, primary: TangemButtonUM, modifier: Modifier = Modifier) {
    TangemFooter(modifier = modifier) {
        TangemButtonGroup(
            modifier = Modifier.fillMaxWidth(),
            orientation = TangemButtonGroup.Orientation.Horizontal,
        ) {
            secondary.forEach { button -> ActionButton(button = button, variant = TangemButton.Variant.Secondary) }
            ActionButton(button = primary, variant = TangemButton.Variant.Primary)
        }
    }
}

@Composable
private fun ActionButton(button: TangemButtonUM, variant: TangemButton.Variant) {
    TangemButton(
        variant = variant,
        size = TangemButton.Size.X12,
        text = button.text,
        isEnabled = button.isEnabled,
        isDimmed = button.isDimmed,
        onClick = button.onClick,
    )
}

/** One entry is one list item, so the position in this list is the list index the scroll maths uses. */
private sealed interface PageItem {

    val key: String

    data object Balance : PageItem {
        override val key = "balance"
    }

    data class StaleNotice(val description: TextReference) : PageItem {
        override val key = "stale"
    }

    data class Rewards(
        val rewards: EarnInfoUM.RewardsUM,
        val isUnderStaleNotice: Boolean,
        val isAboveChart: Boolean,
    ) : PageItem {
        override val key = "rewards"
    }

    data object Chart : PageItem {
        override val key = "chart"
    }

    data object Tabs : PageItem {
        override val key = "tabs"
    }

    data class Section(val tab: EarnInfoTab) : PageItem {
        override val key = tab.name
    }
}

private fun buildPageItems(state: EarnInfoUM, hasChart: Boolean): List<PageItem> = buildList {
    add(PageItem.Balance)
    state.staleNotice?.let { add(PageItem.StaleNotice(description = it)) }
    state.rewards?.let { rewards ->
        add(
            PageItem.Rewards(
                rewards = rewards,
                isUnderStaleNotice = state.staleNotice != null,
                isAboveChart = hasChart,
            ),
        )
    }
    if (hasChart) add(PageItem.Chart)
    add(PageItem.Tabs)
    state.tabs.forEach { tab -> add(PageItem.Section(tab = tab)) }
}

private fun List<PageItem>.sectionIndices(): Map<EarnInfoTab, Int> = withIndex()
    .mapNotNull { (index, item) -> (item as? PageItem.Section)?.let { section -> section.tab to index } }
    .toMap()