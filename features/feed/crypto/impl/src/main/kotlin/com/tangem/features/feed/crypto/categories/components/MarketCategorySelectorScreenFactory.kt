package com.tangem.features.feed.crypto.categories.components

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.feed.crypto.categories.MarketCategorySelectorRoute
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.feed.nav.FeedScreenFactory
import javax.inject.Inject

/** Registry adapter: [MarketCategorySelectorComponent.Factory] is typed on the concrete route. */
internal class MarketCategorySelectorScreenFactory @Inject constructor(
    private val componentFactory: MarketCategorySelectorComponent.Factory,
) : FeedScreenFactory {

    override fun create(context: AppComponentContext, route: FeedRoute): FeedScreenComponent {
        return componentFactory.create(context = context, params = route as MarketCategorySelectorRoute)
    }
}