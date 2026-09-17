package com.tangem.features.feed.components.v2.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.select
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.feed.analytics.FeedSectionAnalyticsEvent
import com.tangem.features.feed.components.v2.blocks.FeedBlockModalCallbacks
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.nav.FeedTabSet
import com.tangem.features.feed.nav.analyticsSection
import com.tangem.features.feed.ui.FeedTabUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

/** Tab set and tab selection of the v2 feed's home screen. */
@Stable
@ModelScoped
internal class FeedHomeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    tabSet: FeedTabSet,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    val contributors: List<FeedTabContributor> = tabSet.tabs

    val contributorsById: Map<FeedTabId, FeedTabContributor> = contributors.associateBy { it.id }

    val tabs: ImmutableList<FeedTabUM> = contributors
        .map { FeedTabUM(id = it.id.value, title = it.title, icon = it.icon) }
        .toImmutableList()

    val pagesNavigation = PagesNavigation<FeedTabId>()

    /** Asks the layout to collapse the top blocks away, as a scroll up would. */
    val topBlocksCollapseRequests: SharedFlow<Unit>
        field = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val topBlocksCallbacks = object : FeedBlockModalCallbacks {
        override fun onOpenTab(tab: FeedTabId) = openTabFromBlock(tab = tab)
    }

    /** Silent on purpose: [selectTab] reports a section the *user* switched to. */
    fun selectSection(id: FeedTabId) {
        val index = contributors.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: return

        pagesNavigation.select(index = index)
    }

    fun selectTab(index: Int) {
        pagesNavigation.select(
            index = index,
            onComplete = { newPages, oldPages ->
                if (newPages.selectedIndex != oldPages.selectedIndex) {
                    analyticsEventHandler.send(
                        event = FeedSectionAnalyticsEvent.EnterSection(
                            newPages.items[newPages.selectedIndex].analyticsSection,
                        ),
                    )
                }
            },
        )
    }

    /** No-op for a tab this feed doesn't contribute — the block's entry point simply leads nowhere. */
    private fun openTabFromBlock(tab: FeedTabId) {
        val index = contributors.indexOfFirst { it.id == tab }.takeIf { it >= 0 } ?: return

        selectTab(index = index)
        // the card that was tapped steps out of the way, leaving the tab it opened in full view
        topBlocksCollapseRequests.tryEmit(Unit)
    }
}