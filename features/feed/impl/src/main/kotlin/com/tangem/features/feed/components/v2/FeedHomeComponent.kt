package com.tangem.features.feed.components.v2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.children.ChildNavState
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.feed.nav.FeedTabComponent
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.ui.v2.FeedV2Content
import com.tangem.features.feed.ui.v2.state.FeedV2TabUM
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Root of the v2 feed stack ([FeedHomeRoute]): top blocks + the tab pager.
 * Tabs come from the contributed [FeedTabContributor] set; each page is a Decompose

 * so an unselected tab keeps its state but knows it is not visible.
 */
internal class FeedHomeComponent(
    context: AppComponentContext,
    tabContributors: Set<FeedTabContributor>,
) : FeedScreenComponent, AppComponentContext by context {

    private val topBlocksComponent = FeedTopBlocksComponent(context = child("feedTopBlocks"))

    private val contributors: List<FeedTabContributor> = run {
        val byId = tabContributors.associateBy { it.id }
        tabContributors
            .filter { it.id !in FeedTabsOrder }
            .forEach { TangemLogger.e("Feed tab '${it.id.value}' is not in FeedTabsOrder — skipped") }

        FeedTabsOrder.mapNotNull(byId::get).filter { it.isAvailable }
    }

    private val contributorsById: Map<FeedTabId, FeedTabContributor> = contributors.associateBy { it.id }

    private val tabs: ImmutableList<FeedV2TabUM> = contributors
        .map { FeedV2TabUM(id = it.id.value, title = it.title) }
        .toImmutableList()

    private val pagesNavigation = PagesNavigation<FeedTabId>()

    private val pages: Value<ChildPages<FeedTabId, FeedTabComponent>> = childPages(
        source = pagesNavigation,
        serializer = null,
        initialPages = { Pages(items = contributors.map { it.id }, selectedIndex = 0) },
        key = "feedTabs",
        pageStatus = { index, pages ->
            if (index == pages.selectedIndex) ChildNavState.Status.RESUMED else ChildNavState.Status.CREATED
        },
        handleBackButton = false,
        childFactory = ::createTab,
    )

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        onExpandSheet: () -> Unit,
        modifier: Modifier,
    ) {
        val pagesState by pages.subscribeAsState()

        FeedV2Content(
            tabs = tabs,
            selectedTabIndex = pagesState.selectedIndex,
            onTabSelect = { index -> pagesNavigation.select(index = index) },
            isExpanded = bottomSheetState.value == BottomSheetState.EXPANDED,
            contentPadding = contentPadding,
            onExpandSheet = onExpandSheet,
            modifier = modifier,
            topBlocks = { blockModifier -> topBlocksComponent.Content(modifier = blockModifier) },
            tabContent = { tabModifier ->
                AnimatedContent(
                    targetState = pagesState.selectedIndex,
                    transitionSpec = {
                        // slide towards the tapped tab: forward → new page enters from the right
                        val isForward = targetState > initialState
                        val enter = slideInHorizontally(
                            initialOffsetX = { fullWidth -> if (isForward) fullWidth else -fullWidth },
                        )
                        val exit = slideOutHorizontally(
                            targetOffsetX = { fullWidth -> if (isForward) -fullWidth else fullWidth },
                        )
                        enter togetherWith exit
                    },
                    label = "feedTabPages",
                    modifier = tabModifier,
                ) { pageIndex ->
                    pagesState.items.getOrNull(pageIndex)?.instance?.Content(
                        bottomSheetState = bottomSheetState,
                        // the feed chrome (top blocks + tab row) is laid out above, not overlaid
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
        )
    }

    private fun createTab(id: FeedTabId, componentContext: ComponentContext): FeedTabComponent {
        val contributor = requireNotNull(contributorsById[id]) { "No tab contributor for ${id.value}" }
        return contributor.createTab(context = childByContext(componentContext = componentContext))
    }
}