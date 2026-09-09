package com.tangem.features.feed.earn.components

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.feed.earn.impl.R
import com.tangem.features.feed.nav.FeedTabComponent
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal class EarnFeedTabContributor @Inject constructor(
    private val componentFactory: EarnFeedTabComponent.Factory,
    private val searchComponentFactory: EarnFeedSearchTabComponent.Factory,
) : FeedTabContributor {

    override val id: FeedTabId = FeedTabId.Earn

    override val title: TextReference = resourceReference(R.string.earn_title)

    override val isAvailable: Boolean = true

    override fun createTab(context: AppComponentContext): FeedTabComponent {
        return componentFactory.create(context = context, params = Unit)
    }

    override fun createSearchTab(context: AppComponentContext, query: StateFlow<String>): FeedTabComponent {
        return searchComponentFactory.create(
            context = context,
            params = EarnFeedSearchTabComponent.Params(query = query),
        )
    }
}