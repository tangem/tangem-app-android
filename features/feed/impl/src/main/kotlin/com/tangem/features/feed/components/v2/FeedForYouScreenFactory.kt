package com.tangem.features.feed.components.v2

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.feed.nav.FeedScreenFactory
import javax.inject.Inject

/** Registry adapter: [FeedForYouComponent.Factory] is typed on the concrete route. */
internal class FeedForYouScreenFactory @Inject constructor(
    private val componentFactory: FeedForYouComponent.Factory,
) : FeedScreenFactory {

    override fun create(context: AppComponentContext, route: FeedRoute): FeedScreenComponent {
        return componentFactory.create(context = context, params = route as FeedRoute.ForYou)
    }
}