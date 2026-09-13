package com.tangem.features.feed.search.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedTabComponent
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.nav.FeedTabSet
import com.tangem.features.feed.search.FeedSearchComponent
import com.tangem.features.feed.search.model.FeedSearchModel
import com.tangem.features.feed.search.ui.FeedSearchPlaceholder
import com.tangem.features.feed.search.ui.FeedSearchTabsContent
import com.tangem.features.feed.ui.FeedTabUM
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Stable
internal class DefaultFeedSearchComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: FeedRoute.Search,
    tabSet: FeedTabSet,
) : FeedSearchComponent, AppComponentContext by context {

    private val model: FeedSearchModel = getOrCreateModel(params)

    private val contributors: List<FeedTabContributor> = tabSet.tabs

    private val contributorsById: Map<FeedTabId, FeedTabContributor> = contributors.associateBy { it.id }

    private val tabs: ImmutableList<FeedTabUM> = contributors
        .map { FeedTabUM(id = it.id.value, title = it.title) }
        .toImmutableList()

    private val pagesNavigation = PagesNavigation<FeedTabId>()

    private val pages: Value<ChildPages<FeedTabId, FeedTabComponent>> = childPages(
        source = pagesNavigation,
        serializer = null,
        initialPages = { Pages(items = contributors.map { it.id }, selectedIndex = 0) },
        key = "feedSearchTabs",
        pageStatus = { index, pages ->
            if (index == pages.selectedIndex) ChildNavState.Status.RESUMED else ChildNavState.Status.CREATED
        },
        // the feed host owns back: `true` here would make back switch tabs instead of leaving search
        handleBackButton = false,
        childFactory = ::createSearchTab,
    )

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        onExpandSheet: () -> Unit,
        modifier: Modifier,
    ) {
        val state by model.uiState.collectAsStateWithLifecycle()

        if (state.query.isBlank()) {
            FeedSearchPlaceholder(recent = state.recent, contentPadding = contentPadding, modifier = modifier)
            return
        }

        val pagesState by pages.subscribeAsState()

        FeedSearchTabsContent(
            tabs = tabs,
            selectedTabIndex = pagesState.selectedIndex,
            onTabSelect = { index ->
                pagesNavigation.select(index = index)
                onExpandSheet()
            },
            contentPadding = contentPadding,
            modifier = modifier,
        ) { tabModifier ->
            pagesState.items.getOrNull(pagesState.selectedIndex)?.instance?.Content(
                bottomSheetState = bottomSheetState,
                // the tab row is laid out above, not overlaid
                contentPadding = PaddingValues(0.dp),
                modifier = tabModifier.fillMaxSize(),
            )
        }
    }

    private fun createSearchTab(id: FeedTabId, componentContext: ComponentContext): FeedTabComponent {
        val contributor = requireNotNull(contributorsById[id]) { "No tab contributor for ${id.value}" }
        val childContext = childByContext(componentContext = componentContext)

        return contributor.createSearchTab(context = childContext, query = model.query)
            ?: UnsupportedSearchTabComponent(context = childContext, tabId = id)
    }

    @AssistedFactory
    interface Factory : FeedSearchComponent.Factory {
        override fun create(context: AppComponentContext, params: FeedRoute.Search): DefaultFeedSearchComponent
    }
}