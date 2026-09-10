package com.tangem.features.feed.rwa.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_earth_20
import com.tangem.features.feed.FeedFeatureToggles
import com.tangem.features.feed.nav.FeedTabComponent
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

internal class RwaFeedTabContributor @Inject constructor(
    private val feedFeatureToggles: FeedFeatureToggles,
    private val componentFactory: RwaFeedTabComponent.Factory,
    private val searchComponentFactory: RwaFeedSearchTabComponent.Factory,
) : FeedTabContributor {

    override val id: FeedTabId = FeedTabId.RealAssets

    override val title: TextReference = resourceReference(R.string.feed_tab_rwa)

    override val icon: ImageVector = Icons.ic_earth_20

    /** The RWA world ships behind the TWI-1477 toggle. */
    override val isAvailable: Boolean
        get() = feedFeatureToggles.isRwaAndCategoriesEnabled

    override fun createTab(context: AppComponentContext): FeedTabComponent {
        return componentFactory.create(context = context, params = Unit)
    }

    override fun createSearchTab(context: AppComponentContext, query: StateFlow<String>): FeedTabComponent {
        return searchComponentFactory.create(
            context = context,
            params = RwaFeedSearchTabComponent.Params(query = query),
        )
    }
}