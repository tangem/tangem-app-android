package com.tangem.features.feed.components.v2.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.select
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.feed.analytics.FeedSectionAnalyticsEvent
import com.tangem.features.feed.components.v2.blocks.FeedBlockCallbacks
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.nav.FeedTabSet
import com.tangem.features.feed.nav.analyticsSection
import com.tangem.features.feed.ui.FeedTabUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
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

    val topBlocksCallbacks = object : FeedBlockCallbacks {

        /** No-op for a tab this feed doesn't contribute — the block's entry point simply leads nowhere. */
        override fun onOpenTab(tab: FeedTabId) {
            val index = contributors.indexOfFirst { it.id == tab }
            if (index >= 0) selectTab(index = index)
        }
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
}