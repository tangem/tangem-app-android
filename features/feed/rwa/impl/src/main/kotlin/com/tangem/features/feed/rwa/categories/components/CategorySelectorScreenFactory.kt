package com.tangem.features.feed.rwa.categories.components

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.feed.nav.FeedScreenFactory
import com.tangem.features.feed.rwa.categories.CategorySelectorRoute
import javax.inject.Inject

/** Registry adapter: [CategorySelectorComponent.Factory] is typed on the concrete route. */
internal class CategorySelectorScreenFactory @Inject constructor(
    private val componentFactory: CategorySelectorComponent.Factory,
) : FeedScreenFactory {

    override fun create(context: AppComponentContext, route: FeedRoute): FeedScreenComponent {
        return componentFactory.create(context = context, params = route as CategorySelectorRoute)
    }
}