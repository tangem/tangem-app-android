package com.tangem.features.feed.rwa.categories.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
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
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.checkbox.TangemCheckmark
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.features.feed.rwa.categories.ui.state.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** The hosting chrome — feed modal and header — is owned by the feed host and the component. */
@Composable
internal fun CategorySelectorContent(state: CategorySelectorUM, modifier: Modifier = Modifier) {
    when (state) {
        is CategorySelectorUM.Loading -> LoadingSkeleton(modifier = modifier)
        is CategorySelectorUM.Content -> SectionsContent(state = state, modifier = modifier)
        is CategorySelectorUM.Error -> LoadError(onRetry = state.onRetry, modifier = modifier)
    }
}

@Composable
private fun SectionsContent(state: CategorySelectorUM.Content, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            SpacerH(22.dp)
            state.sections.forEachIndexed { index, section ->
                if (index > 0) Spacer(modifier = Modifier.height(34.dp))
                when (section) {
                    is CategorySelectorSectionUM.Sectors -> SectorSection(section = section)
                    is CategorySelectorSectionUM.Items -> ItemsSection(section = section)
                }
            }

            FooterSpacer()
        }

        Footer(
            state = state.footer,
            isEnabled = state.isEdited,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ItemsSection(section: CategorySelectorSectionUM.Items, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = section.title,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        SpacerH(8.dp)
        section.items.forEach { item ->
            TangemRow(
                contentLead = TangemRowContentLead.Start,
                verticalAlignment = TangemRowVerticalAlignment.Center,
                titleSlot = { TangemRowText(text = item.title, role = TangemRowTextRole.Title) },
                valueSlot = { TangemRowText(text = item.caption, role = TangemRowTextRole.Subvalue) },
                endSlot = {
                    TangemCheckmark(
                        checked = item.isSelected,
                        onCheckedChange = { item.onItemClick() },
                    )
                },
                onClick = item.onItemClick,
            )
        }
    }
}

@Composable
private fun SectorSection(section: CategorySelectorSectionUM.Sectors, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = section.title,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(18.dp))
        SectorsRail(
            tags = section.sectors,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun SectionHeader(title: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = title.resolveReference(),
        style = TangemTheme.typography3.subheading.medium,
        color = TangemTheme.colors3.text.secondary,
        modifier = modifier,
        maxLines = 1,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectorsRail(tags: ImmutableList<CategorySectorUM>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.fastForEach { tag ->
            TangemBadge(
                text = tag.label,
                variant = if (tag.isSelected) TangemBadge.Variant.Inverse else TangemBadge.Variant.Solid,
                size = TangemBadge.Size.X9,
                onClick = tag.onItemClick,
            )
        }
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

@Composable
private fun Footer(state: CategorySelectorFooterUM, isEnabled: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = isEnabled,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box {
            TangemFade(
                position = TangemFade.Position.Bottom,
                variant = TangemFade.Variant.Hard,
                modifier = Modifier.matchParentSize(),
            )
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(all = 16.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.common_reset),
                    onClick = state.onReset,
                )
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.common_apply),
                    onClick = state.onApply,
                )
            }
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
            .padding(top = 22.dp, bottom = 24.dp)
            .padding(horizontal = 16.dp),
    ) {
        TangemShimmer(
            style = TangemTheme.typography3.subheading.medium,
            modifier = Modifier.width(width = 160.dp),
        )
        SpacerH(10.dp)
        repeat(times = 4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                TangemShimmer(style = TangemTheme.typography3.body.medium)
            }
        }
        SpacerH(34.dp)
        TangemShimmer(
            style = TangemTheme.typography3.subheading.medium,
            modifier = Modifier.width(width = 160.dp),
        )
        SpacerH(18.dp)
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
            previewBody(isTypeOnly = false, withFooter = false),
            previewBody(isTypeOnly = true, withFooter = true),
            CategorySelectorUM.Loading,
            CategorySelectorUM.Error(onRetry = {}),
        )
}

private fun previewBody(isTypeOnly: Boolean, withFooter: Boolean): CategorySelectorUM.Content {
    val types = listOf("xStocks", "Commodities", "ETFs", "Bonds")
    val sectors = listOf("All", "Technology", "Financials", "Healthcare", "Energy", "Consumer", "Industrials")

    return CategorySelectorUM.Content(
        sections = buildList {
            add(
                CategorySelectorSectionUM.Items(
                    title = stringReference("Type"),
                    items = types.mapIndexed { index, title ->
                        CategoryItemUM(
                            id = title,
                            title = stringReference(title),
                            caption = stringReference("212 assets"),
                            isSelected = if (isTypeOnly) index == 1 else index == 0,
                            onItemClick = {},
                        )
                    }.toImmutableList(),
                ),
            )
            if (!isTypeOnly) {
                add(
                    CategorySelectorSectionUM.Sectors(
                        title = stringReference("Sectors"),
                        sectors = sectors.mapIndexed { index, label ->
                            CategorySectorUM(
                                id = label,
                                label = stringReference(label),
                                isSelected = index == 0,
                                onItemClick = {},
                            )
                        }.toImmutableList(),
                    ),
                )
            }
        }.toImmutableList(),
        isEdited = withFooter,
        footer = CategorySelectorFooterUM(onReset = {}, onApply = {}),
    )
}
// endregion