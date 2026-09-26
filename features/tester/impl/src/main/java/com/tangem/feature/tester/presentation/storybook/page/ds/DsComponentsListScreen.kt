package com.tangem.feature.tester.presentation.storybook.page.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.button.TangemButtonStack
import com.tangem.core.ui.ds2.checkbox.TangemCheckbox
import com.tangem.core.ui.ds2.checkbox.TangemCheckmark
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.filter.TangemFilterGroup
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.segmentedcontrol.TangemSegmentedControl
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.shtorka.TangemShtorka
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRowMarket
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.StoryPageFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.badge.tangemBadgeV2StoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.button.tangemButtonStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.buttongroup.tangemButtonGroupStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.buttonstack.tangemButtonStackStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.checkbox.tangemCheckboxV2StoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.checkmark.tangemCheckmarkStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.fade.tangemFadeStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.filter.tangemFilterGroupStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.footer.tangemFooterStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.glowring.tangemGlowRingStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.loader.tangemLoaderStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.messagebanner.tangemMessageBannerStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.modal.tangemModalStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.row.tangemRowStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.search.tangemSearchStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.segmentedcontrol.tangemSegmentedControlStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.shimmer.tangemShimmerStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.shtorka.tangemShtorkaStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.statusscreen.tangemStatusScreenStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.tabnavigation.tangemTabNavigationStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.textstyle.textStyleStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.tokenicon.tangemTokenIconStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.tokenrowmarket.tangemTokenRowMarketStoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.tokenrowv2.tangemTokenRowV2StoryFactory
import com.tangem.feature.tester.presentation.storybook.page.ds.topnavigation.tangemTopNavigationStoryFactory

/** [version] is `null` for entries that showcase tokens rather than a versioned DS component. */
private data class DsStoryItem(val title: String, val version: DsVersion?, val factory: StoryPageFactory)

private fun buildDsStories() = listOf(
    DsStoryItem("⏳ TangemLoader", TangemLoader.version, tangemLoaderStoryFactory),
    DsStoryItem("🔘 TangemButton", TangemButton.version, tangemButtonStoryFactory),
    DsStoryItem("🔘 TangemButtonGroup", TangemButtonGroup.version, tangemButtonGroupStoryFactory),
    DsStoryItem("🔘 TangemButtonStack", TangemButtonStack.version, tangemButtonStackStoryFactory),
    DsStoryItem("🏷️ TangemBadge", TangemBadge.version, tangemBadgeV2StoryFactory),
    DsStoryItem("☑️ TangemCheckbox", TangemCheckbox.version, tangemCheckboxV2StoryFactory),
    DsStoryItem("⭕ TangemCheckmark", TangemCheckmark.version, tangemCheckmarkStoryFactory),
    DsStoryItem("🎨 TangemTokenIcon", TangemTokenIcon.version, tangemTokenIconStoryFactory),
    DsStoryItem("📋 TangemRow", TangemRow.version, tangemRowStoryFactory),
    DsStoryItem("🪙 TangemTokenRow", TangemTokenRow.version, tangemTokenRowV2StoryFactory),
    DsStoryItem("📈 TangemTokenRowMarket", TangemTokenRowMarket.version, tangemTokenRowMarketStoryFactory),
    DsStoryItem("🔎 TangemSearch", TangemSearch.version, tangemSearchStoryFactory),
    DsStoryItem("✨ TangemShimmer", TangemShimmer.version, tangemShimmerStoryFactory),
    DsStoryItem("🪟 TangemShtorka", TangemShtorka.version, tangemShtorkaStoryFactory),
    DsStoryItem("🗂️ TangemModal", TangemModal.version, tangemModalStoryFactory),
    DsStoryItem("🌫️ TangemFade", TangemFade.version, tangemFadeStoryFactory),
    DsStoryItem("🧭 TangemTopNavigation", TangemTopNavigation.version, tangemTopNavigationStoryFactory),
    DsStoryItem("🧱 TangemFooter", TangemFooter.version, tangemFooterStoryFactory),
    DsStoryItem("💫 TangemGlowRing", TangemGlowRing.version, tangemGlowRingStoryFactory),
    DsStoryItem("🚥 TangemStatusScreen", TangemStatusScreen.version, tangemStatusScreenStoryFactory),
    DsStoryItem("📢 TangemMessageBanner", TangemMessageBanner.version, tangemMessageBannerStoryFactory),
    DsStoryItem("🎛️ TangemFilterGroup", TangemFilterGroup.version, tangemFilterGroupStoryFactory),
    DsStoryItem("📑 TangemTabNavigation", TangemTabNavigation.version, tangemTabNavigationStoryFactory),
    DsStoryItem("🎚️ TangemSegmentedControl", TangemSegmentedControl.version, tangemSegmentedControlStoryFactory),
    DsStoryItem("🔤 Text styles", null, textStyleStoryFactory),
)

@Composable
internal fun DsComponentsListStory(
    onStoryClick: (StoryPageFactory, DsVersion?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stories = remember { buildDsStories() }

    LazyColumn(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary),
    ) {
        items(items = stories, key = { it.title }) { item ->
            PrimaryButton(
                text = item.version?.let { "${item.title}  v$it" } ?: item.title,
                onClick = { onStoryClick(item.factory, item.version) },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
            )
        }
    }
}