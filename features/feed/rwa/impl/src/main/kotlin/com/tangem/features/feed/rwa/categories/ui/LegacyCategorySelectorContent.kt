package com.tangem.features.feed.rwa.categories.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.checkbox.TangemCheckmark
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.ds2.togglebadge.TangemToggleBadge
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.rwa.categories.ui.state.*
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** The selector body before the redesign: a "Type" section, the selected category's "Sectors" rail and an Apply footer. */
@RemoveWithToggle("AND_19514_CATEGORY_FILTERS_REDESIGN_ENABLED")
@Composable
internal fun LegacySectionsContent(state: CategorySelectorUM.LegacyContent, modifier: Modifier = Modifier) {
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
                    is LegacyCategorySelectorSectionUM.Sectors -> LegacySectorSection(section = section)
                    is LegacyCategorySelectorSectionUM.Items -> LegacyItemsSection(section = section)
                }
            }

            LegacyFooterSpacer()
        }

        LegacyFooter(
            state = state.footer,
            isVisible = state.isApplyVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun LegacyItemsSection(section: LegacyCategorySelectorSectionUM.Items, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        LegacySectionHeader(
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
private fun LegacySectorSection(section: LegacyCategorySelectorSectionUM.Sectors, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        LegacySectionHeader(
            title = section.title,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(18.dp))
        LegacySectorsRail(
            tags = section.sectors,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun LegacySectionHeader(title: TextReference, modifier: Modifier = Modifier) {
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
private fun LegacySectorsRail(tags: ImmutableList<CategorySectorUM>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.fastForEach { tag ->
            TangemToggleBadge(
                text = tag.label,
                isSelected = tag.isSelected,
                onSelectedChange = { tag.onItemClick() },
            )
        }
    }
}

/** Keeps the last rows scrollable above the [LegacyFooter], which sits over the list including the navigation bar inset. */
@Composable
private fun LegacyFooterSpacer() {
    Spacer(
        modifier = Modifier
            .navigationBarsPadding()
            .height(128.dp),
    )
}

/** No Reset: the "All" row clears the filter in one tap, like every other category without a sector choice. */
@Composable
private fun LegacyFooter(state: LegacyCategorySelectorFooterUM, isVisible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = isVisible,
        // the default size transitions clip for good, cutting off the part of the footer fade drawn above it
        enter = fadeIn() + expandIn(clip = false),
        exit = shrinkOut(clip = false) + fadeOut(),
        modifier = modifier.fillMaxWidth(),
    ) {
        TangemFooter(
            contentPadding = PaddingValues(all = 16.dp),
            // inside the feed modal the blur samples the screen behind the sheet, not the list under the footer
            blurBackground = false,
        ) {
            TangemButton(
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_apply),
                onClick = state.onApply,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun LegacySectionsContent_Preview() {
    TangemThemePreviewRedesign {
        LegacySectionsContent(
            state = previewLegacyContent(),
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}

private fun previewLegacyContent(): CategorySelectorUM.LegacyContent {
    val types = listOf("xStocks", "Commodities", "ETFs", "Bonds")
    val sectors = listOf("All", "Technology", "Financials", "Healthcare", "Energy", "Consumer", "Industrials")

    return CategorySelectorUM.LegacyContent(
        sections = persistentListOf(
            LegacyCategorySelectorSectionUM.Items(
                title = stringReference("Type"),
                items = types.mapIndexed { index, title ->
                    CategoryItemUM(
                        id = title,
                        title = stringReference(title),
                        caption = stringReference("212 assets"),
                        isSelected = index == 0,
                        sectors = persistentListOf(),
                        onItemClick = {},
                    )
                }.toImmutableList(),
            ),
            LegacyCategorySelectorSectionUM.Sectors(
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
        ),
        isApplyVisible = true,
        footer = LegacyCategorySelectorFooterUM(onApply = {}),
    )
}
// endregion