package com.tangem.features.feed.earn.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_coins_stack_arrow_up_20
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

    override val title: TextReference = resourceReference(R.string.feed_tab_earn)

    override val icon: ImageVector = Icons.ic_coins_stack_arrow_up_20

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