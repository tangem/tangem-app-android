package com.tangem.features.feed.components.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.features.feed.model.feed.FeedComponentModel
import com.tangem.features.feed.ui.feed.FeedListContent
import com.tangem.features.feed.ui.feed.FeedListHeader

internal class DefaultFeedComponent(
    appComponentContext: AppComponentContext,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    private val feedComponentModel = getOrCreateModel<FeedComponentModel>()

    @Composable
    override fun Title() {
        val state by feedComponentModel.state.collectAsStateWithLifecycle()
        FeedListHeader(state.searchBar)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by feedComponentModel.state.collectAsStateWithLifecycle()
        FeedListContent(
            modifier = modifier,
            state = state,
        )
    }

    @Composable
    override fun Footer() = Unit
}