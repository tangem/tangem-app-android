package com.tangem.features.feed.components.v2

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.feed.nav.FeedScreenFactory
import javax.inject.Inject

internal class FeedNewsDetailsScreenFactory @Inject constructor(
    private val componentFactory: FeedNewsDetailsComponent.Factory,
) : FeedScreenFactory {

    override fun create(context: AppComponentContext, route: FeedRoute): FeedScreenComponent {
        return componentFactory.create(context = context, params = route as FeedRoute.NewsDetails)
    }
}