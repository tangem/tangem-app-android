package com.tangem.features.feed.crypto.categories.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.checkbox.TangemCheckmark
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.togglebadge.TangemToggleBadge
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_search_20
import com.tangem.features.feed.crypto.categories.ui.state.*
import com.tangem.features.feed.crypto.impl.R
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private const val LOAD_NEXT_PAGE_ON_END_INDEX = 10
private const val NO_RESULTS_ICON_ALPHA = 0.9f

/**
 * The hosting chrome — feed modal and title header — is owned by the feed host and the component.
 *
 * The search field floats over the body rather than scrolling with the list: it must remain
 * reachable over the loading, empty and error bodies too. The list scrolls under it, behind a top fade.
 */
@Composable
internal fun MarketCategorySelectorContent(state: MarketCategorySelectorUM, modifier: Modifier = Modifier) {
    // shtorka content inherits the haze of the screen behind the shtorka; the field must blur this
    // screen instead, as the search in the shtorka header does
    val outerHazeState = LocalHazeState.current
    val contentHazeState = rememberHazeState()
    SideEffect { contentHazeState.blurEnabled = outerHazeState.blurEnabled }

    Box(modifier = modifier.fillMaxSize()) {
        Body(
            content = state.content,
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem(state = contentHazeState),
        )

        TangemFade(
            position = TangemFade.Position.Top,
            variant = TangemFade.Variant.Hard,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .height(72.dp)
                .hazeSourceTangem(state = contentHazeState, zIndex = 1f),
        )

        CompositionLocalProvider(LocalHazeState provides contentHazeState) {
            TangemSearch(
                state = state.search,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
        }

        Footer(
            state = state.footer,
            isVisible = state.isResetVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun Body(content: MarketCategorySelectorUM.ContentUM, modifier: Modifier = Modifier) {
    val belowSearchModifier = modifier.padding(top = 56.dp)
    when (content) {
        is MarketCategorySelectorUM.ContentUM.Loading -> LoadingSkeleton(modifier = belowSearchModifier)
        is MarketCategorySelectorUM.ContentUM.Content -> SectionsContent(state = content, modifier = modifier)
        is MarketCategorySelectorUM.ContentUM.Empty -> NoResults(modifier = belowSearchModifier)
        is MarketCategorySelectorUM.ContentUM.Error -> LoadError(
            onRetry = content.onRetry,
            modifier = belowSearchModifier,
        )
    }
}

@Composable
private fun SectionsContent(state: MarketCategorySelectorUM.ContentUM.Content, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val categories = state.categories

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = 56.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "topSpacer") { SpacerH(24.dp) }

        if (state.featured != null) {
            item(key = "featuredSection") {
                FeaturedSection(section = state.featured)
                SpacerH(24.dp)
            }
        }

        if (categories.items.isNotEmpty()) {
            item(key = "categoriesHeader") {
                SectionHeader(title = categories.title)
            }
        }

        items(items = categories.items, key = MarketCategoryItemUM::id) { item ->
            CategoryRow(item = item)
        }

        if (categories.isLoadingMore) {
            item(key = "loadingMore") { LoadingMoreRow() }
        }

        item(key = "footerSpacer") { FooterSpacer() }
    }

    InfiniteListHandler(
        listState = listState,
        buffer = LOAD_NEXT_PAGE_ON_END_INDEX,
        triggerLoadMoreCheckOnItemsCountChange = true,
        onLoadMore = remember(categories) {
            {
                categories.loadMore()
                true
            }
        },
    )
}

@Composable
private fun FeaturedSection(section: FeaturedCategoriesUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = section.title)
        SpacerH(8.dp)
        FeaturedRail(items = section.items, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

/** Wraps instead of scrolling: the rail is a shortcut strip, and a hidden shortcut is no shortcut. */
@Composable
private fun FeaturedRail(items: ImmutableList<FeaturedCategoryUM>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.fastForEach { item ->
            TangemToggleBadge(
                text = item.label,
                isSelected = item.isSelected,
                onSelectedChange = { item.onItemClick() },
            )
        }
    }
}

@Composable
private fun CategoryRow(item: MarketCategoryItemUM, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = { TangemRowText(text = item.title, role = TangemRowTextRole.Title) },
        valueSlot = item.subtitle?.let { subtitle ->
            { TangemRowText(text = subtitle, role = TangemRowTextRole.Value) }
        },
        subvalueSlot = item.caption?.let { caption ->
            { TangemRowText(text = caption, role = TangemRowTextRole.Subvalue) }
        },
        endSlot = {
            TangemCheckmark(
                checked = item.isSelected,
                onCheckedChange = { item.onItemClick() },
            )
        },
        onClick = item.onItemClick,
    )
}

@Composable
private fun LoadingMoreRow(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        TangemShimmer(style = TangemTheme.typography3.body.medium)
    }
}

@Composable
private fun SectionHeader(title: TextReference, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 38.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            maxLines = 1,
        )
    }
}

/** Keeps the last rows scrollable above the [Footer], which sits over the list including the navigation bar inset. */
@Composable
private fun FooterSpacer() {
    Spacer(
        modifier = Modifier
            .navigationBarsPadding()
            .height(128.dp),
    )
}

