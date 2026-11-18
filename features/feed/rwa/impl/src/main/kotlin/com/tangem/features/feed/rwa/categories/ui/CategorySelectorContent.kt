package com.tangem.features.feed.rwa.categories.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.checkbox.TangemCheckmark
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.togglebadge.TangemToggleBadge
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.features.feed.rwa.categories.ui.state.CategoryItemUM
import com.tangem.features.feed.rwa.categories.ui.state.CategorySectorUM
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** The hosting chrome — feed modal and header — is owned by the feed host and the component. */
@Composable
internal fun CategorySelectorContent(state: CategorySelectorUM, modifier: Modifier = Modifier) {
    when (state) {
        is CategorySelectorUM.Loading -> LoadingSkeleton(modifier = modifier)
        is CategorySelectorUM.Content -> ItemsContent(items = state.items, modifier = modifier)
        is CategorySelectorUM.LegacyContent -> LegacySectionsContent(state = state, modifier = modifier)
        is CategorySelectorUM.Error -> LoadError(onRetry = state.onRetry, modifier = modifier)
    }
}

@Composable
private fun ItemsContent(items: ImmutableList<CategoryItemUM>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.fastForEach { item ->
            CategoryItem(item = item)
        }
    }
}

@Composable
private fun CategoryItem(item: CategoryItemUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        TangemRow(
            contentLead = TangemRowContentLead.Start,
            verticalAlignment = TangemRowVerticalAlignment.Center,
            titleSlot = { TangemRowText(text = item.title, role = TangemRowTextRole.Title) },
            subtitleSlot = { TangemRowText(text = item.caption, role = TangemRowTextRole.Subtitle) },
            endSlot = {
                TangemCheckmark(
                    checked = item.isSelected,
                    onCheckedChange = { item.onItemClick() },
                )
            },
            onClick = item.onItemClick,
        )
        if (item.sectors.isNotEmpty()) {
            SectorsRail(
                sectors = item.sectors,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun SectorsRail(sectors: ImmutableList<CategorySectorUM>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        sectors.fastForEach { sector ->
            TangemToggleBadge(
                text = sector.label,
                isSelected = sector.isSelected,
                onSelectedChange = { sector.onItemClick() },
            )
        }
    }
}

@Composable
private fun LoadError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TangemButton(
            iconStart = TangemIconUM.Icon(Icons.ic_arrow_refresh_20),
            onClick = onRetry,
        )
        SpacerH(12.dp)
        Text(
            text = resourceReference(R.string.market_failed_to_load_filters).resolveReference(),
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
            .padding(top = 12.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(times = 5) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                TangemShimmer(
                    style = TangemTheme.typography3.body.medium,
                    modifier = Modifier.width(width = 120.dp),
                )
                TangemShimmer(
                    style = TangemTheme.typography3.caption.medium,
                    modifier = Modifier.width(width = 64.dp),
                )
            }
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun CategorySelectorContent_Preview(
    @PreviewParameter(CategorySelectorContentPreviewProvider::class) params: CategorySelectorUM,
) {
    TangemThemePreviewRedesign {
        CategorySelectorContent(params, modifier = Modifier.background(TangemTheme.colors3.bg.primary))
    }
}

private class CategorySelectorContentPreviewProvider : PreviewParameterProvider<CategorySelectorUM> {
    override val values: Sequence<CategorySelectorUM>
        get() = sequenceOf(
            previewContent(selectedIndex = 1),
            previewContent(selectedIndex = 0),
            CategorySelectorUM.Loading,
            CategorySelectorUM.Error(onRetry = {}),
        )
}

private fun previewContent(selectedIndex: Int): CategorySelectorUM.Content {
    val categories = listOf("All", "xStocks", "Commodities", "ETFs", "Bonds")
    val sectors = listOf("Healthcare", "Energy", "Consumer", "Financials", "Industrials")

    return CategorySelectorUM.Content(
        items = categories.mapIndexed { index, title ->
            val isSelected = index == selectedIndex
            CategoryItemUM(
                id = title,
                title = stringReference(title),
                caption = stringReference("212 assets"),
                isSelected = isSelected,
                sectors = if (isSelected && index != 0) {
                    sectors.map { label ->
                        CategorySectorUM(
                            id = label,
                            label = stringReference(label),
                            isSelected = false,
                            onItemClick = {},
                        )
                    }.toImmutableList()
                } else {
                    persistentListOf()
                },
                onItemClick = {},
            )
        }.toImmutableList(),
    )
}
// endregion