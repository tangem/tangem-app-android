package com.tangem.features.collectibles.impl.discovery

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.collectibles.impl.discovery.model.CollectiblesDiscoveryModel
import com.tangem.features.collectibles.impl.discovery.ui.collectiblesDiscoveryList
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow

@Stable
internal class DefaultCollectiblesDiscoveryComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
) : CollectiblesDiscoveryComponent, AppComponentContext by context {

    private val model: CollectiblesDiscoveryModel = getOrCreateModel()

    override val uiState: StateFlow<CollectiblesDiscoveryUM>
        get() = model.uiState

    override val tabScrollState: ScrollState = ScrollState(initial = 0)

    override fun onLoadMore() = model.onLoadMore()

    override fun onRefresh() = model.onRefresh()

    override fun LazyListScope.content(uiState: CollectiblesDiscoveryUM, modifier: Modifier) {
        collectiblesDiscoveryList(state = uiState, tabScrollState = tabScrollState)
    }

    @AssistedFactory
    interface Factory : CollectiblesDiscoveryComponent.Factory {
        override fun create(context: AppComponentContext): DefaultCollectiblesDiscoveryComponent
    }
}