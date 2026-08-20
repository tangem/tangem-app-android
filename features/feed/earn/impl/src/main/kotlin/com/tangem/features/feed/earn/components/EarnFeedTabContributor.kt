package com.tangem.features.feed.earn.components

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.feed.nav.FeedTabComponent
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import javax.inject.Inject

internal class EarnFeedTabContributor @Inject constructor(
    private val componentFactory: EarnFeedTabComponent.Factory,
) : FeedTabContributor {

    override val id: FeedTabId = FeedTabId.Earn

    override val title: TextReference = stringReference("Earn")

    override val isAvailable: Boolean = true

    override fun createTab(context: AppComponentContext): FeedTabComponent {
        return componentFactory.create(context = context, params = Unit)
    }
}