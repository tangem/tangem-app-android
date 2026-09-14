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
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.children.ChildNavState
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.feed.components.v2.model.FeedHomeModel
import com.tangem.features.feed.nav.FeedTabComponent
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.ui.v2.FeedV2Content

/**
 * Root of the v2 feed stack ([FeedHomeRoute]): top blocks + the tab pager.
 * Tabs come from the contributed [FeedTabContributor] set; each page is a Decompose

 * so an unselected tab keeps its state but knows it is not visible.
 */
internal class FeedHomeComponent(
    context: AppComponentContext,
    portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : FeedScreenComponent, AppComponentContext by context {

    override val isPartialExpansionSupported: Boolean = true

    private val model: FeedHomeModel = getOrCreateModel()

    private val topBlocksComponent = FeedTopBlocksComponent(
        context = child("feedTopBlocks"),
        portfolioSelectorComponentFactory = portfolioSelectorComponentFactory,
    )

    private val pages: Value<ChildPages<FeedTabId, FeedTabComponent>> = childPages(
        source = model.pagesNavigation,
        serializer = null,
        initialPages = { Pages(items = model.contributors.map { it.id }, selectedIndex = 0) },
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
            tabs = model.tabs,
            selectedTabIndex = pagesState.selectedIndex,
            onTabSelect = model::selectTab,
            isExpanded = bottomSheetState.value == BottomSheetState.EXPANDED,
            contentPadding = contentPadding,
            onExpandSheet = onExpandSheet,
            modifier = modifier,
            topBlocks = { blockModifier -> topBlocksComponent.Content(modifier = blockModifier) },
            tabContent = { tabModifier, tabContentPadding ->
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
                        contentPadding = tabContentPadding,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
        )
    }

    private fun createTab(id: FeedTabId, componentContext: ComponentContext): FeedTabComponent {
        val contributor = requireNotNull(model.contributorsById[id]) { "No tab contributor for ${id.value}" }
        return contributor.createTab(context = childByContext(componentContext = componentContext))
    }
}