/** A tap on a category applies it, so the footer only offers Reset — and only once there is a filter to clear. */
@Composable
private fun Footer(state: MarketCategorySelectorFooterUM, isVisible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = isVisible,
        // the default size transitions clip for good, cutting off the part of the footer fade drawn above it
        enter = fadeIn() + expandIn(clip = false),
        exit = shrinkOut(clip = false) + fadeOut(),
        modifier = modifier
            .fillMaxWidth(),
    ) {
        TangemFooter(
            contentPadding = PaddingValues(all = 16.dp),
            // inside the feed modal the blur samples the screen behind the sheet, not the list under the footer
            blurBackground = false,
        ) {
            TangemButton(
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_reset),
                onClick = state.onReset,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NoResults(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp, alignment = Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .alpha(NO_RESULTS_ICON_ALPHA)
                .background(color = TangemTheme.colors3.bg.transparent.secondary, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.ic_search_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResourceSafe(R.string.prediction_search_empty_title),
                textAlign = TextAlign.Center,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = stringResourceSafe(R.string.prediction_search_empty_subtitle),
                textAlign = TextAlign.Center,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Composable
private fun LoadError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TangemButton(
            iconStart = TangemIconUM.Icon(Icons.ic_arrow_refresh_20),
            onClick = onRetry,
        )
        SpacerH(12.dp)
        Text(
            text = stringResourceSafe(R.string.market_failed_to_load_filters),
            textAlign = TextAlign.Center,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun LoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 24.dp, bottom = 24.dp)
            .padding(horizontal = 16.dp),
    ) {
        SectionHeaderShimmer()
        SpacerH(8.dp)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(times = 8) { index ->
                TangemShimmer(
                    modifier = Modifier.size(width = (20 * (index % 5 + 2)).dp, height = 36.dp),
                    radius = 999.dp,
                )
            }
        }
        SpacerH(24.dp)
        SectionHeaderShimmer()
        repeat(times = 6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                TangemShimmer(style = TangemTheme.typography3.body.medium)
            }
        }
    }
}

@Composable
private fun SectionHeaderShimmer() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        TangemShimmer(
            style = TangemTheme.typography3.subheading.medium,
            modifier = Modifier.width(width = 160.dp),
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun MarketCategorySelectorContent_Preview(
    @PreviewParameter(MarketCategorySelectorPreviewProvider::class) params: MarketCategorySelectorUM,
) {
    TangemThemePreviewRedesign {
        MarketCategorySelectorContent(params)
    }
}

private class MarketCategorySelectorPreviewProvider : PreviewParameterProvider<MarketCategorySelectorUM> {
    override val values: Sequence<MarketCategorySelectorUM>
        get() = sequenceOf(
            previewState(body = previewBody(withFeatured = true), isResetVisible = true),
            previewState(body = previewBody(withFeatured = false), isResetVisible = false),
            previewState(body = previewBody(withFeatured = true, query = "mem"), query = "mem"),
            previewState(body = MarketCategorySelectorUM.ContentUM.Empty, query = "zzz"),
            previewState(body = MarketCategorySelectorUM.ContentUM.Loading),
            previewState(body = MarketCategorySelectorUM.ContentUM.Error(onRetry = {})),
        )
}

private fun previewState(
    body: MarketCategorySelectorUM.ContentUM,
    isResetVisible: Boolean = false,
    query: String = "",
): MarketCategorySelectorUM = MarketCategorySelectorUM(
    search = TangemSearch.State(
        placeholderText = resourceReference(R.string.common_search),
        query = query,
        onQueryChange = {},
        isActive = query.isNotEmpty(),
        onActiveChange = {},
    ),
    isResetVisible = isResetVisible,
    footer = MarketCategorySelectorFooterUM(onReset = {}),
    content = body,
)

private fun previewBody(withFeatured: Boolean, query: String = ""): MarketCategorySelectorUM.ContentUM.Content {
    val featured = listOf("AI", "Meme", "Real world assets", "Gaming", "Layer 1")
    val all = listOf("AI", "DeFi", "Gaming", "Meme", "Layer 1", "AI 2", "DeFi 2", "Gaming 2", "Meme 2", "Layer 2")
    return MarketCategorySelectorUM.ContentUM.Content(
        featured = FeaturedCategoriesUM(
            title = resourceReference(R.string.common_featured),
            items = featured.filter { it.contains(other = query, ignoreCase = true) }.map { label ->
                FeaturedCategoryUM(
                    id = label,
                    label = stringReference(label),
                    isSelected = label == "Meme",
                    onItemClick = {},
                )
            }.toImmutableList(),
        ).takeIf { withFeatured && it.items.isNotEmpty() },
        categories = MarketCategoryListUM(
            title = resourceReference(R.string.market_categories),
            items = buildList {
                all.filter { it.contains(other = query, ignoreCase = true) }.forEach { name ->
                    add(
                        MarketCategoryItemUM(
                            id = name,
                            title = stringReference(name),
                            subtitle = stringReference("$ 352.899 B "),
                            caption = stringReference("212 coins"),
                            isSelected = name == "Meme",
                            onItemClick = {},
                        ),
                    )
                }
            }.toImmutableList(),
            isLoadingMore = false,
            loadMore = {},
        ),
    )
}
// endregion