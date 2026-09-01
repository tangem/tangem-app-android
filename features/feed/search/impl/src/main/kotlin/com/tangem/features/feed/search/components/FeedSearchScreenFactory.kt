package com.tangem.features.feed.search.components

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.feed.nav.FeedScreenFactory
import com.tangem.features.feed.search.FeedSearchComponent
import javax.inject.Inject

/** Registry adapter: [FeedSearchComponent.Factory] is typed on the concrete route. */
internal class FeedSearchScreenFactory @Inject constructor(
    private val componentFactory: FeedSearchComponent.Factory,
) : FeedScreenFactory {

    override fun create(context: AppComponentContext, route: FeedRoute): FeedScreenComponent {
        return componentFactory.create(context = context, params = route as FeedRoute.Search)
    }
}