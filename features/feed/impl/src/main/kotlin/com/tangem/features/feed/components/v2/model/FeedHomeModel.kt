package com.tangem.features.feed.components.v2.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.select
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.nav.FeedTabSet
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
) : Model() {

    val contributors: List<FeedTabContributor> = tabSet.tabs

    val contributorsById: Map<FeedTabId, FeedTabContributor> = contributors.associateBy { it.id }

    val tabs: ImmutableList<FeedTabUM> = contributors
        .map { FeedTabUM(id = it.id.value, title = it.title, icon = it.icon) }
        .toImmutableList()

    val pagesNavigation = PagesNavigation<FeedTabId>()

    fun selectTab(index: Int) {
        pagesNavigation.select(index = index)
    }
}